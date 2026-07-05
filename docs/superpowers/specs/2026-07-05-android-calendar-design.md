# Little Things Android — Calendar Tab 设计

**日期：** 2026-07-05  
**状态：** 已批准  
**Figma 参考：** [Calendar 屏](https://www.figma.com/design/b0c3FPp8qfuqyTnBo7rHfb/The-Little-Things--NEW-?node-id=4824-4705&m=dev)  
**前置文档：** [Home 壳层设计](2026-07-05-android-home-shell-design.md)  
**iOS 对照：** `Domain/Calendar/`、`Service/Reflection/`

---

## 1. 目标与已确认决策

### 目标

将 Home Tab index 0 从 `PlaceholderTabFragment` 替换为 **接近 iOS 完整体验的 Calendar 页**：Fixed Header、Weekday 行、多月份横向切月、7 列月历网格、stamp 图标、Today 高亮、Footer 文案，并对接 `GET /api/calendar-view` 等 Reflection API。

### 已确认决策

| 项 | 选择 |
|----|------|
| 交付深度 | **C — 接近 iOS 完整体验**（分 Phase A–D 迭代交付） |
| 实现方案 | **方案 1** — XML + ViewPager2 横向切月 + Grid RecyclerView |
| UI 技术 | XML + ViewBinding（与 SignIn / Home 一致） |
| TabBar | 沿用 Home 壳层 `LtHomeTabBar`，Calendar 为 Tab 0 内容 |
| Tab 切换 | 仅点击 TabBar；Home `ViewPager2` 仍禁滑动 |
| Stamp / Tab icon | 系统 vector **占位**；图片 URL 用 Coil 加载，失败回退占位 |
| 接口参考 | iOS `Service/Reflection/`；响应格式见 `LTApp-BE/docs/doc/API.md` |
| 详情页深度（Phase C） | **B — 最小可读详情**（问题标题 + 回答正文 + icon；无复杂动画） |

### Phase 拆分

| Phase | 范围 | 可独立验收 |
|-------|------|-----------|
| **A** | Service 层 + ViewModel + Header / Weekday / 横向切月 / 网格 / Footer + API | ✅ 有网格、可切月、显示日期与 Footer |
| **B** | 月份选择器、Pull-to-refresh、未来月 monthLock | ✅ 可选月、可刷新、未来月占位 |
| **C** | Coil stamp 布局（1/2/3/4+）、点击详情、`markIconRead` | ✅ stamp 视觉 + 详情可读 |
| **D** | `TodayQuestionView` 浮层、`questions-of-the-day`、Add 跳转 stub | ✅ 浮层可见、Add stub |

### 不在本轮

- 自定义字体 *The Little Things 02*（先用系统 / 现有 typography）
- TabBar 随滚动隐藏（iOS `TabbarVisibility`）
- 完整 Submit Answer 流程（Phase D 仅 stub 导航）
- SSE icon 生成进度（`/api/icon/progress/:iconId`）
- Thread / Insights / User Tab 业务

---

## 2. 架构与文件结构

### 2.1 布局层次

```
fragment_calendar.xml
└── ConstraintLayout（background: lt_oat）
    ├── CalendarHeader          @+id/calendarHeader
    ├── CalendarWeekdayRow      @+id/weekdayRow
    ├── ViewPager2              @+id/monthViewPager   （horizontal, paging）
    │   └── 每页 item_calendar_month_page.xml
    │       └── NestedScrollView
    │           ├── RecyclerView GridLayoutManager(7)  @+id/dayGrid
    │           └── TextView Footer                      @+id/monthFooter
    ├── CalendarMonthPicker     @+id/monthPicker       （Phase B，默认 gone）
    └── TodayQuestionBanner     @+id/todayQuestion    （Phase D，默认 gone）

activity_home.xml（不变）
└── LtHomeTabBar 浮于 Calendar 之上
```

### 2.2 新增 / 修改文件

| 路径 | 说明 |
|------|------|
| **`:app` — domain/calendar/** | |
| `CalendarFragment.kt` | Tab 0 入口；绑定 ViewModel、ViewPager、Header |
| `CalendarViewModel.kt` | 镜像 iOS `CalendarViewModel` |
| `CalendarMonthPagerAdapter.kt` | ViewPager2 每月一页 |
| `CalendarDayGridAdapter.kt` | 7 列 Grid，单日 cell |
| `CalendarMonthPickerAdapter.kt` | Header 下拉月份列表（Phase B） |
| `model/CalendarDay.kt` | 对齐 iOS `CalendarDay` |
| `model/CalendarMonth.kt` | 对齐 iOS `CalendarMonth` |
| `model/WeekDay.kt` | 星期标题 |
| `detail/ReflectionDetailFragment.kt` | Phase C：stamp 点击详情 |
| **`:app` — service/reflection/** | |
| `request/ReflectionRequest.kt` | `calendar`、`questionsOfToday`、`markIconRead` 等 |
| `dto/*.kt` | `DayReflectionsDto`、`AnswerDto`、`IconDto`、`QuestionDto` |
| `repository/ReflectionRepository.kt` | API 调用 |
| `usecase/CalendarReflectionsUseCase.kt` | |
| `usecase/FetchTodayQuestionsUseCase.kt` | Phase D |
| `usecase/MarkIconReadUseCase.kt` | Phase C |
| **`:app` — 修改** | |
| `domain/home/HomeTabAdapter.kt` | index 0 → `CalendarFragment` |
| `service/AppDataWithAuthorizationService.kt` | 替换 placeholder UseCase |
| `app/AppGraph.kt` | 注册 `ReflectionRepository` |
| `app/home/HomeCoordinator.kt` | 扩展子 Fragment 导航（详情 push） |
| `domain/coordinator/Route.kt` | 可选 `CalendarRoute.REFLECTION_DETAIL` |
| **`:app` — res/** | |
| `layout/fragment_calendar.xml` | |
| `layout/item_calendar_month_page.xml` | |
| `layout/item_calendar_day_cell.xml` | |
| `layout/view_calendar_header.xml` | |
| `layout/view_calendar_weekday_row.xml` | |
| `layout/view_calendar_month_picker.xml` | Phase B |
| `layout/fragment_reflection_detail.xml` | Phase C |
| `drawable/bg_calendar_grid_dash.xml` | 对角虚线占位 |
| `drawable/ic_calendar_today_badge.xml` | Today 角标占位 |
| `values/strings.xml` | Footer / monthLock / 详情文案 |

### 2.3 依赖方向

```
CalendarFragment → CalendarViewModel → AppDataWithAuthorizationService
                                              → CalendarReflectionsUseCase
                                              → ReflectionRepository
                                              → authenticatedApiClient
HomeTabAdapter → CalendarFragment（Tab 0）
HomeCoordinator → CalendarFragment 内 child FragmentManager（详情）
```

### 2.4 新增 Gradle 依赖（`:app`）

```kotlin
implementation(libs.coil)                    // Phase C 图片加载
implementation(libs.androidx.swiperefreshlayout)  // Phase B 下拉刷新
```

---

## 3. UI 规格（对齐 Figma / iOS）

### 3.1 设计 Token

| Token | 值 | 用途 |
|-------|-----|------|
| `lt_oat` | `#FFFDF8` | Calendar 背景（已有） |
| 日期文字（当月） | `#323232` | Day cell 日期 |
| 日期文字（非当月） | `#CDCDCD` |  padding 月外日期 |
| 网格线 | `#E0E0E0` 或 dash drawable | 单元格分隔 |
| Footer 文字 | `#000000` | section 风格 |
| monthLock 文案 | 灰色居中 | "The best is yet to come" |

### 3.2 Fixed Header

| 元素 | 行为 |
|------|------|
| 月份 + 下箭头 | 点击展开/收起 MonthPicker（Phase B）；显示 `January` 等全称 |
| 年份 | 月份下方，section 字号 |
| Today 角标 | 右侧 brush 占位 icon + 当日 `dayDesc`；点击 `scrollToCurrentMonth()` |

水平 padding 对齐 iOS `Constants.hP`（约 24dp）。

### 3.3 Weekday Row

固定一行：`S M T W T F S`（与 iOS `WeekDay` 一致）。  
未来月整行可置灰（Phase B `monthLock` 态）。

### 3.4 月历网格（Day Cell）

**Cell 状态（对齐 iOS `CalendarItemView` / Flutter `calendar_item_view`）：**

| 状态 | UI |
|------|-----|
| 有 1 个 reflection | 单 stamp（Coil，Phase C；Phase A 用占位 icon） |
| 2 / 3 个 | 组合布局（右上/左下/三角等，Phase C） |
| 4+ | 3 stamp + `N+` 徽章（Phase C） |
| Today 无回答 | 渐变圆 + `+`（Phase C；Phase A 可简化为 `+` 占位） |
| 过去无回答（`isAbsent`） | 对角 Dash |
| 非当月日期 | 日期 `#CDCDCD`，stamp 区域 opacity 降低 |

**Cell 尺寸：** 等宽 7 列；高度按行数自适应（`GridLayoutManager` span=7）。

### 3.5 Footer 文案

镜像 iOS `footerView(momth:)`：

| 条件 | 文案 |
|------|------|
| 当前月且非月末 | `{iconCount} stamps collected so far \n take your time with the {moreDaysToGo} days ahead` |
| 当前月且是月末 | `{iconCount} stamps collected so far \n take your time today` |
| 其他月 | `{iconCount} stamps collected this month` |

### 3.6 未来月 monthLock（Phase B）

当 `CalendarMonth.isFuture == true`：网格区显示居中占位文案 **"The best is yet to come"**，不展示 day cells。

### 3.7 Reflection 详情页（Phase C — 方案 B）

最小可读布局：

```
┌─────────────────────────┐
│ ← Back                  │
│ [Icon 大图 Coil]        │
│ Question title          │
│ Answer content          │
│ Category name（可选）    │
└─────────────────────────┘
```

- 从 stamp 点击 push `ReflectionDetailFragment`（Home 内 child FM 或 Activity 级 container）
- 进入后延迟 500ms 调用 `markIconRead`（对齐 iOS）
- 无 hero 动画、无编辑/删除（后续迭代）

### 3.8 TodayQuestion 浮层（Phase D）

- 浮于 TabBar 上方（`marginBottom` 避让 `LtHomeTabBar`）
- 展示 `questions-of-the-day` 第一条 + 展开更多
- Add / 点击问题 → Toast 或 stub 导航（`SubmitAnswer` 未实现）

---

## 4. 数据模型

### 4.1 Domain Model（镜像 iOS）

```kotlin
data class WeekDay(val title: String)

data class CalendarDay(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    var isAbsent: Boolean,
    var reflections: DayReflections? = null,
) {
    val dayType: DayType // past | today | future
    fun copyWith(reflections: DayReflections): CalendarDay
}

enum class DayType { PAST, TODAY, FUTURE }
enum class MonthItemType { NORMAL, YEAR_PLACEHOLDER }

data class CalendarMonth(
    val id: String = UUID.randomUUID().toString(),
    val date: YearMonth,
    var days: List<CalendarDay>,
    var iconCount: Int = 0,
    var moreDaysToGo: Int = 0,
    val itemType: MonthItemType = MonthItemType.NORMAL,
) {
    val isFuture: Boolean
    val isValidMonth: Boolean  // itemType == NORMAL
}

data class DayReflections(val day: LocalDate, val reflections: List<Answer>)
data class Answer(val id: String, val content: String, val question: Question?, val icon: Icon?)
data class Icon(val id: String?, val url: String?, val status: String, val readAt: String?)
data class Question(val id: String, val title: String, val category: Category?)
data class Category(val id: String, val name: String)
```

### 4.2 API 映射

**`GET /api/calendar-view?start=YYYY-MM-DD&end=YYYY-MM-DD`**

- `start` / `end`：当月 `startOfMonth` / `endOfMonth`（`LocalDate` → ISO date string）
- 响应：`List<CalendarDayDto>`，`date` + `reflections[]`
- DTO → Domain：`ReflectionRepository.mapToDayReflections()`

**`GET /api/questions-of-the-day`**（Phase D）

**`POST /api/icons/{iconId}/read`** 或 iOS 等价 mark read 端点（实现前对照 iOS `MarkIconReadUseCase`）

### 4.3 ViewModel 核心逻辑（镜像 iOS）

| 方法 | 说明 |
|------|------|
| `generateMonths()` | 2025-01-01 至 `now + 1 month`；每年 1 月前插入 `yearPlaceholder` |
| `generateSingleMonthData()` | 计算 weekday offset，填充 leading/trailing 非当月日 |
| `fetchData()` | 拉当前月 API，merge reflections，重算 `isAbsent` / `iconCount` / `moreDaysToGo` |
| `scrollToCurrentMonth()` | ViewPager 滚至含 today 的 normal month |
| `didTapMonth(month)` | Header 选月 → 同步 ViewPager |
| `onMonthPageSelected(index)` | 滑动切月 → 更新 `currentMonth` → `fetchData()` |
| `markIconAsRead(answer)` | Phase C |
| `fetchTodayQuestions()` | Phase D |

**`isAbsent` 算法（与 iOS 一致）：**  
在 merge reflections 后，对无 reflection 的 day：若其 date 落在该月「首个有回答日」与「末个有回答日」之间，则 `isAbsent = true`。

---

## 5. 导航与 Coordinator

### 5.1 HomeTabAdapter

```kotlin
override fun createFragment(position: Int): Fragment =
    when (tabs[position]) {
        HomeRoute.CALENDAR -> CalendarFragment()
        else -> PlaceholderTabFragment.newInstance(...)
    }
```

### 5.2 详情导航（Phase C）

`CalendarFragment` 内使用 `childFragmentManager`：

```
点击 stamp → ReflectionDetailFragment.newInstance(answerId)
  → add R.id.calendarDetailContainer 或 replace
返回 → popBackStack
```

`HomeCoordinator` 本轮 **不强制** 感知 Calendar 子栈；若需 `popToRoot` 清空详情，CalendarFragment 实现 `onHiddenChanged` / 接口回调。

---

## 6. 交互流程

```
CalendarFragment.onViewCreated
  → viewModel.generateMonths()
  → monthViewPager.adapter = CalendarMonthPagerAdapter
  → scrollToCurrentMonth(animated = false)
  → fetchData()
  → fetchTodayQuestions()          // Phase D

用户横向滑动 ViewPager2
  → onPageSelected → currentMonth 更新 → Header 同步 → fetchData()

用户点击 Header 月份
  → toggle MonthPicker（Phase B）
  → 选中月份 → didTapMonth → setCurrentItem

用户点击 Today 角标
  → scrollToCurrentMonth()

Pull-to-refresh（Phase B）
  → fetchData()

点击 stamp（Phase C）
  → ReflectionDetailFragment + markIconRead
```

---

## 7. 错误处理与边界

| 场景 | 行为 |
|------|------|
| API 失败 | Snackbar / 静默保留本地 grid；可选 retry |
| 无网络 | 显示空网格 + 上次缓存（本轮可不缓存，仅 error toast） |
| Icon URL 空 / PENDING | 显示 vector 占位 |
| Session 过期 | 现有 `SessionExpirationObserver` 跳转登录 |
| 配置变更 | ViewModel `SavedStateHandle` 保存 `currentMonthIndex` |
| yearPlaceholder | ViewPager 页宽 0 或 skip（filter `isValidMonth` 索引映射） |

---

## 8. 测试策略

| 类型 | 内容 |
|------|------|
| 单元测试 | `CalendarViewModelTest`：`generateMonths` 范围、`fetchData` merge、`isAbsent` 逻辑 |
| 单元测试 | `CalendarReflectionsUseCaseTest` + MockWebServer |
| 单元测试 | `CalendarMonthIndexTest`：ViewPager index ↔ valid month 映射 |
| 手动 | Mock 登录 → Calendar Tab；切月、Footer 文案、Today 跳转 |
| Phase C 手动 | 点击 stamp → 详情可读；read API 调用 |
| Phase D 手动 | TodayQuestion 浮层显示 |

---

## 9. 交付标准（按 Phase）

### Phase A

- [ ] `HomeTabAdapter` index 0 为 `CalendarFragment`
- [ ] `ReflectionRepository` + `CalendarReflectionsUseCase` 对接 API
- [ ] Header + Weekday + 横向 ViewPager2 + 7 列 Grid + Footer
- [ ] `generateMonths` / `fetchData` / `scrollToCurrentMonth` 行为对齐 iOS
- [ ] Day cell 显示日期；有数据时占位 stamp；`isAbsent` dash
- [ ] `./gradlew assembleDebug` + ViewModel 单元测试通过

### Phase B

- [ ] MonthPicker 展开/选中切月
- [ ] SwipeRefreshLayout 刷新当前月
- [ ] 未来月 monthLock 占位

### Phase C

- [ ] 1/2/3/4+ stamp 布局 + Coil
- [ ] `ReflectionDetailFragment` 最小可读详情
- [ ] `markIconRead` 调用

### Phase D

- [ ] `TodayQuestionBanner` + `questions-of-the-day`
- [ ] Add 按钮 stub 导航

---

## 10. iOS 对照

| Android | iOS |
|---------|-----|
| `CalendarFragment` | `CalendarView` |
| `CalendarViewModel` | `CalendarViewModel` |
| `CalendarMonthPagerAdapter` | `ScrollView` + `scrollTargetBehavior(.paging)` |
| `CalendarDayGridAdapter` | `LazyVGrid` 7 列 |
| `ReflectionRepository` | `ReflectionRepository` |
| `CalendarReflectionsUseCase` | `CalendarReflectionsUseCase` |
| `ReflectionDetailFragment` | `ReflectionDetailView` / `TodayAnswerSubmittedView` |
| `LtHomeTabBar`（Home 层） | `AppTabbar` |

---

## 11. 后续迭代

- Figma 导出字体、brush Today 角标、真实 Tab icon
- 完整 Submit Answer 流程对接 `POST /api/answers`
- Icon SSE 进度与生成态动画
- TabBar 滚动隐藏
- 答案编辑 / 删除
