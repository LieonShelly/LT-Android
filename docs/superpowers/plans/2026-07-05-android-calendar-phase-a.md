# Calendar Phase A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Home Tab 0 替换为可用的 Calendar 页：Service 层对接 `GET /api/calendar-view`，ViewModel 生成月历数据，UI 展示 Header + Weekday + 横向切月 ViewPager2 + 7 列 Grid + Footer（stamp 用占位 icon，详情留 Phase C）。

**Architecture:** 方案 1 — `CalendarFragment` + `CalendarViewModel`；`ViewPager2` 横向 paging 切月；每月一页内 `RecyclerView` + `GridLayoutManager(7)`；Reflection 层镜像 iOS `ReflectionRepository` / `CalendarReflectionsUseCase`。

**Tech Stack:** Kotlin 2.2、AGP 9.1、XML + ViewBinding、ViewPager2、RecyclerView、kotlinx-serialization、MockWebServer、Robolectric

**Spec:** [2026-07-05-android-calendar-design.md](../specs/2026-07-05-android-calendar-design.md)

**Figma:** [Calendar 屏](https://www.figma.com/design/b0c3FPp8qfuqyTnBo7rHfb/The-Little-Things--NEW-?node-id=4824-4705&m=dev)

**iOS 对照（实现前 codegraph 检索）：**

| 领域 | iOS 路径 |
|------|----------|
| Calendar UI | `Domain/Calendar/CalendarView.swift` |
| ViewModel | `Domain/Calendar/CalendarViewModel.swift` |
| Models | `Domain/Calendar/CalendarDay.swift` |
| API | `Service/Reflection/Request/ReflectionRequest.swift` |
| Repository | `Service/Reflection/Repository/ReflectionRepository.swift` |

**后续 Phase：** B（MonthPicker + refresh + monthLock）、C（Coil + 详情 + markRead）、D（TodayQuestion）

---

## File Structure Overview

```
LittleThingsAndroidAI/app/src/main/kotlin/com/littlethingsandroidai/
├── domain/calendar/
│   ├── CalendarFragment.kt                    # CREATE
│   ├── CalendarViewModel.kt                   # CREATE
│   ├── CalendarMonthPagerAdapter.kt           # CREATE
│   ├── CalendarDayGridAdapter.kt              # CREATE
│   └── model/
│       ├── CalendarDay.kt                     # CREATE
│       ├── CalendarMonth.kt                   # CREATE
│       └── WeekDay.kt                         # CREATE
├── domain/home/
│   └── HomeTabAdapter.kt                      # MODIFY: CalendarFragment @0
├── service/reflection/
│   ├── request/ReflectionRequest.kt           # CREATE
│   ├── dto/CalendarDayDto.kt                  # CREATE (+ nested DTOs)
│   ├── repository/ReflectionRepository.kt     # CREATE
│   └── usecase/CalendarReflectionsUseCase.kt  # CREATE
├── service/
│   └── AppDataWithAuthorizationService.kt     # MODIFY
├── app/
│   └── AppGraph.kt                            # MODIFY
└── res/
    ├── layout/fragment_calendar.xml           # CREATE
    ├── layout/view_calendar_header.xml        # CREATE
    ├── layout/view_calendar_weekday_row.xml   # CREATE
    ├── layout/item_calendar_month_page.xml    # CREATE
    ├── layout/item_calendar_day_cell.xml      # CREATE
    ├── drawable/bg_calendar_grid_dash.xml     # CREATE
    ├── drawable/ic_calendar_stamp_placeholder.xml  # CREATE
    └── values/strings.xml                     # MODIFY

app/src/test/kotlin/com/littlethingsandroidai/
├── domain/calendar/CalendarViewModelTest.kt   # CREATE
└── service/reflection/CalendarReflectionsUseCaseTest.kt  # CREATE
```

---

### Task 1: Domain Models

**Files:**

- Create: `app/src/main/kotlin/com/littlethingsandroidai/domain/calendar/model/WeekDay.kt`
- Create: `app/src/main/kotlin/com/littlethingsandroidai/domain/calendar/model/CalendarDay.kt`
- Create: `app/src/main/kotlin/com/littlethingsandroidai/domain/calendar/model/CalendarMonth.kt`

- [ ] **Step 1: 创建 `WeekDay.kt`**

```kotlin
package com.littlethingsandroidai.domain.calendar.model

data class WeekDay(val title: String)
```

- [ ] **Step 2: 创建 `CalendarDay.kt`**

```kotlin
package com.littlethingsandroidai.domain.calendar.model

import java.time.LocalDate
import java.util.UUID

enum class DayType { PAST, TODAY, FUTURE }

data class CalendarDay(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    var isAbsent: Boolean,
    var reflections: DayReflections? = null,
) {
    val dayType: DayType
        get() {
            val today = LocalDate.now()
            return when {
                date.isBefore(today) -> DayType.PAST
                date.isEqual(today) -> DayType.TODAY
                else -> DayType.FUTURE
            }
        }

    fun copyWith(reflections: DayReflections): CalendarDay =
        copy(reflections = reflections, isAbsent = false)
}

data class DayReflections(
    val day: LocalDate,
    val reflections: List<Answer>,
)

data class Answer(
    val id: String,
    val content: String,
    val question: Question?,
    val icon: Icon?,
)

data class Icon(
    val id: String?,
    val url: String?,
    val status: String,
    val readAt: String?,
)

data class Question(
    val id: String,
    val title: String,
    val category: Category?,
)

data class Category(
    val id: String,
    val name: String,
)
```

- [ ] **Step 3: 创建 `CalendarMonth.kt`**

```kotlin
package com.littlethingsandroidai.domain.calendar.model

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class MonthItemType { NORMAL, YEAR_PLACEHOLDER }

data class CalendarMonth(
    val id: String = UUID.randomUUID().toString(),
    val date: YearMonth,
    var days: List<CalendarDay> = emptyList(),
    var iconCount: Int = 0,
    var moreDaysToGo: Int = 0,
    val itemType: MonthItemType = MonthItemType.NORMAL,
) {
    val isFuture: Boolean
        get() = date.atDay(1).isAfter(LocalDate.now())

    val isValidMonth: Boolean
        get() = itemType == MonthItemType.NORMAL
}
```

- [ ] **Step 4: 验证编译**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:compileDebugKotlin`  
Expected: BUILD SUCCESSFUL

---

### Task 2: Reflection DTOs + Request

**Files:**

- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/reflection/dto/CalendarDayDto.kt`
- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/reflection/request/ReflectionRequest.kt`

- [ ] **Step 1: 创建 DTO（kotlinx.serialization）**

```kotlin
package com.littlethingsandroidai.service.reflection.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarDayDto(
    val date: String,
    val reflections: List<AnswerDto> = emptyList(),
)

@Serializable
data class AnswerDto(
    val id: String,
    val content: String,
    @SerialName("created_ymd") val createdYmd: String? = null,
    val question: QuestionDto? = null,
    val icon: IconDto? = null,
)

@Serializable
data class QuestionDto(
    val id: String,
    val title: String,
    val category: CategoryDto? = null,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
)

@Serializable
data class IconDto(
    val id: String? = null,
    val url: String? = null,
    val status: String? = null,
    @SerialName("read_at") val readAt: String? = null,
)
```

- [ ] **Step 2: 创建 `ReflectionRequest.kt`（Phase A 仅需 calendar）**

```kotlin
package com.littlethingsandroidai.service.reflection.request

import com.littlethingsandroidai.core.network.ApiRequest
import com.littlethingsandroidai.core.network.EndPoint
import com.littlethingsandroidai.core.network.HttpMethod
import com.littlethingsandroidai.core.network.HttpPayload
import com.littlethingsandroidai.service.DefaultEndPoint

sealed class ReflectionRequest : ApiRequest {

    data class Calendar(
        private val startDate: String,
        private val endDate: String,
    ) : ReflectionRequest() {
        override val endPoint: EndPoint = DefaultEndPoint.baseUrl(path = "/api/calendar-view")
        override val method: HttpMethod = HttpMethod.GET
        override val payload: HttpPayload =
            HttpPayload.UrlEncoding(
                params = listOf("start" to startDate, "end" to endDate),
            )
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:compileDebugKotlin`  
Expected: BUILD SUCCESSFUL

---

### Task 3: ReflectionRepository + UseCase

**Files:**

- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/reflection/repository/ReflectionRepository.kt`
- Create: `app/src/main/kotlin/com/littlethingsandroidai/service/reflection/usecase/CalendarReflectionsUseCase.kt`
- Test: `app/src/test/kotlin/com/littlethingsandroidai/service/reflection/CalendarReflectionsUseCaseTest.kt`

- [ ] **Step 1: 写 failing test**

```kotlin
package com.littlethingsandroidai.service.reflection

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.service.reflection.repository.DefaultReflectionRepository
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCase
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalendarReflectionsUseCaseTest {
    private lateinit var server: MockWebServer
    private lateinit var useCase: CalendarReflectionsUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiClient = ApiClient(
            environment = AppEnvironment.DEV,
            baseUrlOverride = server.url("/").toString(),
        )
        useCase = CalendarReflectionsUseCase(DefaultReflectionRepository(apiClient))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun execute_parsesCalendarViewResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "success": true,
                      "data": [
                        {
                          "date": "2026-07-05",
                          "reflections": [
                            {
                              "id": "ans1",
                              "content": "A warm moment.",
                              "question": {
                                "id": "q1",
                                "title": "What warmed you today?",
                                "category": { "id": "c1", "name": "Life" }
                              },
                              "icon": {
                                "id": "icon1",
                                "url": "https://example.com/icon.webp",
                                "status": "GENERATED",
                                "read_at": null
                              }
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = useCase.execute(
            startMonth = LocalDate.of(2026, 7, 1),
            endMonth = LocalDate.of(2026, 7, 31),
        )

        assertEquals(1, result.size)
        assertEquals(LocalDate.of(2026, 7, 5), result[0].day)
        assertEquals("ans1", result[0].reflections[0].id)
        assertEquals("What warmed you today?", result[0].reflections[0].question?.title)
    }
}
```

- [ ] **Step 2: 运行 test，确认 FAIL**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:testDebugUnitTest --tests "com.littlethingsandroidai.service.reflection.CalendarReflectionsUseCaseTest"`  
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 Repository + UseCase**

`ReflectionRepository.kt`:

```kotlin
package com.littlethingsandroidai.service.reflection.repository

import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.core.network.UniversalResponse
import com.littlethingsandroidai.domain.calendar.model.Answer
import com.littlethingsandroidai.domain.calendar.model.Category
import com.littlethingsandroidai.domain.calendar.model.DayReflections
import com.littlethingsandroidai.domain.calendar.model.Icon
import com.littlethingsandroidai.domain.calendar.model.Question
import com.littlethingsandroidai.service.reflection.dto.AnswerDto
import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import com.littlethingsandroidai.service.reflection.request.ReflectionRequest
import java.time.LocalDate

interface ReflectionRepository {
    suspend fun fetchCalendarReflections(startMonth: LocalDate, endMonth: LocalDate): List<DayReflections>
}

class DefaultReflectionRepository(
    private val apiClient: ApiClient,
) : ReflectionRepository {
    override suspend fun fetchCalendarReflections(
        startMonth: LocalDate,
        endMonth: LocalDate,
    ): List<DayReflections> {
        val request = ReflectionRequest.Calendar(
            startDate = startMonth.toString(),
            endDate = endMonth.toString(),
        )
        val response = apiClient.sendRequest(request)
        val parsed: UniversalResponse<List<CalendarDayDto>> = response.parseJson()
        return parsed.data.map { dto ->
            DayReflections(
                day = LocalDate.parse(dto.date),
                reflections = dto.reflections.map(::mapAnswer),
            )
        }
    }

    private fun mapAnswer(dto: AnswerDto): Answer =
        Answer(
            id = dto.id,
            content = dto.content,
            question = dto.question?.let {
                Question(
                    id = it.id,
                    title = it.title,
                    category = it.category?.let { c -> Category(id = c.id, name = c.name) },
                )
            },
            icon = dto.icon?.let {
                Icon(
                    id = it.id,
                    url = it.url,
                    status = it.status.orEmpty(),
                    readAt = it.readAt,
                )
            },
        )
}
```

`CalendarReflectionsUseCase.kt`:

```kotlin
package com.littlethingsandroidai.service.reflection.usecase

import com.littlethingsandroidai.domain.calendar.model.DayReflections
import com.littlethingsandroidai.service.reflection.repository.ReflectionRepository
import java.time.LocalDate

interface CalendarReflectionsUseCaseType {
    suspend fun execute(startMonth: LocalDate, endMonth: LocalDate): List<DayReflections>
}

class CalendarReflectionsUseCase(
    private val repository: ReflectionRepository,
) : CalendarReflectionsUseCaseType {
    override suspend fun execute(startMonth: LocalDate, endMonth: LocalDate): List<DayReflections> =
        repository.fetchCalendarReflections(startMonth, endMonth)
}
```

- [ ] **Step 4: 运行 test，确认 PASS**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:testDebugUnitTest --tests "com.littlethingsandroidai.service.reflection.CalendarReflectionsUseCaseTest"`  
Expected: BUILD SUCCESSFUL, 1 test passed

---

### Task 4: AppGraph + AppDataWithAuthorizationService

**Files:**

- Modify: `app/src/main/kotlin/com/littlethingsandroidai/app/AppGraph.kt`
- Modify: `app/src/main/kotlin/com/littlethingsandroidai/service/AppDataWithAuthorizationService.kt`

- [ ] **Step 1: 扩展 `AppDataWithAuthorizationServiceful`**

```kotlin
import com.littlethingsandroidai.service.reflection.repository.DefaultReflectionRepository
import com.littlethingsandroidai.service.reflection.repository.ReflectionRepository
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCase
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCaseType

interface AppDataWithAuthorizationServiceful {
    val authUseCase: AuthUseCaseType
    val calendarReflectionsUseCase: CalendarReflectionsUseCaseType
    // ... existing placeholders
}

class AppDataWithAuthorizationService(
    private val authRepository: AuthRepository,
    private val reflectionRepository: ReflectionRepository,
) : AppDataWithAuthorizationServiceful {

    override val calendarReflectionsUseCase: CalendarReflectionsUseCaseType by lazy {
        CalendarReflectionsUseCase(repository = reflectionRepository)
    }
    // ... existing
}
```

- [ ] **Step 2: 更新 `AppGraph.build()`**

```kotlin
val reflectionRepository = DefaultReflectionRepository(apiClient = authenticatedApiClient)
val appDataWithAuthorizationService = AppDataWithAuthorizationService(
    authRepository = authRepository,
    reflectionRepository = reflectionRepository,
)
```

- [ ] **Step 3: 验证编译**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:assembleDebug`  
Expected: BUILD SUCCESSFUL

---

### Task 5: CalendarViewModel + Unit Tests

**Files:**

- Create: `app/src/main/kotlin/com/littlethingsandroidai/domain/calendar/CalendarViewModel.kt`
- Test: `app/src/test/kotlin/com/littlethingsandroidai/domain/calendar/CalendarViewModelTest.kt`

- [ ] **Step 1: 写 failing test — `generateMonths` 包含当前月**

```kotlin
package com.littlethingsandroidai.domain.calendar

import com.littlethingsandroidai.domain.calendar.model.MonthItemType
import com.littlethingsandroidai.service.AppDataWithAuthorizationServiceful
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCaseType
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CalendarViewModelTest {
    private val calendarUseCase: CalendarReflectionsUseCaseType = mock()
    private val service: AppDataWithAuthorizationServiceful = mock {
        whenever(it.calendarReflectionsUseCase).thenReturn(calendarUseCase)
    }

    @Test
    fun generateMonths_includesCurrentMonth() = runTest {
        val viewModel = CalendarViewModel(service)
        viewModel.generateMonths()

        val current = YearMonth.now()
        val validMonths = viewModel.months.value.filter { it.isValidMonth }
        assertTrue(validMonths.any { it.date == current })
    }

    @Test
    fun generateMonths_startsFrom2025January() = runTest {
        val viewModel = CalendarViewModel(service)
        viewModel.generateMonths()

        val validMonths = viewModel.months.value.filter { it.isValidMonth }
        assertTrue(validMonths.first().date >= YearMonth.of(2025, 1))
    }
}
```

- [ ] **Step 2: 运行 test，确认 FAIL**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:testDebugUnitTest --tests "com.littlethingsandroidai.domain.calendar.CalendarViewModelTest"`  
Expected: FAIL

- [ ] **Step 3: 实现 `CalendarViewModel`（核心逻辑镜像 iOS）**

要点：

- `StateFlow<List<CalendarMonth>> months`
- `StateFlow<CalendarMonth?> currentMonth`
- `val weekdays = listOf(WeekDay("S")..("S"))`
- `generateMonths()`：`2025-01-01` 至 `now + 1 month`；每年 1 月前插 `YEAR_PLACEHOLDER`
- `generateSingleMonthData(yearMonth)`：weekday offset + leading/trailing days
- `fetchData()`：调用 useCase，merge reflections，重算 `isAbsent` / `iconCount` / `moreDaysToGo`
- `scrollToCurrentMonth()`：设置 `currentMonthIndex` StateFlow
- `onMonthSelected(index: Int)`：映射 valid month index → 更新 currentMonth → launch fetchData

使用 `viewModelScope` + `StateFlow`；构造函数注入 `AppDataWithAuthorizationServiceful`。

- [ ] **Step 4: 运行 test，确认 PASS**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:testDebugUnitTest --tests "com.littlethingsandroidai.domain.calendar.CalendarViewModelTest"`  
Expected: PASS

---

### Task 6: Calendar 布局 XML

**Files:**

- Create: `app/src/main/res/layout/fragment_calendar.xml`
- Create: `app/src/main/res/layout/view_calendar_header.xml`
- Create: `app/src/main/res/layout/view_calendar_weekday_row.xml`
- Create: `app/src/main/res/layout/item_calendar_month_page.xml`
- Create: `app/src/main/res/layout/item_calendar_day_cell.xml`
- Create: `app/src/main/res/drawable/bg_calendar_grid_dash.xml`
- Create: `app/src/main/res/drawable/ic_calendar_stamp_placeholder.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: `fragment_calendar.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/lt_oat">

    <include
        android:id="@+id/calendarHeader"
        layout="@layout/view_calendar_header"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <include
        android:id="@+id/weekdayRow"
        layout="@layout/view_calendar_weekday_row"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/calendarHeader" />

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/monthViewPager"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="8dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/weekdayRow" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Header — 月份 TextView `@+id/monthTitle`、年份 `@+id/yearTitle`、Today `@+id/todayBadge`**

- [ ] **Step 3: Weekday — 7 个 TextView 或 LinearLayout 均分**

- [ ] **Step 4: `item_calendar_month_page.xml` — NestedScrollView + RecyclerView `@+id/dayGrid` + Footer `@+id/monthFooter`**

- [ ] **Step 5: `item_calendar_day_cell.xml` — 日期 `@+id/dayNumber`、stamp 占位 `@+id/stampPlaceholder`、dash `@+id/absentDash`（visibility gone 默认）**

- [ ] **Step 6: strings.xml 追加 Footer 模板**

```xml
<string name="calendar_footer_current">%1$d stamps collected so far\n take your time with the %2$d days ahead</string>
<string name="calendar_footer_current_last_day">%1$d stamps collected so far\n take your time today</string>
<string name="calendar_footer_other">%1$d stamps collected this month</string>
```

- [ ] **Step 7: 验证资源合并**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:mergeDebugResources`  
Expected: BUILD SUCCESSFUL

---

### Task 7: Adapters（MonthPager + DayGrid）

**Files:**

- Create: `app/src/main/kotlin/com/littlethingsandroidai/domain/calendar/CalendarMonthPagerAdapter.kt`
- Create: `app/src/main/kotlin/com/littlethingsandroidai/domain/calendar/CalendarDayGridAdapter.kt`

- [ ] **Step 1: `CalendarDayGridAdapter`**

- `GridLayoutManager` span=7 由 Fragment/ViewHolder 设置
- bind：日期文字；非当月灰色；`isAbsent` 显示 dash；有 reflections 显示 stamp 占位 ImageView
- Phase A：stamp 统一用 `ic_calendar_stamp_placeholder`

- [ ] **Step 2: `CalendarMonthPagerAdapter`**

- 输入：`List<CalendarMonth>`（仅 `isValidMonth`）
- 每页 inflate `item_calendar_month_page.xml`，设置 Grid + Footer 文案
- Footer 逻辑镜像 iOS `footerView`

- [ ] **Step 3: 验证编译**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:compileDebugKotlin`  
Expected: BUILD SUCCESSFUL

---

### Task 8: CalendarFragment + HomeTabAdapter

**Files:**

- Create: `app/src/main/kotlin/com/littlethingsandroidai/domain/calendar/CalendarFragment.kt`
- Modify: `app/src/main/kotlin/com/littlethingsandroidai/domain/home/HomeTabAdapter.kt`

- [ ] **Step 1: 修改 `HomeTabAdapter`**

```kotlin
import androidx.fragment.app.Fragment
import com.littlethingsandroidai.domain.calendar.CalendarFragment

override fun createFragment(position: Int): Fragment =
    when (tabs[position]) {
        HomeRoute.CALENDAR -> CalendarFragment()
        else -> PlaceholderTabFragment.newInstance(
            tabName = tabs[position].name.lowercase().replaceFirstChar(Char::titlecase),
        )
    }
```

- [ ] **Step 2: 实现 `CalendarFragment`**

要点：

```kotlin
class CalendarFragment : Fragment(R.layout.fragment_calendar) {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(AppGraph.current.appDataWithAuthorizationService)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // bind header month/year from currentMonth StateFlow
        // monthViewPager.adapter = CalendarMonthPagerAdapter(...)
        // monthViewPager.registerOnPageChangeCallback → viewModel.onMonthSelected
        // todayBadge click → viewModel.scrollToCurrentMonth() + setCurrentItem
        // lifecycleScope: generateMonths → scrollToCurrentMonth → fetchData
        // collect months/currentMonth → notify adapter + header
    }
}
```

- Factory 从 `AppGraph.current` 取 service（与 SignIn 模式一致）。

- [ ] **Step 3: 全量构建**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:assembleDebug`  
Expected: BUILD SUCCESSFUL

---

### Task 9: 全量验证与文档

**Files:**

- Modify: `docs/app-startup-flow.md`（Calendar Tab 说明）
- Modify: `LittleThingsAndroidAI/README.md`（可选，Calendar Phase A 状态）

- [x] **Step 1: 运行全部单元测试**

Run: `cd LittleThingsAndroidAI && ./gradlew :app:testDebugUnitTest`  
Expected: ALL PASS

- [ ] **Step 2: 手动验证**

1. `SignInDevConfig.MOCK_GOOGLE_SIGN_IN = true` 登录
2. 默认 Calendar Tab：Header 显示当前月/年
3. 横向滑动切月，Footer 文案变化
4. 点击 Today 角标回到当前月
5. Grid 显示日期；有 API 数据时显示 stamp 占位

- [x] **Step 3: 更新 `app-startup-flow.md`**

在 Home 章节追加 Calendar Phase A 流程说明。

---

## Phase A 交付检查清单

- [x] `HomeTabAdapter` index 0 → `CalendarFragment`
- [x] `ReflectionRepository` + `CalendarReflectionsUseCase` 对接 API
- [x] Header + Weekday + ViewPager2 + 7 列 Grid + Footer
- [x] `generateMonths` / `fetchData` / `scrollToCurrentMonth`
- [x] ViewModel + UseCase 单元测试通过
- [x] `./gradlew assembleDebug` 成功

---

## 后续 Plan 文件（待 Phase A 完成后撰写）

| 文件 | Phase |
|------|-------|
| `2026-07-05-android-calendar-phase-b.md` | MonthPicker + SwipeRefresh + monthLock |
| `2026-07-05-android-calendar-phase-c.md` | Coil + stamp 布局 + 详情 + markRead |
| `2026-07-05-android-calendar-phase-d.md` | TodayQuestion + Add stub |
