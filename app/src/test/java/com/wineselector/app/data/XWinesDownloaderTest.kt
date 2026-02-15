package com.wineselector.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for XWinesDownloader constants and DatasetSize configuration.
 * Actual download tests require an Android context (instrumentation tests).
 */
class XWinesDownloaderTest {

    @Test
    fun `SLIM URL should be HTTPS`() {
        assertTrue(
            "Slim URL should use HTTPS",
            DatasetSize.SLIM.url.startsWith("https://")
        )
    }

    @Test
    fun `FULL URL should be HTTPS`() {
        assertTrue(
            "Full URL should use HTTPS",
            DatasetSize.FULL.url.startsWith("https://")
        )
    }

    @Test
    fun `SLIM URL should point to zip file`() {
        assertTrue(
            "Slim URL should end with .zip",
            DatasetSize.SLIM.url.endsWith(".zip")
        )
    }

    @Test
    fun `FULL URL should point to zip file`() {
        assertTrue(
            "Full URL should end with .zip",
            DatasetSize.FULL.url.endsWith(".zip")
        )
    }

    @Test
    fun `SLIM requires less space than FULL`() {
        assertTrue(
            "Slim should require less space than Full",
            DatasetSize.SLIM.requiredSpaceMb < DatasetSize.FULL.requiredSpaceMb
        )
    }

    @Test
    fun `FULL requires at least 1GB`() {
        assertTrue(
            "Full dataset should require at least 1GB",
            DatasetSize.FULL.requiredSpaceMb >= 1024
        )
    }

    @Test
    fun `SLIM requires at least 300MB`() {
        assertTrue(
            "Slim dataset should require at least 300MB",
            DatasetSize.SLIM.requiredSpaceMb >= 300
        )
    }

    @Test
    fun `dataset filenames should be CSV`() {
        for (ds in DatasetSize.values()) {
            assertTrue(
                "${ds.name} wines filename should end with .csv",
                ds.winesFilename.endsWith(".csv")
            )
            assertTrue(
                "${ds.name} ratings filename should end with .csv",
                ds.ratingsFilename.endsWith(".csv")
            )
        }
    }
}
