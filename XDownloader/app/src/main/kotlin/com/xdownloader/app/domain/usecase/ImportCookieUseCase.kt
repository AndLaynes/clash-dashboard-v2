package com.xdownloader.app.domain.usecase

import com.xdownloader.app.data.CookieRepository
import com.xdownloader.app.domain.model.Cookie
import javax.inject.Inject

class ImportCookieUseCase @Inject constructor(
    private val cookieRepository: CookieRepository
) {
    operator fun invoke(netscapeContent: String): Result<Cookie> = runCatching {
        val fields = parseNetscapeCookies(netscapeContent)
        val authToken = fields["auth_token"] ?: error("auth_token not found in cookie file")
        val ct0 = fields["ct0"] ?: error("ct0 not found in cookie file")
        val cookie = Cookie(authToken = authToken, ct0 = ct0)
        cookieRepository.saveCookie(cookie)
        cookie
    }

    private fun parseNetscapeCookies(content: String): Map<String, String> =
        content.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.trim().split("\t")
                if (parts.size >= 7) parts[5] to parts[6] else null
            }
            .toMap()
}
