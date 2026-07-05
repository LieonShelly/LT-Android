# Calendar Phase D Implementation Plan

**Goal:** TodayQuestion 浮层 + `GET /api/questions-of-the-day` + Add stub。

**Spec:** [2026-07-05-android-calendar-design.md](../specs/2026-07-05-android-calendar-design.md) §3.8

## 检查清单

- [x] `FetchTodayQuestionsUseCase` + Repository API
- [x] `view_calendar_today_question.xml` 浮层（避让 TabBar）
- [x] `CalendarViewModel.fetchTodayQuestions()` + 展开更多
- [x] Add / 点击问题 → Toast stub
- [x] 单元测试 + build 通过
