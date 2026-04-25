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

    public ResponseEntity<ResponseDTO> searchTvShows(String name, int page, boolean nsfw) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            TmdbSearchResponse resp = tmdbClient.searchTvShows(name, page, nsfw);
            responseDto.setData(resp);
            responseDto.setMessage("Success!");
            return buildResponse(responseDto);
        }catch(Exception e){
            e.printStackTrace();
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setMessage(e.getMessage());
            return buildResponse(responseDto);
        }
    }

    private ResponseDTO createDefaultResponseDTO() {
        ResponseDTO responseDto = new ResponseDTO();
        responseDto.setStatusCode(200);
        responseDto.setStatus(true);
        return responseDto;
    }

    private ResponseEntity<ResponseDTO> buildResponse(ResponseDTO responseDto) {
        HttpStatus status = HttpStatus.resolve(responseDto.getStatusCode());

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity
                .status(status)
                .body(responseDto);
    }

    public ResponseEntity<ResponseDTO> getTrendingTvShows(int page) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            TmdbSearchResponse resp = tmdbClient.trendingTvShows(page);
            responseDto.setData(resp);
            responseDto.setMessage("Success!");
            return buildResponse(responseDto);
        }catch(Exception e){
            e.printStackTrace();
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setMessage(e.getMessage());
            return buildResponse(responseDto);
        }
    }

    public ResponseEntity<ResponseDTO> getShowById(String showId) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            TmdbTvShowDetailsDto resp = tmdbClient.getTvShowById(showId);
            responseDto.setData(resp);
            responseDto.setMessage("Success!");
            return buildResponse(responseDto);
        }catch(Exception e){
            e.printStackTrace();
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setMessage(e.getMessage());
            return buildResponse(responseDto);
        }
    }

    public ResponseEntity<ResponseDTO> tvShowsReleasingToday(int page) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            TmdbSearchResponse resp = tmdbClient.tvShowsReleasingToday(page);
            responseDto.setData(resp);
            responseDto.setMessage("Success!");
            return buildResponse(responseDto);
        }catch(Exception e){
            e.printStackTrace();
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setMessage(e.getMessage());
            return buildResponse(responseDto);
        }
    }
}
