package io.github.deveshb1.losslessradio

object NowPlaying {
    /**
     * Normalises stream metadata into (artist, title). Icecast/Shoutcast usually send one
     * "Artist - Title" string; Ogg streams may send proper fields. Blank values become null.
     */
    fun split(artist: String?, title: String?): Pair<String?, String?> {
        val a = artist?.trim()?.ifBlank { null }
        val t = title?.trim()?.ifBlank { null } ?: return a to null
        if (a != null) return a to t
        val dash = t.indexOf(" - ")
        return if (dash > 0) t.substring(0, dash).trim() to t.substring(dash + 3).trim() else null to t
    }
}
