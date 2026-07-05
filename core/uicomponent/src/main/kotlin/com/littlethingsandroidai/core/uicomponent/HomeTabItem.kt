package com.littlethingsandroidai.core.uicomponent

import androidx.annotation.DrawableRes

data class HomeTabItem(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
)
