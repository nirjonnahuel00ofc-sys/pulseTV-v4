package com.example.model

data class ChannelPreset(
    val canonicalName: String,
    val category: String,
    val language: String,
    val country: String,
    val logoUrl: String,
    val aliases: List<String> = emptyList(),
)

data class ChannelAutoFillResult(
    val canonicalName: String,
    val category: String,
    val language: String,
    val country: String,
    val logoUrl: String,
    val matchedPreset: Boolean,
)

/**
 * Built-in TV channel metadata lookup and smart keyword inference engine so
 * admins typing a channel name get instant auto-fill for Category, Language,
 * Country, and Logo URL.
 */
object ChannelAutoFillCatalog {

    private const val LOGO_BASE = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries"

    val PRESETS: List<ChannelPreset> = listOf(
        // Bangladesh Channels
        ChannelPreset("Somoy TV", "News", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/somoy-tv-bd.png", listOf("somoy", "somoy news")),
        ChannelPreset("Jamuna TV", "News", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/jamuna-tv-bd.png", listOf("jamuna")),
        ChannelPreset("Independent TV", "News", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/independent-tv-bd.png", listOf("independent")),
        ChannelPreset("Ekattor TV", "News", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/ekattor-tv-bd.png", listOf("ekattor", "71 tv")),
        ChannelPreset("Channel 24", "News", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/channel-24-bd.png", listOf("ch 24")),
        ChannelPreset("DBC News", "News", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/dbc-news-bd.png", listOf("dbc")),
        ChannelPreset("ATN News", "News", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/atn-news-bd.png"),
        ChannelPreset("ATN Bangla", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/atn-bangla-bd.png"),
        ChannelPreset("Channel i", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/channel-i-bd.png"),
        ChannelPreset("NTV", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/ntv-bd.png", listOf("ntv bangla")),
        ChannelPreset("RTV", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/rtv-bd.png", listOf("rtv bangla")),
        ChannelPreset("Banglavision", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/banglavision-bd.png"),
        ChannelPreset("Maasranga TV", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/maasranga-tv-bd.png", listOf("maasranga")),
        ChannelPreset("GTV", "Sports", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/gtv-bd.png", listOf("gazi tv")),
        ChannelPreset("T Sports", "Sports", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/t-sports-bd.png", listOf("tsports", "t-sports")),
        ChannelPreset("BTV", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/btv-bd.png", listOf("bangladesh television")),
        ChannelPreset("Deepto TV", "Entertainment", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/deepto-tv-bd.png", listOf("deepto")),
        ChannelPreset("Duronto TV", "Kids", "Bengali", "Bangladesh", "$LOGO_BASE/bangladesh/duronto-tv-bd.png", listOf("duronto")),

        // India Channels
        ChannelPreset("Star Sports 1", "Sports", "English", "India", "$LOGO_BASE/india/star-sports-1-in.png", listOf("star sports", "starsports")),
        ChannelPreset("Star Sports 1 Hindi", "Sports", "Hindi", "India", "$LOGO_BASE/india/star-sports-1-hindi-in.png"),
        ChannelPreset("Sony Sports Ten 1", "Sports", "English", "India", "$LOGO_BASE/india/sony-sports-ten-1-in.png", listOf("sony ten 1", "ten 1", "sony ten")),
        ChannelPreset("Sony Sports Ten 2", "Sports", "English", "India", "$LOGO_BASE/india/sony-sports-ten-2-in.png", listOf("sony ten 2", "ten 2")),
        ChannelPreset("Star Jalsha", "Entertainment", "Bengali", "India", "$LOGO_BASE/india/star-jalsha-in.png", listOf("jalsha")),
        ChannelPreset("Zee Bangla", "Entertainment", "Bengali", "India", "$LOGO_BASE/india/zee-bangla-in.png"),
        ChannelPreset("Colors Bangla", "Entertainment", "Bengali", "India", "$LOGO_BASE/india/colors-bangla-in.png"),
        ChannelPreset("Sony Aath", "Entertainment", "Bengali", "India", "$LOGO_BASE/india/sony-aath-in.png"),
        ChannelPreset("Star Plus", "Entertainment", "Hindi", "India", "$LOGO_BASE/india/star-plus-in.png"),
        ChannelPreset("Sony TV", "Entertainment", "Hindi", "India", "$LOGO_BASE/india/sony-entertainment-television-in.png", listOf("sony set", "set india")),
        ChannelPreset("Zee TV", "Entertainment", "Hindi", "India", "$LOGO_BASE/india/zee-tv-in.png"),
        ChannelPreset("Colors TV", "Entertainment", "Hindi", "India", "$LOGO_BASE/india/colors-in.png", listOf("colors")),
        ChannelPreset("Sony Max", "Movies", "Hindi", "India", "$LOGO_BASE/india/sony-max-in.png", listOf("set max")),
        ChannelPreset("Star Gold", "Movies", "Hindi", "India", "$LOGO_BASE/india/star-gold-in.png"),
        ChannelPreset("Zee Cinema", "Movies", "Hindi", "India", "$LOGO_BASE/india/zee-cinema-in.png"),
        ChannelPreset("ABP Ananda", "News", "Bengali", "India", "$LOGO_BASE/india/abp-ananda-in.png"),
        ChannelPreset("Aaj Tak", "News", "Hindi", "India", "$LOGO_BASE/india/aaj-tak-in.png", listOf("aajtak")),

        // Global / UK / US Channels
        ChannelPreset("BBC World News", "News", "English", "United Kingdom", "$LOGO_BASE/united-kingdom/bbc-news-uk.png", listOf("bbc news", "bbc")),
        ChannelPreset("Sky Sports Cricket", "Sports", "English", "United Kingdom", "$LOGO_BASE/united-kingdom/sky-sports-cricket-uk.png", listOf("sky cricket", "sky sports")),
        ChannelPreset("Sky Sports Premier League", "Sports", "English", "United Kingdom", "$LOGO_BASE/united-kingdom/sky-sports-premier-league-uk.png", listOf("sky pl")),
        ChannelPreset("CNN International", "News", "English", "United States", "$LOGO_BASE/united-states/cnn-us.png", listOf("cnn")),
        ChannelPreset("ESPN", "Sports", "English", "United States", "$LOGO_BASE/united-states/espn-us.png"),
        ChannelPreset("HBO", "Movies", "English", "United States", "$LOGO_BASE/united-states/hbo-us.png"),
        ChannelPreset("Cartoon Network", "Kids", "English", "United States", "$LOGO_BASE/united-states/cartoon-network-us.png", listOf("cn")),
        ChannelPreset("Nickelodeon", "Kids", "English", "United States", "$LOGO_BASE/united-states/nickelodeon-us.png", listOf("nick")),
        ChannelPreset("Discovery Channel", "Documentary", "English", "United States", "$LOGO_BASE/united-states/discovery-channel-us.png", listOf("discovery")),
        ChannelPreset("National Geographic", "Documentary", "English", "United States", "$LOGO_BASE/united-states/national-geographic-us.png", listOf("nat geo", "natgeo")),
        ChannelPreset("Al Jazeera English", "News", "English", "Global", "$LOGO_BASE/international/al-jazeera-english-int.png", listOf("al jazeera", "aljazeera")),
        ChannelPreset("beIN Sports", "Sports", "Arabic", "UAE", "$LOGO_BASE/international/bein-sports-1-int.png", listOf("bein")),
        ChannelPreset("MTV", "Music", "English", "United States", "$LOGO_BASE/united-states/mtv-us.png"),
    )

    fun suggestionsFor(query: String, limit: Int = 5): List<ChannelPreset> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return PRESETS.filter { preset ->
            preset.canonicalName.lowercase().contains(q) ||
                preset.aliases.any { it.contains(q) }
        }.take(limit)
    }

    fun lookupOrInfer(rawName: String): ChannelAutoFillResult {
        val trimmed = rawName.trim()
        val q = trimmed.lowercase()

        // 1. Exact or alias match first
        val exact = PRESETS.firstOrNull { preset ->
            preset.canonicalName.equals(trimmed, ignoreCase = true) ||
                preset.aliases.any { it.equals(q, ignoreCase = true) }
        }
        if (exact != null) {
            return ChannelAutoFillResult(
                canonicalName = exact.canonicalName,
                category = exact.category,
                language = exact.language,
                country = exact.country,
                logoUrl = exact.logoUrl,
                matchedPreset = true,
            )
        }

        // 2. Strong prefix/contains match if user typed >= 3 chars
        if (q.length >= 3) {
            val partial = PRESETS.firstOrNull { preset ->
                preset.canonicalName.lowercase().startsWith(q) ||
                    preset.aliases.any { it.startsWith(q) }
            }
            if (partial != null) {
                return ChannelAutoFillResult(
                    canonicalName = partial.canonicalName,
                    category = partial.category,
                    language = partial.language,
                    country = partial.country,
                    logoUrl = partial.logoUrl,
                    matchedPreset = true,
                )
            }
        }

        // 3. Keyword-based inference for any custom channel name
        val inferredCategory = when {
            containsAny(q, "sport", "cricket", "football", "soccer", "ten ", "espn", "ptv", "willow", "arena", "fc") -> "Sports"
            containsAny(q, "news", "24", "tv18", "tak", "times", "express", "bbc", "cnn", "cnbc", "al jazeera") -> "News"
            containsAny(q, "movie", "cinema", "flix", "hbo", "max", "gold", "pictures", "action") -> "Movies"
            containsAny(q, "kid", "cartoon", "nick", "disney", "pogo", "hungama", "duronto", "baby") -> "Kids"
            containsAny(q, "discovery", "nat geo", "natgeo", "history", "animal", "science", "doc") -> "Documentary"
            containsAny(q, "music", "mtv", "9xm", "vh1", "gaan", "hits", "radio") -> "Music"
            else -> "Entertainment"
        }

        val inferredCountry = when {
            containsAny(q, "bd", "bangla", "dhaka", "somoy", "jamuna", "ekattor", "maasranga", "deepto", "atn", "boishakhi", "desh", "nagorik", "bijoy") -> "Bangladesh"
            containsAny(q, "india", "hindi", "star ", "zee", "sony", "colors", "aaj", "ndtv", "sun tv", "jalsha", "ananda") -> "India"
            containsAny(q, "pak", "geo", "ary", "hum ", "samaa", "urdu") -> "Pakistan"
            containsAny(q, "uk", "bbc", "sky ", "itv", "channel 4") -> "United Kingdom"
            containsAny(q, "usa", "cnn", "espn", "fox", "hbo", "abc", "nbc", "cbs") -> "United States"
            containsAny(q, "arab", "bein", "mbc", "dubai", "uae", "saudi") -> "UAE"
            else -> "Global"
        }

        val inferredLanguage = when {
            containsAny(q, "bengali", "bangla", "bd", "somoy", "jamuna", "ekattor", "jalsha", "ananda", "maasranga", "deepto") -> "Bengali"
            containsAny(q, "hindi", "aaj", "zee tv", "zee cinema", "star plus", "colors tv", "sony max", "sab") -> "Hindi"
            containsAny(q, "urdu", "pak", "geo", "ary", "hum") -> "Urdu"
            containsAny(q, "arab", "mbc", "bein", "alarabiya") -> "Arabic"
            containsAny(q, "tamil", "sun tv", "vijay") -> "Tamil"
            inferredCountry == "Bangladesh" -> "Bengali"
            inferredCountry == "United Kingdom" || inferredCountry == "United States" -> "English"
            else -> "English"
        }

        return ChannelAutoFillResult(
            canonicalName = trimmed,
            category = inferredCategory,
            language = inferredLanguage,
            country = inferredCountry,
            logoUrl = "",
            matchedPreset = false,
        )
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }
}
