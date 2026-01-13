package com.p2.isitout.controller;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.request.TrackShowRequest;
import com.p2.isitout.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
