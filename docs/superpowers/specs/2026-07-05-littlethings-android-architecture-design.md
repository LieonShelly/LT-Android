# Little Things Android — 架构设计（镜像 iOS）

**日期：** 2026-07-05  
**状态：** 已批准（2026-07-05 修订：接口/UI 参考源）  
**参考工程：** `LittleThingsApp`（iOS）  
**目标工程：** `LittleThingsAndroidAI`  
**接口权威来源（优先级）：**

1. `LittleThingsApp/app/LTApp/LTApp/Source/Service/` — `*Request.swift`、Repository、DTO、UseCase（**主参考**）
2. `LittleThingsApp/app/LTApp/API/api.md` — 辅助参考（文档可能不全）

---

## 1. 目标与约束

### 目标

- 将 Android 工程架构对齐 iOS `LittleThingsApp`：多模块 Core + App 内 Domain/Service 分层。
- 第一版交付**可运行的 Auth 最小链路**（Google 登录 → Token 持久化 → 进入 Home 占位页）。
- UI 第一版**仅完整实现 SignIn**；Splash、Home 四 Tab 及其余页面为占位。
- 通过 XML 为主熟悉 Android 生态；Compose 依赖保留在工程中，第二版再用于 Insights 等页面。

### 已确认约束

| 项 | 选择 |
|----|------|
| 模块结构 | 完整 Gradle 多模块（方案 A） |
| DI | 手动 DI，`AppGraph` 对齐 iOS `AppCoordinator.init` |
| UI | XML + ViewBinding 为主；Compose BOM 保留待用 |
| 导航 | 双 Activity 根：`PreHomeActivity` / `HomeActivity`（方案 B） |
| 交付深度 | 骨架 + Auth 最小可运行链路（方案 B） |
| 第一版 UI | 仅 SignIn 完整实现，其余占位 |
| 认证 | Google Sign-In（Apple 登录后续迭代） |
| 接口参考 | **优先** iOS `Service/` 目录；`api.md` 为辅；不检索 `flutter_framework` / `LTApp-BE` |
| 代码检索 | 优先 **codegraph** MCP（见根目录 `AGENTS.md` §5） |
| UI 参考 | 优先 iOS `Domain/` 对应 View/ViewModel；Figma 由用户后续提供时通过 figma MCP 补充 |

### 不在第一版范围

- Onboarding / Splash / Welcome / FirstQuestion 真实 UI
- Calendar、Thread、Insights、User 业务 UI（Home 仅 Tab 占位）
- Compose 嵌入页面、Rive/Lottie 动画、Metal/SVG 图标渲染
- 推送通知、Feature Toggle UI、NetworkMonitor 调试面板
- SSE 流式请求（`:core:network` 预留接口）

---

## 2. 参考源与工具链

### 2.1 接口与网络层移植规则

Android 端每个 API 以 iOS `Service/` 为准，按以下顺序对照移植：

| iOS 文件 | Android 对应 |
|----------|--------------|
| `service/{domain}/request/*Request.swift` | `service/{domain}/request/*Request.kt`（sealed class + `ApiRequest`） |
| `service/dto/*DTO.swift` | `service/dto/*Dto.kt` |
| `service/{domain}/repository/*Repository.swift` | `service/{domain}/repository/*Repository.kt` |
| `service/{domain}/usecase/*UseCase.swift` | `service/{domain}/usecase/*UseCase.kt` |
| `service/DefaultEndPoint.swift` | `service/DefaultEndPoint.kt` |

**示例（Auth，第一版必移植）：**

| 请求 | Method | Path | Body |
|------|--------|------|------|
| `googleLogin` | POST | `/api/auth/google` | `{ "idToken": "..." }` |
| `refreshToken` | POST | `/api/auth/refresh` | `{ "refresh_token": "..." }` |

路径、HTTP 方法、JSON 字段名须与 iOS `AuthRequest.swift` 保持一致；`api.md` 与其冲突时以 Service 代码为准。

### 2.2 代码检索（codegraph）

实现与对照 iOS 时：

1. **首选** `codegraph_explore`，查询目标符号（如 `AuthRequest`、`SignInViewModel`）
2. 仅在 codegraph 未覆盖时，对已知文件路径使用 Read/Grep
3. 禁止从 `flutter_framework`、`LTApp-BE` 检索 Android 相关实现

### 2.3 UI 实现规则

| 优先级 | 来源 | 说明 |
|--------|------|------|
| 1 | iOS `Domain/{Feature}/` View + ViewModel | 布局结构、文案、交互流程、状态机 |
| 2 | iOS `:core:UIComponent` / `AppColor` / 字体 | 颜色、字号、圆角等设计 token |
| 3 | 用户提供的 Figma 设计图（figma MCP） | 覆盖或细化 iOS 实现；以 Figma 为准 |

