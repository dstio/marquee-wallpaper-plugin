package ca.dstudio.marquee.ui.panel

/**
 * One row of a settings page. [key] names the row across rebuilds, so the cursor stays on it when
 * the page is redrawn; [signature] is what the row shows, so a rebuild only rebinds rows that changed.
 */
sealed class Item(val key: String) {

    abstract val signature: String

    /** A section's name: not focusable. */
    class Header(key: String, val text: String) : Item(key) {
        override val signature get() = "h|$text"
    }

    /** Opens another page (or a dialog); shows a chevron. */
    class Link(
        key: String,
        val title: String,
        val description: String? = null,
        val value: String? = null,
        val onOpen: () -> Unit,
    ) : Item(key) {
        override val signature get() = "l|$title|$description|$value"
    }

    /**
     * One of [options], chosen from a dropdown list. [value] overrides what the row shows (for a
     * stored value that is not among the options). With [onPreview], moving through the list
     * previews each option, and BACK calls [onCancel] to put the chosen one back.
     */
    class Choice(
        key: String,
        val title: String,
        val description: String? = null,
        val options: List<String>,
        val selected: Int,
        val value: String? = null,
        val onPreview: ((Int) -> Unit)? = null,
        val onCancel: (() -> Unit)? = null,
        val onChoose: (Int) -> Unit,
    ) : Item(key) {
        val shown: String? get() = value ?: options.getOrNull(selected)
        override val signature get() = "c|$title|$description|$shown"
    }

    /** A switch; OK flips it. */
    class Toggle(
        key: String,
        val title: String,
        val description: String? = null,
        val on: Boolean,
        val onToggle: (Boolean) -> Unit,
    ) : Item(key) {
        override val signature get() = "t|$title|$description|$on"
    }

    /** A number LEFT and RIGHT change by [step] while the row has the cursor. */
    class Stepper(
        key: String,
        val title: String,
        val description: String? = null,
        val value: Int,
        val min: Int,
        val max: Int,
        val step: Int,
        val label: (Int) -> String,
        val onChange: (Int) -> Unit,
    ) : Item(key) {
        override val signature get() = "s|$title|$description|$value"
    }

    /** Does something now; its description says how it went. */
    class Action(
        key: String,
        val title: String,
        val description: String? = null,
        val onPress: () -> Unit,
    ) : Item(key) {
        override val signature get() = "a|$title|$description"
    }

    /** Text to read; focusable so a long page can be scrolled, but OK does nothing. */
    class Info(
        key: String,
        val title: String,
        val description: String? = null,
        /** Keeps a spot beside the text for a logo, empty until one is added. */
        val logoSpot: Boolean = false,
    ) : Item(key) {
        override val signature get() = "i|$title|$description"
    }
}
