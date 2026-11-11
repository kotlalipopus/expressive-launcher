package com.example.expressivelauncher.ui.home

import com.example.expressivelauncher.ui.components.accentGlow
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.AppStyle
import com.example.expressivelauncher.data.AppStyles
import com.example.expressivelauncher.ui.LocalRepo
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.components.AppIconImage
import com.example.expressivelauncher.ui.components.ThemedSurface
import com.example.expressivelauncher.ui.settings.ActionItem
import com.example.expressivelauncher.ui.settings.GroupItem
import com.example.expressivelauncher.ui.settings.SettingsSection
import com.example.expressivelauncher.ui.settings.ShapePicker
import com.example.expressivelauncher.ui.settings.SliderItem
import com.example.expressivelauncher.ui.settings.ToggleItem
import kotlin.math.roundToInt

/** Какое приложение сейчас настраивается; меню значка (на столе и в меню приложений) задаёт, стол показывает панель. */
object AppCustomize {
    var app by mutableStateOf<AppInfo?>(null)
}

/**
 * Настройка одного приложения под себя: картинка значка, форма, размер, своё название и подпись.
 * Всё применяется сразу — значок на столе меняется, пока панель открыта.
 */
@Composable
fun AnimatedVisibilityScope.AppStyleSheet(app: AppInfo, onChangeIcon: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val repo = LocalRepo.current
    val settings = LocalSettings.current
    val style = AppStyles.of(app.key) ?: AppStyle()
    fun update(f: (AppStyle) -> AppStyle) = AppStyles.set(context, app.key, f(AppStyles.of(app.key) ?: AppStyle()))
    val openedAt = remember { android.os.SystemClock.uptimeMillis() }
    Box(
        Modifier.fillMaxSize().background(cs.scrim.copy(alpha = 0.32f)).clickable(interactionSource = null, indication = null) {
            if (android.os.SystemClock.uptimeMillis() - openedAt > MENU_GUARD_MS) onDismiss()
        },
    )
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ThemedSurface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            color = cs.surfaceContainer,
            contentColor = cs.onSurface,
            shadowElevation = 3.dp,
            blurPanel = false,
            modifier = Modifier.padding(12.dp).fillMaxWidth().heightIn(max = 640.dp)
                .accentGlow(MaterialTheme.shapes.extraLargeIncreased)
                .animateEnterExit(
                enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
                exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
            ),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Превью: значок так, как он выглядит на столе — с выбранной формой и размером
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) { AppIconImage(app, 64.dp, showBadge = false) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_style_title), style = MaterialTheme.typography.headlineSmall)
                        Text(app.label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
                SettingsSection(null) {
                    ActionItem(Icons.Rounded.Image, stringResource(R.string.menu_change_icon), stringResource(R.string.app_style_icon_hint), onClick = { onChangeIcon(app) })
                    if (repo.hasOverride(app)) {
                        ActionItem(Icons.Rounded.Restore, stringResource(R.string.menu_reset_icon), null, onClick = { repo.clearOverride(app) })
                    }
                }
                SettingsSection(stringResource(R.string.app_style_shape)) {
                    GroupItem {
                        Column(Modifier.padding(top = 8.dp)) {
                            ShapePicker(style.shape ?: settings.iconShape) { v -> update { it.copy(shape = v) } }
                        }
                    }
                    if (style.shape != null) {
                        ActionItem(Icons.Rounded.Restore, stringResource(R.string.app_style_shape_default), null, onClick = { update { it.copy(shape = null) } })
                    }
                    SliderItem(Icons.Rounded.PhotoSizeSelectLarge, stringResource(R.string.app_style_size), style.scale, 0.6f..1.4f, { "${(it * 100).roundToInt()}%" }, steps = 7) { v ->
                        update { it.copy(scale = v) }
                    }
                }
                SettingsSection(stringResource(R.string.app_style_name)) {
                    GroupItem {
                        var text by remember(app.key) { mutableStateOf(style.label ?: "") }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { v -> text = v; update { it.copy(label = v.takeIf { s -> s.isNotBlank() }) } },
                            placeholder = { Text(app.label) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            leadingIcon = { androidx.compose.material3.Icon(Icons.Rounded.TextFields, null) },
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                    ToggleItem(Icons.Rounded.TextFields, stringResource(R.string.app_style_show_label), null, !style.hideLabel) { on ->
                        update { it.copy(hideLabel = !on) }
                    }
                }
                if (!style.isDefault || repo.hasOverride(app)) {
                    SettingsSection(null) {
                        ActionItem(Icons.Rounded.Restore, stringResource(R.string.app_style_reset), null, onClick = {
                            AppStyles.set(context, app.key, AppStyle())
                            if (repo.hasOverride(app)) repo.clearOverride(app)
                        })
                    }
                }
            }
        }
    }
}
