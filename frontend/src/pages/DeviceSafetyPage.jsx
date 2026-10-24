import React, { useState } from 'react';
import { 
  Smartphone, ShieldAlert, WifiOff, Lock, RefreshCw, AlertTriangle, 
  CheckCircle2, ExternalLink, HelpCircle, Shield, KeyRound, Globe, PhoneCall
} from 'lucide-react';

export default function DeviceSafetyPage({ setActiveTab }) {
  const [activeWorkflow, setActiveWorkflow] = useState('stolen_phone');

  const workflows = [
    {
      id: 'stolen_phone',
      title: 'Lost or Stolen Phone',
      icon: Smartphone,
      badge: 'Immediate Priority',
      badgeColor: 'bg-red-100 text-red-800',
      description: 'Block your SIM, unlink UPI apps, blacklist your IMEI on CEIR, and trigger remote wipe.',
      steps: [
        {
          title: '1. Suspend Your SIM Card Immediately',
          details: 'Call your telecom carrier from another phone to disable outgoing/incoming SMS so thieves cannot receive 2FA banking OTPs.',
          action: 'Carrier Numbers: Jio (198), Airtel (198), Vi (198), BSNL (1503)',
        },
        {
          title: '2. Deactivate UPI & Mobile Banking Apps',
          details: 'Call your bank customer care to unlink your mobile number from Google Pay, PhonePe, Paytm, and NetBanking.',
          action: 'Bank Emergency Helplines: SBI (1800 1234), HDFC (1800 1600), ICICI (1800 1080)',
        },
        {
          title: '3. Blacklist IMEI on Indian CEIR Portal',
          details: 'Visit India\'s official Central Equipment Identity Register to block your handset across all Indian telecom networks.',
          link: 'https://www.ceir.gov.in/',
          linkText: 'Visit ceir.gov.in (Official Portal)',
        },
        {
          title: '4. Remotely Erase Your Device',
          details: 'Use Google Find My Device or Apple iCloud to wipe confidential photos, documents, and credentials.',
          link: 'https://www.google.com/android/find',
          linkText: 'Google Find My Device',
        },
      ],
    },
    {
      id: 'sim_swap',
      title: 'Suspected SIM Swap',
      icon: WifiOff,
      badge: 'Critical Telecom Risk',
      badgeColor: 'bg-amber-100 text-amber-800',
      description: 'If your phone suddenly shows "No Service" while others have signal, act immediately.',
      steps: [
        {
          title: '1. Verify with Your Carrier Right Away',
          details: 'If your SIM stopped working without physical damage, a scammer may have fraudulently requested a duplicate SIM.',
          action: 'Call your telecom operator immediately to ask if a SIM exchange was issued.',
        },
        {
          title: '2. Check Your Bank Accounts from a PC',
          details: 'Log in to NetBanking from a secure computer and verify recent account debits.',
          action: 'If suspicious transfers appear, call the bank fraud desk to freeze electronic banking.',
        },
        {
          title: '3. Call National Cyber Crime Helpline (1930)',
          details: 'Report the suspected SIM swap within the Golden Hour so destination accounts can be frozen.',
          action: 'Helpline: 1930 (24x7 India)',
        },
      ],
    },
    {
      id: 'malicious_apk',
      title: 'Suspicious App or Malicious APK',
      icon: ShieldAlert,
      badge: 'Device Malware Risk',
      badgeColor: 'bg-rose-100 text-rose-800',
      description: 'Contain apps downloaded from Telegram, WhatsApp, or fake bank/utility links.',
      steps: [
        {
          title: '1. Turn On Airplane Mode Immediately',
          details: 'Cutting off Wi-Fi and mobile data stops the malicious APK from transmitting your SMS OTPs to attacker servers.',
          action: 'Swipe down on Android/iOS -> Turn ON Airplane Mode.',
        },
        {
          title: '2. Boot into Android Safe Mode',
          details: 'Safe Mode prevents third-party downloaded apps from running in the background.',
          action: 'Press & hold Power button -> Long press "Power off" -> Tap "Safe Mode".',
        },
        {
          title: '3. Uninstall the Suspicious App',
          details: 'Open Android Settings -> Apps -> All Apps. Look for recent suspicious downloads, fake "SBI KYC" apps, or unknown names, and tap Uninstall.',
          action: 'Settings -> Apps -> Uninstall App.',
        },
        {
          title: '4. Revoke Device Administrator Privileges',
          details: 'Some malicious apps lock themselves as Device Admins. Deactivate them first before uninstalling.',
          action: 'Settings -> Security -> Device Admin Apps -> Deactivate.',
        },
      ],
    },
    {
      id: 'account_compromise',
      title: 'Account Compromise & Unauthorized Access',
      icon: KeyRound,
      badge: 'Credential Exposure',
      badgeColor: 'bg-blue-100 text-blue-800',
      description: 'Secure your primary email, social media, and banking credentials.',
      steps: [
        {
          title: '1. Change Password from a Clean Device',
          details: 'Change passwords using a separate, trusted device. Choose a strong, unique 14+ character password.',
          action: 'Never reuse the compromised password on other portals.',
        },
        {
          title: '2. Select "Log Out of All Active Sessions"',
          details: 'This terminates active login tokens held by attackers on unauthorized devices.',
          action: 'Settings -> Security -> Manage Devices -> Sign Out Everywhere.',
        },
        {
          title: '3. Switch to App-Based Authenticator (2FA)',
          details: 'Move from SMS-based OTPs to app authenticators (Google Authenticator, Microsoft Authenticator) which cannot be intercepted via SIM swaps.',
          action: 'Enable Authenticator App in Account Security settings.',
        },
      ],
    },
  ];

  const currentWf = workflows.find(w => w.id === activeWorkflow) || workflows[0];

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="border-b border-slate-200 pb-5">
        <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
          My Device Safety
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Guided emergency protocols for lost phones, SIM swaps, malicious APKs, and account compromise.
        </p>
      </div>

      {/* Honest Boundary Notice */}
      <div className="p-3 bg-blue-50/80 border border-blue-200 rounded-2xl flex items-start gap-2.5 text-xs text-blue-900">
        <HelpCircle className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold">Device Safety:</span> Step-by-step containment guides with verified links to official portals (CEIR, Find My Device, 1930 Helpline). Prahar does not access private device records.
        </div>
      </div>

      {/* Workflow Tabs */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
        {workflows.map((wf) => {
          const Icon = wf.icon;
          const isActive = activeWorkflow === wf.id;
          return (
            <button
              key={wf.id}
              onClick={() => setActiveWorkflow(wf.id)}
              className={`p-3.5 rounded-2xl border text-left flex flex-col justify-between transition ${
                isActive
                  ? 'bg-blue-700 text-white border-blue-700 shadow-sm'
                  : 'bg-white border-slate-200 hover:bg-slate-50 text-slate-800'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <Icon className={`w-5 h-5 ${isActive ? 'text-white' : 'text-blue-600'}`} />
                <span className={`text-[9px] font-bold px-2 py-0.5 rounded-full ${
                  isActive ? 'bg-white/20 text-white' : wf.badgeColor
                }`}>
                  {wf.badge}
                </span>
              </div>
              <span className="text-xs font-bold leading-tight">{wf.title}</span>
            </button>
          );
        })}
      </div>

      {/* Active Workflow Card */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-xs space-y-6">
        <div>
          <h2 className="text-lg font-black text-slate-900">{currentWf.title}</h2>
          <p className="text-xs text-slate-500 mt-0.5">{currentWf.description}</p>
        </div>

        <div className="space-y-4">
          {currentWf.steps.map((st, idx) => (
            <div key={idx} className="p-4 bg-slate-50 border border-slate-100 rounded-2xl space-y-2">
              <h3 className="text-xs font-bold text-slate-900">{st.title}</h3>
              <p className="text-xs text-slate-600 leading-relaxed">{st.details}</p>
              
              {st.action && (
                <div className="text-[11px] font-bold text-blue-700 bg-blue-50 px-3 py-1.5 rounded-xl border border-blue-100/60 inline-block">
                  {st.action}
                </div>
              )}

              {st.link && (
                <div>
                  <a
                    href={st.link}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-700 hover:bg-blue-800 text-white text-xs font-bold rounded-xl transition shadow-2xs"
                  >
                    <span>{st.linkText}</span>
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}
