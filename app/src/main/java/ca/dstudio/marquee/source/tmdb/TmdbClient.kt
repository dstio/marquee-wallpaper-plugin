package ca.dstudio.marquee.source.tmdb

import ca.dstudio.marquee.work.Net
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

/**
 * TMDB's API v3, authorised with a v4 read access token in the `Authorization` header. The token
 * never leaves this class: it is not logged, and no URL carries it.
 */
internal class TmdbClient(private val token: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Throws(IOException::class)
    fun page(path: String, query: Map<String, String>): TmdbPage = get(path, query)

    @Throws(IOException::class)
    fun details(mediaKey: String, id: Int, language: String, imageLanguages: String): TmdbDetails = get(
        "/3/$mediaKey/$id",
        mapOf(
            "language" to language,
            "append_to_response" to "images,external_ids,watch/providers",
            "include_image_language" to imageLanguages,
        ),
    )

    /** The title's overview alone, in [language]. */
    @Throws(IOException::class)
    fun overview(mediaKey: String, id: Int, language: String): TmdbOverview =
        get("/3/$mediaKey/$id", mapOf("language" to language))

    private inline fun <reified T> get(path: String, query: Map<String, String>): T {
        val url = API.newBuilder()
            .encodedPath(path)
            .apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()
        val bytes = Net.get(url, Net.JSON_CAP, mapOf("Authorization" to "Bearer $token", "Accept" to "application/json"))
        return try {
            json.decodeFromString<T>(bytes.decodeToString())
        } catch (e: IllegalArgumentException) {
            throw IOException("unreadable answer for $path", e)
        }
    }

    companion object {
        private val API: HttpUrl = "https://api.themoviedb.org/".toHttpUrl()
        private val IMAGES: HttpUrl = "https://image.tmdb.org/".toHttpUrl()

        /** An image of TMDB's at [size] (`original`, `w500`, `w342`). */
        fun imageUrl(size: String, filePath: String): HttpUrl =
            IMAGES.newBuilder().encodedPath("/t/p/$size$filePath").build()
    }
}
