package com.p2.isitout.controller;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.request.TrackShowRequest;
import com.p2.isitout.dto.request.UpdateTrackShow;
import com.p2.isitout.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/track")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/new")
    public ResponseEntity<ResponseDTO> createNew(@RequestBody TrackShowRequest trackShowRequest) {
        return trackingService.createNew(trackShowRequest);
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO> getAll() {
        return trackingService.getAllTrackedShows();
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO> updateTrackedShows(@RequestBody List<UpdateTrackShow> trackShowRequestList) {
        return trackingService.updateTrackedShows(trackShowRequestList);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDTO> deleteTrackedShows(@RequestBody List<UUID> showIds) {
        return trackingService.deleteTrackedShows(showIds);
    }
}
