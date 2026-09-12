import React, { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext();

export function ThemeProvider({ children }) {
  // Light mode is default as per requirements
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('prahar_theme') || 'light';
  });

  // Simple Mode for accessibility & senior citizens
  const [simpleMode, setSimpleMode] = useState(() => {
    return localStorage.getItem('prahar_simple_mode') === 'true';
  });

  useEffect(() => {
    localStorage.setItem('prahar_theme', theme);
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [theme]);

  useEffect(() => {
    localStorage.setItem('prahar_simple_mode', simpleMode.toString());
    const root = document.documentElement;
    if (simpleMode) {
      root.classList.add('simple-mode');
    } else {
      root.classList.remove('simple-mode');
    }
  }, [simpleMode]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  };

  const toggleSimpleMode = () => {
    setSimpleMode(prev => !prev);
  };

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggleTheme, simpleMode, setSimpleMode, toggleSimpleMode }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}
