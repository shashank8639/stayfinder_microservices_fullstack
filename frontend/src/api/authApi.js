import client from './axiosClient';

export function login(email, password) {
  return client.post('/api/auth/login', { email, password }).then((res) => res.data);
}

export function register(email, password) {
  return client.post('/api/auth/register', { email, password }).then((res) => res.data);
}

export function me() {
  return client.get('/api/auth/me').then((res) => res.data);
}
