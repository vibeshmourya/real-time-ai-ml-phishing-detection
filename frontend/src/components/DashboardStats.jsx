import React from 'react';
import { ShieldCheck, ShieldAlert, AlertTriangle, Activity } from 'lucide-react';
import { useScan } from '../context/ScanContext';

export default function DashboardStats() {
  const { history } = useScan();

  const totalChecks = history.length;
  const safeCount = history.filter(s => s.riskLevel === 'SAFE').length;
  const warnCount = history.filter(s => s.riskLevel === 'SUSPICIOUS').length;
  const dangerCount = history.filter(s => s.riskLevel === 'HIGH_RISK').length;

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          Your Security Activity
        </h3>
        <span className="text-xs text-slate-400">
          {totalChecks} total checks performed
        </span>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5">
        
        {/* Total Checks */}
        <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-1">
            <span className="text-xs font-semibold">Total Checked</span>
            <Activity className="w-4 h-4 text-blue-600" />
          </div>
          <p className="text-2xl font-black font-mono text-slate-900 dark:text-white">
            {totalChecks}
          </p>
          <span className="text-[11px] text-slate-400">Links, messages & images</span>
        </div>

        {/* Safe */}
        <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card">
          <div className="flex items-center justify-between text-emerald-600 dark:text-emerald-400 mb-1">
            <span className="text-xs font-semibold">Verified Safe</span>
            <ShieldCheck className="w-4 h-4" />
          </div>
          <p className="text-2xl font-black font-mono text-emerald-600 dark:text-emerald-400">
            {safeCount}
          </p>
          <span className="text-[11px] text-slate-400">No malicious indicators</span>
        </div>

        {/* Needs Attention / Suspicious */}
        <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card">
          <div className="flex items-center justify-between text-amber-600 dark:text-amber-400 mb-1">
            <span className="text-xs font-semibold">Needs Attention</span>
            <AlertTriangle className="w-4 h-4" />
          </div>
          <p className="text-2xl font-black font-mono text-amber-600 dark:text-amber-400">
            {warnCount}
          </p>
          <span className="text-[11px] text-slate-400">Unusual characteristics</span>
        </div>

        {/* High Risk */}
        <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card">
          <div className="flex items-center justify-between text-red-600 dark:text-red-400 mb-1">
            <span className="text-xs font-semibold">High Risk Phishing</span>
            <ShieldAlert className="w-4 h-4" />
          </div>
          <p className="text-2xl font-black font-mono text-red-600 dark:text-red-400">
            {dangerCount}
          </p>
          <span className="text-[11px] text-slate-400">Deceptive traps blocked</span>
        </div>

      </div>
    </div>
  );
}
