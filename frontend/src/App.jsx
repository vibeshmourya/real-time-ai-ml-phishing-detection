import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ScanProvider, useScan } from './context/ScanContext';
import { LanguageProvider } from './context/LanguageContext';
import { ThemeProvider } from './context/ThemeContext';
import Navbar from './components/Navbar';
import AuthModal from './components/AuthModal';
import ScanVerdictModal from './components/ScanVerdictModal';
import EmergencyWizard from './components/EmergencyWizard';
import ReportPhishModal from './components/ReportPhishModal';
import DashboardPage from './pages/DashboardPage';
import ScannerPage from './pages/ScannerPage';
import ScanHistoryPage from './pages/ScanHistoryPage';
import EmergencyPage from './pages/EmergencyPage';
import AwarenessPage from './pages/AwarenessPage';
import ReportPage from './pages/ReportPage';
import AboutPage from './pages/AboutPage';
import SettingsPage from './pages/SettingsPage';
import AdminPortalPage from './pages/AdminPortalPage';
import { Shield, ShieldAlert, CheckCircle2, AlertTriangle, PhoneCall, Globe, Lock } from 'lucide-react';

function AppContent() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const { toastMessage } = useScan();

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-dark-950 text-slate-900 dark:text-slate-100 transition-colors duration-200">
      
      {/* Top Navbar */}
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

      {/* Main Content Area */}
      <main className="flex-1 w-full">
        {activeTab === 'dashboard' && <DashboardPage setActiveTab={setActiveTab} />}
        {activeTab === 'scanner' && <ScannerPage />}
        {activeTab === 'history' && <ScanHistoryPage setActiveTab={setActiveTab} />}
        {activeTab === 'learn' && <AwarenessPage />}
        {activeTab === 'emergency' && <EmergencyPage />}
        {activeTab === 'report' && <ReportPage />}
        {activeTab === 'about' && <AboutPage />}
        {activeTab === 'settings' && <SettingsPage />}
        {activeTab === 'analyst' && <AdminPortalPage />}
      </main>

      {/* Global Interactive Modals */}
      <AuthModal />
      <ScanVerdictModal />
      <EmergencyWizard />
      <ReportPhishModal />

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
              <span className="font-bold text-slate-800 dark:text-slate-200">Prahar</span>
              <span>— Real-Time Phishing Detection & Prevention</span>
            </div>

            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1.5 text-red-600 dark:text-red-400 font-semibold">
                <PhoneCall className="w-3.5 h-3.5" /> Cyber Fraud Helpline: <strong>1930</strong>
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
              © 2026 Prahar. Emergency cyber fraud reporting: Use official bank and government channels.
            </div>
            <div className="flex items-center gap-4">
              <button onClick={() => setActiveTab('about')} className="hover:underline">About</button>
              <button onClick={() => setActiveTab('learn')} className="hover:underline">Learn</button>
              <button onClick={() => setActiveTab('emergency')} className="hover:underline">Emergency</button>
              <button onClick={() => setActiveTab('settings')} className="hover:underline">Privacy & Settings</button>
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
