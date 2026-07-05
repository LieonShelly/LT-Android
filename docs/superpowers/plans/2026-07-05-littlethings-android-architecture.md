# Little Things Android 架构搭建 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `LittleThingsAndroidAI` 搭建与 iOS 对齐的多模块架构，交付 Google 登录最小可运行链路；SignIn 完整 UI，其余占位。

**Architecture:** 5 个 Gradle module（`:core:common/network/persistence/uicomponent` + `:app`）；`AppGraph` 手动 DI；双 Activity 根（PreHome/Home）；Service 层镜像 iOS `Source/Service/`；接口定义以 iOS `*Request.swift` 为准。

**Tech Stack:** Kotlin 2.2、AGP 9.1、ViewBinding、Navigation Component、OkHttp 4、EncryptedSharedPreferences、Google Sign-In、JUnit 5、MockWebServer

**Spec:** [2026-07-05-littlethings-android-architecture-design.md](../specs/2026-07-05-littlethings-android-architecture-design.md)

**iOS 对照（实现前用 codegraph 检索）：**

| 领域 | iOS 路径 |
|------|----------|
| Auth Request | `Service/Auth/Repository/AuthRequest.swift` |
| Auth Repository | `Service/Auth/Repository/AuthRepository.swift` |
| Session | `Service/Auth/SessionService.swift` |
| Interceptors | `Service/Interceptor/*.swift` |
| SignIn UI | `Domain/SignIn/SignInView.swift`, `SignInViewModel.swift` |
| AppGraph 对照 | `Domain/Coordinator/AppCoordinator.swift` |
| EndPoint | `Service/DefaultEndPoint.swift` |

---

## File Structure Overview

```
LittleThingsAndroidAI/
├── settings.gradle.kts                          # MODIFY: include 4 core modules
├── gradle/libs.versions.toml                    # MODIFY: okhttp, nav, viewbinding, google-signin
├── core/
│   ├── common/build.gradle.kts                  # CREATE
│   └── common/src/main/java/com/littlethingsandroidai/core/common/
│       ├── AppEnvironment.kt
│       ├── feature/LTAppFeatureConfig.kt
│       ├── injection/InjectionValues.kt
│       └── log/LTLog.kt
│   ├── persistence/build.gradle.kts             # CREATE
│   └── persistence/src/main/java/.../persistence/
│       ├── TokenProvider.kt
│       ├── SessionService.kt
│       └── SecureTokenStorage.kt
│   ├── network/build.gradle.kts                 # CREATE
│   └── network/src/
│       ├── main/java/.../network/
│       │   ├── ApiClient.kt
│       │   ├── ApiRequest.kt
│       │   ├── HttpMethod.kt
│       │   ├── UniversalResponse.kt
│       │   ├── AppNetworkError.kt
│       │   ├── RetryInterceptor.kt
│       │   └── EndPoint.kt
│       └── test/java/.../network/ApiClientTest.kt
│   └── uicomponent/build.gradle.kts             # CREATE
│       └── uicomponent/src/main/
│           ├── java/.../uicomponent/AppColors.kt
│           └── res/values/colors.xml, themes.xml, fonts/
├── app/
│   ├── build.gradle.kts                         # MODIFY: modules, viewBinding, nav, google
│   └── src/main/
│       ├── AndroidManifest.xml                  # MODIFY: activities, application
│       ├── java/com/littlethingsandroidai/
│       │   ├── app/{LTApplication,AppGraph,SplashActivity,...}.kt
│       │   ├── domain/coordinator/, signin/, home/
│       │   └── service/{auth,interceptor,dto,...}.kt
│       └── res/layout/, navigation/, drawable/
```

---

### Task 1: Gradle 多模块骨架

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Create: `core/common/build.gradle.kts`
- Create: `core/network/build.gradle.kts`
- Create: `core/persistence/build.gradle.kts`
- Create: `core/uicomponent/build.gradle.kts`

- [ ] **Step 1: 更新 `gradle/libs.versions.toml`**

在 `[versions]` 追加：

```toml
okhttp = "4.12.0"
navigation = "2.9.0"
fragment = "1.8.8"
viewpager2 = "1.1.0"
material = "1.12.0"
googleSignIn = "21.3.0"
securityCrypto = "1.1.0-alpha06"
datastore = "1.1.7"
lifecycle = "2.9.0"
mockwebserver = "4.12.0"
kotlinxSerialization = "1.8.1"
```

