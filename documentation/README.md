# Test Cases

## Unit Tests

| # | Component | Test | Expected |
|---|-----------|------|----------|
| U1 | `FareService` | Colombo(0km)→Kandy(121km) at LKR 15/km | LKR 1,815.00 |
| U2 | `FareService` | Kandy(121km)→Badulla(292km) at LKR 15/km | LKR 2,565.00 |
| U3 | Overlap logic | Ravindu[0,3) vs Kavindu[3,9) | No conflict |
| U4 | Overlap logic | Ravindu[0,3) vs Eve[2,4) | Conflict |
| U5 | Overlap logic | Full journey[0,9) vs any sub-leg | Conflict |
| U6 | `DataSeeder` | Run twice on same DB | Seeds only once (idempotent) |

## Integration Tests

| # | Endpoint | Scenario | Expected HTTP |
|---|----------|----------|---------------|
| I1 | `POST /api/bookings` | Valid seat, clear segment | 201 Created |
| I2 | `POST /api/bookings` | Same seat, overlapping segment | 409 Conflict |
| I3 | `POST /api/bookings` | Same seat, adjacent (non-overlapping) segment | 201 Created |
| I4 | `GET /api/availability?from=0&to=3` | Fresh DB | All 150 seats AVAILABLE |
| I5 | `GET /api/availability?from=0&to=9` | After booking [0,3) | 1 seat PARTIALLY_BOOKED, rest AVAILABLE |
| I6 | `DELETE /api/bookings/{id}` | Cancel confirmed booking | 200 + status CANCELLED |
| I7 | `POST /api/waitlist` | Valid segment | 201 Created |
| I8 | `GET /api/admin/dashboard` | After 2 bookings | totalBookings=2, revenue>0 |

## Concurrency Test

| # | Scenario | Expected |
|---|----------|----------|
| C1 | 10 concurrent `POST /api/bookings` for the same seat+segment | Exactly 1 succeeds (201), 9 fail (409) |
| C2 | 2 concurrent bookings for same seat, different non-overlapping legs | Both succeed (201) |
