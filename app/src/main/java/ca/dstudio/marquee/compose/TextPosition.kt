package ca.dstudio.marquee.compose

/** The four wallpaper layouts, by the key the settings store. */
enum class Layout(val key: String) {
    CINEMATIC("cinematic"),
    MINIMAL("minimal"),
    POSTER_WALL("poster_wall"),
    CLEAN("clean");

    companion object {
        fun of(key: String?): Layout = entries.firstOrNull { it.key == key } ?: CINEMATIC
    }
}

/** Where a layout's text sits. */
enum class TextPosition(val key: String) {
    TOP_LEFT("top_left"),
    TOP_RIGHT("top_right"),
    /** Bottom left, on a solid panel. */
    BOTTOM_PANEL("bottom_panel");

    companion object {
        fun of(key: String?): TextPosition = entries.firstOrNull { it.key == key } ?: TOP_LEFT
    }
}

/** Everything about a picture's look that the settings decide. */
data class Look(
    val layout: Layout,
    val position: TextPosition,
    /** 0 to 3. */
    val synopsisLines: Int,
    /** Clean's bottom shade. */
    val bottomShade: Boolean,
)
