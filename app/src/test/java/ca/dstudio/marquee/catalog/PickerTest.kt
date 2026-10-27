package ca.dstudio.marquee.catalog

import ca.dstudio.marquee.compose.Layout
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.compose.TextPosition
import ca.dstudio.marquee.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PickerTest {

    private val settings = Settings(
        lists = listOf("a", "b"),
        titlesPerList = 2,
        pick = "top",
        order = "lists",
        releasedWithinYears = 0,
        hideAdult = true,
        excludedGenres = emptySet(),
        needsLogo = false,
        needsSynopsis = false,
        noRepeats = false,
        language = "",
        look = Look(Layout.CINEMATIC, TextPosition.TOP_LEFT, 3, true),
        pictureSize = 1080,
        intervalHours = 12,
    )

    private val today = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 27) }

    private fun title(id: Int, date: String = "2025-01-01", adult: Boolean = false, genres: List<Int> = emptyList()) =
        Title(Media.MOVIE, id, "Title $id", date, adult, genres, rank = id)

    private fun detailsOf(t: Title, backdrop: String? = "/b.jpg") =
        TitleDetails(Media.MOVIE.key, t.id, t.name, releaseDate = t.releaseDate, genreIds = t.genreIds, adult = t.adult, backdrop = backdrop)

    private fun pick(
        lists: List<ListInput>,
        s: Settings = settings,
        previous: Set<String> = emptySet(),
        details: (Title) -> TitleDetails? = { detailsOf(it) },
    ) = Picker(s, "en", today = today).pick(lists, previous, details) { Unit }

    private fun status(result: PickResult<Unit>, list: Int) = result.lists[list].candidates.map { it.status }

    @Test
    fun eachListGivesItsCountFromTheTop() {
        val result = pick(listOf(ListInput("a", "A", (1..4).map { title(it) })))
        assertEquals(listOf(1, 2), result.picks.map { it.details.id })
        assertEquals(listOf("used", "used", "over_count", "over_count"), status(result, 0))
    }

    @Test
    fun aTitleTwoListsShareIsUsedOnce() {
        val shared = title(1)
        val result = pick(
            listOf(
                ListInput("a", "A", listOf(shared, title(2))),
                ListInput("b", "B", listOf(shared, title(3))),
            ),
        )
        assertEquals(listOf(1, 2, 3), result.picks.map { it.details.id })
        assertEquals(listOf("duplicate", "used"), status(result, 1))
    }

    @Test
    fun theFiltersLeaveTitlesOutWithTheirReason() {
        val s = settings.copy(titlesPerList = 10, releasedWithinYears = 5, excludedGenres = setOf(27), noRepeats = true)
        val titles = listOf(
            title(1, adult = true),
            title(2, date = "2015-06-01"),
            title(3, genres = listOf(27)),
            title(4),
            title(5),
            title(6),
            title(7),
        )
        val result = pick(
            listOf(ListInput("a", "A", titles)),
            s = s,
            previous = setOf("movie/4"),
            details = { t ->
                when (t.id) {
                    5 -> detailsOf(t, backdrop = null)
                    6 -> null
                    else -> detailsOf(t)
                }
            },
        )
        assertEquals(listOf(7), result.picks.map { it.details.id })
        assertEquals(listOf("adult", "older", "genre", "repeat", "no_backdrop", "failed", "used"), status(result, 0))
    }

    @Test
    fun aListThatCouldNotBeReadIsReportedAndTheOthersStillGive() {
        val result = pick(listOf(ListInput("a", "A", null), ListInput("b", "B", listOf(title(1)))))
        assertEquals(listOf(1), result.picks.map { it.details.id })
        assertEquals("failed", result.lists[0].error)
    }
}
