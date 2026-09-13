package app.snake.scores

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScoreRow(
    val boardKey: String,
    val name: String,
    val score: Int,
    val at: Long,
    val win: Boolean = true,
)

data class ScorePrompt(
    val boardKey: String,
    val score: Int,
    val lowerBetter: Boolean,
    val headline: String,
    val detail: String,
    val win: Boolean = true,
)

class ScoreStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var lastName: String
        get() = prefs.getString(LAST_NAME, "") ?: ""
        set(value) {
            prefs.edit().putString(LAST_NAME, value.trim().take(NAME_MAX)).apply()
        }

    fun list(boardKey: String): List<ScoreRow> = all().filter { it.boardKey == boardKey }

    fun top(boardKey: String, lowerBetter: Boolean, limit: Int = PODIUM): List<ScoreRow> {
        val pool = list(boardKey).let { rows ->
            if (lowerBetter) rows.filter { it.win } else rows
        }
        return sort(pool, lowerBetter).take(limit)
    }

    fun history(boardKey: String): List<ScoreRow> =
        list(boardKey).sortedByDescending { it.at }

    fun best(boardKey: String, lowerBetter: Boolean): ScoreRow? =
        top(boardKey, lowerBetter, 1).firstOrNull()

    fun beatsBest(boardKey: String, score: Int, lowerBetter: Boolean, win: Boolean = true): Boolean {
        if (lowerBetter && !win) return false
        val champ = best(boardKey, lowerBetter) ?: return true
        return if (lowerBetter) score < champ.score else score > champ.score
    }

    fun knownNames(): List<String> {
        val latest = LinkedHashMap<String, Long>()
        all().forEach { row ->
            val prev = latest[row.name]
            if (prev == null || row.at > prev) latest[row.name] = row.at
        }
        if (lastName.isNotBlank()) latest.putIfAbsent(lastName, Long.MAX_VALUE)
        return latest.entries.sortedByDescending { it.value }.map { it.key }
    }

    fun submit(boardKey: String, name: String, score: Int, win: Boolean = true): ScoreRow {
        val cleaned = name.trim().ifBlank { "PLAYER" }.take(NAME_MAX)
        lastName = cleaned
        val row = ScoreRow(boardKey, cleaned, score, System.currentTimeMillis(), win)
        saveAll(all() + row)
        return row
    }

    fun migrateAnonymous(boardKey: String, score: Int) {
        if (score <= 0) return
        if (list(boardKey).isNotEmpty()) return
        submit(boardKey, "PLAYER", score, win = true)
    }

    private fun all(): List<ScoreRow> {
        val unified = prefs.getString(RUNS, null)
        if (unified != null) {
            return runCatching { parseRuns(unified) }.getOrDefault(emptyList())
        }
        val migrated = migrateLegacy()
        if (migrated.isNotEmpty()) saveAll(migrated)
        return migrated
    }

    private fun saveAll(rows: List<ScoreRow>) {
        prefs.edit().putString(RUNS, stringify(rows)).apply()
    }

    private fun migrateLegacy(): List<ScoreRow> {
        val out = ArrayList<ScoreRow>()
        LEGACY_KEYS.forEach { key ->
            val raw = prefs.getString(key, null) ?: return@forEach
            runCatching { parseLegacy(key, raw) }.getOrDefault(emptyList()).let { out += it }
        }
        return out
    }

    companion object {
        const val PODIUM = 3
        const val NAME_MAX = 12
        private const val PREFS = "arcade_scores"
        private const val LAST_NAME = "lastName"
        private const val RUNS = "runs"
        private val LEGACY_KEYS = listOf(
            "snake",
            "breakout",
            "stacks",
            "merge",
            "mines_beginner",
            "mines_intermediate",
            "mines_expert",
        )
        private val stamp = SimpleDateFormat("MMM d  HH:mm", Locale.getDefault())

        fun sort(rows: List<ScoreRow>, lowerBetter: Boolean): List<ScoreRow> =
            if (lowerBetter) rows.sortedWith(compareBy<ScoreRow> { it.score }.thenBy { it.at })
            else rows.sortedWith(compareByDescending<ScoreRow> { it.score }.thenBy { it.at })

        fun formatScore(score: Int, lowerBetter: Boolean): String {
            if (!lowerBetter) return score.toString()
            val m = score / 60
            val s = score % 60
            return "%d:%02d".format(m, s)
        }

        fun formatWhen(at: Long): String = stamp.format(Date(at))

        private fun parseRuns(raw: String): List<ScoreRow> {
            val arr = JSONArray(raw)
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ScoreRow(
                            boardKey = o.getString("board"),
                            name = o.getString("name"),
                            score = o.getInt("score"),
                            at = o.getLong("at"),
                            win = o.optBoolean("win", true),
                        ),
                    )
                }
            }
        }

        private fun parseLegacy(boardKey: String, raw: String): List<ScoreRow> {
            val arr = JSONArray(raw)
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ScoreRow(
                            boardKey = boardKey,
                            name = o.getString("name"),
                            score = o.getInt("score"),
                            at = o.getLong("at"),
                            win = true,
                        ),
                    )
                }
            }
        }

        private fun stringify(rows: List<ScoreRow>): String {
            val arr = JSONArray()
            rows.forEach { row ->
                arr.put(
                    JSONObject()
                        .put("board", row.boardKey)
                        .put("name", row.name)
                        .put("score", row.score)
                        .put("at", row.at)
                        .put("win", row.win),
                )
            }
            return arr.toString()
        }
    }
}
