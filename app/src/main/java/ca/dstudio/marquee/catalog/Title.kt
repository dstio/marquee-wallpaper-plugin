package ca.dstudio.marquee.catalog

import kotlinx.serialization.Serializable
import java.util.Locale

/** Film or show: TMDB's two media types, by the key its API and its web pages use. */
enum class Media(val key: String) {
    MOVIE("movie"),
    TV("tv");

    companion object {
        fun of(key: String?): Media? = entries.firstOrNull { it.key == key }
    }
}

/**
 * One entry of a list, as the list gives it: enough to filter on before asking for details.
 * [rank] is its 1-based place in the list.
 */
data class Title(
    val media: Media,
    val id: Int,
    val name: String,
    val releaseDate: String?,
    val adult: Boolean,
    val genreIds: List<Int>,
    val rank: Int,
    /** The poster as an image reference, when the list gives one (Explore shows it if cached). */
    val poster: String? = null,
) {
    /** Identifies the title across lists and sets. */
    val key: String get() = "${media.key}/$id"
}

/**
 * What a wallpaper says about a title, in the user's language where the source has it. Images are
 * references the source resolves into files ([ca.dstudio.marquee.source.Source.image]); they are
 * kept with the set so a change of look recomposes without asking the source again.
 */
@Serializable
data class TitleDetails(
    val media: String,
    val id: Int,
    val name: String,
    val releaseDate: String? = null,
    val genres: List<String> = emptyList(),
    val genreIds: List<Int> = emptyList(),
    val runtimeMinutes: Int? = null,
    val seasons: Int? = null,
    val overview: String = "",
    /**
     * The overview's language (ISO 639-1) when it is not the run's own: English, fetched because
     * the run's language had none. Null when the overview is in the run's language.
     */
    val overviewLanguage: String? = null,
    val voteAverage: Double? = null,
    val adult: Boolean = false,
    val logo: String? = null,
    val logoLanguage: String? = null,
    val backdrop: String? = null,
    val poster: String? = null,
    /** The subscription services it streams on in the user's country, in display order, at most three. */
    val streaming: List<String> = emptyList(),
) {
    val key: String get() = "$media/$id"

    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
}

/** The user's language, as TMDB takes it. */
data class Lang(
    /** BCP 47, "en-US". */
    val tag: String,
    /** ISO 639-1, "en". */
    val two: String,
    /** ISO 3166-1, "US", or empty. */
    val country: String,
) {
    companion object {
        /** The language a setting names (a BCP 47 tag), or the device's when it is empty. */
        fun of(setting: String): Lang {
            if (setting.isBlank()) return device()
            return from(Locale.forLanguageTag(setting))
        }

        fun device(): Lang = from(Locale.getDefault())

        private fun from(locale: Locale): Lang {
            // Java's legacy codes for three languages; TMDB takes the current ones.
            val two = when (val l = locale.language.ifBlank { "en" }) {
                "in" -> "id"
                "iw" -> "he"
                "ji" -> "yi"
                else -> l
            }
            val country = locale.country.orEmpty()
            val tag = if (country.isNotEmpty()) "$two-$country" else two
            return Lang(tag, two, country)
        }
    }
}
