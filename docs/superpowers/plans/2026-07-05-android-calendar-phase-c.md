# Calendar Phase C Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 交付 Calendar Phase C — 1/2/3/4+ stamp 布局 + Coil 图片加载、最小可读详情页、`POST /api/answers/icons/:id/read`。

**Architecture:** `CalendarStampBinder` 在 day cell 内按 reflection 数量布局；Coil 加载 icon URL；点击 stamp → `CalendarFragment` child FM push `ReflectionDetailFragment`；`ReflectionDetailViewModel` 延迟 500ms 调用 `MarkIconReadUseCase`。

**Tech Stack:** Kotlin、ViewBinding、Coil 3、Fragment child FM、MockWebServer

**Spec:** [2026-07-05-android-calendar-design.md](../specs/2026-07-05-android-calendar-design.md) §3.4 / §3.7 / §9 Phase C

---

## Phase C 交付检查清单

- [x] Coil 依赖 + `CalendarStampBinder`（1/2/3/4+ 布局）
- [x] `IconRepository` + `MarkIconReadUseCase` → `POST /api/answers/icons/:id/read`
- [x] `ReflectionDetailFragment` + ViewModel（方案 B 最小可读）
- [x] Calendar stamp 点击 → 详情 + markRead
- [x] 单元测试 + `./gradlew :app:testDebugUnitTest` 通过

---

## 主要文件

| 文件 | 说明 |
|------|------|
| `domain/calendar/CalendarStampBinder.kt` | stamp 布局 + Coil |
| `domain/calendar/detail/ReflectionDetailFragment.kt` | 详情页 |
| `domain/calendar/detail/ReflectionDetailViewModel.kt` | 展示 + markRead |
| `service/icon/*` | Icon API 层 |
| `res/layout/fragment_reflection_detail.xml` | 详情布局 |
| `fragment_calendar.xml` | `calendarDetailContainer`  overlay |
