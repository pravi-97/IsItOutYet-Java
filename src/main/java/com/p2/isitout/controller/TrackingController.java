package com.p2.isitout.controller;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.request.TrackShowRequest;
import com.p2.isitout.dto.request.UpdateTrackShow;
import com.p2.isitout.dto.response.MyShowListDto;
import com.p2.isitout.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/new")
    public ResponseEntity<ResponseDTO<Void>> createNew(@RequestBody TrackShowRequest trackShowRequest) {
        trackingService.createNew(trackShowRequest);
        return createResponse(null, "Show tracking created successfully", HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO< List<MyShowListDto>>> getAll() {
        List<MyShowListDto> myShowListDtos = trackingService.getAllTrackedShows();
        return createResponse(myShowListDtos, "Fetched All Tracked Shows", HttpStatus.OK);
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<Void>> updateTrackedShows(@RequestBody List<UpdateTrackShow> trackShowRequestList) {
        trackingService.updateTrackedShows(trackShowRequestList);
        return createResponse(null, "Successfully Updated Tracked Shows", HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDTO<Void>> deleteTrackedShows(@RequestBody List<UUID> showIds) {
        return createResponse(null, "Successfully Deleted Tracked Show", HttpStatus.OK);
    }

    private <T> ResponseEntity<ResponseDTO<T>> createResponse( T data, String message, HttpStatus status) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setData(data);
        response.setMessage(message);
        response.setStatusCode(status.value());
        return new ResponseEntity<>(response, status);
    }
}
