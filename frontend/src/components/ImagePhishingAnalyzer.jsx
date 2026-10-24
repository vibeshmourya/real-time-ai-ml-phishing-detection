import React, { useState, useRef, useEffect } from 'react';
import { 
  Upload, QrCode, Image as ImageIcon, ShieldAlert, ShieldCheck, 
  AlertTriangle, CheckCircle2, Lock, Eye, AlertCircle, Copy, Check, 
  RefreshCw, X, ExternalLink, ArrowRight, Info, FileCheck, Layers
} from 'lucide-react';
import { scannerApi } from '../api/scannerApi';
import { useScan } from '../context/ScanContext';

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB limit
const ALLOWED_MIME_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif', 'image/bmp'];
const ALLOWED_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp'];

export default function ImagePhishingAnalyzer() {
  const { showToast, setActiveVerdict, setIsVerdictModalOpen } = useScan();

  // File state
  const [file, setFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [textHint, setTextHint] = useState('');
  const [showTextHintInput, setShowTextHintInput] = useState(false);
  const [isDragging, setIsDragging] = useState(false);

  // Analysis state
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisPhase, setAnalysisPhase] = useState('');
  const [analysisResult, setAnalysisResult] = useState(null);
  const [errorState, setErrorState] = useState(null);

  // UI state
  const [copiedText, setCopiedText] = useState(null);
  const [isTextExpanded, setIsTextExpanded] = useState(false);
  const [warningModalUrl, setWarningModalUrl] = useState(null);

  const fileInputRef = useRef(null);

  // Revoke object URL on unmount or replace
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const validateFile = (selectedFile) => {
    if (!selectedFile) return 'No file selected.';

    const lowerName = selectedFile.name.toLowerCase();
    const hasValidExt = ALLOWED_EXTENSIONS.some(ext => lowerName.endsWith(ext));
    const hasValidMime = ALLOWED_MIME_TYPES.includes(selectedFile.type);

    if (!hasValidExt && !hasValidMime) {
      return 'Unsupported file format. Please upload a PNG, JPEG, WebP, GIF, or BMP image.';
    }

    if (selectedFile.size > MAX_FILE_SIZE_BYTES) {
      const sizeMb = (selectedFile.size / (1024 * 1024)).toFixed(1);
      return `File size (${sizeMb} MB) exceeds maximum allowed limit of 10 MB.`;
    }

    return null;
  };

  const handleFileSelect = (selectedFile) => {
    setErrorState(null);
    setAnalysisResult(null);

    const validationError = validateFile(selectedFile);
    if (validationError) {
      setErrorState({
        category: 'VALIDATION_ERROR',
        message: validationError
      });
      showToast(validationError, 'warn');
      return;
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    setFile(selectedFile);
    setPreviewUrl(URL.createObjectURL(selectedFile));
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files?.[0];
    if (droppedFile) {
      handleFileSelect(droppedFile);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const clearFile = () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setFile(null);
    setPreviewUrl(null);
    setErrorState(null);
    setAnalysisResult(null);
    setTextHint('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const executeAnalysis = async () => {
    if (!file && !textHint.trim()) {
      showToast('Please select an image file to analyze.', 'warn');
      return;
    }

    if (isAnalyzing) return; // Prevent duplicate submission

    setIsAnalyzing(true);
    setErrorState(null);
    setAnalysisResult(null);

    // Progressive loading indicator
    setAnalysisPhase('Validating image signature and bounds...');
    const t1 = setTimeout(() => setAnalysisPhase('Decoding optical QR codes on canvas...'), 600);
    const t2 = setTimeout(() => setAnalysisPhase('Extracting optical text and heuristic cues...'), 1400);
    const t3 = setTimeout(() => setAnalysisPhase('Scanning extracted URL candidates through threat engine...'), 2400);

    try {
      const response = await scannerApi.scanUnifiedImage(file, textHint);
      const data = response.data;
      setAnalysisResult(data);

      if (data.status === 'PARTIAL_SUCCESS') {
        showToast('Image analyzed with partial component notices.', 'warn');
      } else {
        showToast('Multi-vector image analysis completed successfully.', 'safe');
      }
    } catch (err) {
      setErrorState({
        category: err.category || 'SERVER_ERROR',
        message: err.message || 'Analysis could not be completed.'
      });
      showToast(err.message || 'Analysis failed. Please try again.', 'danger');
    } finally {
      clearTimeout(t1);
      clearTimeout(t2);
      clearTimeout(t3);
      setIsAnalyzing(false);
      setAnalysisPhase('');
    }
  };

  const handleCopy = (text, key) => {
    navigator.clipboard.writeText(text);
    setCopiedText(key);
    setTimeout(() => setCopiedText(null), 2000);
    showToast('Copied to clipboard', 'info');
  };

  const handleInspectFullScan = (scanResult) => {
    if (!scanResult) return;
    setActiveVerdict(scanResult);
    setIsVerdictModalOpen(true);
  };

  const openWarningDialog = (targetUrl) => {
    setWarningModalUrl(targetUrl);
  };

  const confirmExternalNavigation = () => {
    if (warningModalUrl) {
      window.open(warningModalUrl, '_blank', 'noopener,noreferrer');
      setWarningModalUrl(null);
    }
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-8">
      
      {/* Header Banner */}
      <div className="bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 dark:bg-blue-950/60 border border-blue-200 dark:border-blue-800 text-blue-700 dark:text-blue-300 text-xs font-semibold mb-2">
              <Layers className="w-3.5 h-3.5" />
              <span>Multi-Vector Phishing Laboratory</span>
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white">
              QR Code & Visual Screenshot Inspector
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1 max-w-2xl leading-relaxed">
              Upload website screenshots, banking alert SMS, payment QR codes, or fraudulent notices. Prahar securely verifies the image binary, decodes canvas QR codes, performs optical text recognition (OCR), and inspects all candidate links.
            </p>
          </div>

          <div className="shrink-0 text-right">
            <span className="inline-block px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-[11px] font-mono text-slate-500 dark:text-slate-400">
              Max: 10 MB • PNG, JPEG, WebP, GIF, BMP
            </span>
          </div>
        </div>
      </div>

      {/* Upload Zone */}
      <div className="bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 shadow-sm space-y-6">
        
        {/* Drag and Drop Zone */}
        {!file ? (
          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => fileInputRef.current?.click()}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                fileInputRef.current?.click();
              }
            }}
            tabIndex={0}
            role="button"
            aria-label="Upload screenshot or QR code image"
            className={`border-2 border-dashed rounded-3xl p-8 sm:p-12 text-center cursor-pointer transition-all outline-none focus:ring-4 focus:ring-blue-600/20 ${
              isDragging
                ? 'border-blue-600 bg-blue-50/50 dark:bg-blue-950/30 scale-[0.99]'
                : 'border-slate-300 dark:border-slate-700 hover:border-blue-500 bg-slate-50/50 dark:bg-dark-950/40'
            }`}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
              accept="image/png, image/jpeg, image/webp, image/gif, image/bmp"
              className="hidden"
            />

            <div className="w-16 h-16 rounded-2xl bg-blue-100/70 dark:bg-blue-950/80 text-blue-600 dark:text-blue-400 flex items-center justify-center mx-auto mb-4 shadow-inner">
              <Upload className="w-8 h-8" />
            </div>

            <h3 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white">
              Drag and drop your image here, or browse
            </h3>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5 max-w-md mx-auto">
              Supports screenshots of banking portals, SMS phishing alerts, invoices, and payment QR codes.
            </p>

            <div className="flex items-center justify-center gap-3 mt-4 text-xs text-slate-400">
              <span className="flex items-center gap-1">
                <Lock className="w-3.5 h-3.5 text-emerald-500" />
                Pixel-bomb & decompression bomb protected
              </span>
              <span>•</span>
              <span>10MB limit</span>
            </div>
          </div>
        ) : (
          /* Image Selected / Preview State */
          <div className="p-5 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4 min-w-0">
                {previewUrl && (
                  <img
                    src={previewUrl}
                    alt="Uploaded preview"
                    className="w-16 h-16 sm:w-20 sm:h-20 object-cover rounded-2xl border border-slate-200 dark:border-slate-700 shadow-xs shrink-0"
                  />
                )}
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded-md bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 text-[10px] font-bold uppercase">
                      {file.type ? file.type.split('/')[1] : 'IMAGE'}
                    </span>
                    <p className="text-sm font-bold text-slate-900 dark:text-white truncate">
                      {file.name}
                    </p>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                    {(file.size / 1024).toFixed(1)} KB • Ready for deep multi-vector inspection
                  </p>
                  <p className="text-[11px] text-emerald-600 dark:text-emerald-400 flex items-center gap-1 mt-1 font-medium">
                    <FileCheck className="w-3.5 h-3.5" />
                    <span>Client pre-validation passed</span>
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isAnalyzing}
                  className="px-3 py-1.5 rounded-xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors disabled:opacity-50"
                >
                  Change Image
                </button>
                <button
                  type="button"
                  onClick={clearFile}
                  disabled={isAnalyzing}
                  className="p-1.5 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors disabled:opacity-50"
                  title="Remove image"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Hidden re-select input */}
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
              accept="image/png, image/jpeg, image/webp, image/gif, image/bmp"
              className="hidden"
            />
          </div>
        )}

        {/* Optional OCR Context Hint Toggle */}
        <div className="pt-1">
          {!showTextHintInput ? (
            <button
              type="button"
              onClick={() => setShowTextHintInput(true)}
              className="text-xs text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1 font-semibold"
            >
              <span>+ Add optional text or OCR transcription hint</span>
            </button>
          ) : (
            <div className="space-y-2 p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">
                  Optional OCR Text Hint (Assists low-resolution captures)
                </label>
                <button
                  type="button"
                  onClick={() => { setShowTextHintInput(false); setTextHint(''); }}
                  className="text-[11px] text-slate-400 hover:text-slate-600"
                >
                  Hide
                </button>
              </div>
              <textarea
                rows={2}
                value={textHint}
                onChange={(e) => setTextHint(e.target.value)}
                placeholder="Paste any transcribed SMS or visible text snippet from the image..."
                className="w-full p-3 rounded-xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-700 text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-600/20"
              />
            </div>
          )}
        </div>

        {/* Action Button & Progressive Status */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
          <div className="text-xs text-slate-400 flex items-center gap-1.5">
            <Lock className="w-3.5 h-3.5 text-slate-400" />
            <span>Images are analyzed in-memory and not permanently retained.</span>
          </div>

          <button
            type="button"
            onClick={executeAnalysis}
            disabled={(!file && !textHint.trim()) || isAnalyzing}
            className={`w-full sm:w-auto px-6 py-3.5 rounded-2xl font-bold text-sm flex items-center justify-center gap-2.5 transition-all shadow-md ${
              isAnalyzing || (!file && !textHint.trim())
                ? 'bg-slate-200 dark:bg-dark-800 text-slate-400 cursor-not-allowed shadow-none'
                : 'bg-blue-600 hover:bg-blue-700 active:scale-[0.98] text-white shadow-blue-600/25'
            }`}
          >
            {isAnalyzing ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>{analysisPhase || 'Analyzing Image...'}</span>
              </>
            ) : (
              <>
                <Eye className="w-4 h-4" />
                <span>Run Multi-Vector Analysis</span>
              </>
            )}
          </button>
        </div>

      </div>

      {/* Error / Rejection Banner */}
      {errorState && (
        <div className="p-6 rounded-3xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/60 space-y-3 animate-in fade-in">
          <div className="flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <h4 className="text-sm font-bold text-rose-900 dark:text-rose-200">
                {errorState.category === 'VALIDATION_ERROR' && 'Image Validation Rejected'}
                {errorState.category === 'NETWORK_ERROR' && 'Backend Service Unavailable'}
                {errorState.category === 'TIMEOUT' && 'Analysis Request Timed Out'}
                {errorState.category === 'RATE_LIMITED' && 'Scan Rate Limit Exceeded'}
                {errorState.category === 'AUTH_ERROR' && 'Authentication Required'}
                {errorState.category === 'SERVER_ERROR' && 'Server Processing Failure'}
              </h4>
              <p className="text-xs text-rose-700 dark:text-rose-300 leading-relaxed">
                {errorState.message}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3 pt-2">
            <button
              type="button"
              onClick={executeAnalysis}
              className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-colors"
            >
              Retry Analysis
            </button>
            <button
              type="button"
              onClick={clearFile}
              className="px-4 py-2 rounded-xl bg-white dark:bg-dark-900 border border-rose-300 dark:border-rose-800 text-rose-700 dark:text-rose-300 text-xs font-bold hover:bg-rose-100 transition-colors"
            >
              Choose Another File
            </button>
          </div>
        </div>
      )}

      {/* Analysis Results Display */}
      {analysisResult && (
        <div className="space-y-6 animate-in fade-in duration-200">
          
          {/* 1. OVERALL ASSESSMENT BANNER */}
          <div className={`p-6 sm:p-8 rounded-3xl border shadow-sm space-y-4 ${
            analysisResult.overallRiskLevel === 'HIGH_RISK'
              ? 'bg-rose-50/70 dark:bg-rose-950/30 border-rose-200 dark:border-rose-900'
              : analysisResult.overallRiskLevel === 'SUSPICIOUS'
              ? 'bg-amber-50/70 dark:bg-amber-950/30 border-amber-200 dark:border-amber-900'
              : 'bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-200 dark:border-emerald-900'
          }`}>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className={`w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 shadow-sm ${
                  analysisResult.overallRiskLevel === 'HIGH_RISK'
                    ? 'bg-rose-600 text-white'
                    : analysisResult.overallRiskLevel === 'SUSPICIOUS'
                    ? 'bg-amber-600 text-white'
                    : 'bg-emerald-600 text-white'
                }`}>
                  {analysisResult.overallRiskLevel === 'HIGH_RISK' && <ShieldAlert className="w-6 h-6" />}
                  {analysisResult.overallRiskLevel === 'SUSPICIOUS' && <AlertTriangle className="w-6 h-6" />}
                  {analysisResult.overallRiskLevel === 'SAFE' && <ShieldCheck className="w-6 h-6" />}
                </div>

                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className={`px-2.5 py-0.5 rounded-full text-xs font-black uppercase tracking-wider ${
                      analysisResult.overallRiskLevel === 'HIGH_RISK'
                        ? 'bg-rose-100 dark:bg-rose-900/60 text-rose-800 dark:text-rose-200'
                        : analysisResult.overallRiskLevel === 'SUSPICIOUS'
                        ? 'bg-amber-100 dark:bg-amber-900/60 text-amber-800 dark:text-amber-200'
                        : 'bg-emerald-100 dark:bg-emerald-900/60 text-emerald-800 dark:text-emerald-200'
                    }`}>
                      {analysisResult.overallRiskLevel === 'HIGH_RISK' && 'High Risk Phishing Detected'}
                      {analysisResult.overallRiskLevel === 'SUSPICIOUS' && 'Suspicious Characteristics Flagged'}
                      {analysisResult.overallRiskLevel === 'SAFE' && 'Clean / No Obvious Phishing Detected'}
                    </span>

                    {analysisResult.status === 'PARTIAL_SUCCESS' && (
                      <span className="px-2 py-0.5 rounded-md bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300 text-[10px] font-bold">
                        Partial Analysis
                      </span>
                    )}

                    <span className="px-2 py-0.5 rounded-md bg-slate-200/80 dark:bg-dark-800 text-slate-700 dark:text-slate-300 text-[10px] font-mono">
                      Category: {analysisResult.threatCategory || 'BENIGN'}
                    </span>
                  </div>

                  <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1">
                    {analysisResult.message || 'Evidence-based threat assessment completed.'}
                  </p>
                </div>
              </div>

              {/* Score Metric */}
              <div className="flex items-baseline gap-1.5 shrink-0 bg-white/80 dark:bg-dark-900/80 px-4 py-2.5 rounded-2xl border border-slate-200 dark:border-slate-800">
                <span className="text-2xl font-black text-slate-900 dark:text-white">
                  {analysisResult.overallRiskScore}
                </span>
                <span className="text-xs font-bold text-slate-400">/ 100</span>
                <span className="text-[10px] uppercase font-bold text-slate-400 ml-1">Risk Score</span>
              </div>
            </div>

            {/* Recommended Next Steps */}
            {analysisResult.recommendedNextSteps && analysisResult.recommendedNextSteps.length > 0 && (
              <div className="p-4 rounded-2xl bg-white/90 dark:bg-dark-900/90 border border-slate-200/80 dark:border-slate-800 space-y-2">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400 flex items-center gap-1.5">
                  <Info className="w-3.5 h-3.5 text-blue-600" />
                  <span>Recommended Next Steps:</span>
                </p>
                <ul className="space-y-1.5 text-xs text-slate-700 dark:text-slate-300">
                  {analysisResult.recommendedNextSteps.map((step, idx) => (
                    <li key={idx} className="flex items-start gap-2">
                      <span className="font-bold text-slate-400">•</span>
                      <span>{step}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>

          {/* 2. IMAGE SECURITY VALIDATION SECTION */}
          {analysisResult.imageValidation && (
            <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-3 shadow-xs">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>1. Image Security Pre-Validation</span>
                </h4>
                <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 text-xs font-bold">
                  Verified Safe
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs pt-1">
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-100 dark:border-slate-800">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Format</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200">
                    {analysisResult.imageValidation.format || 'PNG'}
                  </span>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-100 dark:border-slate-800">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Canvas Dimensions</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200">
                    {analysisResult.imageValidation.width} × {analysisResult.imageValidation.height} px
                  </span>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-100 dark:border-slate-800">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Payload Size</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200">
                    {analysisResult.imageValidation.sizeBytes ? (analysisResult.imageValidation.sizeBytes / 1024).toFixed(1) + ' KB' : 'N/A'}
                  </span>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-100 dark:border-slate-800 truncate">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Sanitized Name</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200 truncate block">
                    {analysisResult.imageValidation.sanitizedFilename || 'image.png'}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* 3. OPTICAL QR CODE ANALYSIS SECTION */}
          {analysisResult.qrAnalysis && (
            <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-xs">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <QrCode className="w-4 h-4 text-blue-600" />
                  <span>2. Optical QR Code Decoding</span>
                </h4>
                <span className="px-2.5 py-0.5 rounded-full bg-slate-100 dark:bg-dark-950 text-slate-700 dark:text-slate-300 text-xs font-bold">
                  {analysisResult.qrAnalysis.totalQrCodesFound} QR code(s) detected
                </span>
              </div>

              {analysisResult.qrAnalysis.totalQrCodesFound === 0 ? (
                <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-100 dark:border-slate-800 text-xs text-slate-500">
                  No optical QR barcodes detected on this image canvas.
                </div>
              ) : (
                <div className="space-y-3">
                  {analysisResult.qrAnalysis.qrCodes?.map((qr, idx) => (
                    <div key={idx} className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-2.5">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 rounded-md bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 text-[10px] font-bold">
                            QR #{qr.index || idx + 1}
                          </span>
                          <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
                            Type: {qr.payloadType || 'URL'}
                          </span>
                        </div>

                        <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${
                          qr.scanStatus === 'SCANNED'
                            ? 'bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300'
                            : qr.scanStatus?.includes('BLOCKED')
                            ? 'bg-rose-100 dark:bg-rose-950 text-rose-700 dark:text-rose-300'
                            : 'bg-slate-200 dark:bg-dark-800 text-slate-600 dark:text-slate-300'
                        }`}>
                          Status: {qr.scanStatus || 'DETECTED'}
                        </span>
                      </div>

                      {/* Payload Content Safe Rendering */}
                      <div className="p-3 rounded-xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3 text-xs font-mono break-all">
                        <span className="text-slate-800 dark:text-slate-200 select-all">
                          {qr.safeDisplayPayload || qr.rawPayload}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleCopy(qr.rawPayload, 'qr_' + idx)}
                          className="shrink-0 p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200"
                          title="Copy payload"
                        >
                          {copiedText === 'qr_' + idx ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                        </button>
                      </div>

                      {qr.validationError && (
                        <p className="text-[11px] text-rose-600 dark:text-rose-400">
                          {qr.validationError}
                        </p>
                      )}

                      {qr.extractedUrl && (
                        <div className="flex items-center justify-between pt-1">
                          <button
                            type="button"
                            onClick={() => openWarningDialog(qr.extractedUrl)}
                            className="text-xs text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1 font-semibold"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                            <span>Inspect destination link</span>
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* 4. OPTICAL CHARACTER RECOGNITION (OCR) SECTION */}
          {analysisResult.ocrAnalysis && (
            <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-xs">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Eye className="w-4 h-4 text-purple-600" />
                  <span>3. Optical Text Recognition & Heuristic Indicators</span>
                </h4>
                <span className="px-2.5 py-0.5 rounded-full bg-slate-100 dark:bg-dark-950 text-slate-700 dark:text-slate-300 text-xs font-bold">
                  Engine: {analysisResult.ocrAnalysis.ocrEngine || 'Standard OCR'}
                </span>
              </div>

              {/* Extracted Text Box */}
              {analysisResult.ocrAnalysis.rawText ? (
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-xs text-slate-400">
                    <span>Extracted Text Content:</span>
                    <button
                      type="button"
                      onClick={() => handleCopy(analysisResult.ocrAnalysis.rawText, 'ocr_raw')}
                      className="text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1 font-semibold"
                    >
                      {copiedText === 'ocr_raw' ? <Check className="w-3 h-3 text-emerald-500" /> : <Copy className="w-3 h-3" />}
                      <span>Copy Text</span>
                    </button>
                  </div>

                  <div className={`p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs text-slate-800 dark:text-slate-200 font-mono leading-relaxed whitespace-pre-wrap break-words ${
                    !isTextExpanded && analysisResult.ocrAnalysis.rawText.length > 300 ? 'max-h-36 overflow-hidden relative' : ''
                  }`}>
                    {analysisResult.ocrAnalysis.rawText}
                    {!isTextExpanded && analysisResult.ocrAnalysis.rawText.length > 300 && (
                      <div className="absolute inset-x-0 bottom-0 h-14 bg-gradient-to-t from-slate-50 dark:from-dark-950 to-transparent flex items-end justify-center pb-1">
                        <button
                          type="button"
                          onClick={() => setIsTextExpanded(true)}
                          className="px-3 py-1 rounded-lg bg-white dark:bg-dark-800 text-blue-600 dark:text-blue-400 text-[11px] font-bold shadow-xs border border-slate-200 dark:border-slate-700"
                        >
                          Show Full Text
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-100 dark:border-slate-800 text-xs text-slate-500">
                  No readable text was extracted from this image.
                </div>
              )}

              {/* Heuristic Threat Indicators */}
              {analysisResult.ocrAnalysis.textIndicators && analysisResult.ocrAnalysis.textIndicators.length > 0 && (
                <div className="space-y-2 pt-2">
                  <span className="text-xs font-bold text-slate-700 dark:text-slate-300 block">
                    Triggered Text Heuristic Rules ({analysisResult.ocrAnalysis.textIndicators.length}):
                  </span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {analysisResult.ocrAnalysis.textIndicators.map((ind, idx) => (
                      <div key={idx} className="p-3.5 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-slate-900 dark:text-white">
                            {ind.indicatorName}
                          </span>
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                            ind.severity === 'CRITICAL' ? 'bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300' :
                            ind.severity === 'HIGH' ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300' :
                            'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300'
                          }`}>
                            {ind.severity}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-normal">
                          {ind.description}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* 5. URL CANDIDATES & MULTI-VECTOR SCANNING SECTION */}
          <div className="p-6 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-4 shadow-xs">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <ExternalLink className="w-4 h-4 text-blue-600" />
                <span>4. Extracted URL Candidates & Threat Verification</span>
              </h4>
              <span className="px-2.5 py-0.5 rounded-full bg-slate-100 dark:bg-dark-950 text-slate-700 dark:text-slate-300 text-xs font-bold">
                {analysisResult.urlCandidates?.length || 0} candidate URL(s)
              </span>
            </div>

            {(!analysisResult.urlCandidates || analysisResult.urlCandidates.length === 0) ? (
              <div className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-100 dark:border-slate-800 text-xs text-slate-500">
                No clickable or external web addresses were extracted from QR payloads or OCR text.
              </div>
            ) : (
              <div className="space-y-3">
                {analysisResult.urlCandidates.map((cand, idx) => (
                  <div key={idx} className="p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-3">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <div className="flex items-center gap-2 flex-wrap">
                        {/* Provenance Badge */}
                        <span className={`px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider ${
                          cand.source === 'BOTH'
                            ? 'bg-purple-100 dark:bg-purple-950 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800'
                            : cand.source === 'QR'
                            ? 'bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800'
                            : 'bg-teal-100 dark:bg-teal-950 text-teal-700 dark:text-teal-300 border border-teal-200 dark:border-teal-800'
                        }`}>
                          Source: {cand.source === 'BOTH' ? 'QR & OCR Text' : cand.source === 'QR' ? 'QR Code' : 'OCR Text'}
                        </span>

                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          cand.scanStatus === 'SCANNED' ? 'bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300' :
                          cand.scanStatus?.includes('BLOCKED') ? 'bg-rose-100 dark:bg-rose-950 text-rose-700 dark:text-rose-300' :
                          'bg-slate-200 dark:bg-dark-800 text-slate-600 dark:text-slate-400'
                        }`}>
                          {cand.scanStatus}
                        </span>
                      </div>

                      {cand.scanResult && (
                        <div className="flex items-center gap-2">
                          <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                            cand.scanResult.riskLevel === 'HIGH_RISK' ? 'bg-rose-600 text-white' :
                            cand.scanResult.riskLevel === 'SUSPICIOUS' ? 'bg-amber-600 text-white' :
                            'bg-emerald-600 text-white'
                          }`}>
                            Score: {cand.scanResult.riskScore}/100 • {cand.scanResult.riskLevel}
                          </span>
                        </div>
                      )}
                    </div>

                    {/* URL text rendering */}
                    <div className="p-3 rounded-xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3 text-xs font-mono break-all">
                      <span className="text-slate-800 dark:text-slate-200 select-all">
                        {cand.url}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleCopy(cand.url, 'cand_' + idx)}
                        className="shrink-0 p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200"
                        title="Copy candidate URL"
                      >
                        {copiedText === 'cand_' + idx ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                      </button>
                    </div>

                    {cand.validationError && (
                      <p className="text-[11px] text-rose-600 dark:text-rose-400">
                        {cand.validationError}
                      </p>
                    )}

                    {/* Actions on candidate */}
                    <div className="flex items-center justify-between pt-1 flex-wrap gap-2">
                      {cand.scanResult && (
                        <button
                          type="button"
                          onClick={() => handleInspectFullScan(cand.scanResult)}
                          className="text-xs text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1 font-bold"
                        >
                          <Info className="w-3.5 h-3.5" />
                          <span>View Full Threat Breakdown</span>
                        </button>
                      )}

                      <button
                        type="button"
                        onClick={() => openWarningDialog(cand.url)}
                        className="text-xs text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 flex items-center gap-1"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                        <span>Visit link (Caution)</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 6. COMPONENT STATUS & LIMITATIONS */}
          {((analysisResult.componentErrors && Object.keys(analysisResult.componentErrors).length > 0) ||
            (analysisResult.limitations && analysisResult.limitations.length > 0)) && (
            <div className="p-6 rounded-3xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-3 text-xs">
              <h5 className="font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                <Info className="w-3.5 h-3.5 text-blue-600" />
                <span>Engine Diagnostics & Analysis Scope</span>
              </h5>

              {/* Component specific errors if partial failure occurred */}
              {analysisResult.componentErrors && Object.keys(analysisResult.componentErrors).length > 0 && (
                <div className="space-y-1.5">
                  <span className="font-semibold text-amber-700 dark:text-amber-400 block text-[11px]">
                    Sub-system notices:
                  </span>
                  {Object.entries(analysisResult.componentErrors).map(([key, msg]) => (
                    <div key={key} className="p-2.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900/60 text-amber-800 dark:text-amber-300 text-[11px]">
                      <strong>{key}:</strong> {msg}
                    </div>
                  ))}
                </div>
              )}

              {/* Backend limitations */}
              {analysisResult.limitations && analysisResult.limitations.length > 0 && (
                <ul className="space-y-1 text-slate-500 dark:text-slate-400 pt-1">
                  {analysisResult.limitations.map((lim, idx) => (
                    <li key={idx} className="flex items-start gap-1.5">
                      <span>•</span>
                      <span>{lim}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {/* Action Row */}
          <div className="flex items-center justify-between pt-2">
            <button
              type="button"
              onClick={clearFile}
              className="px-5 py-2.5 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 text-xs font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors shadow-xs"
            >
              Analyze Another Image
            </button>
          </div>

        </div>
      )}

      {/* External Navigation Warning Modal (Never Auto-Open) */}
      {warningModalUrl && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 space-y-4 shadow-2xl">
            <div className="w-12 h-12 rounded-2xl bg-amber-100 dark:bg-amber-950 text-amber-600 flex items-center justify-center">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div className="space-y-1.5">
              <h3 className="text-base font-bold text-slate-900 dark:text-white">
                External Link Navigation Warning
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                You are about to navigate to an external URL extracted from the uploaded image:
              </p>
              <div className="p-3 rounded-xl bg-slate-100 dark:bg-dark-950 font-mono text-xs text-slate-800 dark:text-slate-200 break-all select-all">
                {warningModalUrl}
              </div>
              <p className="text-[11px] text-rose-600 dark:text-rose-400 font-semibold pt-1">
                ⚠️ Never enter passwords, banking credentials, or OTP codes on unverified pages.
              </p>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setWarningModalUrl(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-dark-800 text-slate-700 dark:text-slate-300 text-xs font-bold hover:bg-slate-200"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={confirmExternalNavigation}
                className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold flex items-center gap-1.5"
              >
                <span>Proceed to Link</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
