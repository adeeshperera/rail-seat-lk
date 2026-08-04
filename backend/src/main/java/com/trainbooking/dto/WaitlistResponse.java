package com.trainbooking.dto;

import com.trainbooking.model.enums.WaitlistStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WaitlistResponse {
    private UUID id;
    private int fromStationIdx;
    private int toStationIdx;
    private String fromStation;
    private String toStation;
    private String passengerName;
    private WaitlistStatus status;
    private long positionInQueue;
    private LocalDateTime createdAt;
}
