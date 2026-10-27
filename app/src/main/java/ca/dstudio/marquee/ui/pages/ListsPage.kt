package ca.dstudio.marquee.ui.pages

import ca.dstudio.marquee.R
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.source.ListKey
import ca.dstudio.marquee.ui.panel.Item
import ca.dstudio.marquee.ui.panel.Page

/** TMDB's lists as switches, then how many titles each gives, how they are picked and ordered. */
class ListsPage(private val host: SettingsHost) : Page() {

    private val c get() = host.context

    /** The list whose switch was refused because it was the last one on. */
    private var refused: String? = null

    override val icon = R.drawable.ic_page_lists

    override fun title(): String = c.getString(R.string.row_lists)

    override fun subtitle(): String = c.getString(R.string.lists_subtitle)

    override fun items(): List<Item> {
        val s = host.settings.read()
        val rows = ArrayList<Item>()
        for (key in ORDER) {
            val on = key.key in s.lists
            val description = when {
                refused == key.key -> c.getString(R.string.lists_at_least_one)
                key == ListKey.TRENDING -> c.getString(R.string.list_trending_description)
                else -> null
            }
            rows += Item.Toggle("list-${key.key}", c.getString(key.label), description, on) { want ->
                val now = host.settings.read().lists
                if (!want && now.size <= 1 && key.key in now) {
                    refused = key.key
                } else {
                    refused = null
                    val next = ORDER.filter { k -> if (k == key) want else k.key in now }.joinToString(",") { it.key }
                    host.settings.set(MarqueeSettings.LISTS, next)
                    host.titlesChanged()
                }
                host.refresh()
            }
        }
        rows += Item.Header("h-each", c.getString(R.string.section_each_list))
        rows += Item.Stepper(
            "titles",
            c.getString(R.string.row_titles),
            value = s.titlesPerList.coerceIn(MIN_TITLES, MAX_TITLES),
            min = MIN_TITLES,
            max = MAX_TITLES,
            step = 2,
            label = { c.getString(R.string.stepper_value, it) },
        ) { n ->
            host.settings.set(MarqueeSettings.TITLES_PER_LIST, n.toString())
            host.titlesChanged()
            host.refresh()
        }
        rows += Item.Choice(
            "pick",
            c.getString(R.string.row_pick),
            options = listOf(c.getString(R.string.pick_random), c.getString(R.string.pick_top)),
            selected = if (s.pick == "top") 1 else 0,
        ) { i ->
            host.settings.set(MarqueeSettings.PICK, if (i == 1) "top" else "random")
            host.titlesChanged()
            host.refresh()
        }
        rows += Item.Choice(
            "order",
            c.getString(R.string.row_order),
            options = listOf(c.getString(R.string.order_mixed), c.getString(R.string.order_lists)),
            selected = if (s.order == "lists") 1 else 0,
        ) { i ->
            host.settings.set(MarqueeSettings.ORDER, if (i == 1) "lists" else "mixed")
            host.titlesChanged()
            host.refresh()
        }
        return rows
    }

    private companion object {
        const val MIN_TITLES = 4
        const val MAX_TITLES = 30

        /** The order the page lists them in. */
        val ORDER = listOf(
            ListKey.TRENDING,
            ListKey.IN_CINEMAS,
            ListKey.TOP_RATED_SHOWS,
            ListKey.POPULAR_FILMS,
            ListKey.UPCOMING,
            ListKey.ON_THE_AIR,
        )
    }
}
