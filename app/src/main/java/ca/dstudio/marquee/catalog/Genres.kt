package ca.dstudio.marquee.catalog

import androidx.annotation.StringRes
import ca.dstudio.marquee.R

/** Which of TMDB's two genre lists a genre belongs to. */
enum class GenreScope { FILMS, SHOWS, BOTH }

/** One of TMDB's genres, by its id; films and shows share an id where TMDB does. */
class Genre(val id: Int, @param:StringRes val label: Int, val scope: GenreScope)

/** TMDB's film and show genres, by name. */
object Genres {

    val all: List<Genre> = listOf(
        Genre(28, R.string.genre_action, GenreScope.FILMS),
        Genre(10759, R.string.genre_action_adventure, GenreScope.SHOWS),
        Genre(12, R.string.genre_adventure, GenreScope.FILMS),
        Genre(16, R.string.genre_animation, GenreScope.BOTH),
        Genre(35, R.string.genre_comedy, GenreScope.BOTH),
        Genre(80, R.string.genre_crime, GenreScope.BOTH),
        Genre(99, R.string.genre_documentary, GenreScope.BOTH),
        Genre(18, R.string.genre_drama, GenreScope.BOTH),
        Genre(10751, R.string.genre_family, GenreScope.BOTH),
        Genre(14, R.string.genre_fantasy, GenreScope.FILMS),
        Genre(36, R.string.genre_history, GenreScope.FILMS),
        Genre(27, R.string.genre_horror, GenreScope.FILMS),
        Genre(10762, R.string.genre_kids, GenreScope.SHOWS),
        Genre(10402, R.string.genre_music, GenreScope.FILMS),
        Genre(9648, R.string.genre_mystery, GenreScope.BOTH),
        Genre(10763, R.string.genre_news, GenreScope.SHOWS),
        Genre(10764, R.string.genre_reality, GenreScope.SHOWS),
        Genre(10749, R.string.genre_romance, GenreScope.FILMS),
        Genre(878, R.string.genre_science_fiction, GenreScope.FILMS),
        Genre(10765, R.string.genre_sci_fi_fantasy, GenreScope.SHOWS),
        Genre(10766, R.string.genre_soap, GenreScope.SHOWS),
        Genre(10767, R.string.genre_talk, GenreScope.SHOWS),
        Genre(53, R.string.genre_thriller, GenreScope.FILMS),
        Genre(10770, R.string.genre_tv_film, GenreScope.FILMS),
        Genre(10752, R.string.genre_war, GenreScope.FILMS),
        Genre(10768, R.string.genre_war_politics, GenreScope.SHOWS),
        Genre(37, R.string.genre_western, GenreScope.BOTH),
    )
}
