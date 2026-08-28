import client from './axiosClient';

export function listHotels() {
  return client.get('/api/bookings/hotels').then((res) => res.data);
}

export function listRooms(hotelId) {
  return client.get(`/api/bookings/hotels/${hotelId}/rooms`).then((res) => res.data);
}

export function checkAvailability(roomId, checkIn, checkOut) {
  return client
    .get(`/api/bookings/rooms/${roomId}/availability`, { params: { checkIn, checkOut } })
    .then((res) => res.data);
}

export function createBooking(roomId, checkIn, checkOut, guestEmail) {
  return client.post('/api/bookings', { roomId, checkIn, checkOut, guestEmail }).then((res) => res.data);
}

export function myBookings() {
  return client.get('/api/bookings/my').then((res) => res.data);
}

export function hotelBookings(hotelId) {
  return client.get(`/api/bookings/hotel/${hotelId}`).then((res) => res.data);
}

export function confirmBooking(bookingId) {
  return client.post(`/api/bookings/${bookingId}/confirm`).then((res) => res.data);
}

export function confirmPayment(bookingId) {
  return client.post(`/api/bookings/${bookingId}/payment/confirm`).then((res) => res.data);
}

export function cancelBooking(bookingId) {
  return client.post(`/api/bookings/${bookingId}/cancel`).then((res) => res.data);
}

export function createHotel(payload) {
  return client.post('/api/bookings/hotels', payload).then((res) => res.data);
}

export function createRoom(payload) {
  return client.post('/api/bookings/rooms', payload).then((res) => res.data);
}

export function updateRoom(roomId, payload) {
  return client.put(`/api/bookings/rooms/${roomId}`, payload).then((res) => res.data);
}
