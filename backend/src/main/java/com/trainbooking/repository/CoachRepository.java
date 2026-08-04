package com.trainbooking.repository;

import com.trainbooking.model.Coach;
import com.trainbooking.model.enums.CoachType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachRepository extends JpaRepository<Coach, Long> {
    List<Coach> findByType(CoachType type);
    List<Coach> findByTypeOrderByCoachNumberAsc(CoachType type);
}
