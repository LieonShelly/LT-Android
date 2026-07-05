# Android Demo Flavor — 本地 Mock JSON（OkHttp Interceptor）设计

**日期：** 2026-07-05  
**状态：** Draft — 待 review  
**范围：** `demo` product flavor，Calendar 完整离线演示，零 HTTP

---

## 1. 背景与目标

### 1.1 现状

- 数据链路：`ViewModel → UseCase → Repository → ApiClient → OkHttp → 远程 API`
- `SignInDevConfig.MOCK_GOOGLE_SIGN_IN` 仅跳过 Google / Auth API，**业务 API 仍依赖网络**
- 单元测试用 `MockWebServer` 内联 JSON，无 `assets` 级 mock 资产

### 1.2 目标

| 项 | 决策 |
|----|------|
| 交付形态 | **`demo` product flavor**，与 `prod` 并存（`applicationIdSuffix = .demo`） |
| 网络 | **完全不连网** — 所有 Calendar 相关请求在 OkHttp 层被拦截并返回本地 JSON |
| v1 功能 | Calendar 全链路：`calendar-view`、`questions-of-the-day`、`markIconRead`、详情 UI |
| 入口 | **保留 SignIn UI**；demo 包点击 Google 登录 → 写 mock session → Home（不调 Auth API） |
| Mock 数据 | **单套 JSON**；`calendar-view` 按 `start`/`end` query **内存过滤** |
| 与 debug 隔离 | demo 行为由 **`BuildConfig.USE_OFFLINE_MOCK`** 控制，**不依赖** `SignInDevConfig` |

### 1.3 非目标（v1）

- Thread / Insights / User Tab 真实数据
- Submit Answer 流程
- 多 scenario 切换 UI
- Auth refresh / 401 离线链路
- SSE icon 生成进度

---

## 2. 方案选型

### 2.1 选定：方案 B — OkHttp Mock Interceptor

在 demo flavor 的 `ApiClient` 上挂载 **`MockResponseInterceptor`**：

- **不调用** `chain.proceed()` → 保证零出站 HTTP
- **Repository / UseCase / ViewModel 不变** — 仍走 `DefaultReflectionRepository`、`DefaultIconRepository`
- JSON 从 `app/src/demo/assets/mock/` 读取

### 2.2 未选方案及原因

| 方案 | 原因 |
|------|------|
| A Mock Repository | 用户选定 B；B 更少改动 Repository 层 |
| C 内嵌 MockWebServer | 仍走网络栈，生命周期复杂，不符合「完全离线 demo 包」语义 |

---

## 3. Gradle & BuildConfig

### 3.1 Product Flavors

```kotlin
flavorDimensions += "distribution"
productFlavors {
    create("prod") {
        dimension = "distribution"
        buildConfigField("boolean", "USE_OFFLINE_MOCK", "false")
    }
    create("demo") {
        dimension = "distribution"
        applicationIdSuffix = ".demo"
        versionNameSuffix = "-demo"
        buildConfigField("boolean", "USE_OFFLINE_MOCK", "true")
    }
}
```

启用 `buildFeatures { buildConfig = true }`。

### 3.2 Source Sets

| 路径 | 用途 |
|------|------|
| `app/src/demo/assets/mock/` | demo 专属 mock JSON + 图片 |
| `app/src/main/` | 共享代码（含 `MockResponseInterceptor`） |

`prod` 构建不包含 demo assets；demo 构建合并 `main` + `demo` assets。

### 3.3 运行

```bash
./gradlew :app:installDemoDebug
# 或 Android Studio Build Variant: demoDebug
```

---

## 4. Mock 资产布局

```
app/src/demo/assets/mock/
├── calendar/
│   └── calendar_view.json       # UniversalResponse envelope，含 2025-01 ~ now+1 月数据
├── reflection/
│   └── questions_of_the_day.json
└── images/
    ├── stamp_01.webp
    ├── stamp_02.webp
    └── ...                      # Coil 离线 URL 引用
```

### 4.1 JSON 格式

与后端 API 一致（`success` + `data` envelope），便于 Repository 现有 `parseJson()` 复用。

**`calendar_view.json`** — 结构同 `GET /api/calendar-view` 响应；单日需覆盖：

