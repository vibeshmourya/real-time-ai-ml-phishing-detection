import React, { useState, Suspense, lazy } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ScanProvider, useScan } from './context/ScanContext';
import { LanguageProvider, useLanguage } from './context/LanguageContext';
import { ThemeProvider } from './context/ThemeContext';
import Navbar from './components/Navbar';
import AuthModal from './components/AuthModal';
import ScanVerdictModal from './components/ScanVerdictModal';
import EmergencyWizard from './components/EmergencyWizard';
import ReportPhishModal from './components/ReportPhishModal';
import PraharChatbot from './components/PraharChatbot';
import DashboardPage from './pages/DashboardPage';

// Lazy-loaded routes for optimal initial bundle size and rapid Time-To-Interactive
const ScannerPage = lazy(() => import('./pages/ScannerPage'));
const ScanHistoryPage = lazy(() => import('./pages/ScanHistoryPage'));
const EmergencyPage = lazy(() => import('./pages/EmergencyPage'));
const AwarenessPage = lazy(() => import('./pages/AwarenessPage'));
const ReportPage = lazy(() => import('./pages/ReportPage'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const SettingsPage = lazy(() => import('./pages/SettingsPage'));
const AdminPortalPage = lazy(() => import('./pages/AdminPortalPage'));
const MyMoneyPage = lazy(() => import('./pages/MyMoneyPage'));
const SafetyCentrePage = lazy(() => import('./pages/SafetyCentrePage'));
const ChatAssistantPage = lazy(() => import('./pages/ChatAssistantPage'));
const DeviceSafetyPage = lazy(() => import('./pages/DeviceSafetyPage'));

import { Shield, ShieldAlert, CheckCircle2, AlertTriangle, PhoneCall, Globe, Lock } from 'lucide-react';

function RouteLoadingFallback() {
  return (
    <div className="flex items-center justify-center min-h-[50vh] text-slate-400">
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
        <span className="text-xs font-medium">Loading Prahar Module...</span>
      </div>
    </div>
  );
}

function AppContent() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const { toastMessage } = useScan();
  const { t } = useLanguage();

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-dark-950 text-slate-900 dark:text-slate-100 transition-colors duration-200">
      
      {/* Top Navbar */}
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

      {/* Main Content Area */}
      <main className="flex-1 w-full">
        <Suspense fallback={<RouteLoadingFallback />}>
          {activeTab === 'dashboard' && <DashboardPage setActiveTab={setActiveTab} />}
          {activeTab === 'scanner' && <ScannerPage />}
          {activeTab === 'money' && <MyMoneyPage setActiveTab={setActiveTab} />}
          {activeTab === 'safety' && <SafetyCentrePage setActiveTab={setActiveTab} />}
          {activeTab === 'chat' && <ChatAssistantPage setActiveTab={setActiveTab} />}
          {activeTab === 'device' && <DeviceSafetyPage setActiveTab={setActiveTab} />}
          {activeTab === 'history' && <ScanHistoryPage setActiveTab={setActiveTab} />}
          {activeTab === 'learn' && <AwarenessPage />}
          {activeTab === 'emergency' && <SafetyCentrePage setActiveTab={setActiveTab} />}
          {activeTab === 'report' && <ReportPage />}
          {activeTab === 'about' && <AboutPage />}
          {activeTab === 'settings' && <SettingsPage />}
          {activeTab === 'analyst' && <AdminPortalPage />}
        </Suspense>
      </main>

      {/* Global Interactive Modals */}
      <AuthModal />
      <ScanVerdictModal />
      <EmergencyWizard />
      <ReportPhishModal />
      <PraharChatbot />

      {/* Floating Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-5 duration-200">
          <div className={`px-4 py-3 rounded-2xl shadow-modal border flex items-center gap-3 text-xs font-bold ${
            toastMessage.type === 'safe'
              ? 'bg-emerald-50 border-emerald-300 text-emerald-900 dark:bg-emerald-950 dark:border-emerald-800 dark:text-emerald-200'
              : toastMessage.type === 'danger'
              ? 'bg-red-50 border-red-300 text-red-900 dark:bg-red-950 dark:border-red-800 dark:text-red-200'
              : toastMessage.type === 'warn'
              ? 'bg-amber-50 border-amber-300 text-amber-900 dark:bg-amber-950 dark:border-amber-800 dark:text-amber-200'
              : 'bg-white border-slate-200 text-slate-800 dark:bg-dark-900 dark:border-slate-700 dark:text-slate-200'
          }`}>
            {toastMessage.type === 'safe' && <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />}
            {toastMessage.type === 'danger' && <ShieldAlert className="w-4 h-4 text-red-600 dark:text-red-400" />}
            {toastMessage.type === 'warn' && <AlertTriangle className="w-4 h-4 text-amber-600 dark:text-amber-400" />}
            <span>{toastMessage.text}</span>
          </div>
        </div>
      )}

      {/* Trustworthy Footer */}
      <footer className="border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-dark-900 py-8 text-xs text-slate-500 dark:text-slate-400 transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-4">
          
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 rounded-lg bg-blue-700 flex items-center justify-center text-white">
                <Shield className="w-3.5 h-3.5" />
              </div>
              <span className="font-bold text-slate-800 dark:text-slate-200">{t('appName', 'Prahar')}</span>
              <span>— {t('appSubtitle', 'Real-Time Phishing Detection & Prevention')}</span>
            </div>

            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1.5 text-red-600 dark:text-red-400 font-semibold">
                <PhoneCall className="w-3.5 h-3.5" /> {t('emergency.helplineTitle', 'Cyber Fraud Helpline')}: <strong>1930</strong>
              </span>
              <span>•</span>
              <a 
                href="https://cybercrime.gov.in" 
                target="_blank" 
                rel="noopener noreferrer" 
                className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors flex items-center gap-1 font-medium"
              >
                <Globe className="w-3.5 h-3.5" /> cybercrime.gov.in
              </a>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100 dark:border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-2 text-[11px] text-slate-400">
            <div>
              © 2026 {t('appName', 'Prahar')}. {t('common.footerNote', 'Emergency cyber fraud reporting: Use official bank and government channels.')}
            </div>
            <div className="flex items-center gap-4">
              <button onClick={() => setActiveTab('about')} className="hover:underline">{t('nav.about', 'About')}</button>
              <button onClick={() => setActiveTab('learn')} className="hover:underline">{t('nav.learn', 'Learn')}</button>
              <button onClick={() => setActiveTab('emergency')} className="hover:underline">{t('nav.emergency', 'Emergency')}</button>
              <button onClick={() => setActiveTab('settings')} className="hover:underline">{t('nav.settings', 'Privacy & Settings')}</button>
            </div>
          </div>

        </div>
      </footer>

    </div>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
        <AuthProvider>
          <ScanProvider>
            <AppContent />
          </ScanProvider>
        </AuthProvider>
      </LanguageProvider>
    </ThemeProvider>
  );
}
