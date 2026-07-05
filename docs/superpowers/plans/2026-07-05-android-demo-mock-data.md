# Demo Flavor Offline Mock Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `demo` product flavor，通过 OkHttp `MockResponseInterceptor` 从本地 JSON 提供 Calendar 全链路数据，完全离线可演示。

**Architecture:** `prod` / `demo` 双 flavor；demo 的 `ApiClient` 仅挂 `MockResponseInterceptor`（永不 `chain.proceed()`）；Repository / UseCase 不变；SignIn 保留 UI，demo 包假登录写 session。

**Tech Stack:** Kotlin、AGP productFlavors、BuildConfig、OkHttp Interceptor、kotlinx-serialization、assets、Robolectric、JUnit4

**Spec:** [2026-07-05-android-demo-mock-data-design.md](../specs/2026-07-05-android-demo-mock-data-design.md)

---

## File Structure Overview

```
LittleThingsAndroidAI/app/
├── build.gradle.kts                              # MODIFY: flavors + buildConfig
├── src/main/kotlin/.../service/mock/
│   ├── MockAssetLoader.kt                        # CREATE
│   ├── AndroidMockAssetLoader.kt                 # CREATE
│   ├── MockCalendarViewFilter.kt                 # CREATE
│   ├── MockResponseInterceptor.kt                # CREATE
│   └── DemoFlavorConfig.kt                       # CREATE
├── src/main/kotlin/.../app/AppGraph.kt           # MODIFY: demo interceptor wiring
├── src/main/kotlin/.../domain/signin/
│   ├── SignInFragment.kt                         # MODIFY: USE_OFFLINE_MOCK branch
│   └── SignInViewModel.kt                        # MODIFY: demo tokens
├── src/demo/assets/mock/
│   ├── calendar/calendar_view.json               # CREATE
│   ├── reflection/questions_of_the_day.json      # CREATE
│   └── images/stamp_01.png                       # CREATE (minimal placeholder)
├── src/test/resources/mock/                      # CREATE (mirror demo JSON for unit tests)
│   ├── calendar/calendar_view.json
│   └── reflection/questions_of_the_day.json
└── src/test/kotlin/.../service/mock/
    ├── MockCalendarViewFilterTest.kt             # CREATE
    ├── ClasspathMockAssetLoader.kt               # CREATE (test helper)
    └── MockResponseInterceptorTest.kt            # CREATE

docs/app-startup-flow.md                           # MODIFY: demo variant 说明
```

---

### Task 1: Gradle — prod / demo flavors

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 启用 buildConfig 并添加 flavor dimension**

在 `android { }` 块内 `buildFeatures` 增加 `buildConfig = true`，并添加：

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

- [ ] **Step 2: 验证 variant 可解析**

Run:

```bash
cd LittleThingsAndroidAI && ./gradlew :app:tasks --group=build | grep -E 'Demo|Prod'
```

Expected: 出现 `assembleDemoDebug`、`assembleProdDebug` 等 task。

- [ ] **Step 3: 验证 prod 构建仍通过**

Run:

```bash
./gradlew :app:assembleProdDebug :app:testProdDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`

---

### Task 2: MockCalendarViewFilter（TDD）