- 1 / 2 / 3 / 4+ reflections（验证 stamp 布局）
- `icon.status = "GENERATED"`，`icon.url = "file:///android_asset/mock/images/stamp_01.webp"`
- 部分 icon `read_at = null`（验证 markRead）

**`questions_of_the_day.json`** — 3 条问题，category 可仅含 `name`（与 API 一致）。

### 4.2 calendar-view 日期过滤

Interceptor 处理 `GET /api/calendar-view` 时：

1. 读取 `calendar_view.json` 全量 `data` 数组
2. 解析 query：`start`、`end`（ISO date，`YYYY-MM-DD`）
3. 过滤：`start <= day.date <= end`
4. 重新包装为 `UniversalResponse` JSON 返回

过滤逻辑抽取为 **`MockCalendarViewFilter`**（纯函数，可单测）。

---

## 5. MockResponseInterceptor

### 5.1 职责

```
OkHttp Request
  → MockResponseInterceptor.intercept()
  → 匹配 path + method
  → 读 assets / 内存生成 body
  → 合成 Response(200, application/json)
  → 不 chain.proceed()
```

### 5.2 路由表（v1）

| Method | Path | 行为 |
|--------|------|------|
| `GET` | `/api/calendar-view` | 读 JSON + `MockCalendarViewFilter.filter(start, end)` |
| `GET` | `/api/questions-of-the-day` | 原样返回 `questions_of_the_day.json` |
| `POST` | `/api/answers/icons/{id}/read` | 返回 `{ success, data: { id, read_at } }`；见 §5.4 |
| 其他 | * | 返回 `404` JSON + `LTLog.w`（demo 不应命中） |

Path 匹配忽略 host（`ApiClient` 仍解析 `DefaultEndPoint`，但 interceptor 在 proceed 前截获）。

### 5.3 MockAssetLoader

```kotlin
interface MockAssetLoader {
    fun readText(assetPath: String): String
}
// 实现：context.assets.open("mock/...").bufferedReader().readText()
```

Interceptor 构造注入 `MockAssetLoader` + `Json`（`ignoreUnknownKeys = true`）。

### 5.4 markIconRead 行为

- 从 URL 提取 `{id}`
- 返回固定 envelope，`read_at` 为当前 UTC ISO-8601
- **内存 Map** `iconId → readAt`（进程内）；可选：下次 calendar JSON merge read_at（v1 **不 merge**，markRead 成功即可，刷新 calendar 仍显示 unread — 可接受；若需一致，v1.1 在 filter 后 patch icon.read_at）

**v1 决策：** markRead 仅返回成功 JSON，不反写 calendar 缓存；UI 详情页不依赖 re-fetch。若 QA 需要 stamp 变已读，在 v1.1 加内存 patch。

### 5.5 延迟（可选）

不加 artificial delay；若需模拟弱网，BuildConfig 字段 `MOCK_LATENCY_MS` 默认 0。

---

## 6. AppGraph 装配（demo vs prod）

### 6.1 prod（现状）

```kotlin
authenticatedApiClient = ApiClient(
    environment = environment,
    interceptors = listOf(authInterceptor, logoutInterceptor, refreshTokenInterceptor),
)
```

### 6.2 demo

```kotlin
val mockInterceptor = MockResponseInterceptor(appContext, MockAssetLoader(appContext))

authenticatedApiClient = ApiClient(
    environment = environment, // host 仍解析，但不会出网
    interceptors = listOf(mockInterceptor), // 仅 mock；不挂 auth/refresh
)
bareApiClient = ApiClient(
    environment = environment,
    interceptors = listOf(mockInterceptor),
)
```

- demo **不注册** `AuthInterceptor` / `RefreshTokenInterceptor` / `LogoutInterceptor`（无真实 token 刷新需求）
- Repository 实现 **不变**：`DefaultReflectionRepository`、`DefaultIconRepository`

### 6.3 零网络保证

- Interceptor **永不** `chain.proceed()`
- 可选：`NetworkSecurityConfig` demo 禁止 cleartext（与 prod 一致）；不依赖此配置保证离线

---

## 7. 假登录（demo flavor）