在 `[libraries]` 追加 okhttp、navigation-fragment-ktx、navigation-ui-ktx、fragment-ktx、viewpager2、material、play-services-auth、security-crypto、datastore-preferences、lifecycle-viewmodel-ktx、mockwebserver、kotlinx-serialization-json。

在 `[plugins]` 追加：

```toml
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: 更新根 `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 3: 更新 `settings.gradle.kts`**

```kotlin
rootProject.name = "LittleThingsAndroidAI"
include(":app")
include(":core:common")
include(":core:network")
include(":core:persistence")
include(":core:uicomponent")
```

- [ ] **Step 4: 创建四个 core 模块 `build.gradle.kts`**

`:core:common` — android library，仅 `kotlin-android` plugin，minSdk 26。

`:core:network` — 依赖 `:core:common`、okhttp、kotlinx-serialization；`testImplementation` mockwebserver + junit。

`:core:persistence` — 依赖 `:core:common`、security-crypto、datastore。

`:core:uicomponent` — 依赖 `:core:common`、material；保留现有 compose BOM（待用）。

- [ ] **Step 5: 修改 `app/build.gradle.kts`**

- 添加 `alias(libs.plugins.kotlin.android)`、`kotlin-serialization`
- `buildFeatures { viewBinding = true }`
- `dependencies` 添加四个 core module、navigation、fragment、viewpager2、material、google-sign-in、lifecycle-viewmodel-ktx
- `namespace` 保持 `com.littlethingsandroidai`

- [ ] **Step 6: 验证同步**

Run: `cd LittleThingsAndroidAI && ./gradlew projects`

Expected: 显示 `:app`, `:core:common`, `:core:network`, `:core:persistence`, `:core:uicomponent`

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle/libs.versions.toml core/ app/build.gradle.kts
git commit -m "chore(android): add multi-module gradle skeleton"
```

---

### Task 2: `:core:common` 基础类型

**Files:**
- Create: `core/common/src/main/java/com/littlethingsandroidai/core/common/AppEnvironment.kt`
- Create: `core/common/src/main/java/com/littlethingsandroidai/core/common/feature/FeatureRolloutStage.kt`
- Create: `core/common/src/main/java/com/littlethingsandroidai/core/common/feature/LTAppFeatureConfig.kt`
- Create: `core/common/src/main/java/com/littlethingsandroidai/core/common/feature/FeatureToggle.kt`
- Create: `core/common/src/main/java/com/littlethingsandroidai/core/common/injection/InjectionValues.kt`
- Create: `core/common/src/main/java/com/littlethingsandroidai/core/common/log/LTLog.kt`

- [ ] **Step 1: 实现 AppEnvironment**

```kotlin
package com.littlethingsandroidai.core.common

enum class AppEnvironment { DEV, STAGING, RELEASE }
```

- [ ] **Step 2: 实现 FeatureToggle（对齐 iOS LTAppFeatureConfig）**

```kotlin
enum class LTAppFeatureConfig {
    LOGOUT, INSIGHTS, CALENDAR_VIEW, THREAD;

    val stage: FeatureRolloutStage
        get() = when (this) {
            LTAppFeatureConfig.LOGOUT -> FeatureRolloutStage.UNDER_DEVELOPMENT
            LTAppFeatureConfig.INSIGHTS -> FeatureRolloutStage.INTERNAL
            LTAppFeatureConfig.CALENDAR_VIEW -> FeatureRolloutStage.UNDER_DEVELOPMENT
            LTAppFeatureConfig.THREAD -> FeatureRolloutStage.RELEASE
        }
}

enum class FeatureRolloutStage { UNDER_DEVELOPMENT, INTERNAL, RELEASE }

class FeatureToggle(private val currentStage: FeatureRolloutStage) {
    fun isEnabled(config: LTAppFeatureConfig): Boolean =
        config.stage.ordinal <= currentStage.ordinal
}
```

- [ ] **Step 3: 实现 InjectionValues**

```kotlin
object InjectionValues {
    private val values = mutableMapOf<Class<*>, Any>()