**Files:**
- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/mock/MockCalendarViewFilter.kt`
- Create: `app/src/test/kotlin/com/littlethingsandroidai/service/mock/MockCalendarViewFilterTest.kt`

- [ ] **Step 1: 写失败测试**

`MockCalendarViewFilterTest.kt`:

```kotlin
package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCalendarViewFilterTest {
    private val days =
        listOf(
            CalendarDayDto(date = "2026-07-01", reflections = emptyList()),
            CalendarDayDto(date = "2026-07-15", reflections = emptyList()),
            CalendarDayDto(date = "2026-08-01", reflections = emptyList()),
        )

    @Test
    fun filter_includesDaysWithinRange() {
        val result =
            MockCalendarViewFilter.filter(
                days = days,
                start = LocalDate.of(2026, 7, 1),
                end = LocalDate.of(2026, 7, 31),
            )
        assertEquals(2, result.size)
        assertEquals("2026-07-01", result[0].date)
        assertEquals("2026-07-15", result[1].date)
    }

    @Test
    fun filter_returnsEmptyWhenNoOverlap() {
        val result =
            MockCalendarViewFilter.filter(
                days = days,
                start = LocalDate.of(2026, 9, 1),
                end = LocalDate.of(2026, 9, 30),
            )
        assertTrue(result.isEmpty())
    }
}
```

- [ ] **Step 2: 运行测试确认 FAIL**

Run:

```bash
./gradlew :app:testProdDebugUnitTest --tests "com.littlethingsandroidai.service.mock.MockCalendarViewFilterTest"
```

Expected: FAIL — `MockCalendarViewFilter` not found

- [ ] **Step 3: 实现 filter**

`MockCalendarViewFilter.kt`:

```kotlin
package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import java.time.LocalDate

object MockCalendarViewFilter {
    fun filter(
        days: List<CalendarDayDto>,
        start: LocalDate,
        end: LocalDate,
    ): List<CalendarDayDto> =
        days.filter { dto ->
            val day = LocalDate.parse(dto.date)
            !day.isBefore(start) && !day.isAfter(end)
        }
}
```

- [ ] **Step 4: 运行测试确认 PASS**

Run:

```bash
./gradlew :app:testProdDebugUnitTest --tests "com.littlethingsandroidai.service.mock.MockCalendarViewFilterTest"
```

Expected: BUILD SUCCESSFUL, 2 tests passed

---

### Task 3: MockAssetLoader

**Files:**
- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/mock/MockAssetLoader.kt`
- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/mock/AndroidMockAssetLoader.kt`
- Create: `app/src/test/kotlin/com/littlethingsandroidai/service/mock/ClasspathMockAssetLoader.kt`

- [ ] **Step 1: 定义接口与 Android 实现**

`MockAssetLoader.kt`:

```kotlin
package com.littlethingsandroidai.service.mock

interface MockAssetLoader {
    fun readText(relativePath: String): String
}
```

`AndroidMockAssetLoader.kt`:

```kotlin
package com.littlethingsandroidai.service.mock

import android.content.Context

class AndroidMockAssetLoader(
    context: Context,
) : MockAssetLoader {
    private val assets = context.applicationContext.assets

    override fun readText(relativePath: String): String =
        assets.open(relativePath).bufferedReader().use { it.readText() }
}
```

- [ ] **Step 2: 测试用 Classpath 实现**

`ClasspathMockAssetLoader.kt`:

```kotlin
package com.littlethingsandroidai.service.mock

