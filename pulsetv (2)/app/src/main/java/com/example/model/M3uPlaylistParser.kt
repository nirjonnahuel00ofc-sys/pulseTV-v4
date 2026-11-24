package com.example.model

import java.security.MessageDigest

object M3uPlaylistParser {

    private val LOGO_REGEX = Regex("""tvg-logo="([^"]*)"""")
    private val GROUP_REGEX = Regex("""group-title="([^"]*)"""")
    private val TVG_ID_REGEX = Regex("""tvg-id="([^"]*)"""")
    private val QUALITY_TAG_REGEX = Regex("""\s*\(\d{3,4}[pi]\)\s*""", RegexOption.IGNORE_CASE)

    /**
     * Parses standard and malformed #EXTM3U playlists (including multi-attribute lines or
     * "channel,url" pairs) into PulseTV [Channel] items ready for Cloud Firestore batch upload.
     */
    fun parse(
        rawContent: String,
        defaultCountry: String = "",
    ): List<Channel> {
        val parsed = ArrayList<Channel>(256)
        val seenUrls = HashSet<String>(512)
        var pendingExtInf: String? = null

        rawContent.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingExtInf = line
                }
                line.startsWith("#") -> {
                    // Skip #EXTM3U, #PLAYLIST, #EXTVLCOPT, etc.
                }
                line.startsWith("http://", ignoreCase = true) ||
                    line.startsWith("https://", ignoreCase = true) -> {
                    if (line.length <= 1000 && seenUrls.add(line)) {
                        val channel = buildChannelFromExtInf(
                            extInfLine = pendingExtInf,
                            streamUrl = line,
                            defaultCountry = defaultCountry,
                        )
                        parsed.add(channel)
                    }
                    pendingExtInf = null
                }
                else -> {
                    pendingExtInf = null
                }
            }
        }
        return parsed
    }

    private fun buildChannelFromExtInf(
        extInfLine: String?,
        streamUrl: String,
        defaultCountry: String,
    ): Channel {
        val rawTitle = extInfLine?.substringAfterLast(',', "")?.trim().orEmpty()
            .ifBlank { deriveNameFromUrl(streamUrl) }
        val cleanName = cleanChannelName(rawTitle).take(80).ifBlank { "Live Channel" }

        val tvgId = extInfLine?.let { TVG_ID_REGEX.findAll(it).lastOrNull()?.groupValues?.getOrNull(1) }?.trim().orEmpty()
        val rawLogo = extInfLine?.let { LOGO_REGEX.findAll(it).lastOrNull()?.groupValues?.getOrNull(1) }?.trim().orEmpty()
        val logo = if ((rawLogo.startsWith("http://") || rawLogo.startsWith("https://")) && rawLogo.length <= 500) {
            rawLogo
        } else {
            ""
        }

        val rawGroup = extInfLine?.let { GROUP_REGEX.findAll(it).lastOrNull()?.groupValues?.getOrNull(1) }?.trim().orEmpty()
        val country = inferCountry(tvgId = tvgId, name = cleanName, defaultCountry = defaultCountry)
        val category = normalizeCategory(rawGroup = rawGroup, name = cleanName)
        val language = inferLanguage(name = cleanName, tvgId = tvgId, country = country)

        return Channel(
            id = stableDocId(streamUrl),
            name = cleanName,
            category = category,
            language = language,
            country = country,
            logo = logo,
            primaryUrl = streamUrl,
            backupUrl = "",
            isActive = true,
        )
    }

    fun stableDocId(streamUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(streamUrl.toByteArray(Charsets.UTF_8))
        val hex = digest.take(12).joinToString("") { "%02x".format(it) }
        return "m3u_$hex"
    }

