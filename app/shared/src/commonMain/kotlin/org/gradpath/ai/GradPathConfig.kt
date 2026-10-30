package org.gradpath.ai

/**
 * GradPathConfig: Centralized network routing configuration for multiplatform runtime.
 * Pure Cloud Mode by default pointing to the Render production gateway.
 */
object GradPathConfig {
    var baseUrl: String = "https://gradpath-backend.aethersystems.com"

    var host: String
        get() = try {
            val uri = baseUrl.removePrefix("https://").removePrefix("http://")
            uri.substringBefore(':').substringBefore('/')
        } catch (_: Exception) { "127.0.0.1" }
        set(value) {
            baseUrl = if (value.startsWith("http://") || value.startsWith("https://")) {
                value
            } else {
                "http://$value:8080"
            }
        }

    val wsUrl: String
        get() = if (baseUrl.startsWith("https://")) {
            baseUrl.replaceFirst("https://", "wss://")
        } else {
            baseUrl.replaceFirst("http://", "ws://")
        }

    fun endpoint(path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$cleanBase$cleanPath"
    }

    fun wsEndpoint(path: String): String {
        val cleanWs = wsUrl.trimEnd('/')
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$cleanWs$cleanPath"
    }
}
