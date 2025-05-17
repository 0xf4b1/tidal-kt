/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.tidal.api

import org.json.JSONArray
import org.json.JSONObject

class Requests {

    enum class Method {
        GET, POST, PUT, DELETE
    }

    open class Endpoint(val route: String, val method: Method)

    class CollectionEndpoint(route: String) : Endpoint(route, Method.GET)

    class CollectionRequest(
        api: TidalApi,
        endpoint: CollectionEndpoint,
        private val reset: Boolean,
        vararg args: Any?
    ) : Request<JSONArray>(api, endpoint, *args) {

        override val url = append(super.url, "limit", session.limit.toString())

        override fun execute(): JSONArray {
            var offset = 0
            if (!reset && url in session.offsets) {
                session.offsets[url]?.let {
                    offset = it
                } ?: return JSONArray()
            }

            val response = request(append(url, "offset", offset.toString()))
            session.offsets[url] = offset + session.limit

            var json = JSONObject(response.value)
            if (json.has("tracks")) {
                json = json.getJSONObject("tracks")
            }
            if (json.has("items")) {
                return json.getJSONArray("items")
            }
            if (json.has("rows")) {
                val rows = json.getJSONArray("rows")
                for (i in 0 until rows.length()) {
                    val modules = rows.getJSONObject(i).getJSONArray("modules")
                    for (j in 0 until modules.length()) {
                        val cur = modules.getJSONObject(j)
                        if (cur.getString("type") in listOf("TRACK_LIST", "ALBUM_ITEMS"))
                            return cur.getJSONObject("pagedList").getJSONArray("items")
                    }
                }
            }
            return JSONArray()
        }
    }

    class ActionRequest(session: TidalApi, endpoint: Endpoint, vararg args: Any?) :
        Request<WebRequests.Response>(session, endpoint, *args) {
        override fun execute(): WebRequests.Response {
            return request(url)
        }
    }

    abstract class Request<T>(val api: TidalApi, val endpoint: Endpoint, vararg args: Any?) {

        val session = api.session

        open val url = append(
            append(
                append(endpoint.route.format(*args), "countryCode", session.countryCode),
                "locale",
                session.locale
            ), "deviceType", session.deviceType
        )

        fun request(url: String): WebRequests.Response {
            if (session.accessToken == null) {
                throw TidalApi.NotAuthenticatedException("Not authenticated!")
            }
            try {
                return WebRequests.request(
                    WebRequests.createConnection(
                        url,
                        endpoint.method.name,
                        mapOf(
                            "authorization" to "Bearer ${session.accessToken}",
                            "x-tidal-client-version" to "2025.4.15"
                        )
                    )
                )
            } catch (e: WebRequests.HttpException) {
                if (e.code == 401) {
                    session.accessToken = null
                    session.refreshToken?.let {
                        if (api.getAccessToken())
                            return WebRequests.request(
                                WebRequests.createConnection(
                                    url,
                                    endpoint.method.name,
                                    mapOf(
                                        "authorization" to "Bearer ${session.accessToken}",
                                        "x-tidal-client-version" to "2025.4.15"
                                    )
                                )
                            )
                    }
                }
                throw e
            }
        }

        fun append(url: String, key: String, value: String?): String {
            return url + when {
                url.contains('?') -> '&'
                else -> '?'
            } + "$key=$value"
        }

        abstract fun execute(): T
    }
}