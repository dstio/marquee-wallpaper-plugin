package ca.dstudio.marquee.ui.pages

import ca.dstudio.marquee.R
import ca.dstudio.marquee.catalog.Languages
import ca.dstudio.marquee.compose.Layout
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.compose.TextPosition
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.ui.panel.Item
import ca.dstudio.marquee.ui.panel.Page

/**
 * How the wallpapers look. Moving through a list here redraws the picture behind the panel with
 * that option; OK keeps it, BACK puts the chosen one back. Rows that do not apply to the chosen
 * layout are left out.
 */
class LookPage(private val host: SettingsHost) : Page() {

    private val c get() = host.context

    override val icon = R.drawable.ic_page_look

    override fun title(): String = c.getString(R.string.row_look)

    override fun subtitle(): String = c.getString(R.string.look_subtitle)

    override fun onLeave() = host.leftLook()

    override fun items(): List<Item> {
        val s = host.settings.read()
        val look = s.look
        val rows = ArrayList<Item>()
        rows += choice(
            "layout",
            c.getString(R.string.row_layout),
            Layout.entries.map { Words.layout(c, it) },
            Layout.entries.indexOf(look.layout),
            preview = { i -> look.copy(layout = Layout.entries[i]) },
        ) { i -> store(MarqueeSettings.LAYOUT, Layout.entries[i].key) }

        if (look.layout != Layout.CLEAN) {
            rows += choice(
                "position",
                c.getString(R.string.row_text_position),
                TextPosition.entries.map { Words.position(c, it) },
                TextPosition.entries.indexOf(look.position),
                preview = { i -> look.copy(position = TextPosition.entries[i]) },
            ) { i -> store(MarqueeSettings.POSITION, TextPosition.entries[i].key) }
        }
        if (look.layout == Layout.CINEMATIC) {
            rows += choice(
                "synopsis",
                c.getString(R.string.row_synopsis),
                Words.SYNOPSIS.map { Words.synopsis(c, it) },
                Words.SYNOPSIS.indexOf(look.synopsisLines),
                value = Words.synopsis(c, look.synopsisLines),
                preview = { i -> look.copy(synopsisLines = Words.SYNOPSIS[i]) },
            ) { i -> store(MarqueeSettings.SYNOPSIS_LINES, Words.SYNOPSIS[i].toString()) }
        }
        if (look.layout == Layout.CLEAN) {
            rows += Item.Toggle("shade", c.getString(R.string.row_bottom_shade), on = look.bottomShade) { on ->
                store(MarqueeSettings.BOTTOM_SHADE, on.toString())
                host.preview(host.settings.read().look)
            }
        }

        // The titles' language: it changes what is fetched, not how a picture is drawn, so it has
        // nothing to preview and takes effect with the next full run.
        val tags = listOf("") + Languages.all.map { it.tag }
        rows += Item.Choice(
            "language",
            c.getString(R.string.row_language),
            options = tags.map { Words.language(c, it) },
            selected = tags.indexOfFirst { it.equals(s.language, ignoreCase = true) },
            value = Words.language(c, s.language),
        ) { i ->
            host.settings.set(MarqueeSettings.LANGUAGE, tags[i])
            host.titlesChanged()
            host.refresh()
        }
        return rows
    }

    private fun choice(
        key: String,
        title: String,
        options: List<String>,
        selected: Int,
        value: String? = null,
        preview: (Int) -> Look,
        choose: (Int) -> Unit,
    ) = Item.Choice(
        key,
        title,
        options = options,
        selected = selected,
        value = value,
        onPreview = { i -> host.preview(preview(i)) },
        onCancel = { host.preview(host.settings.read().look) },
        onChoose = choose,
    )

    private fun store(key: String, value: String) {
        host.settings.set(key, value)
        host.lookChanged()
        host.refresh()
    }
}
