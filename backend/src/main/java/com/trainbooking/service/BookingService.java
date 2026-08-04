package com.trainbooking.service;

import com.trainbooking.dto.BookingRequest;
import com.trainbooking.dto.BookingResponse;
import com.trainbooking.exception.InvalidSegmentException;
import com.trainbooking.exception.SeatAlreadyBookedException;
import com.trainbooking.model.Booking;
import com.trainbooking.model.Seat;
import com.trainbooking.model.enums.BookingStatus;
import com.trainbooking.repository.BookingRepository;
import com.trainbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final FareService fareService;
    private final StationService stationService;
    private final WaitlistService waitlistService;

    /**
     * Create a booking with pessimistic locking to prevent double-booking.
     * The SELECT ... FOR UPDATE on the seat row ensures only one transaction
     * can book a given seat at a time.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        validateSegment(request.getFromStationIdx(), request.getToStationIdx());

        // 1. Lock the seat row (SELECT ... FOR UPDATE)
        Seat seat = seatRepository.findByIdForUpdate(request.getSeatId())
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Seat not found: " + request.getSeatId()));

        // 2. Check for overlapping confirmed bookings
        var conflicts = bookingRepository.findOverlappingBookings(
            seat.getId(), request.getFromStationIdx(), request.getToStationIdx());

        if (!conflicts.isEmpty()) {
            throw new SeatAlreadyBookedException(
                "Seat " + seat.getSeatNumber() + " in Coach " + seat.getCoach().getCoachNumber() +
                " is already booked for an overlapping segment");
        }

        // 3. Calculate fare
        var fare = fareService.calculateFare(request.getFromStationIdx(), request.getToStationIdx());

        // 4. Create and save booking
        Booking booking = Booking.builder()
            .seat(seat)
            .fromStationIdx(request.getFromStationIdx())
            .toStationIdx(request.getToStationIdx())
            .passengerName(request.getPassengerName())
            .passengerEmail(request.getPassengerEmail())
            .fare(fare)
            .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created: {} for seat {} (Coach {}) from {} to {}",
            booking.getId(), seat.getSeatNumber(), seat.getCoach().getCoachNumber(),
            request.getFromStationIdx(), request.getToStationIdx());

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Booking not found: " + id));
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Booking not found: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidSegmentException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking cancelled: {}", id);

        // Trigger waitlist processing for the freed segment
        waitlistService.processWaitlistForSegment(
            booking.getFromStationIdx(), booking.getToStationIdx());

        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        var fromStation = stationService.getBySequenceIndex(booking.getFromStationIdx());
        var toStation = stationService.getBySequenceIndex(booking.getToStationIdx());

        return BookingResponse.builder()
            .id(booking.getId())
            .seatId(booking.getSeat().getId())
            .seatNumber(booking.getSeat().getSeatNumber())
            .coachNumber(booking.getSeat().getCoach().getCoachNumber())
            .fromStation(fromStation.getName())
            .fromStationIdx(booking.getFromStationIdx())
            .toStation(toStation.getName())
            .toStationIdx(booking.getToStationIdx())
            .passengerName(booking.getPassengerName())
            .passengerEmail(booking.getPassengerEmail())
            .fare(booking.getFare())
            .status(booking.getStatus())
            .createdAt(booking.getCreatedAt())
            .build();
    }

    private void validateSegment(int fromIdx, int toIdx) {
        if (fromIdx >= toIdx) {
            throw new InvalidSegmentException("Origin must come before destination");
        }
    }
}
