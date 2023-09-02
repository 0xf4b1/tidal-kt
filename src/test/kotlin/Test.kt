/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

import com.tiefensuche.tidal.api.Artist
import com.tiefensuche.tidal.api.Endpoints
import com.tiefensuche.tidal.api.TidalApi
import com.tiefensuche.tidal.api.Track
import kotlin.test.*
import kotlin.test.Test

class Test {

    private val api = TidalApi(TidalApi.Session("foo", null))

    private fun printArtists(artists: List<Artist>) {
        artists.forEach { println("id: ${it.id}, name: ${it.name}, artwork: ${it.artwork}, url: ${it.url}") }
    }

    private fun printTracks(tracks: List<Track>) {
        tracks.forEach { println("id: ${it.id}, artist: ${it.artist}, title: ${it.title}, duration: ${it.duration}, artwork: ${it.artwork}, url: ${it.url}, liked: ${it.liked}") }
    }

    @BeforeTest
    fun setAuth() {
        api.session.setAuth(1337, "US", "foo", "bar")
    }

    @Test
    fun testGetArtists() {
        val first = api.getArtists(false)
        printArtists(first)

        val second = api.getArtists(false)
        printArtists(second)
        assertNotEquals(first, second)

        val third = api.getArtists(true)
        printArtists(third)
        assertEquals(first, third)

        val tracks = api.getArtist(first[0].id, false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }

    @Test
    fun testGetTracks() {
        val tracks = api.getTracks(false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)

        val stream = api.getStreamUrl(tracks[0].id)
        println("stream: $stream")
    }

    @Test
    fun testGetMix() {
        val tracks = api.getMix(Endpoints.Mixes.MIX_DAILY_DISCOVERY, false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }

    @Test
    fun testQuery() {
        val tracks = api.query("Solee", false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }

    @Test
    fun testLike() {
        val beforeTracks = api.getTracks(true)
        assert(beforeTracks.isNotEmpty())
        val beforeFirst = beforeTracks[0]
        assert(api.toggleLike(beforeFirst.id.toString()))

        val afterTracks = api.getTracks(true)
        assert(afterTracks.isNotEmpty())
        val afterFirst = afterTracks[0]
        assert(beforeFirst.id != afterFirst.id)
        assert(api.toggleLike(beforeFirst.id.toString()))

        val restoreTracks = api.getTracks(true)
        assert(afterTracks.isNotEmpty())
        val restoreFirst = restoreTracks[0]
        assert(beforeFirst.id == restoreFirst.id)
    }

    @Test
    fun testRefreshToken() {
        api.session.accessToken = ""
        assertTrue(api.getAccessToken())
        println("access_token: ${api.session.accessToken}")
    }
}