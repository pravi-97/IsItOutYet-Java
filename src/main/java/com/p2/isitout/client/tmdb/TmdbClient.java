package com.p2.isitout.client.tmdb;

import com.p2.isitout.client.tmdb.dto.TmdbSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TmdbClient {

    private final WebClient webClient;

    public TmdbClient(WebClient webClient) {
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

}
