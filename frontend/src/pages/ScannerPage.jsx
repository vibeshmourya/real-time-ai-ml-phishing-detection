import React, { useState } from 'react';
import QuickScanner from '../components/QuickScanner';
import ImagePhishingAnalyzer from '../components/ImagePhishingAnalyzer';
import { Link2, Layers } from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';

export default function ScannerPage() {
  const [activeMode, setActiveMode] = useState('image'); // 'image' | 'quick'
  const { t } = useLanguage();

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Title & Mode Switcher */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 dark:border-slate-800 pb-6">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">
            {t('scanner.pageTitle', 'Security Threat Scanner')}
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">
            {t('scanner.pageSubtitle', 'Deep multi-channel phishing detection across links, messages, emails, visual screenshots, and QR codes.')}
          </p>
        </div>

        {/* Mode Selector Segmented Control */}
        <div className="flex items-center p-1.5 bg-slate-100 dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-2xl shrink-0">
          <button
            type="button"
            onClick={() => setActiveMode('image')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeMode === 'image'
                ? 'bg-blue-600 text-white shadow-sm shadow-blue-600/30'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            <span>{t('scanner.visualQrLab', 'Visual & QR Lab')}</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveMode('quick')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeMode === 'quick'
                ? 'bg-blue-600 text-white shadow-sm shadow-blue-600/30'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Link2 className="w-3.5 h-3.5" />
            <span>{t('scanner.quickTextLink', 'Quick Text / Link')}</span>
          </button>
        </div>
      </div>

      {/* Main View Area */}
      {activeMode === 'image' ? (
        <ImagePhishingAnalyzer />
      ) : (
        <QuickScanner />
      )}

    </div>
  );
}
