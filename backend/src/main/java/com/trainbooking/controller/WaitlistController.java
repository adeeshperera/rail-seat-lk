package com.trainbooking.controller;

import com.trainbooking.dto.WaitlistRequest;
import com.trainbooking.dto.WaitlistResponse;
import com.trainbooking.service.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistResponse joinWaitlist(@Valid @RequestBody WaitlistRequest request) {
        return waitlistService.joinWaitlist(request);
    }

    @GetMapping("/{id}")
    public WaitlistResponse getWaitlistEntry(@PathVariable UUID id) {
        return waitlistService.getWaitlistEntry(id);
    }
}
