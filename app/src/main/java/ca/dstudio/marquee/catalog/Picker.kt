package ca.dstudio.marquee.catalog

import ca.dstudio.marquee.settings.Settings
import ca.dstudio.atvlauncher.wallpaper.api.WallpaperContract
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

/** A list as a run read it: its titles in list order, or null when it could not be read. */
class ListInput(val key: String, val label: String, val titles: List<Title>?)

/** A title that made it into the set, with what [Picker.pick]'s `assets` gave for it. */
class Picked<A>(val listKey: String, val listLabel: String, val details: TitleDetails, val assets: A)

class PickResult<A>(val picks: List<Picked<A>>, val lists: List<ListReport>)

/**
 * Chooses the set's titles from the lists. Each list gives at most `titlesPerList`, taken at random
 * from its first [POOL] or from the top, through the filters; the lists take turns one title at a
 * time, so a title two lists share goes to whichever reaches it first and is used once. The whole
 * set is at most [MAX_TOTAL]. Every candidate ends with `used` or the reason it was left out.
 *
 * Pure apart from the two lookups it is handed, which a run backs with the source.
 */
class Picker(
    private val settings: Settings,
    private val langTwo: String,
    private val random: Random = Random.Default,
    private val today: Calendar = Calendar.getInstance(),
) {

    fun <A> pick(
        lists: List<ListInput>,
        previous: Set<String>,
        details: (Title) -> TitleDetails?,
        assets: (TitleDetails) -> A?,
    ): PickResult<A> {
        val states = lists.map { input ->
            val titles = input.titles.orEmpty().take(POOL).distinctBy { it.key }
            val order = if (settings.pick == "random") titles.shuffled(random) else titles
            ListState(input, titles, order)
        }
        val used = HashSet<String>()
        var total = 0
        val cutoff = cutoff()

        fun skipBefore(t: Title): Skip? = when {
            t.key in used -> Skip.DUPLICATE
            settings.noRepeats && t.key in previous -> Skip.REPEAT
            settings.hideAdult && t.adult -> Skip.ADULT
            cutoff != null && t.releaseDate != null && t.releaseDate < cutoff -> Skip.OLDER
            t.genreIds.any { it in settings.excludedGenres } -> Skip.GENRE
            else -> null
        }

        fun skipAfter(d: TitleDetails): Skip? = when {
            settings.hideAdult && d.adult -> Skip.ADULT
            cutoff != null && d.releaseDate != null && d.releaseDate < cutoff -> Skip.OLDER
            d.genreIds.any { it in settings.excludedGenres } -> Skip.GENRE
            d.backdrop == null -> Skip.NO_BACKDROP
            settings.needsLogo && (d.logo == null || d.logoLanguage != langTwo) -> Skip.NO_LOGO
            // An English stand-in does not count as a synopsis in the user's language.
            settings.needsSynopsis && (d.overview.isBlank() || d.overviewLanguage != null) -> Skip.NO_SYNOPSIS
            else -> null
        }

        var progress = true
        while (progress && total < MAX_TOTAL) {
            progress = false
            for (state in states) {
                if (total >= MAX_TOTAL) break
                if (state.picks.size >= settings.titlesPerList || state.cursor >= state.order.size) continue
                while (state.cursor < state.order.size) {
                    val t = state.order[state.cursor++]
                    progress = true
                    skipBefore(t)?.let { state.status[t.key] = it.key; continue }
                    val d = details(t)
                    if (d == null) {
                        state.status[t.key] = Skip.FAILED.key; continue
                    }
                    skipAfter(d)?.let { state.status[t.key] = it.key; continue }
                    val a = assets(d)
                    if (a == null) {
                        state.status[t.key] = Skip.FAILED.key; continue
                    }
                    state.status[t.key] = RunReport.USED
                    state.picks += Picked(state.input.key, state.input.label, d, a)
                    used += t.key
                    total++
                    break
                }
            }
        }

        val reports = states.map { state ->
            ListReport(
                key = state.input.key,
                label = state.input.label,
                error = if (state.input.titles == null) Skip.FAILED.key else null,
                candidates = state.titles.map { t ->
                    Candidate(
                        media = t.media.key,
                        id = t.id,
                        title = t.name,
                        rank = t.rank,
                        status = state.status[t.key] ?: Skip.OVER_COUNT.key,
                        year = t.releaseDate?.take(4)?.toIntOrNull(),
                        poster = t.poster,
                    )
                },
            )
        }
        val ordered = if (settings.order == "lists") {
            states.flatMap { it.picks }
        } else {
            buildList {
                var i = 0
                while (states.any { it.picks.size > i }) {
                    states.forEach { s -> s.picks.getOrNull(i)?.let { add(it) } }
                    i++
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return PickResult(ordered.take(MAX_TOTAL) as List<Picked<A>>, reports)
    }

    /** The oldest release date kept, "YYYY-MM-DD", or null for any age. */
    private fun cutoff(): String? {
        val years = settings.releasedWithinYears
        if (years <= 0) return null
        val c = today.clone() as Calendar
        c.add(Calendar.YEAR, -years)
        return String.format(Locale.ROOT, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    private class ListState(val input: ListInput, val titles: List<Title>, val order: List<Title>) {
        var cursor = 0
        val status = HashMap<String, String>()
        val picks = ArrayList<Picked<*>>()
    }

    companion object {
        /** A random pick is made from the list's first 40. */
        const val POOL = 40

        /** The launcher shows at most 60 pictures of a plugin. */
        const val MAX_TOTAL = WallpaperContract.MAX_ITEMS
    }
}
