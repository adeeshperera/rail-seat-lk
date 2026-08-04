package com.trainbooking.controller;

import com.trainbooking.dto.AvailabilityResponse;
import com.trainbooking.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public AvailabilityResponse getAvailability(
            @RequestParam("from") int fromIdx,
            @RequestParam("to") int toIdx) {
        return availabilityService.getAvailability(fromIdx, toIdx);
    }
}
