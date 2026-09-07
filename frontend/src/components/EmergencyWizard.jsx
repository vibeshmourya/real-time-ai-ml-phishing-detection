import React, { useState } from 'react';
import { 
  ShieldAlert, AlertOctagon, PhoneCall, Globe, KeyRound, CreditCard, 
  Smartphone, FileWarning, CheckCircle2, ChevronRight, ArrowLeft, Copy, Check, X, ExternalLink
} from 'lucide-react';
import { useScan } from '../context/ScanContext';

const COMPROMISE_SCENARIOS = [
  {
    id: 'PASSWORD',
    title: 'Entered Username & Password',
    icon: KeyRound,
    desc: 'You typed your login credentials into the fake website.',
    urgency: 'HIGH',
    steps: [
      {
        title: 'Change Password Immediately',
        detail: 'Open a new tab, navigate directly to the OFFICIAL website (e.g. netbanking.bank.com) and change your password.',
        action: 'Manual Navigation'
      },
      {
        title: 'Revoke Active Sessions',
        detail: 'Look for "Security Settings > Log out of all active sessions & devices" to immediately kick the attacker out.',
        action: 'Account Settings'
      },
      {
        title: 'Enable Two-Factor Authentication (2FA)',
        detail: 'Set up Google Authenticator or Microsoft Authenticator app (avoid SMS 2FA if possible).',
        action: 'Enable 2FA'
      }
    ]
  },
  {
    id: 'BANKING',
    title: 'Entered Debit / Credit Card or Bank Details',
    icon: CreditCard,
    desc: 'You provided card numbers, CVV, expiry date, or NetBanking PIN.',
    urgency: 'CRITICAL',
    steps: [
      {
        title: 'Immediately Freeze / Block Your Card',
        detail: 'Open your genuine Mobile Banking app and switch off International & Online transactions, or block the card entirely.',
        action: 'Bank App Toggle'
      },
      {
        title: 'Call Your Bank Fraud Hotline (24/7)',
        detail: 'Call the customer care number on the back of your physical card or bank portal to report unauthorized exposure.',
        action: 'Call Bank Hotline'
      },
      {
        title: 'Report on National Cyber Crime Portal',
        detail: 'Dial 1930 immediately (Golden Hour reporting) or file a complaint on cybercrime.gov.in.',
        action: 'Dial 1930 / cybercrime.gov.in'
      }
    ]
  },
  {
    id: 'OTP',
    title: 'Shared a One-Time Password (OTP)',
    icon: Smartphone,
    desc: 'You provided an SMS or WhatsApp OTP to the scammer.',
    urgency: 'CRITICAL',
    steps: [
      {
        title: 'Immediate Bank Account Freeze Request',
        detail: 'OTPs permit instant fund transfer. Call bank fraud desk NOW to block current outgoing transfers.',
        action: 'Urgent Bank Call'
      },
      {
        title: 'Dial 1930 Cyber Fraud Helpline',
        detail: 'Report transaction reference details immediately to trigger financial intermediary freezes.',
        action: 'Dial 1930'
      },
      {
        title: 'File an Online Police Complaint',
        detail: 'Save screenshots of the SMS, caller ID, and transaction debits as digital evidence.',
        action: 'Save Screenshots'
      }
    ]
  },
  {
    id: 'DOWNLOAD',
    title: 'Downloaded an Unknown File or APK',
    icon: FileWarning,
    desc: 'You installed an application sent via WhatsApp/SMS (e.g., electricity.apk, kyc.apk).',
    urgency: 'CRITICAL',
    steps: [
      {
        title: 'Turn on Airplane Mode Immediately',
        detail: 'Cut off Wi-Fi and Mobile Data to prevent the trojan from exfiltrating your OTPs and contacts.',
        action: 'Airplane Mode'
      },
      {
        title: 'Uninstall Malicious App in Safe Mode',
        detail: 'Reboot phone into Safe Mode, go to Settings > Apps > Device Admin Apps, revoke permissions, and uninstall.',
        action: 'Remove APK'
      },
      {
        title: 'Change All Banking Passwords from Another Device',
        detail: 'Do not use the infected phone to log in to bank accounts until completely scanned or factory reset.',
        action: 'Password Reset'
      }
    ]
  }
];

