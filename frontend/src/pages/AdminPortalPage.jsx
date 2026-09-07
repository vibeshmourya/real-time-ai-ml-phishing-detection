import React, { useState, useEffect } from 'react';
import { 
  Activity, ShieldAlert, CheckCircle2, AlertOctagon, BarChart2, 
  Users, Globe, Server, RefreshCw, Check, X, Eye
} from 'lucide-react';
import { dashboardApi } from '../api/dashboardApi';
import { useScan } from '../context/ScanContext';

export default function AdminPortalPage() {
  const { showToast } = useScan();
  const [stats, setStats] = useState(null);
  const [reports, setReports] = useState([
    {
      id: 501,
      url: 'http://income-tax-refund-verify.tk/tax/claim.php',
      category: 'GOVERNMENT_IMPERSONATION',
      reporterEmail: 'citizen.kumar@gmail.com',
      description: 'Received SMS promising Rs 15,000 income tax refund directly to card.',
      status: 'PENDING',
      createdAt: '15 mins ago'
    },
    {
      id: 502,
      url: 'http://sbi-yono-kyc-reactivate.xyz/login',
      category: 'BANKING_PHISHING',
      reporterEmail: 'rajesh.sharma@yahoo.in',
      description: 'SMS threatening YONO account freeze by midnight unless Aadhaar entered.',
      status: 'PENDING',
      createdAt: '35 mins ago'
    },
    {
      id: 503,
      url: 'http://free-gift-voucher.top/win',
      category: 'LOTTERY_SCAM',
      reporterEmail: 'student@college.edu',
      description: 'WhatsApp viral chain claiming free Amazon gift vouchers.',
      status: 'VERIFIED_PHISH',
      createdAt: '2 hours ago'
    }
  ]);

  useEffect(() => {
    dashboardApi.getAdminStats().then(data => setStats(data));
  }, []);

  const handleUpdateReportStatus = (id, newStatus) => {
    setReports(prev => prev.map(r => r.id === id ? { ...r, status: newStatus } : r));
    showToast(`Report #${id} marked as ${newStatus}`, 'safe');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-widest bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 px-3 py-0.5 rounded-full">
              CERT-In & Security Analyst Telemetry
            </span>
            <span className="text-xs text-emerald-400 font-mono flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" /> Live Stream
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-white mt-1">
            Global Threat Radar & Operations Center
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-mono bg-dark-950 px-3 py-1.5 rounded-xl border border-slate-800 text-slate-300">
            Engine Latency: <span className="text-emerald-400 font-bold">{stats?.avgDetectionTimeMs || 42}ms</span>
          </span>
          <span className="text-xs font-mono bg-dark-950 px-3 py-1.5 rounded-xl border border-slate-800 text-slate-300">
            Model Accuracy: <span className="text-cyber-cyan font-bold">{stats?.accuracyRate || '99.4%'}</span>
          </span>
        </div>
      </div>

      {/* Global Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-card p-5 border border-indigo-500/30 bg-indigo-950/20">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Total Scanned Globally</span>
            <Globe className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl sm:text-3xl font-black font-mono text-white">
            {stats?.totalGlobalScans?.toLocaleString() || '14,829'}
          </p>
          <p className="text-[11px] text-slate-400 mt-1">+{stats?.scansToday || 842} scans today</p>
        </div>

        <div className="glass-card p-5 border border-rose-500/30 bg-rose-950/20">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Malicious Threats Blocked</span>
            <ShieldAlert className="w-4 h-4 text-rose-400" />
          </div>
          <p className="text-2xl sm:text-3xl font-black font-mono text-rose-400">
            {stats?.totalThreatsBlocked?.toLocaleString() || '4,120'}
          </p>
          <p className="text-[11px] text-slate-400 mt-1">27.8% malicious strike rate</p>
        </div>

        <div className="glass-card p-5 border border-amber-500/30 bg-amber-950/20">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Active Phishing Campaigns</span>
            <Activity className="w-4 h-4 text-amber-400" />
          </div>
          <p className="text-2xl sm:text-3xl font-black font-mono text-amber-400">
            {stats?.activeMaliciousCampaigns || 24}
          </p>
          <p className="text-[11px] text-slate-400 mt-1">Under CERT mitigation</p>
        </div>

        <div className="glass-card p-5 border border-cyan-500/30 bg-cyan-950/20">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Pending Triage Queue</span>
            <Server className="w-4 h-4 text-cyan-400" />
          </div>
          <p className="text-2xl sm:text-3xl font-black font-mono text-cyan-400">
            {reports.filter(r => r.status === 'PENDING').length}
          </p>
          <p className="text-[11px] text-slate-400 mt-1">Citizen crowd submissions</p>
        </div>
      </div>

      {/* 2-Column Analyst Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left: Community Incident Triage Queue (8 cols) */}
        <div className="lg:col-span-8 glass-panel p-6 border border-slate-800">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <AlertOctagon className="w-4 h-4 text-rose-400" />
              Community Threat Submissions (Triage Queue)
            </h3>
            <span className="text-xs text-slate-400 font-mono">
              {reports.length} Reports Logged
            </span>
          </div>

          <div className="space-y-3">
            {reports.map((report) => (
              <div
                key={report.id}
                className="p-4 rounded-xl bg-dark-950/80 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div className="space-y-1 min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-indigo-400 font-bold">#{report.id}</span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                      report.status === 'VERIFIED_PHISH' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' :
                      report.status === 'FALSE_POSITIVE' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' :
                      'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    }`}>
                      {report.status}
                    </span>
                    <span className="text-slate-400 text-[11px]">{report.category}</span>
                    <span className="text-slate-400 text-[10px] ml-auto">{report.createdAt}</span>
                  </div>

                  <p className="font-mono text-slate-200 truncate font-semibold">
                    {report.url}
                  </p>
                  <p className="text-slate-400 text-[11px]">
                    "{report.description}" — <span className="text-slate-400 font-mono">{report.reporterEmail}</span>
                  </p>
                </div>

                {/* Status update buttons */}
                {report.status === 'PENDING' && (
                  <div className="flex items-center gap-1.5 shrink-0 self-end sm:self-center">
                    <button
                      onClick={() => handleUpdateReportStatus(report.id, 'VERIFIED_PHISH')}
                      className="px-2.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-bold flex items-center gap-1 transition-all"
                      title="Confirm phishing & add to global blocklist"
                    >
                      <Check className="w-3.5 h-3.5" />
                      <span>Block</span>
                    </button>
                    <button
                      onClick={() => handleUpdateReportStatus(report.id, 'FALSE_POSITIVE')}
                      className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold flex items-center gap-1 transition-all"
                    >
                      <X className="w-3.5 h-3.5" />
                      <span>Dismiss</span>
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Right: Top Targeted Brand Intelligence (4 cols) */}
        <div className="lg:col-span-4 glass-panel p-6 border border-slate-800 space-y-4">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <BarChart2 className="w-4 h-4 text-cyber-cyan" />
            Top Spoofed Brands
          </h3>

          <div className="space-y-3">
            {(stats?.topTargetedBrands || []).map((brand, idx) => (
              <div key={idx} className="p-3 rounded-xl bg-dark-950/70 border border-slate-800 text-xs">
                <div className="flex justify-between font-semibold text-slate-200 mb-1">
                  <span>{brand.brand}</span>
                  <span className="font-mono text-indigo-400">{brand.count} blocks</span>
                </div>
                <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                  <div
                    style={{ width: `${Math.min(100, (brand.count / 1400) * 100)}%` }}
                    className="h-full bg-gradient-to-r from-indigo-500 to-rose-500 rounded-full"
                  />
                </div>
              </div>
            ))}
          </div>

          <div className="p-3 rounded-xl bg-indigo-950/30 border border-indigo-500/20 text-xs text-indigo-200 space-y-1">
            <p className="font-bold">Automated Threat Sync:</p>
            <p className="text-[11px] text-slate-400">
              Heuristic patterns auto-update across connected Chrome Extensions and backend API instances every 15 minutes.
            </p>
          </div>
        </div>

      </div>

    </div>
  );
}
