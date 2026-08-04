'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { api, WaitlistResponse } from '@/lib/api';

function WaitlistContent() {
  const searchParams = useSearchParams();
  const id = searchParams.get('id');

  const [entry, setEntry] = useState<WaitlistResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    const fetchEntry = () => {
      api.getWaitlistEntry(id)
        .then(setEntry)
        .catch(() => setEntry(null))
        .finally(() => setLoading(false));
    };
    fetchEntry();
    const interval = setInterval(fetchEntry, 15000);
    return () => clearInterval(interval);
  }, [id]);

  if (!id) {
    return <div className="loading-page"><p>No waitlist ID provided.</p></div>;
  }

  if (loading) {
    return <div className="loading-page"><div className="spinner"></div><p>Loading waitlist status...</p></div>;
  }

  if (!entry) {
    return <div className="loading-page"><p>Waitlist entry not found.</p></div>;
  }

  const statusColors: Record<string, string> = {
    WAITING: 'waiting',
    OFFERED: 'offered',
    CONFIRMED: 'confirmed',
    EXPIRED: 'cancelled',
  };

  return (
    <div className="confirmation">
      <div className="confirmation-icon" style={{ background: 'rgba(251, 191, 36, 0.15)' }}>
        
      </div>
      <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.5rem' }}>
        Waitlist Status
      </h1>
      <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
        {entry.status === 'WAITING'
          ? `You are #${entry.positionInQueue} in the queue. This page auto-refreshes.`
          : entry.status === 'OFFERED'
          ? 'A seat is available for you! Please book now.'
          : `Status: ${entry.status}`}
      </p>

      <div className="confirmation-details">
        <div className="detail-row">
          <span className="detail-label">Waitlist ID</span>
          <span className="detail-value" style={{ fontSize: '0.8rem', fontFamily: 'monospace' }}>{entry.id}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Status</span>
          <span className={`badge ${statusColors[entry.status] || ''}`}>{entry.status}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Queue Position</span>
          <span className="detail-value">#{entry.positionInQueue}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Passenger</span>
          <span className="detail-value">{entry.passengerName}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Route</span>
          <span className="detail-value">{entry.fromStation} → {entry.toStation}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Joined At</span>
          <span className="detail-value">{new Date(entry.createdAt).toLocaleString()}</span>
        </div>
      </div>

      {entry.status === 'OFFERED' && (
        <a
          href={`/book?from=${entry.fromStationIdx}&to=${entry.toStationIdx}`}
          className="btn btn-primary btn-lg"
          style={{ marginTop: '1.5rem' }}
        >
           Book Now
        </a>
      )}
    </div>
  );
}

export default function WaitlistPage() {
  return (
    <Suspense fallback={<div className="loading-page"><div className="spinner"></div></div>}>
      <WaitlistContent />
    </Suspense>
  );
}
