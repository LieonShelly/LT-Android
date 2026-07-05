package com.littlethingsandroidai.core.common.feature

enum class LTAppFeatureConfig {
    LOGOUT,
    INSIGHTS,
    CALENDAR_VIEW,
    THREAD;

    val stage: FeatureRolloutStage
        get() = when (this) {
            LOGOUT -> FeatureRolloutStage.UNDER_DEVELOPMENT
            INSIGHTS -> FeatureRolloutStage.INTERNAL
            CALENDAR_VIEW -> FeatureRolloutStage.UNDER_DEVELOPMENT
            THREAD -> FeatureRolloutStage.RELEASE
        }
}
