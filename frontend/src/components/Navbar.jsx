import React, { useState } from 'react';
import { Shield, ShieldAlert, AlertTriangle, BookOpen, BarChart3, User, ChevronDown, Sparkles, Activity } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useScan } from '../context/ScanContext';

export default function Navbar({ activeTab, setActiveTab }) {
  const { user, switchRole, isAnalystOrAdmin } = useAuth();
  const { openEmergencyPlaybook, setIsReportModalOpen } = useScan();
  const [isRoleDropdownOpen, setIsRoleDropdownOpen] = useState(false);

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: BarChart3 },
    { id: 'scanner', label: 'Deep Scanner', icon: Shield },
    { id: 'emergency', label: 'Incident Playbook', icon: ShieldAlert, highlight: true },
    { id: 'awareness', label: 'Threat Academy', icon: BookOpen },
  ];

  if (isAnalystOrAdmin) {
    navItems.push({ id: 'analyst', label: 'Security Radar', icon: Activity });
  }

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800/80 bg-dark-950/85 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">
          
          {/* Logo & Brand */}
          <div 
            onClick={() => setActiveTab('dashboard')} 
            className="flex items-center gap-3 cursor-pointer group"
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-cyber-indigo via-indigo-600 to-cyber-cyan flex items-center justify-center shadow-glow-indigo group-hover:scale-105 transition-transform">
              <Shield className="w-6 h-6 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl font-extrabold tracking-tight text-white font-sans">
                  Phish<span className="text-transparent bg-clip-text bg-gradient-to-r from-cyber-cyan to-cyber-indigo">Shield</span>
                </span>
                <span className="text-[10px] uppercase font-bold tracking-widest bg-cyber-indigo/20 text-indigo-300 border border-indigo-500/30 px-1.5 py-0.5 rounded">
                  AI 2.0
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-medium hidden sm:block">
                Real-Time Phishing Detection & Prevention
              </p>
            </div>
          </div>

          {/* Center Navigation */}
          <nav className="hidden md:flex items-center gap-1.5 p-1 rounded-2xl bg-dark-900/90 border border-slate-800/70">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all ${
                    isActive
                      ? 'bg-gradient-to-r from-indigo-600 to-cyber-indigo text-white shadow-md'
                      : item.highlight
                      ? 'text-rose-400 hover:text-rose-300 hover:bg-rose-500/10'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`}
                >
                  <Icon className={`w-4 h-4 ${item.highlight && !isActive ? 'text-rose-400 animate-pulse' : ''}`} />
                  {item.label}
                </button>
              );
            })}
          </nav>

          {/* Right Action Area */}
          <div className="flex items-center gap-3">
            
            {/* Quick Emergency Button */}
            <button
              onClick={() => openEmergencyPlaybook('BANKING_PHISHING')}
              className="flex items-center gap-1.5 px-3 py-1.5 sm:px-3.5 sm:py-2 rounded-xl bg-gradient-to-r from-rose-600/90 to-red-600 hover:from-rose-500 hover:to-red-500 text-white text-xs font-bold shadow-glow-danger transition-all active:scale-95"
              title="Immediate containment steps if you clicked a suspicious link"
            >
              <ShieldAlert className="w-4 h-4" />
              <span className="hidden sm:inline">I Already Clicked!</span>
              <span className="sm:hidden">Help!</span>
            </button>

            {/* Report Phish Button */}
            <button
              onClick={() => setIsReportModalOpen(true)}
              className="hidden lg:flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700/60 text-slate-300 text-xs font-medium transition-all"
            >
              <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
              Report Link
            </button>

            {/* User Profile / Role Selector Switch */}
            <div className="relative">
              <button
                onClick={() => setIsRoleDropdownOpen(!isRoleDropdownOpen)}
                className="flex items-center gap-2 pl-2 pr-2.5 py-1.5 rounded-xl bg-dark-900 border border-slate-800 hover:border-slate-700 transition-all text-xs text-slate-300"
              >
                <div className="w-6 h-6 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center font-bold text-xs">
                  {user.fullName ? user.fullName[0] : 'U'}
                </div>
                <span className="hidden sm:inline font-medium text-slate-200 truncate max-w-[90px]">
                  {user.username}
                </span>
                <ChevronDown className="w-3.5 h-3.5 text-slate-500" />
              </button>

              {isRoleDropdownOpen && (
                <div className="absolute right-0 mt-2 w-56 glass-panel p-2 shadow-2xl z-50 animate-in fade-in zoom-in-95">
                  <div className="px-2 py-1.5 border-b border-slate-800 mb-1">
                    <p className="text-xs font-semibold text-white truncate">{user.fullName}</p>
                    <p className="text-[10px] text-slate-400 font-mono">{user.role}</p>
                  </div>
                  <div className="text-[11px] text-slate-400 px-2 py-1 font-semibold uppercase tracking-wider">
                    Switch Test Persona:
                  </div>
                  <button
                    onClick={() => { switchRole('USER'); setIsRoleDropdownOpen(false); }}
                    className={`w-full text-left px-2 py-1.5 rounded-lg text-xs flex items-center justify-between ${
                      user.role === 'ROLE_USER' ? 'bg-indigo-600/20 text-indigo-300 font-semibold' : 'text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <span>Citizen User</span>
                    {user.role === 'ROLE_USER' && <span className="text-[10px]">● Active</span>}
                  </button>
                  <button
                    onClick={() => { switchRole('ANALYST'); setIsRoleDropdownOpen(false); }}
                    className={`w-full text-left px-2 py-1.5 rounded-lg text-xs flex items-center justify-between ${
                      user.role === 'ROLE_SECURITY_ANALYST' ? 'bg-indigo-600/20 text-indigo-300 font-semibold' : 'text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <span>Security Analyst</span>
                    {user.role === 'ROLE_SECURITY_ANALYST' && <span className="text-[10px]">● Active</span>}
                  </button>
                  <button
                    onClick={() => { switchRole('ADMIN'); setIsRoleDropdownOpen(false); }}
                    className={`w-full text-left px-2 py-1.5 rounded-lg text-xs flex items-center justify-between ${
                      user.role === 'ROLE_ADMIN' ? 'bg-indigo-600/20 text-indigo-300 font-semibold' : 'text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <span>System Admin</span>
                    {user.role === 'ROLE_ADMIN' && <span className="text-[10px]">● Active</span>}
                  </button>
                </div>
              )}
            </div>

          </div>
        </div>

        {/* Mobile Tab Row */}
        <div className="flex md:hidden items-center justify-around py-2 border-t border-slate-800/60 overflow-x-auto gap-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap ${
                  isActive
                    ? 'bg-indigo-600 text-white'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                {item.label}
              </button>
            );
          })}
        </div>

      </div>
    </header>
  );
}
