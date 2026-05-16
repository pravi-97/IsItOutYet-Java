package com.p2.isitout.controller;

import com.p2.isitout.client.tmdb.dto.TmdbSearchResponse;
import com.p2.isitout.client.tmdb.dto.TmdbTvShowDetailsDto;
import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.service.ShowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shows")
public class ShowSearchController {

    private final ShowService showService;

    public ShowSearchController(ShowService showService){
        this.showService = showService;
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseDTO<TmdbSearchResponse>> searchTvShows(@RequestParam String name, @RequestParam int page, @RequestParam(defaultValue = "true") boolean nsfw){
        return createResponse(showService.searchTvShows(name, page, nsfw), "Successfully fetched search results", HttpStatus.OK);
    }

    @GetMapping("/trending")
    public ResponseEntity<ResponseDTO<TmdbSearchResponse>> trendingTvShows(@RequestParam int page){
        return createResponse(showService.getTrendingTvShows(page), "Successfully fetched trending shows", HttpStatus.OK);
    }

    @GetMapping("releasing-today")
    public ResponseEntity<ResponseDTO<TmdbSearchResponse>> tvShowsReleasingToday(@RequestParam int page){
        return createResponse(showService.tvShowsReleasingToday(page), "Successfully fetched shows releasing today", HttpStatus.OK);
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ResponseDTO<TmdbTvShowDetailsDto>> getShowById(@PathVariable String showId){
        return createResponse(showService.getShowById(showId), "Successfully fetched show by ID", HttpStatus.OK);
    }

    private <T> ResponseEntity<ResponseDTO<T>> createResponse( T data, String message, HttpStatus status) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setData(data);
        response.setMessage(message);
        response.setStatusCode(status.value());
        return new ResponseEntity<>(response, status);
    }
}