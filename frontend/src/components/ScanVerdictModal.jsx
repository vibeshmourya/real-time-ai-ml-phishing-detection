import React, { useState } from 'react';
import { 
  ShieldCheck, ShieldAlert, AlertTriangle, X, Copy, Check, 
  ChevronDown, ChevronUp, MessageSquare, AlertCircle, Sparkles, RefreshCw
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { useLanguage } from '../context/LanguageContext';

export default function ScanVerdictModal() {
  const { 
    activeVerdict, 
    isVerdictModalOpen, 
    setIsVerdictModalOpen, 
    openEmergencyPlaybook, 
    setIsReportModalOpen,
    openChatWithScan,
    showToast 
  } = useScan();
  const { t } = useLanguage();

  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false);
  const [showAllIndicators, setShowAllIndicators] = useState(false);
  const [copied, setCopied] = useState(false);

  if (!isVerdictModalOpen || !activeVerdict) return null;

  const { 
    riskScore = 0, 
    riskLevel = 'SAFE', 
    url = '', 
    domain = '', 
    threatCategory = 'BENIGN', 
    detection = {}, 
    technicalDetails = {} 
  } = activeVerdict;

  const isSafe = riskLevel === 'SAFE';
  const isDanger = riskLevel === 'HIGH_RISK';
  const isWarn = riskLevel === 'SUSPICIOUS';

  // 1. Explicit Verdict Text as required
  const verdictText = isDanger 
    ? 'Dangerous — avoid opening this link'
    : isWarn 
    ? 'Be careful — this link looks suspicious'
    : 'No obvious threats detected';

  // 4. Recommended Action as required
  const recommendedActionText = isDanger
    ? 'Do not open this link or provide personal information.'
    : isWarn
    ? 'Do not enter passwords, payment details, or OTPs.'
    : 'Still verify the sender before sharing sensitive information.';

  const handleCopy = () => {
    const summaryText = `[Prahar Security Verdict]\nTarget: ${url}\nVerdict: ${verdictText}\nRisk Score: ${riskScore}/100\nAction: ${recommendedActionText}`;
    navigator.clipboard.writeText(summaryText);
    setCopied(true);
    showToast('Verdict copied to clipboard', 'info');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleAskChatbot = () => {
    setIsVerdictModalOpen(false);
    openChatWithScan(activeVerdict, 'Why was my link flagged?');
  };

  const handleScanAnother = () => {
    setIsVerdictModalOpen(false);
    // Focus scanner input if available
    setTimeout(() => {
      const input = document.getElementById('url-scan-input') || document.getElementById('unified-scan-input');
      input?.focus();
    }, 100);
  };

  const indicators = detection?.indicators || [];
  const primaryIndicators = indicators.slice(0, 3);
  const additionalIndicators = indicators.slice(3);

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/60 backdrop-blur-sm overflow-y-auto animate-in fade-in duration-150"
      role="dialog"
      aria-label="Scan Result Verdict"
    >
      <div className="relative w-full max-w-xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-2xl p-6 sm:p-8 my-6 transition-all">
        
        {/* Close Button */}
        <button
          onClick={() => setIsVerdictModalOpen(false)}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="Close result"
          aria-label="Close"
        >
          <X className="w-5 h-5" />
        </button>

        {/* 1. CLEAR VERDICT */}
        <div className="flex items-start gap-4 pb-5 border-b border-slate-100 dark:border-slate-800">
          <div className={`w-13 h-13 rounded-2xl flex items-center justify-center shrink-0 p-3 ${
            isDanger 
              ? 'bg-red-100 dark:bg-red-950/70 text-red-600 dark:text-red-400' 
              : isWarn 
              ? 'bg-amber-100 dark:bg-amber-950/70 text-amber-600 dark:text-amber-400' 
              : 'bg-emerald-100 dark:bg-emerald-950/70 text-emerald-600 dark:text-emerald-400'
          }`}>
            {isDanger ? (
              <ShieldAlert className="w-7 h-7" />
            ) : isWarn ? (
              <AlertTriangle className="w-7 h-7" />
            ) : (
              <ShieldCheck className="w-7 h-7" />
            )}
          </div>

          <div className="min-w-0 pr-6">
            <span className={`inline-block px-2.5 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider ${
              isDanger 
                ? 'bg-red-600 text-white' 
                : isWarn 
                ? 'bg-amber-500 text-slate-950' 
                : 'bg-emerald-600 text-white'
            }`}>
              {isDanger ? 'High Risk' : isWarn ? 'Suspicious' : 'Low Risk'}
            </span>

            <h2 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white mt-1 leading-snug">
              {verdictText}
            </h2>

            <p className="text-xs font-mono text-slate-500 dark:text-slate-400 truncate mt-1">
              {domain || url}
            </p>
          </div>
        </div>

        {/* 2. SIMPLE VISUAL RISK INDICATOR (Score out of 100) */}
        <div className="my-5 p-4 rounded-2xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Risk Score
            </span>
            <span className={`text-xl font-black font-mono ${
              isDanger ? 'text-red-600 dark:text-red-400' : isWarn ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'
            }`}>
              {riskScore} <span className="text-xs font-normal text-slate-400">/ 100</span>
            </span>
          </div>

          {/* Clean Progress Meter */}
          <div className="w-full h-2.5 bg-slate-200 dark:bg-slate-800 rounded-full overflow-hidden">
            <div 
              style={{ width: `${Math.max(5, riskScore)}%` }}
              className={`h-full rounded-full transition-all duration-500 ${
                isDanger ? 'bg-red-600' : isWarn ? 'bg-amber-500' : 'bg-emerald-500'
              }`}
            />
          </div>

          <div className="flex justify-between items-center text-[10px] font-medium text-slate-400 mt-1.5">
            <span>0 (Low risk)</span>
            <span>50 (Suspicious)</span>
            <span>100 (High danger)</span>
          </div>
        </div>

        {/* 3. SHORT EXPLANATION ("Why did Prahar give this result?") */}
        <div className="space-y-3.5 my-5">
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5 flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4 text-blue-600" />
              Why did Prahar give this result?
            </h3>
            
            <p className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed bg-slate-50 dark:bg-dark-950 p-3.5 rounded-xl border border-slate-200 dark:border-slate-800">
              {detection?.explanationSummary || 'Automated analysis completed based on domain structure and security checks.'}
            </p>
          </div>

          {/* Up to 3 Key Reasons Initially */}
          {primaryIndicators.length > 0 && (
            <div className="space-y-2">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Key reasons detected:
              </span>
              <div className="space-y-1.5">
                {primaryIndicators.map((ind, idx) => (
                  <div 
                    key={idx}
                    className="p-2.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs flex items-start gap-2"
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-blue-600 mt-1.5 shrink-0" />
                    <div>
                      <strong className="text-slate-900 dark:text-white font-semibold block">
                        {ind.indicatorName}
                      </strong>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 leading-snug">
                        {ind.description}
                      </p>
                    </div>
                  </div>
                ))}

                {/* Optional "See more details" Expandable Section */}
                {additionalIndicators.length > 0 && (
                  <div>
                    {showAllIndicators && (
                      <div className="space-y-1.5 mt-1.5">
                        {additionalIndicators.map((ind, idx) => (
                          <div 
                            key={idx}
                            className="p-2.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs flex items-start gap-2"
                          >
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-400 mt-1.5 shrink-0" />
                            <div>
                              <strong className="text-slate-900 dark:text-white font-semibold block">
                                {ind.indicatorName}
                              </strong>
                              <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 leading-snug">
                                {ind.description}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    <button
                      type="button"
                      onClick={() => setShowAllIndicators(!showAllIndicators)}
                      className="text-xs font-semibold text-blue-700 dark:text-blue-400 hover:underline mt-1 flex items-center gap-1"
                    >
                      <span>{showAllIndicators ? 'Show fewer reasons' : `See ${additionalIndicators.length} more details`}</span>
                      {showAllIndicators ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 4. RECOMMENDED ACTION */}
          <div className="p-3.5 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900/60">
            <span className="text-[11px] font-bold uppercase tracking-wider text-blue-900 dark:text-blue-300 block mb-1">
              Recommended action:
            </span>
            <p className="text-xs sm:text-sm text-blue-950 dark:text-blue-100 font-semibold leading-relaxed">
              {recommendedActionText}
            </p>
          </div>

          {/* Automated Detection Disclaimer */}
          <p className="text-[11px] text-slate-400 italic">
            Automated detection can make mistakes. Never treat any web address as guaranteed safe. Always verify the sender before sharing private details.
          </p>
        </div>

        {/* Technical Details Collapsible (Hidden by default) */}
        <div className="pt-2 border-t border-slate-100 dark:border-slate-800">
          <button
            type="button"
            onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
            className="w-full flex items-center justify-between py-1.5 text-xs text-slate-400 hover:text-slate-700 dark:hover:text-slate-300 transition-colors"
          >
            <span>{showTechnicalDetails ? 'Hide analysis & model explainability' : 'Show analysis & model explainability'}</span>
            {showTechnicalDetails ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>

          {showTechnicalDetails && (
            <div className="mt-2 space-y-3">
              {/* ML Model Attribution Section */}
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-blue-500" />
                    Machine Learning Model Attribution
                  </span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    activeVerdict?.modelPrediction?.isMlAvailable
                      ? 'bg-emerald-100 dark:bg-emerald-950/80 text-emerald-700 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800'
                      : 'bg-amber-100 dark:bg-amber-950/80 text-amber-700 dark:text-amber-300 border border-amber-300 dark:border-amber-800'
                  }`}>
                    {activeVerdict?.modelPrediction?.isMlAvailable ? 'ML ACTIVE' : 'ML OFFLINE'}
                  </span>
                </div>

                {activeVerdict?.modelPrediction?.isMlAvailable ? (
                  <div className="space-y-1.5 text-[11px] text-slate-600 dark:text-slate-300">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Classifier:</span>
                      <span className="font-semibold text-slate-800 dark:text-slate-100">
                        {activeVerdict.modelPrediction.modelName || 'Logistic Regression'} (v{activeVerdict.modelPrediction.modelVersion || '1.0.0'})
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">ML Phishing Probability:</span>
                      <span className="font-semibold text-slate-800 dark:text-slate-100">
                        {Math.round((activeVerdict.modelPrediction.phishingProbability || 0) * 100)}%
                      </span>
                    </div>
                    {activeVerdict.modelPrediction.explanations?.length > 0 && (
                      <div className="mt-2 pt-2 border-t border-slate-200 dark:border-slate-800">
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Model Synthesis:</span>
                        <ul className="list-disc list-inside space-y-0.5 text-slate-600 dark:text-slate-300">
                          {activeVerdict.modelPrediction.explanations.map((exp, i) => (
                            <li key={i} className="leading-tight">{exp}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-[11px] text-amber-800 dark:text-amber-300/90 space-y-1">
                    <p className="font-medium">
                      {activeVerdict?.modelPrediction?.mlFailureReason || 'Python ML microservice was unreachable at scan time.'}
                    </p>
                    <p className="text-[10px] text-slate-500 dark:text-slate-400">
                      This verdict was established independently using Prahar's 4-layer deterministic heuristic rule engine (lexical, domain reputation, and structural patterns).
                    </p>
                  </div>
                )}

                <div className="mt-2 pt-2 border-t border-slate-200 dark:border-slate-800 text-[10px] text-slate-400 italic">
                  Safety Notice: A low ML probability score does not override high-risk structural heuristic flags.
                </div>
              </div>

              {/* Threat Intelligence Reputation Section */}
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-indigo-500" />
                    External Threat Intelligence
                  </span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    activeVerdict?.threatIntel?.status === 'MALICIOUS'
                      ? 'bg-red-100 dark:bg-red-950/80 text-red-700 dark:text-red-300 border border-red-300 dark:border-red-800'
                      : activeVerdict?.threatIntel?.status === 'CLEAN'
                      ? 'bg-emerald-100 dark:bg-emerald-950/80 text-emerald-700 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-300 dark:border-slate-700'
                  }`}>
                    {activeVerdict?.threatIntel?.status || 'NOT CONFIGURED'}
                  </span>
                </div>

                <div className="space-y-1.5 text-[11px] text-slate-600 dark:text-slate-300">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Provider:</span>
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {activeVerdict?.threatIntel?.providerName || 'Google SafeBrowsing / VirusTotal'}
                    </span>
                  </div>
                  {activeVerdict?.threatIntel?.details && (
                    <p className="text-[10px] text-slate-500 dark:text-slate-400 mt-1">
                      {activeVerdict.threatIntel.details}
                    </p>
                  )}
                  {activeVerdict?.threatIntel?.fromCache && (
                    <span className="inline-block px-1.5 py-0.5 rounded text-[9px] bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-mono">
                      Cached Result (TTL valid)
                    </span>
                  )}
                </div>
              </div>

              {/* Explainable Risk Scoring Breakdown */}
              {activeVerdict?.explainability && (
                <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs space-y-2">
                  <span className="font-bold text-slate-800 dark:text-slate-200 block">
                    Scoring Fusion Rationale
                  </span>
                  <p className="text-[11px] text-slate-600 dark:text-slate-300">
                    {activeVerdict.explainability.scoringRationale}
                  </p>

                  {/* Unavailable Checks (Transparency) */}
                  {activeVerdict.explainability.unavailableChecks?.length > 0 && (
                    <div className="pt-2 border-t border-slate-200 dark:border-slate-800">
                      <span className="text-[10px] font-bold text-amber-700 dark:text-amber-400 uppercase tracking-wider block mb-1">
                        Unavailable Checks (Missing Dependencies):
                      </span>
                      <ul className="list-disc list-inside space-y-0.5 text-[11px] text-slate-500 dark:text-slate-400">
                        {activeVerdict.explainability.unavailableChecks.map((check, i) => (
                          <li key={i}>{check}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Uncertainty Factors */}
                  {activeVerdict.explainability.uncertaintyFactors?.length > 0 && (
                    <div className="pt-2 border-t border-slate-200 dark:border-slate-800">
                      <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">
                        Uncertainty Factors:
                      </span>
                      <ul className="list-disc list-inside space-y-0.5 text-[11px] text-slate-500 dark:text-slate-400">
                        {activeVerdict.explainability.uncertaintyFactors.map((factor, i) => (
                          <li key={i}>{factor}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}

              {/* Protocol & Technical Metadata */}
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-[11px] font-mono space-y-1 text-slate-600 dark:text-slate-300">
                <div className="flex justify-between">
                  <span className="text-slate-400">Protocol:</span>
                  <span>{url.startsWith('https') ? 'HTTPS (Encrypted)' : 'HTTP (Unencrypted)'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Scan Pipeline:</span>
                  <span>{activeVerdict?.modelPrediction?.isMlAvailable ? 'Hybrid ML + Deterministic Heuristics' : 'Deterministic Heuristic Rules'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Target Address:</span>
                  <span className="truncate max-w-[240px]">{url}</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 5. SIMPLE ACTION BUTTONS */}
        <div className="mt-5 pt-4 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3">
          
          <div className="flex items-center gap-2 w-full sm:w-auto">
            {/* Scan Another */}
            <button
              type="button"
              onClick={handleScanAnother}
              className="flex-1 sm:flex-initial prahar-btn-secondary py-2.5 px-4 text-xs font-semibold"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Scan another</span>
            </button>

            {/* Report This Threat */}
            <button
              type="button"
              onClick={() => {
                setIsVerdictModalOpen(false);
                setIsReportModalOpen(true);
              }}
              className="flex-1 sm:flex-initial px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-semibold text-slate-700 dark:text-slate-300 transition-colors"
            >
              Report threat
            </button>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            {/* What should I do? */}
            {isDanger && (
              <button
                type="button"
                onClick={() => {
                  setIsVerdictModalOpen(false);
                  openEmergencyPlaybook(threatCategory);
                }}
                className="prahar-btn-danger py-2.5 px-4 text-xs font-bold"
              >
                What should I do?
              </button>
            )}

            {/* Ask Prahar AI Assistant */}
            <button
              type="button"
              onClick={handleAskChatbot}
              className="prahar-btn-primary py-2.5 px-4 text-xs font-bold flex items-center gap-1.5"
              title="Ask Prahar AI Assistant about this result"
            >
              <Sparkles className="w-3.5 h-3.5 text-white" />
              <span>Ask Prahar</span>
            </button>
          </div>

        </div>

      </div>
    </div>
  );
}
