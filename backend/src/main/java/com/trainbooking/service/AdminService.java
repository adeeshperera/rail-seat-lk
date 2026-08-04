package com.trainbooking.service;

import com.trainbooking.dto.AdminDashboardResponse;
import com.trainbooking.model.Coach;
import com.trainbooking.model.Seat;
import com.trainbooking.model.Station;
import com.trainbooking.model.enums.CoachType;
import com.trainbooking.model.enums.WaitlistStatus;
import com.trainbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;
    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final WaitlistRepository waitlistRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalBookings = bookingRepository.countConfirmedBookings();
        BigDecimal totalRevenue = bookingRepository.getTotalRevenue();
        long totalWaitlisted = waitlistRepository.findByStatusOrderByCreatedAtAsc(WaitlistStatus.WAITING).size();

        // Calculate per-segment occupancy
        List<Station> stations = stationRepository.findAllByOrderBySequenceIndexAsc();
        List<Coach> reservedCoaches = coachRepository.findByTypeOrderByCoachNumberAsc(CoachType.RESERVED);

        // Get all reserved seat IDs
        List<Long> allSeatIds = reservedCoaches.stream()
            .flatMap(c -> seatRepository.findByCoachIdOrderBySeatNumberAsc(c.getId()).stream())
            .map(Seat::getId)
            .collect(Collectors.toList());

        long totalReservedSeats = allSeatIds.size();

        List<AdminDashboardResponse.SegmentOccupancy> segmentOccupancies = new ArrayList<>();
        double totalOccupancy = 0;

        for (int i = 0; i < stations.size() - 1; i++) {
            Station from = stations.get(i);
            Station to = stations.get(i + 1);

            long occupied = allSeatIds.isEmpty() ? 0 :
                bookingRepository.countOccupiedSeats(allSeatIds, from.getSequenceIndex(), to.getSequenceIndex());

            double rate = totalReservedSeats > 0 ?
                (double) occupied / totalReservedSeats * 100 : 0;

            totalOccupancy += rate;

            segmentOccupancies.add(AdminDashboardResponse.SegmentOccupancy.builder()
                .fromStation(from.getName())
                .toStation(to.getName())
                .fromIdx(from.getSequenceIndex())
                .toIdx(to.getSequenceIndex())
                .occupiedSeats(occupied)
                .totalSeats(totalReservedSeats)
                .occupancyRate(Math.round(rate * 100.0) / 100.0)
                .build());
        }

        double avgOccupancy = segmentOccupancies.isEmpty() ? 0 :
            totalOccupancy / segmentOccupancies.size();

        // Recent bookings
        var recentBookings = bookingRepository.findRecentConfirmedBookings().stream()
            .limit(20)
            .map(b -> {
                String fromName = stationRepository.findBySequenceIndex(b.getFromStationIdx())
                    .map(Station::getName).orElse("Unknown");
                String toName = stationRepository.findBySequenceIndex(b.getToStationIdx())
                    .map(Station::getName).orElse("Unknown");

                return AdminDashboardResponse.RecentBooking.builder()
                    .id(b.getId().toString())
                    .passengerName(b.getPassengerName())
                    .fromStation(fromName)
                    .toStation(toName)
                    .seatNumber(b.getSeat().getSeatNumber())
                    .coachNumber(b.getSeat().getCoach().getCoachNumber())
                    .fare(b.getFare())
                    .createdAt(b.getCreatedAt().toString())
                    .build();
            })
            .collect(Collectors.toList());

        return AdminDashboardResponse.builder()
            .totalBookings(totalBookings)
            .totalRevenue(totalRevenue)
            .averageOccupancy(Math.round(avgOccupancy * 100.0) / 100.0)
            .totalWaitlisted(totalWaitlisted)
            .segmentOccupancies(segmentOccupancies)
            .recentBookings(recentBookings)
            .build();
    }
}
