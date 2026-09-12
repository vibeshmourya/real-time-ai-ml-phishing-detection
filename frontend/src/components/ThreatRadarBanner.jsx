import React, { useState } from 'react';
import { AlertCircle, ChevronRight, X, ShieldAlert } from 'lucide-react';
import { useScan } from '../context/ScanContext';

const PUBLIC_ALERTS = [
  {
    id: 1,
    title: '⚠️ Active KYC Expiry SMS Scam',
    desc: 'Scammers are sending SMS claiming bank accounts will be frozen unless KYC is verified on unauthorized links.',
    category: 'FAKE_KYC'
  },
  {
    id: 2,
    title: '⚡ Electricity Disconnection Threats',
    desc: 'Fraudulent notices threatening power disconnection tonight unless a phone number is called.',
    category: 'GOVERNMENT_IMPERSONATION'
  },
  {
    id: 3,
    title: '📦 Package Address Verification Traps',
    desc: 'Fake delivery alerts asking for Rs 5-10 card payments to update package shipping addresses.',
    category: 'DELIVERY_SCAM'
  }
];

export default function ThreatRadarBanner() {
  const { openAwarenessGuide } = useScan();
  const [currentIdx, setCurrentIdx] = useState(0);
  const [dismissed, setDismissed] = useState(false);

  if (dismissed) return null;

  const activeAlert = PUBLIC_ALERTS[currentIdx];

  return (
    <div className="bg-amber-50 dark:bg-amber-950/40 border-b border-amber-200 dark:border-amber-900/60 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2.5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-xs">
        
        <div className="flex items-center gap-2.5 min-w-0">
          <span className="px-2 py-0.5 rounded-md bg-amber-200 dark:bg-amber-900 text-amber-900 dark:text-amber-200 font-bold text-[10px] uppercase shrink-0">
            Security Alert
          </span>
          <p className="text-amber-950 dark:text-amber-200 truncate font-medium">
            <strong>{activeAlert.title}:</strong> {activeAlert.desc}
          </p>
        </div>

        <div className="flex items-center gap-3 shrink-0 self-end sm:self-center">
          <button
            onClick={() => openAwarenessGuide(activeAlert.category)}
            className="text-amber-900 dark:text-amber-300 font-bold hover:underline flex items-center gap-1"
          >
            <span>Learn How to Stay Safe</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={() => setDismissed(true)}
            className="p-1 text-amber-700 dark:text-amber-400 hover:text-amber-900"
            title="Dismiss"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>

      </div>
    </div>
  );
}
