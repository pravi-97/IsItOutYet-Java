package com.p2.isitout.service;

import com.p2.isitout.client.tmdb.TmdbClient;
import com.p2.isitout.client.tmdb.dto.TmdbSearchResponse;
import com.p2.isitout.client.tmdb.dto.TmdbTvShowDetailsDto;
import com.p2.isitout.dto.ResponseDTO;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ShowService {

    private final TmdbClient tmdbClient;

    public TmdbSearchResponse searchTvShows(String name, int page, boolean nsfw) {
        return tmdbClient.searchTvShows(name, page, nsfw);
    }

    public TmdbSearchResponse getTrendingTvShows(int page) {
        return tmdbClient.trendingTvShows(page);
    }

    public TmdbTvShowDetailsDto getShowById(String showId) {
        return tmdbClient.getTvShowById(showId);
    }

    public TmdbSearchResponse tvShowsReleasingToday(int page) {
        return tmdbClient.tvShowsReleasingToday(page);
    }
}
