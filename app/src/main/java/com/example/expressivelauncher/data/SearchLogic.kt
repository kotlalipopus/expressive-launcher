package com.example.expressivelauncher.data

/** Нечёткий поиск по приложениям: префикс > начало слова > подстрока > инициалы. */
object SearchLogic {

    fun searchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return apps.asSequence()
            .map { it to score(it, q) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<AppInfo, Int>> { it.second }.thenBy { it.first.sortKey })
            .map { it.first }
            .toList()
    }

    private fun score(app: AppInfo, q: String): Int {
        val name = app.sortKey
        if (name == q) return 100
        if (name.startsWith(q)) return 90
        if (name.split(' ', '-', '_', '.').any { it.startsWith(q) }) return 70
        if (name.contains(q)) return 50
        val initials = name.split(' ', '-', '_', '.').filter { it.isNotEmpty() }.joinToString("") { it.take(1) }
        if (q.length >= 2 && initials.startsWith(q)) return 40
        if (q.length >= 3 && app.packageName.lowercase().contains(q)) return 20
        return 0
    }
}
