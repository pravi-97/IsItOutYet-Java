package com.p2.isitout.service;

import com.p2.isitout.client.tmdb.TmdbClient;
import com.p2.isitout.client.tmdb.dto.TmdbSearchResponse;
import com.p2.isitout.dto.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ShowService {

    private final TmdbClient tmdbClinet;

    public ShowService(TmdbClient tmdbClinet){
        this.tmdbClinet = tmdbClinet;
    }

    public ResponseEntity<ResponseDTO> searchTvShows(String name, int page) {
        ResponseDTO responseDto = new ResponseDTO();
        try{
            TmdbSearchResponse resp = tmdbClinet.searchTvShows(name, page);
            return buildResponse(responseDto);
        }catch(Exception e){
            return buildResponse(responseDto);
        }
    }

    private <T> ResponseEntity<ResponseDTO<T>> buildResponse(ResponseDTO<T> responseDto) {
        HttpStatus status = HttpStatus.resolve(responseDto.getStatusCode());

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity
                .status(status)
                .body(responseDto);
    }

}