    fun <T : Any> register(type: Class<T>, component: T) {
        values[type] = component
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: Class<T>): T = values[type] as? T
        ?: error("InjectionValues: ${type.simpleName} not registered")
}
```

- [ ] **Step 4: 实现 LTLog（Logcat sink）**

```kotlin
object LTLog {
    fun d(tag: String, message: String) = Log.d(tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        Log.e(tag, message, throwable)
}
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :core:common:assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git commit -am "feat(core-common): add environment, feature toggle, injection, logging"
```

---

### Task 3: `:core:network` — ApiClient 与测试

**Files:**
- Create: `core/network/src/main/java/com/littlethingsandroidai/core/network/EndPoint.kt`
- Create: `core/network/src/main/java/com/littlethingsandroidai/core/network/ApiRequest.kt`
- Create: `core/network/src/main/java/com/littlethingsandroidai/core/network/UniversalResponse.kt`
- Create: `core/network/src/main/java/com/littlethingsandroidai/core/network/AppNetworkError.kt`
- Create: `core/network/src/main/java/com/littlethingsandroidai/core/network/RetryInterceptor.kt`
- Create: `core/network/src/main/java/com/littlethingsandroidai/core/network/ApiClient.kt`
- Create: `core/network/src/test/java/com/littlethingsandroidai/core/network/ApiClientTest.kt`

- [ ] **Step 1: 写失败测试 `ApiClientTest`**

```kotlin
@Test
fun sendRequest_returnsBody_on200() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":0,"data":{"ok":true}}"""))
    val client = ApiClient(baseUrl = server.url("/"), environment = AppEnvironment.DEV)
    val response = client.sendRequest(TestRequest.GetPing)
    assertTrue(response.body.contains("ok"))
}

@Test
fun sendRequest_retries_onFailure_upToMax() { /* MockWebServer 503 x3, assert callCount == 3 */ }
```

- [ ] **Step 2: Run test — 预期 FAIL**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*.ApiClientTest"`

- [ ] **Step 3: 实现 ApiRequest 与 ApiClient**

```kotlin
interface EndPoint {
    fun absoluteUrl(environment: AppEnvironment): String
}

interface ApiRequest {
    val endPoint: EndPoint
    val method: HttpMethod
    val payload: HttpPayload
}

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }

sealed class HttpPayload {
    data object Empty : HttpPayload()
    data class Json(val body: Map<String, Any?>) : HttpPayload()
    data class UrlEncoding(val params: List<Pair<String, String>>) : HttpPayload()
}

class ApiClient(
    baseUrl: HttpUrl? = null,
    environment: AppEnvironment,
    interceptors: List<Interceptor> = emptyList(),
    private val maxRetry: Int = 2,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val okHttp = OkHttpClient.Builder()
        .apply { interceptors.forEach(::addInterceptor) }
        .addInterceptor(RetryInterceptor(maxRetry))
        .build()

    suspend fun sendRequest(request: ApiRequest): ApiResponse = withContext(Dispatchers.IO) {
        // build Request from ApiRequest, execute, map errors to AppNetworkError
    }
}

data class ApiResponse(val code: Int, val body: String) {
    inline fun <reified T> parseJson(): UniversalResponse<T> = /* kotlinx.serialization */
}
```

- [ ] **Step 4: 实现 RetryInterceptor**

失败时最多重试 `maxRetry` 次（对齐 iOS max 2）。

- [ ] **Step 5: Run tests — 预期 PASS**

Run: `./gradlew :core:network:testDebugUnitTest`

- [ ] **Step 6: Commit**

```bash
git commit -am "feat(core-network): add ApiClient with retry and unit tests"
```

---

### Task 4: `:core:persistence` — Token 存储

**Files:**
- Create: `core/persistence/.../TokenProvider.kt`
- Create: `core/persistence/.../SecureTokenStorage.kt`
- Create: `core/persistence/.../SessionService.kt`
- Create: `core/persistence/src/test/.../SessionServiceTest.kt`（Robolectric 或 instrumented，首版可用 androidTest）

- [ ] **Step 1: 定义 TokenProvider（对齐 iOS）**

```kotlin
interface TokenProvider {
    val accessToken: String?
    val refreshToken: String?
    fun updateTokens(accessToken: String, refreshToken: String)
    fun clear()
    fun hasValidToken(): Boolean = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()
}
```

- [ ] **Step 2: SecureTokenStorage（EncryptedSharedPreferences）**

Key: `refresh_token`（对齐 iOS `StorageKey.refreshToken`）；accessToken 内存持有。

- [ ] **Step 3: SessionService implements TokenProvider**

对齐 iOS `SessionService.swift`：`updateTokens` 写 refresh 到加密存储，access 存内存。

- [ ] **Step 4: 编译验证**

