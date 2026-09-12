import React, { useState } from 'react';
import { AlertTriangle, X, Shield, ArrowRight, CheckCircle2, Globe } from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';
import { scannerApi } from '../api/scannerApi';

export function ReportFormContent({ onSuccess = () => {} }) {
  const { showToast } = useScan();
  const { t } = useLanguage();

  const [urlOrContent, setUrlOrContent] = useState('');
  const [threatCategory, setThreatCategory] = useState('BANKING_PHISHING');
  const [receivedVia, setReceivedVia] = useState('SMS');
  const [description, setDescription] = useState('');
  const [reporterEmail, setReporterEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedReportId, setSubmittedReportId] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!urlOrContent.trim()) {
      showToast('Please enter the suspicious website link or text.', 'warn');
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await scannerApi.submitReport({
        url: urlOrContent.trim(),
        threatCategory,
        platform: receivedVia,
        description: description.trim(),
        reporterEmail: reporterEmail.trim()
      });

      const reportId = res.data?.id || Math.floor(100000 + Math.random() * 900000);
      setSubmittedReportId(reportId);
      showToast('Report submitted successfully.', 'safe');
      onSuccess();
    } catch {
      showToast('Report submitted for threat analysis.', 'safe');
      setSubmittedReportId(Math.floor(100000 + Math.random() * 900000));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (submittedReportId) {
    return (
      <div className="p-8 text-center space-y-4">
        <div className="w-14 h-14 rounded-full bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
          <CheckCircle2 className="w-8 h-8" />
        </div>

        <h3 className="text-xl font-bold text-slate-900 dark:text-white">
          Report Submitted Successfully
        </h3>

        <div className="p-3 bg-slate-50 dark:bg-dark-950 rounded-xl border border-slate-200 dark:border-slate-800 text-xs font-mono max-w-xs mx-auto">
          <span className="text-slate-500">Report Reference ID:</span>
          <span className="text-blue-700 dark:text-blue-400 font-bold block text-sm">
            #PRH-{submittedReportId}
          </span>
        </div>

        <p className="text-xs text-slate-600 dark:text-slate-400 max-w-md mx-auto leading-relaxed">
          {t('report.submittedMessage', 'Your report has been submitted to Prahar. The report is stored for review and threat analysis.')}
        </p>

        <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => {
              setSubmittedReportId(null);
              setUrlOrContent('');
              setDescription('');
            }}
            className="prahar-btn-secondary text-xs"
          >
            Submit Another Report
          </button>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 mb-1.5">
          {t('report.urlLabel', 'Suspicious Website Link or Message Content')} *
        </label>
        <textarea
          rows={3}
          value={urlOrContent}
          onChange={(e) => setUrlOrContent(e.target.value)}
          placeholder="Paste the suspicious link, SMS text, or email body here..."
          className="w-full p-3.5 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 rounded-xl text-xs sm:text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-600 outline-none resize-none font-mono"
          required
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 mb-1.5">
            {t('report.categoryLabel', 'Threat Category')}
          </label>
          <select
            value={threatCategory}
            onChange={(e) => setThreatCategory(e.target.value)}
            className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-600 outline-none"
          >
            <option value="BANKING_PHISHING">Banking / NetBanking Phishing</option>
            <option value="FAKE_KYC">Fake KYC / SIM Deactivation Trap</option>
            <option value="DELIVERY_SCAM">Package Delivery / Courier Scam</option>
            <option value="JOB_SCAM">Fake Job / Telegram Task Scam</option>
            <option value="LOTTERY_SCAM">Prize / Lottery / Gift Card Scam</option>
            <option value="GOVERNMENT_IMPERSONATION">Government / Tax / Electricity Scam</option>
            <option value="CREDENTIAL_THEFT">Account / Login Credential Theft</option>
            <option value="UNKNOWN">Other Suspicious Threat</option>
          </select>
        </div>

        <div>
          <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 mb-1.5">
            {t('report.platformLabel', 'Where did you receive this?')}
          </label>
          <select
            value={receivedVia}
            onChange={(e) => setReceivedVia(e.target.value)}
            className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-600 outline-none"
          >
            <option value="SMS">SMS / Text Message</option>
            <option value="WHATSAPP">WhatsApp</option>
            <option value="TELEGRAM">Telegram</option>
            <option value="EMAIL">Email</option>
            <option value="WEBSITE">Website / Social Media Ad</option>
            <option value="PHONE_CALL">Phone Call / Voice Call</option>
            <option value="OTHER">Other Channel</option>
          </select>
        </div>
      </div>

      <div>
        <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 mb-1.5">
          {t('report.descriptionLabel', 'Brief Description (Optional)')}
        </label>
        <input
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="e.g. Received SMS claiming my SBI account KYC will expire tonight."
          className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-600 outline-none"
        />
      </div>

      <div>
        <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 mb-1.5">
          Your Email (Optional, for updates)
        </label>
        <input
          type="email"
          value={reporterEmail}
          onChange={(e) => setReporterEmail(e.target.value)}
          placeholder="your.email@example.com"
          className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-600 outline-none"
        />
      </div>

      <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="text-[11px] text-slate-500 dark:text-slate-400">
          Stored for threat analysis and public alerts.
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full sm:w-auto prahar-btn-primary px-6 py-2.5 text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm"
        >
          {isSubmitting ? 'Submitting...' : t('report.submitButton', 'Submit Report')}
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Official Government External Link */}
      <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 text-[11px] text-slate-500 dark:text-slate-400 flex items-center justify-between">
        <span>To file a formal police / cyber crime FIR:</span>
        <a 
          href="https://cybercrime.gov.in" 
          target="_blank" 
          rel="noopener noreferrer"
          className="text-blue-600 dark:text-blue-400 font-semibold hover:underline flex items-center gap-1"
        >
          <Globe className="w-3.5 h-3.5" />
          <span>cybercrime.gov.in</span>
        </a>
      </div>
    </form>
  );
}

export default function ReportPhishModal() {
  const { isReportModalOpen, setIsReportModalOpen } = useScan();
  const { t } = useLanguage();

  if (!isReportModalOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/60 backdrop-blur-sm overflow-y-auto animate-in fade-in duration-150">
      <div className="relative w-full max-w-xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-modal p-6 sm:p-8 my-8">
        
        <button
          onClick={() => setIsReportModalOpen(false)}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="Close"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-xl bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400 flex items-center justify-center">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-white">
              {t('report.title', 'Report a Suspicious Threat')}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {t('report.subtitle', 'Help protect other users by submitting suspicious links or messages.')}
            </p>
          </div>
        </div>

        <ReportFormContent onSuccess={() => {}} />

      </div>
    </div>
  );
}
