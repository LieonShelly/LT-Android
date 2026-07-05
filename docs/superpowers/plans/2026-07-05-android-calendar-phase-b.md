# Calendar Phase B Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 交付 Calendar Phase B — 月份选择器、Pull-to-refresh、未来月 monthLock（对齐 spec §3.2 / §3.3 / §3.6）。

**Architecture:** 在 Phase A 基础上扩展 `CalendarViewModel` 状态（`isMonthPickerVisible`）；Header 点击展开 `CalendarMonthPickerAdapter`；`SwipeRefreshLayout` 包裹 `ViewPager2`；`CalendarMonthPagerAdapter` 对 `isFuture` 月显示 lock 占位。

**Tech Stack:** Kotlin、ViewBinding、SwipeRefreshLayout、RecyclerView、ViewPager2

**Spec:** [2026-07-05-android-calendar-design.md](../specs/2026-07-05-android-calendar-design.md)

**Status:** ✅ Implemented (2026-07-05)

---

## Phase B 交付检查清单

- [x] Header 月份 + Chevron 点击展开/收起 MonthPicker
- [x] Chevron 旋转 180°；选中月份后切 ViewPager + 收起
- [x] 年份分隔行（`YEAR_PLACEHOLDER` → YearHeader）
- [x] `SwipeRefreshLayout` 下拉刷新当前月（未来月跳过 API）
- [x] 未来月 monthLock：「The best is yet to come」
- [x] 未来月 Weekday 行置灰（`#CDCDCD`）
- [x] `CalendarViewModelTest` 扩展（picker / isFuture）
- [x] `./gradlew :app:testDebugUnitTest` 通过

---

## 主要文件

| 文件 | 说明 |
|------|------|
| `CalendarMonthPickerAdapter.kt` | 年份 + 月份列表 |
| `CalendarViewModel.kt` | `toggleMonthPicker` / `selectMonth` / `refreshCurrentMonth` |
| `CalendarFragment.kt` | Header / Picker / SwipeRefresh wiring |
| `CalendarMonthPagerAdapter.kt` | monthLock UI |
| `fragment_calendar.xml` | SwipeRefresh + monthPicker include |
| `view_calendar_month_picker.xml` | Picker RecyclerView |
| `item_calendar_month_page.xml` | monthLockMessage overlay |

---

## 后续

Phase C：Coil stamp 布局 + 详情页 + markIconRead
