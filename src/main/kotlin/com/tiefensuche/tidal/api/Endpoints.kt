/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.tidal.api

object Endpoints {
    const val TIDAL_API_BASE_URL = "https://api.tidalhifi.com/v1/"
    const val TIDAL_LISTEN_BASE_URL = "https://listen.tidal.com/v1/"
    const val TIDAL_RESOURCES_URL = "https://resources.tidal.com/images/%s/320x320.jpg"

    const val OAUTH2_BASE_URL = "https://auth.tidal.com/v1/oauth2/"
    const val OAUTH2_DEVICE_AUTH = "${OAUTH2_BASE_URL}device_authorization"
    const val OAUTH2_TOKEN = "${OAUTH2_BASE_URL}token"

    val QUERY = Requests.CollectionEndpoint("${TIDAL_API_BASE_URL}search?query=%s&types=TRACKS")
    val TRACKS =
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}users/%s/favorites/tracks?order=DATE&orderDirection=DESC")
    val ARTISTS =
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}users/%s/favorites/artists?order=DATE&orderDirection=DESC")
    val ARTIST_TRACKS =
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}pages/single-module-page/ae223310-a4c2-4568-a770-ffef70344441/4/889e7c2a-e3be-472b-9801-bf6b40655050/1?artistId=%s")
    val MIX =
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}pages/mix?mixId=%s")

    enum class Mixes(val id: String) {
        MIX_DAILY_DISCOVERY("01662f6c25aa0072685266086e3593"),
        MIX_NEW_ARRIVALS("011c1107cf3b1894c3e426ebb968ef"),
    }

    val LIKES = Requests.Endpoint(
        "${TIDAL_LISTEN_BASE_URL}users/%s/favorites/ids",
        Requests.Method.GET
    )
    val LIKE = Requests.Endpoint(
        "${TIDAL_LISTEN_BASE_URL}users/%s/favorites/tracks",
        Requests.Method.POST
    )
    val UNLIKE = Requests.Endpoint(
        "${TIDAL_LISTEN_BASE_URL}users/%s/favorites/tracks/%s",
        Requests.Method.DELETE
    )
    val STREAM = Requests.Endpoint(
        "${TIDAL_API_BASE_URL}tracks/%s/playbackinfopostpaywall?audioquality=%s&playbackmode=STREAM&assetpresentation=FULL",
        Requests.Method.GET
    )
}