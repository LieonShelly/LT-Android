# Little Things Android — Home 壳层设计

**日期：** 2026-07-05  
**状态：** 已批准（方案 1）  
**Figma 参考：** [Calendar 屏（含底部 TabBar）](https://www.figma.com/design/b0c3FPp8qfuqyTnBo7rHfb/The-Little-Things--NEW-?node-id=4824-4705&m=dev)  
**前置文档：** [架构设计](2026-07-05-littlethings-android-architecture-design.md)  
**iOS 对照：** `AppHomeView` + `AppTabbar`

---

## 1. 目标与已确认决策

### 目标

登录后进入 `HomeActivity`，交付 **Figma 对齐的 Home 壳层**：底部黑色胶囊 TabBar + 四 Tab 切换框架。Tab 内容本轮仍为占位，为后续 Calendar / Thread / Insights / User 业务迭代预留接口。

### 已确认决策

| 项 | 选择 |
|----|------|
| 交付范围 | **B — 仅 Home 壳层**，四 Tab 内容占位 |
| 实现方案 | **方案 1** — 保留 `ViewPager2` + 自定义底部 `LtHomeTabBar` |
| Tab 切换 | **仅点击 TabBar**，`ViewPager2.isUserInputEnabled = false` |
| Tab 图标 | Android 系统 vector **占位**，命名预留 `ic_tab_*`，用户后续替换 |
| UI 技术 | XML + ViewBinding（与 SignIn 一致） |
| 默认 Tab | Calendar（index 0） |
| 接口参考 | iOS `Domain/Home/`、`AppTabbar`；Figma 视觉为准 |

### 不在本轮

- Calendar 月历网格、Fixed Header、stamp 图标、底部文案、API（`GET /api/calendar-view`）
- Thread / Insights / User 业务 UI
- TabBar 随滚动隐藏（iOS `TabbarVisibility`）
- 自定义字体 *The Little Things 02*
- Compose TabBar、Fragment show/hide 替代 ViewPager2

---

## 2. 架构与文件结构

### 2.1 布局层次

```
activity_home.xml
└── ConstraintLayout / FrameLayout（background: lt_oat #FFFDF8）
    ├── ViewPager2          @+id/homeViewPager   （全屏，底部 inset 避让 TabBar）
    └── LtHomeTabBar        @+id/homeTabBar     （底部居中浮动）
        └── 4 × TabItem（ImageButton / ImageView，40dp，间距 24dp）
```

### 2.2 新增 / 修改文件

| 路径 | 说明 |
|------|------|
| `:core:uicomponent` | |
| `res/values/colors.xml` | 新增 `lt_oat` = `#FFFDF8` |
| `AppColors.kt` | 新增 `oat` 常量 |
| `res/drawable/bg_home_tab_bar.xml` | 黑色圆角 32dp + 可选 shadow |
| `res/layout/view_lt_home_tab_bar.xml` | TabBar 布局 |
| `LtHomeTabBar.kt` | Custom View，Tab 选中态与点击回调 |
| `HomeTabItem.kt` | Tab 配置 data class |
| `:app` | |
| `res/layout/activity_home.xml` | 移除顶部 `TabLayout`，加入 `LtHomeTabBar` |
| `app/home/HomeActivity.kt` |  wiring TabBar ↔ ViewPager2 ↔ Coordinator |
| `app/home/HomeCoordinator.kt` | 增加 TabBar 选中态同步（可选 `OnPageChangeCallback`） |
| `domain/home/HomeTabAdapter.kt` | 不变（仍返回 `PlaceholderTabFragment`） |
| `domain/home/PlaceholderTabFragment.kt` | 微调：背景 `lt_oat`，居中占位文案 |

### 2.3 依赖方向

```
:app → :core:uicomponent（LtHomeTabBar、colors）
HomeActivity → HomeCoordinator → ViewPager2
HomeActivity → LtHomeTabBar（双向绑定选中 index）
```

---

## 3. UI 规格（对齐 Figma / iOS AppTabbar）

### 3.1 设计 Token

| Token | 值 | 用途 |
|-------|-----|------|
| `lt_oat` | `#FFFDF8` | Home 页面背景（Figma `oat`） |
| TabBar 背景 | `#000000` | 胶囊底 |
| Tab icon 选中 | `#FFFFFF` | 100% 白 |
| Tab icon 未选中 | `#FFFFFF` @ 50% alpha | 区分选中态 |
| TabBar 圆角 | 32dp | 对齐 Figma / iOS |
| TabBar 内边距 | horizontal 42dp, vertical 16dp | |
| Icon 尺寸 | 40×40dp | |
| Icon 间距 | 24dp | |
| TabBar 距屏幕底 | 48dp + `navigationBars` inset | |
| TabBar 水平边距 | ~41dp（或 `layout_marginHorizontal` 使内容宽约 319dp） |

> 注：现有 `lt_background`（`#F5F0E8`）保留给 SignIn 等页面；Home 专用 `lt_oat`。

### 3.2 Tab 顺序与占位 Icon

与 iOS / Figma 一致：

| Index | Route | 占位 icon（系统 vector） | 后续替换 |
|-------|-------|--------------------------|----------|
| 0 | `HomeRoute.CALENDAR` | Material `calendar_month_24` 或等效 | `ic_tab_calendar` |
| 1 | `HomeRoute.THREAD` | Material `linear_scale` / 近似 | `ic_tab_thread` |
| 2 | `HomeRoute.INSIGHTS` | Material `lightbulb_outline` | `ic_tab_insights` |
| 3 | `HomeRoute.USER` | Material `person_outline` | `ic_tab_user` |

占位 icon 放在 `:core:uicomponent/src/main/res/drawable/` 或 `:app`（若仅 Home 使用则放 uicomponent 便于复用）。

### 3.3 占位 Tab 内容

`PlaceholderTabFragment` 保持极简：

- 背景 `lt_oat`
- 居中 TextView：`Calendar` / `Thread` / `Insights` / `User`（或 `@string/home_tab_*`）
- 无业务逻辑、无 ViewModel

---

## 4. 导航与 Coordinator

### 4.1 双向同步

```
用户点击 TabBar[index]
  → viewPager.setCurrentItem(index, smoothScroll = false)
  → tabBar.setSelectedIndex(index)

HomeCoordinator.push(HomeRoute.X)
  → viewPager.setCurrentItem(tabs.indexOf(route))
  → tabBar.setSelectedIndex(...)

ViewPager2.OnPageChangeCallback（若 programmatic 切换）
  → tabBar.setSelectedIndex(position)
```

### 4.2 HomeCoordinator（现有扩展）

```kotlin
class HomeCoordinator(
    private val viewPager: ViewPager2,
    private val tabBar: LtHomeTabBar,  // 新增
    private val tabs: List<HomeRoute>,
) : Coordinator {
    fun bind() {
        viewPager.isUserInputEnabled = false
        viewPager.registerOnPageChangeCallback(...)
        tabBar.setOnTabSelectedListener { index -> viewPager.setCurrentItem(index, false) }
    }
    override fun push(route: Route) { /* 同步 tabBar + viewPager */ }
    override fun popToRoot() { push(HomeRoute.CALENDAR) }
}
```

`UserHomeCoordinator` 本轮不改动（仍为空骨架）。

### 4.3 HomeActivity 初始化顺序

1. `setContentView` + edge-to-edge / WindowInsets
2. `viewPager.adapter = HomeTabAdapter(...)`
3. `viewPager.isUserInputEnabled = false`
4. `coordinator.bind()` / TabBar wiring
5. `coordinator.push(HomeRoute.CALENDAR)` 或默认 index 0
6. `observeSessionExpiration()`

---

## 5. LtHomeTabBar 组件 API

```kotlin
class LtHomeTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    fun setTabs(items: List<HomeTabItem>)
    fun setSelectedIndex(index: Int)
    fun setOnTabSelectedListener(listener: (Int) -> Unit)
}
```

**职责单一：** 只负责渲染 Tab 项与选中态，不包含 ViewPager 引用（由 Activity/Coordinator 连接）。

---

## 6. 错误处理与边界

| 场景 | 行为 |
|------|------|
| 重复点击当前 Tab | 无操作（可选：预留 scroll-to-top 钩子） |
| 配置变更（旋转） | ViewPager2 + Fragment 默认 restore；TabBar 从 `savedInstanceState` 或 ViewPager currentItem 恢复 |
| Session 过期 | 现有 `observeSessionExpiration()` 不变 |

---

## 7. 测试策略

| 类型 | 内容 |
|------|------|
| 单元测试 | `HomeCoordinatorTest`：push 各 `HomeRoute` 验证 target index；popToRoot → Calendar |
| 可选 UI | `HomeActivity` 仪器测试：点击 Tab 2/3/4 后 `PlaceholderTabFragment` 文案变化 |
| 手动 | Mock 登录 → Home；验证 TabBar 视觉、四 Tab 切换、不可滑动 |

---

## 8. 交付标准

- [ ] 顶部 Material `TabLayout` 已移除
- [ ] 底部黑色胶囊 TabBar 视觉对齐 Figma（圆角、间距、浮动位置）
- [ ] 四 Tab 可点击切换，ViewPager 不可滑动
- [ ] 默认显示 Calendar 占位页
- [ ] 系统 icon 占位 + drawable 命名预留文档注释
- [ ] `lt_oat` token 已加入 `:core:uicomponent`
- [ ] `./gradlew assembleDebug` 与相关单元测试通过

---

## 9. 后续迭代（Calendar Tab）

下一轮可在不改动壳层的前提下：

1. 将 index 0 的 `PlaceholderTabFragment` 替换为 `CalendarFragment`
2. 按 Figma `4824:4705` 实现 Fixed Header + 月历网格
3. 对接 iOS `Service/` 中 calendar 相关 Request / Repository
4. 将 TabBar 与占位 icon 替换为用户提供的 Figma 导出资源

---

## 10. iOS 对照

| Android | iOS |
|---------|-----|
| `HomeActivity` | `HomeCoordinator` + Tab 容器 |
| `LtHomeTabBar` | `AppTabbar` |
| `ViewPager2` + Fragments | `AppScrollContentView` 纵向 Tab 页（Android 用横向 pager 仅作容器，不滑动） |
| `HomeCoordinator.push(HomeRoute)` | `HomeCoordinator.push(HomeRoute)` |
| `PlaceholderTabFragment` | 各 Tab 占位 / 真实 View |
