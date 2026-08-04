package com.trainbooking.repository;

import com.trainbooking.model.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT s FROM Seat s JOIN FETCH s.coach WHERE s.coach.type = com.trainbooking.model.enums.CoachType.RESERVED ORDER BY s.coach.coachNumber, s.seatNumber")
    List<Seat> findAllReservedSeats();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);

    List<Seat> findByCoachIdOrderBySeatNumberAsc(Long coachId);
}
