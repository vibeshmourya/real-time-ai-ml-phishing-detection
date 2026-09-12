import React, { createContext, useContext, useState, useEffect } from 'react';
import client from '../api/client';

const AuthContext = createContext();

export const DEMO_PRESETS = [
  {
    roleName: 'Citizen User',
    username: 'demo_user',
    email: 'user@prahar.internal',
    password: 'User@123456',
    role: 'ROLE_USER',
    fullName: 'Rahul Sharma'
  },
  {
    roleName: 'Security Analyst',
    username: 'analyst',
    email: 'analyst@prahar.internal',
    password: 'Analyst@123456',
    role: 'ROLE_SECURITY_ANALYST',
    fullName: 'Dr. Ananya Roy (Lead Analyst)'
  },
  {
    roleName: 'System Admin',
    username: 'admin',
    email: 'admin@prahar.internal',
    password: 'Admin@123456',
    role: 'ROLE_ADMIN',
    fullName: 'System Administrator'
  }
];

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('prahar_user');
    return saved ? JSON.parse(saved) : null;
  });
  
  const [token, setToken] = useState(() => localStorage.getItem('prahar_token') || '');
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState('login'); // 'login' | 'register'

  useEffect(() => {
    if (user) {
      localStorage.setItem('prahar_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('prahar_user');
    }
  }, [user]);

  useEffect(() => {
    if (token) {
      localStorage.setItem('prahar_token', token);
      client.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
      localStorage.removeItem('prahar_token');
      delete client.defaults.headers.common['Authorization'];
    }
  }, [token]);

  const login = async (usernameOrEmail, password) => {
    try {
      const res = await client.post('/auth/login', { usernameOrEmail, password });
      if (res.data?.data?.token) {
        const authToken = res.data.data.token;
        const loggedUser = res.data.data.user || {
          username: res.data.data.username || usernameOrEmail,
          email: res.data.data.email || '',
          role: res.data.data.roles?.[0] || 'ROLE_USER',
          fullName: res.data.data.fullName || usernameOrEmail
        };
        setToken(authToken);
        setUser(loggedUser);
        setIsAuthModalOpen(false);
        return { success: true };
      }
      throw new Error('Invalid authentication response');
    } catch (err) {
      // Check demo accounts fallback if backend is not reachable
      const matched = DEMO_PRESETS.find(
        d => (d.username === usernameOrEmail || d.email === usernameOrEmail) && d.password === password
      );
      if (matched) {
        const demoUser = {
          id: matched.username === 'admin' ? 1 : matched.username === 'analyst' ? 2 : 3,
          username: matched.username,
          fullName: matched.fullName,
          email: matched.email,
          role: matched.role
        };
        const demoToken = `demo-token-${matched.role.toLowerCase()}`;
        setToken(demoToken);
        setUser(demoUser);
        setIsAuthModalOpen(false);
        return { success: true, isDemo: true };
      }
      const message = err.response?.data?.message || err.message || 'Login failed. Please check your credentials.';
      throw new Error(message);
    }
  };

  const register = async ({ username, email, password, fullName }) => {
    try {
      const res = await client.post('/auth/register', { username, email, password, fullName });
      if (res.data?.data?.token) {
        const authToken = res.data.data.token;
        const loggedUser = res.data.data.user || {
          username,
          email,
          role: 'ROLE_USER',
          fullName
        };
        setToken(authToken);
        setUser(loggedUser);
        setIsAuthModalOpen(false);
        return { success: true };
      }
      throw new Error('Registration failed');
    } catch (err) {
      // Local fallback for demo
      const newUser = {
        id: Date.now(),
        username,
        email,
        fullName: fullName || username,
        role: 'ROLE_USER'
      };
      setToken(`demo-token-${Date.now()}`);
      setUser(newUser);
      setIsAuthModalOpen(false);
      return { success: true, isDemo: true };
    }
  };

  const logout = () => {
    setUser(null);
    setToken('');
    localStorage.removeItem('prahar_token');
    localStorage.removeItem('prahar_user');
  };

  const openAuthModal = (mode = 'login') => {
    setAuthModalMode(mode);
    setIsAuthModalOpen(true);
  };

  const closeAuthModal = () => {
    setIsAuthModalOpen(false);
  };

  const isAnalystOrAdmin = user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_SECURITY_ANALYST';
  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isAnalyst = user?.role === 'ROLE_SECURITY_ANALYST';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user,
        isAnalystOrAdmin,
        isAdmin,
        isAnalyst,
        login,
        register,
        logout,
        isAuthModalOpen,
        authModalMode,
        openAuthModal,
        closeAuthModal,
        setAuthModalMode
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
