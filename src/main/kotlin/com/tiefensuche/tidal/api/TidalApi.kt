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
        return parseTracksFromJSONArray(Requests.CollectionRequest(this, Endpoints.TRACKS, reset, session.userId).execute())
    }

    fun getArtists(reset: Boolean): List<Artist> {
        return parseArtistsFromJSONArray(Requests.CollectionRequest(this, Endpoints.ARTISTS, reset, session.userId).execute())
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
            TIDAL_RESOURCES_URL.format(json.getString("picture").replace("-", "/")),
            json.getString("url")
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
            "trackIds=$trackId&onArtifactNotFound=FAIL", mapOf("Authorization" to "Bearer ${session.accessToken}")
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