package com.trainbooking.dto;

import com.trainbooking.model.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private Long seatId;
    private int seatNumber;
    private int coachNumber;
    private String fromStation;
    private int fromStationIdx;
    private String toStation;
    private int toStationIdx;
    private String passengerName;
    private String passengerEmail;
    private BigDecimal fare;
    private BookingStatus status;
    private LocalDateTime createdAt;
}
