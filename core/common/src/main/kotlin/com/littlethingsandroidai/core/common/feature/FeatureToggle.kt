package com.littlethingsandroidai.core.common.feature

class FeatureToggle(private val currentStage: FeatureRolloutStage) {
    fun isEnabled(config: LTAppFeatureConfig): Boolean =
        config.stage.ordinal <= currentStage.ordinal
}
