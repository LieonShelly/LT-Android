package com.littlethingsandroidai.core.uicomponent

import androidx.annotation.ColorInt

object AppColors {
    /** Mirrors [R.color.lt_background] — iOS AppColor.backgroundPage / defaultBackground. */
    @ColorInt
    const val background: Int = 0xFFF5F0E8.toInt()

    /** Mirrors [R.color.lt_white] — iOS AppColor.white. */
    @ColorInt
    const val white: Int = 0xFFFFFFFF.toInt()

    /** Mirrors [R.color.lt_grey_dark] — SignIn button label, iOS AppColor.greyDark approx. */
    @ColorInt
    const val greyDark: Int = 0xFF1D1D1D.toInt()

    /** Mirrors [R.color.lt_border] — SignIn Google button stroke, iOS hex 0x1D1D1D. */
    @ColorInt
    const val border: Int = 0xFF1D1D1D.toInt()
}
