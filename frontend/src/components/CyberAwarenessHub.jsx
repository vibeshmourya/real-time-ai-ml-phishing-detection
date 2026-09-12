import React, { useState } from 'react';
import { 
  BookOpen, ShieldCheck, Film, Volume2, VolumeX, 
  HelpCircle, CheckCircle2, XCircle, ArrowRight, Sparkles, AlertTriangle
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

export const LEARN_MODULES = [
  {
    id: 'BANKING_PHISHING',
    title: 'Banking & Financial Scams',
    icon: '🏦',
    shortDesc: 'Fake lookalike login pages mimicking top banks to capture NetBanking IDs, Passwords, and OTPs.',
    whatIsIt: 'Attackers create deceptive websites that look nearly identical to your bank’s official online portal.',
    howItWorks: [
      'You receive an urgent message claiming your account or debit card has been suspended.',
      'The message includes a suspicious link directing to a clone portal (e.g. secure-login-hdfc.tk).',
      'When you enter your customer ID, password, or OTP, the data is instantly captured by the scammer.'
    ],
    warningSigns: [
      'Unofficial domain name (e.g., .xyz, .tk, or raw IP address)',
      'Threatening urgency ("Account blocked within 2 hours")',
      'Requests for 6-digit OTP, ATM PIN, or card CVV'
    ],
    preventionSteps: [
      'Always type the official bank URL directly into your browser address bar or use official banking apps.',
      'Never share OTP or PIN with anyone, including bank employees.',
      'If compromised, call 1930 and freeze your card immediately.'
    ],
    quiz: {
      question: 'Your bank sends an SMS asking you to click a link and enter your OTP to prevent account suspension. What should you do?',
      options: [
        'Click the link and quickly enter the OTP to keep the account active.',
        'Ignore the SMS link and contact your bank branch or official app directly.',
        'Forward the SMS to family members.'
      ],
      correctIndex: 1,
      explanation: 'Banks never ask for OTPs or login credentials via unofficial SMS links. Always verify via the official banking app.'
    }
  },
  {
    id: 'FAKE_KYC',
    title: 'Fake KYC & SIM Deactivation',
    icon: '🪪',
    shortDesc: 'Threats of immediate SIM or bank account blocking unless you complete an urgent KYC document update.',
    whatIsIt: 'Scammers pretend to be telecom operators or banks claiming your KYC has expired and your service will stop.',
    howItWorks: [
      'You receive an SMS warning that your SIM or bank account will be blocked by midnight.',
      'You are asked to call a mobile number or click a link to submit Aadhaar/PAN details.',
      'The caller may also instruct you to download remote support software (AnyDesk/TeamViewer).'
    ],
    warningSigns: [
      'Sent from an ordinary 10-digit mobile number rather than an official sender ID',
      'Demands to install unknown APK files or screen sharing apps',
      'Unreasonable deadlines ("within 24 hours")'
    ],
    preventionSteps: [
      'KYC updates must only be performed at official branches or verified apps.',
      'Never download APK files suggested by unknown callers.',
      'Block and report the sender number to Prahar.'
    ],
    quiz: {
      question: 'A caller claiming to be from your telecom company asks you to install an app to complete SIM KYC. What is this?',
      options: [
        'A standard routine telecom update.',
        'A remote screen sharing scam to steal your OTPs and credentials.',
        'A mandatory government verification.'
      ],
      correctIndex: 1,
      explanation: 'Installing remote desktop apps allows attackers to see your screen and intercept bank OTPs.'
    }
  },
  {
    id: 'DELIVERY_SCAM',
    title: 'Package Delivery & Courier Traps',
    icon: '📦',
    shortDesc: 'Deceptive SMS claiming your package cannot be delivered due to wrong address or unpaid Rs 5 fee.',
    whatIsIt: 'Scammers exploit online shopping habits by sending fake courier delay alerts.',
    howItWorks: [
      'You receive a text stating a parcel is on hold due to address issues.',
      'The link directs you to a fake payment gateway requesting a nominal Rs 5 / Rs 10 fee.',
      'Once card details or OTP are submitted, attackers debit much larger amounts.'
    ],
    warningSigns: [
      'Informal SMS without your name or package tracking number',
      'Requests for nominal card payments via link',
      'Non-official URL imitating postal services'
    ],
    preventionSteps: [
      'Track packages only through the shopping website where you ordered.',
      'India Post and legitimate couriers do not ask for card payments via SMS.',
      'Check tracking numbers directly on official courier portals.'
    ],
    quiz: {
      question: 'You receive an SMS saying your package is held for a Rs 5 re-delivery charge. What should you do?',
      options: [
        'Pay the Rs 5 immediately so the package arrives.',
        'Check your order status on the genuine shopping app where you made the purchase.',
        'Share your card CVV to authenticate the address.'
      ],
      correctIndex: 1,
      explanation: 'Legitimate postal services never ask for micro-payments via random SMS links.'
    }
  },
  {
    id: 'JOB_SCAM',
    title: 'Work from Home & Telegram Task Scams',
    icon: '💼',
    shortDesc: 'Fake part-time job offers promising Rs 3,000–5,000/day for liking videos or rating products.',
    whatIsIt: 'Unsolicited messages on WhatsApp or Telegram offering high pay for simple online tasks.',
    howItWorks: [
      'You are assigned simple tasks (like YouTube video likes) and paid Rs 200–500 initially.',
      'You are then added to a Telegram group and asked to "invest" money in prepaid tasks.',
      'Once you deposit large amounts, the scammer blocks you and disappears.'
    ],
    warningSigns: [
      'Unsolicited job offers from unknown international numbers',
      'Unrealistic earnings for trivial tasks',
      'Demands to deposit money to "unlock" earnings'
    ],
    preventionSteps: [
      'Legitimate employers never demand payment from candidates.',
      'Do not engage with unsolicited employment messages on WhatsApp or Telegram.',
      'Report fake job groups to cybercrime authorities.'
    ],
    quiz: {
      question: 'A job recruiter on Telegram asks you to deposit Rs 5,000 to unlock higher commission tasks. Is this legitimate?',
      options: [
        'Yes, it is a standard security deposit.',
        'No, it is a task-based investment scam. Never deposit money for jobs.',
        'Yes, if they already paid Rs 200 for previous tasks.'
      ],
      correctIndex: 1,
      explanation: 'Initial small payouts are bait. Real employers never charge employees to work.'
    }
  },
  {
    id: 'QR_SCAM',
    title: 'QR Code Payment Fraud',
    icon: '📷',
    shortDesc: 'Scammers sending QR codes claiming "Scan this QR code to receive money in your account".',
    whatIsIt: 'Deceptive QR codes sent on marketplace apps (like OLX) or WhatsApp.',
    howItWorks: [
      'A buyer pretends to purchase an item and sends you a QR code to "receive payment".',
      'When you scan the QR code and enter your UPI PIN, money is debited from your account.'
    ],
    warningSigns: [
      'Claims that scanning a QR code or entering UPI PIN is needed to RECEIVE money',
      'Urgent pressure to scan immediately',
      'Refusal to meet in person or pay in cash'
    ],
    preventionSteps: [
      'Golden Rule: You NEVER need to enter your UPI PIN to RECEIVE money.',
      'UPI PIN is only used for DEBITING funds from your account.',
      'Inspect any QR code with Prahar before scanning with a payment app.'
    ],
    quiz: {
      question: 'Someone on OLX sends you a QR code and asks you to scan it and enter your UPI PIN to receive money. Will you receive funds?',
      options: [
        'Yes, the UPI PIN confirms your identity to receive money.',
        'No, entering your UPI PIN will DEBIT money from your bank account.',
        'Only if the amount is less than Rs 1,000.'
      ],
      correctIndex: 1,
      explanation: 'Entering a UPI PIN always authorizes money to leave your account. Receiving money never requires a PIN.'
    }
  },
  {
    id: 'GOVERNMENT_IMPERSONATION',
    title: 'Government Scheme & Tax Refund Scams',
    icon: '🏛️',
    shortDesc: 'Impersonation of government portals promising subsidies or threatening electricity disconnections.',
    whatIsIt: 'Fake messages claiming to be from the Income Tax department, PM schemes, or state electricity boards.',
    howItWorks: [
      'You receive an alert stating a tax refund or subsidy has been approved for your account.',
      'You are prompted to enter banking credentials or download an app to process the refund.'
    ],
    warningSigns: [
      'Official Indian government websites always use .gov.in or .nic.in domains',
      'Threats of power disconnection tonight from private mobile numbers'
    ],
    preventionSteps: [
      'Check that government links end strictly in .gov.in.',
      'Income tax refunds are credited automatically without link authorization.'
    ],
    quiz: {
      question: 'Which of the following is a genuine Indian government website domain?',
      options: [
        'incometax-refund-portal.org',
        'incometax.gov.in',
        'gov-scheme-verify.in'
      ],
      correctIndex: 1,
      explanation: 'All official government departments use the protected .gov.in top-level domain.'
    }
  }
];

export default function CyberAwarenessHub() {
  const { isAwarenessOpen, setIsAwarenessOpen, selectedCategory } = useScan();
  const [activeModuleId, setActiveModuleId] = useState(selectedCategory || 'BANKING_PHISHING');
  const [selectedQuizAnswer, setSelectedQuizAnswer] = useState(null);
  const [showQuizResult, setShowQuizResult] = useState(false);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);

  const activeModule = LEARN_MODULES.find(m => m.id === activeModuleId) || LEARN_MODULES[0];

  const handleQuizOption = (idx) => {
    setSelectedQuizAnswer(idx);
    setShowQuizResult(true);
  };

  const toggleAudio = () => {
    if ('speechSynthesis' in window) {
      if (isPlayingAudio) {
        window.speechSynthesis.cancel();
        setIsPlayingAudio(false);
      } else {
        const text = `Learning module: ${activeModule.title}. ${activeModule.whatIsIt}. Warning signs include: ${activeModule.warningSigns.join(', ')}. Key safety advice: ${activeModule.preventionSteps.join(', ')}`;
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.onend = () => setIsPlayingAudio(false);
        utterance.onerror = () => setIsPlayingAudio(false);
        window.speechSynthesis.speak(utterance);
        setIsPlayingAudio(true);
      }
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
              Cyber Safety Education
            </span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-1">
            Prahar Learn
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
            Simple guides and warning signs to help you recognize and avoid common digital scams.
          </p>
        </div>

        <button
          onClick={toggleAudio}
          className="prahar-btn-secondary py-2 text-xs flex items-center gap-1.5"
        >
          {isPlayingAudio ? <VolumeX className="w-4 h-4 text-blue-600" /> : <Volume2 className="w-4 h-4 text-blue-600" />}
          <span>{isPlayingAudio ? 'Stop Audio' : 'Listen to this guide'}</span>
        </button>
      </div>

      {/* Module Navigation Tabs */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
        {LEARN_MODULES.map((m) => {
          const isSelected = m.id === activeModuleId;
          return (
            <button
              key={m.id}
              onClick={() => {
                setActiveModuleId(m.id);
                setSelectedQuizAnswer(null);
                setShowQuizResult(false);
                if (isPlayingAudio && 'speechSynthesis' in window) {
                  window.speechSynthesis.cancel();
                  setIsPlayingAudio(false);
                }
              }}
              className={`p-3 rounded-xl text-left transition-all border ${
                isSelected
                  ? 'bg-blue-700 text-white font-bold shadow-sm border-blue-700'
                  : 'bg-white dark:bg-dark-900 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
              }`}
            >
              <span className="text-xl block mb-1">{m.icon}</span>
              <p className="text-xs font-bold truncate">{m.title}</p>
            </button>
          );
        })}
      </div>

      {/* Active Module Lesson Content */}
      <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-6 shadow-card">
        
        {/* Module Header */}
        <div className="flex items-center gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
          <span className="text-3xl">{activeModule.icon}</span>
          <div>
            <h3 className="text-xl font-bold text-slate-900 dark:text-white">
              {activeModule.title}
            </h3>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-0.5">
              {activeModule.shortDesc}
            </p>
          </div>
        </div>

        {/* 1. What is it & How it works */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-2">
              1. What is this scam?
            </h4>
            <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed font-medium">
              {activeModule.whatIsIt}
            </p>
          </div>

          {/* Video Placeholder (Honest claim: Video lesson coming soon) */}
          <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex flex-col items-center justify-center text-center">
            <Film className="w-8 h-8 text-slate-400 mb-2" />
            <span className="text-xs font-bold text-slate-700 dark:text-slate-300">
              Short Video Lesson
            </span>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              Video lesson coming soon.
            </p>
          </div>
        </div>

        {/* 2. How it usually works */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3">
            2. How this scam usually works
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {activeModule.howItWorks.map((step, idx) => (
              <div key={idx} className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs">
                <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-bold text-xs flex items-center justify-center mb-2">
                  {idx + 1}
                </span>
                <p className="text-slate-700 dark:text-slate-300 font-medium leading-snug">
                  {step}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* 3. Warning Signs & Prevention Steps */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          
          {/* Warning signs */}
          <div className="p-5 rounded-2xl bg-amber-50/60 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/40">
            <h4 className="text-xs font-bold uppercase tracking-wider text-amber-900 dark:text-amber-300 mb-3 flex items-center gap-1.5">
              <AlertTriangle className="w-4 h-4" />
              3. Warning Signs
            </h4>
            <ul className="space-y-2 text-xs text-amber-950 dark:text-amber-200">
              {activeModule.warningSigns.map((w, idx) => (
                <li key={idx} className="flex items-start gap-2">
                  <span className="text-amber-600 font-bold">•</span>
                  <span>{w}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Prevention steps */}
          <div className="p-5 rounded-2xl bg-emerald-50/60 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40">
            <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-900 dark:text-emerald-300 mb-3 flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4" />
              4. How to Stay Safe
            </h4>
            <ul className="space-y-2 text-xs text-emerald-950 dark:text-emerald-200 font-medium">
              {activeModule.preventionSteps.map((p, idx) => (
                <li key={idx} className="flex items-start gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 mt-0.5 shrink-0" />
                  <span>{p}</span>
                </li>
              ))}
            </ul>
          </div>

        </div>

        {/* 4. Quick Interactive Quiz */}
        {activeModule.quiz && (
          <div className="p-5 rounded-2xl bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 space-y-4">
            <div className="flex items-center gap-2 text-blue-900 dark:text-blue-300 text-xs font-bold uppercase tracking-wider">
              <HelpCircle className="w-4 h-4" />
              <span>5. Quick Quiz: Test Your Understanding</span>
            </div>

            <p className="text-sm font-bold text-slate-900 dark:text-white">
              {activeModule.quiz.question}
            </p>

            <div className="space-y-2">
              {activeModule.quiz.options.map((opt, idx) => {
                const isSelected = selectedQuizAnswer === idx;
                const isCorrect = idx === activeModule.quiz.correctIndex;
                return (
                  <button
                    key={idx}
                    onClick={() => handleQuizOption(idx)}
                    className={`w-full text-left p-3 rounded-xl text-xs font-medium transition-all border flex items-center justify-between ${
                      showQuizResult
                        ? isCorrect
                          ? 'bg-emerald-100 dark:bg-emerald-950 border-emerald-400 text-emerald-900 dark:text-emerald-200 font-bold'
                          : isSelected
                          ? 'bg-red-100 dark:bg-red-950 border-red-400 text-red-900 dark:text-red-200'
                          : 'bg-white dark:bg-dark-900 border-slate-200 dark:border-slate-800 text-slate-600'
                        : isSelected
                        ? 'bg-blue-100 dark:bg-blue-900 border-blue-400 text-blue-900 font-bold'
                        : 'bg-white dark:bg-dark-900 border-slate-200 dark:border-slate-800 text-slate-700 dark:text-slate-300 hover:border-slate-300'
                    }`}
                  >
                    <span>{opt}</span>
                    {showQuizResult && isCorrect && <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />}
                    {showQuizResult && isSelected && !isCorrect && <XCircle className="w-4 h-4 text-red-600 shrink-0" />}
                  </button>
                );
              })}
            </div>

            {showQuizResult && (
              <div className="p-3 rounded-xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 text-xs animate-in fade-in">
                <span className="font-bold block mb-1 text-slate-800 dark:text-slate-200">
                  {selectedQuizAnswer === activeModule.quiz.correctIndex ? '🎉 Correct!' : '⚠️ Incorrect.'}
                </span>
                <p className="text-slate-600 dark:text-slate-400">
                  {activeModule.quiz.explanation}
                </p>
              </div>
            )}
          </div>
        )}

      </div>

    </div>
  );
}
