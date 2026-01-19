package com.p2.isitout.client.tvmaze;

import com.p2.isitout.client.tvmaze.dto.TvMazeShowDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
public class TvMazeClient {

    private final WebClient webClient;

    public TvMazeClient(@Qualifier("tvMazeWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public TvMazeShowDto getShowByImdbId(String imdbId) {
        try {
            ResponseEntity<TvMazeShowDto> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/lookup/shows")
                            .queryParam("imdb", imdbId)
                            .build()
                    )
                    .retrieve()
                    .toEntity(TvMazeShowDto.class)
                    .block();

            return response != null ? response.getBody() : null;

        } catch (WebClientResponseException ex) {
            throw ex;
        }
    }

    public <T> T getDataFromTvMazeApi(String path, Map<String, String> queryParams, Class<T> responseType) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        if (queryParams != null && !queryParams.isEmpty()) {
                            queryParams.forEach(uriBuilder::queryParam);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException ex) {
            // log provider, path, params, status
            throw ex;
        }
    }

}
