import React, { useState, useEffect, useRef } from 'react';
import { 
  MessageSquare, X, Send, Trash2, Bot, User, Sparkles, 
  ShieldCheck, AlertTriangle, ExternalLink, RefreshCw, ChevronDown, Check, Copy, WifiOff, AlertCircle
} from 'lucide-react';
import { useScan } from '../context/ScanContext';
import { chatApi, ChatServiceError } from '../api/chatApi';

const SUGGESTED_QUESTIONS = [
  'How do I identify a phishing link?',
  '₹5,000 was debited from my account',
  'What should I do if I shared an OTP?',
  'Why was my link flagged?',
  'What is the 1930 helpline and Golden Hour?'
];

const WELCOME_MESSAGE = {
  id: 'welcome',
  role: 'assistant',
  content: 'Hi! I am Prahar AI Safety Assistant. I can help you investigate suspicious links, contain unauthorized bank/UPI debits, and prepare official cybercrime complaints.',
  timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  isLiveAi: false,
  provider: 'system',
  followUps: SUGGESTED_QUESTIONS
};

export default function PraharChatbot() {
  const { 
    isChatOpen, 
    setIsChatOpen, 
    chatScanContext, 
    setChatScanContext, 
    initialChatPrompt, 
    setInitialChatPrompt 
  } = useScan();

  const [messages, setMessages] = useState(() => {
    try {
      const saved = sessionStorage.getItem('prahar_chat_messages');
      return saved ? JSON.parse(saved) : [WELCOME_MESSAGE];
    } catch {
      return [WELCOME_MESSAGE];
    }
  });

  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [copiedId, setCopiedId] = useState(null);
  const [serviceStatus, setServiceStatus] = useState(null);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Sync messages to session storage
  useEffect(() => {
    try {
      sessionStorage.setItem('prahar_chat_messages', JSON.stringify(messages));
    } catch (e) {
      console.warn('Could not save chat history to session storage', e);
    }
  }, [messages]);

  // Scroll to bottom on new message
  useEffect(() => {
    if (isChatOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isTyping, isChatOpen]);

  // Check backend & AI provider status when opened
  useEffect(() => {
    if (isChatOpen) {
      chatApi.getServiceStatus().then(status => {
        setServiceStatus(status);
      }).catch(() => {
        setServiceStatus({ isOnline: false, provider: 'none', configured: false });
      });
    }
  }, [isChatOpen]);

  // Handle incoming initial prompt from context (e.g., from ScanVerdictModal)
  useEffect(() => {
    if (isChatOpen && initialChatPrompt) {
      const promptText = initialChatPrompt;
      setInitialChatPrompt(null);
      handleSend(promptText);
    }
  }, [isChatOpen, initialChatPrompt]);

  // Focus input when chat opens
  useEffect(() => {
    if (isChatOpen) {
      setTimeout(() => {
        inputRef.current?.focus();
      }, 150);
    }
  }, [isChatOpen]);

  const handleSend = async (customMessage = null) => {
    const textToSend = typeof customMessage === 'string' ? customMessage : inputValue;
    if (!textToSend || !textToSend.trim() || isTyping) return;

    const userMsg = {
      id: 'usr_' + Date.now(),
      role: 'user',
      content: textToSend.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages(prev => [...prev, userMsg]);
    setInputValue('');
    setIsTyping(true);

    const historyTurns = messages
      .filter(m => m.id !== 'welcome' && !m.isError && m.role)
      .slice(-6)
      .map(m => ({ role: m.role, content: m.content }));

    const payload = {
      message: textToSend.trim(),
      conversationHistory: historyTurns
    };

    if (chatScanContext) {
      payload.scanId = chatScanContext.scanId;
      payload.scanUrl = chatScanContext.url || chatScanContext.domain;
      payload.scanRiskScore = chatScanContext.riskScore;
      payload.scanRiskLevel = chatScanContext.riskLevel;
      payload.scanThreatCategory = chatScanContext.threatCategory;
      payload.scanExplanation = chatScanContext.detection?.explanationSummary;
    }

    try {
      const res = await chatApi.sendMessage(payload);

      const botMsg = {
        id: 'bot_' + Date.now(),
        role: 'assistant',
        content: res.reply || 'I am ready to help you with your cyber safety questions.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isLiveAi: res.isLiveAi,
        provider: res.provider,
        intent: res.intent,
        isOfflineFallback: false,
        actionChecklist: res.actionChecklist || [],
        followUps: res.followUpSuggestions || []
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
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
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

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleClearChat = () => {
    setMessages([WELCOME_MESSAGE]);
    sessionStorage.removeItem('prahar_chat_messages');
  };

  const handleCopyText = (id, text) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  // Extract latest assistant follow-up chips or fall back to default
  const latestBot = [...messages].reverse().find(m => m.role === 'assistant' && !m.isError && m.followUps?.length > 0);
  const activeChips = latestBot?.followUps?.length > 0 ? latestBot.followUps : SUGGESTED_QUESTIONS;

  return (
    <>
      {/* Floating Launcher Button */}
      {!isChatOpen && (
        <button
          id="ask-prahar-floating-btn"
          onClick={() => setIsChatOpen(true)}
          className="fixed bottom-5 right-5 z-40 flex items-center gap-2.5 px-4 py-3 bg-blue-700 hover:bg-blue-800 text-white rounded-full shadow-lg hover:shadow-xl transition-all duration-200 active:scale-95 group focus:outline-none focus:ring-4 focus:ring-blue-600/30 font-sans"
          aria-label="Open Prahar AI Assistant Chat"
          title="Ask Prahar AI Assistant"
        >
          <div className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center">
            <Sparkles className="w-3.5 h-3.5 text-white" />
          </div>
          <span className="font-bold text-sm tracking-wide">Ask Prahar</span>
          {chatScanContext && (
            <span className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-pulse" title="Scan context attached" />
          )}
        </button>
      )}

      {/* Floating Chat Window */}
      {isChatOpen && (
        <div 
          role="dialog"
          aria-label="Prahar AI Assistant"
          className="fixed bottom-4 right-4 sm:bottom-6 sm:right-6 z-50 w-[calc(100vw-2rem)] sm:w-[420px] h-[580px] max-h-[85vh] bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-4 duration-200 font-sans"
        >
          {/* Header */}
          <div className="p-3.5 sm:p-4 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-8 h-8 rounded-xl bg-blue-600 text-white flex items-center justify-center shrink-0">
                <Bot className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-sm text-white truncate">Prahar Assistant</h3>
                  
                  {/* Honest Status Pill */}
                  {serviceStatus && !serviceStatus.isOnline ? (
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-red-500/20 text-red-300 border border-red-500/30 font-medium flex items-center gap-1">
                      <WifiOff className="w-2.5 h-2.5" /> Offline
                    </span>
                  ) : serviceStatus?.configured && serviceStatus?.provider !== 'none' ? (
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-400/30 font-medium">
                      AI: {serviceStatus.provider}
                    </span>
                  ) : (
                    <span 
                      className="text-[10px] px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-300 border border-blue-400/30 font-medium"
                      title="Operating with verified Prahar safety rules (No external generative AI configured)"
                    >
                      Rule Safety Engine
                    </span>
                  )}
                </div>
                <p className="text-[11px] text-slate-300 truncate">Detect. Understand. Stay Safe.</p>
              </div>
            </div>

            <div className="flex items-center gap-1">
              <button
                onClick={handleClearChat}
                className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-colors"
                title="Clear conversation"
                aria-label="Clear conversation"
              >
                <Trash2 className="w-4 h-4" />
              </button>
              <button
                onClick={() => setIsChatOpen(false)}
                className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-colors"
                title="Close chat"
                aria-label="Close Prahar Chatbot"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Active Scan Context Banner */}
          {chatScanContext && (
            <div className="px-3.5 py-2 bg-blue-50 dark:bg-blue-950/60 border-b border-blue-100 dark:border-blue-900/40 flex items-center justify-between gap-2 text-xs">
              <div className="flex items-center gap-1.5 truncate">
                <ShieldCheck className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400 shrink-0" />
                <span className="text-slate-600 dark:text-slate-300 text-[11px]">Active Target:</span>
                <span className="font-mono font-bold text-slate-900 dark:text-white truncate text-[11px]">
                  {chatScanContext.url || chatScanContext.domain}
                </span>
                <span className={`text-[10px] px-1.5 py-0.2 rounded font-bold ${
                  chatScanContext.riskScore > 60 
                    ? 'bg-red-100 dark:bg-red-950 text-red-700 dark:text-red-300' 
                    : chatScanContext.riskScore > 20 
                    ? 'bg-amber-100 dark:bg-amber-950 text-amber-700 dark:text-amber-300' 
                    : 'bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300'
                }`}>
                  {chatScanContext.riskScore}/100
                </span>
              </div>

              <button
                onClick={() => setChatScanContext(null)}
                className="text-[10px] text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 underline shrink-0"
                title="Remove scan context"
              >
                Clear
              </button>
            </div>
          )}

          {/* Chat Messages Area */}
          <div className="flex-1 overflow-y-auto p-3.5 sm:p-4 space-y-3.5 bg-slate-50/50 dark:bg-dark-950/50">
            {messages.map((msg) => {
              const isUser = msg.role === 'user';

              if (msg.isError) {
                return (
                  <div key={msg.id} className="w-full p-3 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-900/60 rounded-xl space-y-2">
                    <div className="flex items-start gap-2">
                      <AlertCircle className="w-4 h-4 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
                      <div className="flex-1 text-xs">
                        <div className="flex items-center gap-1.5 font-bold text-red-800 dark:text-red-200">
                          <span>Service Alert: {msg.errorType?.replace(/_/g, ' ') || 'Error'}</span>
                          {msg.statusCode && <span className="font-mono text-[10px]">({msg.statusCode})</span>}
                        </div>
                        <p className="text-red-700 dark:text-red-300 mt-1 leading-relaxed">
                          {msg.errorMessage}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 pt-1 border-t border-red-100 dark:border-red-900/40 text-[11px]">
                      <button
                        onClick={() => handleSend(msg.lastPayload?.message)}
                        className="px-2.5 py-1 bg-red-600 hover:bg-red-700 text-white rounded-lg font-semibold flex items-center gap-1 transition-colors"
                      >
                        <RefreshCw className="w-3 h-3" /> Retry
                      </button>
                      <button
                        onClick={() => handleLoadOfflineGuidance(msg.lastPayload, msg.errorMessage)}
                        className="px-2.5 py-1 bg-white dark:bg-slate-800 text-red-700 dark:text-red-300 hover:bg-red-100 dark:hover:bg-slate-700 rounded-lg font-semibold border border-red-200 dark:border-red-800 transition-colors"
                      >
                        View Offline Guidance
                      </button>
                    </div>
                  </div>
                );
              }

              return (
                <div 
                  key={msg.id} 
                  className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} space-y-1`}
                >
                  <div className={`flex items-start gap-2 max-w-[90%] ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
                    {/* Avatar */}
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 text-xs mt-0.5 ${
                      isUser 
                        ? 'bg-blue-700 text-white' 
                        : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-200'
                    }`}>
                      {isUser ? <User className="w-3.5 h-3.5" /> : <Bot className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />}
                    </div>

                    {/* Bubble */}
                    <div className={`relative px-3.5 py-2.5 rounded-2xl text-xs sm:text-[13px] leading-relaxed group ${
                      isUser
                        ? 'bg-blue-700 text-white rounded-tr-sm shadow-xs'
                        : msg.isOfflineFallback
                        ? 'bg-amber-50/80 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800 text-slate-800 dark:text-slate-100 rounded-tl-sm shadow-xs'
                        : 'bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-100 rounded-tl-sm shadow-xs'
                    }`}>
                      {/* Offline Banner pill if cached fallback */}
                      {msg.isOfflineFallback && (
                        <div className="mb-2 px-2 py-0.5 rounded bg-amber-100 dark:bg-amber-900/60 border border-amber-300 dark:border-amber-700 text-amber-800 dark:text-amber-200 text-[10px] font-bold inline-flex items-center gap-1">
                          <AlertTriangle className="w-3 h-3 text-amber-600 dark:text-amber-400" />
                          Offline Safety Guidance (Pre-loaded Rules)
                        </div>
                      )}

                      <div className="whitespace-pre-wrap break-words">
                        {renderFormattedContent(msg.content)}
                      </div>

                      {/* Immediate Action Checklist */}
                      {!isUser && msg.actionChecklist && msg.actionChecklist.length > 0 && (
                        <div className="mt-2.5 pt-2 border-t border-slate-100 dark:border-slate-800 space-y-1">
                          <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                            Immediate Action Checklist:
                          </p>
                          <ul className="space-y-1">
                            {msg.actionChecklist.map((action, aIdx) => (
                              <li key={aIdx} className="flex items-start gap-1.5 text-[11px] text-slate-700 dark:text-slate-300">
                                <span className="w-3.5 h-3.5 rounded-full bg-blue-100 dark:bg-blue-900/60 text-blue-700 dark:text-blue-300 flex items-center justify-center text-[9px] font-bold shrink-0 mt-0.5">
                                  {aIdx + 1}
                                </span>
                                <span>{action}</span>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}

                      {/* Utility metadata bar */}
                      {!isUser && msg.id !== 'welcome' && (
                        <div className="mt-1.5 pt-1.5 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between text-[10px] text-slate-400">
                          <span>
                            {msg.isOfflineFallback
                              ? 'Prahar Offline Rules'
                              : msg.isLiveAi 
                              ? `Live AI (${msg.provider})` 
                              : 'Prahar Safety Engine'}
                            {msg.intent ? ` • ${msg.intent.replace(/_/g, ' ')}` : ''}
                          </span>
                          <button
                            onClick={() => handleCopyText(msg.id, msg.content)}
                            className="hover:text-slate-600 dark:hover:text-slate-200 flex items-center gap-1 transition-colors"
                            title="Copy advice"
                          >
                            {copiedId === msg.id ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                            <span>{copiedId === msg.id ? 'Copied' : 'Copy'}</span>
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                  <span className={`text-[10px] text-slate-400 px-8 ${isUser ? 'text-right' : 'text-left'}`}>
                    {msg.timestamp}
                  </span>
                </div>
              );
            })}

            {/* Typing Indicator */}
            {isTyping && (
              <div className="flex items-start gap-2">
                <div className="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center shrink-0">
                  <Bot className="w-3.5 h-3.5 text-blue-600" />
                </div>
                <div className="px-4 py-2.5 bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-2xl rounded-tl-sm shadow-xs">
                  <div className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-blue-600 animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="w-2 h-2 rounded-full bg-blue-600 animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="w-2 h-2 rounded-full bg-blue-600 animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Dynamic Suggestion Chips */}
          {activeChips && activeChips.length > 0 && !isTyping && (
            <div className="p-2 bg-white dark:bg-dark-900 border-t border-slate-100 dark:border-slate-800 overflow-x-auto">
              <div className="flex items-center gap-1.5 whitespace-nowrap scrollbar-none">
                {activeChips.map((chip, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSend(chip)}
                    className="px-2.5 py-1 rounded-full bg-slate-100 hover:bg-blue-50 dark:bg-slate-800 dark:hover:bg-blue-950/60 text-slate-700 hover:text-blue-700 dark:text-slate-300 dark:hover:text-blue-300 text-[11px] font-medium border border-slate-200 dark:border-slate-700 transition-colors shrink-0"
                  >
                    {chip}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Input Box */}
          <form 
            onSubmit={(e) => { e.preventDefault(); handleSend(); }}
            className="p-2.5 sm:p-3 bg-white dark:bg-dark-900 border-t border-slate-200 dark:border-slate-800 flex items-center gap-2"
          >
            <input
              ref={inputRef}
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask Prahar a question (Enter to send)..."
              className="flex-1 max-h-24 px-3 py-2 bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-700 rounded-xl text-xs sm:text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-600 outline-none transition-all"
            />
            <button
              type="submit"
              disabled={!inputValue.trim() || isTyping}
              className="p-2.5 rounded-xl bg-blue-700 hover:bg-blue-800 text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all active:scale-95 shrink-0"
              title="Send message"
              aria-label="Send message"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>

          {/* Chatbot Safety Guardrail Footer */}
          <div className="px-3 py-1.5 bg-slate-50 dark:bg-dark-950 border-t border-slate-100 dark:border-slate-800 text-center text-[10px] text-slate-400">
            Prahar Cyber Assistant. In financial cyber emergencies, dial <strong>1930</strong> immediately.
          </div>
        </div>
      )}
    </>
  );
}

// Simple markdown formatter helper for bold, code, and bullet points
function renderFormattedContent(text) {
  if (!text) return null;

  const lines = text.split('\n');
  return lines.map((line, idx) => {
    // Bullet points
    if (line.startsWith('- ') || line.startsWith('* ')) {
      return (
        <div key={idx} className="flex items-start gap-1.5 my-0.5">
          <span className="text-blue-600 dark:text-blue-400 font-bold shrink-0">•</span>
          <span>{parseInline(line.substring(2))}</span>
        </div>
      );
    }

    // Numbered lists
    const numMatch = line.match(/^(\d+)\.\s+(.*)/);
    if (numMatch) {
      return (
        <div key={idx} className="flex items-start gap-1.5 my-0.5">
          <span className="font-bold text-slate-500 shrink-0">{numMatch[1]}.</span>
          <span>{parseInline(numMatch[2])}</span>
        </div>
      );
    }

    if (line.trim() === '') {
      return <div key={idx} className="h-1.5" />;
    }

    return <div key={idx}>{parseInline(line)}</div>;
  });
}

function parseInline(str) {
  const parts = [];
  let remaining = str;
  let key = 0;

  while (remaining.length > 0) {
    const boldMatch = remaining.match(/\*\*(.*?)\*\*/);
    const codeMatch = remaining.match(/`(.*?)`/);

    let firstIndex = -1;
    let matchType = null;
    let match = null;

    if (boldMatch && (codeMatch ? boldMatch.index < codeMatch.index : true)) {
      firstIndex = boldMatch.index;
      matchType = 'bold';
      match = boldMatch;
    } else if (codeMatch) {
      firstIndex = codeMatch.index;
      matchType = 'code';
      match = codeMatch;
    }

    if (firstIndex === -1) {
      parts.push(<span key={key++}>{remaining}</span>);
      break;
    }

    if (firstIndex > 0) {
      parts.push(<span key={key++}>{remaining.substring(0, firstIndex)}</span>);
    }

    if (matchType === 'bold') {
      parts.push(<strong key={key++} className="font-bold">{match[1]}</strong>);
      remaining = remaining.substring(firstIndex + match[0].length);
    } else if (matchType === 'code') {
      parts.push(
        <code key={key++} className="px-1 py-0.5 rounded bg-slate-100 dark:bg-slate-800 font-mono text-[11px] text-blue-700 dark:text-blue-300">
          {match[1]}
        </code>
      );
      remaining = remaining.substring(firstIndex + match[0].length);
    }
  }

  return parts;
}
