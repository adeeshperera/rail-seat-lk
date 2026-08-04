'use client';

import { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { api, AvailabilityResponse, SeatInfo, ApiError } from '@/lib/api';

function BookingPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const fromIdx = parseInt(searchParams.get('from') || '');
  const toIdx = parseInt(searchParams.get('to') || '');

  const [availability, setAvailability] = useState<AvailabilityResponse | null>(null);
  const [selectedSeat, setSelectedSeat] = useState<SeatInfo | null>(null);
  const [selectedCoachNum, setSelectedCoachNum] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState<{ msg: string; type: string } | null>(null);

  // Booking form
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');

  // Waitlist form
  const [showWaitlist, setShowWaitlist] = useState(false);
  const [wlName, setWlName] = useState('');
  const [wlEmail, setWlEmail] = useState('');

  const fetchAvailability = useCallback(async () => {
    if (isNaN(fromIdx) || isNaN(toIdx)) return;
    try {
      const data = await api.getAvailability(fromIdx, toIdx);
      setAvailability(data);
      // Check if all seats are booked
      const totalAvailable = data.coaches.reduce((sum, c) => sum + c.availableCount, 0);
      if (totalAvailable === 0) setShowWaitlist(true);
    } catch {
      setError('Failed to load availability');
    } finally {
      setLoading(false);
    }
  }, [fromIdx, toIdx]);

  useEffect(() => {
    fetchAvailability();
    // Auto-refresh every 10 seconds
    const interval = setInterval(fetchAvailability, 10000);
    return () => clearInterval(interval);
  }, [fetchAvailability]);

  const showToast = (msg: string, type: string) => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 4000);
  };

  const handleBook = async () => {
    if (!selectedSeat || !name.trim()) return;
    setBooking(true);
    setError('');
    try {
      const res = await api.createBooking({
        seatId: selectedSeat.seatId,
        fromStationIdx: fromIdx,
        toStationIdx: toIdx,
        passengerName: name.trim(),
        passengerEmail: email.trim() || undefined,
      });
      showToast('Booking confirmed!', 'success');
      router.push(`/booking?id=${res.id}`);
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      if (apiErr.status === 409) {
        showToast('Seat was just taken! Refreshing...', 'error');
        setSelectedSeat(null);
        fetchAvailability();
      } else {
        setError(apiErr.message || 'Booking failed');
      }
    } finally {
      setBooking(false);
    }
  };

  const handleWaitlist = async () => {
    if (!wlName.trim()) return;
    try {
      const res = await api.joinWaitlist({
        fromStationIdx: fromIdx,
        toStationIdx: toIdx,
        passengerName: wlName.trim(),
        passengerEmail: wlEmail.trim() || undefined,
      });
      router.push(`/waitlist?id=${res.id}`);
    } catch {
      showToast('Failed to join waitlist', 'error');
    }
  };

  if (isNaN(fromIdx) || isNaN(toIdx)) {
    return (
      <div className="loading-page">
        <p>Invalid route. Please <a href="/" style={{ color: 'var(--accent-primary)' }}>go back</a> and select stations.</p>
      </div>
    );
  }

  if (loading) {
    return <div className="loading-page"><div className="spinner"></div><p>Finding available seats...</p></div>;
  }

  if (!availability) {
    return <div className="loading-page"><p>No data available. {error}</p></div>;
  }

  const totalAvailable = availability.coaches.reduce((sum, c) => sum + c.availableCount, 0);
  const totalSeats = availability.coaches.reduce((sum, c) => sum + c.totalSeats, 0);

  return (
    <div>
      <div className="section-header">
        <div>
          <h1 className="section-title">
            {availability.fromStation} → {availability.toStation}
          </h1>
          <p className="section-subtitle">
            {totalAvailable} of {totalSeats} seats available · Fare: LKR {availability.fare.toFixed(2)}
          </p>
        </div>
        <button className="btn btn-secondary" onClick={() => router.push('/')}>
          ← Change Route
        </button>
      </div>

      <div className="legend">
        <div className="legend-item"><div className="legend-dot available"></div> Available</div>
        <div className="legend-item"><div className="legend-dot partial"></div> Available (used on other legs)</div>
        <div className="legend-item"><div className="legend-dot booked"></div> Booked</div>
        <div className="legend-item"><div className="legend-dot selected"></div> Selected</div>
      </div>

      <div className="booking-layout">
        {/* Seat Map */}
        <div className="seat-map">
          {availability.coaches.map(coach => (
            <div key={coach.coachId} className="coach-section">
              <div className="coach-header">
                <span className="coach-name"> Coach {coach.coachNumber}</span>
                <span className="coach-count">{coach.availableCount} / {coach.totalSeats} available</span>
              </div>
              <div className="seat-grid">
                {coach.seats.map(seat => {
                  const isSelected = selectedSeat?.seatId === seat.seatId;
                  const statusClass = seat.status === 'AVAILABLE' ? 'available'
                    : seat.status === 'PARTIALLY_BOOKED' ? 'partially-booked'
                    : 'booked';

                  return (
                    <div
                      key={seat.seatId}
                      className={`seat ${statusClass} ${isSelected ? 'selected' : ''}`}
                      onClick={() => {
                        if (seat.status !== 'BOOKED') {
                          setSelectedSeat(seat);
                          setSelectedCoachNum(coach.coachNumber);
                          setShowWaitlist(false);
                        }
                      }}
                      title={`Seat ${seat.seatNumber} - ${seat.status.replace('_', ' ')}`}
                    >
                      {seat.seatNumber}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/* Booking Sidebar */}
        <div className="booking-sidebar">
          {selectedSeat ? (
            <div className="card">
              <div className="card-header">
                <h2 className="card-title">Book Seat {selectedSeat.seatNumber}</h2>
                <p className="card-subtitle">Coach {selectedCoachNum} · {availability.fromStation} → {availability.toStation}</p>
              </div>

              <div style={{ padding: '1rem 0', borderBottom: '1px solid var(--border-color)', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Fare</span>
                  <span style={{ fontWeight: 700, fontSize: '1.25rem', color: 'var(--accent-success)' }}>
                    LKR {availability.fare.toFixed(2)}
                  </span>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="passenger-name">Passenger Name *</label>
                <input
                  id="passenger-name"
                  type="text"
                  className="form-input"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  placeholder="Enter your name"
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="passenger-email">Email (optional)</label>
                <input
                  id="passenger-email"
                  type="email"
                  className="form-input"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="your@email.com"
                />
              </div>

              {error && <p style={{ color: 'var(--accent-danger)', fontSize: '0.85rem', marginBottom: '1rem' }}>{error}</p>}

              <button
                id="confirm-booking"
                className="btn btn-primary btn-lg"
                style={{ width: '100%' }}
                onClick={handleBook}
                disabled={booking || !name.trim()}
              >
                {booking ? ' Booking...' : ' Confirm Booking'}
              </button>
            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
              <div style={{ fontSize: '3rem', marginBottom: '1rem', opacity: 0.5 }}></div>
              <h3 style={{ fontWeight: 600, marginBottom: '0.5rem' }}>Select a Seat</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                Click on an available seat in the map to select it for booking.
              </p>
            </div>
          )}

          {/* Waitlist */}
          {(showWaitlist || totalAvailable === 0) && (
            <div className="waitlist-section" style={{ marginTop: '1rem' }}>
              <h3> No seats? Join the Waitlist</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1rem' }}>
                You&apos;ll be notified when a seat becomes available for this leg.
              </p>
              <div className="form-group">
                <input
                  id="waitlist-name"
                  type="text"
                  className="form-input"
                  value={wlName}
                  onChange={e => setWlName(e.target.value)}
                  placeholder="Your name"
                />
              </div>
              <div className="form-group">
                <input
                  id="waitlist-email"
                  type="email"
                  className="form-input"
                  value={wlEmail}
                  onChange={e => setWlEmail(e.target.value)}
                  placeholder="Email (optional)"
                />
              </div>
              <button
                id="join-waitlist"
                className="btn btn-secondary"
                style={{ width: '100%' }}
                onClick={handleWaitlist}
                disabled={!wlName.trim()}
              >
                Join Waitlist
              </button>
            </div>
          )}
        </div>
      </div>

      {toast && <div className={`toast ${toast.type}`}>{toast.msg}</div>}
    </div>
  );
}

export default function BookingPage() {
  return (
    <Suspense fallback={<div className="loading-page"><div className="spinner"></div></div>}>
      <BookingPageContent />
    </Suspense>
  );
}
