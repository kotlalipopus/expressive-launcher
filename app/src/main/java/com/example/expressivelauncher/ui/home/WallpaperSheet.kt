package com.example.expressivelauncher.ui.home

import com.example.expressivelauncher.ui.components.accentGlow
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.ui.components.CustomWallpaper
import com.example.expressivelauncher.ui.components.ThemedSurface
import com.example.expressivelauncher.ui.settings.ActionItem
import com.example.expressivelauncher.ui.settings.LauncherWallpaperItems
import com.example.expressivelauncher.ui.settings.SettingsSection

/**
 * Своё меню «Обои» (долгое нажатие на стол → «Обои»): вместо системного выбора — стеклянная панель лаунчера.
 * Превью текущей картинки, выбор фото, включение своих обоев, затемнение, размытие, листание вместе со страницами,
 * «поставить и на системные обои» и переход к системным (живым) обоям.
 */
@Composable
fun AnimatedVisibilityScope.WallpaperSheet(vm: LauncherViewModel, onDismiss: () -> Unit, onSystemPicker: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val openedAt = remember { android.os.SystemClock.uptimeMillis() }
    Box(
        Modifier.fillMaxSize().background(cs.scrim.copy(alpha = 0.32f)).clickable(interactionSource = null, indication = null) {
            if (android.os.SystemClock.uptimeMillis() - openedAt > MENU_GUARD_MS) onDismiss()
        },
    )
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars), contentAlignment = Alignment.BottomCenter) {
        ThemedSurface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            color = cs.surfaceContainer,
            contentColor = cs.onSurface,
            shadowElevation = 3.dp,
            blurPanel = false,
            modifier = Modifier.padding(12.dp).fillMaxWidth().heightIn(max = 620.dp)
                .accentGlow(MaterialTheme.shapes.extraLargeIncreased)
                .animateEnterExit(
                enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
                exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
            ),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Превью: та же картинка, что на столе (с размытием, если оно включено)
                    val image = CustomWallpaper.image
                    Box(
                        Modifier.size(width = 64.dp, height = 136.dp).clip(RoundedCornerShape(14.dp)).background(cs.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.home_wallpaper), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(if (image != null) R.string.custom_wallpaper_hint else R.string.wallpaper_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
                SettingsSection(null) {
                    LauncherWallpaperItems(vm)
                    ActionItem(
                        Icons.Rounded.Wallpaper,
                        stringResource(R.string.wallpaper_system_picker),
                        stringResource(R.string.wallpaper_system_picker_hint),
                        onClick = onSystemPicker,
                    )
                }
            }
        }
    }
}