第一版 SignIn：按 iOS `SignInView` / `SignInViewModel` 实现 Android 等价 UI（见 §10）。

---

## 3. 架构总览

### 推荐方案：严格镜像 iOS 分层（方案一）

单个 `:app` 模块承载 Domain + Service；Core 能力拆为独立 Gradle module，与 iOS `core/` 对应。

**未采纳方案：**

- **独立 `:data` 模块：** 与 iOS（Service 在 app 内）不一致，首版 wiring 成本不必要。
- **按 Feature 拆 module（`:feature:signin` 等）：** 当前仅搭架构，过度拆分。

### Gradle 模块

```
LittleThingsAndroidAI/
├── core/
│   ├── network/          # ApiClient、拦截器链、Request/Response、SSL Pinning
│   ├── common/           # FeatureToggle、InjectionValues、Logger、AppEnvironment
│   ├── persistence/      # TokenStorage、KeyDataStorage
│   └── uicomponent/      # Theme、Typography、共享 UI 组件
├── app/                  # Application、Activity、Domain、Service
├── build.gradle.kts
└── settings.gradle.kts
```

### 模块依赖方向（只向内）

```
:app
  → :core:uicomponent, :core:network, :core:common, :core:persistence

:core:uicomponent → :core:common
:core:network     → :core:common
:core:persistence → :core:common
:core:common      → （无项目内依赖）
```

### 与 iOS 对照

| iOS | Android |
|-----|---------|
| `core/Network` (LTNetwork) | `:core:network` |
| `core/Common` | `:core:common` |
| `core/Persistence` | `:core:persistence` |
| `core/UIComponent` | `:core:uicomponent` |
| `app/LTApp/Source/Domain/` | `app/.../domain/` |
| `app/LTApp/Source/Service/` | `app/.../service/` |
| `AppCoordinator` | `AppGraph` + Activity 根切换 |
| `AppDataWithAuthorizationService` | `AppDataWithAuthorizationService` |
| `NavigationPath` + Coordinator | NavController + Coordinator 封装 |

---

## 4. `:app` 包结构

```
app/src/main/java/com/littlethingsandroidai/
├── app/
│   ├── LTApplication.kt
│   ├── AppGraph.kt
│   ├── SplashActivity.kt          # launcher，占位 UI
│   ├── prehome/
│   │   ├── PreHomeActivity.kt
│   │   └── PreHomeCoordinator.kt
│   └── home/
│       ├── HomeActivity.kt        # 占位四 Tab
│       └── HomeCoordinator.kt
├── common/                        # Paginator 等（首版可空）
├── domain/
│   ├── coordinator/
│   │   ├── Coordinator.kt
│   │   └── Route.kt               # sealed interface
│   ├── signin/
│   │   ├── SignInFragment.kt      # 第一版完整实现
│   │   └── SignInViewModel.kt
│   └── home/
│       └── HomeTabAdapter.kt        # ViewPager2 占位 Tab
└── service/
    ├── AppDataWithAuthorizationService.kt
    ├── AppDataWithoutAuthorizationService.kt
    ├── dto/
    ├── interceptor/               # Auth / RefreshToken / Logout
    ├── network/
    │   └── SSLPinningValidator.kt
    ├── auth/
    │   ├── Repository/
    │   ├── UseCase/
    │   └── SessionService.kt
    ├── reflection/                # 接口 + 占位实现
    ├── report/
    ├── user/
    ├── icon/
    └── notification/
```

---

## 5. 依赖注入（手动 DI）

### AppGraph 组装顺序

对齐 iOS `AppCoordinator.init`：

1. `SSLPinningValidator(environment)`
2. 无拦截器 `ApiClient` → `SessionDataRepository` + `AppDataWithoutAuthorizationService`
3. `SessionService`（`:core:persistence`）
4. 拦截器链：`AuthInterceptor` → `RefreshTokenInterceptor` → `LogoutInterceptor`
5. 正式 `ApiClient` + 各 `Repository`
6. `AppDataWithAuthorizationService`（lazy UseCase 属性）
7. `InjectionValues.register(FeatureToggle)` 等

### 访问方式

```kotlin
// Application
class LTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.build(AppEnvironment.RELEASE)
    }
}

// Activity / ViewModel
val service = AppGraph.current.appDataService
```

ViewModel 由 Activity/Fragment 从 `AppGraph` 取依赖并通过构造函数传入，避免全局静态调用散落在 UI 层。

---

## 6. 导航

### 根切换（对齐 AppRootType）

| iOS | Android |
|-----|---------|
| `AppRootType.preHome` | `PreHomeActivity` |
| `AppRootType.home` | `HomeActivity` |

