package com.example

import com.example.model.M3uPlaylistParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistParserTest {

    @Test
    fun parseBundledIndiaPlaylist_extractsAll808ValidChannels() {
        val rawFile = File("src/main/res/raw/india_iptv_playlist.m3u")
            .takeIf { it.exists() }
            ?: File("app/src/main/res/raw/india_iptv_playlist.m3u")
        assertTrue("Bundled M3U file should exist", rawFile.exists())

        val channels = M3uPlaylistParser.parse(rawFile.readText(), defaultCountry = "India")
        assertTrue("Expected ~808 unique channels, got ${channels.size}", channels.size >= 800)

        channels.forEach { ch ->
            assertTrue("Channel name must not contain header artifact: ${ch.name}", !ch.name.contains("Gecko"))
            assertTrue("Channel name must be 1..80 chars: ${ch.name}", ch.name.length in 1..80)
            assertTrue("Category must be 1..40 chars: ${ch.category}", ch.category.length in 1..40)
            assertTrue("Language must be 1..40 chars: ${ch.language}", ch.language.length in 1..40)
            assertTrue("Country must be 1..50 chars: ${ch.country}", ch.country.length in 1..50)
            assertTrue("Logo must be <= 500 chars", ch.logo.length <= 500)
            assertTrue(
                "Primary URL must be valid http(s) <= 1000 chars: ${ch.primaryUrl}",
                (ch.primaryUrl.startsWith("http://") || ch.primaryUrl.startsWith("https://")) &&
                    ch.primaryUrl.length in 8..1000
            )
            assertTrue(
                "Deterministic ID must satisfy firestore.rules: ${ch.id}",
                ch.id.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))
            )
        }

        val abpAnanda = channels.firstOrNull { it.name.contains("ABP Ananda", ignoreCase = true) }
        assertEquals("Bengali", abpAnanda?.language)
        assertEquals("News", abpAnanda?.category)
        assertEquals("India", abpAnanda?.country)
    }
}
