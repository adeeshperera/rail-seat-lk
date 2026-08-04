package com.trainbooking.service;

import com.trainbooking.dto.WaitlistRequest;
import com.trainbooking.dto.WaitlistResponse;
import com.trainbooking.exception.InvalidSegmentException;
import com.trainbooking.model.Waitlist;
import com.trainbooking.model.enums.WaitlistStatus;
import com.trainbooking.repository.CoachRepository;
import com.trainbooking.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final CoachRepository coachRepository;
    private final StationService stationService;

    @Transactional
    public WaitlistResponse joinWaitlist(WaitlistRequest request) {
        if (request.getFromStationIdx() >= request.getToStationIdx()) {
            throw new InvalidSegmentException("Origin must come before destination");
        }

        Waitlist.WaitlistBuilder builder = Waitlist.builder()
            .fromStationIdx(request.getFromStationIdx())
            .toStationIdx(request.getToStationIdx())
            .passengerName(request.getPassengerName())
            .passengerEmail(request.getPassengerEmail());

        if (request.getCoachId() != null) {
            var coach = coachRepository.findById(request.getCoachId()).orElse(null);
            builder.coach(coach);
        }

        Waitlist waitlist = waitlistRepository.save(builder.build());
        log.info("Waitlist entry created: {} for segment {}-{}",
            waitlist.getId(), request.getFromStationIdx(), request.getToStationIdx());

        return toResponse(waitlist);
    }

    @Transactional(readOnly = true)
    public WaitlistResponse getWaitlistEntry(UUID id) {
        Waitlist waitlist = waitlistRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Waitlist entry not found: " + id));
        return toResponse(waitlist);
    }

    /**
     * Called when a booking is cancelled. Finds the oldest WAITING entry
     * that overlaps with the freed segment and marks it as OFFERED.
     */
    @Transactional
    public void processWaitlistForSegment(int fromIdx, int toIdx) {
        List<Waitlist> waitingEntries = waitlistRepository.findWaitingForSegment(fromIdx, toIdx);

        if (!waitingEntries.isEmpty()) {
            Waitlist first = waitingEntries.get(0);
            first.setStatus(WaitlistStatus.OFFERED);
            waitlistRepository.save(first);
            log.info("Waitlist entry {} offered for segment {}-{}", first.getId(), fromIdx, toIdx);
        }
    }

    private WaitlistResponse toResponse(Waitlist waitlist) {
        var fromStation = stationService.getBySequenceIndex(waitlist.getFromStationIdx());
        var toStation = stationService.getBySequenceIndex(waitlist.getToStationIdx());

        // Calculate position in queue
        long position = waitlistRepository.findWaitingForSegment(
            waitlist.getFromStationIdx(), waitlist.getToStationIdx())
            .stream()
            .filter(w -> w.getCreatedAt().isBefore(waitlist.getCreatedAt()) ||
                         w.getCreatedAt().equals(waitlist.getCreatedAt()))
            .count();

        return WaitlistResponse.builder()
            .id(waitlist.getId())
            .fromStationIdx(waitlist.getFromStationIdx())
            .toStationIdx(waitlist.getToStationIdx())
            .fromStation(fromStation.getName())
            .toStation(toStation.getName())
            .passengerName(waitlist.getPassengerName())
            .status(waitlist.getStatus())
            .positionInQueue(position)
            .createdAt(waitlist.getCreatedAt())
            .build();
    }
}
