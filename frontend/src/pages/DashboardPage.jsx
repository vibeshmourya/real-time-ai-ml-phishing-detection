import React from 'react';
import QuickScanner from '../components/QuickScanner';
import { 
  ShieldAlert, ShieldCheck, AlertTriangle, ArrowRight, 
  Clock, Lock, CheckCircle2, AlertCircle, Eye, HelpCircle
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

export default function DashboardPage({ setActiveTab }) {
  const { history, setActiveVerdict, setIsVerdictModalOpen, openEmergencyPlaybook } = useScan();
  const { t } = useLanguage();

  // Maximum of three items as explicitly requested
  const recentThreeScans = history.slice(0, 3);

  const handleViewScan = (scan) => {
    setActiveVerdict(scan);
    setIsVerdictModalOpen(true);
  };

  return (
    <div className="w-full pb-16">
      
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 space-y-10 pt-6 sm:pt-10">
        
        {/* Main Content Hero */}
        <div className="text-center space-y-2 max-w-xl mx-auto">
          <h1 className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-white tracking-tight">
            {t('hero.title', 'Check before you trust.')}
          </h1>
          <p className="text-sm sm:text-base text-slate-600 dark:text-slate-400">
            {t('hero.subtitle', 'Check a suspicious link or message before you open it.')}
          </p>
        </div>

        {/* Prominent Scan Panel */}
        <section>
          <QuickScanner />
        </section>

        {/* Below Scanner: Only Recent Scans (Max 3), "What to do if you clicked?", and Useful Safety Tips */}
        <div className="space-y-8 pt-2">
          
          {/* Recent Scans Section (Max 3) */}
          <section className="bg-white dark:bg-dark-900 border border-slate-200/90 dark:border-slate-800 rounded-3xl p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-blue-600" />
                <h2 className="text-sm font-bold text-slate-900 dark:text-white">
                  {t('dashboard.recentChecks', 'Recent Checks')}
                </h2>
              </div>

              {history.length > 3 && (
                <button
                  type="button"
                  onClick={() => setActiveTab('history')}
                  className="text-xs font-semibold text-blue-700 dark:text-blue-400 hover:underline flex items-center gap-1"
                >
                  <span>{t('dashboard.viewAllScans', { count: history.length }, `View all scans (${history.length})`)}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {recentThreeScans.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-400">
                {t('dashboard.noRecentChecks', 'No recent checks. Paste a link or message above to get an instant safety verdict.')}
              </div>
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {recentThreeScans.map((scan) => {
                  const isDanger = scan.riskLevel === 'HIGH_RISK';
                  const isWarn = scan.riskLevel === 'SUSPICIOUS';

                  return (
                    <div
                      key={scan.scanId}
                      onClick={() => handleViewScan(scan)}
                      className="py-3 flex items-center justify-between gap-3 hover:bg-slate-50/60 dark:hover:bg-dark-950/60 px-2 rounded-xl cursor-pointer transition-colors"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className={`p-1.5 rounded-lg shrink-0 ${
                          isDanger 
                            ? 'bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400' 
                            : isWarn 
                            ? 'bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400' 
                            : 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400'
                        }`}>
                          {isDanger ? (
                            <ShieldAlert className="w-4 h-4" />
                          ) : isWarn ? (
                            <AlertTriangle className="w-4 h-4" />
                          ) : (
                            <ShieldCheck className="w-4 h-4" />
                          )}
                        </div>

                        <div className="min-w-0">
                          <p className="font-mono text-xs font-semibold text-slate-900 dark:text-white truncate">
                            {scan.url || scan.domain}
                          </p>
                          <p className="text-[11px] text-slate-400">
                            {isDanger ? 'Dangerous' : isWarn ? 'Suspicious' : 'No obvious threats'} • {scan.scanType || 'Link'}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <span className={`text-xs font-mono font-bold ${
                          isDanger ? 'text-red-600 dark:text-red-400' : isWarn ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'
                        }`}>
                          {scan.riskScore}/100
                        </span>
                        <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          {/* Small "What to do if you clicked?" Link */}
          <div className="flex items-center justify-between p-4 rounded-2xl bg-amber-50/70 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/40 text-xs">
            <div className="flex items-center gap-2.5">
              <AlertTriangle className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0" />
              <span className="font-semibold text-amber-950 dark:text-amber-200">
                {t('dashboard.clickedPromptTitle', 'Already opened a suspicious link or shared details?')}
              </span>
            </div>

            <button
              type="button"
              onClick={() => openEmergencyPlaybook('BANKING_PHISHING')}
              className="font-bold text-amber-900 dark:text-amber-300 hover:underline flex items-center gap-1 shrink-0 ml-2"
            >
              <span>{t('dashboard.clickedPromptDesc', 'What to do if you clicked →')}</span>
            </button>
          </div>

          {/* Useful Safety Tips (3 Simple Cards) */}
          <section className="space-y-3">
            <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              {t('dashboard.safetyTipsTitle', 'Digital Safety Habits')}
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5">
              
              <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200/90 dark:border-slate-800 space-y-1.5 shadow-xs">
                <div className="flex items-center gap-2 font-bold text-xs text-slate-900 dark:text-white">
                  <span className="w-5 h-5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-600 flex items-center justify-center font-bold text-[11px]">
                    1
                  </span>
                  <span>{t('dashboard.safetyTip1Title', 'Look beyond HTTPS')}</span>
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">
                  {t('dashboard.safetyTip1Desc', 'The padlock icon only encrypts transit. Phishing sites also get free SSL certificates. Always verify the domain name spelling.')}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200/90 dark:border-slate-800 space-y-1.5 shadow-xs">
                <div className="flex items-center gap-2 font-bold text-xs text-slate-900 dark:text-white">
                  <span className="w-5 h-5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-600 flex items-center justify-center font-bold text-[11px]">
                    2
                  </span>
                  <span>{t('dashboard.safetyTip2Title', 'Never share bank OTPs')}</span>
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">
                  {t('dashboard.safetyTip2Desc', 'Banks never ask for OTPs or login passwords via SMS links. If a message threatens an account freeze within hours, it is a scam.')}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200/90 dark:border-slate-800 space-y-1.5 shadow-xs">
                <div className="flex items-center gap-2 font-bold text-xs text-slate-900 dark:text-white">
                  <span className="w-5 h-5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-600 flex items-center justify-center font-bold text-[11px]">
                    3
                  </span>
                  <span>{t('dashboard.safetyTip3Title', 'UPI PIN is only to pay')}</span>
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">
                  {t('dashboard.safetyTip3Desc', 'Entering your UPI PIN is never required to receive money, cashback, or rewards. Scanning a QR code to receive money is always fraud.')}
                </p>
              </div>

            </div>
          </section>

        </div>

      </div>

    </div>
  );
}
