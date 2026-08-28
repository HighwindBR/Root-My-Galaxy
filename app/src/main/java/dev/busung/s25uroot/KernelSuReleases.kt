package dev.busung.s25uroot

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves assets from the latest tiann/KernelSU GitHub release.
 *
 * Used in two places:
 *  1. [KernelSuManagerInstaller] — finds the manager .apk so the user can
 *     install the KernelSU manager app after rooting.
 *  2. [PayloadRepository] — finds the ksud-arm64 binary that is staged into
 *     the kernel during install; this ensures every root attempt uses the
 *     latest upstream ksud instead of a pinned copy.
 */
object KernelSuReleases {

    private const val KSU_API = "https://api.github.com/repos/tiann/KernelSU/releases/latest"
    const val KSU_RELEASES_PAGE = "https://github.com/tiann/KernelSU/releases/latest"

    /**
     * Calls the GitHub Releases API and returns the [RemoteArtifact] for the
     * ksud-arm64 binary in the latest KernelSU release, or null on any error.
     *
     * Asset name priority (first match wins):
     *   1. "ksud-aarch64-linux-android" (exact, no extension)
     *   2. Any asset whose name starts with "ksud" and contains "arm64" or "aarch64"
     *   3. Any asset whose name starts with "ksud" (catch-all)
     */
    fun resolveKsud(): RemoteArtifact? = runCatching {
        val connection = URL(KSU_API).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "RootMyGalaxy")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets") ?: return@runCatching null
            val candidates = mutableListOf<RemoteArtifact>()
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url").ifEmpty { null } ?: continue
                val size = asset.optLong("size", -1L)
                if (size <= 0L) continue
                if (!name.startsWith("ksud")) continue
                // Tier 1: exact canonical name
                if (name == "ksud-aarch64-linux-android") return RemoteArtifact(url, size)
                // Tier 2: contains arm64 or aarch64
                if (name.contains("arm64", ignoreCase = true) ||
                    name.contains("aarch64", ignoreCase = true)
                ) {
                    candidates.add(0, RemoteArtifact(url, size))
                } else {
                    candidates.add(RemoteArtifact(url, size))
                }
            }
            candidates.firstOrNull()
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    /**
     * Returns the browser_download_url of the first .apk in the latest KSU
     * release, or null on any error.
     */
    fun fetchManagerApkUrl(): String? = runCatching {
        val connection = URL(KSU_API).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "RootMyGalaxy")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets") ?: return@runCatching null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk")) {
                    return asset.optString("browser_download_url").ifEmpty { null }
                }
            }
            null
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
