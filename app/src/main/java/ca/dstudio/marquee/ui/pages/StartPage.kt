package ca.dstudio.marquee.ui.pages

import ca.dstudio.marquee.R
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.ui.panel.Item
import ca.dstudio.marquee.ui.panel.Page
import ca.dstudio.marquee.work.Generator

/** The first page: every part of the settings one step away, each row saying what it is set to. */
class StartPage(private val host: SettingsHost) : Page() {

    private val c get() = host.context

    override val icon = R.drawable.ic_page_start

    override fun title(): String = c.getString(R.string.app_name)

    override fun subtitle(): String {
        val status = host.status
        val set = host.set
        if (set == null) {
            val failed = status.running == null && status.lastOutcome != null && status.lastOutcome != Generator.Outcome.DONE
            return if (failed) Words.outcome(c, status.lastOutcome) else c.getString(R.string.start_making_first)
        }
        if (status.running != null) return Words.making(c, status)
        return c.resources.getQuantityString(R.plurals.start_summary, set.count, set.count, Words.age(c, set.createdAt))
    }

    override fun items(): List<Item> {
        val s = host.settings.read()
        return listOf(
            Item.Header("h-wallpapers", c.getString(R.string.section_wallpapers)),
            Item.Link("lists", c.getString(R.string.row_lists), value = c.getString(R.string.value_lists, s.lists.size)) {
                host.open(ListsPage(host))
            },
            Item.Link("filters", c.getString(R.string.row_filters), value = Words.filters(c, s)) {
                host.open(FiltersPage(host))
            },
            Item.Link("look", c.getString(R.string.row_look), value = Words.layout(c, s.look.layout)) {
                host.open(LookPage(host))
            },
            Item.Header("h-updates", c.getString(R.string.section_updates)),
            Item.Choice(
                "interval",
                c.getString(R.string.row_interval),
                options = Words.INTERVALS.map { Words.interval(c, it) },
                selected = Words.INTERVALS.indexOf(s.intervalHours),
                value = Words.interval(c, s.intervalHours),
            ) { i ->
                host.settings.set(MarqueeSettings.INTERVAL_HOURS, Words.INTERVALS[i].toString())
                host.intervalChanged()
                host.refresh()
            },
            Item.Action("update", c.getString(R.string.row_update_now), updateLine()) {
                host.updateNow()
            },
            Item.Choice(
                "size",
                c.getString(R.string.row_picture_size),
                options = SIZES.map { Words.pictureSize(c, it) },
                selected = SIZES.indexOf(s.pictureSize),
            ) { i ->
                host.settings.set(MarqueeSettings.PICTURE_SIZE, SIZES[i].toString())
                host.sizeChanged()
                host.refresh()
            },
            // Debug: the last run's report - why each title was shown or left out.
            Item.Link("debug", c.getString(R.string.row_explore), c.getString(R.string.row_explore_description)) {
                host.openExplore()
            },
            Item.Link("about", c.getString(R.string.row_about)) { host.open(AboutPage(host)) },
        )
    }

    /** Update now's description: its run's progress, then how it ended; nothing before it is pressed. */
    private fun updateLine(): String? {
        val status = host.status
        val asked = host.updateRequestedAt
        if (asked == 0L) return null
        if (status.running == Generator.Kind.FULL) return Words.making(c, status)
        if (status.finishedAt >= asked && status.lastOutcome != null) return Words.outcome(c, status.lastOutcome)
        return Words.making(c, status)
    }

    private companion object {
        val SIZES = listOf(1080, 2160)
    }
}
