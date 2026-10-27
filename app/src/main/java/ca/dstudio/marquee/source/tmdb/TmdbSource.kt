package ca.dstudio.marquee.source.tmdb

import android.content.Context
import android.util.Log
import ca.dstudio.marquee.catalog.Lang
import ca.dstudio.marquee.catalog.Media
import ca.dstudio.marquee.catalog.Title
import ca.dstudio.marquee.catalog.TitleDetails
import ca.dstudio.marquee.source.ImageKind
import ca.dstudio.marquee.source.ListKey
import ca.dstudio.marquee.source.Source
import ca.dstudio.marquee.source.SourceCache
import ca.dstudio.marquee.source.TitleSource
import ca.dstudio.marquee.work.Net
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * TMDB: its lists, each title's details with its images, and the pictures through the source cache.
 * Details are kept in memory for the life of this object, which is one run.
 */
class TmdbSource(
    context: Context,
    token: String,
    private val cache: SourceCache,
) : Source {

    private val app = context.applicationContext
    private val client = TmdbClient(token)
    private val detailsMemo = ConcurrentHashMap<String, TitleDetails>()

    override val name = "tmdb"

    override fun list(key: ListKey, lang: Lang): TitleSource = object : TitleSource {
        override val key = key.key
        override val label: String = app.getString(key.label)

        override fun titles(limit: Int): List<Title> {
            val (path, mediaHint, regional) = when (key) {
                ListKey.TRENDING -> Triple("/3/trending/all/week", null, false)
                ListKey.IN_CINEMAS -> Triple("/3/movie/now_playing", Media.MOVIE, true)
                ListKey.POPULAR_FILMS -> Triple("/3/movie/popular", Media.MOVIE, false)
                ListKey.UPCOMING -> Triple("/3/movie/upcoming", Media.MOVIE, true)
                ListKey.TOP_RATED_SHOWS -> Triple("/3/tv/top_rated", Media.TV, false)
                ListKey.ON_THE_AIR -> Triple("/3/tv/on_the_air", Media.TV, false)
            }
            val out = ArrayList<Title>()
            var page = 1
            var pages = 1
            while (out.size < limit && page <= pages && page <= MAX_PAGES) {
                val query = linkedMapOf("language" to lang.tag, "page" to "$page")
                if (regional && lang.country.isNotEmpty()) query["region"] = lang.country
                val answer = client.page(path, query)
                pages = answer.totalPages ?: 1
                for (r in answer.results) {
                    val media = mediaHint ?: Media.of(r.mediaType) ?: continue
                    val id = r.id ?: continue
                    out += Title(
                        media = media,
                        id = id,
                        name = (r.title ?: r.name).orEmpty(),
                        releaseDate = (r.releaseDate ?: r.firstAirDate)?.takeIf { it.isNotBlank() },
                        adult = r.adult == true,
                        genreIds = r.genreIds,
                        rank = out.size + 1,
                        poster = r.posterPath?.takeIf { it.isNotBlank() }?.let { REF + it },
                    )
                }
                page++
            }
            return out.take(limit)
        }
    }

    override fun details(title: Title, lang: Lang): TitleDetails? {
        detailsMemo[title.key]?.let { return it }
        val imageLanguages = listOf(lang.two, "en", "null").distinct().joinToString(",")
        val d = try {
            client.details(title.media.key, title.id, lang.tag, imageLanguages)
        } catch (e: IOException) {
            Log.w(TAG, "details ${title.key}: ${Net.describe(e)}")
            return null
        }
        val logo = pickLogo(d.images?.logos.orEmpty(), lang.two)
        var overview = d.overview.orEmpty().trim()
        var overviewLanguage: String? = null
        if (overview.isEmpty() && lang.two != ENGLISH) {
            // None in the user's language: English, when TMDB has it.
            overview = try {
                client.overview(title.media.key, title.id, ENGLISH).overview.orEmpty().trim()
            } catch (e: IOException) {
                Log.w(TAG, "overview ${title.key}: ${Net.describe(e)}")
                ""
            }
            if (overview.isNotEmpty()) overviewLanguage = ENGLISH
        }
        val details = TitleDetails(
            media = title.media.key,
            id = title.id,
            name = (d.title ?: d.name)?.takeIf { it.isNotBlank() } ?: title.name,
            releaseDate = (d.releaseDate ?: d.firstAirDate)?.takeIf { it.isNotBlank() } ?: title.releaseDate,
            genres = d.genres.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } },
            genreIds = d.genres.mapNotNull { it.id }.ifEmpty { title.genreIds },
            runtimeMinutes = if (title.media == Media.MOVIE) d.runtime?.takeIf { it > 0 } else null,
            seasons = if (title.media == Media.TV) d.numberOfSeasons?.takeIf { it > 0 } else null,
            overview = overview,
            overviewLanguage = overviewLanguage,
            voteAverage = d.voteAverage?.takeIf { it > 0.0 && (d.voteCount ?: 1) > 0 },
            adult = d.adult == true || title.adult,
            logo = logo?.filePath?.let { REF + it },
            logoLanguage = logo?.language,
            backdrop = pickBackdrop(d.images?.backdrops.orEmpty())?.let { REF + it }
                ?: d.backdropPath?.takeIf { it.isNotBlank() }?.let { REF + it },
            poster = d.posterPath?.takeIf { it.isNotBlank() }?.let { REF + it },
            streaming = WatchProviders.names(d.watchProviders, WatchProviders.country(Locale.getDefault())),
        )
        detailsMemo[title.key] = details
        return details
    }

    override fun cachedImage(ref: String, kind: ImageKind): File? =
        if (ref.startsWith(REF)) cache.hit(cacheFile(ref.removePrefix(REF), kind)) else null

    private fun sizeOf(kind: ImageKind) = when (kind) {
        ImageKind.BACKDROP -> "original"
        ImageKind.LOGO -> "w500"
        ImageKind.POSTER -> "w342"
    }

    private fun cacheFile(path: String, kind: ImageKind): File {
        val ext = path.substringAfterLast('.', "jpg").lowercase().take(4)
        return cache.fileFor("tmdb:${sizeOf(kind)}$path", ext)
    }

    override fun image(ref: String, kind: ImageKind): File? {
        if (!ref.startsWith(REF)) return null
        val path = ref.removePrefix(REF)
        val size = sizeOf(kind)
        val file = cacheFile(path, kind)
        cache.hit(file)?.let { return it }
        return try {
            Net.download(TmdbClient.imageUrl(size, path), file, Net.IMAGE_CAP)
            file
        } catch (e: IOException) {
            Log.w(TAG, "image ${kind.name.lowercase()}: ${Net.describe(e)}")
            null
        }
    }

    override fun pageUrl(media: Media, id: Int): String = "https://www.themoviedb.org/${media.key}/$id"

    private companion object {
        const val TAG = "Marquee"
        const val REF = "tmdb:"
        const val MAX_PAGES = 2
        const val ENGLISH = "en"

        /** The user's language first, then English; the best voted PNG of those. */
        fun pickLogo(logos: List<TmdbImage>, two: String): TmdbImage? {
            val png = logos.filter { it.filePath?.endsWith(".png", ignoreCase = true) == true }
            for (language in listOf(two, "en").distinct()) {
                png.filter { it.language == language }.maxByOrNull { it.voteAverage ?: 0.0 }?.let { return it }
            }
            return null
        }

        /**
         * Textless first: a backdrop with no language, at least 1280 wide; full-HD ones before
         * smaller ones, then the best voted, then the widest. Null when there is none.
         */
        fun pickBackdrop(backdrops: List<TmdbImage>): String? = backdrops
            .filter { it.language == null && (it.width ?: 0) >= 1280 && !it.filePath.isNullOrBlank() }
            .sortedWith(
                compareByDescending<TmdbImage> { (it.width ?: 0) >= 1920 }
                    .thenByDescending { it.voteAverage ?: 0.0 }
                    .thenByDescending { it.width ?: 0 },
            )
            .firstOrNull()?.filePath
    }
}
