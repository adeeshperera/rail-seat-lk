package com.trainbooking.repository;

import com.trainbooking.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findAllByOrderBySequenceIndexAsc();
    Optional<Station> findBySequenceIndex(int sequenceIndex);
}