### 启动流程

```
SplashActivity (launcher, 占位)
  → AppGraph.sessionService.hasValidToken()
  → true  → HomeActivity
  → false → PreHomeActivity → SignInFragment
```

### PreHome 路由（第一版）

| Route | 第一版 |
|-------|--------|
| `PreHomeRoute.Login` | **完整实现**（SignInFragment） |
| `PreHomeRoute.Splash` | sealed class 预留，无 UI |
| `PreHomeRoute.Onboarding` | 预留 |
| `PreHomeRoute.Welcome` | 预留 |
| `PreHomeRoute.FirstQuestion` | 预留 |

登录成功：`PreHomeActivity.finish()` → `startActivity(HomeActivity)`。

### Home 路由（第一版）

- `HomeActivity` + `ViewPager2` + `TabLayout` 四 Tab 占位（Calendar / Thread / Insights / User）。
- `HomeCoordinator`、`UserHomeCoordinator` 骨架建好；`UserRoute`（aboutMe、persona 等）sealed class 预留。
- 第一版无深层 push 页面。

### Coordinator 接口

```kotlin
interface Coordinator {
    fun push(route: Route)
    fun pop()
    fun popToRoot()
}
```

`PreHomeCoordinator` 持有 `NavController`，封装 push/pop 语义，对齐 iOS `Coordinator` 扩展方法。

---

## 7. Service 层

### 目录约定（与 iOS 一致）

```
service/{Domain}/
├── model/          # 领域模型（可选，部分领域合并到顶层）
├── dto/            # 或在 service/dto/ 集中
├── request/        # ApiRequest 枚举
├── repository/     # 接口 + 实现
└── usecase/        # 接口 + 实现
```

### 第一版实现范围

| 领域 | 第一版 |
|------|--------|
| **Auth** | 完整：`AuthRepository`、`SessionDataRepository`、`AuthUseCase`（Google）、`RefreshTokenUseCase`、`SessionService` |
| Reflection | 接口 + `UnsupportedOperationException` 或 no-op 占位 |
| Report | 接口 + 占位 |
| User | 接口 + 占位 |
| Icon | 接口 + 占位 |
| Notification | 接口 + 占位 |

### AppDataWithAuthorizationService

对齐 iOS 协议，第一版仅 `authUseCase` 等有真实实现；其余 lazy 属性指向占位 UseCase，保证 `AppGraph` 依赖图完整、后续填充时不改 wiring。

---

## 8. Network 层（`:core:network`）

### 核心类型

| iOS (LTNetwork) | Android |
|-----------------|---------|
| `ApiClient` | `ApiClient`（OkHttp 封装） |
| `Request` | `interface ApiRequest` |
| `UniversalResponse<T>` | `data class UniversalResponse<T>` |
| `AppNetworkError` | sealed class `AppNetworkError` |
| 拦截器链 | OkHttp `Interceptor` |
| `SSLPinningValidator` | OkHttp `CertificatePinner` + 环境配置 |
| 最大重试 2 次 | `RetryInterceptor` |

### 环境 Base URL（对齐 DefaultEndPoint）

| 环境 | Host |
|------|------|
| `DEV` / `STAGING` | `things.dvacode.tech` |
| `RELEASE` | `api.thelilthings.app` |

### 拦截器链（应用层，`:app`）

与 iOS 相同顺序：

1. `AuthInterceptor` — 附加 Bearer Token
2. `RefreshTokenInterceptor` — 401 时刷新 Token 并重试
3. `LogoutInterceptor` — 刷新失败时清 Session，通知 UI 回 SignIn

无拦截器的 `ApiClient` 实例专用于 RefreshToken 请求，避免循环依赖。

---

## 9. Persistence 层（`:core:persistence`）

| iOS | Android |
|-----|---------|
| `KeyChainStorage` | `EncryptedSharedPreferences`（Token、敏感字符串） |
| `UserDefaultStorage` | `DataStore<Preferences>`（非敏感键值） |
| `TokenProvider` | `SessionService` implements `TokenProvider` |

---

## 10. Common 层（`:core:common`）

| 能力 | 第一版 |
|------|--------|
| `AppEnvironment` | DEV / STAGING / RELEASE |
| `FeatureToggle` + `LTAppFeatureConfig` | 枚举与 iOS 对齐，UI 开关不做 |
| `InjectionValues` | FeatureToggle 注册 |
| `LTLog` | 基础日志接口 + Android Logcat sink |
| JailBreak / Crash 采集 | 接口预留，不实现 |

---

## 11. UI 层（第一版）

