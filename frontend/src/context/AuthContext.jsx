import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { login as loginApi, register as registerApi } from '../api/authApi';
import { setUnauthorizedHandler, TOKEN_KEY, USER_KEY } from '../api/axiosClient';

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(readStoredUser);

  const persist = (accessToken, nextUser) => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setToken(accessToken);
    setUser(nextUser);
  };

  const clear = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  };

  useEffect(() => {
    setUnauthorizedHandler(clear);
    return () => setUnauthorizedHandler(() => {});
  }, []);

  const login = async (email, password) => {
    const data = await loginApi(email, password);
    persist(data.accessToken, data.user);
    return data.user;
  };

  const register = async (email, password) => {
    const data = await registerApi(email, password);
    persist(data.accessToken, data.user);
    return data.user;
  };

  const value = useMemo(() => {
    const roles = user?.roles || [];
    return {
      token,
      user,
      login,
      register,
      logout: clear,
      isAuthenticated: Boolean(token),
      isCustomer: roles.includes('CUSTOMER'),
      isStaff: roles.includes('ADMIN') || roles.includes('HOTEL_OWNER'),
    };
  }, [token, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return ctx;
}
