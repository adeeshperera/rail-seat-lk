package com.trainbooking.repository;

import com.trainbooking.model.Booking;
import com.trainbooking.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Find all confirmed bookings that overlap with the given segment on a specific seat.
     * Two segments [a1, a2) and [b1, b2) overlap iff a1 < b2 AND a2 > b1.
     */
    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.fromStationIdx < :toIdx " +
           "AND b.toStationIdx > :fromIdx")
    List<Booking> findOverlappingBookings(@Param("seatId") Long seatId,
                                          @Param("fromIdx") int fromIdx,
                                          @Param("toIdx") int toIdx);

    /**
     * Find all confirmed bookings for a given seat.
     */
    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedBySeatId(@Param("seatId") Long seatId);

    /**
     * Count confirmed bookings that overlap with a segment across ALL seats.
     */
    @Query("SELECT COUNT(DISTINCT b.seat.id) FROM Booking b WHERE b.seat.id IN :seatIds " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.fromStationIdx < :toIdx " +
           "AND b.toStationIdx > :fromIdx")
    long countOccupiedSeats(@Param("seatIds") List<Long> seatIds,
                            @Param("fromIdx") int fromIdx,
                            @Param("toIdx") int toIdx);

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.fare), 0) FROM Booking b WHERE b.status = 'CONFIRMED'")
    BigDecimal getTotalRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CONFIRMED'")
    long countConfirmedBookings();

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' ORDER BY b.createdAt DESC")
    List<Booking> findRecentConfirmedBookings();
}
