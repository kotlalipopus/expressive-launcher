package com.example.expressivelauncher.data

import android.net.Uri

/**
 * Поисковик: [urlTemplate] — поиск по запросу (меню приложений), [home] — главная страница (строка поиска на столе),
 * [packages] — его приложения: строка поиска открывает приложение, если оно стоит, иначе сайт.
 * [mark] и [color] — значок в строке поиска (у Google свой многоцветный).
 */
data class SearchEngine(
    val name: String,
    val urlTemplate: String,
    val home: String,
    val packages: List<String> = emptyList(),
    val mark: String = name.take(1),
    val color: Long = 0xFF5F6368,
) {
    fun url(query: String): String = urlTemplate + Uri.encode(query)
}

val SearchEngines = listOf(
    SearchEngine("Google", "https://www.google.com/search?q=", "https://www.google.com/", listOf("com.google.android.googlequicksearchbox"), "G", 0xFF4285F4),
    SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=", "https://duckduckgo.com/", listOf("com.duckduckgo.mobile.android"), "D", 0xFFDE5833),
    SearchEngine("Bing", "https://www.bing.com/search?q=", "https://www.bing.com/", listOf("com.microsoft.bing"), "b", 0xFF008373),
    SearchEngine("Яндекс", "https://yandex.ru/search/?text=", "https://ya.ru/", listOf("ru.yandex.searchplugin", "com.yandex.browser"), "Я", 0xFFFC3F1D),
    SearchEngine("Brave", "https://search.brave.com/search?q=", "https://search.brave.com/", listOf("com.brave.browser"), "B", 0xFFFB542B),
    SearchEngine("Perplexity", "https://www.perplexity.ai/search?q=", "https://www.perplexity.ai/", listOf("ai.perplexity.app.android"), "P", 0xFF20808D),
)
