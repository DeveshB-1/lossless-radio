package io.github.deveshb1.losslessradio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationTest {

    @Test
    fun `flac codec is lossless`() {
        assertTrue(Station.looksLossless("FLAC", 0, "Rondo Classic"))
    }

    @Test
    fun `ogg named flac is lossless`() {
        assertTrue(Station.looksLossless("OGG", 1441, "Radio Paradise Main Mix FLAC https://stream.radioparadise.com/flac"))
        assertTrue(Station.looksLossless("OGG", 0, "Pure Lounge Radio FLAC https://x/lounge.ogg"))
    }

    @Test
    fun `lossy codec wins over a FLAC name`() {
        assertFalse(Station.looksLossless("AAC", 320, "1st Greek Jazz FLAC Radio AAC+"))
        assertFalse(Station.looksLossless("MP3", 320, "lossless vibes"))
    }

    @Test
    fun `plain ogg without a lossless hint is not lossless`() {
        assertFalse(Station.looksLossless("OGG", 128, "Some Vorbis Station"))
    }

    @Test
    fun `hls flac is detected and labelled`() {
        val s = Station("x", "SomaFM Groove Salad (HLS FLAC)", "https://hls.somafm.com/hls/groovesalad/FLAC/program.m3u8", codec = "MP4",
            lossless = Station.looksLossless("MP4", 0, "SomaFM Groove Salad (HLS FLAC)"))
        assertTrue(s.lossless)
        assertEquals("HLS FLAC", s.formatLabel)
    }

    @Test
    fun `format labels`() {
        assertEquals("FLAC", Station("a", "n", "u", codec = "OGG", lossless = true).formatLabel)
        assertEquals("AAC 320k", Station("a", "n", "u", codec = "aac", bitrate = 320).formatLabel)
        assertEquals("Stream", Station("a", "n", "u").formatLabel)
    }

    @Test
    fun `country flag`() {
        assertEquals("🇫🇮", Station("a", "n", "u", countryCode = "fi").flag)
        assertEquals("", Station("a", "n", "u", countryCode = "").flag)
    }

    @Test
    fun `json round trip`() {
        val s = Station("id1", "Name", "https://x/y.flac", "https://x/i.png", "OGG", 1441, "US", "rock", "https://x", true, false)
        assertEquals(s, Station.fromJson(s.toJson()))
    }

    @Test
    fun `dedupe keeps first of each url`() {
        val a = Station("1", "Blues A", "http://streams.radiomast.io/radioblues-flac")
        val b = Station("2", "Blues B", "https://streams.radiomast.io/radioblues-flac/")
        val c = Station("3", "Other", "https://other/flac")
        assertEquals(listOf(a, c), RadioBrowser.dedupe(listOf(a, b, c)))
    }

    @Test
    fun `playlist parsing`() {
        assertEquals("http://s/a", Playlists.firstStream("[playlist]\nNumberOfEntries=1\nFile1=http://s/a\nTitle1=x"))
        assertEquals("https://s/b.flac", Playlists.firstStream("#EXTM3U\n#EXTINF:-1,x\nhttps://s/b.flac\n"))
        assertTrue(Playlists.isPlaylist("https://x/listen.pls?x=1"))
        assertFalse(Playlists.isPlaylist("https://x/program.m3u8"))
        assertFalse(Playlists.isPlaylist("https://hls.somafm.com/hls/groovesalad/FLAC/program.m3u"))
    }

    @Test
    fun `now playing splits artist and title`() {
        assertEquals("Grace Jones" to "Love Is the Drug", NowPlaying.split(null, "Grace Jones - Love Is the Drug"))
        assertEquals("A" to "T", NowPlaying.split("A", "T"))
        assertEquals(null to "Just a title", NowPlaying.split(null, "Just a title"))
        assertEquals(null to null, NowPlaying.split(" ", " "))
    }
}
