package com.p2.isitout.service;

import com.p2.isitout.client.tmdb.TmdbClient;
import com.p2.isitout.client.tmdb.dto.TmdbSearchResponse;
import com.p2.isitout.dto.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ShowService {

    private final TmdbClient tmdbClient;

    public ShowService(TmdbClient tmdbClient){
        this.tmdbClient = tmdbClient;
    }

    public ResponseEntity<ResponseDTO> searchTvShows(String name, int page) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            TmdbSearchResponse resp = tmdbClient.searchTvShows(name, page);
            responseDto.setData(resp);
            responseDto.setMessage("Success!");
            return buildResponse(responseDto);
        }catch(Exception e){
            e.printStackTrace();
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
}
