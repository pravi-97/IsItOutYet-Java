package com.p2.isitout.controller;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.service.ShowService;
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
    public ResponseEntity<ResponseDTO> searchTvShows(@RequestParam String name, @RequestParam int page){
        return showService.searchTvShows(name, page);
    }

    @GetMapping("/trending")
    public ResponseEntity<ResponseDTO> trendingTvShows(@RequestParam int page){
        return showService.getTrendingTvShows(page);
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ResponseDTO> getShowById(@PathVariable String showId){
        return showService.getShowById(showId);
    }
}