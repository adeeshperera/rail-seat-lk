'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { api, BookingResponse } from '@/lib/api';

function ConfirmationContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const id = searchParams.get('id');

  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!id) return;
    api.getBooking(id)
      .then(setBooking)
      .catch(() => setBooking(null))
      .finally(() => setLoading(false));
  }, [id]);

  const handleCancel = async () => {
    if (!id || !confirm('Are you sure you want to cancel this booking?')) return;
    setCancelling(true);
    try {
      const updated = await api.cancelBooking(id);
      setBooking(updated);
    } catch {
      alert('Failed to cancel booking');
    } finally {
      setCancelling(false);
    }
  };

  if (!id) {
    return <div className="loading-page"><p>No booking ID provided.</p></div>;
  }

  if (loading) {
    return <div className="loading-page"><div className="spinner"></div><p>Loading booking...</p></div>;
  }

  if (!booking) {
    return <div className="loading-page"><p>Booking not found.</p></div>;
  }

  const isCancelled = booking.status === 'CANCELLED';

  return (
    <div className="confirmation">
      <div className="confirmation-icon">
        {isCancelled ? '' : ''}
      </div>
      <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
        {isCancelled ? 'Booking Cancelled' : 'Booking Confirmed!'}
      </h1>
      <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
        {isCancelled
          ? 'This booking has been cancelled. The seat is now available for others.'
          : 'Your seat has been reserved. Have a wonderful journey!'}
      </p>

      <div className="confirmation-details">
        <div className="detail-row">
          <span className="detail-label">Booking ID</span>
          <span className="detail-value" style={{ fontSize: '0.8rem', fontFamily: 'monospace' }}>{booking.id}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Status</span>
          <span className={`badge ${isCancelled ? 'cancelled' : 'confirmed'}`}>
            {booking.status}
          </span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Passenger</span>
          <span className="detail-value">{booking.passengerName}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Route</span>
          <span className="detail-value">{booking.fromStation} → {booking.toStation}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Seat</span>
          <span className="detail-value">Coach {booking.coachNumber}, Seat {booking.seatNumber}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Fare</span>
          <span className="detail-value" style={{ color: 'var(--accent-success)' }}>LKR {booking.fare.toFixed(2)}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Booked At</span>
          <span className="detail-value">{new Date(booking.createdAt).toLocaleString()}</span>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem', justifyContent: 'center' }}>
        <button className="btn btn-primary" onClick={() => router.push('/')}>
          Book Another Seat
        </button>
        {!isCancelled && (
          <button className="btn btn-danger" onClick={handleCancel} disabled={cancelling}>
            {cancelling ? 'Cancelling...' : 'Cancel Booking'}
          </button>
        )}
      </div>
    </div>
  );
}

export default function BookingConfirmation() {
  return (
    <Suspense fallback={<div className="loading-page"><div className="spinner"></div></div>}>
      <ConfirmationContent />
    </Suspense>
  );
}
