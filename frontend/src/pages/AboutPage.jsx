import React, { useState } from 'react';
import { 
  Shield, CheckCircle2, ArrowRight, Layers, Cpu, 
  Lock, BookOpen, Smartphone, ChevronDown, ChevronUp, Activity, Sparkles
} from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';

export default function AboutPage() {
  const { t } = useLanguage();
  const [showArchitecture, setShowArchitecture] = useState(false);

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Hero Header */}
      <div className="text-center max-w-2xl mx-auto space-y-3">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
          <Shield className="w-3.5 h-3.5" />
          <span>About the System</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
          About Prahar
        </h1>
        <p className="text-sm sm:text-base text-slate-600 dark:text-slate-400 leading-relaxed">
          Prahar is a real-time, multi-channel AI/ML-based cybersecurity safety system designed to help everyday citizens, students, employees, and seniors identify phishing and social engineering threats before harm occurs.
        </p>
      </div>

      {/* Core Capabilities */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-6 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card space-y-2">
          <span className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 flex items-center justify-center font-bold text-lg mb-3">
            🔍
          </span>
          <h3 className="text-base font-bold text-slate-900 dark:text-white">
            What Prahar Checks
          </h3>
          <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
            Suspicious website links, SMS messages, WhatsApp/Telegram text, email bodies, screenshots, and decoded QR codes.
          </p>
        </div>

        <div className="p-6 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card space-y-2">
          <span className="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 flex items-center justify-center font-bold text-lg mb-3">
            🧠
          </span>
          <h3 className="text-base font-bold text-slate-900 dark:text-white">
            Explainable Detection
          </h3>
          <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
            Combines lexical heuristics with machine learning classification to explain in plain language exactly why something was flagged.
          </p>
        </div>

        <div className="p-6 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 shadow-card space-y-2">
          <span className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-950 text-red-700 dark:text-red-300 flex items-center justify-center font-bold text-lg mb-3">
            🛡️
          </span>
          <h3 className="text-base font-bold text-slate-900 dark:text-white">
            Immediate Response
          </h3>
          <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
            Step-by-step "I Already Clicked" incident containment playbooks, official helpline 1930 guidance, and cyber-awareness lessons.
          </p>
        </div>
      </div>

      {/* 5-Step Simple Flow */}
      <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 space-y-6 shadow-card">
        <h2 className="text-xl font-bold text-slate-900 dark:text-white">
          How Prahar Works (Simple 5-Step Flow)
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-5 gap-3">
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs text-center space-y-1">
            <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-bold mx-auto flex items-center justify-center">1</span>
            <p className="font-bold text-slate-900 dark:text-white">Submit Content</p>
            <p className="text-slate-500 text-[11px]">Paste link, message, or upload image</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs text-center space-y-1">
            <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-bold mx-auto flex items-center justify-center">2</span>
            <p className="font-bold text-slate-900 dark:text-white">Signal Extraction</p>
            <p className="text-slate-500 text-[11px]">Lexical, domain, and language analysis</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs text-center space-y-1">
            <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-bold mx-auto flex items-center justify-center">3</span>
            <p className="font-bold text-slate-900 dark:text-white">ML & Rules</p>
            <p className="text-slate-500 text-[11px]">Ensemble ML models and threat rules</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs text-center space-y-1">
            <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-bold mx-auto flex items-center justify-center">4</span>
            <p className="font-bold text-slate-900 dark:text-white">Risk Score</p>
            <p className="text-slate-500 text-[11px]">Safe, Caution, or High Risk verdict</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-xs text-center space-y-1">
            <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-bold mx-auto flex items-center justify-center">5</span>
            <p className="font-bold text-slate-900 dark:text-white">Actionable Help</p>
            <p className="text-slate-500 text-[11px]">Clear advice and emergency steps</p>
          </div>
        </div>
      </div>

      {/* Technical Architecture for Project Reviewers / Evaluators */}
      <div className="p-6 sm:p-8 rounded-3xl bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <Cpu className="w-5 h-5 text-blue-600" />
              Technical System Architecture (For Evaluators)
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Three-tier microservice architecture with SSRF containment and ML inference.
            </p>
          </div>

          <button
            onClick={() => setShowArchitecture(!showArchitecture)}
            className="prahar-btn-secondary py-1.5 px-3 text-xs flex items-center gap-1"
          >
            <span>{showArchitecture ? 'Hide Diagram' : 'View Architecture'}</span>
            {showArchitecture ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>
        </div>

        {showArchitecture && (
          <div className="p-4 rounded-2xl bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 text-xs font-mono space-y-3 animate-in fade-in">
            <div className="p-3 bg-slate-100 dark:bg-dark-950 rounded-xl border border-slate-200 dark:border-slate-800 overflow-x-auto text-[11px] leading-relaxed">
              <pre>{`[ Client Layer ]
  React (Vite) Frontend  <--->  Manifest V3 Chrome Extension
          │                                 │
          ▼                                 ▼
[ Backend API Layer - Spring Boot 3.3.4 (Java 17/25) ]
  • SSRF Protection Guard & Safe Normalization
  • JWT Authentication & Role-Based Access Control
  • Rule-Based Heuristic Scoring Engine
  • ZXing QR & In-Process Image Processing
  • Audit Logging & Threat Telemetry Management
          │
          ▼
[ Machine Learning Layer - Python FastAPI ]
  • 30-Dimension Lexical & Entropy Feature Extraction
  • Ensemble Model Inference (XGBoost Classifier)
  • LIME / SHAP Explainability & Indicator Attribution
  • OCR Text Pipeline (OpenCV & Character Extraction)`}</pre>
            </div>

            <p className="text-slate-600 dark:text-slate-400 font-sans text-xs">
              <strong>Academic Note:</strong> Prahar is engineered as an integration-focused system combining fast heuristic filters with tree-based machine learning classifiers to deliver deterministic and explainable outputs without black-box opacity.
            </p>
          </div>
        )}
      </div>

    </div>
  );
}
