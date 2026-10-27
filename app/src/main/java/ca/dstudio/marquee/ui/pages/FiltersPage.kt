package ca.dstudio.marquee.ui.pages

import ca.dstudio.marquee.R
import ca.dstudio.marquee.catalog.GenreScope
import ca.dstudio.marquee.catalog.Genres
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.ui.panel.Item
import ca.dstudio.marquee.ui.panel.Page

/** What every list's titles must pass. */
class FiltersPage(private val host: SettingsHost) : Page() {

    private val c get() = host.context

    override val icon = R.drawable.ic_page_filters

    override fun title(): String = c.getString(R.string.row_filters)

    override fun subtitle(): String = c.getString(R.string.filters_subtitle)

    override fun items(): List<Item> {
        val s = host.settings.read()
        return listOf(
            Item.Choice(
                "within",
                c.getString(R.string.row_released_within),
                options = Words.WITHIN.map { Words.within(c, it) },
                selected = Words.WITHIN.indexOf(s.releasedWithinYears),
            ) { i -> store(MarqueeSettings.RELEASED_WITHIN, Words.WITHIN[i].toString()) },
            Item.Link("genres", c.getString(R.string.row_genres), value = Words.genres(c, s.excludedGenres)) {
                host.open(GenresPage(host))
            },
            Item.Toggle("adult", c.getString(R.string.row_hide_adult), on = s.hideAdult) {
                store(MarqueeSettings.HIDE_ADULT, it.toString())
            },
            Item.Toggle("repeats", c.getString(R.string.row_no_repeats), c.getString(R.string.row_no_repeats_description), s.noRepeats) {
                store(MarqueeSettings.NO_REPEATS, it.toString())
            },
            Item.Header("h-language", c.getString(R.string.section_your_language)),
            Item.Toggle("logo", c.getString(R.string.row_needs_logo), on = s.needsLogo) {
                store(MarqueeSettings.NEEDS_LOGO, it.toString())
            },
            Item.Toggle("synopsis", c.getString(R.string.row_needs_synopsis), on = s.needsSynopsis) {
                store(MarqueeSettings.NEEDS_SYNOPSIS, it.toString())
            },
        )
    }

    private fun store(key: String, value: String) {
        host.settings.set(key, value)
        host.titlesChanged()
        host.refresh()
    }
}

/** One switch per TMDB genre; a title in a genre that is off is left out. */
class GenresPage(private val host: SettingsHost) : Page() {

    private val c get() = host.context

    override val icon = R.drawable.ic_page_genres

    override fun title(): String = c.getString(R.string.row_genres)

    override fun subtitle(): String = c.getString(R.string.genres_subtitle)

    override fun items(): List<Item> {
        val excluded = host.settings.read().excludedGenres
        return Genres.all
            .sortedBy { c.getString(it.label) }
            .map { g ->
                val scope = when (g.scope) {
                    GenreScope.FILMS -> c.getString(R.string.genre_for_films)
                    GenreScope.SHOWS -> c.getString(R.string.genre_for_shows)
                    GenreScope.BOTH -> null
                }
                Item.Toggle("genre-${g.id}", c.getString(g.label), scope, g.id !in excluded) { on ->
                    val now = host.settings.read().excludedGenres
                    val next = if (on) now - g.id else now + g.id
                    host.settings.set(MarqueeSettings.GENRES_EXCLUDED, next.sorted().joinToString(","))
                    host.titlesChanged()
                    host.refresh()
                }
            }
    }
}
