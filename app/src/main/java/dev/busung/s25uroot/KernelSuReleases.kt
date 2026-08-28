package dev.busung.s25uroot

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object KernelSuReleases {

    private const val API_URL =
        "https://api.github.com/repos/tiann/KernelSU/releases/latest"

    /**
     * Fetches the latest tiann/KernelSU release and returns a [RemoteArtifact]
     * pointing to the ksud binary that matches the device ABI.
     * Returns null if the API is unreachable or no matching asset is found,
     * allowing callers to fall back to a static payload-repo pin.
     */
    fun resolveKsud(
        abi: String = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
    ): RemoteArtifact? {
        return try {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Root-My-Galaxy")
                connect()
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val assets = JSONObject(body).getJSONArray("assets")

            // Asset names follow the pattern: ksud-<abi>
            // e.g. ksud-arm64-v8a, ksud-x86_64, ksud-arm64_v8a
            val normalizedAbi = abi.replace("-", "_") // arm64-v8a -> arm64_v8a
            var url: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name.startsWith("ksud") &&
                    (name.contains(abi, ignoreCase = true) ||
                        name.contains(normalizedAbi, ignoreCase = true))
                ) {
                    url = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    break
                }
            }

            // size = -1 signals to downloadArtifact that content-length is unknown
            url?.let { RemoteArtifact(url = it, size = -1L) }
        } catch (_: Exception) {
            null
        }
    }
}
