package com.trainbooking.service;

import com.trainbooking.dto.AvailabilityResponse;
import com.trainbooking.exception.InvalidSegmentException;
import com.trainbooking.model.Booking;
import com.trainbooking.model.Coach;
import com.trainbooking.model.Seat;
import com.trainbooking.model.enums.CoachType;
import com.trainbooking.model.enums.SeatStatus;
import com.trainbooking.repository.BookingRepository;
import com.trainbooking.repository.CoachRepository;
import com.trainbooking.repository.SeatRepository;
import com.trainbooking.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final FareService fareService;

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(int fromIdx, int toIdx) {
        validateSegment(fromIdx, toIdx);

        var fromStation = stationRepository.findBySequenceIndex(fromIdx)
            .orElseThrow(() -> new InvalidSegmentException("Invalid origin station"));
        var toStation = stationRepository.findBySequenceIndex(toIdx)
            .orElseThrow(() -> new InvalidSegmentException("Invalid destination station"));

        var fare = fareService.calculateFare(fromIdx, toIdx);
        var reservedCoaches = coachRepository.findByTypeOrderByCoachNumberAsc(CoachType.RESERVED);

        List<AvailabilityResponse.CoachAvailability> coachAvailabilities = new ArrayList<>();

        for (Coach coach : reservedCoaches) {
            var seats = seatRepository.findByCoachIdOrderBySeatNumberAsc(coach.getId());
            List<AvailabilityResponse.SeatInfo> seatInfos = new ArrayList<>();
            int availableCount = 0;

            for (Seat seat : seats) {
                List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                    seat.getId(), fromIdx, toIdx);
                List<Booking> allConfirmed = bookingRepository.findConfirmedBySeatId(seat.getId());

                SeatStatus status;
                if (!overlapping.isEmpty()) {
                    status = SeatStatus.BOOKED;
                } else if (!allConfirmed.isEmpty()) {
                    status = SeatStatus.PARTIALLY_BOOKED;
                    availableCount++;
                } else {
                    status = SeatStatus.AVAILABLE;
                    availableCount++;
                }

                seatInfos.add(AvailabilityResponse.SeatInfo.builder()
                    .seatId(seat.getId())
                    .seatNumber(seat.getSeatNumber())
                    .status(status)
                    .build());
            }

            coachAvailabilities.add(AvailabilityResponse.CoachAvailability.builder()
                .coachId(coach.getId())
                .coachNumber(coach.getCoachNumber())
                .totalSeats(coach.getTotalSeats())
                .availableCount(availableCount)
                .seats(seatInfos)
                .build());
        }

        return AvailabilityResponse.builder()
            .fromStationIdx(fromIdx)
            .toStationIdx(toIdx)
            .fromStation(fromStation.getName())
            .toStation(toStation.getName())
            .fare(fare)
            .coaches(coachAvailabilities)
            .build();
    }

    private void validateSegment(int fromIdx, int toIdx) {
        if (fromIdx >= toIdx) {
            throw new InvalidSegmentException("Origin station must come before destination station");
        }
        if (fromIdx < 0) {
            throw new InvalidSegmentException("Invalid origin station index");
        }
    }
}
