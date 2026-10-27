package ca.dstudio.marquee.source.tmdb

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WatchProvidersTest {

    private fun provider(name: String, priority: Int) = TmdbWatchProvider(name, priority)

    private val providers = TmdbWatchProviders(
        mapOf(
            "US" to TmdbWatchCountry(
                flatrate = listOf(
                    provider("Hulu", 4),
                    provider("Netflix", 1),
                    provider("Max", 9),
                    provider("Disney Plus", 2),
                ),
            ),
            "CA" to TmdbWatchCountry(flatrate = listOf(provider("Crave", 3))),
            // Rent and buy only: no subscription service.
            "GB" to TmdbWatchCountry(),
        ),
    )

    @Test
    fun countryComesFromTheLocaleElseTheUs() {
        assertEquals("CA", WatchProviders.country(Locale.CANADA))
        assertEquals("US", WatchProviders.country(Locale.ENGLISH))
    }

    @Test
    fun subscriptionServicesInPriorityOrderAtMostThree() {
        assertEquals(listOf("Netflix", "Disney Plus", "Hulu"), WatchProviders.names(providers, "US"))
    }

    @Test
    fun theCountrysOwnEntry() {
        assertEquals(listOf("Crave"), WatchProviders.names(providers, "CA"))
    }

    @Test
    fun eachServiceOnceByItsBaseName() {
        val tiers = TmdbWatchProviders(
            mapOf(
                "US" to TmdbWatchCountry(
                    flatrate = listOf(
                        provider("Netflix", 1),
                        provider("Netflix Standard with Ads", 2),
                        provider("Paramount Plus Premium", 3),
                        provider("Paramount Plus Essential", 4),
                        provider("HBO Max Amazon Channel", 5),
                        provider("HBO Max", 6),
                        provider("Paramount+", 4),
                        provider("Peacock Premium Plus", 7),
                    ),
                ),
            ),
        )
        assertEquals(listOf("Netflix", "Paramount Plus", "HBO Max"), WatchProviders.names(tiers, "US"))
        // A name that is only a suffix word is left as it is; "Plus" alone is part of a name.
        assertEquals("Premium", WatchProviders.baseName("Premium"))
        assertEquals("Peacock", WatchProviders.baseName("Peacock Premium Plus"))
        assertEquals("Disney Plus", WatchProviders.baseName("Disney Plus"))
    }

    @Test
    fun noneWhenTheCountryHasNoSubscriptionOrNoEntry() {
        assertEquals(emptyList<String>(), WatchProviders.names(providers, "GB"))
        assertEquals(emptyList<String>(), WatchProviders.names(providers, "FR"))
        assertEquals(emptyList<String>(), WatchProviders.names(null, "US"))
    }
}
