import React, { useState } from 'react';
import { 
  User, Globe, Moon, Sun, Eye, Volume2, 
  Trash2, ShieldCheck, Lock, LogOut, CheckCircle2, AlertTriangle
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useScan } from '../context/ScanContext';

export default function SettingsPage() {
  const { user, isAuthenticated, logout, openAuthModal } = useAuth();
  const { lang, setLang, t, supportedLanguages } = useLanguage();
  const { theme, toggleTheme, simpleMode, toggleSimpleMode } = useTheme();
  const { clearHistory, history, showToast } = useScan();

  const [audioEnabled, setAudioEnabled] = useState(true);

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Header */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white">
          {t('nav.settings', 'Settings & Preferences')}
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">
          Customize your experience, accessibility preferences, and privacy controls.
        </p>
      </div>

      <div className="space-y-6">
        
        {/* 1. Account Section */}
        <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-card">
          <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <User className="w-5 h-5 text-blue-600" />
            Account Information
          </h2>

          {isAuthenticated ? (
            <div className="space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800">
                <div>
                  <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                    {user?.fullName}
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 font-mono">
                    {user?.email} • @{user?.username}
                  </p>
                  <span className="inline-block mt-1 text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300">
                    {user?.role?.replace('ROLE_', '')}
                  </span>
                </div>

                <button
                  onClick={logout}
                  className="prahar-btn-secondary text-xs text-red-600 dark:text-red-400 flex items-center gap-1.5 self-start sm:self-auto"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  <span>{t('nav.logout', 'Sign Out')}</span>
                </button>
              </div>
            </div>
          ) : (
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-bold text-slate-800 dark:text-slate-200">
                  You are currently using Prahar as a guest
                </p>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  Sign in to synchronize scan records and access analyst portals.
                </p>
              </div>
              <button
                onClick={() => openAuthModal('login')}
                className="prahar-btn-primary py-2 px-4 text-xs font-bold shrink-0"
              >
                Sign In
              </button>
            </div>
          )}
        </div>

        {/* 2. Preferences & Accessibility */}
        <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-card">
          <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Globe className="w-5 h-5 text-blue-600" />
            Display & Preferences
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            
            {/* Language Selector */}
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-2">
              <label className="font-bold text-slate-800 dark:text-slate-200 block">
                Preferred Language
              </label>
              <select
                value={lang}
                onChange={(e) => setLang(e.target.value)}
                className="w-full px-3 py-2 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs font-medium text-slate-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-600"
              >
                {supportedLanguages.map(l => (
                  <option key={l.code} value={l.code}>{l.nativeName} ({l.label})</option>
                ))}
              </select>
            </div>

            {/* Simple Mode Toggle */}
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
              <div>
                <span className="font-bold text-slate-800 dark:text-slate-200 block">
                  Simple Mode (Accessibility)
                </span>
                <span className="text-[11px] text-slate-500 dark:text-slate-400">
                  Larger fonts, simplified plain language, and high contrast for seniors.
                </span>
              </div>
              <button
                onClick={toggleSimpleMode}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${
                  simpleMode 
                    ? 'bg-amber-500 text-slate-950' 
                    : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                }`}
              >
                {simpleMode ? 'Enabled' : 'Disabled'}
              </button>
            </div>

            {/* Theme Toggle */}
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
              <div>
                <span className="font-bold text-slate-800 dark:text-slate-200 block">
                  Theme Appearance
                </span>
                <span className="text-[11px] text-slate-500 dark:text-slate-400">
                  Currently set to {theme === 'dark' ? 'Dark Mode' : 'Light Mode (Default)'}.
                </span>
              </div>
              <button
                onClick={toggleTheme}
                className="prahar-btn-secondary py-1.5 px-3 text-xs flex items-center gap-1"
              >
                {theme === 'dark' ? <Sun className="w-3.5 h-3.5 text-amber-400" /> : <Moon className="w-3.5 h-3.5" />}
                <span>{theme === 'dark' ? 'Light Mode' : 'Dark Mode'}</span>
              </button>
            </div>

            {/* Audio Warnings */}
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
              <div>
                <span className="font-bold text-slate-800 dark:text-slate-200 block">
                  Audio Speech Assistance
                </span>
                <span className="text-[11px] text-slate-500 dark:text-slate-400">
                  Voice reading for verdicts and prevention advice.
                </span>
              </div>
              <button
                onClick={() => setAudioEnabled(!audioEnabled)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${
                  audioEnabled 
                    ? 'bg-blue-600 text-white' 
                    : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                }`}
              >
                {audioEnabled ? 'Active' : 'Muted'}
              </button>
            </div>

          </div>
        </div>

        {/* 3. Privacy & History */}
        <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-card">
          <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Lock className="w-5 h-5 text-blue-600" />
            Privacy by Design
          </h2>

          <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-2 text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
            <p>
              <strong>Data Minimization:</strong> Prahar processes submitted URLs, text, and screenshots solely for extracting structural threat signals. Raw screenshots are not stored permanently.
            </p>
            <p>
              <strong>Local Storage:</strong> Your personal scan history is stored locally in your browser storage. You can delete individual scans or wipe your history at any time.
            </p>
          </div>

          <div className="flex items-center justify-between pt-2">
            <div>
              <span className="text-xs font-bold text-slate-800 dark:text-slate-200 block">
                Manage Stored Scans ({history.length} records)
              </span>
              <span className="text-[11px] text-slate-500">
                Permanently removes all scan logs and saved verdicts from this device.
              </span>
            </div>

            <button
              onClick={clearHistory}
              disabled={history.length === 0}
              className="prahar-btn-secondary text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 flex items-center gap-1.5 disabled:opacity-50"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Clear History</span>
            </button>
          </div>
        </div>

      </div>

    </div>
  );
}