export default function EmergencyWizard() {
  const { isEmergencyOpen, setIsEmergencyOpen, showToast } = useScan();
  const [selectedScenario, setSelectedScenario] = useState('BANKING');
  const [completedSteps, setCompletedSteps] = useState({});
  const [copiedDraft, setCopiedDraft] = useState(false);

  if (!isEmergencyOpen) return null;

  const currentScenario = COMPROMISE_SCENARIOS.find(s => s.id === selectedScenario) || COMPROMISE_SCENARIOS[0];

  const toggleStep = (stepIdx) => {
    setCompletedSteps(prev => ({
      ...prev,
      [`${selectedScenario}-${stepIdx}`]: !prev[`${selectedScenario}-${stepIdx}`]
    }));
  };

  const handleCopyComplaintDraft = () => {
    const draft = `CYBER FRAUD INCIDENT COMPLAINT DRAFT
----------------------------------------
Date/Time: ${new Date().toLocaleString()}
Nature of Threat: Phishing / Financial Deception (${currentScenario.title})
Containment Status: Emergency Response Checklist Initiated

Description:
I was directed to a counterfeit phishing portal via a deceptive link/message and inadvertently exposed sensitive details.
Helpline Contacted: National Cyber Crime Helpline (1930)

Request:
Please initiate immediate account/card freeze and trace unauthorized transaction logs.

Generated via PhishShield Incident Playbook`;

    navigator.clipboard.writeText(draft);
    setCopiedDraft(true);
    showToast('Incident complaint draft copied', 'info');
    setTimeout(() => setCopiedDraft(false), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-dark-950/90 backdrop-blur-lg overflow-y-auto animate-in fade-in duration-200">
      <div className="relative w-full max-w-3xl glass-panel p-6 sm:p-8 border-2 border-rose-500/80 shadow-glow-danger bg-dark-900/95 my-8">
        
        {/* Header */}
        <div className="flex items-start justify-between pb-5 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-rose-500/20 border border-rose-500/40 text-rose-400 flex items-center justify-center shrink-0">
              <ShieldAlert className="w-7 h-7 animate-bounce" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-black uppercase tracking-wider bg-rose-500 text-white px-2 py-0.5 rounded">
                  EMERGENCY INCIDENT PLAYBOOK
                </span>
                <span className="text-xs text-rose-300 font-mono">
                  Golden Hour Containment
                </span>
              </div>
              <h3 className="text-xl sm:text-2xl font-black text-white mt-1">
                "I Already Clicked the Link"
              </h3>
            </div>
          </div>

          <button
            onClick={() => setIsEmergencyOpen(false)}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700 transition-all"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Emergency Quick Helplines Bar */}
        <div className="my-5 p-4 rounded-xl bg-gradient-to-r from-rose-950/80 to-dark-950 border border-rose-500/40 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-rose-600 flex items-center justify-center text-white shrink-0 shadow-glow-danger">
              <PhoneCall className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <p className="text-xs font-bold text-rose-200">
                National Cyber Crime Helpline (India)
              </p>
              <p className="text-lg font-black text-white font-mono">
                Dial 1930 <span className="text-xs font-normal text-rose-300 font-sans">(Toll-Free 24/7)</span>
              </p>
            </div>
          </div>

          <a
            href="https://cybercrime.gov.in"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow-md transition-all shrink-0"
          >
            <Globe className="w-4 h-4" />
            <span>cybercrime.gov.in</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>

        {/* Step 1: Select What Was Shared */}
        <div className="mb-6">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
            Step 1: Select What You Entered or Downloaded
          </h4>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            {COMPROMISE_SCENARIOS.map((scenario) => {
              const Icon = scenario.icon;
              const isSelected = selectedScenario === scenario.id;
              return (
                <button
                  key={scenario.id}
                  onClick={() => setSelectedScenario(scenario.id)}
                  className={`p-3 rounded-xl border text-left transition-all flex flex-col justify-between ${
                    isSelected
                      ? 'bg-rose-500/20 border-rose-500 text-white shadow-glow-danger'
                      : 'bg-dark-950/70 border-slate-800 text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
                  }`}
                >
                  <Icon className={`w-5 h-5 mb-2 ${isSelected ? 'text-rose-400' : 'text-slate-400'}`} />
                  <span className="text-xs font-bold line-clamp-1">{scenario.title}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Step 2: Tailored Immediate Action Checklist */}
        <div className="space-y-3 mb-6">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Step 2: Execute Immediate Containment Checklist
            </h4>
            <span className="text-[11px] text-indigo-400 font-mono">
              {currentScenario.steps.filter((_, idx) => completedSteps[`${selectedScenario}-${idx}`]).length} / {currentScenario.steps.length} Completed
            </span>
          </div>

          <div className="space-y-2.5">
            {currentScenario.steps.map((step, idx) => {
              const isDone = !!completedSteps[`${selectedScenario}-${idx}`];
              return (
                <div
                  key={idx}
                  onClick={() => toggleStep(idx)}
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

                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <h5 className={`text-xs font-bold ${isDone ? 'line-through text-emerald-300' : 'text-white'}`}>
                        {idx + 1}. {step.title}
                      </h5>
                      <span className="text-[10px] font-mono uppercase bg-slate-800 text-slate-400 px-2 py-0.5 rounded">
                        {step.action}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                      {step.detail}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Step 3: Copy Complaint Summary */}
        <div className="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3">
          <button
            onClick={handleCopyComplaintDraft}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 text-xs font-bold transition-all"
          >
            {copiedDraft ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-cyber-cyan" />}
            <span>{copiedDraft ? 'Complaint Draft Copied!' : 'Copy Official Complaint Draft'}</span>
          </button>

          <button
            onClick={() => setIsEmergencyOpen(false)}
            className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-md transition-all"
          >
            Done / Close Playbook
          </button>
        </div>

      </div>
    </div>
  );
}
