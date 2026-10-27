package ca.dstudio.marquee.source.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TMDB's v3 answers, as far as Marquee reads them. Every field is optional: a missing or null one
// takes its default rather than failing the title.

@Serializable
internal data class TmdbPage(
    val page: Int? = null,
    val results: List<TmdbResult> = emptyList(),
    @SerialName("total_pages") val totalPages: Int? = null,
)

@Serializable
internal data class TmdbResult(
    val id: Int? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val adult: Boolean? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
internal data class TmdbDetails(
    val id: Int? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val runtime: Int? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val overview: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val adult: Boolean? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    val images: TmdbImages? = null,
    @SerialName("watch/providers") val watchProviders: TmdbWatchProviders? = null,
)

@Serializable
internal data class TmdbGenre(
    val id: Int? = null,
    val name: String? = null,
)

@Serializable
internal data class TmdbImages(
    val backdrops: List<TmdbImage> = emptyList(),
    val logos: List<TmdbImage> = emptyList(),
    val posters: List<TmdbImage> = emptyList(),
)

@Serializable
internal data class TmdbImage(
    @SerialName("file_path") val filePath: String? = null,
    @SerialName("iso_639_1") val language: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)

/** Where a title streams, rents and sells, by ISO 3166-1 country (TMDB's data comes from JustWatch). */
@Serializable
internal data class TmdbWatchProviders(
    val results: Map<String, TmdbWatchCountry> = emptyMap(),
)

@Serializable
internal data class TmdbWatchCountry(
    /** Subscription services. Marquee leaves out `rent`, `buy`, `free` and `ads`. */
    val flatrate: List<TmdbWatchProvider> = emptyList(),
)

@Serializable
internal data class TmdbWatchProvider(
    @SerialName("provider_name") val name: String? = null,
    @SerialName("display_priority") val displayPriority: Int? = null,
)

/** The bare overview, for the English fallback. */
@Serializable
internal data class TmdbOverview(
    val overview: String? = null,
)
