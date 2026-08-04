package com.trainbooking.model;

import com.trainbooking.model.enums.CoachType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coaches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coach_number", nullable = false, unique = true)
    private int coachNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoachType type;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @OneToMany(mappedBy = "coach", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();
}
