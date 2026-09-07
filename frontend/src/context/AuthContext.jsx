import React, { createContext, useContext, useState, useEffect } from 'react';
import client from '../api/client';

const AuthContext = createContext();

const DEMO_USERS = {
  USER: {
    id: 1,
    username: 'citizen_user',
    fullName: 'Rahul Sharma',
    email: 'rahul@example.com',
    role: 'ROLE_USER'
  },
  ANALYST: {
    id: 2,
    username: 'cyber_analyst',
    fullName: 'Dr. Ananya Roy (Security Lead)',
    email: 'ananya@cert-in.gov.in',
    role: 'ROLE_SECURITY_ANALYST'
  },
  ADMIN: {
    id: 3,
    username: 'phishshield_admin',
    fullName: 'System Administrator',
    email: 'admin@phishshield.gov.in',
    role: 'ROLE_ADMIN'
  }
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('phishshield_current_user');
    return saved ? JSON.parse(saved) : DEMO_USERS.USER;
  });
  const [token, setToken] = useState(() => localStorage.getItem('phishshield_token') || 'demo-jwt-token');

  useEffect(() => {
    if (user) {
      localStorage.setItem('phishshield_current_user', JSON.stringify(user));
    }
  }, [user]);

  const switchRole = (roleKey) => {
    if (DEMO_USERS[roleKey]) {
      setUser(DEMO_USERS[roleKey]);
    }
  };

  const login = async (usernameOrEmail, password) => {
    try {
      const res = await client.post('/auth/login', { usernameOrEmail, password });
      if (res.data?.data?.token) {
        setToken(res.data.data.token);
        localStorage.setItem('phishshield_token', res.data.data.token);
        setUser(res.data.data.user);
        return { success: true };
      }
    } catch {
      // Demo fallback login
      const matched = Object.values(DEMO_USERS).find(u => u.username === usernameOrEmail) || DEMO_USERS.USER;
      setUser(matched);
      return { success: true, isDemo: true };
    }
  };

  const logout = () => {
    setUser(DEMO_USERS.USER);
    setToken('');
    localStorage.removeItem('phishshield_token');
  };

  const isAnalystOrAdmin = user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_SECURITY_ANALYST';

  return (
    <AuthContext.Provider value={{ user, token, switchRole, login, logout, isAnalystOrAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
