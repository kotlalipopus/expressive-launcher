package com.example.expressivelauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.ui.components.AppIconImage
import com.example.expressivelauncher.ui.home.APP_PREFIX
import com.example.expressivelauncher.ui.home.AiPackages
import com.example.expressivelauncher.ui.home.PixelSearchBar
import com.example.expressivelauncher.ui.home.SearchTool
import com.example.expressivelauncher.ui.home.ToolGlyph

private const val MAX_BUTTONS = 4

/** Строка поиска на столе: что открывает нажатие и какие кнопки стоят справа. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SearchBarPage(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val apps by vm.allApps.collectAsState()
    val appMap = remember(apps) { apps.associateBy { it.key } }
    val buttons = s.searchButtons
    fun setButtons(list: List<String>) = vm.updateSettings { it.copy(searchButtons = list) }
    val full = buttons.size >= MAX_BUTTONS

    SettingsSection(stringResource(R.string.preview)) {
        GroupItem {
            Box(Modifier.padding(12.dp)) {
                PixelSearchBar(appMap, onAppsSearch = {}, onApps = {}, onLaunchApp = { _, _ -> }, modifier = Modifier)
            }
        }
    }
    SettingsSection(null) {
        // Строку можно убрать совсем — тогда в доке остаются только значки
        ToggleItem(Icons.Rounded.Search, stringResource(R.string.sb_show), null, s.showSearchPill) { v ->
            vm.updateSettings { it.copy(showSearchPill = v) }
        }
        ToggleItem(Icons.Rounded.Search, stringResource(R.string.sb_google), stringResource(R.string.sb_google_hint), s.searchGoogle) { v ->
            vm.updateSettings { it.copy(searchGoogle = v) }
        }
    }
    // Поисковик строки поиска (и поиска в интернете из меню приложений)
    if (s.searchGoogle) SettingsSection(stringResource(R.string.search_engine)) {
        com.example.expressivelauncher.data.SearchEngines.forEachIndexed { i, e ->
            GroupItem(onClick = { vm.updateSettings { it.copy(searchEngine = i) } }) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    com.example.expressivelauncher.ui.home.EngineMark(e, Modifier.size(28.dp))
                    Text(e.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (s.searchEngine == i) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    SettingsSection(stringResource(R.string.sb_buttons)) {
        if (buttons.isEmpty()) {
            GroupItem { Text(stringResource(R.string.sb_empty), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        buttons.forEachIndexed { i, id ->
            GroupItem {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ButtonIcon(id, appMap)
                    Text(buttonTitle(id, appMap), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { setButtons(buttons.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.KeyboardArrowUp, null)
                    }
                    IconButton(onClick = { setButtons(buttons.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < buttons.lastIndex, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.KeyboardArrowDown, null)
                    }
                    IconButton(onClick = { setButtons(buttons - id) }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Close, stringResource(R.string.sb_remove)) }
                }
            }
        }
    }

    // Встроенные кнопки, которых ещё нет
    val freeTools = SearchTool.entries.filter { it.id !in buttons }
    if (freeTools.isNotEmpty()) SettingsSection(stringResource(R.string.sb_add_builtin)) {
        for (t in freeTools) AddRow(full, { ToolGlyph(t, 24.dp) }, stringResource(t.title)) { setButtons(buttons + t.id) }
    }

    // Приложения: сначала ИИ-помощники, потом все остальные с поиском
    var query by rememberSaveable { mutableStateOf("") }
    val notAdded = apps.filter { APP_PREFIX + it.key !in buttons }
    val ai = notAdded.filter { it.packageName in AiPackages }.sortedBy { AiPackages.indexOf(it.packageName) }
    if (ai.isNotEmpty()) SettingsSection(stringResource(R.string.sb_add_ai)) {
        for (a in ai) AddRow(full, { AppIconImage(a, 32.dp, showBadge = false) }, a.label) { setButtons(buttons + (APP_PREFIX + a.key)) }
    }
    SettingsSection(stringResource(R.string.sb_add_app)) {
        GroupItem {
            OutlinedTextField(
                query, { query = it },
                Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                shape = CircleShape,
            )
        }
        val q = query.trim().lowercase()
        val list = notAdded.filter { it.packageName !in AiPackages && (q.isEmpty() || q in it.sortKey) }
        for (a in list.take(if (q.isEmpty()) 30 else 60)) {
            AddRow(full, { AppIconImage(a, 32.dp, showBadge = false) }, a.label) { setButtons(buttons + (APP_PREFIX + a.key)) }
        }
    }
}

@Composable
private fun AddRow(full: Boolean, icon: @Composable () -> Unit, title: String, onAdd: () -> Unit) {
    GroupItem(onClick = if (full) null else onAdd) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { icon() }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onAdd, enabled = !full, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Add, stringResource(R.string.sb_add)) }
        }
    }
}

@Composable
private fun ButtonIcon(id: String, appMap: Map<String, AppInfo>) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val tool = SearchTool.of(id)
        val app = appMap[id.removePrefix(APP_PREFIX)]
        when {
            tool != null -> ToolGlyph(tool, 22.dp)
            app != null -> AppIconImage(app, 30.dp, showBadge = false)
        }
    }
}

@Composable
private fun buttonTitle(id: String, appMap: Map<String, AppInfo>): String {
    SearchTool.of(id)?.let { return stringResource(it.title) }
    return appMap[id.removePrefix(APP_PREFIX)]?.label ?: stringResource(R.string.sb_missing)
}
