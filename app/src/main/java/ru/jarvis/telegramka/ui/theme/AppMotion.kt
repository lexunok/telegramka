package ru.jarvis.telegramka.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

private val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

object AppMotion {
    const val EnterDuration = 420
    const val ExitDuration = 260
    const val ContentDuration = 320

    fun screenEnter(offsetDivisor: Int = 12): EnterTransition =
        fadeIn(
            animationSpec = tween(durationMillis = EnterDuration, easing = StandardEasing)
        ) + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / offsetDivisor },
            animationSpec = tween(durationMillis = EnterDuration, easing = StandardEasing)
        )

    fun screenExit(offsetDivisor: Int = 18): ExitTransition =
        fadeOut(
            animationSpec = tween(durationMillis = ExitDuration, easing = StandardEasing)
        ) + slideOutVertically(
            targetOffsetY = { fullHeight -> -(fullHeight / offsetDivisor) },
            animationSpec = tween(durationMillis = ExitDuration, easing = StandardEasing)
        )
}
