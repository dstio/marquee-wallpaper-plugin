package ca.dstudio.marquee.catalog

import kotlinx.serialization.Serializable

/** Why a candidate was left out of the set; [key] is what the report stores. */
enum class Skip(val key: String) {
    OLDER("older"),
    ADULT("adult"),
    GENRE("genre"),
    NO_LOGO("no_logo"),
    NO_SYNOPSIS("no_synopsis"),
    NO_BACKDROP("no_backdrop"),
    REPEAT("repeat"),
    DUPLICATE("duplicate"),
    OVER_COUNT("over_count"),
    FAILED("failed"),
}

/**
 * Every candidate of every list in the last run, in list order, with `used` or why it was left
 * out. Written to `filesDir/report.json`; the settings' Debug page reads it.
 */
@Serializable
data class RunReport(
    val createdAt: Long,
    val source: String,
    val lists: List<ListReport>,
    /** The run's language (BCP 47), for the reasons that name it. */
    val lang: String = "",
    /** The "released within" filter the run used, in years (0 for any age). */
    val withinYears: Int = 0,
    /** How the run picked each list's titles (`random` or `top`), and how many it took from each. */
    val pick: String = "",
    val titlesPerList: Int = 0,
) {
    companion object {
        const val USED = "used"
    }
}

@Serializable
data class ListReport(
    val key: String,
    val label: String,
    /** Set when the list itself could not be read; [candidates] is then empty. */
    val error: String? = null,
    val candidates: List<Candidate> = emptyList(),
)

@Serializable
data class Candidate(
    val media: String,
    val id: Int,
    val title: String,
    val rank: Int,
    val status: String,
    val year: Int? = null,
    /** The poster as an image reference, or null. */
    val poster: String? = null,
)
