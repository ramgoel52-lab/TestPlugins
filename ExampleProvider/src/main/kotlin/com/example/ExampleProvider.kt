package com.example

import com.lagradost.cloudstream3.*
import org.json.JSONObject
import java.net.URLEncoder

class ExampleProvider : MainAPI() {

override var mainUrl = "https://api.themoviedb.org/3"
override var name = "JustWatch India"
override var lang = "hi"
override val hasMainPage = true
override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

private val apiKey = "PASTE_YOUR_TMDB_API_KEY"

private val imageBase = "https://image.tmdb.org/t/p/w500"

override val mainPage = mainPageOf(
    "trending/movie/week" to "Latest Movies",
    "movie/popular" to "Movies",
    "tv/popular" to "Web Series",
    "discover/tv?with_origin_country=KR" to "Korean",
    "discover/tv?with_genres=16&with_original_language=ja" to "Anime",
    "movie/top_rated" to "Top IMDb"
)

private fun getData(path: String): JSONObject {
    val separator = if (path.contains("?")) "&" else "?"
    val url = "$mainUrl/$path${separator}api_key=$apiKey&region=IN"
    return JSONObject(app.get(url).text)
}

private fun convert(data: JSONObject, tv: Boolean): List<SearchResponse> {
    val array = data.optJSONArray("results") ?: return emptyList()

    return (0 until array.length()).mapNotNull { i ->
        val item = array.optJSONObject(i) ?: return@mapNotNull null
        val id = item.optInt("id")
        val title = item.optString(if (tv) "name" else "title")
        if (title.isBlank()) return@mapNotNull null

        val poster = item.optString("poster_path")
            .takeIf { it.isNotBlank() }
            ?.let { "$imageBase$it" }

        if (tv) {
            newTvSeriesSearchResponse(
                title,
                "https://www.themoviedb.org/tv/$id",
                TvType.TvSeries
            ) {
                this.posterUrl = poster
            }
        } else {
            newMovieSearchResponse(
                title,
                "https://www.themoviedb.org/movie/$id",
                TvType.Movie
            ) {
                this.posterUrl = poster
            }
        }
    }
}

override suspend fun getMainPage(
    page: Int,
    request: MainPageRequest
): HomePageResponse {
    val tv = request.data.startsWith("tv/") ||
            request.data.startsWith("discover/tv")

    return newHomePageResponse(
        request.name,
        convert(getData(request.data), tv)
    )
}

override suspend fun search(query: String): List<SearchResponse> {
    val q = URLEncoder.encode(query, "UTF-8")

    return convert(getData("search/movie?query=$q"), false) +
            convert(getData("search/tv?query=$q"), true)
}

override suspend fun load(url: String): LoadResponse {
    throw ErrorLoadingException(
        "Details ke liye TMDB integration agle step mein add hogi."
    )
}

}
