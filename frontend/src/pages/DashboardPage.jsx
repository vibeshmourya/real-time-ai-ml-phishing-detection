import React from 'react';
import QuickScanner from '../components/QuickScanner';
import DashboardStats from '../components/DashboardStats';
import RecentScansTable from '../components/RecentScansTable';
import ThreatRadarBanner from '../components/ThreatRadarBanner';
import { 
  ShieldAlert, ArrowRight, ShieldCheck, Link2, 
  MessageSquare, Mail, Image as ImageIcon, QrCode, BookOpen
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

export default function DashboardPage({ setActiveTab }) {
  const { openEmergencyPlaybook } = useScan();
  const { t } = useLanguage();

  return (
    <div className="w-full space-y-6 pb-12">
      
      {/* Security Alerts Banner */}
      <ThreatRadarBanner />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8 pt-4">
        
        {/* Hero Section */}
        <div className="text-center max-w-2xl mx-auto space-y-2">
          <h1 className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-white tracking-tight">
            {t('hero.title', 'Stay Safe Online with Prahar')}
          </h1>
          <p className="text-sm sm:text-base text-slate-600 dark:text-slate-400 leading-relaxed">
            {t('hero.subtitle', 'Check a suspicious link, message, email, screenshot or QR code before you trust it.')}
          </p>
        </div>

        {/* Primary Multi-Modal Scanner */}
        <section>
          <QuickScanner />
        </section>

        {/* Emergency Help Banner */}
        <section className="p-5 rounded-2xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-sm">
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-red-600 text-white flex items-center justify-center shrink-0">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-red-950 dark:text-red-100">
                Accidentally entered your password, OTP, or banking information?
              </h4>
              <p className="text-xs text-red-800 dark:text-red-300 mt-0.5">
                Launch the immediate containment playbook to protect your accounts and freeze cards.
              </p>
            </div>
          </div>

          <button
            onClick={() => openEmergencyPlaybook('BANKING_PHISHING')}
            className="w-full sm:w-auto prahar-btn-danger py-2 px-4 text-xs font-bold shrink-0"
          >
            <span>Open Emergency Help</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </section>

        {/* User Activity Stats */}
        <section>
          <DashboardStats />
        </section>

        {/* Dual-Column Activity & Learn Hub */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* Left Column: Recent Scans (7 cols) */}
          <div className="lg:col-span-7">
            <RecentScansTable setActiveTab={setActiveTab} />
          </div>

          {/* Right Column: Quick Learn Card (5 cols) */}
          <div className="lg:col-span-5 p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-card">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-blue-600" />
                Prahar Learn Highlights
              </h3>
              <button
                onClick={() => setActiveTab('learn')}
                className="text-xs font-bold text-blue-700 dark:text-blue-400 hover:underline"
              >
                All Guides →
              </button>
            </div>

            <div className="space-y-2.5 text-xs">
              <div
                onClick={() => setActiveTab('learn')}
                className="p-3.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 hover:border-slate-300 cursor-pointer transition-all"
              >
                <span className="font-bold text-slate-900 dark:text-white block mb-0.5">
                  🏦 Banking Phishing & Lookalike Portals
                </span>
                <p className="text-slate-500 dark:text-slate-400 text-[11px]">
                  How scammers disguise web addresses to capture customer NetBanking credentials.
                </p>
              </div>

              <div
                onClick={() => setActiveTab('learn')}
                className="p-3.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 hover:border-slate-300 cursor-pointer transition-all"
              >
                <span className="font-bold text-slate-900 dark:text-white block mb-0.5">
                  🪪 Fake KYC & SIM Deactivation Threats
                </span>
                <p className="text-slate-500 dark:text-slate-400 text-[11px]">
                  Why banks and telecoms never ask for KYC updates via unofficial SMS links.
                </p>
              </div>

              <div
                onClick={() => setActiveTab('learn')}
                className="p-3.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 hover:border-slate-300 cursor-pointer transition-all"
              >
                <span className="font-bold text-slate-900 dark:text-white block mb-0.5">
                  📷 QR Code Scams: Golden Rule
                </span>
                <p className="text-slate-500 dark:text-slate-400 text-[11px]">
                  Remember: Entering your UPI PIN is NEVER required to receive money.
                </p>
              </div>
            </div>
          </div>

        </section>

      </div>
    </div>
  );
}