class ClasspathMockAssetLoader : MockAssetLoader {
    override fun readText(relativePath: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(relativePath)) {
            "Missing test resource: $relativePath"
        }.bufferedReader().use { it.readText() }
}
```

---

### Task 4: Mock JSON fixtures

**Files:**
- Create: `app/src/test/resources/mock/calendar/calendar_view.json`
- Create: `app/src/test/resources/mock/reflection/questions_of_the_day.json`
- Create: `app/src/demo/assets/mock/calendar/calendar_view.json`（内容与 test 相同）
- Create: `app/src/demo/assets/mock/reflection/questions_of_the_day.json`（内容与 test 相同）
- Create: `app/src/demo/assets/mock/images/stamp_01.png`（24×24 占位 PNG，可复制 `res/drawable/ic_calendar_stamp_placeholder` 导出或任意小图）

- [ ] **Step 1: 创建 `calendar_view.json`**

`app/src/test/resources/mock/calendar/calendar_view.json`（demo 目录复制同文件）:

```json
{
  "success": true,
  "data": [
    {
      "date": "2026-07-01",
      "reflections": [
        {
          "id": "ans_demo_1",
          "content": "A quiet morning coffee.",
          "question": {
            "id": "q1",
            "title": "What warmed you today?",
            "category": { "name": "Life" }
          },
          "icon": {
            "id": "icon_demo_1",
            "url": "file:///android_asset/mock/images/stamp_01.png",
            "status": "GENERATED",
            "read_at": null
          }
        }
      ]
    },
    {
      "date": "2026-07-05",
      "reflections": [
        {
          "id": "ans_demo_2a",
          "content": "First stamp.",
          "question": { "id": "q2", "title": "Small win?", "category": { "name": "Wins" } },
          "icon": { "id": "icon_demo_2a", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null }
        },
        {
          "id": "ans_demo_2b",
          "content": "Second stamp.",
          "question": { "id": "q3", "title": "Another moment?", "category": { "name": "Wins" } },
          "icon": { "id": "icon_demo_2b", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null }
        }
      ]
    },
    {
      "date": "2026-07-10",
      "reflections": [
        { "id": "ans_demo_3a", "content": "One.", "question": { "id": "q4", "title": "Q4", "category": { "name": "Life" } }, "icon": { "id": "icon_demo_3a", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null } },
        { "id": "ans_demo_3b", "content": "Two.", "question": { "id": "q5", "title": "Q5", "category": { "name": "Life" } }, "icon": { "id": "icon_demo_3b", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null } },
        { "id": "ans_demo_3c", "content": "Three.", "question": { "id": "q6", "title": "Q6", "category": { "name": "Life" } }, "icon": { "id": "icon_demo_3c", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null } }
      ]
    },
    {
      "date": "2026-07-12",
      "reflections": [
        { "id": "ans_demo_4a", "content": "A", "question": { "id": "q7", "title": "Q7", "category": { "name": "Life" } }, "icon": { "id": "icon_demo_4a", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null } },
        { "id": "ans_demo_4b", "content": "B", "question": { "id": "q8", "title": "Q8", "category": { "name": "Life" } }, "icon": { "id": "icon_demo_4b", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null } },
        { "id": "ans_demo_4c", "content": "C", "question": { "id": "q9", "title": "Q9", "category": { "name": "Life" } }, "icon": { "id": "icon_demo_4c", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null } },
        { "id": "ans_demo_4d", "content": "D", "question": { "id": "q10", "title": "Q10", "category": { "name": "Life" } }, "icon": { "id": "icon_demo_4d", "url": "file:///android_asset/mock/images/stamp_01.png", "status": "GENERATED", "read_at": null } }
      ]
    }
  ]
}
```

- [ ] **Step 2: 创建 `questions_of_the_day.json`**

```json
{
  "success": true,
  "data": [
    { "id": "qod_1", "title": "What warmed you today?", "category": { "name": "Life" } },
    { "id": "qod_2", "title": "What are you grateful for?", "category": { "name": "Gratitude" } },
    { "id": "qod_3", "title": "Which moment made you pause?", "category": { "name": "Present" } }
  ]
}
```

- [ ] **Step 3: 复制到 demo assets 并添加 stamp_01.png**

```bash
mkdir -p app/src/demo/assets/mock/calendar app/src/demo/assets/mock/reflection app/src/demo/assets/mock/images
cp app/src/test/resources/mock/calendar/calendar_view.json app/src/demo/assets/mock/calendar/
cp app/src/test/resources/mock/reflection/questions_of_the_day.json app/src/demo/assets/mock/reflection/
# stamp_01.png: 添加任意小 PNG（≥1 张供 Coil 离线加载）
```

---

### Task 5: MockResponseInterceptor（TDD）

**Files:**
- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/mock/MockResponseInterceptor.kt`
- Create: `app/src/test/kotlin/com/littlethingsandroidai/service/mock/MockResponseInterceptorTest.kt`

- [ ] **Step 1: 写失败测试**

`MockResponseInterceptorTest.kt`:

