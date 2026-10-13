package io.github.deveshb1.losslessradio

import java.net.HttpURLConnection
import java.net.URL

/** Turns a .pls / .m3u link into the stream URL it points at. HLS (.m3u8) is left alone. */
object Playlists {

    fun isPlaylist(url: String): Boolean {
        if (Station.isHls(url)) return false
        val path = url.substringBefore('?').lowercase()
        return path.endsWith(".pls") || path.endsWith(".m3u")
    }

    /** Blocking; call off the main thread. Returns [url] unchanged if it isn't a playlist. */
    fun resolve(url: String): String {
        if (!isPlaylist(url)) return url
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", RadioBrowser.USER_AGENT)
        val body = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        // Some HLS streams use a plain .m3u name; those play as-is.
        if ("#EXT-X-" in body) return url
        return firstStream(body) ?: error("No stream found in playlist")
    }

    internal fun firstStream(body: String): String? = body.lineSequence()
        .map { it.trim() }
        .map { line -> if (line.startsWith("File", ignoreCase = true) && '=' in line) line.substringAfter('=').trim() else line }
        .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
}
