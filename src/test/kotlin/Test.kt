/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

import com.tiefensuche.tidal.api.*
import kotlin.test.*
import kotlin.test.Test

class Test {

    private val api = TidalApi(TidalApi.Session("foo", null))

    private fun printArtists(artists: List<Artist>) {
        artists.forEach { println("id: ${it.id}, name: ${it.name}, artwork: ${it.artwork}, url: ${it.url}") }
    }

    private fun printAlbums(albums: List<Album>) {
        albums.forEach { println("id: ${it.id}, name: ${it.name}, artist: ${it.artist}, artwork: ${it.artwork}, url: ${it.url}") }
    }

    private fun printTracks(tracks: List<Track>) {
        tracks.forEach { println("id: ${it.id}, artist: ${it.artist}, title: ${it.title}, duration: ${it.duration}, artwork: ${it.artwork}, url: ${it.url}, liked: ${it.liked}") }
    }

    private fun printPlaylists(playlists: List<Playlist>) {
        playlists.forEach { println("uuid: ${it.uuid}, title: ${it.title}, duration: ${it.duration}, artwork: ${it.artwork}") }
    }

    @BeforeTest
    fun setAuth() {
        api.session.setAuth(1337, "US", "foo", "bar")
    }

    @Test
    fun testGetArtists() {
        val first = api.getArtists(false)
        printArtists(first)
        assert(first.isNotEmpty())

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
    fun testGetAlbums() {
        val first = api.getAlbums(false)
        printAlbums(first)
        assert(first.isNotEmpty())

        val second = api.getAlbums(false)
        printAlbums(second)
        assertNotEquals(first, second)

        val third = api.getAlbums(true)
        printAlbums(third)
        assertEquals(first, third)

        val tracks = api.getAlbum(first[0].id, false)
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
    fun testGetMixes() {
        val playlists = api.getMixes()
        assert(playlists.isNotEmpty())
        printPlaylists(playlists)
        playlists.forEach { printTracks(api.getMix(it.uuid, false)) }
    }

    @Test
    fun testGetPlaylists() {
        val playlists = api.getPlaylists(false)
        printPlaylists(playlists)
        val playlist = api.getPlaylist(playlists[0].uuid, false)
        printTracks(playlist)
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

    @Test
    fun testPlaylists() {
        val playlist = api.createPlaylist("test", "test")
        assertNotNull(playlist)
        printPlaylists(listOf(playlist))
        println("ETag: ${playlist.etag}")
        val tracks = api.query("Solee", false)
        assertTrue(api.playlistAdd(playlist.uuid, playlist.etag!!, tracks.map { track -> track.id }))
//        assertTrue(api.playlistDelete(playlist.uuid, tracks[0].id))
        assertTrue(api.deletePlaylist(playlist.uuid))
    }
}