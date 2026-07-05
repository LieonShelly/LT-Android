# Home 壳层 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `HomeActivity` 从顶部 Material TabLayout 占位页，升级为 Figma 对齐的底部黑色胶囊 TabBar + ViewPager2 四 Tab 切换框架（内容仍占位，禁用手势滑动）。

**Architecture:** 方案 1 — 保留 `ViewPager2` + `HomeTabAdapter`；新增 `:core:uicomponent` 组件 `LtHomeTabBar`；`HomeCoordinator.bind()` 负责 TabBar ↔ ViewPager 双向同步；占位 icon 用 vector drawable（`ic_tab_*_placeholder`），后续可替换为 Figma 导出资源。

**Tech Stack:** Kotlin 2.2、AGP 9.1、XML + ViewBinding、ViewPager2、Material、ConstraintLayout、WindowInsets

**Spec:** [2026-07-05-android-home-shell-design.md](../specs/2026-07-05-android-home-shell-design.md)

**Figma:** [Calendar 屏（含 TabBar）](https://www.figma.com/design/b0c3FPp8qfuqyTnBo7rHfb/The-Little-Things--NEW-?node-id=4824-4705&m=dev)

**iOS 对照（实现前 codegraph 检索）：**


| 领域          | iOS 路径                                     |
| ----------- | ------------------------------------------ |
| Home 壳层     | `Domain/Home/View/AppHomeView.swift`       |
| TabBar      | `Domain/Home/View/AppTabbar.swift`         |
| Coordinator | `Domain/Coordinator/HomeCoordinator.swift` |


---

## File Structure Overview

```
LittleThingsAndroidAI/
├── core/uicomponent/
│   ├── build.gradle.kts                                    # MODIFY: viewBinding
│   └── src/main/
│       ├── kotlin/.../uicomponent/
│       │   ├── AppColors.kt                                # MODIFY: oat
│       │   ├── HomeTabItem.kt                              # CREATE
│       │   └── LtHomeTabBar.kt                             # CREATE
│       └── res/
│           ├── values/colors.xml                           # MODIFY: lt_oat
│           ├── drawable/bg_home_tab_bar.xml                # CREATE
│           ├── drawable/ic_tab_*_placeholder.xml           # CREATE ×4
│           └── layout/view_lt_home_tab_bar.xml             # CREATE
├── app/src/main/
│   ├── kotlin/.../app/home/
│   │   ├── HomeActivity.kt                                 # MODIFY
│   │   └── HomeCoordinator.kt                              # MODIFY
│   ├── kotlin/.../domain/home/
│   │   └── PlaceholderTabFragment.kt                       # MODIFY
│   ├── res/layout/
│   │   ├── activity_home.xml                               # MODIFY
│   │   └── fragment_placeholder_tab.xml                    # MODIFY
│   └── res/values/strings.xml                              # MODIFY: tab labels
└── app/src/test/kotlin/.../app/home/
    └── HomeCoordinatorTest.kt                              # CREATE
```

---

### Task 1: 设计 Token（lt_oat）

**Files:**

- Modify: `core/uicomponent/src/main/res/values/colors.xml`
- Modify: `core/uicomponent/src/main/kotlin/com/littlethingsandroidai/core/uicomponent/AppColors.kt`
- **Step 1: 新增 `lt_oat` 颜色**

`core/uicomponent/src/main/res/values/colors.xml` 追加：

```xml
<!-- Figma oat — Home 背景 -->
<color name="lt_oat">#FFFDF8</color>
<color name="lt_tab_bar_icon_selected">#FFFFFFFF</color>
<color name="lt_tab_bar_icon_unselected">#80FFFFFF</color>
<color name="lt_tab_bar_background">#FF000000</color>
```

- **Step 2: 更新 `AppColors.kt`**

```kotlin
/** Figma oat — Home 页面背景。 */
@ColorInt
const val oat: Int = 0xFFFFFDF8.toInt()

@ColorInt
const val tabBarBackground: Int = 0xFF000000.toInt()
```

- **Step 3: 验证 uicomponent 编译**

Run: `cd LittleThingsAndroidAI && ./gradlew :core:uicomponent:assembleDebug`  
Expected: BUILD SUCCESSFUL

---

### Task 2: TabBar 占位 Icon（vector drawable）

**Files:**

- Create: `core/uicomponent/src/main/res/drawable/ic_tab_calendar_placeholder.xml`
- Create: `core/uicomponent/src/main/res/drawable/ic_tab_thread_placeholder.xml`
- Create: `core/uicomponent/src/main/res/drawable/ic_tab_insights_placeholder.xml`
- Create: `core/uicomponent/src/main/res/drawable/ic_tab_user_placeholder.xml`

> 注释：`<!-- Placeholder: replace with ic_tab_calendar from Figma export -->`

- **Step 1: 创建四个占位 vector（白色 stroke，24dp viewport）**

`ic_tab_calendar_placeholder.xml` 示例：

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Placeholder: replace with ic_tab_calendar from Figma export -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M7,2h2v2h6V2h2v2h2a2,2 0,0 1,2 2v14a2,2 0,0 1,-2 2H5a2,2 0,0 1,-2 -2V6a2,2 0,0 1,2 -2h2V2zM5,8v12h14V8H5z" />
</vector>
```

`ic_tab_thread_placeholder.xml` — 三条横线（thread/spool 近似）：

```xml
<vector ... viewport 24>
    <path android:fillColor="#FFFFFF"
        android:pathData="M4,7h16v2H4zM4,11h16v2H4zM4,15h16v2H4z" />
</vector>
```

`ic_tab_insights_placeholder.xml` — 灯泡轮廓近似：

```xml
<vector ...>
    <path android:fillColor="#FFFFFF"
        android:pathData="M12,2a7,7 0,0 0,-4 12.74V18h8v-3.26A7,7 0,0 0,12 2zM11,22h2v-2h-2v2z" />
</vector>
```

`ic_tab_user_placeholder.xml` — 人形轮廓：

```xml
<vector ...>
    <path android:fillColor="#FFFFFF"
        android:pathData="M12,12a4,4 0,1 0,-0.001 -8.001A4,4 0,0 0,12 12zM6,20v-1a6,6 0,0 1,12 0v1H6z" />
</vector>
```

- **Step 2: 验证资源合并**

Run: `./gradlew :core:uicomponent:assembleDebug`  
Expected: BUILD SUCCESSFUL

---

### Task 3: LtHomeTabBar 组件

**Files:**

- Modify: `core/uicomponent/build.gradle.kts`
- Create: `core/uicomponent/src/main/kotlin/com/littlethingsandroidai/core/uicomponent/HomeTabItem.kt`
- Create: `core/uicomponent/src/main/res/drawable/bg_home_tab_bar.xml`
- Create: `core/uicomponent/src/main/res/layout/view_lt_home_tab_bar.xml`
- Create: `core/uicomponent/src/main/kotlin/com/littlethingsandroidai/core/uicomponent/LtHomeTabBar.kt`
- **Step 1: 启用 uicomponent ViewBinding**

`core/uicomponent/build.gradle.kts`：

```kotlin
android {
    ...
    buildFeatures {
        viewBinding = true
    }
}
```

- **Step 2: TabBar 背景 drawable**

`bg_home_tab_bar.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/lt_tab_bar_background" />
    <corners android:radius="32dp" />
</shape>
```

- **Step 3: `HomeTabItem.kt`**

```kotlin
package com.littlethingsandroidai.core.uicomponent

import androidx.annotation.DrawableRes

data class HomeTabItem(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
)
```

- **Step 4: TabBar 布局**

`view_lt_home_tab_bar.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<merge xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    tools:parentTag="android.widget.FrameLayout">

    <LinearLayout
        android:id="@+id/tabBarContainer"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_home_tab_bar"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingHorizontal="42dp"
        android:paddingVertical="16dp">

        <LinearLayout
            android:id="@+id/tabItemsContainer"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="horizontal" />
    </LinearLayout>
</merge>
```

- **Step 5: `LtHomeTabBar.kt`**

```kotlin
package com.littlethingsandroidai.core.uicomponent

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.littlethingsandroidai.core.uicomponent.databinding.ViewLtHomeTabBarBinding

class LtHomeTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val binding = ViewLtHomeTabBarBinding.inflate(LayoutInflater.from(context), this)
    private val tabViews = mutableListOf<ImageView>()
    private var selectedIndex: Int = 0
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    fun setTabs(items: List<HomeTabItem>) {
        val container = binding.tabItemsContainer
        container.removeAllViews()
        tabViews.clear()

        items.forEachIndexed { index, item ->
            val tabView =
                ImageView(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                            if (index > 0) marginStart = dp(24)
                        }
                    setImageResource(item.iconRes)
                    contentDescription = item.contentDescription
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (selectedIndex != index) {
                            setSelectedIndex(index)
                            onTabSelectedListener?.invoke(index)
                        }
                    }
                }
            tabViews.add(tabView)
            container.addView(tabView)
        }
        updateSelectionVisuals()
    }

    fun setSelectedIndex(index: Int) {
        if (index !in tabViews.indices) return
        selectedIndex = index
        updateSelectionVisuals()
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    private fun updateSelectionVisuals() {
        val selectedColor = ContextCompat.getColor(context, R.color.lt_tab_bar_icon_selected)
        val unselectedColor = ContextCompat.getColor(context, R.color.lt_tab_bar_icon_unselected)
        tabViews.forEachIndexed { index, imageView ->
            imageView.setColorFilter(if (index == selectedIndex) selectedColor else unselectedColor)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
```

- **Step 6: 编译验证**

Run: `./gradlew :core:uicomponent:assembleDebug`  
Expected: BUILD SUCCESSFUL

---

### Task 4: HomeCoordinator 双向绑定

**Files:**

- Modify: `app/src/main/kotlin/com/littlethingsandroidai/app/home/HomeCoordinator.kt`
- Create: `app/src/test/kotlin/com/littlethingsandroidai/app/home/HomeCoordinatorTest.kt`
- Modify: `app/build.gradle.kts`（测试依赖）
- **Step 1: 添加 Mockito 测试依赖**

`app/build.gradle.kts` → `dependencies`：

```kotlin
testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
testImplementation("org.robolectric:robolectric:4.14.1")
```

- **Step 2: 编写失败测试**

`HomeCoordinatorTest.kt`：

```kotlin
package com.littlethingsandroidai.app.home

import androidx.viewpager2.widget.ViewPager2
import com.littlethingsandroidai.core.uicomponent.LtHomeTabBar
import com.littlethingsandroidai.domain.coordinator.HomeRoute
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeCoordinatorTest {

    private val tabs =
        listOf(
            HomeRoute.CALENDAR,
            HomeRoute.THREAD,
            HomeRoute.INSIGHTS,
            HomeRoute.USER,
        )

    @Test
    fun push_thread_selectsIndex1() {
        val viewPager = mock<ViewPager2>()
        val tabBar = mock<LtHomeTabBar>()
        val coordinator = HomeCoordinator(viewPager, tabBar, tabs)

        coordinator.push(HomeRoute.THREAD)

        verify(viewPager).setCurrentItem(1, false)
        verify(tabBar).setSelectedIndex(1)
    }

    @Test
    fun popToRoot_selectsCalendar() {
        val viewPager = mock<ViewPager2>()
        val tabBar = mock<LtHomeTabBar>()
        val coordinator = HomeCoordinator(viewPager, tabBar, tabs)

        coordinator.popToRoot()

        verify(viewPager).setCurrentItem(0, false)
        verify(tabBar).setSelectedIndex(0)
    }

    @Test
    fun push_unknownRoute_doesNothing() {
        val viewPager = mock<ViewPager2>()
        val tabBar = mock<LtHomeTabBar>()
        val coordinator = HomeCoordinator(viewPager, tabBar, tabs)

        coordinator.push(com.littlethingsandroidai.domain.coordinator.PreHomeRoute.LOGIN)

        verify(viewPager, org.mockito.kotlin.never()).setCurrentItem(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }
}
```

- **Step 3: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.littlethingsandroidai.app.home.HomeCoordinatorTest"`  
Expected: FAIL（构造函数签名不匹配 / verify tabBar 未调用）

- **Step 4: 实现 `HomeCoordinator`**

```kotlin
package com.littlethingsandroidai.app.home

import androidx.viewpager2.widget.ViewPager2
import com.littlethingsandroidai.core.uicomponent.LtHomeTabBar
import com.littlethingsandroidai.domain.coordinator.Coordinator
import com.littlethingsandroidai.domain.coordinator.HomeRoute
import com.littlethingsandroidai.domain.coordinator.Route

class HomeCoordinator(
    private val viewPager: ViewPager2,
    private val tabBar: LtHomeTabBar,
    private val tabs: List<HomeRoute>,
) : Coordinator {

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabBar.setSelectedIndex(position)
            }
        }

    fun bind() {
        viewPager.isUserInputEnabled = false
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
        tabBar.setOnTabSelectedListener { index ->
            if (viewPager.currentItem != index) {
                viewPager.setCurrentItem(index, false)
            }
        }
    }

    fun unbind() {
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    }

    override fun push(route: Route) {
        if (route !is HomeRoute) return
        val targetIndex = tabs.indexOf(route)
        if (targetIndex >= 0) {
            viewPager.setCurrentItem(targetIndex, false)
            tabBar.setSelectedIndex(targetIndex)
        }
    }

    override fun pop() = Unit

    override fun popToRoot() {
        push(HomeRoute.CALENDAR)
    }
}
```

- **Step 5: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.littlethingsandroidai.app.home.HomeCoordinatorTest"`  
Expected: BUILD SUCCESSFUL，3 tests passed

---

### Task 5: HomeActivity 布局与 wiring

**Files:**

- Modify: `app/src/main/res/layout/activity_home.xml`
- Modify: `app/src/main/kotlin/com/littlethingsandroidai/app/home/HomeActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- **Step 1: 字符串资源**

`strings.xml` 追加：

```xml
<string name="home_tab_calendar">Calendar</string>
<string name="home_tab_thread">Thread</string>
<string name="home_tab_insights">Insights</string>
<string name="home_tab_user">User</string>
<string name="home_tab_calendar_desc">Calendar tab</string>
<string name="home_tab_thread_desc">Thread tab</string>
<string name="home_tab_insights_desc">Insights tab</string>
<string name="home_tab_user_desc">User tab</string>
```

- **Step 2: 重写 `activity_home.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/lt_oat"
    android:clipChildren="false"
    android:clipToPadding="false">

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/homeViewPager"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <com.littlethingsandroidai.core.uicomponent.LtHomeTabBar
        android:id="@+id/homeTabBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="41dp"
        android:layout_marginBottom="48dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

> 注：`lt_oat` 来自 `:core:uicomponent` R；app 模块已依赖 uicomponent，布局中引用 `@color/lt_oat` 需确保资源合并（或通过 theme 设置 background）。若 IDE 无法解析，在 `app/src/main/res/values/colors.xml` 添加 `<color name="lt_oat">#FFFDF8</color>` 别名或改用 `@android:color/white` 临时 — **优先**在 app 的 `themes.xml` 设置 `android:windowBackground` 为 lt_oat，Activity root 用 theme。

**更稳妥做法：** Activity 根布局 background 用 `@color/lt_oat`，在 `app/build.gradle.kts` 已 `implementation(project(":core:uicomponent"))`，Android Gradle Plugin 会合并 library 资源，可直接 `@color/lt_oat`。

- **Step 3: 重写 `HomeActivity.kt`**

```kotlin
package com.littlethingsandroidai.app.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.littlethingsandroidai.R
import com.littlethingsandroidai.app.observeSessionExpiration
import com.littlethingsandroidai.core.uicomponent.HomeTabItem
import com.littlethingsandroidai.core.uicomponent.R as UiR
import com.littlethingsandroidai.databinding.ActivityHomeBinding
import com.littlethingsandroidai.domain.coordinator.HomeRoute
import com.littlethingsandroidai.domain.home.HomeTabAdapter

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var coordinator: HomeCoordinator
    private lateinit var userHomeCoordinator: UserHomeCoordinator

    private val tabs =
        listOf(
            HomeRoute.CALENDAR,
            HomeRoute.THREAD,
            HomeRoute.INSIGHTS,
            HomeRoute.USER,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyNavigationBarInsets()
        setupViewPager()
        setupTabBar()
        coordinator = HomeCoordinator(binding.homeViewPager, binding.homeTabBar, tabs)
        coordinator.bind()
        coordinator.push(HomeRoute.CALENDAR)
        userHomeCoordinator = UserHomeCoordinator()
        observeSessionExpiration()
    }

    override fun onDestroy() {
        if (::coordinator.isInitialized) {
            coordinator.unbind()
        }
        super.onDestroy()
    }

    private fun applyNavigationBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeTabBar) { view, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = navBars.bottom)
            insets
        }
    }

    private fun setupViewPager() {
        binding.homeViewPager.adapter = HomeTabAdapter(this, tabs)
        binding.homeViewPager.isUserInputEnabled = false
        binding.homeViewPager.offscreenPageLimit = tabs.size - 1
    }

    private fun setupTabBar() {
        binding.homeTabBar.setTabs(
            listOf(
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_calendar_placeholder,
                    contentDescription = getString(R.string.home_tab_calendar_desc),
                ),
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_thread_placeholder,
                    contentDescription = getString(R.string.home_tab_thread_desc),
                ),
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_insights_placeholder,
                    contentDescription = getString(R.string.home_tab_insights_desc),
                ),
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_user_placeholder,
                    contentDescription = getString(R.string.home_tab_user_desc),
                ),
            ),
        )
    }
}
```

- **Step 4: 编译验证**

Run: `./gradlew :app:assembleDebug`  
Expected: BUILD SUCCESSFUL

---

### Task 6: 占位 Fragment 微调

**Files:**

- Modify: `app/src/main/res/layout/fragment_placeholder_tab.xml`
- Modify: `app/src/main/kotlin/com/littlethingsandroidai/domain/home/PlaceholderTabFragment.kt`
- **Step 1: 更新占位布局**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/lt_oat">

    <TextView
        android:id="@+id/tabNameText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:textColor="@color/lt_grey_dark"
        android:textSize="18sp" />
</FrameLayout>
```

- **Step 2: 使用 string 资源显示 Tab 名**

`PlaceholderTabFragment.kt` — `onViewCreated` 中：

```kotlin
val tabName = arguments?.getString(ARG_TAB_NAME).orEmpty()
binding.tabNameText.text = tabName
```

`HomeTabAdapter.kt` — `createFragment` 传入可读名称：

```kotlin
override fun createFragment(position: Int): PlaceholderTabFragment =
    PlaceholderTabFragment.newInstance(
        tabName = tabs[position].name.lowercase().replaceFirstChar(Char::titlecase),
    )
```

（若已如此则仅确认背景色生效。）

- **Step 3: 手动冒烟**

1. Mock 登录（`SignInDevConfig.MOCK_GOOGLE_SIGN_IN = true`）进入 Home
2. 默认显示 Calendar 占位
3. 点击四 Tab 切换，ViewPager 不可滑动
4. TabBar 黑色胶囊、底部浮动、icon 选中高亮

---

### Task 7: 全量验证与文档

**Files:**

- Modify: `LittleThingsAndroidAI/README.md`（可选一行 Home 壳层说明）
- Modify: `docs/app-startup-flow.md`（Home 一节补充 TabBar）
- **Step 1: 运行全量单元测试**

Run:

```bash
cd LittleThingsAndroidAI
./gradlew :app:testDebugUnitTest :core:uicomponent:assembleDebug :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- **Step 2: 对照 spec §8 交付标准逐项勾选**
- **Step 3: README 追加 Home 壳层说明（1 段）**

```markdown
## Home Shell

After sign-in, `HomeActivity` shows a Figma-aligned floating bottom tab bar (`LtHomeTabBar`) with four placeholder tabs. See [docs/superpowers/specs/2026-07-05-android-home-shell-design.md](docs/superpowers/specs/2026-07-05-android-home-shell-design.md).
```

---

## Spec Coverage Checklist


| Spec 要求              | Task         |
| -------------------- | ------------ |
| 移除顶部 TabLayout       | Task 5       |
| 底部黑色胶囊 TabBar        | Task 2, 3, 5 |
| ViewPager 禁滑动        | Task 4, 5    |
| 四 Tab 占位             | Task 6       |
| 系统 icon 占位 + 命名预留    | Task 2       |
| `lt_oat` token       | Task 1       |
| HomeCoordinator 双向同步 | Task 4       |
| WindowInsets nav bar | Task 5       |
| HomeCoordinatorTest  | Task 4       |
| assembleDebug 通过     | Task 7       |


---

## Commit 建议（实现完成后，用户要求时再提交）

```bash
git add core/uicomponent app/src docs/superpowers/plans/2026-07-05-android-home-shell.md
git commit -m "feat: add Home shell with floating bottom tab bar"
```

