import axios from 'axios';

export const TOKEN_KEY = 'stayfinder.token';
export const USER_KEY = 'stayfinder.user';

/**
 * Single Axios instance. Base URL is the Gateway, never a service port.
 * Token is stored in localStorage for the demo (XSS-vulnerable; production would use httpOnly cookies).
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

let onUnauthorized = () => {};

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    const isLoginOrRegister =
      url.includes('/api/auth/login') || url.includes('/api/auth/register');
    if (status === 401 && !isLoginOrRegister) {
      onUnauthorized();
    }
    return Promise.reject(error);
  }
);

export function apiErrorMessage(error) {
  const status = error.response?.status;
  const raw = error.response?.data?.message || '';
  const url = error.config?.url || '';
  const isLoginOrRegister =
    url.includes('/api/auth/login') || url.includes('/api/auth/register');
  if (status === 401 && isLoginOrRegister) {
    return 'Email or password is incorrect.';
  }
  if (status === 401) {
    return 'Please sign in to continue.';
  }
  if (status === 403) {
    return "You don't have permission to do that.";
  }
  if (status === 409) {
    return 'Those dates are no longer available for this room.';
  }
  if (status === 503 || /unavailable/i.test(raw)) {
    return 'That request could not be completed right now. Please try again in a moment.';
  }
  if (status === 400 && /CONFIRMED/i.test(raw)) {
    return 'Room service is available after your stay is confirmed.';
  }
  if (raw && raw.length < 120 && !/Feign|Kafka|JWT|Gateway|Exception/i.test(raw)) {
    return raw;
  }
  if (!error.response) {
    return 'Cannot reach StayFinder. Check that the app is running and try again.';
  }
  return 'Something went wrong. Please try again.';
}

export default client;
