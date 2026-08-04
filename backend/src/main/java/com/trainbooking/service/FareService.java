package com.trainbooking.service;

import com.trainbooking.model.Station;
import com.trainbooking.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FareService {

    private final StationRepository stationRepository;

    @Value("${app.fare.base-rate-per-km:15}")
    private BigDecimal baseRatePerKm;

    /**
     * Calculate fare based on distance between two stations.
     * fare = baseRatePerKm × (destination.distanceKm - origin.distanceKm)
     */
    public BigDecimal calculateFare(int fromIdx, int toIdx) {
        Station from = stationRepository.findBySequenceIndex(fromIdx)
            .orElseThrow(() -> new IllegalArgumentException("Invalid from station index: " + fromIdx));
        Station to = stationRepository.findBySequenceIndex(toIdx)
            .orElseThrow(() -> new IllegalArgumentException("Invalid to station index: " + toIdx));

        BigDecimal distance = to.getDistanceKm().subtract(from.getDistanceKm());
        return baseRatePerKm.multiply(distance).setScale(2, RoundingMode.HALF_UP);
    }
}
