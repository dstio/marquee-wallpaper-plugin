package ca.dstudio.marquee.source.sample

import android.content.Context
import android.graphics.Bitmap
import ca.dstudio.marquee.catalog.Lang
import ca.dstudio.marquee.catalog.Media
import ca.dstudio.marquee.catalog.Title
import ca.dstudio.marquee.catalog.TitleDetails
import ca.dstudio.marquee.source.ImageKind
import ca.dstudio.marquee.source.ListKey
import ca.dstudio.marquee.source.Source
import ca.dstudio.marquee.source.SourceCache
import ca.dstudio.marquee.source.TitleSource
import java.io.File
import java.io.IOException

/**
 * Twelve made-up titles with painted backdrops and posters and no logos, for a debug build with no
 * TMDB token: the picker, the composer, the store and the launcher can all be checked offline.
 * Every title, story and picture here is invented.
 */
class SampleSource(context: Context, private val cache: SourceCache) : Source {

    private val app = context.applicationContext

    override val name = "sample"

    override fun list(key: ListKey, lang: Lang): TitleSource = object : TitleSource {
        override val key = key.key
        override val label: String = app.getString(key.label)

        override fun titles(limit: Int): List<Title> {
            val order = when (key) {
                ListKey.TRENDING -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
                ListKey.IN_CINEMAS -> listOf(2, 7, 1, 5, 10, 8)
                ListKey.POPULAR_FILMS -> listOf(1, 2, 5, 7, 8, 10)
                ListKey.UPCOMING -> listOf(10, 7, 5)
                ListKey.TOP_RATED_SHOWS -> listOf(0, 3, 4, 6, 9, 11)
                ListKey.ON_THE_AIR -> listOf(6, 0, 9, 11)
            }
            return order.take(limit).mapIndexed { i, n ->
                val t = TITLES[n]
                Title(t.media, ID_BASE + n, t.name, t.date, false, t.genreIds, i + 1, "$REF$n/poster/v$PAINT_VERSION")
            }
        }
    }

    override fun details(title: Title, lang: Lang): TitleDetails? {
        val n = title.id - ID_BASE
        val t = TITLES.getOrNull(n) ?: return null
        return TitleDetails(
            media = t.media.key,
            id = title.id,
            name = t.name,
            releaseDate = t.date,
            genres = t.genres,
            genreIds = t.genreIds,
            runtimeMinutes = t.runtime,
            seasons = t.seasons,
            overview = t.overview,
            voteAverage = t.vote,
            logo = null,
            backdrop = "$REF$n/backdrop/v$PAINT_VERSION",
            poster = "$REF$n/poster/v$PAINT_VERSION",
        )
    }

    override fun image(ref: String, kind: ImageKind): File? {
        if (!ref.startsWith(REF)) return null
        val n = ref.removePrefix(REF).substringBefore('/').toIntOrNull() ?: return null
        if (n !in TITLES.indices) return null
        val file = cache.fileFor(ref, "jpg")
        cache.hit(file)?.let { return it }
        val bitmap = when (kind) {
            ImageKind.BACKDROP -> SamplePainter.backdrop(n, 1920, 1080)
            ImageKind.POSTER -> SamplePainter.poster(n, TITLES[n].name, 342, 513)
            ImageKind.LOGO -> return null
        }
        return try {
            val tmp = File(file.path + ".tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            if (!tmp.renameTo(file)) throw IOException("could not move ${file.name}")
            file
        } catch (e: IOException) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    /** Painted on the device, so always to hand. */
    override fun cachedImage(ref: String, kind: ImageKind): File? = image(ref, kind)

    /** Made-up titles have no page. */
    override fun pageUrl(media: Media, id: Int): String? = null

    private class Sample(
        val media: Media,
        val name: String,
        val date: String,
        val genres: List<String>,
        val genreIds: List<Int>,
        val runtime: Int?,
        val seasons: Int?,
        val vote: Double,
        val overview: String,
    )

    private companion object {
        const val REF = "sample:"
        const val ID_BASE = 900_000

        /** Bumped when the painter's pictures change, so cached ones are painted again. */
        const val PAINT_VERSION = 6

        val TITLES = listOf(
            Sample(Media.TV, "The Kestrel Line", "2025-03-14", listOf("Drama", "Mystery"), listOf(18, 9648), null, 3, 7.9,
                "When the last keeper of a remote lighthouse vanishes, his estranged daughter returns to the island and finds the logbook still being written, one night at a time."),
            Sample(Media.MOVIE, "Northbound", "2024-11-02", listOf("Adventure", "Drama"), listOf(12, 18), 131, null, 7.4,
                "Two estranged brothers drive a failing sled team to the coast before the ice road closes for the season, and learn what their father never told them."),
            Sample(Media.MOVIE, "Dust Road", "2025-06-20", listOf("Thriller"), listOf(53), 108, null, 7.1,
                "A courier with one delivery left crosses the salt flats at night, followed by a car that never turns its headlights on."),
            Sample(Media.TV, "Glass Orchard", "2025-09-05", listOf("Drama", "Family"), listOf(18, 10751), null, 1, 6.8,
                "Three sisters inherit a failing apple farm in the hills and a greenhouse their mother kept locked for thirty years."),
            Sample(Media.TV, "Harbour Saints", "2016-04-10", listOf("Crime", "Drama"), listOf(80, 18), null, 5, 8.2,
                "A harbour police unit works the night shift on the busiest docks of the coast, where every container has a story and most of them are lies."),
            Sample(Media.MOVIE, "Low Orbit", "2023-02-17", listOf("Science Fiction"), listOf(878), 117, null, 6.9,
                "The last crew of an ageing space station has nine days to bring it down safely, and one of them does not want to go home."),
            Sample(Media.TV, "Paper Wolves", "2026-01-08", listOf("Comedy"), listOf(35), null, 2, 7.6,
                "A struggling city newspaper hires its first new reporter in a decade, who turns out to be far better at inventing news than finding it."),
            Sample(Media.MOVIE, "Ninth Floor", "2025-10-31", listOf("Horror", "Mystery"), listOf(27, 9648), 96, null, 6.4,
                "A night guard in an empty office tower keeps getting calls from the ninth floor, which the building's plans say was never built."),
            Sample(Media.MOVIE, "Salt Kings", "2021-07-23", listOf("Drama", "History"), listOf(18, 36), 142, null, 7.8,
                "Two families fight over the salt marshes of a small coastal town for three generations, until the sea settles it for them."),
            Sample(Media.TV, "The Quiet Hour", "2024-05-12", listOf("Drama"), listOf(18), null, 4, 8.0,
                "A late-night radio host takes calls from a town that cannot sleep, and one caller seems to know what happens before it does."),
            Sample(Media.MOVIE, "Red Ledger", "2026-02-27", listOf("Crime", "Thriller"), listOf(80, 53), 124, null, 7.2,
                "An accountant finds one number that does not add up in a family firm's books, and the family would rather she forgot it."),
            Sample(Media.TV, "Mirror Lake", "2022-08-19", listOf("Mystery", "Drama"), listOf(9648, 18), null, 2, 7.3,
                "Every winter the lake freezes so clear you can see the bottom, and this year the town sees something it buried there."),
        )
    }
}
