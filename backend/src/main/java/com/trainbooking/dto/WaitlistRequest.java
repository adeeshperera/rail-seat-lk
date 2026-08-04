package com.trainbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WaitlistRequest {

    @NotNull(message = "Origin station index is required")
    private Integer fromStationIdx;

    @NotNull(message = "Destination station index is required")
    private Integer toStationIdx;

    private Long coachId; // optional preference

    @NotBlank(message = "Passenger name is required")
    private String passengerName;

    @Email(message = "Invalid email format")
    private String passengerEmail;
}
