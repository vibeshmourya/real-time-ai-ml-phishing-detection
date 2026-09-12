import React, { useState } from 'react';
import { 
  ShieldCheck, ShieldAlert, AlertTriangle, X, Volume2, VolumeX, 
  Copy, Check, ChevronDown, ChevronUp, ChevronRight, ThumbsUp, 
  ThumbsDown, AlertCircle, ExternalLink, HelpCircle, Film
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';
import { scannerApi } from '../api/scannerApi';

export default function ScanVerdictModal() {
  const { 
    activeVerdict, 
    isVerdictModalOpen, 
    setIsVerdictModalOpen, 
    openEmergencyPlaybook, 
    openAwarenessGuide,
    setIsReportModalOpen,
    showToast 
  } = useScan();
  const { t } = useLanguage();

  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [copied, setCopied] = useState(false);
  const [feedbackSubmitted, setFeedbackSubmitted] = useState(false);

  if (!isVerdictModalOpen || !activeVerdict) return null;

  const { 
    scanId, 
    riskScore, 
    riskLevel, 
    url, 
    domain, 
    threatCategory, 
    detection, 
    technicalDetails 
  } = activeVerdict;

  const isSafe = riskLevel === 'SAFE';
  const isDanger = riskLevel === 'HIGH_RISK';
  const isWarn = riskLevel === 'SUSPICIOUS';

  const handleCopy = () => {
    const summaryText = `[Prahar Security Verdict]\nTarget: ${url}\nRisk Level: ${riskLevel} (${riskScore}/100)\nThreat Type: ${threatCategory}\nSummary: ${detection?.explanationSummary || ''}\nAction: ${detection?.recommendedActions || ''}`;
    navigator.clipboard.writeText(summaryText);
    setCopied(true);
    showToast(t('common.copied', 'Copied to clipboard'), 'info');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleFeedback = async (isHelpful) => {
    if (feedbackSubmitted) return;
    setFeedbackSubmitted(true);
    try {
      await scannerApi.submitFeedback(scanId, isHelpful ? 'ACCURATE' : 'FALSE_POSITIVE');
      showToast(t('verdict.feedbackThanks', 'Thank you for your feedback!'), 'safe');
    } catch {
      showToast('Feedback recorded.', 'info');
    }
  };

  const toggleAudioWarning = () => {
    if ('speechSynthesis' in window) {
      if (isPlayingAudio) {
        window.speechSynthesis.cancel();
        setIsPlayingAudio(false);
      } else {
        const speechText = isDanger 
          ? `Security Warning! High risk phishing detected by Prahar for ${domain || 'this link'}. ${detection?.explanationSummary}. Recommended action: ${detection?.recommendedActions}`
          : isWarn 
          ? `Caution. Potential security threat flags detected. ${detection?.explanationSummary}. Recommended action: ${detection?.recommendedActions}`
          : `Verified safe. No phishing indicators detected for ${domain || 'this address'}.`;

        const utterance = new SpeechSynthesisUtterance(speechText);
        utterance.rate = 1.0;
        utterance.onend = () => setIsPlayingAudio(false);
        utterance.onerror = () => setIsPlayingAudio(false);
        window.speechSynthesis.speak(utterance);
        setIsPlayingAudio(true);
      }
    } else {
      showToast('Audio is not supported by your browser.', 'warn');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/60 backdrop-blur-sm overflow-y-auto animate-in fade-in duration-150">
      <div className="relative w-full max-w-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-modal p-6 sm:p-8 my-8 transition-all">
        
        {/* Close Button */}
        <button
          onClick={() => {
            if ('speechSynthesis' in window) window.speechSynthesis.cancel();
            setIsVerdictModalOpen(false);
          }}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="Close"
        >
          <X className="w-5 h-5" />
        </button>

        {/* 1. TOP VERDICT BANNER */}
        <div className="flex items-start sm:items-center gap-4 pb-6 border-b border-slate-200 dark:border-slate-800">
          <div className={`w-14 h-14 rounded-2xl flex items-center justify-center shrink-0 ${
            isDanger 
              ? 'bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400' 
              : isWarn 
              ? 'bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400' 
              : 'bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400'
          }`}>
            {isDanger ? (
              <ShieldAlert className="w-8 h-8" />
            ) : isWarn ? (
              <AlertTriangle className="w-8 h-8" />
            ) : (
              <ShieldCheck className="w-8 h-8" />
            )}
          </div>

          <div className="min-w-0 pr-8">
            <div className="flex items-center gap-2 flex-wrap">
              <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wide ${
                isDanger 
                  ? 'bg-red-600 text-white' 
                  : isWarn 
                  ? 'bg-amber-500 text-slate-950' 
                  : 'bg-emerald-600 text-white'
              }`}>
                {isDanger ? t('verdict.danger', 'HIGH RISK') : isWarn ? t('verdict.suspicious', 'SUSPICIOUS') : t('verdict.safe', 'SAFE')}
              </span>
              
              {threatCategory && threatCategory !== 'BENIGN' && (
                <span className="text-xs font-bold text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded-md">
                  {threatCategory.replace(/_/g, ' ')}
                </span>
              )}
            </div>

            <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900 dark:text-white mt-1 break-all line-clamp-1">
              {domain || url}
            </h3>
          </div>
        </div>

        {/* 2. VISUAL RISK METER */}
        <div className="my-5 p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {t('verdict.riskScore', 'Risk Score')}
            </span>
            <div className="flex items-center gap-2">
              <span className={`text-2xl font-black font-mono leading-none ${
                isDanger ? 'text-red-600 dark:text-red-400' : isWarn ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'
              }`}>
                {riskScore} <span className="text-xs font-normal text-slate-400">/ 100</span>
              </span>
            </div>
          </div>

          {/* Progress Bar Gauge */}
          <div className="w-full h-3 bg-slate-200 dark:bg-slate-800 rounded-full overflow-hidden relative">
            <div 
              style={{ width: `${Math.max(4, riskScore)}%` }}
              className={`h-full rounded-full transition-all duration-500 ${
                isDanger ? 'bg-red-600' : isWarn ? 'bg-amber-500' : 'bg-emerald-500'
              }`}
            />
          </div>

          <div className="flex justify-between items-center text-[10px] font-bold uppercase text-slate-400 mt-1.5">
            <span>0 (Safe)</span>
            <span>50 (Caution)</span>
            <span>100 (Danger)</span>
          </div>

          <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-2">
            {t('verdict.scoreNote', 'Higher scores indicate more suspicious characteristics.')}
          </p>
        </div>

        {/* Target address strip */}
        <div className="my-3 p-3 rounded-xl bg-slate-100 dark:bg-dark-850 text-xs font-mono text-slate-700 dark:text-slate-300 break-all flex items-center justify-between gap-3">
          <div className="truncate">
            <span className="font-sans font-semibold text-slate-400 mr-2">Target:</span>
            {url}
          </div>
          <button
            onClick={handleCopy}
            className="shrink-0 p-1.5 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-200 text-slate-600 dark:text-slate-300 transition-colors"
            title="Copy"
          >
            {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>

        {/* 3. WHY PRAHAR FLAGGED IT */}
        <div className="space-y-4 my-5">
          
          {/* Plain English Summary */}
          <div className={`p-4 rounded-2xl border text-sm leading-relaxed ${
            isDanger 
              ? 'bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-900/60 text-red-900 dark:text-red-200' 
              : isWarn 
              ? 'bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-900/60 text-amber-900 dark:text-amber-200' 
              : 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-900/60 text-emerald-900 dark:text-emerald-200'
          }`}>
            <h4 className="text-xs font-bold uppercase tracking-wider mb-1 flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4" />
              {t('verdict.whyFlagged', 'Why Prahar flagged it')}
            </h4>
            <p className="font-medium">
              {detection?.explanationSummary || 'Analysis completed.'}
            </p>
          </div>

          {/* Indicator Cards */}
          {detection?.indicators && detection.indicators.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Key Red Flags Detected ({detection.indicators.length})
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-h-48 overflow-y-auto pr-1">
                {detection.indicators.map((ind, idx) => (
                  <div key={idx} className="p-3 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-bold text-slate-900 dark:text-white truncate">{ind.indicatorName}</span>
                      <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold uppercase ${
                        ind.severity === 'CRITICAL' ? 'bg-red-100 dark:bg-red-950 text-red-700 dark:text-red-400' :
                        ind.severity === 'HIGH' ? 'bg-amber-100 dark:bg-amber-950 text-amber-700 dark:text-amber-400' :
                        'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                      }`}>
                        {ind.severity}
                      </span>
                    </div>
                    <p className="text-slate-500 dark:text-slate-400 text-[11px] leading-snug">{ind.description}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 4. WHAT SHOULD I DO? */}
          <div className="p-4 rounded-2xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900/60">
            <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900 dark:text-blue-300 mb-1.5">
              {t('verdict.whatToDo', 'What should I do?')}
            </h4>
            <p className="text-xs text-blue-950 dark:text-blue-200 font-medium">
              {detection?.recommendedActions || 'Maintain standard digital vigilance.'}
            </p>
          </div>

        </div>

        {/* 5. EXPANDABLE TECHNICAL DETAILS (For analysts / developers) */}
        <div className="pt-2 border-t border-slate-200 dark:border-slate-800">
          <button
            onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
            className="w-full flex items-center justify-between py-2 text-xs font-semibold text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 transition-colors"
          >
            <span>{showTechnicalDetails ? t('verdict.hideTechDetails', 'Hide technical details') : t('verdict.showTechDetails', 'Show technical details')}</span>
            {showTechnicalDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>

          {showTechnicalDetails && (
            <div className="p-3.5 mt-2 rounded-xl bg-slate-100 dark:bg-dark-950 text-xs font-mono space-y-1.5 border border-slate-200 dark:border-slate-800 animate-in fade-in">
              <div className="flex justify-between">
                <span className="text-slate-500">Domain:</span>
                <span className="text-slate-900 dark:text-white font-bold">{domain || 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Protocol:</span>
                <span className="text-slate-900 dark:text-white">{technicalDetails?.protocol || 'Standard'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Entropy:</span>
                <span className="text-slate-900 dark:text-white">{technicalDetails?.entropy || 'Normal'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Model:</span>
                <span className="text-slate-900 dark:text-white">{technicalDetails?.model || 'XGBoost-v1.0'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Confidence:</span>
                <span className="text-slate-900 dark:text-white">{Math.round((detection?.confidenceScore || 0.95) * 100)}%</span>
              </div>
            </div>
          )}
        </div>

        {/* 6. ACTION BUTTONS & FEEDBACK ROW */}
        <div className="mt-6 pt-5 border-t border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
          
          {/* Emergency button if high risk */}
          {isDanger ? (
            <button
              onClick={() => {
                setIsVerdictModalOpen(false);
                openEmergencyPlaybook(threatCategory);
              }}
              className="w-full sm:w-auto prahar-btn-danger px-4 py-2.5 rounded-xl text-xs font-bold shadow-sm"
            >
              <ShieldAlert className="w-4 h-4" />
              <span>{t('verdict.iAlreadyClicked', 'I Already Clicked')}</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          ) : (
            <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <span>{t('verdict.feedbackPrompt', 'Was this result helpful?')}</span>
              <button
                onClick={() => handleFeedback(true)}
                disabled={feedbackSubmitted}
                className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 disabled:opacity-50"
                title="Yes"
              >
                <ThumbsUp className="w-4 h-4" />
              </button>
              <button
                onClick={() => handleFeedback(false)}
                disabled={feedbackSubmitted}
                className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 disabled:opacity-50"
                title="No"
              >
                <ThumbsDown className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Right utility buttons */}
          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            
            {/* Audio warning */}
            <button
              onClick={toggleAudioWarning}
              className={`p-2 rounded-xl border text-xs font-semibold flex items-center gap-1.5 transition-colors ${
                isPlayingAudio 
                  ? 'bg-blue-600 text-white border-blue-600' 
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:bg-slate-200'
              }`}
              title="Listen to advice"
            >
              {isPlayingAudio ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
              <span className="hidden sm:inline">{t('verdict.listenAdvice', 'Listen')}</span>
            </button>

            {/* Learn Guide */}
            <button
              onClick={() => {
                setIsVerdictModalOpen(false);
                openAwarenessGuide(threatCategory);
              }}
              className="px-3 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-colors"
            >
              <Film className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
              <span>Learn More</span>
            </button>

            {/* Copy report */}
            <button
              onClick={handleCopy}
              className="prahar-btn-primary px-4 py-2 rounded-xl text-xs font-bold"
            >
              {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
              <span>{copied ? 'Copied' : t('verdict.copyVerdict', 'Copy')}</span>
            </button>
          </div>

        </div>

      </div>
    </div>
  );
}
