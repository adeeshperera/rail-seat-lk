package com.trainbooking.config;

import com.trainbooking.model.Coach;
import com.trainbooking.model.Seat;
import com.trainbooking.model.Station;
import com.trainbooking.model.enums.CoachType;
import com.trainbooking.repository.CoachRepository;
import com.trainbooking.repository.SeatRepository;
import com.trainbooking.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;

    @Value("${app.seed.reserved-coaches:3}")
    private int reservedCoaches;

    @Value("${app.seed.unreserved-coaches:5}")
    private int unreservedCoaches;

    @Value("${app.seed.seats-per-coach:50}")
    private int seatsPerCoach;

    @Override
    @Transactional
    public void run(String... args) {
        if (stationRepository.count() > 0) {
            log.info("Database already seeded. Skipping.");
            return;
        }

        log.info("Seeding database with initial data...");
        seedStations();
        seedCoachesAndSeats();
        log.info("Database seeding complete.");
    }

    private void seedStations() {
        // Real stations on the Colombo Fort–Badulla line with approximate distances
        String[][] stationData = {
            {"Colombo Fort", "0"},
            {"Polgahawela", "76"},
            {"Peradeniya Junction", "115"},
            {"Kandy", "121"},
            {"Gampola", "138"},
            {"Nawalapitiya", "158"},
            {"Hatton", "186"},
            {"Nanu Oya", "214"},
            {"Haputale", "248"},
            {"Badulla", "292"}
        };

        for (int i = 0; i < stationData.length; i++) {
            Station station = Station.builder()
                .name(stationData[i][0])
                .sequenceIndex(i)
                .distanceKm(new BigDecimal(stationData[i][1]))
                .build();
            stationRepository.save(station);
        }
        log.info("Seeded {} stations", stationData.length);
    }

    private void seedCoachesAndSeats() {
        for (int c = 1; c <= reservedCoaches; c++) {
            Coach coach = Coach.builder()
                .coachNumber(c)
                .type(CoachType.RESERVED)
                .totalSeats(seatsPerCoach)
                .build();
            coach = coachRepository.save(coach);

            for (int s = 1; s <= seatsPerCoach; s++) {
                seatRepository.save(Seat.builder().coach(coach).seatNumber(s).build());
            }
        }
        log.info("Seeded {} reserved coaches with {} seats each", reservedCoaches, seatsPerCoach);

        for (int c = reservedCoaches + 1; c <= reservedCoaches + unreservedCoaches; c++) {
            coachRepository.save(Coach.builder()
                .coachNumber(c)
                .type(CoachType.UNRESERVED)
                .totalSeats(0)
                .build());
        }
        log.info("Seeded {} unreserved coaches", unreservedCoaches);
    }
}
