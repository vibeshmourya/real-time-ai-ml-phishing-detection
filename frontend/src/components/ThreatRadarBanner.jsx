import React, { useState, useEffect } from 'react';
import { AlertCircle, ChevronRight, ShieldCheck, Flame, X } from 'lucide-react';
import { dashboardApi } from '../api/dashboardApi';
import { useScan } from '../context/ScanContext';

export default function ThreatRadarBanner() {
  const [alerts, setAlerts] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isDismissed, setIsDismissed] = useState(false);
  const { openEmergencyPlaybook } = useScan();

  useEffect(() => {
    dashboardApi.getActiveAlerts().then(data => {
      if (data && data.length > 0) {
        setAlerts(data);
      }
    });
  }, []);

  useEffect(() => {
    if (alerts.length <= 1) return;
    const interval = setInterval(() => {
      setCurrentIndex(prev => (prev + 1) % alerts.length);
    }, 8000);
    return () => clearInterval(interval);
  }, [alerts]);

  if (isDismissed || alerts.length === 0) return null;

  const current = alerts[currentIndex];

  return (
    <div className="w-full bg-gradient-to-r from-rose-950/70 via-dark-900 to-indigo-950/70 border-y border-rose-500/20 py-2.5 px-4">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-3 text-xs">
        
        <div className="flex items-center gap-2.5 overflow-hidden">
          <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 font-bold tracking-wide uppercase text-[10px] shrink-0 border border-rose-500/30 animate-pulse">
            <Flame className="w-3 h-3 text-rose-400" />
            Active Cyber Threat
          </span>

          <div className="flex items-center gap-2 truncate">
            <span className="font-semibold text-white truncate">
              {current.title}
            </span>
            <span className="text-slate-400 hidden md:inline truncate">
              — {current.summary}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={() => openEmergencyPlaybook(current.category)}
            className="flex items-center gap-1 text-indigo-300 hover:text-white font-medium hover:underline text-[11px]"
          >
            <span>Safety Checklist</span>
            <ChevronRight className="w-3 h-3" />
          </button>
          <button
            onClick={() => setIsDismissed(true)}
            className="text-slate-500 hover:text-slate-300 p-0.5 rounded"
            title="Dismiss advisory"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>

      </div>
    </div>
  );
}
