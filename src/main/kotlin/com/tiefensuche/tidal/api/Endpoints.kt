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
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}pages/data/25b47120-6a2f-4dbb-8a38-daa415367d22?artistId=%s")
    val MIX =
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}pages/mix?mixId=%s")
    val HOME =
        Requests.Endpoint("${TIDAL_LISTEN_BASE_URL}pages/home", Requests.Method.GET)
    val STATIC =
        Requests.Endpoint("https://listen.tidal.com/v2/home/feed/static?platform=WEB", Requests.Method.GET)
    val PLAYLISTS =
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}users/%s/playlists?order=DATE_UPDATED&orderDirection=DESC")
    val PLAYLIST =
        Requests.CollectionEndpoint("${TIDAL_LISTEN_BASE_URL}playlists/%s/items")

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