```kotlin
package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.service.reflection.repository.DefaultReflectionRepository
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCase
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockResponseInterceptorTest {
    private val assetLoader = ClasspathMockAssetLoader()
    private val interceptor = MockResponseInterceptor(assetLoader)

    @Test
    fun intercept_calendarView_filtersByQueryParams() = runBlocking {
        val client = apiClientWithMock()
        val useCase = CalendarReflectionsUseCase(DefaultReflectionRepository(client))
        val result =
            useCase.execute(
                startMonth = LocalDate.of(2026, 7, 1),
                endMonth = LocalDate.of(2026, 7, 31),
            )
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.day.monthValue == 7 })
    }

    @Test
    fun intercept_neverCallsProceed() {
        var proceedCalled = false
        val chain =
            object : okhttp3.Interceptor.Chain by OkHttpClient().newCall(
                Request.Builder().url("https://example.com/api/calendar-view?start=2026-07-01&end=2026-07-31").build(),
            ).let { throw UnsupportedOperationException() } {
                // simplified: verify response is synthetic
            }
        val request =
            Request.Builder()
                .url("https://things.dvacode.tech/api/calendar-view?start=2026-07-01&end=2026-07-31")
                .get()
                .build()
        val response = interceptor.intercept(
            object : okhttp3.Interceptor.Chain {
                override fun request() = request
                override fun proceed(req: Request): okhttp3.Response {
                    proceedCalled = true
                    throw AssertionError("Must not proceed to network")
                }
                override fun connection() = null
                override fun call() = throw UnsupportedOperationException()
                override fun connectTimeoutMillis() = 0
                override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
                override fun readTimeoutMillis() = 0
                override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
                override fun writeTimeoutMillis() = 0
                override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            },
        )
        assertEquals(200, response.code)
        assertTrue(response.body!!.string().contains("\"success\":true"))
        assertTrue(!proceedCalled)
    }

    @Test
    fun intercept_markIconRead_returnsSuccessEnvelope() {
        val request =
            Request.Builder()
                .url("https://things.dvacode.tech/api/answers/icons/icon_demo_1/read")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .build()
        val response = interceptor.intercept(
            object : okhttp3.Interceptor.Chain {
                override fun request() = request
                override fun proceed(req: Request) = throw AssertionError("no network")
                override fun connection() = null
                override fun call() = throw UnsupportedOperationException()
                override fun connectTimeoutMillis() = 0
                override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
                override fun readTimeoutMillis() = 0
                override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
                override fun writeTimeoutMillis() = 0
                override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            },
        )
        assertEquals(200, response.code)
        assertTrue(response.body!!.string().contains("icon_demo_1"))
    }

    private fun apiClientWithMock(): ApiClient =
        ApiClient(
            environment = AppEnvironment.DEV,
            interceptors = listOf(interceptor),
        )
}
```

- [ ] **Step 2: 运行测试确认 FAIL**

Run:

```bash
./gradlew :app:testProdDebugUnitTest --tests "com.littlethingsandroidai.service.mock.MockResponseInterceptorTest"
```

Expected: FAIL — class not found

- [ ] **Step 3: 实现 MockResponseInterceptor**

`MockResponseInterceptor.kt`:

