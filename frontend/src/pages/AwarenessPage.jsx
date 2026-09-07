import React, { useState } from 'react';
import CyberAwarenessHub from '../components/CyberAwarenessHub';
import { 
  Award, CheckCircle2, XCircle, HelpCircle, ArrowRight, RotateCcw, 
  BookOpen, Sparkles, Shield, AlertTriangle
} from 'lucide-react';
import { useScan } from '../context/ScanContext';

const QUIZ_QUESTIONS = [
  {
    id: 1,
    question: 'You receive an SMS: "Electricity power will be cut tonight at 9.30 PM. Call officer at 9876543210 immediately." What should you do?',
    options: [
      'Call the officer immediately to prevent disconnection',
      'Ignore the message and verify your bill status only on the official state electricity portal',
      'Forward the message to your family group',
      'Download the APK attachment provided in the SMS'
    ],
    correctAnswer: 1,
    explanation: 'Scammers use panic regarding utility disconnections. Always check billing directly with the official power board.'
  },
  {
    id: 2,
    question: 'Which of the following URLs is a dangerous spoofing attempt mimicking HDFC Bank?',
    options: [
      'https://netbanking.hdfcbank.com/netbanking/',
      'https://www.hdfcbank.com/personal',
      'http://hdfcbank.secure-login-verify.tk/portal',
      'https://leads.hdfcbank.com/loans'
    ],
    correctAnswer: 2,
    explanation: 'The third URL is hosted on the apex domain "secure-login-verify.tk" using HDFC as a deceptive subdomain prefix.'
  },
  {
    id: 3,
    question: 'A job recruiter on WhatsApp offers ₹4,000/day for liking YouTube videos, but asks for a ₹500 registration deposit. Is this legitimate?',
    options: [
      'Yes, small registration fees are normal',
      'Yes, if they show a company certificate',
      'No, legitimate companies never ask candidates to pay fees to start work',
      'Yes, if they promise 100% daily refunds'
    ],
    correctAnswer: 2,
    explanation: 'Task and rating scams lure victims with easy money then demand escalating deposits that cannot be withdrawn.'
  }
];

export default function AwarenessPage() {
  const { showToast } = useScan();
  const [selectedAnswers, setSelectedAnswers] = useState({});
  const [showResults, setShowResults] = useState(false);

  const handleSelectOption = (qId, optionIdx) => {
    if (showResults) return;
    setSelectedAnswers(prev => ({ ...prev, [qId]: optionIdx }));
  };

  const calculateScore = () => {
    let score = 0;
    QUIZ_QUESTIONS.forEach(q => {
      if (selectedAnswers[q.id] === q.correctAnswer) {
        score++;
      }
    });
    return score;
  };

  const handleReset = () => {
    setSelectedAnswers({});
    setShowResults(false);
  };

  const score = calculateScore();

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-10">
      
      {/* Academy Hero */}
      <div className="text-center max-w-2xl mx-auto">
        <span className="text-xs font-bold uppercase tracking-widest bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 px-3 py-1 rounded-full">
          Digital Defense Academy
        </span>
        <h1 className="text-3xl sm:text-4xl font-black text-white mt-3 tracking-tight">
          Cyber Threat Awareness & Prevention
        </h1>
        <p className="text-sm text-slate-400 mt-2">
          Master the psychological tactics used by modern scammers in under 2 minutes.
        </p>
      </div>

      {/* Micro-Guides Hub */}
      <div>
        <CyberAwarenessHub />
      </div>

      {/* Interactive Spot-the-Phish Quiz */}
      <div className="glass-panel p-6 sm:p-8 border border-slate-800">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-6 pb-4 border-b border-slate-800">
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-amber-400" />
              Spot-the-Phish Interactive Challenge
            </h3>
            <p className="text-xs text-slate-400">
              Test your ability to identify real-world digital deception.
            </p>
          </div>

          {showResults && (
            <button
              onClick={handleReset}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:text-white text-xs font-semibold"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Retake Quiz</span>
            </button>
          )}
        </div>

        <div className="space-y-6">
          {QUIZ_QUESTIONS.map((q, qIdx) => {
            const isAnswered = selectedAnswers[q.id] !== undefined;
            const isCorrect = selectedAnswers[q.id] === q.correctAnswer;

            return (
              <div key={q.id} className="p-4 rounded-xl bg-dark-950/70 border border-slate-800 space-y-3">
                <p className="text-xs sm:text-sm font-bold text-white">
                  {qIdx + 1}. {q.question}
                </p>

                <div className="space-y-2">
                  {q.options.map((opt, optIdx) => {
                    const isSelected = selectedAnswers[q.id] === optIdx;
                    let optionStyle = 'bg-dark-900 border-slate-800 text-slate-300 hover:border-slate-700';

                    if (showResults) {
                      if (optIdx === q.correctAnswer) {
                        optionStyle = 'bg-emerald-950/40 border-emerald-500 text-emerald-200 font-semibold';
                      } else if (isSelected) {
                        optionStyle = 'bg-rose-950/40 border-rose-500 text-rose-200';
                      }
                    } else if (isSelected) {
                      optionStyle = 'bg-indigo-600/30 border-indigo-500 text-white font-semibold';
                    }

                    return (
                      <button
                        key={optIdx}
                        type="button"
                        onClick={() => handleSelectOption(q.id, optIdx)}
                        className={`w-full p-3 rounded-xl border text-left text-xs transition-all flex items-center justify-between ${optionStyle}`}
                      >
                        <span>{opt}</span>
                        {showResults && optIdx === q.correctAnswer && (
                          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 ml-2" />
                        )}
                        {showResults && isSelected && optIdx !== q.correctAnswer && (
                          <XCircle className="w-4 h-4 text-rose-400 shrink-0 ml-2" />
                        )}
                      </button>
                    );
                  })}
                </div>

                {showResults && (
                  <div className="p-3 rounded-lg bg-dark-900 border border-slate-800 text-[11px] text-slate-400">
                    <span className="font-bold text-indigo-300">Explanation: </span>
                    {q.explanation}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Submit Quiz / Results Banner */}
        <div className="mt-6 pt-5 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
          {showResults ? (
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center font-bold">
                <Award className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-bold text-white">
                  Score: {score} / {QUIZ_QUESTIONS.length}
                </p>
                <p className="text-[11px] text-slate-400">
                  {score === QUIZ_QUESTIONS.length
                    ? '🎉 Perfect Score! You are well protected against modern phishing attacks.'
                    : 'Review the explanations above to sharpen your cybersecurity hygiene.'}
                </p>
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-400">
              Select an answer for each scenario to test your cybersecurity readiness.
            </p>
          )}

          {!showResults && (
            <button
              onClick={() => {
                if (Object.keys(selectedAnswers).length < QUIZ_QUESTIONS.length) {
                  showToast('Please answer all quiz questions first', 'warn');
                  return;
                }
                setShowResults(true);
              }}
              className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-md transition-all"
            >
              Submit Answers & Check Score
            </button>
          )}
        </div>

      </div>

    </div>
  );
}
