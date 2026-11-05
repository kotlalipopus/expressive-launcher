package com.example.expressivelauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.ui.theme.LocalThemeSpec
import kotlinx.coroutines.delay

/**
 * Короткое сообщение внутри лаунчера вместо системного Toast: системный на Android 11+ нельзя оформить,
 * а этот — стеклянная «пилюля» в Liquid Glass и тональная поверхность в остальных темах.
 */
object LauncherToast {
    internal var text by mutableStateOf<String?>(null)
    /** Растёт с каждым сообщением: одинаковый текст подряд тоже показывается заново. */
    internal var serial by mutableIntStateOf(0)

    fun show(message: String) {
        text = message
        serial++
    }
}

@Composable
fun LauncherToastHost() {
    var visible by remember { mutableStateOf(false) }
    val serial = LauncherToast.serial
    LaunchedEffect(serial) {
        if (serial == 0) return@LaunchedEffect
        visible = true
        delay(2600)
        visible = false
    }
    val lite = LocalThemeSpec.current.lite
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = if (lite) fadeIn(tween(150)) else fadeIn(tween(200)) + scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.9f) +
                slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 2 },
            exit = if (lite) fadeOut(tween(150)) else fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.94f) +
                slideOutVertically(tween(180)) { it / 3 },
        ) {
            ThemedSurface(
                CircleShape, MaterialTheme.colorScheme.inverseSurface,
                Modifier.widthIn(max = 340.dp).padding(horizontal = 24.dp),
                contentColor = if (LocalThemeSpec.current.glass) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 3.dp,
            ) {
                Text(
                    LauncherToast.text.orEmpty(),
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
