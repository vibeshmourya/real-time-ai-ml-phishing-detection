import React, { useState } from 'react';
import { Shield, Link2, MessageSquare, Mail, QrCode, ArrowRight, Sparkles, Clipboard, Check, RefreshCw, AlertOctagon } from 'lucide-react';
import { useScan } from '../context/ScanContext';

const SAMPLE_CHIPS = [
  {
    label: '🏦 Fake HDFC Bank Link',
    type: 'URL',
    value: 'http://secure-login-hdfcbank.account-verify.tk/login.php',
    riskHint: 'High Risk'
  },
  {
    label: '🟢 Google Official',
    type: 'URL',
    value: 'https://www.google.com/search?q=cyber+security',
    riskHint: 'Safe'
  },
  {
    label: '📱 Fake KYC SMS',
    type: 'MESSAGE',
    value: 'URGENT: Your SBI Bank Account KYC expired. Update Aadhaar details immediately at http://sbi-kyc-reactivate.xyz to avoid permanent account block.',
    riskHint: 'High Risk'
  },
  {
    label: '⚡ Power Disconnection',
    type: 'MESSAGE',
    value: 'Dear Consumer, Electricity power will be disconnected tonight at 9.30 PM from the electricity office because previous month bill was not updated. Please call officer at 9876543210.',
    riskHint: 'High Risk'
  },
  {
    label: '🎁 Gift Card Reward',
    type: 'URL',
    value: 'http://free-iphone-reward.top/claim-prize?user=win',
    riskHint: 'Suspicious'
  }
];

export default function QuickScanner({ compact = false }) {
  const { executeScan, isScanning, showToast } = useScan();
  const [scanType, setScanType] = useState('URL');
  const [inputValue, setInputValue] = useState('');
  const [copied, setCopied] = useState(false);

  const handleScan = async (e) => {
    if (e) e.preventDefault();
    if (!inputValue.trim()) {
      showToast('Please paste or type a URL or message to scan', 'warn');
      return;
    }
    try {
      await executeScan(inputValue.trim(), scanType);
    } catch {
      // Handled in context
    }
  };

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setInputValue(text);
        // Auto-detect type
        if (/^https?:\/\//i.test(text.trim()) || text.trim().split(' ').length === 1) {
          setScanType('URL');
        } else {
          setScanType('MESSAGE');
        }
        showToast('Pasted from clipboard', 'info');
      }
    } catch {
      showToast('Unable to access clipboard automatically', 'warn');
    }
  };

  const loadSample = (sample) => {
    setScanType(sample.type);
    setInputValue(sample.value);
  };

  return (
    <div className={`w-full glass-panel p-5 sm:p-7 relative overflow-hidden border border-slate-700/70 shadow-2xl ${
      compact ? 'max-w-3xl' : 'max-w-4xl'
    } mx-auto`}>
      
      {/* Background ambient glow */}
      <div className="absolute -top-24 -right-24 w-72 h-72 bg-cyber-indigo/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-24 -left-24 w-72 h-72 bg-cyber-cyan/15 rounded-full blur-3xl pointer-events-none" />

      {/* Header / Mode Switcher */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-5">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-cyber-cyan" />
              Instant Threat Scanner
            </h2>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            AI-powered XAI detection for malicious links, phishing texts, and social engineering traps.
          </p>
        </div>

        {/* Input Format Tabs */}
        <div className="flex items-center gap-1 p-1 bg-dark-950/80 rounded-xl border border-slate-800 text-xs font-semibold self-stretch sm:self-auto justify-between sm:justify-start">
          <button
            type="button"
            onClick={() => { setScanType('URL'); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
              scanType === 'URL'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Link2 className="w-3.5 h-3.5" />
            <span>Link / URL</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('MESSAGE'); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
              scanType === 'MESSAGE'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>SMS / Text</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('EMAIL'); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
              scanType === 'EMAIL'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Mail className="w-3.5 h-3.5" />
            <span>Email</span>
          </button>
        </div>
      </div>

      {/* Main Input Form */}
      <form onSubmit={handleScan} className="space-y-4">
        <div className="relative">
          {scanType === 'URL' ? (
            <div className="relative flex items-center">
              <div className="absolute left-4 pointer-events-none text-slate-500">
                <Link2 className="w-5 h-5 text-indigo-400" />
              </div>
              <input
                type="text"
                id="url-scan-input"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                placeholder="Paste suspicious website link here (e.g. http://secure-login-hdfc.tk)..."
                className="w-full pl-12 pr-28 py-3.5 sm:py-4 bg-dark-950/90 border-2 border-slate-700/80 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/20 rounded-2xl text-sm sm:text-base text-white placeholder-slate-500 transition-all font-mono shadow-inner outline-none"
              />
              <button
                type="button"
                onClick={handlePaste}
                className="absolute right-3 px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-xs font-semibold flex items-center gap-1 transition-all"
                title="Paste from clipboard"
              >
                <Clipboard className="w-3.5 h-3.5 text-cyber-cyan" />
                <span className="hidden sm:inline">Paste</span>
              </button>
            </div>
          ) : (
            <div className="relative">
              <textarea
                rows={3}
                id="text-scan-input"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                placeholder={
                  scanType === 'MESSAGE'
                    ? 'Paste suspicious SMS, WhatsApp message, or Telegram text here...'
                    : 'Paste suspicious email subject and body content here...'
                }
                className="w-full p-4 pr-24 bg-dark-950/90 border-2 border-slate-700/80 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/20 rounded-2xl text-sm text-white placeholder-slate-500 transition-all shadow-inner outline-none resize-none font-sans"
              />
              <button
                type="button"
                onClick={handlePaste}
                className="absolute top-3 right-3 px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-xs font-semibold flex items-center gap-1 transition-all"
              >
                <Clipboard className="w-3.5 h-3.5 text-cyber-cyan" />
                <span>Paste</span>
              </button>
            </div>
          )}
        </div>

        {/* Action Button Row */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          
          {/* Quick Demo Test Samples */}
          <div className="flex items-center gap-1.5 flex-wrap w-full sm:w-auto">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 mr-1">
              Quick Test:
            </span>
            {SAMPLE_CHIPS.map((chip, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => loadSample(chip)}
                className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-indigo-900/40 hover:text-indigo-200 border border-slate-700 text-slate-300 transition-all active:scale-95"
              >
                {chip.label}
              </button>
            ))}
          </div>

          {/* Big Scan Button */}
          <button
            type="submit"
            disabled={isScanning || !inputValue.trim()}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-8 py-3.5 rounded-xl bg-gradient-to-r from-indigo-600 via-cyber-indigo to-cyber-cyan hover:from-indigo-500 hover:to-cyan-400 text-white font-extrabold text-sm shadow-glow-indigo disabled:opacity-50 disabled:cursor-not-allowed transition-all active:scale-95 shrink-0"
          >
            {isScanning ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin text-white" />
                <span>Analyzing Neural Signals...</span>
              </>
            ) : (
              <>
                <Shield className="w-4 h-4 text-white" />
                <span>Check Link Safety</span>
                <ArrowRight className="w-4 h-4 ml-1" />
              </>
            )}
          </button>
        </div>
      </form>

      {/* Safety & SSRF Shield Badge */}
      <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400">
        <div className="flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
          <span>SSRF Guard Shield & XAI Engine Online</span>
        </div>
        <div className="text-slate-400">
          Zero-logs privacy protected
        </div>
      </div>

    </div>
  );
}
