package ca.dstudio.marquee.source.tmdb

import java.util.Locale

/** Where a title streams, as the footer names it. */
internal object WatchProviders {

    /** At most this many services are named. */
    const val MAX = 3

    /** The country streaming availability is read for: the locale's, else the US. */
    fun country(locale: Locale): String = locale.country.uppercase(Locale.ROOT).ifBlank { "US" }

    /**
     * The subscription services [country] streams the title on, in TMDB's display order, at most
     * [MAX]; empty when there is none or no entry for the country.
     */
    fun names(providers: TmdbWatchProviders?, country: String): List<String> =
        providers?.results?.get(country)?.flatrate.orEmpty()
            .filter { !it.name.isNullOrBlank() }
            .sortedBy { it.displayPriority ?: Int.MAX_VALUE }
            .mapNotNull { it.name?.trim()?.let(::baseName) }
            // TMDB spells some services two ways ("Paramount Plus", "Paramount+"): the first one wins.
            .distinctBy { it.lowercase(Locale.ROOT).replace("+", "plus").filter(Char::isLetterOrDigit) }
            .take(MAX)

    /**
     * The service a TMDB provider belongs to: TMDB lists each plan and each store channel on its own
     * ("Netflix Standard with Ads", "Paramount Plus Premium", "HBO Max Amazon Channel"), and the
     * footer names the service once.
     */
    fun baseName(name: String): String {
        var base = name
        while (true) {
            val suffix = SUFFIXES.firstOrNull { base.endsWith(it, ignoreCase = true) && base.length > it.length } ?: return base
            base = base.dropLast(suffix.length).trim()
        }
    }

    // Longest first, so "Standard with Ads" goes before "with Ads".
    private val SUFFIXES = listOf(
        " Roku Premium Channel", " Apple TV Channel", " Amazon Channel",
        " Standard with Ads", " Basic with Ads", " with Ads",
        " Premium Plus", " Premium", " Essential", " Standard", " Basic",
    )
}
