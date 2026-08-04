package com.trainbooking.dto;

import com.trainbooking.model.enums.SeatStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AvailabilityResponse {
    private int fromStationIdx;
    private int toStationIdx;
    private String fromStation;
    private String toStation;
    private java.math.BigDecimal fare;
    private List<CoachAvailability> coaches;

    @Data
    @Builder
    public static class CoachAvailability {
        private Long coachId;
        private int coachNumber;
        private int totalSeats;
        private int availableCount;
        private List<SeatInfo> seats;
    }

    @Data
    @Builder
    public static class SeatInfo {
        private Long seatId;
        private int seatNumber;
        private SeatStatus status;
    }
}
