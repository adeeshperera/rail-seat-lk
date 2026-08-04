package com.trainbooking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalBookings;
    private BigDecimal totalRevenue;
    private double averageOccupancy;
    private long totalWaitlisted;
    private List<SegmentOccupancy> segmentOccupancies;
    private List<RecentBooking> recentBookings;

    @Data
    @Builder
    public static class SegmentOccupancy {
        private String fromStation;
        private String toStation;
        private int fromIdx;
        private int toIdx;
        private long occupiedSeats;
        private long totalSeats;
        private double occupancyRate;
    }

    @Data
    @Builder
    public static class RecentBooking {
        private String id;
        private String passengerName;
        private String fromStation;
        private String toStation;
        private int seatNumber;
        private int coachNumber;
        private BigDecimal fare;
        private String createdAt;
    }
}
