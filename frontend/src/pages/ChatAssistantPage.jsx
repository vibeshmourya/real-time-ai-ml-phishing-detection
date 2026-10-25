import React, { useState, useEffect, useRef } from 'react';
import { 
  Bot, Send, User, Trash2, ShieldCheck, AlertTriangle, PhoneCall, 
  ExternalLink, Check, Copy, Sparkles, RefreshCw, ChevronRight, HelpCircle,
  WifiOff, AlertCircle, CheckCircle2
} from 'lucide-react';
import { chatApi, ChatServiceError } from '../api/chatApi';

const DEFAULT_SUGGESTIONS = [
  '₹5,000 was debited from my account',
  'How do I identify a phishing message?',
  'What should I do if my phone is stolen?',
  'What is the 1930 helpline and Golden Hour?',
  'Summarise my recent transactions',
];

const WELCOME_CARD = {
  id: 'welcome',
  role: 'assistant',
  content: '### Prahar AI Safety Assistant\n\nI am your personal cybersecurity and fraud response advisor. You can describe an incident (like an unrecognized debit, UPI scam, or lost phone), ask how to verify a suspicious message, or request an official complaint draft.',
  timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  followUps: DEFAULT_SUGGESTIONS
};

export default function ChatAssistantPage({ setActiveTab }) {
  const [messages, setMessages] = useState(() => {
    try {
      const saved = sessionStorage.getItem('prahar_chat_messages_full');
      return saved ? JSON.parse(saved) : [WELCOME_CARD];
    } catch {
      return [WELCOME_CARD];
    }
  });

  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [copiedId, setCopiedId] = useState(null);
  const [serviceStatus, setServiceStatus] = useState(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    try {
      sessionStorage.setItem('prahar_chat_messages_full', JSON.stringify(messages));
    } catch (e) {
      console.warn(e);
    }
  }, [messages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  useEffect(() => {
    chatApi.getServiceStatus().then(status => {
      setServiceStatus(status);
    }).catch(() => {
      setServiceStatus({ isOnline: false, provider: 'none', configured: false });
    });
  }, []);

  const handleSend = async (textToSend) => {
    const text = typeof textToSend === 'string' ? textToSend : input;
    if (!text || !text.trim() || isTyping) return;

    const userMsg = {
      id: 'u_' + Date.now(),
      role: 'user',
      content: text.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setIsTyping(true);

    const historyTurns = messages
      .filter(m => m.id !== 'welcome' && !m.isError && m.role)
      .slice(-6)
      .map(m => ({ role: m.role, content: m.content }));

    const payload = {
      message: text.trim(),
      conversationHistory: historyTurns,
    };

    try {
      const res = await chatApi.sendMessage(payload);

      const botMsg = {
        id: 'b_' + Date.now(),
        role: 'assistant',
        content: res.reply || 'Here is the verified guidance for your safety.',
        intent: res.intent,
        confidence: res.confidence,
        isLiveAi: res.isLiveAi,
        provider: res.provider,
        isOfflineFallback: false,
        actionChecklist: res.actionChecklist || [],
        followUps: res.followUpSuggestions || [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages(prev => [...prev, botMsg]);
    } catch (err) {
      const errorMsg = {
        id: 'err_' + Date.now(),
        role: 'assistant',
        isError: true,
        errorType: err.type || 'CONNECTION_ERROR',
        errorMessage: err.message,
        statusCode: err.status,
        lastPayload: payload,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, errorMsg]);
    } finally {
      setIsTyping(false);
    }
  };

  const handleLoadOfflineGuidance = (payload, errMessage) => {
    const offlineRes = chatApi.getOfflineEmergencyGuidance(payload, { message: errMessage });
    const botMsg = {
      id: 'bot_offline_' + Date.now(),
      role: 'assistant',
      content: offlineRes.reply,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isLiveAi: false,
      isOfflineFallback: true,
      provider: 'prahar_offline_cached',
      intent: offlineRes.intent,
      actionChecklist: offlineRes.actionChecklist || [],
      followUps: offlineRes.followUpSuggestions || []
    };
    setMessages(prev => [...prev, botMsg]);
  };

  const handleClear = () => {
    sessionStorage.removeItem('prahar_chat_messages_full');
    setMessages([WELCOME_CARD]);
  };

  const copyText = (id, text) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-4 animate-in fade-in duration-200 font-sans">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2">
              <Bot className="w-7 h-7 text-blue-700 dark:text-blue-500" />
              <span>Ask Prahar Assistant</span>
            </h1>

            {/* Provider Status Pill */}
            {serviceStatus && !serviceStatus.isOnline ? (
              <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-red-100 dark:bg-red-950/80 text-red-700 dark:text-red-300 border border-red-300 dark:border-red-800 font-semibold flex items-center gap-1">
                <WifiOff className="w-3 h-3" /> Offline Mode
              </span>
            ) : serviceStatus?.configured && serviceStatus?.provider !== 'none' ? (
              <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-purple-100 dark:bg-purple-950/80 text-purple-700 dark:text-purple-300 border border-purple-300 dark:border-purple-800 font-semibold">
                AI: {serviceStatus.provider}
              </span>
            ) : (
              <span 
                className="text-[11px] px-2.5 py-0.5 rounded-full bg-blue-100 dark:bg-blue-950/80 text-blue-700 dark:text-blue-300 border border-blue-300 dark:border-blue-800 font-semibold"
                title="Operating in Prahar Rule Guidance Mode"
              >
                Rule Safety Engine
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Contextual conversational guidance for threats, fraud containment, and financial queries.
          </p>
        </div>

        <button
          onClick={handleClear}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-xl transition font-semibold"
        >
          <Trash2 className="w-3.5 h-3.5" />
          <span>Clear Conversation</span>
        </button>
      </div>

      {/* Chat Container */}
      <div className="bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-xs flex flex-col h-[650px] overflow-hidden">
        
        {/* Messages Body */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4 bg-slate-50/50 dark:bg-dark-950/50">
          {messages.map((m) => {
            const isBot = m.role === 'assistant';

            if (m.isError) {
              return (
                <div key={m.id} className="w-full p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 rounded-2xl space-y-2">
                  <div className="flex items-start gap-2.5">
                    <AlertCircle className="w-5 h-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
                    <div className="flex-1 text-xs">
                      <div className="flex items-center gap-2 font-bold text-red-800 dark:text-red-200 text-sm">
                        <span>Service Alert: {m.errorType?.replace(/_/g, ' ') || 'Connection Error'}</span>
                        {m.statusCode && <span className="font-mono text-xs">({m.statusCode})</span>}
                      </div>
                      <p className="text-red-700 dark:text-red-300 mt-1.5 leading-relaxed">
                        {m.errorMessage}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2.5 pt-2 border-t border-red-100 dark:border-red-900/40 text-xs">
                    <button
                      onClick={() => handleSend(m.lastPayload?.message)}
                      className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-xl font-semibold flex items-center gap-1.5 transition"
                    >
                      <RefreshCw className="w-3.5 h-3.5" /> Retry Request
                    </button>
                    <button
                      onClick={() => handleLoadOfflineGuidance(m.lastPayload, m.errorMessage)}
                      className="px-3 py-1.5 bg-white dark:bg-slate-800 text-red-700 dark:text-red-300 hover:bg-red-100 dark:hover:bg-slate-700 rounded-xl font-semibold border border-red-200 dark:border-red-800 transition"
                    >
                      View Emergency Offline Advice
                    </button>
                  </div>
                </div>
              );
            }

            return (
              <div
                key={m.id}
                className={`flex gap-3 ${isBot ? 'items-start' : 'items-start flex-row-reverse'}`}
              >
                <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 text-white ${
                  isBot ? 'bg-blue-700' : 'bg-slate-800 dark:bg-slate-700'
                }`}>
                  {isBot ? <Bot className="w-4 h-4" /> : <User className="w-4 h-4" />}
                </div>

                <div className={`max-w-[85%] space-y-2 ${
                  isBot ? 'text-slate-800 dark:text-slate-100' : 'text-white'
                }`}>
                  <div className={`p-4 rounded-2xl text-xs sm:text-sm leading-relaxed whitespace-pre-wrap ${
                    isBot 
                      ? m.isOfflineFallback
                        ? 'bg-amber-50/80 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800 text-slate-800 dark:text-slate-100 shadow-2xs'
                        : 'bg-white dark:bg-dark-850 border border-slate-200 dark:border-slate-800 shadow-2xs' 
                      : 'bg-blue-700 font-medium'
                  }`}>
                    {/* Offline Banner pill if cached fallback */}
                    {m.isOfflineFallback && (
                      <div className="mb-2 px-2.5 py-1 rounded bg-amber-100 dark:bg-amber-900/60 border border-amber-300 dark:border-amber-700 text-amber-800 dark:text-amber-200 text-xs font-bold inline-flex items-center gap-1.5">
                        <AlertTriangle className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
                        Offline Safety Guidance (Pre-loaded Rules)
                      </div>
                    )}

                    {m.content}

                    {/* Action Checklist Badge */}
                    {m.actionChecklist && m.actionChecklist.length > 0 && !m.isLiveAi && (
                      <div className="mt-4 pt-3 border-t border-slate-200 dark:border-slate-800 space-y-2">
                        <div className="text-[11px] font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider flex items-center gap-1">
                          <CheckCircle2 className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
                          <span>Immediate Action Checklist:</span>
                        </div>
                        <ul className="space-y-1.5 text-xs text-slate-700 dark:text-slate-300">
                          {m.actionChecklist.map((act, idx) => (
                            <li key={idx} className="flex items-start gap-2 bg-slate-50 dark:bg-slate-900 p-2 rounded-xl border border-slate-200 dark:border-slate-800">
                              <span className="w-4 h-4 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                                {idx + 1}
                              </span>
                              <span>{act}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {isBot && (
                      <div className="flex items-center justify-between text-[11px] text-slate-400 dark:text-slate-500 mt-2.5 pt-2 border-t border-slate-100 dark:border-slate-800">
                        <span>
                          {m.timestamp}
                          {m.isOfflineFallback && ' • Offline mode'}
                        </span>
                        <button
                          onClick={() => copyText(m.id, m.content)}
                          className="hover:text-slate-700 dark:hover:text-slate-200 transition flex items-center gap-1"
                        >
                          {copiedId === m.id ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                          <span>{copiedId === m.id ? 'Copied' : 'Copy'}</span>
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Follow Up Chips */}
                  {isBot && m.followUps && m.followUps.length > 0 && !isTyping && (
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {m.followUps.map((chip, idx) => (
                        <button
                          key={idx}
                          onClick={() => handleSend(chip)}
                          className="px-3 py-1.5 bg-blue-50 dark:bg-blue-950/60 hover:bg-blue-100 dark:hover:bg-blue-900/60 text-blue-800 dark:text-blue-300 rounded-xl text-xs font-semibold transition border border-blue-200 dark:border-blue-900"
                        >
                          {chip}
                        </button>
                      ))}
                    </div>
                  )}

                </div>
              </div>
            );
          })}

          {isTyping && (
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-blue-700 flex items-center justify-center text-white">
                <Bot className="w-4 h-4" />
              </div>
              <div className="p-4 bg-slate-50 dark:bg-dark-850 rounded-2xl flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-blue-600 animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-2 h-2 rounded-full bg-blue-600 animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-2 h-2 rounded-full bg-blue-600 animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <form
          onSubmit={(e) => { e.preventDefault(); handleSend(); }}
          className="p-4 bg-white dark:bg-dark-900 border-t border-slate-200 dark:border-slate-800 flex items-center gap-3"
        >
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type your cybersecurity or fraud query here..."
            className="flex-1 px-4 py-3 bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-700 rounded-2xl text-xs sm:text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-600"
          />
          <button
            type="submit"
            disabled={!input.trim() || isTyping}
            className="px-5 py-3 bg-blue-700 hover:bg-blue-800 text-white rounded-2xl font-bold text-xs sm:text-sm disabled:opacity-50 transition active:scale-95 flex items-center gap-1.5 shrink-0"
          >
            <span>Send</span>
            <Send className="w-4 h-4" />
          </button>
        </form>

      </div>
    </div>
  );
}
