import React, { useState } from 'react';
import { 
  ShieldCheck, ShieldAlert, AlertTriangle, Search, Filter, 
  ExternalLink, RotateCcw, Trash2, Eye, Clock, ArrowUpRight
} from 'lucide-react';
import { useScan } from '../context/ScanContext';

export default function RecentScansTable() {
  const { history, setActiveVerdict, setIsVerdictModalOpen, clearHistory, executeScan } = useScan();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterLevel, setFilterLevel] = useState('ALL');

  const filteredHistory = history.filter((item) => {
    const matchesSearch = (item.url || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                          (item.domain || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                          (item.threatCategory || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterLevel === 'ALL' || item.riskLevel === filterLevel;
    return matchesSearch && matchesFilter;
  });

  const handleView = (scan) => {
    setActiveVerdict(scan);
    setIsVerdictModalOpen(true);
  };

  const handleRescan = (scan) => {
    executeScan(scan.url, scan.scanType || 'URL');
  };

  return (
    <div className="w-full glass-panel p-5 sm:p-6 border border-slate-800/80">
      
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-5">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            <Clock className="w-4 h-4 text-indigo-400" />
            Recent Threat Inspections
          </h3>
          <p className="text-xs text-slate-400">
            Click on any previous scan to review the explainable AI safety report.
          </p>
        </div>

        {/* Search & Filter Bar */}
        <div className="flex items-center gap-2 w-full sm:w-auto flex-wrap">
          <div className="relative flex-1 sm:w-48">
            <Search className="w-3.5 h-3.5 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search history..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 bg-dark-950 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          <select
            value={filterLevel}
            onChange={(e) => setFilterLevel(e.target.value)}
            className="px-2.5 py-1.5 bg-dark-950 border border-slate-800 rounded-xl text-xs text-slate-300 focus:outline-none focus:border-indigo-500 cursor-pointer"
          >
            <option value="ALL">All Scans</option>
            <option value="HIGH_RISK">High Risk (🔴)</option>
            <option value="SUSPICIOUS">Suspicious (🟡)</option>
            <option value="SAFE">Safe (🟢)</option>
          </select>

          {history.length > 0 && (
            <button
              onClick={clearHistory}
              className="p-1.5 rounded-xl bg-slate-800/80 hover:bg-rose-950/50 hover:text-rose-400 text-slate-400 border border-slate-700/60 transition-all text-xs flex items-center gap-1"
              title="Clear scan history"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>

      {/* Table / List */}
      {filteredHistory.length === 0 ? (
        <div className="text-center py-12 px-4 rounded-xl bg-dark-950/40 border border-dashed border-slate-800">
          <Clock className="w-8 h-8 text-slate-600 mx-auto mb-2" />
          <p className="text-sm font-semibold text-slate-400">No scan records found</p>
          <p className="text-xs text-slate-400 mt-1">
            {searchTerm || filterLevel !== 'ALL'
              ? 'Try adjusting your search query or filter'
              : 'Paste a link above in the Threat Scanner to inspect it'}
          </p>
        </div>
      ) : (
        <div className="space-y-2.5">
          {filteredHistory.map((scan) => {
            const isDanger = scan.riskLevel === 'HIGH_RISK';
            const isWarn = scan.riskLevel === 'SUSPICIOUS';
            const isSafe = scan.riskLevel === 'SAFE';

            return (
              <div
                key={scan.scanId}
                className="p-3.5 sm:p-4 rounded-xl bg-dark-950/70 border border-slate-800/80 hover:border-slate-700 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 group"
              >
                <div className="flex items-start gap-3 min-w-0 flex-1">
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 mt-0.5 ${
                    isDanger 
                      ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' 
                      : isWarn 
                      ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' 
                      : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                  }`}>
                    {isDanger ? <ShieldAlert className="w-4 h-4" /> : isWarn ? <AlertTriangle className="w-4 h-4" /> : <ShieldCheck className="w-4 h-4" />}
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                        isDanger ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' :
                        isWarn ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' :
                        'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                      }`}>
                        {isDanger ? 'High Risk' : isWarn ? 'Suspicious' : 'Safe'}
                      </span>
                      <span className="text-[11px] font-mono text-slate-400">
                        Score: {scan.riskScore}/100
                      </span>
                      <span className="text-[10px] text-slate-400">
                        {new Date(scan.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>

                    <p className="text-xs font-mono text-slate-200 mt-1 truncate max-w-full">
                      {scan.url}
                    </p>

                    <p className="text-[11px] text-slate-400 truncate mt-0.5 line-clamp-1">
                      {scan.detection?.explanationSummary}
                    </p>
                  </div>
                </div>

                {/* Row Action Buttons */}
                <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                  <button
                    onClick={() => handleRescan(scan)}
                    className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700 text-xs transition-all"
                    title="Re-scan this target"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => handleView(scan)}
                    className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-indigo-600/30 hover:bg-indigo-600 text-indigo-200 hover:text-white border border-indigo-500/40 text-xs font-semibold transition-all"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>View Verdict</span>
                  </button>
                </div>

              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}
