package com.littlethingsandroidai.domain.coordinator

sealed interface Route

enum class PreHomeRoute : Route {
    LOGIN,
    SPLASH,
    ONBOARDING,
    WELCOME,
    FIRST_QUESTION,
}

enum class HomeRoute : Route {
    CALENDAR,
    THREAD,
    INSIGHTS,
    USER,
}

enum class UserRoute : Route {
    PROFILE,
    SETTINGS,
}
