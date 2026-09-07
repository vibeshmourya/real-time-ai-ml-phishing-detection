import React from 'react';
import { ShieldCheck, ShieldAlert, AlertTriangle, Activity, TrendingUp, CheckCircle, Shield } from 'lucide-react';
import { useScan } from '../context/ScanContext';

export default function DashboardStats() {
  const { history } = useScan();

  const totalScans = history.length;
  const highRiskCount = history.filter(s => s.riskLevel === 'HIGH_RISK').length;
  const suspiciousCount = history.filter(s => s.riskLevel === 'SUSPICIOUS').length;
  const safeCount = history.filter(s => s.riskLevel === 'SAFE').length;

  const threatPct = totalScans > 0 ? Math.round((highRiskCount / totalScans) * 100) : 0;
  const safePct = totalScans > 0 ? Math.round((safeCount / totalScans) * 100) : 100;

  const statCards = [
    {
      label: 'Total Scanned',
      value: totalScans,
      icon: Activity,
      color: 'from-blue-500/20 to-indigo-500/10',
      textColor: 'text-indigo-400',
      borderColor: 'border-indigo-500/30',
      subtext: 'Real-time AI processed'
    },
    {
      label: 'Threats Blocked',
      value: highRiskCount,
      icon: ShieldAlert,
      color: 'from-rose-500/20 to-red-500/10',
      textColor: 'text-rose-400',
      borderColor: 'border-rose-500/30',
      subtext: `${threatPct}% detected phishing`
    },
    {
      label: 'Suspicious Flagged',
      value: suspiciousCount,
      icon: AlertTriangle,
      color: 'from-amber-500/20 to-yellow-500/10',
      textColor: 'text-amber-400',
      borderColor: 'border-amber-500/30',
      subtext: 'Caution indicators'
    },
    {
      label: 'Verified Safe',
      value: safeCount,
      icon: ShieldCheck,
      color: 'from-emerald-500/20 to-teal-500/10',
      textColor: 'text-emerald-400',
      borderColor: 'border-emerald-500/30',
      subtext: `${safePct}% verified legitimate`
    }
  ];

  return (
    <div className="w-full space-y-4">
      {/* 4 Stat Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {statCards.map((stat, idx) => {
          const Icon = stat.icon;
          return (
            <div
              key={idx}
              className={`glass-card p-4 sm:p-5 border bg-gradient-to-br ${stat.color} ${stat.borderColor} relative overflow-hidden group hover:scale-[1.02] transition-transform`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  {stat.label}
                </span>
                <div className={`p-2 rounded-xl bg-dark-950/60 ${stat.textColor}`}>
                  <Icon className="w-4 h-4" />
                </div>
              </div>

              <div className="flex items-baseline gap-2">
                <span className={`text-2xl sm:text-3xl font-black font-mono ${stat.textColor}`}>
                  {stat.value}
                </span>
              </div>

              <p className="text-[11px] text-slate-400 mt-1 font-medium">
                {stat.subtext}
              </p>
            </div>
          );
        })}
      </div>

      {/* Cyber Hygiene & Risk Bar */}
      <div className="glass-card p-4 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <div className="w-9 h-9 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0 border border-emerald-500/30">
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-white">Shield Protection Status</span>
              <span className="text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full font-bold uppercase border border-emerald-500/30">
                100% Active
              </span>
            </div>
            <p className="text-[11px] text-slate-400">
              Heuristic neural engines + SSRF guards continuously analyzing threat patterns.
            </p>
          </div>
        </div>

        {/* Mini progress breakdown */}
        <div className="w-full sm:w-72 space-y-1.5">
          <div className="flex justify-between text-[11px] font-mono text-slate-400">
            <span>Threat Breakdown</span>
            <span>{safeCount} Safe / {highRiskCount} Blocked</span>
          </div>
          <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden flex">
            <div 
              style={{ width: `${totalScans > 0 ? (safeCount / totalScans) * 100 : 70}%` }} 
              className="h-full bg-emerald-500 transition-all duration-500" 
              title="Safe Scans"
            />
            <div 
              style={{ width: `${totalScans > 0 ? (suspiciousCount / totalScans) * 100 : 15}%` }} 
              className="h-full bg-amber-500 transition-all duration-500" 
              title="Suspicious Scans"
            />
            <div 
              style={{ width: `${totalScans > 0 ? (highRiskCount / totalScans) * 100 : 15}%` }} 
              className="h-full bg-rose-500 transition-all duration-500" 
              title="High Risk Scans"
            />
          </div>
        </div>
      </div>

    </div>
  );
}
