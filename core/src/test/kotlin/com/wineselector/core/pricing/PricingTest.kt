package com.wineselector.core.pricing

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PricingTest {

    private fun offer(title: String, price: Double, merchant: String = "Shop $price") =
        PriceOffer(merchant, title, price, "£", "https://example.com/$price")

    private class FakeProvider(val results: Map<String, List<PriceOffer>>, override val isConfigured: Boolean = true) : PriceProvider {
        override val name = "Fake"
        val queries = mutableListOf<String>()
        override suspend fun search(query: String, countryCode: String?): List<PriceOffer> {
            queries += query
            return results[query] ?: emptyList()
        }
    }

    @Test fun `vintage specific prices are preferred`() = runTest {
        val provider = FakeProvider(mapOf(
            "Sileni Sauvignon Blanc 2022" to listOf(
                offer("Sileni Cellar Selection Sauvignon Blanc 2022", 11.0),
                offer("Sileni Sauvignon Blanc 2022 75cl", 12.5),
                offer("Sileni Sauvignon Blanc 2021", 9.0),
                offer("Sileni Sauvignon Blanc 6 x 75cl", 60.0)
            )
        ))
        val r = PriceService(provider).lookup("Sileni Sauvignon Blanc", 2022, "gb") as PriceResult.Found
        assertTrue(r.summary.vintageSpecific)
        assertEquals(2, r.summary.offers.size)
        assertEquals(11.0, r.summary.low, 0.001)
        assertEquals(11.75, r.summary.median, 0.001)
        assertEquals(listOf("Sileni Sauvignon Blanc 2022"), provider.queries)
    }

    @Test fun `falls back to any vintage and says so`() = runTest {
        val provider = FakeProvider(mapOf(
            "Tignanello" to listOf(offer("Antinori Tignanello 2019", 120.0), offer("Tignanello 2020 Toscana IGT", 130.0))
        ))
        val r = PriceService(provider).lookup("Tignanello", 2016, null) as PriceResult.Found
        assertFalse(r.summary.vintageSpecific)
        assertEquals(listOf("Tignanello 2016", "Tignanello"), provider.queries)
    }

    @Test fun `irrelevant listings and outliers are dropped`() {
        val offers = listOf(
            offer("Cloudy Bay Sauvignon Blanc", 25.0), offer("Cloudy Bay Sauvignon Blanc 75cl", 27.0),
            offer("Cloudy Bay Sauvignon Blanc", 26.0, "Other"), offer("Cloudy Bay Sauvignon Blanc", 260.0, "Typo shop"),
            offer("Cloudy Bay Pinot Noir", 30.0), offer("Cloudy Bay Sauvignon Blanc Magnum", 55.0),
            offer("Wine glasses set Cloudy Bay Sauvignon Blanc", 15.0)
        )
        val kept = OfferFilter.filter(offers, "Cloudy Bay Sauvignon Blanc", null, strictVintage = false)
        assertEquals(listOf(25.0, 27.0, 26.0), kept.map { it.price })
    }

    @Test fun `no key means unavailable not an error`() = runTest {
        val r = PriceService(FakeProvider(emptyMap(), isConfigured = false)).lookup("Anything", null, null)
        assertTrue(r is PriceResult.Unavailable)
    }

    @Test fun `results are cached`() = runTest {
        val provider = FakeProvider(mapOf("Rioja Reserva" to listOf(offer("Rioja Reserva", 15.0))))
        val service = PriceService(provider)
        service.lookup("Rioja Reserva", null, null)
        service.lookup("Rioja Reserva", null, null)
        assertEquals(1, provider.queries.size)
    }

    @Test fun `serpapi response parsing`() {
        val body = """{"shopping_results":[
            {"position":1,"title":"Whispering Angel Rosé 2023","source":"Majestic","price":"£19.99","extracted_price":19.99,"product_link":"https://g.co/p/1"},
            {"position":2,"title":"Whispering Angel Rosé","source":"Tesco","price":"£21.00","link":"https://t.co/2"},
            {"position":3,"title":"No price"}]}"""
        val offers = SerpApiShoppingProvider.parse(body)
        assertEquals(2, offers.size)
        assertEquals("Majestic", offers[0].merchant)
        assertEquals("£", offers[0].currencySymbol)
        assertEquals(21.0, offers[1].price, 0.001)
        assertEquals("https://t.co/2", offers[1].url)
    }

    @Test fun `serpapi price text with decimal comma`() {
        assertEquals(1299.0, SerpApiShoppingProvider.parsePrice("1.299,00 €")!!, 0.001)
        assertEquals(24.5, SerpApiShoppingProvider.parsePrice("24,50 €")!!, 0.001)
        assertEquals(1250.0, SerpApiShoppingProvider.parsePrice("\$1,250")!!, 0.001)
    }

    @Test fun `serpapi request carries engine query country and key`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"shopping_results":[{"title":"Barolo 2016","source":"S","extracted_price":55.0,"price":"$55.00"}]}"""))
        server.start()
        try {
            val provider = SerpApiShoppingProvider({ "KEY123" }, OkHttpClient(), server.url("/"))
            val offers = provider.search("Barolo 2016", "US")
            assertEquals(55.0, offers.single().price, 0.001)
            val url = server.takeRequest().requestUrl!!
            assertEquals("/search.json", url.encodedPath)
            assertEquals("google_shopping", url.queryParameter("engine"))
            assertEquals("Barolo 2016", url.queryParameter("q"))
            assertEquals("us", url.queryParameter("gl"))
            assertEquals("KEY123", url.queryParameter("api_key"))
        } finally { server.shutdown() }
    }

    @Test fun `serpapi api error surfaces as failure`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Invalid API key."}"""))
        server.start()
        try {
            val provider = SerpApiShoppingProvider({ "bad" }, OkHttpClient(), server.url("/"))
            val r = PriceService(provider).lookup("Barolo", null, null)
            assertEquals("Invalid API key.", (r as PriceResult.Failed).message)
        } finally { server.shutdown() }
    }

    @Test fun `markup verdicts`() {
        val s = PriceSummary("q", emptyList(), 10.0, 20.0, 30.0, "£", true, "x", 0)
        assertEquals(MarkupVerdict.TYPICAL, MarkupAnalyzer.analyze(55.0, "£", s)!!.verdict)
        assertEquals(MarkupVerdict.BARGAIN, MarkupAnalyzer.analyze(24.0, "£", s)!!.verdict)
        assertEquals(MarkupVerdict.VERY_STEEP, MarkupAnalyzer.analyze(95.0, "£", s)!!.verdict)
        assertNull(MarkupAnalyzer.analyze(55.0, "$", s))
        assertEquals(MarkupVerdict.TYPICAL, MarkupAnalyzer.analyze(27.0, "£", s, halfBottle = true)!!.verdict)
    }

    @Test fun `links are well formed`() {
        assertEquals("https://www.wine-searcher.com/find/chateau+musar/2015", PriceLinks.wineSearcher("Château Musar", 2015))
        assertTrue(PriceLinks.vivino("Château Musar").startsWith("https://www.vivino.com/search/wines?q=Ch"))
        assertTrue(PriceLinks.googleShopping("Musar", 2015).endsWith("q=Musar+2015"))
    }

    @Test fun `file cache round trip`() {
        val dir = Files.createTempDirectory("pc").toFile()
        val f = File(dir, "prices.json")
        val summary = PriceSummary("q", listOf(offer("A", 10.0).copy(vintageMatched = true)), 10.0, 10.0, 10.0, "£", true, "Fake", 123L)
        JsonFilePriceCache(f).put("k", summary)
        assertEquals(summary, JsonFilePriceCache(f).get("k"))
        f.writeText("not json")
        assertNull(JsonFilePriceCache(f).get("k"))
        dir.deleteRecursively()
    }
}