### 7.1 SignInFragment

```kotlin
when {
    BuildConfig.USE_OFFLINE_MOCK -> viewModel.signInWithMockGoogle()
    SignInDevConfig.MOCK_GOOGLE_SIGN_IN -> viewModel.signInWithMockGoogle()
    else -> signInLauncher.launch(...)
}
```

### 7.2 signInWithMockGoogle()

- 写 `SessionService.updateTokens(demo-access, demo-refresh)`
- **不调用** `authUseCase.executeGoogleLogin`
- 不调 Google Play Services

### 7.3 Token 常量

```kotlin
// DemoFlavorConfig.kt（或 BuildConfig 字符串字段）
const val DEMO_ACCESS_TOKEN = "demo-offline-access-token"
const val DEMO_REFRESH_TOKEN = "demo-offline-refresh-token"
```

与 `SignInDevConfig` 分离。

---

## 8. 数据流（demo）

```
SignInFragment [USE_OFFLINE_MOCK]
  → SessionService (local tokens)
  → HomeActivity → CalendarFragment
  → CalendarViewModel.fetchData()
  → CalendarReflectionsUseCase
  → DefaultReflectionRepository
  → ApiClient.sendRequest(GET /api/calendar-view?start=&end=)
  → MockResponseInterceptor → assets JSON (filtered)
  → UniversalResponse parse → Domain merge
```

markRead / questions-of-the-day 同理。

---

## 9. 文件清单

| 文件 | 说明 |
|------|------|
| `app/build.gradle.kts` | `prod` / `demo` flavors + `buildConfig` |
| `service/mock/MockResponseInterceptor.kt` | 核心拦截器 |
| `service/mock/MockAssetLoader.kt` | assets 读取 |
| `service/mock/MockCalendarViewFilter.kt` | start/end 过滤 |
| `service/mock/DemoFlavorConfig.kt` | demo token 常量 |
| `app/src/demo/assets/mock/**` | JSON + 图片 |
| `domain/signin/SignInFragment.kt` | demo 分支 |
| `app/AppGraph.kt` | demo 拦截器 wiring |
| `docs/app-startup-flow.md` | 补充 demo 构建说明 |

**不改：** `ReflectionRepository` 接口、`CalendarViewModel`、UseCase 签名。

---

## 10. 测试策略

| 测试 | 内容 |
|------|------|
| `MockCalendarViewFilterTest` | 纯函数 date range 过滤 |
| `MockResponseInterceptorTest` | Robolectric + demo assets；断言 path 路由与 status 200 |
| `MockAssetLoaderTest` | demo assets 可读 |
| 现有 UseCase 测试 | **保持** MockWebServer（与 demo assets 结构对齐即可） |

demo flavor **不做** 仪器测试门禁（可选后续）。

---

## 11. 验收标准

- [ ] `./gradlew :app:assembleDemoDebug` 成功
- [ ] 安装 `com.littlethingsandroidai.demo`，**飞行模式下**可完成：假登录 → Calendar 展示 stamp → 点 stamp 详情 → markRead 不 crash
- [ ] TodayQuestion 浮层展示 3 题
- [ ] `./gradlew :app:assembleProdDebug` 行为与现网一致（`USE_OFFLINE_MOCK = false`）
- [ ] 单元测试全部通过

---

## 12. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 误调用 `chain.proceed()` 出网 | Code review + 单测断言 interceptor 不 proceed |
| Coil 无法加载 `file:///android_asset/...` | 启动时用 1 张图验证；失败则改 `android.resource://` drawable |
| demo/prod 行为漂移 | JSON schema 与 `CalendarReflectionsUseCaseTest` 样例同步 |
| Interceptor 路由遗漏 | 未知 path 返回 404 + log，便于发现 |

---

## 13. 后续迭代

- markRead 后 patch calendar in-memory read_at
- 多 scenario 切换（`assets/mock/scenarios/`）
- Thread / Insights mock 路由扩展
- demo 包应用名 / icon 角标「DEMO」

---

## 14. iOS 对照

无直接 iOS 等价物；最接近的是在 DEBUG 使用 stub repository。Android demo flavor + interceptor 为 **Android 专用离线演示方案**。
