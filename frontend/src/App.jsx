import React, { useState } from 'react';
import { AuthProvider } from './context/AuthContext';
import { ScanProvider, useScan } from './context/ScanContext';
import Navbar from './components/Navbar';
import ScanVerdictModal from './components/ScanVerdictModal';
import EmergencyWizard from './components/EmergencyWizard';
import ReportPhishModal from './components/ReportPhishModal';
import DashboardPage from './pages/DashboardPage';
import ScannerPage from './pages/ScannerPage';
import EmergencyPage from './pages/EmergencyPage';
import AwarenessPage from './pages/AwarenessPage';
import AdminPortalPage from './pages/AdminPortalPage';
import { Shield, ShieldAlert, CheckCircle2, AlertTriangle, PhoneCall, Globe, Heart } from 'lucide-react';

function AppContent() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const { toastMessage, isEmergencyOpen, openEmergencyPlaybook } = useScan();

  return (
    <div className="min-h-screen flex flex-col bg-dark-950 text-slate-100 relative">
      
      {/* Top Navbar */}
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

      {/* Main Content Area */}
      <main className="flex-1 w-full">
        {activeTab === 'dashboard' && <DashboardPage setActiveTab={setActiveTab} />}
        {activeTab === 'scanner' && <ScannerPage />}
        {activeTab === 'emergency' && <EmergencyPage />}
        {activeTab === 'awareness' && <AwarenessPage />}
        {activeTab === 'analyst' && <AdminPortalPage />}
      </main>

      {/* Global Interactive Modals */}
      <ScanVerdictModal />
      <EmergencyWizard />
      <ReportPhishModal />

      {/* Floating Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-5 duration-300">
          <div className={`px-4 py-3 rounded-2xl shadow-2xl border flex items-center gap-3 backdrop-blur-xl text-xs font-bold ${
            toastMessage.type === 'safe'
              ? 'bg-emerald-950/90 border-emerald-500/60 text-emerald-200 shadow-glow-safe'
              : toastMessage.type === 'danger'
              ? 'bg-rose-950/90 border-rose-500/60 text-rose-200 shadow-glow-danger'
              : toastMessage.type === 'warn'
              ? 'bg-amber-950/90 border-amber-500/60 text-amber-200 shadow-glow-warn'
              : 'bg-dark-900/90 border-slate-700 text-slate-200'
          }`}>
            {toastMessage.type === 'safe' && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
            {toastMessage.type === 'danger' && <ShieldAlert className="w-4 h-4 text-rose-400 animate-pulse" />}
            {toastMessage.type === 'warn' && <AlertTriangle className="w-4 h-4 text-amber-400" />}
            <span>{toastMessage.text}</span>
          </div>
        </div>
      )}

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-dark-950 py-8 text-xs text-slate-400">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-indigo-600 flex items-center justify-center text-white">
              <Shield className="w-3.5 h-3.5" />
            </div>
            <span className="font-bold text-slate-300">PhishShield AI</span>
            <span>— Real-Time Explainable Threat Protection Platform</span>
          </div>

          <div className="flex items-center gap-4 text-slate-400">
            <span className="flex items-center gap-1 text-rose-300">
              <PhoneCall className="w-3.5 h-3.5" /> Helpline: <strong>1930</strong>
            </span>
            <span>•</span>
            <a 
              href="https://cybercrime.gov.in" 
              target="_blank" 
              rel="noopener noreferrer" 
              className="hover:text-indigo-300 transition-colors flex items-center gap-1"
            >
              <Globe className="w-3.5 h-3.5" /> cybercrime.gov.in
            </a>
          </div>
        </div>
      </footer>

    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ScanProvider>
        <AppContent />
      </ScanProvider>
    </AuthProvider>
  );
}
