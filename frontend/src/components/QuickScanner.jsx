import React, { useState, useRef } from 'react';
import { 
  Shield, Link2, MessageSquare, Image as ImageIcon, 
  QrCode, Clipboard, RefreshCw, Upload, X, Lock, ArrowRight, CheckCircle2
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

export default function QuickScanner({ compact = false }) {
  const { executeScan, isScanning, showToast } = useScan();
  const { t } = useLanguage();

  const sampleChips = [
    {
      label: t('scanner.sampleFakeBank', '🏦 Fake HDFC Bank Link'),
      type: 'URL',
      value: 'http://secure-login-hdfcbank.account-verify.tk/login.php'
    },
    {
      label: t('scanner.sampleUrgentKyc', '📱 Urgent KYC SMS'),
      type: 'MESSAGE',
      value: 'URGENT: Your SBI Bank Account KYC expired. Update Aadhaar details immediately at http://sbi-kyc-reactivate.xyz to avoid account block.'
    },
    {
      label: t('scanner.sampleElectricity', '⚡ Electricity Cutoff Notice'),
      type: 'MESSAGE',
      value: 'Dear Consumer, Electricity power will be disconnected tonight at 9.30 PM because previous bill was not updated. Call officer at 9876543210.'
    },
    {
      label: t('scanner.sampleGoogleSafe', '🟢 Google Official (Safe)'),
      type: 'URL',
      value: 'https://www.google.com/search?q=cyber+security'
    }
  ];

  const [scanType, setScanType] = useState('URL'); // 'URL' | 'MESSAGE' | 'SCREENSHOT' | 'QR'
  const [inputValue, setInputValue] = useState('');
  
  // File upload state for Screenshot & QR
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [decodedQrUrl, setDecodedQrUrl] = useState('');
  const [ocrText, setOcrText] = useState('');
  const [isProcessingFile, setIsProcessingFile] = useState(false);
  
  const fileInputRef = useRef(null);

  const handleScan = async (e) => {
    if (e) e.preventDefault();

    if (scanType === 'SCREENSHOT') {
      if (!selectedFile && !inputValue.trim()) {
        showToast('Please upload an image screenshot or enter text to check.', 'warn');
        return;
      }
      try {
        await executeScan(ocrText || inputValue.trim(), 'SCREENSHOT', { file: selectedFile });
      } catch (err) {
        // Handled in context
      }
      return;
    }

    if (scanType === 'QR') {
      const targetUrl = decodedQrUrl || inputValue.trim();
      if (!targetUrl && !selectedFile) {
        showToast('Please upload a QR code image or paste the destination link.', 'warn');
        return;
      }
      try {
        await executeScan(targetUrl, 'QR', { file: selectedFile });
      } catch (err) {
        // Handled in context
      }
      return;
    }

    if (!inputValue.trim()) {
      showToast('Please paste a link or message to check.', 'warn');
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
        if (/^https?:\/\//i.test(text.trim()) && scanType === 'MESSAGE') {
          setScanType('URL');
        }
        showToast('Pasted from clipboard', 'info');
      }
    } catch {
      showToast('Please paste text manually using Ctrl+V or right-click.', 'warn');
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      showToast('Please upload a PNG, JPG, or WEBP image file.', 'warn');
      return;
    }

    setSelectedFile(file);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    setIsProcessingFile(false);
    showToast('Image loaded. Click Check for Threats to run deep analysis.', 'info');
  };

  const clearFile = () => {
    setSelectedFile(null);
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setDecodedQrUrl('');
    setOcrText('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const loadSample = (sample) => {
    clearFile();
    setScanType(sample.type);
    setInputValue(sample.value);
  };

  return (
    <div className={`w-full bg-white dark:bg-dark-900 border border-slate-200/90 dark:border-slate-800 rounded-3xl p-6 sm:p-8 ${
      compact ? 'max-w-2xl' : 'max-w-3xl'
    } mx-auto shadow-sm`}>
      
      {/* Channel Tabs */}
      <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-4 mb-5 flex-wrap gap-2">
        <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
          {t('scanner.selectInputType', 'Select input type:')}
        </span>

        <div className="flex items-center gap-1 p-1 bg-slate-100 dark:bg-dark-950 rounded-xl text-xs font-medium">
          <button
            type="button"
            onClick={() => { setScanType('URL'); clearFile(); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
              scanType === 'URL'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            <Link2 className="w-3.5 h-3.5" />
            <span>{t('scanner.tabLink', 'Link')}</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('MESSAGE'); clearFile(); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
              scanType === 'MESSAGE'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>{t('scanner.tabMessage', 'Message')}</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('SCREENSHOT'); setInputValue(''); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
              scanType === 'SCREENSHOT'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            <ImageIcon className="w-3.5 h-3.5" />
            <span>{t('scanner.tabScreenshot', 'Screenshot')}</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('QR'); setInputValue(''); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
              scanType === 'QR'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-xs font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>{t('scanner.tabQr', 'QR Code')}</span>
          </button>
        </div>
      </div>

      {/* Main Input Form */}
      <form onSubmit={handleScan} className="space-y-4">
        
        {/* URL or Message Input */}
        {(scanType === 'URL' || scanType === 'MESSAGE') && (
          <div className="relative">
            {scanType === 'URL' ? (
              <div className="relative flex items-center">
                <input
                  type="text"
                  id="url-scan-input"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  placeholder={t('scanner.linkPlaceholder', 'Paste a suspicious website link (e.g. https://secure-login-hdfc.tk)...')}
                  className="w-full pl-4 pr-24 py-3.5 sm:py-4 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 rounded-2xl text-sm sm:text-base text-slate-900 dark:text-white placeholder-slate-400 font-mono outline-none transition-all shadow-inner"
                />
                <button
                  type="button"
                  onClick={handlePaste}
                  className="absolute right-3 px-3 py-1.5 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-100 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-xs"
                >
                  <Clipboard className="w-3.5 h-3.5 text-blue-600" />
                  <span>{t('scanner.pasteButton', 'Paste')}</span>
                </button>
              </div>
            ) : (
              <div className="relative">
                <textarea
                  rows={3}
                  id="message-scan-input"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  placeholder={t('scanner.messagePlaceholder', 'Paste the suspicious SMS, WhatsApp message, or Telegram text...')}
                  className="w-full p-4 pr-24 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 rounded-2xl text-sm text-slate-900 dark:text-white placeholder-slate-400 outline-none resize-none transition-all shadow-inner"
                />
                <button
                  type="button"
                  onClick={handlePaste}
                  className="absolute top-3 right-3 px-3 py-1.5 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-100 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-xs"
                >
                  <Clipboard className="w-3.5 h-3.5 text-blue-600" />
                  <span>{t('scanner.pasteButton', 'Paste')}</span>
                </button>
              </div>
            )}
          </div>
        )}

        {/* Screenshot Upload */}
        {scanType === 'SCREENSHOT' && (
          <div>
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              accept="image/png, image/jpeg, image/webp"
              className="hidden"
            />

            {!selectedFile ? (
              <div
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-blue-600 rounded-2xl p-7 text-center cursor-pointer transition-colors bg-slate-50/50 dark:bg-dark-950/50"
              >
                <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-950 text-blue-600 flex items-center justify-center mx-auto mb-2">
                  <Upload className="w-5 h-5" />
                </div>
                <p className="text-sm font-bold text-slate-800 dark:text-slate-200">
                  {t('scanner.uploadScreenshot', 'Upload message screenshot')}
                </p>
                <p className="text-xs text-slate-400 mt-0.5">
                  {t('scanner.uploadScreenshotDesc', 'PNG, JPG, or WEBP image of a suspicious message or webpage')}
                </p>
              </div>
            ) : (
              <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  {previewUrl && (
                    <img
                      src={previewUrl}
                      alt="Uploaded preview"
                      className="w-12 h-12 object-cover rounded-xl border border-slate-200 dark:border-slate-700 shrink-0"
                    />
                  )}
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-slate-900 dark:text-white truncate">
                      {selectedFile.name}
                    </p>
                    <p className="text-[11px] text-slate-400">
                      {(selectedFile.size / 1024).toFixed(1)} KB • {isProcessingFile ? t('common.loading', 'Loading...') : t('common.ready', 'Ready for threat check')}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={clearFile}
                  className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        )}

        {/* QR Code Upload */}
        {scanType === 'QR' && (
          <div className="space-y-2.5">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              accept="image/png, image/jpeg, image/webp"
              className="hidden"
            />

            {!selectedFile ? (
              <div
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-blue-600 rounded-2xl p-7 text-center cursor-pointer transition-colors bg-slate-50/50 dark:bg-dark-950/50"
              >
                <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-950 text-blue-600 flex items-center justify-center mx-auto mb-2">
                  <QrCode className="w-5 h-5" />
                </div>
                <p className="text-sm font-bold text-slate-800 dark:text-slate-200">
                  {t('scanner.uploadQr', 'Upload QR code image')}
                </p>
                <p className="text-xs text-slate-400 mt-0.5">
                  {t('scanner.uploadQrDesc', 'Prahar will decode the destination URL and inspect it for threats')}
                </p>
              </div>
            ) : (
              <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  {previewUrl && (
                    <img
                      src={previewUrl}
                      alt="QR preview"
                      className="w-12 h-12 object-cover rounded-xl border border-slate-200 dark:border-slate-700 shrink-0"
                    />
                  )}
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-slate-900 dark:text-white truncate">
                      {selectedFile.name}
                    </p>
                    <p className="text-[11px] text-emerald-600 dark:text-emerald-400 truncate">
                      Decoded: {decodedQrUrl || 'Ready to analyze'}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={clearFile}
                  className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        )}

        {/* Action Controls & Primary Button */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-2">
          
          {/* Quick Examples */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-[11px] font-semibold text-slate-400">
              {t('scanner.sampleTests', 'Try sample:')}
            </span>
            {sampleChips.map((chip, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => loadSample(chip)}
                className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-blue-50 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 hover:text-blue-700 transition-colors border border-slate-200 dark:border-slate-700"
              >
                {chip.label}
              </button>
            ))}
          </div>

          {/* Primary Action Button: "Check for threats" */}
          <button
            type="submit"
            disabled={isScanning || isProcessingFile}
            className="prahar-btn-primary px-8 py-3.5 rounded-2xl text-sm font-bold shadow-md hover:shadow-lg transition-all active:scale-95 shrink-0"
          >
            {isScanning ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin text-white" />
                <span>{t('scanner.checkingButton', 'Checking for threats...')}</span>
              </>
            ) : (
              <>
                <Shield className="w-4 h-4 text-white" />
                <span>{t('scanner.checkButton', 'Check for threats')}</span>
              </>
            )}
          </button>

        </div>

      </form>

      {/* Privacy Note */}
      <div className="mt-5 pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-[11px] text-slate-400">
        <div className="flex items-center gap-1.5">
          <Lock className="w-3.5 h-3.5 text-emerald-600" />
          <span>{t('scanner.privacyNote', 'Prahar inspects submitted content in real-time for threat markers. Passwords, OTPs, or private data are never stored or shared.')}</span>
        </div>
      </div>

    </div>
  );
}
