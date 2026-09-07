import React, { useState } from 'react';
import { 
  ShieldAlert, PhoneCall, Globe, KeyRound, CreditCard, 
  Smartphone, FileWarning, CheckCircle2, Copy, Check, ExternalLink, HelpCircle
} from 'lucide-react';
import { useScan } from '../context/ScanContext';

const BANK_HOTLINES = [
  { bank: 'State Bank of India (SBI)', phone: '1800 1234 / 1800 2100', portal: 'onlinesbi.sbi' },
  { bank: 'HDFC Bank', phone: '1800 1600 / 1800 2600', portal: 'hdfcbank.com' },
  { bank: 'ICICI Bank', phone: '1800 1080', portal: 'icicibank.com' },
  { bank: 'Axis Bank', phone: '1800 419 5959', portal: 'axisbank.com' },
  { bank: 'Paytm Payments Bank', phone: '0120 4456 456', portal: 'paytmbank.com' }
];

export default function EmergencyPage() {
  const { showToast } = useScan();
  const [activeTab, setActiveTab] = useState('BANKING');
  const [completedSteps, setCompletedSteps] = useState({});
  const [copiedDraft, setCopiedDraft] = useState(false);

  const toggleStep = (id) => {
    setCompletedSteps(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const handleCopyDraft = () => {
    const draft = `FORMAL CYBERCRIME & FRAUD REPORT COMPLAINT
--------------------------------------------------
Timestamp: ${new Date().toLocaleString()}
Incident Type: Phishing & Credential Compromise
Immediate Helpline Contacted: 1930 (National Cyber Crime Helpline)
Portal: https://cybercrime.gov.in

Incident Description:
I was misled by an urgent phishing link/SMS and accidentally entered confidential credentials.
I request an immediate freeze of my compromised payment cards / NetBanking accounts and initiation of transaction tracing.

Generated via PhishShield Incident Playbook`;

    navigator.clipboard.writeText(draft);
    setCopiedDraft(true);
    showToast('Complaint draft copied to clipboard', 'info');
    setTimeout(() => setCopiedDraft(false), 2500);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Hero */}
      <div className="text-center max-w-2xl mx-auto">
        <span className="text-xs font-black uppercase tracking-widest bg-rose-500/20 text-rose-300 border border-rose-500/30 px-3 py-1 rounded-full">
          Immediate Incident Response
        </span>
        <h1 className="text-3xl sm:text-4xl font-black text-white mt-3 tracking-tight">
          "I Already Clicked the Link" Playbook
        </h1>
        <p className="text-sm text-slate-400 mt-2">
          Do not panic. Follow these rapid containment steps within the Golden Hour to prevent financial loss.
        </p>
      </div>

      {/* Primary 1930 Helpline Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-rose-950 via-dark-900 to-rose-950 border-2 border-rose-500/60 shadow-glow-danger flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-rose-600 text-white flex items-center justify-center shrink-0 shadow-lg">
            <PhoneCall className="w-8 h-8 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold uppercase tracking-wider text-rose-300 bg-rose-500/20 px-2 py-0.5 rounded border border-rose-500/30">
                Golden Hour Financial Freeze
              </span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-black text-white font-mono mt-1">
              Dial 1930
            </h2>
            <p className="text-xs text-rose-200">
              National Cyber Crime Reporting Helpline (Govt. of India) • 24/7 Toll-Free
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <a
            href="https://cybercrime.gov.in"
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 md:flex-initial flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow-md transition-all"
          >
            <Globe className="w-4 h-4" />
            <span>File Complaint on cybercrime.gov.in</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>

      {/* 4 Interactive Scenarios */}
      <div className="glass-panel p-6 sm:p-8 border border-slate-800">
        <h3 className="text-base font-bold text-white mb-4">
          Select Your Incident Scenario:
        </h3>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {[
            { id: 'BANKING', label: 'Card / Banking Data', icon: CreditCard },
            { id: 'PASSWORD', label: 'Password / Login', icon: KeyRound },
            { id: 'OTP', label: 'Shared OTP Code', icon: Smartphone },
            { id: 'APK', label: 'Downloaded Unknown APK', icon: FileWarning },
          ].map((tab) => {
            const Icon = tab.icon;
            const isSelected = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`p-3.5 rounded-xl border flex flex-col items-center text-center gap-2 transition-all ${
                  isSelected 
                    ? 'bg-rose-500/20 border-rose-500 text-white shadow-glow-danger' 
                    : 'bg-dark-950 border-slate-800 text-slate-400 hover:bg-slate-800 hover:text-white'
                }`}
              >
                <Icon className={`w-5 h-5 ${isSelected ? 'text-rose-400' : 'text-slate-400'}`} />
                <span className="text-xs font-bold">{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Action Steps Checklist */}
        <div className="space-y-3">
          {activeTab === 'BANKING' && (
            <>
              <ChecklistItem 
                id="b1" 
                title="1. Freeze your Debit/Credit Card instantly" 
                desc="Open your genuine Mobile Banking app > Manage Cards > Temporarily Switch Off Card & disable online transactions." 
                isDone={completedSteps['b1']} 
                onToggle={() => toggleStep('b1')} 
              />
              <ChecklistItem 
                id="b2" 
                title="2. Call Bank 24/7 Fraud Hotline" 
                desc="Contact customer care immediately to report unauthorized compromise and request hotlisting." 
                isDone={completedSteps['b2']} 
                onToggle={() => toggleStep('b2')} 
              />
              <ChecklistItem 
                id="b3" 
                title="3. Report on National Portal (cybercrime.gov.in / 1930)" 
                desc="Prompt reporting within 2 hours enables banking fraud coordinators to block money transfers in transit." 
                isDone={completedSteps['b3']} 
                onToggle={() => toggleStep('b3')} 
              />
            </>
          )}

          {activeTab === 'PASSWORD' && (
            <>
              <ChecklistItem 
                id="p1" 
                title="1. Manually navigate to official website & change password" 
                desc="Never use links from emails. Type the official URL directly and update your password immediately." 
                isDone={completedSteps['p1']} 
                onToggle={() => toggleStep('p1')} 
              />
              <ChecklistItem 
                id="p2" 
                title="2. Terminate all active sessions" 
                desc="In Security Settings, click 'Log out of all devices' to revoke any attacker sessions." 
                isDone={completedSteps['p2']} 
                onToggle={() => toggleStep('p2')} 
              />
              <ChecklistItem 
                id="p3" 
                title="3. Turn on Authenticator App 2-Factor Authentication" 
                desc="Add Google Authenticator or Microsoft Authenticator for hardware-bound security." 
                isDone={completedSteps['p3']} 
                onToggle={() => toggleStep('p3')} 
              />
            </>
          )}

          {activeTab === 'OTP' && (
            <>
              <ChecklistItem 
                id="o1" 
                title="1. Immediately request transaction rollback via Bank Hotline" 
                desc="Call your bank within minutes to alert them that the OTP was obtained fraudulently." 
                isDone={completedSteps['o1']} 
                onToggle={() => toggleStep('o1')} 
              />
              <ChecklistItem 
                id="o2" 
                title="2. Dial 1930 Cyber Fraud Helpline" 
                desc="State the transaction ID, debit amount, and recipient UPI ID to trigger financial freeze." 
                isDone={completedSteps['o2']} 
                onToggle={() => toggleStep('o2')} 
              />
            </>
          )}

          {activeTab === 'APK' && (
            <>
              <ChecklistItem 
                id="a1" 
                title="1. Switch ON Airplane Mode Immediately" 
                desc="Disconnect cellular data and Wi-Fi to stop the malicious app from uploading OTPs and data to command servers." 
                isDone={completedSteps['a1']} 
                onToggle={() => toggleStep('a1')} 
              />
              <ChecklistItem 
                id="a2" 
                title="2. Remove Device Admin Privileges & Uninstall" 
                desc="Go to Settings > Security > Device Admin Apps > Deactivate suspicious app, then uninstall." 
                isDone={completedSteps['a2']} 
                onToggle={() => toggleStep('a2')} 
              />
            </>
          )}
        </div>

        {/* Copy Draft Button */}
        <div className="mt-6 pt-5 border-t border-slate-800 flex justify-end">
          <button
            onClick={handleCopyDraft}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-bold transition-all"
          >
            {copiedDraft ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-cyber-cyan" />}
            <span>{copiedDraft ? 'Complaint Draft Copied!' : 'Copy Official Cyber Complaint Draft'}</span>
          </button>
        </div>

      </div>

      {/* Bank Hotlines Directory */}
      <div className="glass-panel p-6 border border-slate-800">
        <h3 className="text-base font-bold text-white mb-4">
          Major Bank Emergency Fraud Helplines:
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {BANK_HOTLINES.map((b, idx) => (
            <div key={idx} className="p-3.5 rounded-xl bg-dark-950 border border-slate-800 text-xs">
              <p className="font-bold text-white">{b.bank}</p>
              <p className="font-mono text-indigo-400 mt-1 font-semibold">📞 {b.phone}</p>
              <p className="text-slate-400 text-[11px] mt-0.5">🌐 {b.portal}</p>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}

function ChecklistItem({ id, title, desc, isDone, onToggle }) {
  return (
    <div
      onClick={onToggle}
      className={`p-4 rounded-xl border cursor-pointer transition-all flex items-start gap-3.5 ${
        isDone 
          ? 'bg-emerald-950/30 border-emerald-500/40 text-slate-300' 
          : 'bg-dark-950/90 border-slate-800 hover:border-slate-700 text-white'
      }`}
    >
      <button
        type="button"
        className={`mt-0.5 w-5 h-5 rounded-lg border flex items-center justify-center shrink-0 transition-all ${
          isDone 
            ? 'bg-emerald-500 border-emerald-400 text-slate-950 font-bold' 
            : 'border-slate-600 bg-slate-800'
        }`}
      >
        {isDone && <Check className="w-3.5 h-3.5" />}
      </button>

      <div>
        <h4 className={`text-xs font-bold ${isDone ? 'line-through text-emerald-300' : 'text-white'}`}>
          {title}
        </h4>
        <p className="text-xs text-slate-400 mt-1 leading-relaxed">
          {desc}
        </p>
      </div>
    </div>
  );
}
