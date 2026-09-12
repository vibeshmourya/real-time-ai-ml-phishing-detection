import React, { useState } from 'react';
import { 
  Shield, ShieldAlert, AlertTriangle, BookOpen, Clock, 
  User, Sun, Moon, Sparkles, LogIn, LogOut, Check, ChevronDown, 
  FileText, Activity, HelpCircle, Eye
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';

export default function Navbar({ activeTab, setActiveTab }) {
  const { user, isAuthenticated, logout, openAuthModal, isAnalystOrAdmin } = useAuth();
  const { openEmergencyPlaybook, setIsReportModalOpen } = useScan();
  const { lang, setLang, t, supportedLanguages } = useLanguage();
  const { theme, toggleTheme, simpleMode, toggleSimpleMode } = useTheme();

  const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState(false);

  const navItems = [
    { id: 'dashboard', label: t('nav.home', 'Home'), icon: Shield },
    { id: 'scanner', label: t('nav.scan', 'Scan'), icon: Sparkles },
    { id: 'history', label: t('nav.myScans', 'My Scans'), icon: Clock },
    { id: 'learn', label: t('nav.learn', 'Prahar Learn'), icon: BookOpen },
    { id: 'emergency', label: t('nav.emergency', 'Emergency Help'), icon: ShieldAlert, highlight: true },
    { id: 'report', label: t('nav.report', 'Report'), icon: AlertTriangle },
    { id: 'about', label: t('nav.about', 'About'), icon: HelpCircle }
  ];

  if (isAnalystOrAdmin) {
    navItems.push({ id: 'analyst', label: t('nav.securityCenter', 'Security Center'), icon: Activity });
  }

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-dark-900/95 backdrop-blur-md transition-colors duration-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">
          
          {/* Logo & Product Identity */}
          <div 
            onClick={() => setActiveTab('dashboard')} 
            className="flex items-center gap-3 cursor-pointer select-none group"
          >
            <div className="w-10 h-10 rounded-xl bg-blue-700 text-white flex items-center justify-center font-black shadow-sm group-hover:bg-blue-800 transition-colors">
              <Shield className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl sm:text-2xl font-black tracking-tight text-slate-900 dark:text-white font-sans">
                  Prahar
                </span>
                <span className="text-[10px] font-bold uppercase tracking-wider bg-blue-50 dark:bg-blue-950/80 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800/80 px-2 py-0.5 rounded-full">
                  Security
                </span>
              </div>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 font-medium hidden sm:block">
                {t('appSubtitle', 'Real-Time Phishing Detection & Prevention')}
              </p>
            </div>
          </div>

          {/* Center Navigation Links */}
          <nav className="hidden lg:flex items-center gap-1 p-1 rounded-xl bg-slate-100 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs font-semibold">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-lg transition-all ${
                    isActive
                      ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-sm font-bold'
                      : item.highlight
                      ? 'text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/40'
                      : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-white/60 dark:hover:bg-slate-800/50'
                  }`}
                >
                  <Icon className={`w-3.5 h-3.5 ${item.highlight && !isActive ? 'text-red-600' : ''}`} />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Right Action Bar */}
          <div className="flex items-center gap-2 sm:gap-3">
            
            {/* Language Selector */}
            <div className="relative">
              <button
                onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-200 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors border border-slate-200 dark:border-slate-700"
                title="Select Language"
              >
                <span>{supportedLanguages.find(l => l.code === lang)?.nativeName || 'English'}</span>
                <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
              </button>

              {isLangDropdownOpen && (
                <div className="absolute right-0 mt-1.5 w-32 bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-lg p-1 z-50 animate-in fade-in zoom-in-95">
                  {supportedLanguages.map((l) => (
                    <button
                      key={l.code}
                      onClick={() => {
                        setLang(l.code);
                        setIsLangDropdownOpen(false);
                      }}
                      className={`w-full text-left px-3 py-1.5 text-xs rounded-lg flex items-center justify-between transition-colors ${
                        lang === l.code 
                          ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 font-bold' 
                          : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                      }`}
                    >
                      <span>{l.nativeName}</span>
                      {lang === l.code && <Check className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Simple Mode / Accessibility Toggle */}
            <button
              onClick={toggleSimpleMode}
              className={`p-2 rounded-lg text-xs transition-colors border ${
                simpleMode 
                  ? 'bg-amber-100 border-amber-300 text-amber-800 font-bold' 
                  : 'bg-slate-100 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
              }`}
              title={simpleMode ? 'Disable Simple Mode' : 'Enable Simple Mode for Seniors & High Contrast'}
            >
              <Eye className="w-4 h-4" />
            </button>

            {/* Dark / Light Mode Toggle */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-lg text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors border border-slate-200 dark:border-slate-700"
              title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            >
              {theme === 'dark' ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4" />}
            </button>

            {/* I Already Clicked Emergency Button */}
            <button
              onClick={() => openEmergencyPlaybook('BANKING_PHISHING')}
              className="flex items-center gap-1.5 px-3 py-1.5 sm:px-3.5 sm:py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold transition-all shadow-sm active:scale-95 shrink-0"
              title="Immediate safety steps if you clicked a suspicious link"
            >
              <ShieldAlert className="w-4 h-4" />
              <span className="hidden sm:inline">I Already Clicked</span>
              <span className="sm:hidden">Help</span>
            </button>

            {/* User Account Button */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  onClick={() => setIsUserDropdownOpen(!isUserDropdownOpen)}
                  className="flex items-center gap-2 pl-2 pr-2.5 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 hover:border-slate-300 text-xs text-slate-700 dark:text-slate-200"
                >
                  <div className="w-6 h-6 rounded-lg bg-blue-700 text-white flex items-center justify-center font-bold text-xs">
                    {user?.fullName ? user.fullName[0].toUpperCase() : 'U'}
                  </div>
                  <span className="hidden md:inline font-semibold truncate max-w-[100px]">
                    {user?.username}
                  </span>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                </button>

                {isUserDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-52 bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-modal p-2 z-50 animate-in fade-in zoom-in-95">
                    <div className="px-3 py-2 border-b border-slate-100 dark:border-slate-800 mb-1">
                      <p className="text-xs font-bold text-slate-900 dark:text-white truncate">{user?.fullName}</p>
                      <p className="text-[10px] text-slate-500 dark:text-slate-400 font-mono truncate">{user?.email}</p>
                      <span className="inline-block mt-1 text-[10px] font-semibold uppercase px-2 py-0.5 rounded bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300">
                        {user?.role?.replace('ROLE_', '')}
                      </span>
                    </div>

                    <button
                      onClick={() => {
                        setActiveTab('history');
                        setIsUserDropdownOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg flex items-center gap-2"
                    >
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{t('nav.myScans', 'My Scans')}</span>
                    </button>

                    <button
                      onClick={() => {
                        setActiveTab('settings');
                        setIsUserDropdownOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg flex items-center gap-2"
                    >
                      <User className="w-3.5 h-3.5 text-slate-400" />
                      <span>{t('nav.settings', 'Settings')}</span>
                    </button>

                    {isAnalystOrAdmin && (
                      <button
                        onClick={() => {
                          setActiveTab('analyst');
                          setIsUserDropdownOpen(false);
                        }}
                        className="w-full text-left px-3 py-2 text-xs text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-lg flex items-center gap-2 font-semibold"
                      >
                        <Activity className="w-3.5 h-3.5 text-blue-600" />
                        <span>Security Center</span>
                      </button>
                    )}

                    <div className="border-t border-slate-100 dark:border-slate-800 my-1" />

                    <button
                      onClick={() => {
                        logout();
                        setIsUserDropdownOpen(false);
                      }}
                      className="w-full text-left px-3 py-2 text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-lg flex items-center gap-2 font-medium"
                    >
                      <LogOut className="w-3.5 h-3.5" />
                      <span>{t('nav.logout', 'Logout')}</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <button
                onClick={() => openAuthModal('login')}
                className="flex items-center gap-1.5 px-3 py-1.5 sm:px-3.5 sm:py-2 rounded-xl bg-blue-700 hover:bg-blue-800 text-white text-xs font-semibold shadow-sm transition-all active:scale-95"
              >
                <LogIn className="w-4 h-4" />
                <span className="hidden sm:inline">{t('nav.login', 'Sign In')}</span>
              </button>
            )}

          </div>
        </div>

        {/* Mobile Navigation Bar */}
        <div className="flex lg:hidden items-center justify-between py-2 border-t border-slate-200 dark:border-slate-800 overflow-x-auto gap-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                  isActive
                    ? 'bg-blue-700 text-white'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
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
