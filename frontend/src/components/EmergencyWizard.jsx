import React, { useState } from 'react';
import { 
  ShieldAlert, X, PhoneCall, Globe, CheckSquare, 
  Square, AlertTriangle, ArrowRight, RefreshCw, Key, 
  CreditCard, Smartphone, Download, DollarSign, HelpCircle, CheckCircle2
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

const SCENARIOS = [
  {
    id: 'opened_only',
    icon: Globe,
    title: 'I only opened the link',
    desc: 'I visited the website but did not submit passwords, OTPs, or financial details.',
    severity: 'LOW',
    steps: [
      { id: 'close_tab', text: 'Close the browser tab immediately.', detail: 'Prevent further scripts or deceptive popups from loading.' },
      { id: 'clear_cache', text: 'Clear recent browser cookies & cache.', detail: 'Remove any tracking cookies or stored session identifiers.' },
      { id: 'run_antivirus', text: 'Run a device antivirus or malware scan.', detail: 'Ensure no silent downloads occurred in the background.' },
      { id: 'monitor', text: 'Stay alert for follow-up phishing calls or SMS.', detail: 'Scammers may attempt secondary social engineering.' }
    ]
  },
  {
    id: 'password_entered',
    icon: Key,
    title: 'I entered my password / login credentials',
    desc: 'I entered my email, social media, or portal username and password.',
    severity: 'HIGH',
    steps: [
      { id: 'change_pw', text: 'Change your password immediately from a trusted device.', detail: 'Navigate manually to the genuine service website, not via links in messages.' },
      { id: 'logout_all', text: 'Select "Log out of all active sessions / devices".', detail: 'Invalidates any active sessions the attacker might have initiated.' },
      { id: 'enable_2fa', text: 'Enable Two-Factor Authentication (2FA) using an Authenticator app.', detail: 'Adds an essential second security layer beyond passwords.' },
      { id: 'check_recovery', text: 'Review account recovery email and phone number.', detail: 'Ensure attacker did not insert their own backup contact info.' }
    ]
  },
  {
    id: 'bank_entered',
    icon: CreditCard,
    title: 'I entered banking or card details',
    desc: 'I provided debit/credit card numbers, CVV, expiry date, or NetBanking ID.',
    severity: 'CRITICAL',
    steps: [
      { id: 'call_bank', text: 'Contact your bank fraud department immediately.', detail: 'Call the official customer care number printed on the back of your physical card.' },
      { id: 'block_card', text: 'Temporarily freeze or block your affected card & NetBanking.', detail: 'Most banking apps allow instant card toggle/freeze in security settings.' },
      { id: 'check_tx', text: 'Inspect recent transaction history for unauthorized debits.', detail: 'Note down any suspicious transactions with exact timestamp and amount.' },
      { id: 'call_1930', text: 'Call 1930 (National Cyber Crime Helpline).', detail: 'The Golden Hour protocol helps banks reverse fraudulent transactions.' }
    ]
  },
  {
    id: 'otp_shared',
    icon: Smartphone,
    title: 'I shared an OTP / SMS verification code',
    desc: 'I typed or verbally shared a 6-digit OTP received on my phone.',
    severity: 'CRITICAL',
    steps: [
      { id: 'freeze_netbanking', text: 'Block NetBanking and UPI access immediately.', detail: 'Attackers use OTPs to authorize instant fund transfers or UPI registrations.' },
      { id: 'call_1930_otp', text: 'Dial 1930 Cyber Fraud Helpline without delay.', detail: 'Immediate reporting within the Golden Hour maximizes chance of recovery.' },
      { id: 'inform_bank', text: 'Notify your bank branch of unauthorized transaction authorization.', detail: 'Request an immediate stop-payment or freeze on outgoing mandates.' }
    ]
  },
  {
    id: 'file_downloaded',
    icon: Download,
    title: 'I downloaded a file or APK',
    desc: 'I installed an app file (.apk) or remote support software.',
    severity: 'HIGH',
    steps: [
      { id: 'airplane_mode', text: 'Turn on Airplane Mode / Disconnect Wi-Fi immediately.', detail: 'Cuts off remote access and data transmission to the attacker server.' },
      { id: 'uninstall_app', text: 'Uninstall the downloaded app from Settings -> Apps.', detail: 'Look for recent unknown apps, fake bank APKs, or remote screen sharers.' },
      { id: 'scan_device', text: 'Run Google Play Protect or a trusted mobile antivirus.', detail: 'Scan and eliminate any remaining malicious payloads.' },
      { id: 'change_pw_other', text: 'Change passwords from another clean device.', detail: 'Avoid entering passwords on the infected phone until verified clean.' }
    ]
  },
  {
    id: 'money_transferred',
    icon: DollarSign,
    title: 'I made a payment or transferred money',
    desc: 'I transferred funds via UPI, QR code, IMPS, or payment gateway.',
    severity: 'CRITICAL',
    steps: [
      { id: 'call_1930_immed', text: 'Call 1930 immediately (National Cyber Crime Helpline).', detail: 'Provide transaction ID, sender bank, receiver UPI/account number, and amount.' },
      { id: 'report_bank_tx', text: 'File a formal fraud dispute ticket with your bank.', detail: 'Request transaction freezing with reference to your 1930 incident number.' },
      { id: 'file_cybercrime', text: 'Submit details on cybercrime.gov.in portal.', detail: 'Official national portal for cyber fraud reporting in India.' }
    ]
  },
  {
    id: 'not_sure',
    icon: HelpCircle,
    title: "I'm not sure what happened",
    desc: 'I interacted with a suspicious link or caller and feel uneasy.',
    severity: 'MEDIUM',
    steps: [
      { id: 'check_accounts', text: 'Review bank and email account activities.', detail: 'Check for unexpected login locations or notification messages.' },
      { id: 'change_key_pw', text: 'Update passwords for primary email and banking.', detail: 'A proactive precaution prevents potential lateral access.' },
      { id: 'verify_official', text: 'Call the official organization using genuine contact numbers.', detail: 'Never call back numbers received in suspicious messages.' }
    ]
  }
];

export function EmergencyWizardContent({ standalone = false }) {
  const { isEmergencyOpen, setIsEmergencyOpen, selectedEmergencyScenario } = useScan();
  const { t } = useLanguage();
  const [activeScenarioId, setActiveScenarioId] = useState(selectedEmergencyScenario || 'bank_entered');
  const [completedSteps, setCompletedSteps] = useState({});

  const activeScenario = SCENARIOS.find(s => s.id === activeScenarioId) || SCENARIOS[0];

  const toggleStep = (stepId) => {
    setCompletedSteps(prev => ({
      ...prev,
      [stepId]: !prev[stepId]
    }));
  };

  const progress = Math.round(
    (activeScenario.steps.filter(s => completedSteps[s.id]).length / activeScenario.steps.length) * 100
  );

  return (
    <div className="space-y-6">
      
      {/* Top Banner */}
      <div className="p-5 sm:p-6 rounded-2xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-red-600 text-white flex items-center justify-center shrink-0 shadow-sm">
            <ShieldAlert className="w-7 h-7" />
          </div>
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-red-950 dark:text-red-100">
              {t('emergency.title', 'I Already Clicked a Suspicious Link')}
            </h2>
            <p className="text-xs sm:text-sm text-red-800 dark:text-red-300 mt-0.5">
              {t('emergency.subtitle', 'Take immediate step-by-step containment actions to protect your accounts and funds.')}
            </p>
          </div>
        </div>

        {/* National Helpline Hotline */}
        <div className="flex items-center gap-3 bg-white dark:bg-dark-900 px-4 py-2.5 rounded-2xl border border-red-300 dark:border-red-800 shrink-0 shadow-xs">
          <PhoneCall className="w-5 h-5 text-red-600 animate-pulse" />
          <div>
            <span className="text-[10px] font-bold uppercase text-slate-500 block">
              {t('emergency.helplineTitle', 'National Cyber Helpline')}:
            </span>
            <span className="text-xl font-black text-red-600 dark:text-red-400 font-mono leading-none">
              1930
            </span>
          </div>
        </div>
      </div>

      {/* 1. QUESTION: WHAT HAPPENED? */}
      <div>
        <h3 className="text-sm font-bold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-3">
          {t('emergency.question', '1. What happened? Select your situation:')}
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {SCENARIOS.map((s) => {
            const Icon = s.icon;
            const isSelected = s.id === activeScenarioId;
            return (
              <button
                key={s.id}
                onClick={() => setActiveScenarioId(s.id)}
                className={`p-3.5 rounded-xl text-left transition-all border ${
                  isSelected
                    ? 'bg-blue-50 dark:bg-blue-950/60 border-blue-600 dark:border-blue-500 shadow-sm'
                    : 'bg-white dark:bg-dark-900 border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-2.5 mb-1.5">
                  <div className={`p-1.5 rounded-lg ${
                    isSelected ? 'bg-blue-600 text-white' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'
                  }`}>
                    <Icon className="w-4 h-4" />
                  </div>
                  <span className={`text-xs font-bold ${
                    isSelected ? 'text-blue-900 dark:text-blue-100' : 'text-slate-800 dark:text-slate-200'
                  }`}>
                    {s.title}
                  </span>
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-snug line-clamp-2">
                  {s.desc}
                </p>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. STEP-BY-STEP CHECKLIST */}
      <div className="p-5 sm:p-6 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-card">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-blue-600" />
              Immediate Safety Steps: {activeScenario.title}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Follow these recommended actions in order. Check items off as you complete them.
            </p>
          </div>

          <div className="text-right">
            <span className="text-xs font-bold text-slate-700 dark:text-slate-300">
              {progress}% Completed
            </span>
          </div>
        </div>

        <div className="space-y-3">
          {activeScenario.steps.map((step, idx) => {
            const isDone = !!completedSteps[step.id];
            return (
              <div
                key={step.id}
                onClick={() => toggleStep(step.id)}
                className={`p-4 rounded-xl border cursor-pointer transition-all flex items-start gap-3.5 ${
                  isDone 
                    ? 'bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-300 dark:border-emerald-800/60 text-slate-700' 
                    : 'bg-slate-50/80 dark:bg-dark-950/80 border-slate-200 dark:border-slate-800 hover:border-slate-300'
                }`}
              >
                <div className="mt-0.5 shrink-0 text-slate-400">
                  {isDone ? (
                    <CheckSquare className="w-5 h-5 text-emerald-600" />
                  ) : (
                    <Square className="w-5 h-5 text-slate-400" />
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 text-[10px] font-black flex items-center justify-center shrink-0">
                      {idx + 1}
                    </span>
                    <h4 className={`text-sm font-bold ${
                      isDone ? 'line-through text-slate-500 dark:text-slate-400' : 'text-slate-900 dark:text-white'
                    }`}>
                      {step.text}
                    </h4>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 pl-7">
                    {step.detail}
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        {/* External Official Cyber Crime Resources */}
        <div className="mt-6 pt-4 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2 text-slate-600 dark:text-slate-400">
            <Globe className="w-4 h-4 text-blue-600" />
            <span>Official Reporting: <strong>National Cyber Crime Portal</strong></span>
          </div>

          <a
            href="https://cybercrime.gov.in"
            target="_blank"
            rel="noopener noreferrer"
            className="prahar-btn-secondary py-2 text-xs flex items-center gap-1.5"
          >
            <span>Visit cybercrime.gov.in</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </a>
        </div>

      </div>

    </div>
  );
}

export default function EmergencyWizard() {
  const { isEmergencyOpen, setIsEmergencyOpen } = useScan();

  if (!isEmergencyOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/60 backdrop-blur-sm overflow-y-auto animate-in fade-in duration-150">
      <div className="relative w-full max-w-4xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-modal p-6 sm:p-8 my-8">
        <button
          onClick={() => setIsEmergencyOpen(false)}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <EmergencyWizardContent />
      </div>
    </div>
  );
}