```kotlin
package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.core.common.log.LTLog
import com.littlethingsandroidai.core.network.UniversalResponse
import com.littlethingsandroidai.service.icon.dto.IconReadResultDto
import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import com.littlethingsandroidai.service.reflection.dto.QuestionDto
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockResponseInterceptor(
    private val assetLoader: MockAssetLoader,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        val body =
            when {
                method == "GET" && path == PATH_CALENDAR_VIEW -> handleCalendarView(request)
                method == "GET" && path == PATH_QUESTIONS_OF_THE_DAY ->
                    assetLoader.readText("mock/reflection/questions_of_the_day.json")
                method == "POST" && path.startsWith(PATH_MARK_READ_PREFIX) && path.endsWith("/read") ->
                    handleMarkRead(path)
                else -> {
                    LTLog.w(TAG, "Unmocked request: $method $path")
                    """{"success":false,"code":404,"data":{},"message":"Not mocked"}"""
                }
            }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(if (body.contains("\"success\":false")) 404 else 200)
            .message("OK")
            .body(body.toResponseBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun handleCalendarView(request: okhttp3.Request): String {
        val start = request.url.queryParameter("start")?.let(LocalDate::parse)
        val end = request.url.queryParameter("end")?.let(LocalDate::parse)
        require(start != null && end != null) { "calendar-view requires start and end" }

        val raw = assetLoader.readText("mock/calendar/calendar_view.json")
        val envelope = json.decodeFromString<UniversalResponse<List<CalendarDayDto>>>(raw)
        val filtered = MockCalendarViewFilter.filter(envelope.data, start, end)
        val filteredEnvelope = UniversalResponse(success = true, data = filtered)
        return json.encodeToString(
            UniversalResponse.serializer(ListSerializer(CalendarDayDto.serializer())),
            filteredEnvelope,
        )
    }

    private fun handleMarkRead(path: String): String {
        val iconId = path.removePrefix(PATH_MARK_READ_PREFIX).removeSuffix("/read")
        val readAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val envelope =
            UniversalResponse(
                success = true,
                data = IconReadResultDto(id = iconId, readAt = readAt),
            )
        return json.encodeToString(
            UniversalResponse.serializer(IconReadResultDto.serializer()),
            envelope,
        )
    }

    private companion object {
        const val TAG = "MockResponseInterceptor"
        const val PATH_CALENDAR_VIEW = "/api/calendar-view"
        const val PATH_QUESTIONS_OF_THE_DAY = "/api/questions-of-the-day"
        const val PATH_MARK_READ_PREFIX = "/api/answers/icons/"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
```

- [ ] **Step 4: 运行测试确认 PASS**

Run:

```bash
./gradlew :app:testProdDebugUnitTest --tests "com.littlethingsandroidai.service.mock.MockResponseInterceptorTest"
```

Expected: 3 tests passed

---

### Task 6: AppGraph demo wiring

**Files:**
- Modify: `app/src/main/kotlin/com/littlethingsandroidai/app/AppGraph.kt`

- [ ] **Step 1: 按 BuildConfig 分支装配 ApiClient**

在 `AppGraph.build` 顶部添加 import：

```kotlin
import com.littlethingsandroidai.BuildConfig
import com.littlethingsandroidai.service.mock.AndroidMockAssetLoader
import com.littlethingsandroidai.service.mock.MockResponseInterceptor
```

在创建 `bareApiClient` 之前：

```kotlin
val mockInterceptor = MockResponseInterceptor(AndroidMockAssetLoader(appContext))

val bareApiClient: ApiClient
val authenticatedApiClient: ApiClient
val authRepository: AuthRepository

if (BuildConfig.USE_OFFLINE_MOCK) {
    bareApiClient = ApiClient(environment = environment, interceptors = listOf(mockInterceptor))
    authenticatedApiClient = ApiClient(environment = environment, interceptors = listOf(mockInterceptor))
    authRepository =
        DefaultAuthRepository(
            apiClient = authenticatedApiClient,
            tokenProvider = sessionService,
        )
} else {
    bareApiClient = ApiClient(environment = environment)
    // ... existing authInterceptor / refresh / logout chain unchanged ...
}
```

demo 分支 **不要** 创建 `RefreshTokenInterceptor` 等（无网络 refresh 无意义）。

- [ ] **Step 2: 验证 demo 构建**

Run:

```bash
./gradlew :app:assembleDemoDebug
```

Expected: BUILD SUCCESSFUL

---

### Task 7: Demo 假登录

**Files:**
- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/mock/DemoFlavorConfig.kt`
- Modify: `app/src/main/kotlin/com/littlethingsandroidai/domain/signin/SignInViewModel.kt`
- Modify: `app/src/main/kotlin/com/littlethingsandroidai/domain/signin/SignInFragment.kt`

- [ ] **Step 1: DemoFlavorConfig**

```kotlin
package com.littlethingsandroidai.service.mock

