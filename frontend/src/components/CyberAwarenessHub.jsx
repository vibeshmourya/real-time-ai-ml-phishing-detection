import React, { useState } from 'react';
import { 
  BookOpen, Play, Volume2, VolumeX, ShieldCheck, 
  ExternalLink, CheckCircle2, ChevronRight, Sparkles, Film, AlertTriangle
} from 'lucide-react';
import { useScan } from '../context/ScanContext';

const AWARENESS_MODULES = [
  {
    category: 'BANKING_PHISHING',
    title: 'Counterfeit Bank Login Traps',
    duration: '30s Lesson',
    icon: '🏦',
    shortDesc: 'How scammers build duplicate NetBanking pages on free .tk/.xyz domains to capture your login and OTP.',
    redFlags: [
      'Unusual domain name (e.g. hdfc-login.tk instead of hdfcbank.com)',
      'Artificial urgency ("Account will be suspended in 2 hours")',
      'Asking for Debit Card CVV / ATM PIN during online verification'
    ],
    audioScript: 'Alert! Legitimate banks never threaten immediate account suspension via SMS links. Always check the exact address bar.'
  },
  {
    category: 'KYC_FRAUD',
    title: 'Fake KYC & PAN Expiration SMS',
    duration: '40s Lesson',
    icon: '📱',
    shortDesc: 'Threat actors send SMS claiming your wallet or SIM KYC has expired, redirecting to an unauthorized credential portal.',
    redFlags: [
      'Sent from an ordinary 10-digit mobile number instead of an official SMS header',
      'Demanding immediate APK app install (e.g. AnyDesk / RustDesk / QuickSupport)',
      'Threatening SIM card deactivation tonight'
    ],
    audioScript: 'Caution! RBI guidelines mandate that banks never complete KYC via unverified third-party web forms or WhatsApp messages.'
  },
  {
    category: 'DELIVERY_SCAM',
    title: 'Parcel Courier & Postal Traps',
    duration: '30s Lesson',
    icon: '📦',
    shortDesc: 'Fake courier SMS stating an India Post or courier parcel cannot be delivered until you pay a ₹10 address update fee.',
    redFlags: [
      'Deceptive links masked via URL shorteners (bit.ly, is.gd)',
      'Demanding small fee via an unknown cloned payment gateway',
      'Vague package tracking descriptions'
    ],
    audioScript: 'Warning! Postal fraud SMS messages attempt to clone your credit or debit card through small ₹5-₹10 re-delivery payments.'
  },
  {
    category: 'LOTTERY_SCAM',
    title: 'Part-Time Job & Lottery Extortion',
    duration: '35s Lesson',
    icon: '💼',
    shortDesc: 'Telegram tasks and YouTube rating part-time jobs asking you to deposit advance money for guaranteed daily returns.',
    redFlags: [
      'Unsolicited WhatsApp / Telegram job offers paying ₹5,000 daily for simple clicks',
      'Requirement to pay "registration fee" or "crypto deposit" to withdraw earnings',
      'High-pressure recruitment managers'
    ],
    audioScript: 'Remember: Real jobs will never ask you to pay money to receive salary or complete registration tasks.'
  }
];

export default function CyberAwarenessHub() {
  const { openAwarenessGuide, showToast } = useScan();
  const [activeAudioCategory, setActiveAudioCategory] = useState(null);

  const handlePlayAudio = (module) => {
    if ('speechSynthesis' in window) {
      if (activeAudioCategory === module.category) {
        window.speechSynthesis.cancel();
        setActiveAudioCategory(null);
      } else {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(module.audioScript);
        utterance.rate = 1.0;
        utterance.onend = () => setActiveAudioCategory(null);
        utterance.onerror = () => setActiveAudioCategory(null);
        window.speechSynthesis.speak(utterance);
        setActiveAudioCategory(module.category);
        showToast(`Playing audio guide: ${module.title}`, 'info');
      }
    } else {
      showToast('Audio synthesis not supported in this browser', 'warn');
    }
  };

  return (
    <div className="w-full glass-panel p-5 sm:p-6 border border-slate-800/80">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-5">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-cyber-cyan" />
              Cyber Threat Defense Academy
            </h3>
            <span className="text-[10px] bg-indigo-500/20 text-indigo-300 font-bold uppercase tracking-wider px-2 py-0.5 rounded-full border border-indigo-500/30">
              Micro-Guides
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Quick 30-second prevention breakdowns for common digital scams.
          </p>
        </div>
      </div>

      {/* Grid of Micro-Guides */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {AWARENESS_MODULES.map((module) => {
          const isPlaying = activeAudioCategory === module.category;
          return (
            <div
              key={module.category}
              className="p-4 rounded-xl bg-dark-950/70 border border-slate-800/80 hover:border-slate-700 transition-all flex flex-col justify-between group"
            >
              <div>
                <div className="flex items-center justify-between mb-2.5">
                  <div className="flex items-center gap-2">
                    <span className="text-xl">{module.icon}</span>
                    <h4 className="text-xs font-bold text-white group-hover:text-indigo-300 transition-colors">
                      {module.title}
                    </h4>
                  </div>
                  <span className="text-[10px] bg-slate-800 text-slate-400 px-2 py-0.5 rounded font-mono">
                    {module.duration}
                  </span>
                </div>

                <p className="text-xs text-slate-400 mb-3 leading-relaxed">
                  {module.shortDesc}
                </p>

                {/* 3 Red Flags */}
                <div className="space-y-1.5 mb-3 bg-dark-900/60 p-2.5 rounded-lg border border-slate-800/50">
                  <div className="text-[10px] font-bold uppercase tracking-wider text-rose-400 flex items-center gap-1">
                    <AlertTriangle className="w-3 h-3" /> Key Red Flags:
                  </div>
                  {module.redFlags.map((flag, idx) => (
                    <div key={idx} className="flex items-start gap-1.5 text-[11px] text-slate-300">
                      <span className="text-rose-400 font-bold">•</span>
                      <span>{flag}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
                <button
                  onClick={() => handlePlayAudio(module)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                    isPlaying 
                      ? 'bg-rose-500 text-white border-rose-400 animate-pulse' 
                      : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
                  }`}
                >
                  {isPlaying ? <VolumeX className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5 text-cyber-cyan" />}
                  <span>{isPlaying ? 'Stop Audio' : 'Listen (30s)'}</span>
                </button>

                <button
                  onClick={() => openAwarenessGuide(module.category)}
                  className="flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 font-semibold hover:underline"
                >
                  <span>Interactive Playbook</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

    </div>
  );
}
