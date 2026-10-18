package com.example.ssmapplication3.core.auth

import java.net.URI
import java.net.URLDecoder

/** Only authorization codes are accepted; never import tokens supplied in a deep link. */
object GoogleOAuthCallback {
    const val REDIRECT_URL = "com.ssmone.mobile://auth/callback"
    const val MAX_AGE_MS = 10 * 60 * 1000L

    fun isCallback(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "com.ssmone.mobile" && uri.rawAuthority == "auth" && uri.rawPath == "/callback"
    }.getOrDefault(false)

    fun authorizationCode(url: String): String {
        require(url.length <= 16384 && isCallback(url)) { "Invalid sign-in callback." }
        val uri = URI(url)
        require(uri.rawFragment == null) { "Token callbacks are not accepted." }
        val parameters = uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.map {
            val pair = it.split('=', limit = 2)
            URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
        }
        require(parameters.map { it.first }.distinct().size == parameters.size) { "Ambiguous sign-in callback." }
        require(parameters.none { it.first in setOf("error", "error_code", "access_token", "refresh_token") }) {
            "Google sign-in was not completed."
        }
        val code = parameters.singleOrNull { it.first == "code" }?.second
        require(!code.isNullOrBlank() && code.length <= 4096 && code.none { it.isWhitespace() || it.isISOControl() }) {
            "Missing sign-in code."
        }
        return code
    }

    fun isFresh(createdAt: Long, now: Long): Boolean = createdAt > 0 && now >= createdAt && now - createdAt <= MAX_AGE_MS
}
