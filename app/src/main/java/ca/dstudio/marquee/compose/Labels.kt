package ca.dstudio.marquee.compose

import android.content.Context
import androidx.core.os.ConfigurationCompat
import ca.dstudio.marquee.R
import ca.dstudio.marquee.catalog.TitleDetails
import java.util.Locale

/** The words a wallpaper prints besides the title's own, in the app's language. */
class Labels(context: Context) {

    private val res = context.applicationContext.resources

    val locale: Locale = ConfigurationCompat.getLocales(res.configuration)[0] ?: Locale.getDefault()

    val from: String = res.getString(R.string.footer_from)
    val streamingOn: String = res.getString(R.string.footer_streaming_on)
    val tmdb: String = res.getString(R.string.rating_tmdb)

    fun kicker(list: String, n: Int, total: Int): String = res.getString(R.string.kicker_position, list, n, total)

    /** Year, two genres at most, then the runtime or the number of seasons. */
    fun facts(d: TitleDetails): List<String> = buildList {
        d.year?.let { add("$it") }
        if (d.genres.isNotEmpty()) add(d.genres.take(2).joinToString(", "))
        d.runtimeMinutes?.let { add(runtime(it)) }
        d.seasons?.let { add(res.getQuantityString(R.plurals.facts_seasons, it, it)) }
    }

    /** TMDB's average as a percentage, "74%", or null when there is none. */
    fun tmdbPercent(d: TitleDetails): String? =
        d.voteAverage?.takeIf { it > 0.0 }?.let { "${Math.round(it * 10)}%" }

    private fun runtime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> res.getString(R.string.facts_runtime_minutes, m)
            m == 0 -> res.getString(R.string.facts_runtime_hours, h)
            else -> res.getString(R.string.facts_runtime_hours_minutes, h, m)
        }
    }
}
