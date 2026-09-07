import React, { useState } from 'react';
import QuickScanner from '../components/QuickScanner';
import RecentScansTable from '../components/RecentScansTable';
import { Shield, ShieldCheck, ShieldAlert, Cpu, Lock, Terminal, CheckCircle2, Zap } from 'lucide-react';
import { useScan } from '../context/ScanContext';

export default function ScannerPage() {
  const { history } = useScan();

  const securityFeatures = [
    {
      title: 'Real-Time Heuristic Neural XAI',
      desc: 'Deep multi-layer parsing analyzing lexical entropy, suspicious free TLDs, and character homoglyphs.',
      icon: Cpu
    },
    {
      title: 'Server-Side Request Forgery (SSRF) Guard',
      desc: 'Prevents loopback & intranet DNS exfiltration attacks before inspecting live endpoint headers.',
      icon: Lock
    },
    {
      title: 'Plain-English Explainability',
      desc: 'Translates high-dimensional machine learning weights into actionable non-technical bullet points.',
      icon: ShieldCheck
    },
    {
      title: 'Multi-Input Vector Processing',
      desc: 'Inspect raw URLs, email MIME headers, SMS/WhatsApp texts, and shortened redirection links.',
      icon: Zap
    }
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Scanner Hero */}
      <div>
        <div className="text-center max-w-2xl mx-auto mb-6">
          <span className="text-xs font-bold uppercase tracking-widest bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 px-3 py-1 rounded-full">
            AI Phishing Inspection Engine
          </span>
          <h1 className="text-3xl sm:text-4xl font-black text-white mt-3 tracking-tight">
            Deep Threat Scanner & Inspector
          </h1>
          <p className="text-sm text-slate-400 mt-2">
            Paste suspicious links, emergency SMS alerts, or deceptive emails for millisecond threat classification.
          </p>
        </div>

        <QuickScanner />
      </div>

      {/* Security Engine Architecture Features */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {securityFeatures.map((feat, idx) => {
          const Icon = feat.icon;
          return (
            <div key={idx} className="glass-card p-4 border border-slate-800 space-y-2">
              <div className="w-8 h-8 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
                <Icon className="w-4 h-4" />
              </div>
              <h3 className="text-xs font-bold text-white">{feat.title}</h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">{feat.desc}</p>
            </div>
          );
        })}
      </div>

      {/* History table */}
      <div>
        <RecentScansTable />
      </div>

    </div>
  );
}
