import React from 'react';
import { ReportFormContent } from '../components/ReportPhishModal';
import { AlertTriangle, ShieldCheck, FileText, Info } from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';

export default function ReportPage() {
  const { t } = useLanguage();

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2">
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-amber-50 dark:bg-amber-950 text-amber-700 dark:text-amber-400 border border-amber-200 dark:border-amber-800">
            Community Safety
          </span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-1">
          {t('report.title', 'Report a Suspicious Threat')}
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-0.5">
          {t('report.subtitle', 'Help protect other users by submitting suspicious links or messages.')}
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* Left Form (8 cols) */}
        <div className="lg:col-span-8 p-6 sm:p-8 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card">
          <ReportFormContent />
        </div>

        {/* Right Info Sidebar (4 cols) */}
        <div className="lg:col-span-4 space-y-4">
          
          <div className="p-5 rounded-2xl bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 space-y-2 text-xs">
            <h4 className="font-bold text-blue-900 dark:text-blue-200 flex items-center gap-1.5">
              <Info className="w-4 h-4 text-blue-600" />
              How your report is used:
            </h4>
            <p className="text-slate-600 dark:text-slate-300 leading-relaxed">
              When you submit a suspicious link, Prahar’s analysts and detection models review the domain structure to improve detection accuracy for all users.
            </p>
          </div>

          <div className="p-5 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-2 text-xs text-slate-600 dark:text-slate-400">
            <h4 className="font-bold text-slate-800 dark:text-slate-200">
              Privacy Notice:
            </h4>
            <p className="leading-relaxed">
              Do not include personal financial details, account passwords, or 6-digit OTPs in report descriptions.
            </p>
          </div>

        </div>

      </div>

    </div>
  );
}