Run: `./gradlew :core:persistence:assembleDebug`

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(core-persistence): add SessionService with encrypted token storage"
```

---

### Task 5: `:core:uicomponent` — SignIn 设计 Token

**Files:**
- Create: `core/uicomponent/src/main/res/values/colors.xml`
- Create: `core/uicomponent/src/main/res/values/themes.xml`
- Create: `core/uicomponent/.../AppColors.kt`
- Copy: app icon drawable 从 iOS Assets（或占位 mipmap）到 `app/src/main/res`

对照 iOS `AppColor` / SignInView：提取 SignIn 所需颜色：

```xml
<!-- colors.xml -->
<color name="lt_white">#FFFFFF</color>
<color name="lt_grey_dark">#FF1D1D1D</color>
<color name="lt_border">#1D1D1D</color>
<color name="lt_background">#F5F0E8</color> <!-- 对照 iOS defaultBackground -->
```

- [ ] **Step 1–4:** 创建 colors、themes、`AppColors` 对象暴露 `@ColorInt` 常量

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(core-uicomponent): add shared colors and theme for SignIn"
```

---

### Task 6: `:app` Service — Auth 全链路

**Files:**
- Create: `app/.../service/DefaultEndPoint.kt`
- Create: `app/.../service/dto/UniversalResponse.kt`（若 network 层已有则 re-export）
- Create: `app/.../service/dto/LoginInfoDto.kt`
- Create: `app/.../service/auth/request/AuthRequest.kt`
- Create: `app/.../service/auth/repository/AuthRepository.kt`
- Create: `app/.../service/auth/repository/SessionDataRepository.kt`
- Create: `app/.../service/auth/usecase/AuthUseCase.kt`
- Create: `app/.../service/auth/usecase/RefreshTokenUseCase.kt`
- Create: `app/.../service/AppDataWithoutAuthorizationService.kt`
- Create: `app/.../service/AppDataWithAuthorizationService.kt`
- Test: `app/src/test/.../AuthUseCaseTest.kt`

- [ ] **Step 1: AuthRequest（严格对齐 iOS）**

```kotlin
sealed class AuthRequest : ApiRequest {
    data class GoogleLogin(val idToken: String) : AuthRequest() {
        override val endPoint = DefaultEndPoint.baseUrl("/api/auth/google")
        override val method = HttpMethod.POST
        override val payload = HttpPayload.Json(mapOf("idToken" to idToken))
    }
    data class RefreshToken(val refreshToken: String) : AuthRequest() {
        override val endPoint = DefaultEndPoint.baseUrl("/api/auth/refresh")
        override val method = HttpMethod.POST
        override val payload = HttpPayload.Json(mapOf("refresh_token" to refreshToken))
    }
}
```

- [ ] **Step 2: LoginInfoDto**

```kotlin
@Serializable
data class LoginInfoDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
)
```

- [ ] **Step 3: AuthRepository**

对齐 iOS：`googleLogin` → sendRequest → parse `UniversalResponse<LoginInfoDto>` → `tokenProvider.updateTokens`

- [ ] **Step 4: AuthUseCaseTest（mock repository）**

```kotlin
@Test
fun executeGoogleLogin_delegatesToRepository() = runTest {
    val repo = mockk<AuthRepository>()
    coEvery { repo.googleLogin(any()) } returns Unit
    AuthUseCase(repo).executeGoogleLogin("token")
    coVerify { repo.googleLogin("token") }
}
```

- [ ] **Step 5: AppDataWithAuthorizationService 骨架**

第一版 `authUseCase` 真实；其余 UseCase 接口 + `PlaceholderUseCase` throwing `NotImplementedError`。

- [ ] **Step 6: Run tests & assemble**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

- [ ] **Step 7: Commit**

```bash
git commit -am "feat(app-service): add auth repository, use cases, and service locator skeleton"
```

---

### Task 7: 拦截器链 + AppGraph

**Files:**
- Create: `app/.../service/interceptor/AuthInterceptor.kt`
- Create: `app/.../service/interceptor/RefreshTokenInterceptor.kt`
- Create: `app/.../service/interceptor/LogoutInterceptor.kt`
- Create: `app/.../service/network/SSLPinningValidator.kt`
- Create: `app/.../app/AppGraph.kt`
- Create: `app/.../app/LTApplication.kt`

- [ ] **Step 1: AuthInterceptor**

OkHttp Interceptor：若 `TokenProvider.accessToken` 非空，添加 `Authorization: Bearer {token}`。

- [ ] **Step 2: RefreshTokenInterceptor**

