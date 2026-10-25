package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Résumé d'une page Wikipédia en français. */
data class WikiSummary(val title: String, val extract: String, val pageUrl: String)

/**
 * Fiche d'un village depuis l'API REST de Wikipédia (fr.wikipedia.org/api/rest_v1/page/summary).
 * Une page n'est retenue que si elle parle bien d'un lieu belge ou luxembourgeois : beaucoup de noms
 * de villages gaumais existent aussi ailleurs (Ethe, Pin, Termes, Muno…). Sinon, pas de fiche.
 */
class WikipediaClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val SUMMARY_URL = "https://fr.wikipedia.org/api/rest_v1/page/summary/"
        private const val USER_AGENT = "GaumeTrailALG/1.0 (appli personnelle ; https://github.com/dash52000/Gaume-trail-ALG)"
        private val PLACE_HINTS = listOf("belgique", "belge", "luxembourg", "gaume", "wallon", "province", "virton", "arlon")

        /** Titres essayés dans l'ordre : le nom, puis « nom (Belgique) ». */
        fun candidateTitles(village: String): List<String> {
            val clean = village.substringBefore(" (").trim()
            return listOf(clean, "$clean (Belgique)", "$clean (Luxembourg)").distinct()
        }

        fun parseSummary(jsonText: String): WikiSummary? {
            val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
            if (root.optString("type") != "standard") return null
            val extract = root.optString("extract").trim()
            if (extract.isBlank()) return null
            val haystack = (root.optString("description") + " " + extract).lowercase(Locale.FRENCH)
            if (PLACE_HINTS.none { haystack.contains(it) }) return null
            val url = root.optJSONObject("content_urls")?.optJSONObject("mobile")?.optString("page")
                ?.takeIf { it.startsWith("http") }
                ?: root.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page").orEmpty()
            return WikiSummary(
                title = root.optString("title").ifBlank { root.optString("displaytitle") },
                extract = extract,
                pageUrl = url
            )
        }
    }

    suspend fun fetchVillageSummary(village: String): Result<WikiSummary?> = withContext(Dispatchers.IO) {
        runCatching {
            candidateTitles(village).firstNotNullOfOrNull { title ->
                val encoded = URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "_")
                val request = Request.Builder()
                    .url(SUMMARY_URL + encoded)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string()?.let { parseSummary(it) } else null
                }
            }
        }
    }
}
