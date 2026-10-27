package ca.dstudio.marquee.catalog

/**
 * The languages Marquee offers for its titles, as a BCP 47 tag and the language's own name. The
 * Language setting stores the tag; an empty one means the device's language.
 */
object Languages {

    class Language(val tag: String, val name: String)

    val all: List<Language> = listOf(
        Language("en", "English"),
        Language("zh-CN", "中文（简体）"),
        Language("es", "Español"),
        Language("ja", "日本語"),
        Language("de", "Deutsch"),
        Language("fr", "Français"),
        Language("uk", "Українська"),
        Language("pt", "Português"),
        Language("ru", "Русский"),
        Language("ko", "한국어"),
        Language("ar", "العربية"),
        Language("it", "Italiano"),
        Language("tr", "Türkçe"),
        Language("pl", "Polski"),
        Language("nl", "Nederlands"),
        Language("hi", "हिन्दी"),
        Language("th", "ไทย"),
        Language("id", "Bahasa Indonesia"),
        Language("vi", "Tiếng Việt"),
    )

    fun named(tag: String): Language? = all.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
}
