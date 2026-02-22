package com.p2.isitout.client.tvmaze;

import com.p2.isitout.client.tvmaze.dto.TvMazeShowDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Slf4j
@Component
public class TvMazeClient {

    private final WebClient webClient;

    private static final long[] BACKOFF_MILLIS = {
            8_000L,
            10_000L,
            12_000L
    };

    public TvMazeClient(@Qualifier("tvMazeWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public TvMazeShowDto getShowByImdbId(String imdbId) {
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/lookup/shows")
                                .queryParam("imdb", imdbId)
                                .build()
                        )
                        .retrieve()
                        .bodyToMono(TvMazeShowDto.class)
                        .block();

            } catch (WebClientResponseException.TooManyRequests ex) {
                if (attempt == BACKOFF_MILLIS.length) {
                    throw ex;
                }
                long delay = resolveRetryAfterOrDefault(ex, BACKOFF_MILLIS[attempt - 1]);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Retry interrupted", ie);
                }
            } catch (WebClientResponseException ex) {
                throw ex;
            }
        }
        return null;
    }

    public <T> T getDataFromTvMazeApi(
            String path,
            Map<String, String> queryParams,
            Class<T> responseType
    ) {
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
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

            } catch (WebClientResponseException.TooManyRequests ex) {
                if (attempt == BACKOFF_MILLIS.length) {
                    throw ex;
                }
                long delay = resolveRetryAfterOrDefault(ex, BACKOFF_MILLIS[attempt - 1]);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Retry interrupted", ie);
                }
        } catch (WebClientResponseException ex) {
                throw ex;
            }
        }
        return null; // unreachable
    }

    private long resolveRetryAfterOrDefault(
            WebClientResponseException.TooManyRequests ex,
            long fallbackMillis
    ) {
        String retryAfter = ex.getHeaders().getFirst("Retry-After");
        if (retryAfter != null) {
            try {
                return Long.parseLong(retryAfter) * 1000;
            } catch (NumberFormatException ignored) {
                log.error("NumberFormatException: retryAfter: {}", retryAfter);
            }
        }
        return fallbackMillis;
    }
}