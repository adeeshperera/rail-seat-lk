'use client';

import { useState, useEffect } from 'react';
import { api, AdminDashboard } from '@/lib/api';

// Dynamic import for recharts to avoid SSR issues
import dynamic from 'next/dynamic';

const BarChartComponent = dynamic(() => import('./AdminCharts'), { ssr: false });

export default function AdminPage() {
  const [data, setData] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchData = () => {
    api.getAdminDashboard()
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return <div className="loading-page"><div className="spinner"></div><p>Loading dashboard...</p></div>;
  }

  if (!data) {
    return <div className="loading-page"><p>Failed to load dashboard data. Is the backend running?</p></div>;
  }

  return (
    <div>
      <div className="section-header">
        <div>
          <h1 className="section-title"> Admin Dashboard</h1>
          <p className="section-subtitle">Real-time overview of bookings, occupancy, and revenue</p>
        </div>
        <button className="btn btn-secondary" onClick={fetchData}>
           Refresh
        </button>
      </div>

      {/* Stat Cards */}
      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-label">Total Bookings</div>
          <div className="stat-value blue">{data.totalBookings}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Total Revenue</div>
          <div className="stat-value green">LKR {data.totalRevenue.toLocaleString()}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Avg Occupancy</div>
          <div className="stat-value yellow">{data.averageOccupancy.toFixed(1)}%</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Waitlisted</div>
          <div className="stat-value purple">{data.totalWaitlisted}</div>
        </div>
      </div>

      {/* Occupancy by Segment */}
      <div className="chart-container">
        <h3 className="chart-title">Segment Occupancy</h3>
        {data.segmentOccupancies.map((seg, i) => {
          const color = seg.occupancyRate > 80 ? 'var(--accent-danger)'
            : seg.occupancyRate > 50 ? 'var(--accent-warning)'
            : 'var(--accent-success)';
          return (
            <div key={i} className="occupancy-bar-wrap">
              <div className="occupancy-label">
                <span style={{ color: 'var(--text-secondary)' }}>
                  {seg.fromStation} → {seg.toStation}
                </span>
                <span style={{ fontWeight: 600 }}>
                  {seg.occupiedSeats}/{seg.totalSeats} ({seg.occupancyRate.toFixed(1)}%)
                </span>
              </div>
              <div className="occupancy-bar">
                <div
                  className="occupancy-fill"
                  style={{ width: `${seg.occupancyRate}%`, background: color }}
                />
              </div>
            </div>
          );
        })}
      </div>

      {/* Revenue Chart */}
      <div className="chart-container">
        <h3 className="chart-title">Revenue by Segment</h3>
        <BarChartComponent segments={data.segmentOccupancies} />
      </div>

      {/* Recent Bookings */}
      <div className="chart-container">
        <h3 className="chart-title">Recent Bookings</h3>
        {data.recentBookings.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '2rem' }}>No bookings yet</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Passenger</th>
                  <th>Route</th>
                  <th>Coach/Seat</th>
                  <th>Fare</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {data.recentBookings.map(b => (
                  <tr key={b.id}>
                    <td>{b.passengerName}</td>
                    <td>{b.fromStation} → {b.toStation}</td>
                    <td>C{b.coachNumber} / S{b.seatNumber}</td>
                    <td style={{ color: 'var(--accent-success)' }}>LKR {b.fare.toLocaleString()}</td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                      {new Date(b.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