收到 HTTP 401 → 调用 `AppDataWithoutAuthorizationService.refreshTokenUseCase` → 重试原请求（对齐 iOS actor 防并发刷新逻辑，用 `Mutex`）。

- [ ] **Step 3: LogoutInterceptor**

刷新失败 → `SessionService.clear()` → 发送 `SessionExpired` 事件（SharedFlow）。

- [ ] **Step 4: AppGraph.build() 组装顺序**

严格按 spec §5：

```kotlin
object AppGraph {
    lateinit var current: AppGraph; private set

    val sessionService: SessionService
    val appDataService: AppDataWithAuthorizationService
    // ...

    companion object {
        fun build(environment: AppEnvironment) {
            // 1. SSL pinning builder
            // 2. bare ApiClient (no auth interceptors)
            // 3. SessionDataRepository + AppDataWithoutAuthorizationService
            // 4. SessionService
            // 5. auth/refresh/logout interceptors
            // 6. authenticated ApiClient + repositories
            // 7. AppDataWithAuthorizationService
            // 8. InjectionValues.register(FeatureToggle(...))
            current = /* built graph */
        }
    }
}
```

- [ ] **Step 5: LTApplication**

```kotlin
class LTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.build(AppEnvironment.DEV) // 首版 dev 环境
    }
}
```

- [ ] **Step 6: AndroidManifest 注册 `android:name=".app.LTApplication"`**

- [ ] **Step 7: Commit**

```bash
git commit -am "feat(app): add AppGraph manual DI and auth interceptor chain"
```

---

### Task 8: 导航骨架 — Coordinator + 双 Activity

