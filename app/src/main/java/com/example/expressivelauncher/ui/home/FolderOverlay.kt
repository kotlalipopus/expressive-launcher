@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.home

import com.example.expressivelauncher.ui.components.accentGlow
import com.example.expressivelauncher.data.displayLabel
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.HomeItem
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.LocalFx
import com.example.expressivelauncher.ui.components.AppIconImage
import com.example.expressivelauncher.ui.components.AppLabel
import com.example.expressivelauncher.ui.components.bouncyClickable
import com.example.expressivelauncher.ui.components.staggerIn

/** Папка вырастает из своего значка ([origin] в долях экрана); затемнение только проявляется и не масштабируется. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityScope.FolderOverlay(
    folder: HomeItem,
    appMap: Map<String, AppInfo>,
    vm: LauncherViewModel,
    origin: TransformOrigin,
    onDismiss: () -> Unit,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
) {
    val settings = LocalSettings.current
    val apps = folder.children.mapNotNull(appMap::get)
    val default = stringResource(R.string.folder_default_name)
    var title by remember(folder.id) { mutableStateOf(folder.title) }
    val motion = MaterialTheme.motionScheme
    val cs = MaterialTheme.colorScheme
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val iconSize = (56 * settings.iconScale).dp
    val iconSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { iconSize.toPx() }
    val iconTopPx = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }
    val blur = settings.blurBackground && LocalFx.current.blur
    // Lite: никаких полупрозрачных слоёв поверх обоев — папка просто сплошная панель
    val lite = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.lite

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (lite) 0f else if (blur) 0.18f else 0.28f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        )
        Box(
            Modifier.fillMaxSize().animateEnterExit(
                enter = scaleIn(motion.defaultSpatialSpec(), initialScale = 0.3f, transformOrigin = origin) + fadeIn(motion.fastEffectsSpec()),
                // Закрытие — с ускорением, без пружины: папка «втягивается» в значок и гаснет у самого значка
                exit = scaleOut(
                    androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)),
                    targetScale = 0.25f, transformOrigin = origin,
                ) + fadeOut(androidx.compose.animation.core.tween(150, delayMillis = 70)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            com.example.expressivelauncher.ui.components.GlassPanel(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(44.dp),
                color = cs.surfaceContainerHigh,
                glassAlpha = 0.68f,
                solidAlpha = 0.92f,
                // Стол под папкой уже размыт одним слоем: сквозь стекло видно его, обои не подкладываем
                overBlur = blur,
                modifier = Modifier.padding(20.dp).widthIn(max = 420.dp).fillMaxWidth()
                    .accentGlow(androidx.compose.foundation.shape.RoundedCornerShape(44.dp))
                    .clickable(interactionSource = null, indication = null) { },
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it.take(24); vm.renameFolder(folder.id, title) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmallEmphasized.copy(
                            color = cs.onSurface, textAlign = TextAlign.Center,
                        ),
                        cursorBrush = SolidColor(cs.primary),
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (title.isEmpty()) {
                                    Text(
                                        default, style = MaterialTheme.typography.headlineSmallEmphasized,
                                        color = cs.onSurface.copy(alpha = 0.5f),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(apps, key = { _, a -> a.key }) { i, app ->
                            var menu by remember { mutableStateOf(false) }
                            Column(
                                Modifier
                                    .animateItem()
                                    .staggerIn(i, stepMs = 30L)
                                    .bouncyClickable({ r -> onLaunch(app, Rect(r.center.x - iconSizePx / 2f, r.top + iconTopPx, r.center.x + iconSizePx / 2f, r.top + iconTopPx + iconSizePx)) }, { menu = true })
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AppIconImage(app, iconSize)
                                AppLabel(app.displayLabel, Modifier.padding(top = 4.dp))
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = MaterialTheme.shapes.large) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_take_out)) },
                                        onClick = { menu = false; vm.removeFromFolder(folder.id, app.key) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_app_info)) },
                                        onClick = { menu = false; onAppInfo(app) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
