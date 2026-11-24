package com.example

import com.example.model.ChannelAutoFillCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelAutoFillTest {

    @Test
    fun lookupOrInfer_matchesKnownPresetChannels() {
        val somoy = ChannelAutoFillCatalog.lookupOrInfer("somoy tv")
        assertTrue(somoy.matchedPreset)
        assertEquals("Somoy TV", somoy.canonicalName)
        assertEquals("News", somoy.category)
        assertEquals("Bengali", somoy.language)
        assertEquals("Bangladesh", somoy.country)
        assertTrue(somoy.logoUrl.startsWith("https://"))

        val tsports = ChannelAutoFillCatalog.lookupOrInfer("tsports")
        assertTrue(tsports.matchedPreset)
        assertEquals("T Sports", tsports.canonicalName)
        assertEquals("Sports", tsports.category)
        assertEquals("Bangladesh", tsports.country)
    }

    @Test
    fun lookupOrInfer_infersMetadataForCustomChannelNames() {
        val customBd = ChannelAutoFillCatalog.lookupOrInfer("Bangla Sports HD")
        assertEquals("Sports", customBd.category)
        assertEquals("Bengali", customBd.language)
        assertEquals("Bangladesh", customBd.country)

        val customNews = ChannelAutoFillCatalog.lookupOrInfer("UK World News 24")
        assertEquals("News", customNews.category)
        assertEquals("English", customNews.language)
        assertEquals("United Kingdom", customNews.country)
    }
}
