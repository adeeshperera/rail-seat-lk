'use client';

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { SegmentOccupancy } from '@/lib/api';

interface Props {
  segments: SegmentOccupancy[];
}

export default function AdminCharts({ segments }: Props) {
  const chartData = segments.map(seg => ({
    name: `${seg.fromStation.split(' ')[0]}→${seg.toStation.split(' ')[0]}`,
    occupancy: seg.occupancyRate,
    occupied: seg.occupiedSeats,
    total: seg.totalSeats,
  }));

  const getColor = (rate: number) => {
    if (rate > 80) return '#f87171';
    if (rate > 50) return '#fbbf24';
    return '#34d399';
  };

  return (
    <div style={{ width: '100%', height: 300 }}>
      <ResponsiveContainer>
        <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 40 }}>
          <XAxis
            dataKey="name"
            tick={{ fill: '#94a3b8', fontSize: 11 }}
            angle={-30}
            textAnchor="end"
            height={60}
          />
          <YAxis
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            domain={[0, 100]}
            tickFormatter={v => `${v}%`}
          />
          <Tooltip
            contentStyle={{
              background: '#1a2235',
              border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: 8,
              color: '#f1f5f9',
            }}
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            formatter={(value: any) => [`${Number(value).toFixed(1)}%`, 'Occupancy']}
          />
          <Bar dataKey="occupancy" radius={[4, 4, 0, 0]}>
            {chartData.map((entry, i) => (
              <Cell key={i} fill={getColor(entry.occupancy)} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
