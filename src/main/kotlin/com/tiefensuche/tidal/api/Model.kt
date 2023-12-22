/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.tidal.api

data class Artist(
    val id: Long,
    val name: String,
    val artwork: String,
    val url: String
)

data class Track(
    val id: Long,
    val artist: String,
    val title: String,
    val duration: Long,
    val artwork: String,
    val url: String,
    val liked: Boolean
)

data class Playlist(
    val uuid: String,
    val title: String,
    val duration: Long,
    val artwork: String
)