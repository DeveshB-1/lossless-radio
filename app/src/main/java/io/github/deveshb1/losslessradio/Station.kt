package io.github.deveshb1.losslessradio

import java.util.UUID
import org.json.JSONObject

data class Station(
    val id: String,
    val name: String,
    val url: String,
    val favicon: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val countryCode: String = "",
    val tags: String = "",
    val homepage: String = "",
    val lossless: Boolean = false,
    /** Added by hand rather than picked from the directory. */
    val custom: Boolean = false,
) {
    /** Short format label for lists, e.g. "FLAC", "HLS FLAC", "AAC 320k". */
    val formatLabel: String
        get() = when {
            lossless && isHls(url) -> "HLS FLAC"
            lossless -> "FLAC"
            codec.isBlank() || codec.equals("UNKNOWN", true) -> if (bitrate > 0) "${bitrate}k" else "Stream"
            bitrate > 0 -> "${codec.uppercase()} ${bitrate}k"
            else -> codec.uppercase()
        }

    val flag: String
        get() = countryCode.takeIf { it.length == 2 && it.all(Char::isLetter) }
            ?.uppercase()
            ?.map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }
            ?.joinToString("")
            .orEmpty()

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("url", url).put("favicon", favicon)
        .put("codec", codec).put("bitrate", bitrate).put("countryCode", countryCode)
        .put("tags", tags).put("homepage", homepage).put("lossless", lossless).put("custom", custom)

    companion object {
        fun fromJson(o: JSONObject) = Station(
            id = o.getString("id"),
            name = o.getString("name"),
            url = o.getString("url"),
            favicon = o.optString("favicon"),
            codec = o.optString("codec"),
            bitrate = o.optInt("bitrate"),
            countryCode = o.optString("countryCode"),
            tags = o.optString("tags"),
            homepage = o.optString("homepage"),
            lossless = o.optBoolean("lossless"),
            custom = o.optBoolean("custom"),
        )

        /** A Radio Browser station object. */
        fun fromDirectory(o: JSONObject): Station {
            val name = o.optString("name").trim()
            val url = o.optString("url_resolved").ifBlank { o.optString("url") }
            val codec = o.optString("codec")
            val bitrate = o.optInt("bitrate")
            val tags = o.optString("tags")
            return Station(
                id = o.getString("stationuuid"),
                name = name,
                url = url,
                favicon = o.optString("favicon"),
                codec = codec,
                bitrate = bitrate,
                countryCode = o.optString("countrycode"),
                tags = tags,
                homepage = o.optString("homepage"),
                lossless = looksLossless(codec, bitrate, "$name $url $tags"),
            )
        }

        fun custom(name: String, url: String) = Station(
            id = "custom-" + UUID.randomUUID(),
            name = name.ifBlank { url.substringAfter("://").substringBefore('/') },
            url = url,
            lossless = looksLossless("", 0, "$name $url"),
            custom = true,
        )

        private val LOSSY_CODECS = setOf("MP3", "AAC", "AAC+", "HE-AAC", "WMA", "OPUS", "MP2", "AC3")
        private val CONTAINER_CODECS = setOf("OGG", "MP4", "", "UNKNOWN")

        /**
         * The directory files most Ogg-FLAC streams under codec "OGG", so we lean on the name/URL.
         * A lossy codec always wins, even when the station's name says "FLAC".
         */
        fun looksLossless(codec: String, bitrate: Int, text: String): Boolean {
            val c = codec.trim().uppercase()
            if (c == "FLAC" || c == "ALAC") return true
            if (c in LOSSY_CODECS) return false
            val t = text.lowercase()
            val mentions = "flac" in t || "lossless" in t || "alac" in t
            return mentions && (c in CONTAINER_CODECS || bitrate >= 1000)
        }

        fun isHls(url: String): Boolean {
            val path = url.substringBefore('?').lowercase()
            return path.endsWith(".m3u8") || "/hls/" in path
        }
    }
}
