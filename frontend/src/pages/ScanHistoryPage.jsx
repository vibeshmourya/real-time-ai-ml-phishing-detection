import React, { useState } from 'react';
import { 
  Clock, ShieldAlert, ShieldCheck, AlertTriangle, Trash2, 
  Eye, RefreshCw, Search, Filter, Link2, MessageSquare, 
  Mail, Image as ImageIcon, QrCode, ArrowRight
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

export default function ScanHistoryPage({ setActiveTab }) {
  const { history, setActiveVerdict, setIsVerdictModalOpen, deleteScan, clearHistory } = useScan();
  const { t } = useLanguage();

  const [searchQuery, setSearchQuery] = useState('');
  const [riskFilter, setRiskFilter] = useState('ALL'); // 'ALL' | 'SAFE' | 'SUSPICIOUS' | 'HIGH_RISK'
  const [typeFilter, setTypeFilter] = useState('ALL'); // 'ALL' | 'URL' | 'MESSAGE' | 'EMAIL' | 'SCREENSHOT' | 'QR'

  const filteredHistory = history.filter((item) => {
    // Search query
    const target = (item.url || item.domain || '').toLowerCase();
    const matchesSearch = target.includes(searchQuery.toLowerCase());

    // Risk filter
    const matchesRisk = riskFilter === 'ALL' || item.riskLevel === riskFilter;

    // Type filter
    const matchesType = typeFilter === 'ALL' || (item.scanType || 'URL') === typeFilter;

    return matchesSearch && matchesRisk && matchesType;
  });

  const handleViewScan = (scan) => {
    setActiveVerdict(scan);
    setIsVerdictModalOpen(true);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2.5">
            <Clock className="w-7 h-7 text-blue-700 dark:text-blue-500" />
            {t('nav.myScans', 'My Scans')}
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">
            Review previous security checks and risk reports on this device.
          </p>
        </div>

        {history.length > 0 && (
          <button
            onClick={clearHistory}
            className="text-xs text-red-600 dark:text-red-400 hover:text-red-700 flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-red-200 dark:border-red-900/40 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Clear Scan History</span>
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 shadow-card">
        
        {/* Search */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search scans by URL or domain..."
            className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-600"
          />
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {/* Risk Level Filter */}
          <select
            value={riskFilter}
            onChange={(e) => setRiskFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-600"
          >
            <option value="ALL">All Risk Levels</option>
            <option value="SAFE">Verified Safe</option>
            <option value="SUSPICIOUS">Suspicious</option>
            <option value="HIGH_RISK">High Risk</option>
          </select>

          {/* Input Type Filter */}
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-600"
          >
            <option value="ALL">All Input Types</option>
            <option value="URL">Link / URL</option>
            <option value="MESSAGE">SMS / Message</option>
            <option value="EMAIL">Email</option>
            <option value="SCREENSHOT">Screenshot</option>
            <option value="QR">QR Code</option>
          </select>
        </div>
      </div>

      {/* Scans List / Cards */}
      {filteredHistory.length === 0 ? (
        <div className="p-12 text-center rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card">
          <Clock className="w-12 h-12 text-slate-300 dark:text-slate-700 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800 dark:text-slate-200">
            No scans match your criteria
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
            {history.length === 0 
              ? 'Your previous checks will appear here once you analyze links, messages or screenshots.' 
              : 'Try clearing your search query or filter settings.'}
          </p>
          {setActiveTab && (
            <button
              onClick={() => setActiveTab('scanner')}
              className="mt-4 prahar-btn-primary py-2 px-5 text-xs font-bold"
            >
              <span>Check Something Suspicious</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {filteredHistory.map((scan) => {
            const isSafe = scan.riskLevel === 'SAFE';
            const isDanger = scan.riskLevel === 'HIGH_RISK';
            const isWarn = scan.riskLevel === 'SUSPICIOUS';

            return (
              <div
                key={scan.scanId}
                className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-card"
              >
                {/* Left info */}
                <div className="flex items-start sm:items-center gap-3.5 min-w-0 flex-1">
                  <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${
                    isDanger 
                      ? 'bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400' 
                      : isWarn 
                      ? 'bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400' 
                      : 'bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400'
                  }`}>
                    {isDanger ? <ShieldAlert className="w-6 h-6" /> : isWarn ? <AlertTriangle className="w-6 h-6" /> : <ShieldCheck className="w-6 h-6" />}
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap mb-1">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${
                        isDanger ? 'bg-red-600 text-white' : isWarn ? 'bg-amber-500 text-slate-950' : 'bg-emerald-600 text-white'
                      }`}>
                        {scan.riskLevel?.replace('_', ' ')}
                      </span>

                      {scan.threatCategory && scan.threatCategory !== 'BENIGN' && (
                        <span className="text-[10px] font-semibold text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded">
                          {scan.threatCategory.replace(/_/g, ' ')}
                        </span>
                      )}

                      <span className="text-[10px] text-slate-400 uppercase font-mono">
                        {scan.scanType || 'URL'} • {new Date(scan.createdAt).toLocaleDateString()}
                      </span>
                    </div>

                    <p className="text-xs font-mono font-bold text-slate-900 dark:text-white truncate">
                      {scan.url || scan.domain}
                    </p>
                  </div>
                </div>

                {/* Right score & actions */}
                <div className="flex items-center justify-between sm:justify-end gap-3 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100 dark:border-slate-800">
                  <div className="text-left sm:text-right">
                    <span className="text-[10px] font-bold uppercase text-slate-400 block">
                      Risk Score
                    </span>
                    <span className={`text-base font-black font-mono ${
                      isDanger ? 'text-red-600 dark:text-red-400' : isWarn ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'
                    }`}>
                      {scan.riskScore}<span className="text-xs font-normal text-slate-400">/100</span>
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => handleViewScan(scan)}
                      className="px-3 py-1.5 rounded-lg bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 hover:bg-blue-100 text-xs font-semibold flex items-center gap-1 transition-colors"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      <span>View</span>
                    </button>

                    <button
                      onClick={() => deleteScan(scan.scanId)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors"
                      title="Delete from history"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}
