package com.p2.isitout.service;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.UserShowDto;
import com.p2.isitout.dto.request.TrackShowRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {


    public ResponseEntity<ResponseDTO> createNew(TrackShowRequest trackShowRequest) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            if(trackShowRequest.getTmdbId() == null || trackShowRequest.getTmdbId().isEmpty()  || trackShowRequest.getTvmazeId() == null || trackShowRequest.getTvmazeId().isEmpty()){
                throw new Exception("Either TmdbId or TmazeId is required");
            }
//            String userId = fetchUserIdFromToken();
//            String showId = fetchShowId(trackShowRequest.getTmdbId() != null ? trackShowRequest.getTmdbId() : trackShowRequest.getTvmazeId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return buildResponse(responseDto);
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

    private ResponseDTO createDefaultResponseDTO() {
        ResponseDTO responseDto = new ResponseDTO();
        responseDto.setStatusCode(200);
        responseDto.setStatus(true);
        return responseDto;
    }
}
