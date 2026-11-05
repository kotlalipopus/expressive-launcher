package com.example.expressivelauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** Клик с пружинящим «вдавливанием» и координатами элемента (для анимации запуска и привязки меню). */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.bouncyClickable(
    onClick: (Rect) -> Unit,
    onLongClick: ((Rect) -> Unit)? = null,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "press",
    )
    val bounds = remember { arrayOf(Rect.Zero) }
    val haptics = com.example.expressivelauncher.ui.LocalHaptics.current
    return this
        .onGloballyPositioned { bounds[0] = it.boundsInRoot() }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            // Вибрацией управляют настройки лаунчера, встроенная сработала бы и при выключенной
            hapticFeedbackEnabled = false,
            onClick = { onClick(bounds[0]) },
            onLongClick = onLongClick?.let { l -> { haptics.play(HapticKind.HEAVY, HapticGroup.TOUCH); l(bounds[0]) } },
        )
}
