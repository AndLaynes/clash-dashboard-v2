package com.xdownloader.app.domain.usecase

import android.util.Log
import com.xdownloader.app.data.CookieRepository
import com.xdownloader.app.data.TwitterMediaRepository
import com.xdownloader.app.domain.model.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

private const val TAG = "XDL"

class ResolveMediaUseCase @Inject constructor(
    private val repository: TwitterMediaRepository,
    private val cookieRepository: CookieRepository,
    private val okHttpClient: OkHttpClient
) {
    suspend operator fun invoke(tweetUrl: String): Result<MediaInfo> = runCatching {
        val resolvedUrl = resolveRedirectIfNeeded(tweetUrl)
        val tweetId = extractTweetId(resolvedUrl)
            ?: error("Could not extract tweet ID from: $tweetUrl")
        repository.resolveMedia(tweetId, cookieRepository.loadCookie())
    }

    private fun extractTweetId(url: String): String? {
        val pattern = Regex("""(?:twitter\.com|x\.com)/\w+/status/(\d+)""")
        return pattern.find(url)?.groupValues?.get(1)
    }

    private suspend fun resolveRedirectIfNeeded(url: String): String {
        if (!url.contains("t.co/")) return url
        return withContext(Dispatchers.IO) {
            runCatching {
                val noRedirectClient = okHttpClient.newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()
                val request = Request.Builder().url(url).head().build()
                val response = noRedirectClient.newCall(request).execute()
                response.header("Location") ?: url
            }.getOrElse { e ->
                Log.w(TAG, "Redirect resolution failed: ${e.message}")
                url
            }
        }
    }
}
