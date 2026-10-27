package ca.dstudio.marquee.ui.pages

import ca.dstudio.marquee.BuildConfig
import ca.dstudio.marquee.R
import ca.dstudio.marquee.ui.panel.Item
import ca.dstudio.marquee.ui.panel.Page

/** Marquee and its version, TMDB's notice, the streaming credit, the fonts, the source and the licence. */
class AboutPage(private val host: SettingsHost) : Page() {

    private val c get() = host.context

    override val icon = R.drawable.ic_page_about

    override fun title(): String = c.getString(R.string.row_about)

    override fun subtitle(): String = c.getString(R.string.about_subtitle)

    override fun items(): List<Item> = listOf(
        Item.Info("marquee", c.getString(R.string.app_name), c.getString(R.string.about_version, BuildConfig.VERSION_NAME)),
        // TMDB asks for its logo beside this notice; the row keeps a spot for it, to be filled from
        // TMDB's own branding files.
        Item.Info("tmdb", c.getString(R.string.about_tmdb_title), c.getString(R.string.about_tmdb_notice), logoSpot = true),
        // TMDB's streaming availability comes from JustWatch, which is to be credited.
        Item.Info("streaming", c.getString(R.string.about_streaming_title), c.getString(R.string.about_streaming)),
        Item.Info("fonts", c.getString(R.string.about_fonts_title), c.getString(R.string.about_fonts)),
        Item.Info("source", c.getString(R.string.about_source_title), c.getString(R.string.about_source)),
        Item.Info("licence", c.getString(R.string.about_licence_title), c.getString(R.string.about_licence)),
    )
}
