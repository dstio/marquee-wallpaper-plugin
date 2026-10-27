package ca.dstudio.marquee.source

import androidx.annotation.StringRes
import ca.dstudio.marquee.R
import ca.dstudio.marquee.catalog.Lang
import ca.dstudio.marquee.catalog.Media
import ca.dstudio.marquee.catalog.Title
import ca.dstudio.marquee.catalog.TitleDetails
import java.io.File
import java.io.IOException

/** The lists a user can pick, by the key the settings store. */
enum class ListKey(val key: String, @param:StringRes val label: Int) {
    TRENDING("trending", R.string.list_trending),
    IN_CINEMAS("in_cinemas", R.string.list_in_cinemas),
    POPULAR_FILMS("popular_films", R.string.list_popular_films),
    UPCOMING("upcoming", R.string.list_upcoming),
    TOP_RATED_SHOWS("top_rated_shows", R.string.list_top_rated_shows),
    ON_THE_AIR("on_the_air", R.string.list_on_the_air);

    companion object {
        fun of(key: String?): ListKey? = entries.firstOrNull { it.key == key }
    }
}

/** A named list of titles, read when a run asks for it. */
interface TitleSource {
    val key: String
    val label: String

    /** The list's titles in its own order, at most [limit]. Throws when it cannot be read. */
    @Throws(IOException::class)
    fun titles(limit: Int): List<Title>
}

/** The kinds of picture a wallpaper is made from. */
enum class ImageKind { BACKDROP, LOGO, POSTER }

/** Where titles come from: the lists, each title's details and its pictures. */
interface Source {
    /** Stored with the set and the report: "tmdb", "sample". */
    val name: String

    fun list(key: ListKey, lang: Lang): TitleSource

    /** The title's details in [lang], or null when they could not be read. */
    fun details(title: Title, lang: Lang): TitleDetails?

    /**
     * The picture [ref] names (a [TitleDetails] image reference), as a file in the source cache,
     * fetched when it is not there yet; null when it could not be had.
     */
    fun image(ref: String, kind: ImageKind): File?

    /** The picture [ref] names if it is already in the source cache; never fetched. */
    fun cachedImage(ref: String, kind: ImageKind): File? = null

    /** A web page about the title, or null. */
    fun pageUrl(media: Media, id: Int): String?
}
