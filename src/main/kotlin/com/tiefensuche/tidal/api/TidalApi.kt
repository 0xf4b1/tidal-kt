/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.tidal.api

import com.tiefensuche.tidal.api.Endpoints.TIDAL_RESOURCES_URL
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class TidalApi() {

    var clientId: String? = null
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

    enum class Quality {
        LOW,
        HIGH,
        LOSSLESS
    }

    constructor(clientId: String) : this() {
        this.clientId = clientId
    }

    constructor(userId: Long, accessToken: String, refreshToken: String) : this() {
        this.userId = userId
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun auth(): String {
        val res = JSONObject(
            WebRequests.post(
                Endpoints.OAUTH2_DEVICE_AUTH,
                "client_id=${clientId}&scope=r_usr+w_usr+w_sub"
            ).value
        )
        deviceCode = res.getString("deviceCode")
        return res.getString("verificationUriComplete")
    }

    fun getAccessToken(): Boolean {
        try {
            val res = JSONObject(
                WebRequests.post(
                    Endpoints.OAUTH2_TOKEN, "client_id=${clientId}" +
                            "&device_code=${deviceCode}" +
                            "&grant_type=urn:ietf:params:oauth:grant-type:device_code" +
                            "&scope=r_usr+w_usr+w_sub"
                ).value
            )
            if (res.has("access_token")) {
                userId = res.getJSONObject("user").getLong("userId")
                accessToken = res.getString("access_token")
                refreshToken = res.getString("refresh_token")
                return true
            }
        } catch (_: WebRequests.HttpException) {
        }
        return false
    }

    fun getTracks(reset: Boolean): List<Track> {
        return parseTracksFromJSONArray(Requests.CollectionRequest(this, Endpoints.TRACKS, reset, userId).execute())
    }

    fun getArtists(reset: Boolean): List<Artist> {
        return parseArtistsFromJSONArray(Requests.CollectionRequest(this, Endpoints.ARTISTS, reset, userId).execute())
    }

    fun getArtist(artist: Long, reset: Boolean): List<Track> {
        return parseTracksFromJSONArray(
            Requests.CollectionRequest(this, Endpoints.ARTIST_TRACKS, reset, artist).execute()
        )
    }

    fun getMix(mix: Endpoints.Mixes, reset: Boolean): List<Track> {
        return parseTracksFromJSONArray(Requests.CollectionRequest(this, Endpoints.MIX, reset, mix.id).execute())
    }

    fun query(query: String, reset: Boolean): List<Track> {
        val tracks =
            Requests.CollectionRequest(this, Endpoints.QUERY, reset, URLEncoder.encode(query, "UTF-8")).execute()
        return parseTracksFromJSONArray(tracks)
    }

    private fun parseTracksFromJSONArray(tracks: JSONArray): List<Track> {
        val result = mutableListOf<Track>()
        for (j in 0 until tracks.length()) {
            try {
                var track = tracks.getJSONObject(j)
                if (track.has("item")) {
                    track = track.getJSONObject("item")
                }
                result.add(buildTrackFromJSON(track))
            } catch (e: JSONException) {
                // skip item
            }
        }
        return result
    }

    private fun parseArtistsFromJSONArray(tracks: JSONArray): List<Artist> {
        val result = mutableListOf<Artist>()
        for (j in 0 until tracks.length()) {
            try {
                var track = tracks.getJSONObject(j)
                if (track.has("item")) {
                    track = track.getJSONObject("item")
                }
                result.add(buildArtistFromJSON(track))
            } catch (e: JSONException) {
                // skip item
            }
        }
        return result
    }

    private fun buildTrackFromJSON(json: JSONObject): Track {
        if (likesTrackIds.isEmpty())
            getLikes()

        return Track(
            json.getLong("id"),
            json.getJSONArray("artists").getJSONObject(0).getString("name"),
            json.getString("title"),
            json.getLong("duration") * 1000,
            TIDAL_RESOURCES_URL.format(json.getJSONObject("album").getString("cover").replace("-", "/")),
            json.getString("url"),
            likesTrackIds.contains(json.getLong("id"))
        )
    }

    private fun buildArtistFromJSON(json: JSONObject): Artist {
        return Artist(
            json.getLong("id"),
            json.getString("name"),
            TIDAL_RESOURCES_URL.format(json.getString("picture").replace("-", "/")),
            json.getString("url")
        )
    }

    fun toggleLike(trackId: String): Boolean {
        return if (!likesTrackIds.contains(trackId.toLong()))
            like(trackId)
        else
            unlike(trackId)
    }

    private fun like(trackId: String): Boolean {
        val result = WebRequests.post(
            Endpoints.LIKE.route.format(userId),
            "trackIds=$trackId&onArtifactNotFound=FAIL", mapOf("Authorization" to "Bearer $accessToken")
        )
        if (result.status == 200) {
            likesTrackIds.add(trackId.toLong())
            return true
        }
        return false
    }

    private fun unlike(trackId: String): Boolean {
        val result = Requests.ActionRequest(this, Endpoints.UNLIKE, userId, trackId).execute()
        if (result.status == 200) {
            likesTrackIds.remove(trackId.toLong())
            return true
        }
        return false
    }

    private fun getLikes() {
        val result = Requests.ActionRequest(this, Endpoints.LIKES, userId).execute()
        val tracks = JSONObject(result.value).getJSONArray("TRACK")
        for (i in 0 until tracks.length()) {
            likesTrackIds.add(tracks.getString(i).toLong())
        }
    }

    fun getStreamUrl(id: Long): String {
        val res = Requests.ActionRequest(this, Endpoints.STREAM, id, quality.name).execute()
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