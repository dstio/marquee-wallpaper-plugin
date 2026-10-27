package ca.dstudio.marquee.settings

import android.content.Context
import android.content.SharedPreferences
import ca.dstudio.marquee.compose.Layout
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.compose.TextPosition
import ca.dstudio.marquee.source.ListKey

/** Every setting at one moment; what a run works from. */
data class Settings(
    val lists: List<String>,
    val titlesPerList: Int,
    /** `random` (from the list's first 40) or `top`. */
    val pick: String,
    /** `mixed` (the lists interleaved) or `lists` (one after another). */
    val order: String,
    /** 0 for any age, else 1, 2, 5 or 10 years. */
    val releasedWithinYears: Int,
    val hideAdult: Boolean,
    val excludedGenres: Set<Int>,
    val needsLogo: Boolean,
    val needsSynopsis: Boolean,
    val noRepeats: Boolean,
    /** A BCP 47 tag, or empty for the device's language. */
    val language: String,
    val look: Look,
    /** 1080 or 2160: the largest picture made. */
    val pictureSize: Int,
    val intervalHours: Int,
)

/**
 * Marquee's settings, in one SharedPreferences file. The defaults are the first-run choice: Trending
 * and In cinemas, twelve titles each at random, mixed, Cinematic at the top left, 1080p, every 12
 * hours.
 */
class MarqueeSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(): Settings = Settings(
        lists = prefs.getString(LISTS, null)?.split(',')?.map { it.trim() }
            ?.filter { ListKey.of(it) != null }?.distinct() ?: DEFAULT_LISTS,
        titlesPerList = prefs.getInt(TITLES_PER_LIST, 12).coerceIn(1, 60),
        pick = prefs.getString(PICK, null)?.takeIf { it in PICKS } ?: "random",
        order = prefs.getString(ORDER, null)?.takeIf { it in ORDERS } ?: "mixed",
        releasedWithinYears = prefs.getInt(RELEASED_WITHIN, 0).takeIf { it in WITHIN } ?: 0,
        hideAdult = prefs.getBoolean(HIDE_ADULT, true),
        excludedGenres = prefs.getString(GENRES_EXCLUDED, "").orEmpty().split(',')
            .mapNotNull { it.trim().toIntOrNull() }.toSet(),
        needsLogo = prefs.getBoolean(NEEDS_LOGO, false),
        needsSynopsis = prefs.getBoolean(NEEDS_SYNOPSIS, false),
        noRepeats = prefs.getBoolean(NO_REPEATS, false),
        language = prefs.getString(LANGUAGE, "").orEmpty().trim(),
        look = Look(
            layout = Layout.of(prefs.getString(LAYOUT, null)),
            position = TextPosition.of(prefs.getString(POSITION, null)),
            synopsisLines = prefs.getInt(SYNOPSIS_LINES, 3).coerceIn(0, 3),
            bottomShade = prefs.getBoolean(BOTTOM_SHADE, true),
        ),
        pictureSize = prefs.getInt(PICTURE_SIZE, 1080).takeIf { it == 1080 || it == 2160 } ?: 1080,
        intervalHours = prefs.getInt(INTERVAL_HOURS, 12).coerceIn(1, 168),
    )

    /** The launcher's screen at the last attach, so a scheduled run makes pictures of that size. */
    var screenWidth: Int
        get() = prefs.getInt(SCREEN_WIDTH, 0)
        set(value) = prefs.edit().putInt(SCREEN_WIDTH, value).apply()

    var screenHeight: Int
        get() = prefs.getInt(SCREEN_HEIGHT, 0)
        set(value) = prefs.edit().putInt(SCREEN_HEIGHT, value).apply()

    /**
     * Sets one setting from its text form, as the debug command passes it. Returns null when it
     * was stored, else what was wrong with it.
     */
    fun set(key: String, value: String): String? {
        val v = value.trim()
        val e = prefs.edit()
        when (key) {
            LISTS -> {
                val keys = v.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val unknown = keys.filter { ListKey.of(it) == null }
                if (keys.isEmpty() || unknown.isNotEmpty()) {
                    return "lists: one or more of ${ListKey.entries.joinToString(",") { it.key }}"
                }
                e.putString(LISTS, keys.distinct().joinToString(","))
            }
            TITLES_PER_LIST -> e.putInt(key, v.toIntOrNull()?.takeIf { it in 1..60 } ?: return "$key: 1 to 60")
            PICK -> e.putString(key, v.takeIf { it in PICKS } ?: return "$key: ${PICKS.joinToString(" | ")}")
            ORDER -> e.putString(key, v.takeIf { it in ORDERS } ?: return "$key: ${ORDERS.joinToString(" | ")}")
            RELEASED_WITHIN -> e.putInt(key, v.toIntOrNull()?.takeIf { it in WITHIN } ?: return "$key: ${WITHIN.joinToString(" | ")}")
            GENRES_EXCLUDED -> {
                val ids = v.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                if (ids.any { it.toIntOrNull() == null }) return "$key: TMDB genre ids, comma-separated"
                e.putString(key, ids.joinToString(","))
            }
            HIDE_ADULT, NEEDS_LOGO, NEEDS_SYNOPSIS, NO_REPEATS, BOTTOM_SHADE ->
                e.putBoolean(key, v.toBooleanStrictOrNull() ?: return "$key: true | false")
            LAYOUT -> e.putString(key, v.takeIf { s -> Layout.entries.any { it.key == s } }
                ?: return "$key: ${Layout.entries.joinToString(" | ") { it.key }}")
            POSITION -> e.putString(key, v.takeIf { s -> TextPosition.entries.any { it.key == s } }
                ?: return "$key: ${TextPosition.entries.joinToString(" | ") { it.key }}")
            SYNOPSIS_LINES -> e.putInt(key, v.toIntOrNull()?.takeIf { it in 0..3 } ?: return "$key: 0 to 3")
            PICTURE_SIZE -> e.putInt(key, v.toIntOrNull()?.takeIf { it == 1080 || it == 2160 } ?: return "$key: 1080 | 2160")
            INTERVAL_HOURS -> e.putInt(key, v.toIntOrNull()?.takeIf { it in 1..168 } ?: return "$key: 1 to 168")
            LANGUAGE -> e.putString(key, v.takeIf { it.isEmpty() || LANGUAGE_TAG.matches(it) } ?: return "$key: empty or a BCP 47 tag")
            else -> return "unknown setting $key"
        }
        e.apply()
        return null
    }

    /** Every setting by key, for the debug dump. */
    fun dump(): Map<String, String> {
        val s = read()
        return linkedMapOf(
            LISTS to s.lists.joinToString(","),
            TITLES_PER_LIST to "${s.titlesPerList}",
            PICK to s.pick,
            ORDER to s.order,
            RELEASED_WITHIN to "${s.releasedWithinYears}",
            HIDE_ADULT to "${s.hideAdult}",
            GENRES_EXCLUDED to s.excludedGenres.joinToString(","),
            NEEDS_LOGO to "${s.needsLogo}",
            NEEDS_SYNOPSIS to "${s.needsSynopsis}",
            NO_REPEATS to "${s.noRepeats}",
            LANGUAGE to s.language.ifEmpty { "(device)" },
            LAYOUT to s.look.layout.key,
            POSITION to s.look.position.key,
            SYNOPSIS_LINES to "${s.look.synopsisLines}",
            BOTTOM_SHADE to "${s.look.bottomShade}",
            PICTURE_SIZE to "${s.pictureSize}",
            INTERVAL_HOURS to "${s.intervalHours}",
            "screen" to "${screenWidth}x$screenHeight",
        )
    }

    companion object {
        private const val FILE = "marquee"

        const val LISTS = "lists"
        const val TITLES_PER_LIST = "titles_per_list"
        const val PICK = "pick"
        const val ORDER = "order"
        const val RELEASED_WITHIN = "released_within"
        const val HIDE_ADULT = "hide_adult"
        const val GENRES_EXCLUDED = "genres_excluded"
        const val NEEDS_LOGO = "needs_logo"
        const val NEEDS_SYNOPSIS = "needs_synopsis"
        const val NO_REPEATS = "no_repeats"
        const val LAYOUT = "layout"
        const val POSITION = "position"
        const val SYNOPSIS_LINES = "synopsis_lines"
        const val BOTTOM_SHADE = "bottom_shade"
        const val PICTURE_SIZE = "picture_size"
        const val INTERVAL_HOURS = "interval_hours"
        const val LANGUAGE = "language"
        private const val SCREEN_WIDTH = "screen_width"
        private const val SCREEN_HEIGHT = "screen_height"

        /** The settings that change only how the pictures look: changing one recomposes. */
        val LOOK_KEYS = setOf(LAYOUT, POSITION, SYNOPSIS_LINES, BOTTOM_SHADE, PICTURE_SIZE)

        private val DEFAULT_LISTS = listOf(ListKey.TRENDING.key, ListKey.IN_CINEMAS.key)
        private val PICKS = listOf("random", "top")
        private val ORDERS = listOf("mixed", "lists")
        private val WITHIN = listOf(0, 1, 2, 5, 10)
        private val LANGUAGE_TAG = Regex("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*")
    }
}
