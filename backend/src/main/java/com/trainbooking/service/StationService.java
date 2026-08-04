package com.trainbooking.service;

import com.trainbooking.model.Station;
import com.trainbooking.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAllByOrderBySequenceIndexAsc();
    }

    public Station getBySequenceIndex(int idx) {
        return stationRepository.findBySequenceIndex(idx)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Station not found with sequence index: " + idx));
    }
}
