import React from 'react';
import { Clock, ShieldAlert, ShieldCheck, AlertTriangle, Eye, ArrowRight } from 'lucide-react';
import { useScan } from '../context/ScanContext';

export default function RecentScansTable({ setActiveTab }) {
  const { history, setActiveVerdict, setIsVerdictModalOpen } = useScan();

  const handleView = (scan) => {
    setActiveVerdict(scan);
    setIsVerdictModalOpen(true);
  };

  const recentItems = history.slice(0, 5);

  return (
    <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-card">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <Clock className="w-4 h-4 text-blue-600" />
          Recent Security Checks
        </h3>

        {history.length > 5 && setActiveTab && (
          <button
            onClick={() => setActiveTab('history')}
            className="text-xs font-bold text-blue-700 dark:text-blue-400 hover:underline flex items-center gap-1"
          >
            <span>View All ({history.length})</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {recentItems.length === 0 ? (
        <div className="py-8 text-center text-xs text-slate-500 dark:text-slate-400">
          <p>No recent checks. Use the scanner above to test a link or message.</p>
        </div>
      ) : (
        <div className="space-y-2.5">
          {recentItems.map((scan) => {
            const isDanger = scan.riskLevel === 'HIGH_RISK';
            const isWarn = scan.riskLevel === 'SUSPICIOUS';

            return (
              <div
                key={scan.scanId}
                onClick={() => handleView(scan)}
                className="p-3.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 transition-all cursor-pointer flex items-center justify-between gap-3 text-xs"
              >
                <div className="flex items-center gap-3 min-w-0 flex-1">
                  <div className={`p-2 rounded-lg shrink-0 ${
                    isDanger ? 'bg-red-100 dark:bg-red-950 text-red-600' : isWarn ? 'bg-amber-100 dark:bg-amber-950 text-amber-600' : 'bg-emerald-100 dark:bg-emerald-950 text-emerald-600'
                  }`}>
                    {isDanger ? <ShieldAlert className="w-4 h-4" /> : isWarn ? <AlertTriangle className="w-4 h-4" /> : <ShieldCheck className="w-4 h-4" />}
                  </div>

                  <div className="min-w-0">
                    <p className="font-mono font-bold text-slate-900 dark:text-white truncate">
                      {scan.url || scan.domain}
                    </p>
                    <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400">
                      <span className="uppercase font-semibold">{scan.scanType || 'URL'}</span>
                      <span>•</span>
                      <span>{scan.threatCategory?.replace(/_/g, ' ') || 'BENIGN'}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <span className={`font-mono font-bold text-xs ${
                    isDanger ? 'text-red-600 dark:text-red-400' : isWarn ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'
                  }`}>
                    {scan.riskScore}/100
                  </span>
                  <Eye className="w-4 h-4 text-slate-400" />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
