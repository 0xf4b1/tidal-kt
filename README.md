# tidal-kt
Tidal API client library for Kotlin

## Building

`./gradlew jar`

## Usage

### Session

```kotlin
    val api = TidalApi(TidalApi.Session(<client_id>, ::callback))

    // Callback to let you save session parameters
    private fun callback(session: TidalApi.Session) {
        session.userId
        session.countryCode
        session.accessToken
        session.refreshToken
    }

    // Start new device auth
    val verificationUriComplete = api.auth()

    // Let the user visit verificationUriComplete and finish auth and
    // periodically check if its completed
    if (api.getAccessToken()) {
        println("Auth complete")
    }

    // If you have a stored session, you can restore it without starting new auth
    api.session.setAuth(<user_id>, <country_code>, <access_token>, <refresh_token>)
```

### Tracks

```kotlin
    private fun printTracks(tracks: List<Track>) {
        tracks.forEach { println("id: ${it.id}, artist: ${it.artist}, title: ${it.title}, duration: ${it.duration}, artwork: ${it.artwork}, url: ${it.url}, liked: ${it.liked}") }
    }

    // Get tracks saved by the user
    // You can call this function multiple times to get next results by passing false.
    // If you want to refresh and get results from the beginning, you can reset by passing true.
    val tracks = api.getTracks(false /* reset? */)
    printTracks(tracks)
```

### Artists

```kotlin
    private fun printArtists(artists: List<Artist>) {
        artists.forEach { println("id: ${it.id}, name: ${it.name}, artwork: ${it.artwork}, url: ${it.url}") }
    }

    // Get artists followed by the user
    val artists = api.getArtists(false /* reset? */)
    printArtists(first)

    // Get tracks from the an artist id
    val tracks = api.getArtist(artists[0].id, false /* reset? */)
    printTracks(tracks)
```

### Mixes

```kotlin
    // Get mixes
    val tracks = api.getMix(Endpoints.Mixes.MIX_DAILY_DISCOVERY, false /* reset? */)
    printTracks(tracks)
```

### Search

```kotlin
    // Search for tracks
    val tracks = api.query("Solee", false /* reset? */)
    printTracks(tracks)
```

### Stream

```kotlin
    // Get stream url for track id
    val stream = api.getStreamUrl(tracks[0].id)
    println("stream: $stream")
```