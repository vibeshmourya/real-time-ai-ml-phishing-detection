import React, { useState } from 'react';
import { 
  ShieldCheck, ShieldAlert, AlertTriangle, X, Volume2, VolumeX, 
  ExternalLink, Copy, Check, ChevronRight, AlertOctagon, HelpCircle, Film, Share2
} from 'lucide-react';
import { useScan } from '../context/ScanContext';

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

  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [copied, setCopied] = useState(false);

  if (!isVerdictModalOpen || !activeVerdict) return null;

  const { riskScore, riskLevel, url, domain, threatCategory, detection, preventionGuide } = activeVerdict;
  const isSafe = riskLevel === 'SAFE';
  const isDanger = riskLevel === 'HIGH_RISK';
  const isWarn = riskLevel === 'SUSPICIOUS';

  const handleCopy = () => {
    const summaryText = `[PhishShield Security Verdict]\nTarget: ${url}\nRisk Level: ${riskLevel} (Score: ${riskScore}/100)\nVerdict: ${detection.explanationSummary}\nAction: ${detection.recommendedActions}`;
    navigator.clipboard.writeText(summaryText);
    setCopied(true);
    showToast('Verdict report copied to clipboard', 'info');
    setTimeout(() => setCopied(false), 2000);
  };

  const toggleAudioWarning = () => {
    if ('speechSynthesis' in window) {
      if (isPlayingAudio) {
        window.speechSynthesis.cancel();
        setIsPlayingAudio(false);
      } else {
        const speechText = isDanger 
          ? `Security Warning! High risk phishing detected for ${domain}. ${detection.explanationSummary}. ${detection.recommendedActions}`
          : isWarn 
          ? `Caution. Potential security threat flags detected. ${detection.explanationSummary}`
          : `Verified safe. No phishing indicators detected for ${domain}.`;

        const utterance = new SpeechSynthesisUtterance(speechText);
        utterance.rate = 1.0;
        utterance.onend = () => setIsPlayingAudio(false);
        utterance.onerror = () => setIsPlayingAudio(false);
        window.speechSynthesis.speak(utterance);
        setIsPlayingAudio(true);
      }
    } else {
      showToast('Speech synthesis not supported in this browser', 'warn');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-dark-950/85 backdrop-blur-md overflow-y-auto animate-in fade-in duration-200">
      <div className={`relative w-full max-w-3xl glass-panel p-6 sm:p-8 border-2 shadow-2xl transition-all my-8 ${
        isDanger 
          ? 'border-rose-500/60 shadow-glow-danger bg-dark-900/95' 
          : isWarn 
          ? 'border-amber-500/60 shadow-glow-warn bg-dark-900/95' 
          : 'border-emerald-500/60 shadow-glow-safe bg-dark-900/95'
      }`}>
        
        {/* Close Button */}
        <button
          onClick={() => {
            if ('speechSynthesis' in window) window.speechSynthesis.cancel();
            setIsVerdictModalOpen(false);
          }}
          className="absolute top-5 right-5 p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700/60 transition-all"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Top Verdict Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
          <div className="flex items-center gap-4">
            <div className={`w-14 h-14 rounded-2xl flex items-center justify-center shadow-lg shrink-0 ${
              isDanger 
                ? 'bg-rose-500/20 text-rose-400 border border-rose-500/40' 
                : isWarn 
                ? 'bg-amber-500/20 text-amber-400 border border-amber-500/40' 
                : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
            }`}>
              {isDanger ? (
                <ShieldAlert className="w-8 h-8 animate-pulse" />
              ) : isWarn ? (
                <AlertTriangle className="w-8 h-8" />
              ) : (
                <ShieldCheck className="w-8 h-8" />
              )}
            </div>

            <div>
              <div className="flex items-center gap-2">
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-black tracking-wider uppercase ${
                  isDanger 
                    ? 'bg-rose-500 text-white shadow-glow-danger' 
                    : isWarn 
                    ? 'bg-amber-500 text-slate-950 font-bold' 
                    : 'bg-emerald-500 text-slate-950 font-bold'
                }`}>
                  {isDanger ? 'HIGH RISK PHISHING' : isWarn ? 'SUSPICIOUS LINK' : 'VERIFIED SAFE'}
                </span>
                <span className="text-xs text-slate-400 font-mono">
                  Confidence: {Math.round((detection.confidenceScore || 0.95) * 100)}%
                </span>
              </div>
              <h3 className="text-xl sm:text-2xl font-black text-white mt-1 break-all line-clamp-1">
                {domain || url}
              </h3>
            </div>
          </div>

          {/* Risk Score Circle / Meter */}
          <div className="flex items-center gap-3 bg-dark-950/80 px-4 py-2.5 rounded-2xl border border-slate-800">
            <div className="text-right">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
                Threat Score
              </span>
              <span className={`text-2xl font-black font-mono leading-none ${
                isDanger ? 'text-rose-400' : isWarn ? 'text-amber-400' : 'text-emerald-400'
              }`}>
                {riskScore}<span className="text-xs text-slate-400 font-normal">/100</span>
              </span>
            </div>
            
            {/* Audio warning toggle */}
            <button
              onClick={toggleAudioWarning}
              className={`p-2.5 rounded-xl border transition-all ${
                isPlayingAudio 
                  ? 'bg-rose-500 text-white border-rose-400 animate-pulse' 
                  : 'bg-slate-800 text-slate-300 hover:text-white border-slate-700'
              }`}
              title="Listen to audio warning"
            >
              {isPlayingAudio ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Scanned Target String */}
        <div className="my-4 p-3 rounded-xl bg-dark-950/90 border border-slate-800/80 font-mono text-xs text-slate-300 break-all flex items-center justify-between gap-3">
          <div className="truncate">
            <span className="text-slate-400 font-sans mr-2">Target:</span>
            {url}
          </div>
          <button
            onClick={handleCopy}
            className="shrink-0 p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
            title="Copy URL"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
        </div>

        {/* Explainable AI Analysis (XAI) */}
        <div className="space-y-4">
          
          {/* Summary Box */}
          <div className={`p-4 rounded-xl border ${
            isDanger 
              ? 'bg-rose-950/40 border-rose-500/30 text-rose-200' 
              : isWarn 
              ? 'bg-amber-950/40 border-amber-500/30 text-amber-200' 
              : 'bg-emerald-950/40 border-emerald-500/30 text-emerald-200'
          }`}>
            <h4 className="text-xs font-bold uppercase tracking-wider mb-1 flex items-center gap-1.5">
              <AlertOctagon className="w-3.5 h-3.5" />
              Plain-English AI Verdict
            </h4>
            <p className="text-sm font-medium leading-relaxed">
              {detection.explanationSummary}
            </p>
          </div>

          {/* Key Threat Indicators */}
          {detection.indicators && detection.indicators.length > 0 && (
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2.5">
                Why was this flagged? ({detection.indicators.length} signals detected)
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-h-48 overflow-y-auto pr-1">
                {detection.indicators.map((ind, idx) => (
                  <div key={idx} className="p-3 rounded-xl bg-dark-950/80 border border-slate-800/80 text-xs">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-semibold text-slate-200 truncate">{ind.indicatorName}</span>
                      <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold uppercase ${
                        ind.severity === 'CRITICAL' ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' :
                        ind.severity === 'HIGH' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' :
                        'bg-slate-700/50 text-slate-400'
                      }`}>
                        {ind.severity}
                      </span>
                    </div>
                    <p className="text-slate-400 text-[11px] leading-snug">{ind.description}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Recommended Actions */}
          <div className="p-3.5 rounded-xl bg-slate-900/90 border border-slate-800">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300 mb-1">
              Recommended Next Action:
            </h4>
            <p className="text-xs text-slate-400">
              {detection.recommendedActions}
            </p>
          </div>

        </div>

        {/* Action Buttons Row */}
        <div className="mt-6 pt-5 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3">
          
          {/* Emergency Containment Action (Highlighted for danger) */}
          {isDanger && (
            <button
              onClick={() => {
                setIsVerdictModalOpen(false);
                openEmergencyPlaybook(threatCategory);
              }}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 text-white text-xs font-black shadow-glow-danger transition-all active:scale-95"
            >
              <ShieldAlert className="w-4 h-4" />
              <span>I Already Entered Information! (Playbook)</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          )}

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            {/* Learn prevention video */}
            <button
              onClick={() => {
                setIsVerdictModalOpen(false);
                openAwarenessGuide(threatCategory);
              }}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-xs font-semibold transition-all"
            >
              <Film className="w-3.5 h-3.5 text-cyber-cyan" />
              <span>30s Safety Guide</span>
            </button>

            {/* Report Phishing button */}
            <button
              onClick={() => {
                setIsVerdictModalOpen(false);
                setIsReportModalOpen(true);
              }}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-rose-300 hover:text-rose-200 text-xs font-semibold transition-all"
            >
              <AlertTriangle className="w-3.5 h-3.5 text-rose-400" />
              <span>Report Link</span>
            </button>

            {/* Copy report */}
            <button
              onClick={handleCopy}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold transition-all shadow-md"
            >
              {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Copied!' : 'Copy Verdict'}</span>
            </button>
          </div>

        </div>

      </div>
    </div>
  );
}
