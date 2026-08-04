package com.trainbooking.repository;

import com.trainbooking.model.Waitlist;
import com.trainbooking.model.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, UUID> {

    /**
     * Find waiting entries whose requested segment overlaps with the freed segment,
     * ordered by creation time (FIFO).
     */
    @Query("SELECT w FROM Waitlist w WHERE w.status = 'WAITING' " +
           "AND w.fromStationIdx < :toIdx " +
           "AND w.toStationIdx > :fromIdx " +
           "ORDER BY w.createdAt ASC")
    List<Waitlist> findWaitingForSegment(@Param("fromIdx") int fromIdx,
                                         @Param("toIdx") int toIdx);

    List<Waitlist> findByStatusOrderByCreatedAtAsc(WaitlistStatus status);

    @Query("SELECT COUNT(w) FROM Waitlist w WHERE w.status = 'WAITING' " +
           "AND w.fromStationIdx < :toIdx AND w.toStationIdx > :fromIdx")
    long countWaitingForSegment(@Param("fromIdx") int fromIdx, @Param("toIdx") int toIdx);
}
