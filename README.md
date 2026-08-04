#  Segment Based Train Seat Booking System

A booking system for Sri Lanka's beautiful **Colombo Fort–Badulla** train line that lets a single reserved seat be booked separately for multiple, non-overlapping legs of the same journey.

## Documentation

### System Architecture
![System Architecture](./documentation/System%20Architecture.png)

### Database ER Diagram
![Database Entity-Relationship Diagram](./documentation/Database%20Entity-Relationship%20Diagram.png)

### Segment Overlap Logic
![Segment Overlap Logic](./documentation/Segment%20Overlap%20Logic.png)

### Booking Flow
![Booking Flow: Sequence Diagram](./documentation/Booking%20Flow%3A%20Sequence%20Diagram.png)

### Cancellation & Waitlist Flow
![Cancellation & Waitlist Flow](./documentation/Cancellation%20%26%20Waitlist%20Flow.png)

### Use Case Diagram
![Use Case Diagram](./documentation/Use%20Case%20Diagram.png)

### Demo Video

[Watch Demo on Google Drive](https://drive.google.com/file/d/1L6d8UHaJiheR2LK1pwSicuvT1Vro7S40/view?usp=sharing)

---

##  Quick Start

```bash
# Clone the repo
git clone <your-repo-url>
cd train-booking-system

# Copy environment variables
cp .env.example .env

# Launch everything (PostgreSQL + Backend + Frontend)
# Docker Compose v2 (plugin):
docker compose up --build
OR docker compose up -d --build

# Docker Compose v1 (standalone):
docker-compose up --build
```

Then open **http://localhost:3000** in your browser.

| Service  | URL                   |
|----------|-----------------------|
| Frontend | http://localhost:3000 |
| Backend  | http://localhost:8080 |
| Database | localhost:5432        |

---

##  Architecture

```
┌──────────────┐     ┌───────────────┐     ┌───────────────┐
│  Next.js 14  │───▶│ Spring Boot 3 │───▶│ PostgreSQL 16 │
│  (Port 3000) │     │  (Port 8080)  │     │  (Port 5432)  │
└──────────────┘     └───────────────┘     └───────────────┘
```

**Tech Stack:**
- **Backend:** Spring Boot 3 (Java 21) + Spring Data JPA
- **Database:** PostgreSQL 16
- **Frontend:** Next.js 14 (App Router, TypeScript)
- **Containerization:** Docker Compose

---

##  Core Design Decisions

### 1. Segment Occupancy Model

Each booking stores `(seat_id, from_station_idx, to_station_idx)`. Two bookings on the same seat **conflict** if their index ranges overlap:

```
conflict = (existing.from < new.to) AND (existing.to > new.from)
```

This allows Seat #1 to be booked Colombo→Kandy (0→3) by Ravindu **and** Kandy→Badulla (3→9) by Kavindu, and neither range overlaps.

**Alternatives considered:**

| Approach | Why rejected |
|----------|-------------|
| Per-station boolean matrix (N booleans per seat) | N rows per booking instead of 1, so it is harder to query as one single step |
| Separate `seat_segments` table with one row per station | Too many writes needed, complex overlap JOIN |
| Range types (PostgreSQL `int4range`) | Adds neatness but needs PG-specific DDL, while the simple integer comparison is just as correct and works on more systems |

### 2. Concurrency: No Double-Booking

`BookingService.createBooking()` uses pessimistic row-level locking inside a `@Transactional` block:

1. `SELECT ... FOR UPDATE` on the seat row, blocking concurrent transactions on the same seat
2. Check for overlapping CONFIRMED bookings
3. Insert if clear, or throw `409 SeatAlreadyBookedException` otherwise

**Alternatives considered:**

| Approach | Why rejected |
|----------|-------------|
| Optimistic locking (`@Version`) | Needs extra retry code on the client, and conflicts can still happen under high load without proper retry delays |
| Redis distributed lock | Extra infrastructure with no benefit, since PostgreSQL already handles row-level access in order |
| Application-level `synchronized` | Fails when scaling out to multiple backend instances |

### 3. Fare Calculation

```
fare = base_rate_per_km × (destination.distance_km − origin.distance_km)
```

`base_rate_per_km` defaults to **LKR 15/km** and is overridable via `FARE_BASE_RATE` env var.

**Alternative considered:** Fixed flat fares per segment pair. Rejected because it doesn't work well when new stations are added and doesn't fairly reflect the actual distance traveled.

### 4. Configurable Design

All variable quantities (stations, coaches, seats-per-coach, fare rate) are kept outside the code:
- Stations and coach layout are seeded from `DataSeeder.java` arrays. To extend the route, update the array and restart.
- `RESERVED_COACHES`, `UNRESERVED_COACHES`, `SEATS_PER_COACH`, `FARE_BASE_RATE` are all environment-variable overridable via `application.yml`

---

### API Testing & Test Cases

- [Postman Collection](./documentation/Train%20Booking%20API.postman_collection.json) — Import into Postman and set `base_url` to `http://localhost:8080`
- [Test Cases](./documentation/README.md) — Unit, integration, and concurrency test specifications

---

##  Challenges Faced

### 1. Segment Overlap Edge Case: Adjacent Bookings
The hardest question about correctness: should a booking for legs [0,3) and [3,9) conflict? They share station index 3 (Kandy) but the first passenger *leaves* at Kandy and the second *boards* there. The answer is **no conflict** because the overlap formula `from < to AND to > from` handles this correctly: 0 < 9 is true, but 3 > 3 is **false**, so they don't overlap. Getting this formula right early on avoided a hidden bug.

### 2. Concurrent Booking Under Load
`SELECT ... FOR UPDATE` locks the *seat row*, not the booking table. This means two passengers trying to book **different seats** on the same coach are never blocked by each other. Only same-seat conflicts are handled one at a time. This keeps speed high while still guaranteeing correctness.

### 3. Frontend Real-Time Consistency
Since the frontend polls every 10 seconds, a seat can appear available in the UI but become booked before the user confirms. The 409 response from the backend is caught, a toast notification is shown, and the seat map auto-refreshes, giving the user clear feedback without a broken state.

### 4. Docker Build Context Size
The initial Docker build for the frontend was slow (609 MB context) because `node_modules` was being sent to the daemon. Adding `.dockerignore` with `node_modules` cut the context down to ~5 MB and made build time much faster.

---

##  Extra Credit Features

###  Seat Map Visualization

**Problem:** Listing available seats as a plain list gives no sense of position, so passengers can't choose a preferred spot in the coach.

**Solution:** An interactive coach-by-coach grid where each cell stands for one seat. Clicking selects it for booking.

**Design:** Three clear colors show the difference between the segment model and a plain system:
-  **Green**: Available for your requested leg
-  **Amber**: Seat is in use *on a different leg* (the segment-sharing concept made visible)
-  **Red**: Booked for an overlapping leg (unavailable)

The amber state is unique to segment-based booking, and it is the visual proof that the system reuses seats across legs.

---

###  Waitlisting

**Problem:** When every seat is booked for a leg, passengers have no other option and may leave the system entirely.

**Solution:** FIFO waitlist per segment. On booking cancellation, `WaitlistService.processWaitlistForSegment()` automatically finds the oldest matching `WAITING` entry and promotes it to `OFFERED`.

**Design:** Waitlist entries store `(from_idx, to_idx)` with the same overlap logic as bookings, so a cancellation of Colombo→Badulla (0→9) correctly notifies someone waiting for Colombo→Kandy (0→3). The status page auto-refreshes every 15 seconds and shows a "Book Now" CTA when status becomes `OFFERED`.

---

###  Admin Dashboard

**Problem:** The department has no visibility into which segments are under/over-utilized, making revenue and scheduling decisions blind.

**Solution:** A real-time dashboard showing total bookings, total revenue, per-segment occupancy rates, and a bar chart of occupancy, plus a recent bookings feed.

**Design:** Occupancy is calculated per adjacent-station segment (not per booking leg), giving a detailed view of where demand is highest. Color coding (green → amber → red) matches the seat map, creating a consistent visual style across the app. Auto-refreshes every 30 seconds.

---

###  Booking Conflict UX

**Problem:** In a concurrent system, a seat shown as available may be taken by the time the user clicks "Confirm". Showing a generic error is confusing.

**Solution:** The frontend handles the `409 Conflict` response specifically: a toast notification appears ("Seat was just taken!"), the selected seat is cleared, and the seat map refreshes automatically so the user immediately sees the current state and can pick another seat without a manual reload.

**Design:** Availability is also polled every 10 seconds while the user is on the booking page, so the map stays fresh even before they attempt to book.

---



##  API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stations` | List all stations in order |
| GET | `/api/availability?from=X&to=Y` | Available seats for a leg |
| POST | `/api/bookings` | Create a booking |
| GET | `/api/bookings/{id}` | Get booking details |
| DELETE | `/api/bookings/{id}` | Cancel a booking |
| POST | `/api/waitlist` | Join waitlist for a leg |
| GET | `/api/waitlist/{id}` | Check waitlist status |
| GET | `/api/admin/dashboard` | Admin dashboard data |

---

##  Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USER` | `trainuser` | PostgreSQL username |
| `DB_PASSWORD` | `trainpass123` | PostgreSQL password |
| `DB_NAME` | `trainbooking` | Database name |
| `FARE_BASE_RATE` | `15` | Base fare rate (LKR per km) |
| `RESERVED_COACHES` | `3` | Number of reserved coaches to seed |
| `UNRESERVED_COACHES` | `5` | Number of unreserved coaches to seed |
| `SEATS_PER_COACH` | `50` | Seats per reserved coach |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Backend URL for the frontend |
