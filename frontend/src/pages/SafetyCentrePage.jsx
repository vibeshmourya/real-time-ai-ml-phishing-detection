import React, { useState, useEffect } from 'react';
import { 
  ShieldAlert, PhoneCall, Globe, CheckCircle2, AlertTriangle, FileText, 
  Copy, Plus, Check, ChevronRight, Clock, ExternalLink, HelpCircle, ArrowRight
} from 'lucide-react';
import { incidentApi } from '../api/incidentApi';
import { useAuth } from '../context/AuthContext';

export default function SafetyCentrePage({ setActiveTab }) {
  const { isAuthenticated, openAuthModal } = useAuth();

  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [complaintDraft, setComplaintDraft] = useState(null);
  const [copiedDraft, setCopiedDraft] = useState(false);

  // New Incident Wizard Modal
  const [showWizard, setShowWizard] = useState(false);
  const [wizardStep, setWizardStep] = useState(1);
  const [wizardForm, setWizardForm] = useState({
    incidentType: 'UNAUTHORIZED_DEBIT',
    amount: '',
    bankOrProvider: 'State Bank of India (SBI)',
    complaintReference: '',
    evidenceNotes: '',
  });

  const incidentTypes = [
    { id: 'UNAUTHORIZED_DEBIT', label: 'Unauthorized Bank Debit', icon: '💸', desc: 'Money deducted without your OTP or approval' },
    { id: 'UPI_FRAUD', label: 'UPI / Collect Request Scam', icon: '📱', desc: 'Scammed via GPay, PhonePe, Paytm, or QR code' },
    { id: 'ATM_CARD_FRAUD', label: 'ATM / Card Cloned', icon: '💳', desc: 'Physical card swiped or unauthorized ATM cash withdrawal' },
    { id: 'OTP_DISCLOSURE', label: 'Shared OTP or Password', icon: '🔑', desc: 'Accidentally gave verification code or NetBanking password' },
    { id: 'PHISHING_LINK_CLICKED', label: 'Clicked Phishing Link', icon: '🔗', desc: 'Opened fake bank or KYC link and entered details' },
    { id: 'PHONE_THEFT', label: 'Phone Lost or Stolen', icon: '🚨', desc: 'Lost mobile device containing SIM and banking apps' },
    { id: 'SIM_SWAP', label: 'SIM Swap Suspicion', icon: '📶', desc: 'Phone suddenly shows No Service or deactivated SIM' },
    { id: 'MALICIOUS_APP', label: 'Installed Suspicious APK', icon: '📦', desc: 'Downloaded screen share or fake app from link' },
  ];

  useEffect(() => {
    if (isAuthenticated) {
      loadIncidents();
    } else {
      setLoading(false);
    }
  }, [isAuthenticated]);

  const loadIncidents = async () => {
    setLoading(true);
    try {
      const data = await incidentApi.getIncidents({ page: 0, size: 20 });
      setIncidents(data.content || []);
      if (data.content && data.content.length > 0 && !selectedIncident) {
        setSelectedIncident(data.content[0]);
      }
    } catch (err) {
      console.error('Failed to load incidents', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateIncident = async (e) => {
    e.preventDefault();
    try {
      const created = await incidentApi.createIncident({
        ...wizardForm,
        amount: wizardForm.amount ? parseFloat(wizardForm.amount) : null,
      });
      setShowWizard(false);
      setWizardStep(1);
      setWizardForm({
        incidentType: 'UNAUTHORIZED_DEBIT',
        amount: '',
        bankOrProvider: 'State Bank of India (SBI)',
        complaintReference: '',
        evidenceNotes: '',
      });
      loadIncidents();
      setSelectedIncident(created);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to record incident');
    }
  };

  const handleToggleChecklist = async (stepId) => {
    if (!selectedIncident) return;
    const current = selectedIncident.checklist || [];
    const updated = current.map(item => 
      item.id === stepId ? { ...item, completed: !item.completed } : item
    );

    try {
      const saved = await incidentApi.updateChecklist(selectedIncident.id, updated);
      setSelectedIncident(saved);
      setIncidents(incidents.map(i => i.id === saved.id ? saved : i));
    } catch (err) {
      console.error('Checklist update failed', err);
    }
  };

  const handleLoadComplaintDraft = async (incidentId) => {
    try {
      const draft = await incidentApi.getComplaintDraft(incidentId);
      setComplaintDraft(draft);
    } catch (err) {
      alert('Failed to generate draft');
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setCopiedDraft(true);
    setTimeout(() => setCopiedDraft(false), 2500);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-in fade-in duration-200">
      
      {/* Header & Quick Emergency Contact */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
            Fraud & Safety Centre
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Prioritized emergency actions, official Indian helplines, evidence tracking, and formal complaint drafts.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              if (!isAuthenticated) {
                openAuthModal('login');
              } else {
                setShowWizard(true);
              }
            }}
            className="flex items-center gap-1.5 px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow-xs transition"
          >
            <Plus className="w-4 h-4" />
            <span>Report New Incident</span>
          </button>
        </div>
      </div>

      {/* Official Government Hotlines Banner */}
      <div className="bg-gradient-to-r from-red-600 to-rose-700 rounded-3xl p-6 text-white shadow-md">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-1">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-white/20 text-white font-bold text-[10px] tracking-wider uppercase">
              Official Indian Cybercrime Protocol
            </div>
            <h2 className="text-xl font-black">Act Within the Golden Hour (2-3 Hours)</h2>
            <p className="text-xs text-red-100 max-w-xl">
              Reporting financial fraud immediately enables cyber police to alert beneficiary banks and freeze money in transit before it is withdrawn.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <a
              href="tel:1930"
              className="flex items-center gap-2 px-5 py-3 bg-white text-red-700 font-black rounded-2xl shadow-sm hover:bg-red-50 transition text-sm"
            >
              <PhoneCall className="w-4 h-4" />
              <span>Call Helpline 1930</span>
            </a>

            <a
              href="https://cybercrime.gov.in"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 px-4 py-3 bg-red-800/80 hover:bg-red-800 text-white font-bold rounded-2xl transition text-xs border border-white/20"
            >
              <Globe className="w-4 h-4" />
              <span>cybercrime.gov.in</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>
      </div>

      {/* Main Content Area: Incident Tracker & Action Guide */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left Column: Recorded Incidents List */}
        <div className="lg:col-span-1 bg-white border border-slate-200 rounded-3xl p-5 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Your Incident Records
            </h2>
            <span className="text-[11px] font-bold text-slate-400">
              {incidents.length} Recorded
            </span>
          </div>

          {!isAuthenticated ? (
            <div className="py-8 text-center text-xs text-slate-500">
              <p className="mb-3">Sign in to track ongoing cyber fraud incidents and save complaint references.</p>
              <button
                onClick={() => openAuthModal('login')}
                className="px-4 py-1.5 bg-blue-700 text-white rounded-xl font-bold"
              >
                Sign In
              </button>
            </div>
          ) : loading ? (
            <div className="py-8 text-center text-xs text-slate-400">Loading incidents...</div>
          ) : incidents.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-400">
              No incidents recorded. If you experience an unauthorized debit or scam, click <strong>Report New Incident</strong> above.
            </div>
          ) : (
            <div className="space-y-2">
              {incidents.map((inc) => (
                <div
                  key={inc.id}
                  onClick={() => setSelectedIncident(inc)}
                  className={`p-3.5 rounded-2xl border text-left cursor-pointer transition ${
                    selectedIncident?.id === inc.id
                      ? 'bg-blue-50/70 border-blue-300 ring-2 ring-blue-600/10'
                      : 'bg-white border-slate-100 hover:border-slate-200'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs font-bold text-slate-900">
                    <span className="truncate">{inc.incidentType.replace(/_/g, ' ')}</span>
                    {inc.amount && (
                      <span className="text-red-600 font-black">₹{Number(inc.amount).toLocaleString('en-IN')}</span>
                    )}
                  </div>
                  <div className="flex items-center justify-between text-[10px] text-slate-400 mt-1">
                    <span>{inc.incidentDateTime ? inc.incidentDateTime.substring(0, 10) : 'Recent'}</span>
                    <span className="font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 uppercase">
                      {inc.status.replace(/_/g, ' ')}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Active Incident Checklist & Complaint Generator */}
        <div className="lg:col-span-2 space-y-6">
          {selectedIncident ? (
            <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-xs space-y-6">
              
              {/* Incident Header */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
                <div>
                  <div className="text-[11px] font-bold text-blue-600 uppercase tracking-wider">
                    Active Incident Response
                  </div>
                  <h3 className="text-lg font-black text-slate-900">
                    {selectedIncident.incidentType.replace(/_/g, ' ')}
                  </h3>
                  <p className="text-xs text-slate-500">
                    Bank / Channel: {selectedIncident.bankOrProvider || 'General'}
                    {selectedIncident.complaintReference && ` • 1930 Ref: ${selectedIncident.complaintReference}`}
                  </p>
                </div>

                <button
                  onClick={() => handleLoadComplaintDraft(selectedIncident.id)}
                  className="flex items-center gap-1.5 px-3.5 py-2 bg-blue-700 hover:bg-blue-800 text-white text-xs font-bold rounded-xl transition self-start"
                >
                  <FileText className="w-3.5 h-3.5" />
                  <span>Generate Bank / Police Letter</span>
                </button>
              </div>

              {/* Step-by-Step Priority Checklist */}
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3">
                  Priority Checklist ({selectedIncident.checklist?.filter(c => c.completed).length || 0} of {selectedIncident.checklist?.length || 0} Done)
                </h4>

                <div className="space-y-2">
                  {selectedIncident.checklist && selectedIncident.checklist.length > 0 ? (
                    selectedIncident.checklist.map((step) => (
                      <div
                        key={step.id}
                        onClick={() => handleToggleChecklist(step.id)}
                        className={`p-3.5 rounded-2xl border flex items-start gap-3 cursor-pointer transition ${
                          step.completed
                            ? 'bg-emerald-50/60 border-emerald-200 text-emerald-900'
                            : 'bg-white border-slate-200 text-slate-800 hover:bg-slate-50'
                        }`}
                      >
                        <div className={`w-5 h-5 rounded-lg flex items-center justify-center shrink-0 mt-0.5 border ${
                          step.completed
                            ? 'bg-emerald-600 border-emerald-600 text-white'
                            : 'border-slate-300 bg-white'
                        }`}>
                          {step.completed && <Check className="w-3.5 h-3.5" />}
                        </div>
                        <div className="flex-1 text-xs">
                          <span className={`font-semibold ${step.completed ? 'line-through text-slate-400' : ''}`}>
                            {step.text}
                          </span>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-xs text-slate-400 py-3">No specific checklist assigned.</div>
                  )}
                </div>
              </div>

              {/* Evidence & Notes */}
              {selectedIncident.evidenceNotes && (
                <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl text-xs space-y-1">
                  <span className="font-bold text-slate-700">Evidence & Incident Notes:</span>
                  <p className="text-slate-600 font-mono text-[11px] whitespace-pre-wrap">
                    {selectedIncident.evidenceNotes}
                  </p>
                </div>
              )}

            </div>
          ) : (
            <div className="bg-white border border-slate-200 rounded-3xl p-8 text-center text-xs text-slate-500 space-y-4">
              <ShieldAlert className="w-12 h-12 text-slate-300 mx-auto" />
              <h3 className="text-base font-bold text-slate-900">Incident Guidance Hub</h3>
              <p className="max-w-md mx-auto text-slate-500">
                Select an existing incident on the left or create a new report to view structured checklists and generate formal dispute letters.
              </p>
            </div>
          )}
        </div>

      </div>

      {/* Complaint Draft Modal */}
      {complaintDraft && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-2xl w-full max-h-[85vh] flex flex-col shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-3">
              <div>
                <h3 className="text-base font-bold text-slate-900">{complaintDraft.recipient}</h3>
                <p className="text-xs text-slate-500">Subject: {complaintDraft.subject}</p>
              </div>
              <button
                onClick={() => setComplaintDraft(null)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold p-1"
              >
                ✕
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 bg-slate-50 border border-slate-200 rounded-2xl font-mono text-xs text-slate-800 whitespace-pre-wrap leading-relaxed">
              {complaintDraft.body}
            </div>

            <div className="flex items-center justify-between pt-4 mt-2">
              <span className="text-[11px] text-slate-400">
                Quotes RBI Zero-Liability circular & IT Act 2000.
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setComplaintDraft(null)}
                  className="px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-xl text-xs font-bold text-slate-700 transition"
                >
                  Close
                </button>
                <button
                  onClick={() => copyToClipboard(complaintDraft.body)}
                  className="flex items-center gap-1.5 px-4 py-2 bg-blue-700 hover:bg-blue-800 rounded-xl text-xs font-bold text-white transition"
                >
                  {copiedDraft ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedDraft ? 'Copied to Clipboard!' : 'Copy Letter'}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* New Incident Wizard Modal */}
      {showWizard && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-lg w-full shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <h2 className="text-base font-bold text-slate-900 mb-1">Report a Cyber / Fraud Incident</h2>
            <p className="text-xs text-slate-500 mb-4">
              Step {wizardStep} of 2 — Prahar prepares your emergency checklist and dispute draft.
            </p>

            {wizardStep === 1 ? (
              <div className="space-y-3">
                <label className="text-xs font-bold text-slate-700 block">Select Incident Type:</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-64 overflow-y-auto">
                  {incidentTypes.map((t) => (
                    <button
                      key={t.id}
                      type="button"
                      onClick={() => {
                        setWizardForm({ ...wizardForm, incidentType: t.id });
                        setWizardStep(2);
                      }}
                      className={`p-3 rounded-2xl border text-left flex items-start gap-2.5 transition ${
                        wizardForm.incidentType === t.id
                          ? 'bg-blue-50 border-blue-400 text-blue-900 font-bold'
                          : 'bg-white border-slate-100 hover:bg-slate-50 text-slate-800'
                      }`}
                    >
                      <span className="text-lg">{t.icon}</span>
                      <div className="text-xs">
                        <div className="font-bold">{t.label}</div>
                        <div className="text-[10px] text-slate-400 line-clamp-1">{t.desc}</div>
                      </div>
                    </button>
                  ))}
                </div>

                <div className="flex justify-end pt-3">
                  <button
                    type="button"
                    onClick={() => setShowWizard(false)}
                    className="px-4 py-2 bg-slate-100 rounded-xl text-xs font-bold text-slate-700"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleCreateIncident} className="space-y-3 text-xs">
                <div>
                  <label className="font-bold text-slate-700 block mb-1">Amount Involved (₹) (if applicable)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="e.g. 5000"
                    value={wizardForm.amount}
                    onChange={(e) => setWizardForm({ ...wizardForm, amount: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl font-bold"
                  />
                </div>

                <div>
                  <label className="font-bold text-slate-700 block mb-1">Bank / Service Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. State Bank of India (SBI)"
                    value={wizardForm.bankOrProvider}
                    onChange={(e) => setWizardForm({ ...wizardForm, bankOrProvider: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl"
                  />
                </div>

                <div>
                  <label className="font-bold text-slate-700 block mb-1">1930 / Portal Complaint Ref (optional)</label>
                  <input
                    type="text"
                    placeholder="e.g. 2026-NCRP-8491823"
                    value={wizardForm.complaintReference}
                    onChange={(e) => setWizardForm({ ...wizardForm, complaintReference: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl"
                  />
                </div>

                <div>
                  <label className="font-bold text-slate-700 block mb-1">Evidence Notes / UTR / Scam Phone Numbers</label>
                  <textarea
                    rows="3"
                    placeholder="Paste UTR reference, scammer phone number, or SMS text..."
                    value={wizardForm.evidenceNotes}
                    onChange={(e) => setWizardForm({ ...wizardForm, evidenceNotes: e.target.value })}
                    className="w-full p-2.5 border border-slate-200 rounded-xl font-mono text-[11px]"
                  />
                </div>

                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setWizardStep(1)}
                    className="flex-1 py-2 bg-slate-100 hover:bg-slate-200 rounded-xl font-bold text-slate-700 transition"
                  >
                    Back
                  </button>
                  <button
                    type="submit"
                    className="flex-1 py-2 bg-red-600 hover:bg-red-700 rounded-xl font-bold text-white transition"
                  >
                    Create Incident Record
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

    </div>
  );
}
