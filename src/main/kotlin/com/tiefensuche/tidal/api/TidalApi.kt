/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.tidal.api

import com.tiefensuche.tidal.api.Endpoints.TIDAL_RESOURCES_URL
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONParserConfiguration
import java.net.URLEncoder
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class TidalApi(val session: Session) {

    class Session(val clientId: String, var callback: ((session: Session) -> Unit)?) {
        var userId: Long? = null
        var countryCode: String = "US"
        var locale: String = "en_US"
        var deviceType = "BROWSER"
        var limit: Int = 50
        var quality = Quality.HIGH
        var deviceCode: String? = null
        var accessToken: String? = null
        var refreshToken: String? = null
        val offsets: HashMap<String, Int> = HashMap()
        var likesTrackIds: MutableSet<Long> = HashSet()

        fun setAuth(userId: Long, countryCode: String, accessToken: String, refreshToken: String) {
            this.userId = userId
            this.countryCode =  countryCode
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }
    }

    enum class Quality {
        LOW,
        HIGH,
        LOSSLESS
    }

    fun auth(): String {
        val res = JSONObject(
            WebRequests.post(
                Endpoints.OAUTH2_DEVICE_AUTH,
                "client_id=${session.clientId}&scope=r_usr+w_usr+w_sub"
            ).value
        )
        session.deviceCode = res.getString("deviceCode")
        return res.getString("verificationUriComplete")
    }

    fun getAccessToken(): Boolean {
        if (session.refreshToken == null && session.deviceCode == null)
            throw NotAuthenticatedException("Can not get access_token without refresh_token or device_code")
        try {
            val res = JSONObject(
                WebRequests.post(
                    Endpoints.OAUTH2_TOKEN,
                    "client_id=${session.clientId}" + (if (session.refreshToken != null) "&refresh_token=${session.refreshToken}" +
                            "&grant_type=refresh_token" else "&device_code=${session.deviceCode}" +
                            "&grant_type=urn:ietf:params:oauth:grant-type:device_code") +
                            "&scope=r_usr+w_usr+w_sub"
                ).value
            )
            if (res.has("access_token")) {
                session.userId = res.getJSONObject("user").getLong("userId")
                session.countryCode = res.getJSONObject("user").getString("countryCode")
                session.accessToken = res.getString("access_token")
                if (res.has("refresh_token"))
                    session.refreshToken = res.getString("refresh_token")
                session.callback?.let { it(session) }
                return true
            }
        } catch (_: WebRequests.HttpException) {
        }
        return false
    }

    fun getTracks(reset: Boolean): List<Track> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.TRACKS, reset, session.userId).execute(), ::buildTrackFromJSON)
    }

    fun getArtists(reset: Boolean): List<Artist> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.ARTISTS, reset, session.userId).execute(), ::buildArtistFromJSON)
    }

    fun getArtist(artist: Long, reset: Boolean): List<Track> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.ARTIST_TRACKS, reset, artist).execute(), ::buildTrackFromJSON)
    }

    fun getAlbums(reset: Boolean): List<Album> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.ALBUMS, reset, session.userId).execute(),::buildAlbumFromJSON)
    }

    fun getAlbum(album: Long, reset: Boolean): List<Track> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.ALBUM_TRACKS, reset, album).execute(), ::buildTrackFromJSON)
    }

    fun getMixes(): List<Playlist> {
        val response = Requests.ActionRequest(this, Endpoints.STATIC).execute().value
        val json = JSONObject(response, JSONParserConfiguration().withOverwriteDuplicateKey(true)).getJSONArray("items")
        for (i in 0 until json.length()) {
            if (json.getJSONObject(i).getString("moduleId") == "DAILY_MIXES") {
                return parseFromJSONArray(json.getJSONObject(i).getJSONArray("items"), ::buildMixFromJSON)
            }
        }
        return emptyList()
    }

    fun getMix(uuid: String, reset: Boolean): List<Track> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.MIX, reset, uuid).execute(), ::buildTrackFromJSON)
    }

    fun getPlaylists(reset: Boolean): List<Playlist> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.PLAYLISTS, reset, session.userId).execute(), ::buildPlaylistFromJSON)
    }

    fun getPlaylist(uuid: String, reset: Boolean): List<Track> {
        return parseFromJSONArray(Requests.CollectionRequest(this, Endpoints.PLAYLIST, reset, uuid).execute(), ::buildTrackFromJSON)
    }

    fun query(query: String, reset: Boolean): List<Track> {
        val tracks =
            Requests.CollectionRequest(this, Endpoints.QUERY, reset, URLEncoder.encode(query, "UTF-8")).execute()
        return parseFromJSONArray(tracks, ::buildTrackFromJSON)
    }

    private fun <T> parseFromJSONArray(json: JSONArray, func: (json: JSONObject) -> T): List<T> {
        val result = mutableListOf<T>()
        for (j in 0 until json.length()) {
            try {
                var track = json.getJSONObject(j)
                if (track.has("item")) {
                    track = track.getJSONObject("item")
                }
                result.add(func(track))
            } catch (e: JSONException) {
                // skip item
            }
        }
        return result
    }

    private fun buildTrackFromJSON(json: JSONObject): Track {
        if (session.likesTrackIds.isEmpty())
            getLikes()

        return Track(
            json.getLong("id"),
            json.getJSONArray("artists").getJSONObject(0).getString("name"),
            json.getString("title"),
            json.getLong("duration") * 1000,
            TIDAL_RESOURCES_URL.format(json.getJSONObject("album").getString("cover").replace("-", "/")),
            json.getString("url"),
            session.likesTrackIds.contains(json.getLong("id"))
        )
    }

    private fun buildArtistFromJSON(json: JSONObject): Artist {
        return Artist(
            json.getLong("id"),
            json.getString("name"),
            if (json.isNull("picture")) "null" else TIDAL_RESOURCES_URL.format(json.getString("picture").replace("-", "/")),
            json.getString("url")
        )
    }

    private fun buildAlbumFromJSON(json: JSONObject): Album {
        return Album(
            json.getLong("id"),
            json.getString("title"),
            json.getJSONObject("artist").getString("name"),
            if (json.isNull("picture")) "null" else TIDAL_RESOURCES_URL.format(json.getString("picture").replace("-", "/")),
            json.getString("url")
        )
    }

    private fun buildPlaylistFromJSON(json: JSONObject): Playlist {
        return Playlist(
            json.getString("uuid"),
            json.getString("title"),
            json.getLong("duration") * 1000,
            TIDAL_RESOURCES_URL.format(json.getString("squareImage").replace("-", "/")))
    }

    private fun buildMixFromJSON(json: JSONObject): Playlist {
        val data = json.getJSONObject("data")
        return Playlist(
            data.getString("id"),
            data.getJSONObject("titleTextInfo").getString("text"),
            0,
            data.getJSONArray("mixImages").getJSONObject(0).getString("url")
        )
    }

    fun toggleLike(trackId: String): Boolean {
        return if (!session.likesTrackIds.contains(trackId.toLong()))
            like(trackId)
        else
            unlike(trackId)
    }

    private fun like(trackId: String): Boolean {
        val result = WebRequests.post(
            Endpoints.LIKE.route.format(session.userId),
            "trackIds=$trackId&onArtifactNotFound=FAIL&countryCode=${session.countryCode}", mapOf("Authorization" to "Bearer ${session.accessToken}")
        )
        if (result.status == 200) {
            session.likesTrackIds.add(trackId.toLong())
            return true
        }
        return false
    }

    private fun unlike(trackId: String): Boolean {
        val result = Requests.ActionRequest(this, Endpoints.UNLIKE, session.userId, trackId).execute()
        if (result.status == 200) {
            session.likesTrackIds.remove(trackId.toLong())
            return true
        }
        return false
    }

    private fun getLikes() {
        val result = Requests.ActionRequest(this, Endpoints.LIKES, session.userId).execute()
        val tracks = JSONObject(result.value).getJSONArray("TRACK")
        for (i in 0 until tracks.length()) {
            session.likesTrackIds.add(tracks.getString(i).toLong())
        }
    }

    fun getStreamUrl(id: Long): String {
        val res = Requests.ActionRequest(this, Endpoints.STREAM, id, session.quality.name).execute()
        if (res.status == 200) {
            val json = JSONObject(res.value)
            val manifest = String(Base64.getDecoder().decode(json.getString("manifest")))
            return JSONObject(manifest).getJSONArray("urls").getString(0)
        }
        throw NotStreamableException("Can not get stream url")
    }

    // Exception types
    class NotAuthenticatedException(message: String) : Exception(message)
    class NotStreamableException(message: String) : Exception(message)
}