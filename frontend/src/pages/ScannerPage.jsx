import React from 'react';
import QuickScanner from '../components/QuickScanner';
import { ShieldCheck, Lock, Eye, AlertCircle } from 'lucide-react';

export default function ScannerPage() {
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white">
          Security Threat Scanner
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">
          Inspect website links, messages, emails, screenshots, or QR codes with Prahar’s explainable detection engine.
        </p>
      </div>

      <QuickScanner />

      {/* Safety Tips Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-4">
        <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-1.5 shadow-card text-xs">
          <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
            <Lock className="w-4 h-4 text-blue-600" />
            <span>Privacy Guarded</span>
          </div>
          <p className="text-slate-500 dark:text-slate-400 leading-relaxed">
            Content is analyzed for structural threat markers. Passwords or OTPs are never stored.
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-1.5 shadow-card text-xs">
          <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
            <Eye className="w-4 h-4 text-emerald-600" />
            <span>Explainable AI</span>
          </div>
          <p className="text-slate-500 dark:text-slate-400 leading-relaxed">
            Results explain in plain language why a web address or message is suspicious.
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-1.5 shadow-card text-xs">
          <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white">
            <AlertCircle className="w-4 h-4 text-amber-600" />
            <span>Multi-Channel Inspection</span>
          </div>
          <p className="text-slate-500 dark:text-slate-400 leading-relaxed">
            Covers URLs, SMS, WhatsApp text, email headers, screenshots, and decoded QR destinations.
          </p>
        </div>
      </div>

    </div>
  );
}