| 页面 | 实现程度 | 技术 |
|------|----------|------|
| **SignIn** | **完整** | XML + ViewBinding；对照 iOS `SignInView` |
| SplashActivity | 占位 | XML；检查 Token 并路由 |
| PreHomeActivity | 容器 | XML + NavHostFragment |
| HomeActivity | 占位 | XML ViewPager2 + TabLayout，每 Tab 一个占位 Fragment |
| 其余 PreHome / Home 子页 | 不实现 | Route sealed class 预留 |

### SignIn — iOS 对照清单（第一版）

参考：`Domain/SignIn/SignInView.swift`、`SignInViewModel.swift`

**布局结构（自上而下）：**

1. 全屏默认背景（对齐 iOS `.defaultBackground()`）
2. 中部：App Icon（72×68 dp 比例）、标题 `"the little things"`（LittleThing 字体 / 最接近的 Android 字体）
3. 底部：登录按钮区 + 条款勾选

**交互与状态（对齐 ViewModel）：**

| 行为 | iOS | Android |
|------|-----|---------|
| 进入页 | `.task { fetchData() }` | `viewLifecycleOwner.lifecycleScope` 调 `fetchOnboardingSentence` |
| Google 登录 | `GIDSignIn` → `loginWihtGoogle` → `executeGoogleLogin` | Google Sign-In SDK → 同 UseCase 链路 |
| 登录成功 | `onLoginSuccess` → 直接进 Home（`onboardingEnabled == false`） | `HomeActivity`，finish PreHome |
| 条款未勾选 | 按钮 shake 动画 + 阻止登录 | 等效 shake + Toast/Snackbar |
| 错误 | `showError` 状态 | Snackbar 或 Dialog |

**按钮样式（对齐 iOS）：**

- **Google 按钮**：白底、高 54dp、圆角 14dp、1dp 描边 `#1D1D1D`；Google 图标 22.8dp + `"Sign in with Google"` 文案（SF Pro Bold → Android 等价 bold）
- **Apple 按钮**：第一版 Android **不展示**（平台限制）；仅 Google 登录

**设计 token（来自 `:core:uicomponent`）：**

- 从 iOS `AppColor`、`TextStyle`、`AppFontName` 提取 SignIn 所需颜色与字号，写入 Android theme
- 用户后续提供 Figma 时，以 Figma 覆盖 token 差异部分

### 后续页面 UI

非 SignIn 页面在实现阶段同样优先对照 iOS `Domain/`；若用户提供 Figma，则 Figma 优先于 iOS 像素级细节。

---

## 12. 第一版交付标准

1. `./gradlew assembleDebug` 编译通过。
2. 冷启动 → Splash（占位）→ PreHomeActivity → SignIn。
3. SignIn 完整可用：Google 登录、loading、错误提示。
4. 登录成功 → `POST /api/auth/google`，body `{ "idToken": "..." }`（与 iOS `AuthRequest.googleLogin` 一致）→ Token 写入 EncryptedSharedPreferences。
5. 跳转 HomeActivity（四 Tab 占位）。
6. 杀进程重启：有效 Token 跳过 SignIn，直达 HomeActivity。
7. Access Token 过期：RefreshToken 拦截器自动刷新；失败清除 Session 并回到 SignIn。
8. `:core:network` 单元测试：ApiClient 重试、拦截器链、401 刷新路径。

---

## 13. 测试策略

| 层级 | 第一版 |
|------|--------|
| `:core:network` | JUnit：ApiClient、RetryInterceptor、InterceptorChain |
| `:core:persistence` | JUnit + Robolectric 或 Instrumentation：Token 读写 |
| `:app` service | JUnit：`AuthUseCase` mock Repository |
| UI | 可选：`SignInFragment` Espresso 冒烟（非阻塞） |

---

## 14. 后续迭代（不在本 spec 实施）

1. PreHome 完整流程 UI（Splash → Onboarding → Welcome → FirstQuestion）
2. Home 四 Tab 真实业务 + Compose Insights Tab
3. Reflection / Report / User 等 Service 实现与 UI
4. `:core:networkmonitor` 调试面板
5. Apple Sign-In（Credential Manager）、推送 FCM
6. Feature Toggle 调试入口

---

## 15. 决策记录

| 日期 | 决策 |
|------|------|
| 2026-07-05 | 采用完整多模块 + 手动 DI + 双 Activity 根 |
| 2026-07-05 | UI 混合策略：首版 XML only，Compose 保留依赖 |
| 2026-07-05 | 首版仅 SignIn 完整 UI，Auth 最小链路可运行 |
| 2026-07-05 | 接口以 iOS `Service/` 为主、`api.md` 为辅；codegraph 优先检索 |
| 2026-07-05 | UI 优先对照 iOS Domain；Figma 由用户后续提供时可覆盖 |
