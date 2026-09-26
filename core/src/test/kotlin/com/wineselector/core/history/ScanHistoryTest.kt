package com.wineselector.core.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ScanHistoryTest {
    private fun record(id: String) = ScanRecord(id, 1000L, ScanKind.MENU, "/x/$id.jpg", "BEEF",
        listOf(HistoryWine("Origem Merlot", 2018, "RED", 9, 34.0, "£", 3.9f), HistoryWine("House", null, null, null, null, null, null)))

    @Test fun `round trip newest first with eviction`() {
        val dir = Files.createTempDirectory("h").toFile()
        val store = ScanHistoryStore(File(dir, "history.json"), maxRecords = 2)
        store.add(record("a"))
        store.add(record("b"))
        val evicted = store.add(record("c"))
        assertEquals(listOf("a"), evicted.map { it.id })
        assertEquals(listOf("c", "b"), store.all().map { it.id })
        assertEquals(record("c"), store.all().first())
        store.remove("b")
        assertEquals(listOf("c"), store.all().map { it.id })
        assertTrue(store.clear().isNotEmpty())
        assertTrue(store.all().isEmpty())
        dir.deleteRecursively()
    }
}
