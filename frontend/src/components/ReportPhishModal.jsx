import React, { useState } from 'react';
import { AlertTriangle, Send, X, ShieldAlert, CheckCircle2, Globe } from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { scannerApi } from '../api/scannerApi';

export default function ReportPhishModal() {
  const { isReportModalOpen, setIsReportModalOpen, showToast } = useScan();
  const [url, setUrl] = useState('');
  const [category, setCategory] = useState('BANKING_PHISHING');
  const [description, setDescription] = useState('');
  const [reporterEmail, setReporterEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  if (!isReportModalOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!url.trim()) {
      showToast('Please enter the suspicious URL or message', 'warn');
      return;
    }
    setIsSubmitting(true);
    try {
      await scannerApi.submitReport({
        url: url.trim(),
        threatCategory: category,
        description: description.trim(),
        reporterEmail: reporterEmail.trim() || 'anonymous@phishshield.gov'
      });
      setIsSuccess(true);
      showToast('Phishing report dispatched to CERT-In & Global Threat Telemetry', 'safe');
      setTimeout(() => {
        setIsSuccess(false);
        setIsReportModalOpen(false);
        setUrl('');
        setDescription('');
      }, 2000);
    } catch {
      showToast('Failed to submit report. Please retry.', 'danger');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-dark-950/85 backdrop-blur-md animate-in fade-in">
      <div className="relative w-full max-w-lg glass-panel p-6 sm:p-7 border border-amber-500/50 shadow-glow-warn bg-dark-900/95">
        
        {/* Close button */}
        <button
          onClick={() => setIsReportModalOpen(false)}
          className="absolute top-4 right-4 p-2 rounded-xl bg-slate-800 text-slate-400 hover:text-white border border-slate-700"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center shrink-0 border border-amber-500/30">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">
              Report Suspicious Threat
            </h3>
            <p className="text-xs text-slate-400">
              Submit malicious links or scam texts to protect other digital citizens.
            </p>
          </div>
        </div>

        {isSuccess ? (
          <div className="py-8 text-center space-y-2">
            <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto animate-bounce" />
            <h4 className="text-base font-bold text-white">Report Successfully Received!</h4>
            <p className="text-xs text-slate-400">
              Thank you for keeping the cyber ecosystem safe. Our automated sandbox and CERT telemetry are analyzing this threat.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-1">
                Suspicious URL or Scam Message *
              </label>
              <input
                type="text"
                required
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="http://phishing-site-example.tk or scam message..."
                className="w-full px-3.5 py-2.5 bg-dark-950 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-1">
                Threat Category
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-dark-950 border border-slate-700 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-amber-500 cursor-pointer"
              >
                <option value="BANKING_PHISHING">Banking & NetBanking Phishing</option>
                <option value="KYC_FRAUD">Fake KYC / SIM Deactivation</option>
                <option value="UTILITY_FRAUD">Electricity / Water Bill Threat</option>
                <option value="DELIVERY_SCAM">India Post / Parcel Delivery Trap</option>
                <option value="LOTTERY_SCAM">Lottery / Part-Time Job Fraud</option>
                <option value="GOVERNMENT_IMPERSONATION">Income Tax / Police Impersonation</option>
                <option value="OTHER">Other Malicious Link</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-1">
                Additional Context (Optional)
              </label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Where did you receive this? (e.g. SMS from +91-9876543210 claiming electricity cut)..."
                className="w-full p-3 bg-dark-950 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 resize-none"
              />
            </div>

            <div className="pt-2 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsReportModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="flex items-center gap-1.5 px-5 py-2 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-slate-950 text-xs font-bold shadow-md disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{isSubmitting ? 'Submitting...' : 'Dispatch Report'}</span>
              </button>
            </div>
          </form>
        )}

      </div>
    </div>
  );
}
