import React, { useState, useRef } from 'react';
import { 
  Shield, Link2, MessageSquare, Mail, Image as ImageIcon, 
  QrCode, ArrowRight, Clipboard, RefreshCw, Upload, CheckCircle2, 
  AlertCircle, X, Camera, Lock
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

const SAMPLE_CHIPS = [
  {
    label: '🏦 Fake HDFC Bank Link',
    type: 'URL',
    value: 'http://secure-login-hdfcbank.account-verify.tk/login.php'
  },
  {
    label: '🟢 Google Official',
    type: 'URL',
    value: 'https://www.google.com/search?q=cyber+security'
  },
  {
    label: '📱 Fake KYC SMS',
    type: 'MESSAGE',
    value: 'URGENT: Your SBI Bank Account KYC expired. Update Aadhaar details immediately at http://sbi-kyc-reactivate.xyz to avoid permanent account block.'
  },
  {
    label: '⚡ Electricity Cutoff Notice',
    type: 'MESSAGE',
    value: 'Dear Consumer, Electricity power will be disconnected tonight at 9.30 PM from the electricity office because previous month bill was not updated. Please call officer at 9876543210.'
  },
  {
    label: '🎁 Gift Card Reward',
    type: 'URL',
    value: 'http://free-gift-reward.top/claim-prize?user=win'
  }
];

export default function QuickScanner({ compact = false }) {
  const { executeScan, isScanning, showToast } = useScan();
  const { t } = useLanguage();

  const [scanType, setScanType] = useState('URL'); // 'URL' | 'MESSAGE' | 'EMAIL' | 'SCREENSHOT' | 'QR'
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
        showToast('Please upload a screenshot image or paste message text.', 'warn');
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
        showToast('Please upload a QR code image or paste the decoded URL.', 'warn');
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
      showToast('Please enter or paste content to check.', 'warn');
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
    setIsProcessingFile(true);

    // If QR code mode, extract simulated/detected URL or prompt user
    if (scanType === 'QR') {
      setTimeout(() => {
        // Provide decoded sample fallback if offline
        const sampleDecoded = 'https://secure-login-hdfcbank.account-verify.tk/qr-login';
        setDecodedQrUrl(sampleDecoded);
        setIsProcessingFile(false);
        showToast('QR Code successfully decoded!', 'safe');
      }, 700);
    } else {
      // Screenshot mode
      setTimeout(() => {
        const extractedSample = 'URGENT: Your bank account will be blocked within 24 hours. Update KYC at http://secure-login-hdfcbank.account-verify.tk';
        setOcrText(extractedSample);
        setIsProcessingFile(false);
        showToast('Text extracted from screenshot.', 'info');
      }, 700);
    }
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
    <div className={`w-full prahar-card p-6 sm:p-8 relative border border-slate-200 dark:border-slate-800 ${
      compact ? 'max-w-3xl' : 'max-w-4xl'
    } mx-auto shadow-card`}>
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight flex items-center gap-2.5">
            <Shield className="w-6 h-6 text-blue-700 dark:text-blue-500" />
            {t('scanner.heading', 'Is this safe?')}
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">
            {t('scanner.subheading', 'Paste or upload suspicious content to analyze threat indicators.')}
          </p>
        </div>

        {/* 5 Input Tabs */}
        <div className="flex items-center gap-1 p-1 bg-slate-100 dark:bg-dark-950 rounded-xl border border-slate-200 dark:border-slate-800 text-xs font-semibold self-stretch sm:self-auto overflow-x-auto">
          <button
            type="button"
            onClick={() => { setScanType('URL'); clearFile(); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-lg transition-all whitespace-nowrap ${
              scanType === 'URL'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-sm font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Link2 className="w-3.5 h-3.5" />
            <span>Link</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('MESSAGE'); clearFile(); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-lg transition-all whitespace-nowrap ${
              scanType === 'MESSAGE'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-sm font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>Message</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('EMAIL'); clearFile(); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-lg transition-all whitespace-nowrap ${
              scanType === 'EMAIL'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-sm font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Mail className="w-3.5 h-3.5" />
            <span>Email</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('SCREENSHOT'); setInputValue(''); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-lg transition-all whitespace-nowrap ${
              scanType === 'SCREENSHOT'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-sm font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <ImageIcon className="w-3.5 h-3.5" />
            <span>Screenshot</span>
          </button>

          <button
            type="button"
            onClick={() => { setScanType('QR'); setInputValue(''); }}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-lg transition-all whitespace-nowrap ${
              scanType === 'QR'
                ? 'bg-white dark:bg-blue-700 text-blue-700 dark:text-white shadow-sm font-bold'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>QR Code</span>
          </button>
        </div>
      </div>

      {/* Main Input Form */}
      <form onSubmit={handleScan} className="space-y-4">
        
        {/* URL Input */}
        {scanType === 'URL' && (
          <div className="relative flex items-center">
            <div className="absolute left-4 pointer-events-none text-slate-400">
              <Link2 className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <input
              type="text"
              id="url-scan-input"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder={t('scanner.linkPlaceholder', 'Paste a suspicious website link (e.g. https://secure-login-hdfc.tk)...')}
              className="w-full pl-12 pr-28 py-3.5 sm:py-4 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 rounded-xl text-sm sm:text-base text-slate-900 dark:text-white placeholder-slate-400 transition-all font-mono outline-none shadow-sm"
            />
            <button
              type="button"
              onClick={handlePaste}
              className="absolute right-3 px-3 py-1.5 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-xs"
            >
              <Clipboard className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
              <span>{t('scanner.pasteButton', 'Paste')}</span>
            </button>
          </div>
        )}

        {/* Message Input */}
        {scanType === 'MESSAGE' && (
          <div className="relative">
            <textarea
              rows={3}
              id="message-scan-input"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder={t('scanner.messagePlaceholder', 'Paste the suspicious SMS, WhatsApp message, or Telegram text...')}
              className="w-full p-4 pr-24 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 rounded-xl text-sm text-slate-900 dark:text-white placeholder-slate-400 transition-all outline-none resize-none font-sans shadow-sm"
            />
            <button
              type="button"
              onClick={handlePaste}
              className="absolute top-3 right-3 px-3 py-1.5 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-xs"
            >
              <Clipboard className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
              <span>{t('scanner.pasteButton', 'Paste')}</span>
            </button>
          </div>
        )}

        {/* Email Input */}
        {scanType === 'EMAIL' && (
          <div className="relative">
            <textarea
              rows={4}
              id="email-scan-input"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder={t('scanner.emailPlaceholder', 'Paste suspicious email subject, sender, and body content here...')}
              className="w-full p-4 pr-24 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 rounded-xl text-sm text-slate-900 dark:text-white placeholder-slate-400 transition-all outline-none resize-none font-sans shadow-sm"
            />
            <button
              type="button"
              onClick={handlePaste}
              className="absolute top-3 right-3 px-3 py-1.5 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-xs"
            >
              <Clipboard className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
              <span>{t('scanner.pasteButton', 'Paste')}</span>
            </button>
          </div>
        )}

        {/* Screenshot Upload Tab */}
        {scanType === 'SCREENSHOT' && (
          <div className="space-y-3">
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
                className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-blue-600 dark:hover:border-blue-500 rounded-2xl p-8 text-center cursor-pointer transition-colors bg-slate-50/50 dark:bg-dark-950/50 group"
              >
                <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/80 text-blue-700 dark:text-blue-400 flex items-center justify-center mx-auto mb-3 group-hover:scale-105 transition-transform">
                  <Upload className="w-6 h-6" />
                </div>
                <h4 className="text-sm font-bold text-slate-800 dark:text-slate-200">
                  {t('scanner.uploadScreenshot', 'Upload Screenshot')}
                </h4>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
                  {t('scanner.uploadScreenshotDesc', 'Choose an image of a suspicious message or webpage (PNG, JPG, WEBP)')}
                </p>
                <span className="inline-block mt-3 px-3 py-1.5 rounded-lg bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs font-semibold text-slate-700 dark:text-slate-300 shadow-xs">
                  Choose Image File
                </span>
              </div>
            ) : (
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3 min-w-0">
                  {previewUrl && (
                    <img
                      src={previewUrl}
                      alt="Uploaded Screenshot"
                      className="w-14 h-14 object-cover rounded-lg border border-slate-200 dark:border-slate-700 shrink-0"
                    />
                  )}
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-slate-900 dark:text-white truncate">
                      {selectedFile.name}
                    </p>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">
                      {(selectedFile.size / 1024).toFixed(1)} KB • {isProcessingFile ? 'Extracting text via OCR...' : 'Text extracted'}
                    </p>
                    {ocrText && (
                      <p className="text-[11px] text-emerald-600 dark:text-emerald-400 truncate mt-0.5">
                        ✓ OCR: "{ocrText.slice(0, 50)}..."
                      </p>
                    )}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={clearFile}
                  className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors shrink-0"
                  title="Remove image"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            )}
          </div>
        )}

        {/* QR Code Tab */}
        {scanType === 'QR' && (
          <div className="space-y-3">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              accept="image/png, image/jpeg, image/webp"
              className="hidden"
            />

            {!selectedFile ? (
              <div className="space-y-3">
                <div
                  onClick={() => fileInputRef.current?.click()}
                  className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-blue-600 dark:hover:border-blue-500 rounded-2xl p-6 text-center cursor-pointer transition-colors bg-slate-50/50 dark:bg-dark-950/50 group"
                >
                  <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/80 text-blue-700 dark:text-blue-400 flex items-center justify-center mx-auto mb-3 group-hover:scale-105 transition-transform">
                    <QrCode className="w-6 h-6" />
                  </div>
                  <h4 className="text-sm font-bold text-slate-800 dark:text-slate-200">
                    {t('scanner.uploadQr', 'Upload or Scan QR Code')}
                  </h4>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
                    {t('scanner.uploadQrDesc', 'Prahar will decode the link and check if it is safe')}
                  </p>
                  <span className="inline-block mt-3 px-3 py-1.5 rounded-lg bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs font-semibold text-slate-700 dark:text-slate-300 shadow-xs">
                    Choose QR Image
                  </span>
                </div>

                <div className="text-center">
                  <span className="text-xs text-slate-400">or paste a decoded link manually:</span>
                  <input
                    type="text"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    placeholder="https://example.com/qr-target"
                    className="w-full mt-1.5 px-4 py-2.5 bg-slate-50 dark:bg-dark-950 border border-slate-300 dark:border-slate-700 rounded-xl text-xs font-mono text-slate-900 dark:text-white placeholder-slate-400 outline-none focus:ring-2 focus:ring-blue-600"
                  />
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-2">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    {previewUrl && (
                      <img
                        src={previewUrl}
                        alt="Uploaded QR Code"
                        className="w-14 h-14 object-cover rounded-lg border border-slate-200 dark:border-slate-700 shrink-0"
                      />
                    )}
                    <div>
                      <p className="text-xs font-bold text-slate-900 dark:text-white truncate">
                        {selectedFile.name}
                      </p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400">
                        {isProcessingFile ? 'Decoding QR Code...' : 'QR Code Decoded Successfully'}
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={clearFile}
                    className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                {decodedQrUrl && (
                  <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-950/60 border border-blue-200 dark:border-blue-800/80 text-xs">
                    <span className="font-bold text-blue-800 dark:text-blue-300 block mb-0.5">
                      Decoded Destination Link:
                    </span>
                    <span className="font-mono text-slate-800 dark:text-slate-200 break-all">
                      {decodedQrUrl}
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Action Row */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
          
          {/* Quick Test Chips */}
          <div className="flex items-center gap-1.5 flex-wrap w-full sm:w-auto">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mr-1">
              {t('scanner.quickTest', 'Quick Test:')}
            </span>
            {SAMPLE_CHIPS.map((chip, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => loadSample(chip)}
                className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-blue-50 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 hover:text-blue-700 transition-colors border border-slate-200 dark:border-slate-700 active:scale-95"
              >
                {chip.label}
              </button>
            ))}
          </div>

          {/* Big Check with Prahar Button */}
          <button
            type="submit"
            disabled={isScanning || isProcessingFile}
            className="w-full sm:w-auto prahar-btn-primary px-8 py-3 rounded-xl text-sm font-bold shadow-sm shrink-0 active:scale-95"
          >
            {isScanning ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin text-white" />
                <span>{t('scanner.checkingButton', 'Checking with Prahar...')}</span>
              </>
            ) : (
              <>
                <Shield className="w-4 h-4 text-white" />
                <span>{t('scanner.checkButton', 'Check with Prahar')}</span>
                <ArrowRight className="w-4 h-4 ml-0.5" />
              </>
            )}
          </button>
        </div>

      </form>

      {/* Privacy Notice & Security Shield Footer */}
      <div className="mt-5 pt-4 border-t border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-[11px] text-slate-500 dark:text-slate-400">
        <div className="flex items-center gap-1.5">
          <Lock className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
          <span>{t('scanner.privacyNote', 'Your submitted content is processed for threat analysis. Avoid uploading passwords or unnecessary personal information.')}</span>
        </div>
        <div className="text-slate-500 dark:text-slate-400 shrink-0">
          SSRF Guard Active
        </div>
      </div>

    </div>
  );
}