object DemoFlavorConfig {
    const val DEMO_ACCESS_TOKEN = "demo-offline-access-token"
    const val DEMO_REFRESH_TOKEN = "demo-offline-refresh-token"
}
```

- [ ] **Step 2: SignInViewModel 支持 demo token**

```kotlin
import com.littlethingsandroidai.BuildConfig
import com.littlethingsandroidai.service.mock.DemoFlavorConfig

fun signInWithMockGoogle() {
    viewModelScope.launch {
        _loading.value = true
        _errorMessage.value = null

        val (access, refresh) =
            if (BuildConfig.USE_OFFLINE_MOCK) {
                DemoFlavorConfig.DEMO_ACCESS_TOKEN to DemoFlavorConfig.DEMO_REFRESH_TOKEN
            } else {
                SignInDevConfig.MOCK_ACCESS_TOKEN to SignInDevConfig.MOCK_REFRESH_TOKEN
            }
        sessionService.updateTokens(accessToken = access, refreshToken = refresh)
        onLoginSuccess()

        _loading.value = false
    }
}
```

- [ ] **Step 3: SignInFragment 优先 demo 分支**

```kotlin
import com.littlethingsandroidai.BuildConfig

// googleSignInButton click listener 内，terms 校验之后：
when {
    BuildConfig.USE_OFFLINE_MOCK -> viewModel.signInWithMockGoogle()
    SignInDevConfig.MOCK_GOOGLE_SIGN_IN -> viewModel.signInWithMockGoogle()
    else -> signInLauncher.launch(googleSignInClient.signInIntent)
}
```

- [ ] **Step 4: 手动验证（飞行模式）**

Run:

```bash
./gradlew :app:installDemoDebug
```

1. 开启飞行模式
2. 勾选 terms → Continue with Google
3. 进入 Calendar，应看到 7 月 stamp 与 TodayQuestion 浮层

---

### Task 8: 文档与全量测试

**Files:**
- Modify: `docs/app-startup-flow.md`

- [ ] **Step 1: 在 app-startup-flow.md 增加 Demo 构建说明**

补充：

```markdown
### Demo 离线包（demoDebug）

- Build Variant: `demoDebug`
- `BuildConfig.USE_OFFLINE_MOCK = true`
- 安装包 ID: `com.littlethingsandroidai.demo`
- 数据来自 `app/src/demo/assets/mock/`，OkHttp `MockResponseInterceptor` 拦截，零 HTTP
- 命令: `./gradlew :app:installDemoDebug`
```

- [ ] **Step 2: 全量单元测试**

Run:

```bash
./gradlew :app:testProdDebugUnitTest :app:testDemoDebugUnitTest
```

Expected: 全部 PASS

- [ ] **Step 3: 双 flavor 构建**

Run:

```bash
./gradlew :app:assembleProdDebug :app:assembleDemoDebug
```

Expected: BUILD SUCCESSFUL

---

## Spec Coverage Checklist

| Spec § | Task |
|--------|------|
| §3 Gradle flavors | Task 1 |
| §4 Mock assets | Task 4 |
| §4.2 calendar filter | Task 2 |
| §5 MockResponseInterceptor | Task 5 |
| §6 AppGraph demo wiring | Task 6 |
| §7 假登录 | Task 7 |
| §10 测试 | Task 2, 5, 8 |
| §11 验收 | Task 7 Step 4, Task 8 |

---

## 交付检查清单

- [ ] `prod` / `demo` flavor 构建成功
- [ ] demo 飞行模式下 Calendar 全链路可用
- [ ] `MockResponseInterceptor` 单测证明不 `proceed()`
- [ ] prod 行为与现网一致（`USE_OFFLINE_MOCK = false`）
- [ ] `./gradlew :app:testProdDebugUnitTest` 全绿
