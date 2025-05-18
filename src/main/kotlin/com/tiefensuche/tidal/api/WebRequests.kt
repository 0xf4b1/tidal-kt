/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.tidal.api

import java.io.BufferedReader
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object WebRequests {

    class Response(val status: Int, val value: String, val headers: Map<String, List<String>>?)

    fun get(url: String, headers: Map<String, String>? = null): Response {
        return request(createConnection(url, "GET", headers))
    }

    fun post(url: String, data: String, headers: Map<String, String>? = null): Response {
        val con = createConnection(url, "POST", headers)
        con.doOutput = true
        con.outputStream.write(data.toByteArray())
        return request(con)
    }

    fun createConnection(
        url: String,
        method: String = "GET",
        headers: Map<String, String>? = null
    ): HttpsURLConnection {
        val con = URL(url).openConnection() as? HttpsURLConnection ?: throw IOException()
        con.instanceFollowRedirects = false
        con.connectTimeout = 30 * 1000 // 30s
        con.readTimeout = 30 * 1000 // 30s
        con.requestMethod = method
        headers?.forEach { (k, v) -> con.setRequestProperty(k, v) }
        return con
    }

    @Throws(HttpException::class)
    fun request(con: HttpsURLConnection): Response {
        if (con.responseCode < 400) {
            return Response(con.responseCode, con.inputStream.bufferedReader().use(BufferedReader::readText), con.headerFields)
        } else {
            throw HttpException(con.responseCode, con.errorStream.bufferedReader().use(BufferedReader::readText))
        }
    }

    class HttpException(val code: Int, message: String?) : IOException(message)
}
