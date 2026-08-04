'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { api, Station } from '@/lib/api';

export default function HomePage() {
  const router = useRouter();
  const [stations, setStations] = useState<Station[]>([]);
  const [fromIdx, setFromIdx] = useState<string>('');
  const [toIdx, setToIdx] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getStations()
      .then(setStations)
      .catch(() => setError('Failed to load stations. Is the backend running?'))
      .finally(() => setLoading(false));
  }, []);

  const handleSearch = () => {
    const from = parseInt(fromIdx);
    const to = parseInt(toIdx);
    if (isNaN(from) || isNaN(to)) {
      setError('Please select both stations');
      return;
    }
    if (from >= to) {
      setError('Origin must be before destination on the line');
      return;
    }
    setError('');
    router.push(`/book?from=${from}&to=${to}`);
  };

  if (loading) {
    return <div className="loading-page"><div className="spinner"></div><p>Loading stations...</p></div>;
  }

  return (
    <div>
      <section className="hero">
        <div className="hero-badge"> Sri Lanka Railways</div>
        <h1>
          Book Your Seat on the<br />
          <span>Colombo–Badulla Line</span>
        </h1>
        <p>
          Travel one of the world&apos;s most scenic rail journeys. Our segment-based booking
          lets you reserve a seat for exactly the distance you travel, with no more paying
          for empty legs.
        </p>

        <div className="station-selector">
          <div className="station-row">
            <div className="form-group">
              <label className="form-label">From Station</label>
              <select
                id="from-station"
                className="form-select"
                value={fromIdx}
                onChange={e => { setFromIdx(e.target.value); setError(''); }}
              >
                <option value="">Select origin...</option>
                {stations.map(s => (
                  <option key={s.id} value={s.sequenceIndex}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="station-arrow">→</div>

            <div className="form-group">
              <label className="form-label">To Station</label>
              <select
                id="to-station"
                className="form-select"
                value={toIdx}
                onChange={e => { setToIdx(e.target.value); setError(''); }}
              >
                <option value="">Select destination...</option>
                {stations.filter(s => fromIdx === '' || s.sequenceIndex > parseInt(fromIdx)).map(s => (
                  <option key={s.id} value={s.sequenceIndex}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {error && <p style={{ color: 'var(--accent-danger)', fontSize: '0.85rem', marginBottom: '1rem' }}>{error}</p>}

          <button
            id="search-seats"
            className="btn btn-primary btn-lg"
            style={{ width: '100%' }}
            onClick={handleSearch}
            disabled={!fromIdx || !toIdx}
          >
             Search Available Seats
          </button>
        </div>
      </section>

      {/* Features */}
      <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginTop: '4rem' }}>
        {[
          { icon: '', title: 'Segment-Based', desc: 'Pay only for the distance you travel. A seat freed mid-journey is resold instantly.' },
          { icon: '', title: 'Real-Time Booking', desc: 'See live seat availability and book instantly with guaranteed conflict-free reservations.' },
          { icon: '', title: 'Visual Seat Map', desc: 'Choose your exact seat from an interactive coach-by-coach seat map.' },
          { icon: '', title: 'Fair Pricing', desc: 'Distance-based fares mean you never subsidize empty seats you don\'t use.' },
        ].map((f, i) => (
          <div key={i} className="card" style={{ textAlign: 'center', padding: '2rem 1.5rem' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>{f.icon}</div>
            <h3 style={{ fontWeight: 600, marginBottom: '0.5rem' }}>{f.title}</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.6 }}>{f.desc}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