    private fun cleanChannelName(raw: String): String {
        val noRes = raw.replace(QUALITY_TAG_REGEX, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return noRes.ifBlank { raw.trim() }
    }

    private fun normalizeCategory(rawGroup: String, name: String): String {
        val tokens = rawGroup.split(';', ',', '|', '/')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val lowerTokens = tokens.map { it.lowercase() }
        val mapped = when {
            lowerTokens.any { it.contains("sport") } -> "Sports"
            lowerTokens.any { it.contains("news") } -> "News"
            lowerTokens.any { it.contains("movie") || it.contains("cinema") } -> "Movies"
            lowerTokens.any { it.contains("kid") || it.contains("animation") || it.contains("cartoon") } -> "Kids"
            lowerTokens.any { it.contains("music") } -> "Music"
            lowerTokens.any { it.contains("documentary") || it.contains("science") || it.contains("history") } -> "Documentary"
            lowerTokens.any { it == "entertainment" || it == "general" || it == "comedy" } -> "Entertainment"
            tokens.isNotEmpty() && !lowerTokens.first().equals("undefined") -> tokens.first()
            else -> ChannelAutoFillCatalog.lookupOrInfer(name).category
        }
        return mapped.take(40).ifBlank { "Entertainment" }
    }

    private fun inferCountry(tvgId: String, name: String, defaultCountry: String): String {
        val codePart = tvgId.substringBefore('@', "").substringAfterLast('.', "").lowercase()
        val fromTvg = when (codePart) {
            "bd" -> "Bangladesh"
            "in" -> "India"
            "pk" -> "Pakistan"
            "uk", "gb" -> "United Kingdom"
            "us" -> "United States"
            "ca" -> "Canada"
            "ae" -> "UAE"
            "sa" -> "Saudi Arabia"
            else -> ""
        }
        if (fromTvg.isNotBlank()) return fromTvg
        if (defaultCountry.isNotBlank()) return defaultCountry
        return ChannelAutoFillCatalog.lookupOrInfer(name).country
    }

    private fun inferLanguage(name: String, tvgId: String, country: String): String {
        val q = "${name.lowercase()} ${tvgId.lowercase()}"
        return when {
            containsAny(q, "bengali", "bangla", "aakaash", "aakash", "ananda", "kolkata", "jalsha", "khabor", "barta", "jago") -> "Bengali"
            containsAny(q, "telugu", "andhra", "etv ", "gemini", "ntv", "tv9", "sakshi", "mahaa", "bhakthi", "abn", "v6", "t news", "vanitha") -> "Telugu"
            containsAny(q, "tamil", "puthuyugam", "kalaignar", "makkal", "jaya", "thanthi", "puthiya", "polimer", "captain", "vasanth", "raj ", "murug", "isai", "chithiram", "sri sankara", "velicham") -> "Tamil"
            containsAny(q, "malayalam", "asianet", "mazhavil", "kairali", "flowers", "mathrubhumi", "manorama", "amrita", "kaumudy", "janam", "reporter", "twentyfour", "24 news", "jeevan", "we tv") -> "Malayalam"
            containsAny(q, "kannada", "suvarna", "udaya", "public tv", "kasthuri", "power tv", "chandana", "digvijaya") -> "Kannada"
            containsAny(q, "marathi", "majha", "sahyadri", "lokshahi", "pudhari", "saam", "fakht marathi", "jhakaas") -> "Marathi"
            containsAny(q, "gujarati", "asmita", "girnar", "sandesh", "vtv", "gstv", "mantavya") -> "Gujarati"
            containsAny(q, "punjabi", "ptc", "sanjha", "chardikla", "balle", "tashan", "pitaara", "mh1", "guru") -> "Punjabi"
            containsAny(q, "odia", "oriya", "tarang", "alankar", "argus", "kanak", "otv", "prameya", "kalinga", "nandighosha", "mbc tv", "prarthana") -> "Odia"
            containsAny(q, "urdu") -> "Urdu"
            containsAny(q, "assam", "prag", "dy 365", "pratidin") -> "Assamese"
            containsAny(q, "bhojpuri", "dabangg", "anjan") -> "Hindi"
            containsAny(q, "english", "cnbc tv18", "cnn", "wion", "mirror now", "times now", "ndtv 24x7", "ndtv profit", "india today", "republic tv", "dd india", "bt tv", "alright", "good news today") -> "English"
            country == "India" -> "Hindi"
            else -> ChannelAutoFillCatalog.lookupOrInfer(name).language
        }
    }

    private fun deriveNameFromUrl(url: String): String {
        val segment = url.substringAfterLast('/', "")
            .substringBefore('?')
            .substringBefore('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        return segment.ifBlank { "Live Stream" }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }
}
