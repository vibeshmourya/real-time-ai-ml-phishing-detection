import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import en from '../locales/en.json';
import hi from '../locales/hi.json';
import kn from '../locales/kn.json';

const LanguageContext = createContext();

const translations = {
  en,
  hi,
  kn
};

export const SUPPORTED_LANGUAGES = [
  { code: 'en', label: 'English', nativeName: 'English' },
  { code: 'hi', label: 'Hindi', nativeName: 'हिन्दी' },
  { code: 'kn', label: 'Kannada', nativeName: 'ಕನ್ನಡ' }
];

const VALID_CODES = new Set(SUPPORTED_LANGUAGES.map(l => l.code));
const STORAGE_KEY = 'prahar_language';

/**
 * Validates language code against supported set; defaults to 'en'
 */
function sanitizeLanguage(code) {
  return code && VALID_CODES.has(code) ? code : 'en';
}

export function LanguageProvider({ children }) {
  const [lang, setLangState] = useState(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return sanitizeLanguage(stored);
    } catch {
      return 'en';
    }
  });

  const setLang = useCallback((newCode) => {
    const sanitized = sanitizeLanguage(newCode);
    setLangState(sanitized);
    try {
      localStorage.setItem(STORAGE_KEY, sanitized);
    } catch {
      // Ignore storage write issues in private mode
    }
    if (typeof document !== 'undefined') {
      document.documentElement.lang = sanitized;
    }
  }, []);

  // Sync with localStorage changes across tabs/windows
  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = lang;
    }

    const handleStorageChange = (e) => {
      if (e.key === STORAGE_KEY && e.newValue) {
        const sanitized = sanitizeLanguage(e.newValue);
        setLangState(sanitized);
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [lang]);

  /**
   * Universal translation helper:
   * - t('nav.home') -> "Home" (en) / "होम" (hi) / "ಮುಖಪುಟ" (kn)
   * - t('dashboard.viewAllScans', { count: 3 }) -> "View all scans (3)"
   * - t('missing.key', 'Fallback text') -> "Fallback text"
   */
  const t = useCallback((keyPath, paramsOrFallback = '', maybeFallback = '') => {
    if (!keyPath) return '';

    let params = null;
    let fallback = '';

    if (typeof paramsOrFallback === 'object' && paramsOrFallback !== null) {
      params = paramsOrFallback;
      fallback = maybeFallback || '';
    } else if (typeof paramsOrFallback === 'string') {
      fallback = paramsOrFallback;
    }

    const keys = keyPath.split('.');

    // Helper to traverse an object given keys
    const traverse = (obj) => {
      let curr = obj;
      for (const k of keys) {
        if (curr && typeof curr === 'object' && k in curr) {
          curr = curr[k];
        } else {
          return null;
        }
      }
      return typeof curr === 'string' ? curr : null;
    };

    // 1. Try active language
    let result = traverse(translations[lang]);

    // 2. Fall back to English if missing in active language
    if (result === null && lang !== 'en') {
      result = traverse(translations.en);
    }

    // 3. Fall back to provided fallback or keyPath
    if (result === null) {
      result = fallback || keyPath;
    }

    // 4. Interpolate variables if params provided: e.g. {count}
    if (params && typeof result === 'string') {
      for (const [pKey, pVal] of Object.entries(params)) {
        result = result.replaceAll(`{${pKey}}`, String(pVal));
      }
    }

    return result;
  }, [lang]);

  const value = {
    lang,
    setLang,
    t,
    supportedLanguages: SUPPORTED_LANGUAGES
  };

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
}
