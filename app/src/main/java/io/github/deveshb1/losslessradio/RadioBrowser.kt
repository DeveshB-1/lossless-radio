package io.github.deveshb1.losslessradio

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray

/**
 * Client for the community station directory at radio-browser.info.
 * All calls block; run them on Dispatchers.IO.
 */
object RadioBrowser {
    const val USER_AGENT = "LosslessRadio/1.0 (+https://github.com/DeveshB-1/lossless-radio)"
    private const val FALLBACK_SERVER = "de1.api.radio-browser.info"

    @Volatile private var server: String? = null

    private fun server(): String = server ?: runCatching {
        val list = JSONArray(get("https://all.api.radio-browser.info/json/servers"))
        (0 until list.length()).map { list.getJSONObject(it).getString("name") }.distinct().random()
    }.getOrDefault(FALLBACK_SERVER).also { server = it }

    private fun api(path: String): String = "https://${server()}/json/$path"

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun stations(path: String): List<Station> {
        val array = JSONArray(get(api(path)))
        return (0 until array.length()).mapNotNull { runCatching { Station.fromDirectory(array.getJSONObject(it)) }.getOrNull() }
    }

    /**
     * Every lossless station the directory knows about. It has no "lossless" filter, so we
     * gather candidates by name, tag and codec, then keep the ones that really look lossless.
     */
    fun losslessStations(): List<Station> {
        val queries = listOf("name=flac", "name=lossless", "tag=flac", "tag=lossless", "codec=FLAC", "tag=hi-res")
        val all = queries.flatMap { q ->
            runCatching { stations("stations/search?$q&hidebroken=true&limit=500&order=clickcount&reverse=true") }
                .getOrDefault(emptyList())
        }
        if (all.isEmpty()) error("Couldn't reach the station directory")
        return dedupe(all.filter { it.lossless })
    }

    /** Free-text search across all stations, most popular first. */
    fun search(query: String): List<Station> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        return dedupe(stations("stations/search?name=$q&hidebroken=true&limit=150&order=clickcount&reverse=true"))
    }

    /** Tells the directory a station was played, which feeds its popularity ranking. */
    fun registerClick(stationId: String) {
        runCatching { get(api("url/$stationId")) }
    }

    /** The same stream is often listed several times; keep one entry per URL, first (most popular) wins. */
    internal fun dedupe(stations: List<Station>): List<Station> =
        stations.distinctBy { it.url.trim().trimEnd('/').substringAfter("://").lowercase() }
}
