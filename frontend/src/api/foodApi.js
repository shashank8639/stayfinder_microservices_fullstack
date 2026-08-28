import client from './axiosClient';

export function hotelMenu(hotelId) {
  return client.get(`/api/food/hotels/${hotelId}/menu`).then((res) => res.data);
}

export function placeOrder(payload) {
  return client.post('/api/food/orders', payload).then((res) => res.data);
}

export function myOrders() {
  return client.get('/api/food/orders/my').then((res) => res.data);
}

export function hotelOrders(hotelId) {
  return client.get(`/api/food/hotel/${hotelId}/orders`).then((res) => res.data);
}

export function updateOrderStatus(orderId, status) {
  return client.put(`/api/food/orders/${orderId}/status`, { status }).then((res) => res.data);
}

export function createMenuItem(payload) {
  return client.post('/api/food/menu', payload).then((res) => res.data);
}

export function updateMenuItem(itemId, payload) {
  return client.put(`/api/food/menu/${itemId}`, payload).then((res) => res.data);
}

export function deleteMenuItem(itemId) {
  return client.delete(`/api/food/menu/${itemId}`);
}
