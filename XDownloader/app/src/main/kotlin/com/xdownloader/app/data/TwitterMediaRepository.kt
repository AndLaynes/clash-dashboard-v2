package com.xdownloader.app.data

import android.util.Log
import com.xdownloader.app.domain.model.Cookie
import com.xdownloader.app.domain.model.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

private const val TAG = "XDL"
private const val SYNDICATION_BASE = "https://cdn.syndication.twimg.com/tweet-result"
private const val BEARER = "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I7wlcjwHDEs%3D4puVIpHjIsasttoGYggrEVvKLZlSdhtmlChUYA9CS0jWhXCbE8"
private const val GRAPHQL_QUERY_ID = "BoHLKeBvibdYDiJON1oqTg"

@Singleton
class TwitterMediaRepository @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun resolveMedia(tweetId: String, cookie: Cookie? = null): MediaInfo =
        withContext(Dispatchers.IO) {
            tryFetchSyndication(tweetId)
                ?: cookie?.let { tryFetchAuthenticated(tweetId, it) }
                ?: error("No media found for tweet $tweetId")
        }

    private fun generateToken(id: Long): String {
        val raw = ((id / 1e15) * Math.PI).roundToLong().toString(36)
        val index = (id % 10).toInt()
        val safeIndex = index.coerceIn(0, raw.length)
        return raw.substring(0, safeIndex) + (index + 3).toString(36) + raw.substring(safeIndex)
    }

    private fun tryFetchSyndication(tweetId: String): MediaInfo? {
        val id = tweetId.toLongOrNull() ?: return null
        val token = generateToken(id)
        val url = "$SYNDICATION_BASE?id=$tweetId&lang=pt&token=$token"
        val request = Request.Builder().url(url).get().build()
        return runCatching {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Syndication returned ${response.code} for $tweetId")
                return null
            }
            val body = response.body?.string() ?: return null
            parseVariants(tweetId, JSONObject(body))
        }.getOrElse { e ->
            Log.e(TAG, "Syndication error: ${e.message}")
            null
        }
    }

    private fun tryFetchAuthenticated(tweetId: String, cookie: Cookie): MediaInfo? {
        val variables = """{"focalTweetId":"$tweetId","count":1,"includePromotedContent":false}"""
        val features = """{"rweb_lists_timeline_redesign_enabled":true}"""
        val url = "https://api.twitter.com/graphql/$GRAPHQL_QUERY_ID/TweetDetail" +
            "?variables=${variables.urlEncode()}&features=${features.urlEncode()}"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $BEARER")
            .addHeader("x-csrf-token", cookie.ct0)
            .addHeader("Cookie", "auth_token=${cookie.authToken}; ct0=${cookie.ct0}")
            .addHeader("User-Agent", "Mozilla/5.0")
            .get()
            .build()
        return runCatching {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Auth API returned ${response.code} for $tweetId")
                return null
            }
            val body = response.body?.string() ?: return null
            extractMediaFromGraphql(tweetId, JSONObject(body))
        }.getOrElse { e ->
            Log.e(TAG, "Auth API error: ${e.message}")
            null
        }
    }

    private fun parseVariants(tweetId: String, root: JSONObject): MediaInfo? {
        val mediaDetails = root.optJSONArray("mediaDetails") ?: return null
        var bestUrl: String? = null
        var bestBitrate = -1
        for (i in 0 until mediaDetails.length()) {
            val media = mediaDetails.getJSONObject(i)
            val videoInfo = media.optJSONObject("video_info") ?: continue
            val variants = videoInfo.optJSONArray("variants") ?: continue
            for (j in 0 until variants.length()) {
                val v = variants.getJSONObject(j)
                if (v.optString("content_type") != "video/mp4") continue
                val bitrate = v.optInt("bitrate", 0)
                if (bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = v.optString("url")
                }
            }
        }
        return bestUrl?.let { MediaInfo(tweetId = tweetId, videoUrl = it, bitrate = bestBitrate) }
    }

    private fun extractMediaFromGraphql(tweetId: String, root: JSONObject): MediaInfo? {
        return runCatching {
            val instructions = root
                .getJSONObject("data")
                .getJSONObject("threaded_conversation_with_injections_v2")
                .getJSONArray("instructions")
            for (i in 0 until instructions.length()) {
                val entries = instructions.getJSONObject(i).optJSONArray("entries") ?: continue
                for (j in 0 until entries.length()) {
                    val tweet = entries.getJSONObject(j)
                        .optJSONObject("content")
                        ?.optJSONObject("itemContent")
                        ?.optJSONObject("tweet_results")
                        ?.optJSONObject("result")
                        ?.optJSONObject("legacy") ?: continue
                    val extended = tweet.optJSONObject("extended_entities") ?: continue
                    val media = extended.optJSONArray("media") ?: continue
                    val synRoot = JSONObject().put("mediaDetails", media)
                    val result = parseVariants(tweetId, synRoot)
                    if (result != null) return@runCatching result
                }
            }
            null
        }.getOrElse { e ->
            Log.e(TAG, "GraphQL parse error: ${e.message}")
            null
        }
    }

    private fun String.urlEncode(): String =
        java.net.URLEncoder.encode(this, "UTF-8")
}
