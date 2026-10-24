import React, { useState } from 'react';
import { 
  Shield, ShieldAlert, BookOpen, Clock, DollarSign, Smartphone, Bot, Search,
  User, Sun, Moon, LogIn, LogOut, Check, ChevronDown, Activity, Sparkles
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import NotificationBell from './NotificationBell';

export default function Navbar({ activeTab, setActiveTab }) {
  const { user, isAuthenticated, logout, openAuthModal, isAnalystOrAdmin } = useAuth();
  const { lang, setLang, supportedLanguages, t } = useLanguage();
  const { theme, toggleTheme } = useTheme();

  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState(false);

  // Core navigation for Prahar 2.0 with full dynamic i18n support
  const navItems = [
    { id: 'dashboard', label: t('nav.home', 'Home'), icon: Shield },
    { id: 'scanner', label: t('nav.scan', 'Scan'), icon: Search },
    { id: 'money', label: t('nav.money', 'My Money'), icon: DollarSign },
    { id: 'safety', label: t('nav.safety', 'Safety Centre'), icon: ShieldAlert, highlight: true },
    { id: 'chat', label: t('nav.chat', 'Ask Prahar'), icon: Bot },
    { id: 'device', label: t('nav.device', 'Device Safety'), icon: Smartphone },
    { id: 'learn', label: t('nav.learn', 'Learn'), icon: BookOpen }
  ];

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200/90 dark:border-slate-800 bg-white/95 dark:bg-dark-900/95 backdrop-blur-md transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-18">
          
          {/* Logo, Name & Tagline */}
          <div 
            onClick={() => setActiveTab('dashboard')} 
            className="flex items-center gap-2.5 cursor-pointer select-none group"
          >
            <div className="w-9 h-9 rounded-xl bg-blue-700 text-white flex items-center justify-center font-black shadow-xs group-hover:bg-blue-800 transition-colors">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="text-xl font-black tracking-tight text-slate-900 dark:text-white font-sans">
                  {t('appName', 'Prahar')}
                </span>
              </div>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 font-medium leading-none hidden sm:block">
                {t('tagline', 'Detect. Understand. Stay Safe.')}
              </p>
            </div>
          </div>

          {/* Minimal Navigation: Home, Scan History, Learn, and Help */}
          <nav className="hidden md:flex items-center gap-1 p-1 rounded-xl bg-slate-100 dark:bg-dark-950 border border-slate-200/80 dark:border-slate-800 text-xs font-semibold">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setActiveTab(item.id)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
                    isActive
                      ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-xs font-bold'
                      : item.highlight
                      ? 'text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/40'
                      : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Right Controls: Theme, Language, Profile/Sign In */}
          <div className="flex items-center gap-2">
            
            {/* Language Selector */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-medium text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
                title="Select language"
              >
                <span>{supportedLanguages.find(l => l.code === lang)?.nativeName || 'EN'}</span>
                <ChevronDown className="w-3 h-3 text-slate-400" />
              </button>

              {isLangDropdownOpen && (
                <div className="absolute right-0 mt-1.5 w-28 bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-lg p-1 z-50 animate-in fade-in">
                  {supportedLanguages.map((l) => (
                    <button
                      key={l.code}
                      type="button"
                      onClick={() => {
                        setLang(l.code);
                        setIsLangDropdownOpen(false);
                      }}
                      className={`w-full text-left px-2.5 py-1.5 text-xs rounded-lg flex items-center justify-between transition-colors ${
                        lang === l.code 
                          ? 'bg-blue-50 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 font-bold' 
                          : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                      }`}
                    >
                      <span>{l.nativeName}</span>
                      {lang === l.code && <Check className="w-3 h-3 text-blue-600" />}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Dark / Light Mode Toggle */}
            <button
              type="button"
              onClick={toggleTheme}
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
              title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4" />}
            </button>

            {/* Real-time Security Notification Bell */}
            <NotificationBell setActiveTab={setActiveTab} />

            {/* User Profile / Login */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setIsUserDropdownOpen(!isUserDropdownOpen)}
                  className="flex items-center gap-1.5 pl-1.5 pr-2.5 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs text-slate-700 dark:text-slate-200 transition-colors"
                >
                  <div className="w-6 h-6 rounded-lg bg-blue-700 text-white flex items-center justify-center font-bold text-xs">
                    {user?.fullName ? user.fullName[0].toUpperCase() : 'U'}
                  </div>
                  <span className="hidden sm:inline font-semibold max-w-[80px] truncate">
                    {user?.username}
                  </span>
                  <ChevronDown className="w-3 h-3 text-slate-400" />
                </button>

                {isUserDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-xl p-1.5 z-50 animate-in fade-in">
                    <div className="px-2.5 py-2 border-b border-slate-100 dark:border-slate-800 mb-1">
                      <p className="text-xs font-bold text-slate-900 dark:text-white truncate">{user?.fullName}</p>
                      <p className="text-[10px] text-slate-400 truncate">{user?.email}</p>
                    </div>

                    <button
                      type="button"
                      onClick={() => {
                        setActiveTab('history');
                        setIsUserDropdownOpen(false);
                      }}
                      className="w-full text-left px-2.5 py-1.5 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg flex items-center gap-2"
                    >
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{t('nav.myScans', 'Scan History')}</span>
                    </button>

                    {isAnalystOrAdmin && (
                      <button
                        type="button"
                        onClick={() => {
                          setActiveTab('analyst');
                          setIsUserDropdownOpen(false);
                        }}
                        className="w-full text-left px-2.5 py-1.5 text-xs text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-lg flex items-center gap-2 font-semibold"
                      >
                        <Activity className="w-3.5 h-3.5 text-blue-600" />
                        <span>{t('nav.securityCenter', 'Security Center')}</span>
                      </button>
                    )}

                    <div className="border-t border-slate-100 dark:border-slate-800 my-1" />

                    <button
                      type="button"
                      onClick={() => {
                        logout();
                        setIsUserDropdownOpen(false);
                      }}
                      className="w-full text-left px-2.5 py-1.5 text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg flex items-center gap-2"
                    >
                      <LogOut className="w-3.5 h-3.5" />
                      <span>{t('nav.logout', 'Logout')}</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <button
                type="button"
                onClick={() => openAuthModal('login')}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-700 hover:bg-blue-800 text-white text-xs font-semibold shadow-xs transition-all active:scale-95"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>{t('nav.login', 'Sign In')}</span>
              </button>
            )}

          </div>
        </div>

        {/* Mobile Navigation Row */}
        <div className="flex md:hidden items-center justify-around py-2 border-t border-slate-100 dark:border-slate-800">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors ${
                  isActive
                    ? 'bg-blue-700 text-white'
                    : item.highlight
                    ? 'text-red-600'
                    : 'text-slate-600 dark:text-slate-400'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>

      </div>
    </header>
  );
}
