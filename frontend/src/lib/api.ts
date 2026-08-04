const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface Station {
  id: number;
  name: string;
  sequenceIndex: number;
  distanceKm: number;
}

export interface SeatInfo {
  seatId: number;
  seatNumber: number;
  status: 'AVAILABLE' | 'BOOKED' | 'PARTIALLY_BOOKED';
}

export interface CoachAvailability {
  coachId: number;
  coachNumber: number;
  totalSeats: number;
  availableCount: number;
  seats: SeatInfo[];
}

export interface AvailabilityResponse {
  fromStationIdx: number;
  toStationIdx: number;
  fromStation: string;
  toStation: string;
  fare: number;
  coaches: CoachAvailability[];
}

export interface BookingResponse {
  id: string;
  seatId: number;
  seatNumber: number;
  coachNumber: number;
  fromStation: string;
  fromStationIdx: number;
  toStation: string;
  toStationIdx: number;
  passengerName: string;
  passengerEmail: string;
  fare: number;
  status: string;
  createdAt: string;
}

export interface WaitlistResponse {
  id: string;
  fromStationIdx: number;
  toStationIdx: number;
  fromStation: string;
  toStation: string;
  passengerName: string;
  status: string;
  positionInQueue: number;
  createdAt: string;
}

export interface SegmentOccupancy {
  fromStation: string;
  toStation: string;
  fromIdx: number;
  toIdx: number;
  occupiedSeats: number;
  totalSeats: number;
  occupancyRate: number;
}

export interface RecentBooking {
  id: string;
  passengerName: string;
  fromStation: string;
  toStation: string;
  seatNumber: number;
  coachNumber: number;
  fare: number;
  createdAt: string;
}

export interface AdminDashboard {
  totalBookings: number;
  totalRevenue: number;
  averageOccupancy: number;
  totalWaitlisted: number;
  segmentOccupancies: SegmentOccupancy[];
  recentBookings: RecentBooking[];
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: 'Request failed' }));
    throw { status: res.status, ...err } as ApiError;
  }
  return res.json();
}

export const api = {
  getStations: () => request<Station[]>('/api/stations'),

  getAvailability: (from: number, to: number) =>
    request<AvailabilityResponse>(`/api/availability?from=${from}&to=${to}`),

  createBooking: (data: {
    seatId: number;
    fromStationIdx: number;
    toStationIdx: number;
    passengerName: string;
    passengerEmail?: string;
  }) => request<BookingResponse>('/api/bookings', {
    method: 'POST',
    body: JSON.stringify(data),
  }),

  getBooking: (id: string) => request<BookingResponse>(`/api/bookings/${id}`),

  cancelBooking: (id: string) => request<BookingResponse>(`/api/bookings/${id}`, {
    method: 'DELETE',
  }),

  joinWaitlist: (data: {
    fromStationIdx: number;
    toStationIdx: number;
    passengerName: string;
    passengerEmail?: string;
    coachId?: number;
  }) => request<WaitlistResponse>('/api/waitlist', {
    method: 'POST',
    body: JSON.stringify(data),
  }),

  getWaitlistEntry: (id: string) => request<WaitlistResponse>(`/api/waitlist/${id}`),

  getAdminDashboard: () => request<AdminDashboard>('/api/admin/dashboard'),
};
