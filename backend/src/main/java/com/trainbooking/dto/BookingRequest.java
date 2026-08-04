package com.trainbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull(message = "Seat ID is required")
    private Long seatId;

    @NotNull(message = "Origin station index is required")
    private Integer fromStationIdx;

    @NotNull(message = "Destination station index is required")
    private Integer toStationIdx;

    @NotBlank(message = "Passenger name is required")
    private String passengerName;

    @Email(message = "Invalid email format")
    private String passengerEmail;
}
