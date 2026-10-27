package ca.dstudio.marquee.ui.pages

import android.content.Context
import ca.dstudio.marquee.R
import ca.dstudio.marquee.catalog.Genres
import ca.dstudio.marquee.catalog.Languages
import ca.dstudio.marquee.compose.Layout
import ca.dstudio.marquee.compose.TextPosition
import ca.dstudio.marquee.settings.Settings
import ca.dstudio.marquee.work.Generator
import java.util.Locale

/** The words the settings pages show for a setting's value. */
internal object Words {

    val INTERVALS = listOf(6, 12, 24, 48, 168)
    val WITHIN = listOf(0, 1, 2, 5, 10)
    val SYNOPSIS = listOf(3, 2, 0)

    fun interval(c: Context, hours: Int): String = when (hours) {
        6 -> c.getString(R.string.interval_6)
        12 -> c.getString(R.string.interval_12)
        24 -> c.getString(R.string.interval_24)
        48 -> c.getString(R.string.interval_48)
        168 -> c.getString(R.string.interval_168)
        else -> c.getString(R.string.interval_hours, hours)
    }

    fun within(c: Context, years: Int): String =
        if (years <= 0) c.getString(R.string.released_any) else c.resources.getQuantityString(R.plurals.released_years, years, years)

    fun layout(c: Context, layout: Layout): String = c.getString(
        when (layout) {
            Layout.CINEMATIC -> R.string.layout_cinematic
            Layout.MINIMAL -> R.string.layout_minimal
            Layout.POSTER_WALL -> R.string.layout_poster_wall
            Layout.CLEAN -> R.string.layout_clean
        },
    )

    fun position(c: Context, position: TextPosition): String = c.getString(
        when (position) {
            TextPosition.TOP_LEFT -> R.string.position_top_left
            TextPosition.TOP_RIGHT -> R.string.position_top_right
            TextPosition.BOTTOM_PANEL -> R.string.position_bottom_left
        },
    )

    fun synopsis(c: Context, lines: Int): String = c.getString(
        when (lines) {
            3 -> R.string.synopsis_3
            2 -> R.string.synopsis_2
            1 -> R.string.synopsis_1
            else -> R.string.synopsis_off
        },
    )

    fun pictureSize(c: Context, size: Int): String =
        c.getString(if (size >= 2160) R.string.picture_size_2160 else R.string.picture_size_1080)

    fun language(c: Context, tag: String): String =
        if (tag.isBlank()) c.getString(R.string.language_device) else Languages.named(tag)?.name ?: tag

    /** "All", "All but Horror", or "<n> genres" for the genres left on. */
    fun genres(c: Context, excluded: Set<Int>): String {
        val off = Genres.all.filter { it.id in excluded }
        val on = Genres.all.size - off.size
        return when (off.size) {
            0 -> c.getString(R.string.genres_all)
            1 -> c.getString(R.string.genres_all_but, c.getString(off.first().label))
            else -> c.resources.getQuantityString(R.plurals.genres_count, on, on)
        }
    }

    /** The Filters row's value: "No adult", "5 years, no adult", or "None". */
    fun filters(c: Context, s: Settings): String {
        val parts = ArrayList<String>()
        if (s.releasedWithinYears > 0) parts += within(c, s.releasedWithinYears)
        if (s.hideAdult) parts += c.getString(R.string.filters_no_adult)
        val off = Genres.all.count { it.id in s.excludedGenres }
        if (off > 0) parts += c.resources.getQuantityString(R.plurals.filters_genres_off, off, off)
        if (s.noRepeats) parts += c.getString(R.string.filters_no_repeats)
        if (parts.isEmpty()) return c.getString(R.string.filters_none)
        val text = parts.joinToString(c.getString(R.string.filters_separator))
        return text.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    /** How long ago [at] was: "5 min", "2 h", "3 days". */
    fun age(c: Context, at: Long, now: Long = System.currentTimeMillis()): String {
        val minutes = ((now - at) / 60_000L).coerceAtLeast(1L).toInt()
        return when {
            minutes < 60 -> c.resources.getQuantityString(R.plurals.age_minutes, minutes, minutes)
            minutes < 48 * 60 -> c.getString(R.string.age_hours, minutes / 60)
            else -> (minutes / (24 * 60)).let { c.resources.getQuantityString(R.plurals.age_days, it, it) }
        }
    }

    /** "Making wallpapers, 3 of 12…", or without the numbers before composing starts. */
    fun making(c: Context, status: Generator.Status): String =
        if (status.steps > 0) c.getString(R.string.start_making_step, status.step, status.steps) else c.getString(R.string.start_making)

    fun outcome(c: Context, outcome: Generator.Outcome): String = c.getString(
        when (outcome) {
            Generator.Outcome.DONE -> R.string.update_done
            Generator.Outcome.NO_CONNECTION -> R.string.update_no_connection
            Generator.Outcome.LISTS_FAILED -> R.string.update_lists_failed
            Generator.Outcome.NOTHING_PICKED -> R.string.update_nothing_picked
            Generator.Outcome.NO_SOURCE -> R.string.update_no_source
            Generator.Outcome.FAILED -> R.string.update_failed
        },
    )
}