**Files:**
- Create: `app/.../domain/coordinator/Route.kt`
- Create: `app/.../domain/coordinator/Coordinator.kt`
- Create: `app/.../app/prehome/PreHomeCoordinator.kt`
- Create: `app/.../app/home/HomeCoordinator.kt`
- Create: `app/.../app/home/UserHomeCoordinator.kt`
- Create: `app/.../app/SplashActivity.kt`
- Create: `app/.../app/prehome/PreHomeActivity.kt`
- Create: `app/.../app/home/HomeActivity.kt`
- Create: `app/src/main/res/navigation/nav_prehome.xml`
- Create: `app/src/main/res/layout/activity_splash.xml`
- Create: `app/src/main/res/layout/activity_prehome.xml`
- Create: `app/src/main/res/layout/activity_home.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Route sealed classes**

```kotlin
sealed interface Route
enum class PreHomeRoute : Route { LOGIN, SPLASH, ONBOARDING, WELCOME, FIRST_QUESTION }
enum class HomeRoute : Route { HOME, QUESTION_LIB, REFLECTION_DETAIL, ADD_NEW_ANSWER }
enum class UserRoute : Route { ABOUT_ME, PERSONA, QUESTION_OF_TODAY, REMINDER }
```

- [ ] **Step 2: SplashActivity（占位）**

```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = if (AppGraph.current.sessionService.hasValidToken()) {
            HomeActivity::class.java
        } else {
            PreHomeActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }
}
```

Manifest：`SplashActivity` 为 LAUNCHER。

- [ ] **Step 3: PreHomeActivity + nav_prehome.xml**

- startDestination = `signInFragment`
- 仅注册 `SignInFragment`（其余 destination 注释预留）

- [ ] **Step 4: HomeActivity 占位**

ViewPager2 + TabLayout，4 个 `PlaceholderTabFragment`（layout 仅 TextView 显示 Tab 名）。

- [ ] **Step 5: Coordinator 骨架类**

`PreHomeCoordinator(navController)` 实现 push/pop；`HomeCoordinator` / `UserHomeCoordinator` 空实现 + 注释。

- [ ] **Step 6: 编译运行到 Splash → PreHome 占位**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 7: Commit**

```bash
git commit -am "feat(app-nav): add dual-activity navigation and coordinator skeleton"
```

---

### Task 9: SignIn UI（完整实现）

**Files:**
- Create: `app/.../domain/signin/SignInViewModel.kt`
- Create: `app/.../domain/signin/SignInFragment.kt`
- Create: `app/src/main/res/layout/fragment_sign_in.xml`
- Create: `app/src/main/res/drawable/bg_google_sign_in_button.xml`
- Create: `app/src/main/res/drawable/ic_google.xml`（或 vector asset）
- Create: `app/src/main/res/anim/shake.xml`
- Modify: `app/build.gradle.kts`（Google Sign-In 需 `default_web_client_id` — 见 Step 0）

- [ ] **Step 0: Google Sign-In 配置**

1. Firebase/Google Cloud Console 创建 OAuth Android client（package `com.littlethingsandroidai` + SHA-1）
2. 在 `app/src/main/res/values/strings.xml` 添加 `default_web_client_id`
3. 实现前用 codegraph 读取 iOS `SignInView.swift` / `SignInViewModel.swift` 确认交互细节

- [ ] **Step 1: fragment_sign_in.xml**

结构对齐 iOS SignInView：

```xml
<!-- 根 ConstraintLayout，background=@color/lt_background -->
<!-- 中部：ImageView app icon 72dp x 68dp，TextView "the little things" 36sp -->
<!-- 底部：CheckBox 条款 + MaterialButton Google 登录 54dp 高，圆角 14dp -->
<!-- ProgressBar loading，默认 gone -->
```

- [ ] **Step 2: SignInViewModel**

```kotlin
class SignInViewModel(
    private val service: AppDataWithAuthorizationService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    var onLoginSuccess: (() -> Unit)? = null

    fun fetchData() { viewModelScope.launch { /* fetchOnboardingSentence 首版可 skip */ } }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                service.authUseCase.executeGoogleLogin(idToken)
                onLoginSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
```

- [ ] **Step 3: SignInFragment**

- ViewBinding 绑定布局
- Google Sign-In：`registerForActivityResult` + `GetSignInIntentRequest`
- 条款未勾选 → `view.startAnimation(shake)` + Snackbar
- 观察 `uiState` 控制 loading / error
- `onLoginSuccess` → 启动 `HomeActivity` + `requireActivity().finish()`

- [ ] **Step 4: 删除旧 XML MainActivity 入口**

移除或废弃 `activity_main.xml` 的 Hello World；`MainActivity.kt` 可删除，Manifest 仅保留 Splash/PreHome/Home。

- [ ] **Step 5: 手动测试 SignIn 流程**

Run app → Google 登录 → 确认进入 HomeActivity 占位页。

- [ ] **Step 6: Commit**

```bash
git commit -am "feat(signin): implement SignIn screen aligned with iOS"
```

---

### Task 10: Session 持久化与过期回归

**Files:**
- Modify: `SplashActivity.kt`（已有 token 跳 Home）
- Modify: `LogoutInterceptor` / `SessionExpired` 监听（PreHomeActivity 注册）

- [ ] **Step 1: 杀进程重启测试**

登录 → 杀 app → 冷启动应进 HomeActivity。

- [ ] **Step 2: Token 过期路径**

MockWebServer 或 dev 环境：401 → refresh → 成功重试；refresh 失败 → 清 token → Splash/SignIn。

- [ ] **Step 3: 补充 RefreshTokenInterceptor 单元测试（app 或 network 模块）**

- [ ] **Step 4: Commit**

```bash
git commit -am "fix(auth): handle session persistence and token refresh logout"
```

---

### Task 11: 交付验证清单

- [ ] **Step 1: 全量编译**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 单元测试**

Run: `./gradlew :core:network:testDebugUnitTest :app:testDebugUnitTest`

Expected: ALL PASS

- [ ] **Step 3: 对照 spec §12 交付标准逐项勾选**

- [ ] **Step 4: 更新 `LittleThingsAndroidAI/README.md`**

添加：模块结构、AppGraph 说明、构建命令、Google Sign-In 配置步骤。

- [ ] **Step 5: Commit**

```bash
git commit -am "docs(android): add README and verify v1 architecture delivery"
```

---

## Spec Coverage Checklist

| Spec 章节 | Task |
|-----------|------|
| §3 多模块 Gradle | Task 1 |
| §2.1 iOS Service 移植 Auth | Task 6 |
| §5 AppGraph 手动 DI | Task 7 |
| §6 双 Activity + Coordinator | Task 8 |
| §8 Network + 重试 | Task 3 |
| §9 Persistence | Task 4 |
| §10 Common FeatureToggle | Task 2 |
| §11 SignIn UI | Task 9 |
| §11 Home 占位 | Task 8 |
| §12 交付标准 | Task 10–11 |
| §13 测试 | Task 3, 6, 10 |

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-05-littlethings-android-architecture.md`.

**Two execution options:**

1. **Subagent-Driven（推荐）** — 每个 Task 派发独立 subagent，任务间 review，迭代快
2. **Inline Execution** — 在本会话按 Task 顺序直接实现，checkpoint 处暂停 review

**Which approach?**
