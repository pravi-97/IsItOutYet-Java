package com.p2.isitout.client.tmdb;

import com.p2.isitout.client.tmdb.dto.TmdbSearchResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;


@Component
public class TmdbClient {

    private final WebClient webClient;

    public TmdbClient(@Qualifier("tmdbWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public TmdbSearchResponse searchTvShows(String query, int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tv")
                        .queryParam("query", query)
                        .queryParam("include_adult", false)
                        .queryParam("language", "en-US")
                        .queryParam("page",page)
                        .build()
                )
                .retrieve()
                .bodyToMono(TmdbSearchResponse.class)
                .block();
    }

    public String getImdbId(String tmdbTvId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{tv_id}/external_ids")
                        .build(tmdbTvId)
                )
                .retrieve()
                // Change JsonNode.class to Map.class
                .bodyToMono(Map.class)
                .map(map -> (String) map.getOrDefault("imdb_id", null))
                .block();
    }
}
