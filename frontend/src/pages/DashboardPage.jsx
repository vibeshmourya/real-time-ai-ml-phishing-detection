import React from 'react';
import QuickScanner from '../components/QuickScanner';
import DashboardStats from '../components/DashboardStats';
import RecentScansTable from '../components/RecentScansTable';
import CyberAwarenessHub from '../components/CyberAwarenessHub';
import ThreatRadarBanner from '../components/ThreatRadarBanner';
import { ShieldAlert, ArrowRight, Sparkles, CheckCircle2, Shield } from 'lucide-react';
import { useScan } from '../context/ScanContext';

export default function DashboardPage({ setActiveTab }) {
  const { openEmergencyPlaybook } = useScan();

  return (
    <div className="w-full space-y-6 pb-12">
      
      {/* Live Threat Radar Banner */}
      <ThreatRadarBanner />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        
        {/* Concise Hero / Scanner Section */}
        <section className="pt-2">
          <QuickScanner />
        </section>

        {/* Emergency Triage Quick Strip */}
        <section className="w-full p-4 rounded-2xl bg-gradient-to-r from-rose-950/60 via-dark-900 to-indigo-950/50 border border-rose-500/30 flex flex-col sm:flex-row items-center justify-between gap-3 shadow-lg">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-rose-600/30 border border-rose-500/40 text-rose-400 flex items-center justify-center shrink-0">
              <ShieldAlert className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h4 className="text-xs sm:text-sm font-bold text-white flex items-center gap-1.5">
                Accidentally clicked a suspicious link or entered your OTP/Password?
              </h4>
              <p className="text-[11px] text-slate-400">
                Launch our 3-step Golden Hour containment playbook to freeze accounts & dial 1930 Cyber Helpline.
              </p>
            </div>
          </div>

          <button
            onClick={() => openEmergencyPlaybook('BANKING_PHISHING')}
            className="w-full sm:w-auto px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-black shadow-glow-danger flex items-center justify-center gap-1.5 transition-all shrink-0 active:scale-95"
          >
            <span>Open Emergency Playbook</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </section>

        {/* Concise Security Metrics & Status */}
        <section>
          <DashboardStats />
        </section>

        {/* Dual-Column Activity & Micro-Guides */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* Left Column: Recent Scans History (7 cols) */}
          <div className="lg:col-span-7 space-y-6">
            <RecentScansTable />
          </div>

          {/* Right Column: 30s Threat Academy Hub (5 cols) */}
          <div className="lg:col-span-5 space-y-6">
            <CyberAwarenessHub />
          </div>

        </section>

      </div>
    </div>
  );
}
