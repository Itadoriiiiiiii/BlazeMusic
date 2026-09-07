package com.blazemusic.app

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Minimal Deezer OAuth/API client.
 * Put your registered Deezer application ID in DeezerConfig.CLIENT_ID and register
 * DeezerConfig.REDIRECT_URI in the Deezer developer console.
 * This syncs playlist metadata only; it never downloads protected audio.
 */
object DeezerConfig {
    const val CLIENT_ID = "6231363283"
    const val REDIRECT_URI = "blazemusic://deezer/callback"
    const val PERMISSIONS = "basic_access,manage_library"
}

data class DeezerPlaylist(val id: Long, val title: String, val trackCount: Int)

object DeezerClient {
    fun authorizationUri(): Uri = Uri.parse("https://connect.deezer.com/oauth/auth.php").buildUpon()
        .appendQueryParameter("app_id", DeezerConfig.CLIENT_ID)
        .appendQueryParameter("redirect_uri", DeezerConfig.REDIRECT_URI)
        .appendQueryParameter("perms", DeezerConfig.PERMISSIONS)
        .build()

    suspend fun exchangeCode(code: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse("https://connect.deezer.com/oauth/access_token.php").buildUpon()
            .appendQueryParameter("app_id", DeezerConfig.CLIENT_ID)
            .appendQueryParameter("code", code)
            .appendQueryParameter("output", "json")
            .build()
        getText(uri.toString()).let { JSONObject(it).getString("access_token") }
    }

    suspend fun playlists(token: String): List<DeezerPlaylist> = withContext(Dispatchers.IO) {
        val json = JSONObject(getText("https://api.deezer.com/user/me/playlists?access_token=${Uri.encode(token)}"))
        val arr = json.optJSONArray("data") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(DeezerPlaylist(o.optLong("id"), o.optString("title"), o.optInt("nb_tracks")))
            }
        }
    }

    private fun getText(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"; c.connectTimeout = 15000; c.readTimeout = 15000
        return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() }
    }
}
