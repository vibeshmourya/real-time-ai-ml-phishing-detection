import React, { useState, useEffect } from 'react';
import { 
  DollarSign, TrendingUp, TrendingDown, AlertTriangle, CheckCircle2, 
  Upload, Plus, Search, Filter, Download, MessageSquare, ArrowUpRight, 
  ArrowDownLeft, RefreshCw, FileText, HelpCircle, ShieldCheck, Tag,
  Info, X, ChevronRight, Eye, ShieldAlert, Sparkles, Check, AlertCircle, FileSpreadsheet
} from 'lucide-react';
import { transactionApi } from '../api/transactionApi';
import { useAuth } from '../context/AuthContext';

export default function MyMoneyPage({ setActiveTab }) {
  const { isAuthenticated, openAuthModal } = useAuth();

  const [summary, setSummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [directionFilter, setDirectionFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals & Panels
  const [showManualModal, setShowManualModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showSmsModal, setShowSmsModal] = useState(false);
  const [inspectTxn, setInspectTxn] = useState(null);

  // Manual Form State
  const [manualForm, setManualForm] = useState({
    amount: '',
    direction: 'EXPENSE',
    type: 'UPI',
    category: 'SHOPPING',
    merchant: '',
    sourceInstitution: 'SBI',
    transactionReference: '',
    notes: '',
  });

  // Unified Statement Import State (Phase 2)
  const [importFile, setImportFile] = useState(null);
  const [importStep, setImportStep] = useState('select'); // 'select' | 'preview' | 'committing' | 'result'
  const [previewData, setPreviewData] = useState(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [importLoading, setImportLoading] = useState(false);
  const [confirmAmbiguousCheck, setConfirmAmbiguousCheck] = useState(false);
  const [importResult, setImportResult] = useState(null);

  // SMS Analyzer State (Phase 5)
  const [smsInput, setSmsInput] = useState('');
  const [smsAnalyzing, setSmsAnalyzing] = useState(false);
  const [smsResult, setSmsResult] = useState(null);

  // In-App Toast Notification
  const [toast, setToast] = useState(null);

  useEffect(() => {
    if (isAuthenticated) {
      loadData();
    } else {
      setLoading(false);
    }
  }, [isAuthenticated, currentPage, statusFilter, categoryFilter, directionFilter]);

  const showNotification = (type, message) => {
    setToast({ type, message });
    setTimeout(() => {
      setToast(null);
    }, 6000);
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const [sumData, txnData] = await Promise.all([
        transactionApi.getSummary(),
        transactionApi.getTransactions({
          page: currentPage,
          size: 15,
          status: statusFilter || undefined,
          category: categoryFilter || undefined,
          direction: directionFilter || undefined,
        }),
      ]);
      setSummary(sumData);
      setTransactions(txnData.content || []);
      setTotalElements(txnData.totalElements || 0);
    } catch (err) {
      console.error('Failed to load transaction data:', err);
      showNotification('error', 'Could not sync financial records with the server.');
    } finally {
      setLoading(false);
    }
  };

  const handleManualSubmit = async (e) => {
    e.preventDefault();
    if (!manualForm.amount || isNaN(manualForm.amount) || parseFloat(manualForm.amount) <= 0) {
      showNotification('error', 'Please enter a valid positive amount.');
      return;
    }
    try {
      const created = await transactionApi.createManual({
        ...manualForm,
        amount: parseFloat(manualForm.amount),
      });
      setShowManualModal(false);
      setManualForm({
        amount: '',
        direction: 'EXPENSE',
        type: 'UPI',
        category: 'SHOPPING',
        merchant: '',
        sourceInstitution: 'SBI',
        transactionReference: '',
        notes: '',
      });
      if (created.status === 'FLAGGED_SUSPICIOUS' || created.status === 'NEEDS_REVIEW') {
        showNotification('warning', `Transaction recorded and flagged for review: ${created.flagReason || 'Unusual debit amount'}`);
      } else {
        showNotification('success', 'Transaction successfully recorded to your ledger.');
      }
      loadData();
    } catch (err) {
      showNotification('error', err.response?.data?.message || 'Failed to save transaction');
    }
  };

  // Step 1: File selection & Preview
  const handleFilePreview = async (selectedFile) => {
    const file = selectedFile || importFile;
    if (!file) return;

    setImportLoading(true);
    try {
      const preview = await transactionApi.previewImport(file);
      setPreviewData(preview);
      setImportStep('preview');
      setConfirmAmbiguousCheck(!preview.isAmbiguous);
    } catch (err) {
      showNotification('error', err.response?.data?.message || 'Failed to parse statement preview.');
    } finally {
      setImportLoading(false);
    }
  };

  // Step 2: Confirm & Commit
  const handleConfirmImport = async () => {
    if (!previewData || !previewData.previewRows) return;
    if (previewData.isAmbiguous && !confirmAmbiguousCheck) {
      showNotification('warning', 'Please review the detected columns and confirm the verification checkbox.');
      return;
    }

    setImportLoading(true);
    try {
      const result = await transactionApi.confirmImport({
        rows: previewData.previewRows,
        fileName: previewData.fileName,
        confirmAmbiguous: confirmAmbiguousCheck,
      });
      setImportResult(result);
      setImportStep('result');
      showNotification('success', result.message || `Imported ${result.importedCount} transactions.`);
      loadData();
    } catch (err) {
      showNotification('error', err.response?.data?.message || 'Failed to commit imported statement.');
    } finally {
      setImportLoading(false);
    }
  };

  // SMS Fraud & Anomaly Analysis (Phase 5)
  const handleAnalyzeSms = async (e) => {
    e.preventDefault();
    if (!smsInput.trim()) return;

    setSmsAnalyzing(true);
    try {
      const res = await transactionApi.analyzeSmsFraud(smsInput.trim());
      setSmsResult(res);
    } catch (err) {
      showNotification('error', err.response?.data?.message || 'Failed to analyze SMS content.');
    } finally {
      setSmsAnalyzing(false);
    }
  };

  const handleSaveSmsTransaction = async () => {
    if (!smsResult?.extractedTransaction) return;
    try {
      await transactionApi.parseNotification(smsInput.trim());
      showNotification('success', 'Verified transaction added to your ledger.');
      setShowSmsModal(false);
      setSmsInput('');
      setSmsResult(null);
      loadData();
    } catch (err) {
      showNotification('error', err.response?.data?.message || 'Failed to record transaction');
    }
  };

  const handleVerify = async (id) => {
    try {
      await transactionApi.verifyTransaction(id);
      showNotification('success', 'Transaction marked as confirmed and verified.');
      if (inspectTxn && inspectTxn.id === id) {
        setInspectTxn(prev => ({ ...prev, status: 'CONFIRMED', flagReason: null, severity: 'LOW', riskScore: 5 }));
      }
      loadData();
    } catch (err) {
      showNotification('error', 'Verification failed: ' + (err.response?.data?.message || err.message));
    }
  };

  const filteredTransactions = transactions.filter(t => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (t.merchant && t.merchant.toLowerCase().includes(q)) ||
           (t.transactionReference && t.transactionReference.toLowerCase().includes(q)) ||
           (t.notes && t.notes.toLowerCase().includes(q));
  });

  if (!isAuthenticated) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <div className="w-16 h-16 bg-blue-100 text-blue-700 rounded-3xl flex items-center justify-center mx-auto mb-4 shadow-sm">
          <ShieldCheck className="w-8 h-8" />
        </div>
        <h1 className="text-2xl font-black text-slate-900 mb-2">My Money & Personal Financial Security</h1>
        <p className="text-sm text-slate-600 max-w-md mx-auto mb-6">
          Monitor your private transactions, import bank statements, detect unusual debits, and analyze suspicious SMS alerts before financial fraud occurs.
        </p>
        <button
          onClick={() => openAuthModal('login')}
          className="px-6 py-2.5 bg-blue-700 text-white font-bold rounded-xl shadow-md hover:bg-blue-800 transition"
        >
          Sign In to Access My Money
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6 animate-in fade-in duration-200">
      
      {/* Toast Alert */}
      {toast && (
        <div className={`p-4 rounded-2xl flex items-center justify-between text-xs font-semibold shadow-lg transition animate-in slide-in-from-top-2 ${
          toast.type === 'error' ? 'bg-rose-50 border border-rose-200 text-rose-800' :
          toast.type === 'warning' ? 'bg-amber-50 border border-amber-200 text-amber-800' :
          'bg-emerald-50 border border-emerald-200 text-emerald-800'
        }`}>
          <div className="flex items-center gap-2">
            {toast.type === 'error' ? <AlertCircle className="w-4 h-4 shrink-0" /> :
             toast.type === 'warning' ? <AlertTriangle className="w-4 h-4 shrink-0" /> :
             <CheckCircle2 className="w-4 h-4 shrink-0" />}
            <span>{toast.message}</span>
          </div>
          <button onClick={() => setToast(null)} className="p-1 hover:bg-black/5 rounded-lg">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Header & Action Toolbar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-slate-900">My Money & Transactions</h1>
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
              Personal Security
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Personal transaction monitoring, statement imports, and explainable behavioral anomaly detection.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setShowManualModal(true)}
            className="flex items-center gap-1.5 px-3 py-2 bg-blue-700 hover:bg-blue-800 text-white text-xs font-bold rounded-xl shadow-xs transition"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add Entry</span>
          </button>

          <button
            onClick={() => {
              setImportStep('select');
              setImportFile(null);
              setPreviewData(null);
              setShowImportModal(true);
            }}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-xs transition"
          >
            <Upload className="w-3.5 h-3.5" />
            <span>Import Statement</span>
          </button>

          <button
            onClick={() => {
              setSmsResult(null);
              setSmsInput('');
              setShowSmsModal(true);
            }}
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition"
          >
            <MessageSquare className="w-3.5 h-3.5 text-blue-600" />
            <span>SMS Fraud Detector</span>
          </button>

          <a
            href={transactionApi.getExportUrl()}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition"
          >
            <Download className="w-3.5 h-3.5 text-slate-600" />
            <span>Export CSV</span>
          </a>
        </div>
      </div>

      {/* Personalized Baseline Health Banner (Phase 3) */}
      <div className={`p-4 rounded-2xl border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs ${
        summary?.personalizationEstablished 
          ? 'bg-blue-50/70 border-blue-200 text-blue-900' 
          : 'bg-amber-50/80 border-amber-200 text-amber-900'
      }`}>
        <div className="flex items-start gap-2.5">
          <Sparkles className={`w-4 h-4 shrink-0 mt-0.5 ${summary?.personalizationEstablished ? 'text-blue-600' : 'text-amber-600'}`} />
          <div>
            <div className="font-bold flex items-center gap-1.5">
              <span>{summary?.personalizationEstablished ? 'Personalized Anomaly Baseline Active' : 'Personalization Baseline Establishing'}</span>
              <span className={`px-1.5 py-0.2 rounded text-[10px] font-black uppercase ${
                summary?.personalizationEstablished ? 'bg-blue-200/70 text-blue-800' : 'bg-amber-200/80 text-amber-800'
              }`}>
                {summary?.personalizationEstablished ? 'Learned' : 'Cold Start'}
              </span>
            </div>
            <p className="text-[11px] mt-0.5 opacity-90">
              {summary?.baselineStatusMessage || 'Evaluating normal expenditure intervals, median spending, and recurring baselines.'}
            </p>
          </div>
        </div>

        {summary?.personalizationEstablished && (
          <div className="flex items-center gap-4 text-[11px] font-semibold border-t sm:border-t-0 sm:border-l border-blue-200 pt-2 sm:pt-0 sm:pl-4">
            <div>
              <span className="text-blue-500 block text-[10px] uppercase font-bold">Typical Median</span>
              <span className="text-slate-900 font-bold">₹{summary.baselineMedian ? Number(summary.baselineMedian).toLocaleString('en-IN') : '0'}</span>
            </div>
            <div>
              <span className="text-blue-500 block text-[10px] uppercase font-bold">Dispersion (MAD)</span>
              <span className="text-slate-900 font-bold">₹{summary.baselineMad ? Number(summary.baselineMad).toLocaleString('en-IN') : '0'}</span>
            </div>
            <div>
              <span className="text-blue-500 block text-[10px] uppercase font-bold">Recurring Patterns</span>
              <span className="text-slate-900 font-bold">{summary.recurringExpensesCount || 0} recognized</span>
            </div>
          </div>
        )}
      </div>

      {/* Financial Health & Inflow/Outflow Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Money In */}
        <div className="p-5 bg-white border border-slate-200 rounded-2xl shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Money In</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <ArrowDownLeft className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-slate-900">
            ₹{summary?.totalInflows ? Number(summary.totalInflows).toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Confirmed inflows & salary</p>
        </div>

        {/* Money Out */}
        <div className="p-5 bg-white border border-slate-200 rounded-2xl shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Money Out</span>
            <div className="w-7 h-7 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
              <ArrowUpRight className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-slate-900">
            ₹{summary?.totalOutflows ? Number(summary.totalOutflows).toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Total recorded debits</p>
        </div>

        {/* Net Flow */}
        <div className="p-5 bg-white border border-slate-200 rounded-2xl shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Net Cash Flow</span>
            <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className={`text-2xl font-black ${
            (summary?.netCashFlow || 0) >= 0 ? 'text-emerald-700' : 'text-rose-700'
          }`}>
            ₹{summary?.netCashFlow ? Number(summary.netCashFlow).toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Inflows minus recorded expenses</p>
        </div>

        {/* Needs Review / Flagged */}
        <div className="p-5 bg-white border border-amber-200 bg-amber-50/20 rounded-2xl shadow-xs">
          <div className="flex items-center justify-between text-amber-700 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Requires Review</span>
            <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-amber-900 flex items-baseline gap-2">
            <span>{(summary?.needsReviewCount || 0) + (summary?.flaggedCount || 0)}</span>
            <span className="text-xs font-normal text-amber-700">({summary?.flaggedCount || 0} high-risk)</span>
          </div>
          <p className="text-[11px] text-amber-700 mt-1">Unusual deviations from normal spend</p>
        </div>
      </div>

      {/* Spending Breakdown by Category */}
      {summary?.categoryExpenses && Object.keys(summary.categoryExpenses).length > 0 && (
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-1.5">
            <Tag className="w-3.5 h-3.5" />
            <span>Spending Distribution by Category</span>
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
            {Object.entries(summary.categoryExpenses).map(([cat, amt]) => (
              <div key={cat} className="p-3 bg-slate-50 rounded-xl border border-slate-100">
                <div className="text-[11px] font-semibold text-slate-500 capitalize">{cat.toLowerCase().replace('_', ' ')}</div>
                <div className="text-sm font-bold text-slate-900 mt-0.5">₹{Number(amt).toLocaleString('en-IN')}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Filter & Search Bar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
        <div className="flex flex-col md:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Search merchant, reference, or description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-600"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 text-xs border border-slate-200 rounded-xl bg-white text-slate-700 font-medium"
            >
              <option value="">All Statuses</option>
              <option value="CONFIRMED">Normal / Confirmed</option>
              <option value="NEEDS_REVIEW">Needs Review</option>
              <option value="FLAGGED_SUSPICIOUS">Flagged Suspicious</option>
            </select>

            <select
              value={directionFilter}
              onChange={(e) => setDirectionFilter(e.target.value)}
              className="px-3 py-2 text-xs border border-slate-200 rounded-xl bg-white text-slate-700 font-medium"
            >
              <option value="">All Directions</option>
              <option value="EXPENSE">Expense (Money Out)</option>
              <option value="INCOME">Income (Money In)</option>
            </select>

            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="px-3 py-2 text-xs border border-slate-200 rounded-xl bg-white text-slate-700 font-medium"
            >
              <option value="">All Categories</option>
              <option value="FOOD">Food & Dining</option>
              <option value="SHOPPING">Shopping</option>
              <option value="BILLS">Bills & Utilities</option>
              <option value="TRAVEL">Travel</option>
              <option value="CASH_WITHDRAWAL">Cash</option>
              <option value="SALARY">Salary</option>
              <option value="OTHER">Other</option>
            </select>

            {(statusFilter || directionFilter || categoryFilter || searchQuery) && (
              <button
                onClick={() => {
                  setStatusFilter('');
                  setDirectionFilter('');
                  setCategoryFilter('');
                  setSearchQuery('');
                }}
                className="px-2.5 py-2 text-xs text-rose-600 hover:text-rose-800 font-bold transition"
              >
                Clear Filters
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-xs overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-bold text-slate-900">Transaction History</h2>
            <span className="text-xs text-slate-400 font-medium">({filteredTransactions.length} of {totalElements})</span>
          </div>
          <button onClick={loadData} className="p-1.5 text-slate-400 hover:text-blue-600 rounded-lg hover:bg-slate-50 transition">
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>

        {loading ? (
          <div className="py-16 text-center text-slate-400 text-xs">
            <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-blue-600" />
            <span>Loading transaction ledger...</span>
          </div>
        ) : filteredTransactions.length === 0 ? (
          <div className="py-16 text-center text-slate-400 text-xs max-w-sm mx-auto">
            <FileText className="w-8 h-8 text-slate-300 mx-auto mb-2" />
            <p className="font-bold text-slate-700 mb-1">No transactions found</p>
            <p className="text-slate-400 mb-4">You have not recorded or imported any transactions matching the selected criteria.</p>
            <button
              onClick={() => {
                setImportStep('select');
                setShowImportModal(true);
              }}
              className="px-4 py-2 bg-blue-700 text-white font-bold rounded-xl text-xs"
            >
              Import Bank Statement
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-bold uppercase tracking-wider text-[10px] border-b border-slate-100">
                <tr>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4">Merchant / Description</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Channel</th>
                  <th className="py-3 px-4">Amount</th>
                  <th className="py-3 px-4">Risk Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredTransactions.map((txn) => {
                  const isExpense = txn.direction === 'EXPENSE';
                  const isFlagged = txn.status === 'FLAGGED_SUSPICIOUS';
                  const isReview = txn.status === 'NEEDS_REVIEW';

                  return (
                    <tr 
                      key={txn.id} 
                      onClick={() => setInspectTxn(txn)}
                      className={`hover:bg-slate-50/80 transition cursor-pointer ${
                        isFlagged ? 'bg-rose-50/20' : isReview ? 'bg-amber-50/20' : ''
                      }`}
                    >
                      <td className="py-3.5 px-4 font-mono text-slate-600 whitespace-nowrap">
                        {txn.transactionDateTime ? new Date(txn.transactionDateTime).toLocaleDateString('en-IN', {
                          day: '2-digit', month: 'short', year: 'numeric'
                        }) : 'N/A'}
                      </td>
                      <td className="py-3.5 px-4 font-bold text-slate-900 max-w-xs truncate">
                        <div>{txn.merchant || 'Unknown Counterparty'}</div>
                        {txn.transactionReference && (
                          <div className="text-[10px] font-mono text-slate-400 font-normal truncate">
                            Ref: {txn.transactionReference}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="px-2 py-0.5 rounded-md text-[10px] font-semibold bg-slate-100 text-slate-700 capitalize">
                          {(txn.category || 'OTHER').toLowerCase().replace('_', ' ')}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-slate-500 font-medium">
                        {txn.transactionType || 'UPI'}
                      </td>
                      <td className="py-3.5 px-4 font-bold whitespace-nowrap">
                        <span className={isExpense ? 'text-slate-900' : 'text-emerald-700 font-black'}>
                          {isExpense ? '- ' : '+ '}₹{Number(txn.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        {isFlagged ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800">
                            <AlertCircle className="w-3 h-3 text-rose-600" />
                            <span>High Risk</span>
                          </span>
                        ) : isReview ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800">
                            <AlertTriangle className="w-3 h-3 text-amber-600" />
                            <span>Unusual</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            <span>Normal</span>
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-end gap-1.5">
                          {(isFlagged || isReview) && (
                            <button
                              onClick={() => handleVerify(txn.id)}
                              className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-bold rounded-lg text-[11px] transition"
                              title="Confirm this was an authorized payment"
                            >
                              Verify
                            </button>
                          )}
                          <button
                            onClick={() => setInspectTxn(txn)}
                            className="p-1 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100 transition"
                            title="Inspect details"
                          >
                            <ChevronRight className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ========================================================= */}
      {/* MODAL 1: UNIFIED STATEMENT IMPORT (Phase 2)                */}
      {/* ========================================================= */}
      {showImportModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-2xl w-full shadow-2xl border border-slate-200 overflow-hidden animate-in zoom-in-95 max-h-[90vh] flex flex-col">
            
            {/* Modal Header */}
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                  <FileSpreadsheet className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-slate-900">Import Bank Statement</h2>
                  <p className="text-[11px] text-slate-500">Supported formats: CSV (.csv), Excel (.xlsx, .xls), and PDF statements (.pdf)</p>
                </div>
              </div>
              <button 
                onClick={() => setShowImportModal(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Content */}
            <div className="p-6 overflow-y-auto space-y-4 text-xs">

              {importStep === 'select' && (
                <div className="space-y-4">
                  {/* Dropzone */}
                  <div
                    onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
                    onDragLeave={() => setIsDragOver(false)}
                    onDrop={(e) => {
                      e.preventDefault();
                      setIsDragOver(false);
                      if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                        const dropped = e.dataTransfer.files[0];
                        setImportFile(dropped);
                        handleFilePreview(dropped);
                      }
                    }}
                    className={`border-2 border-dashed rounded-2xl p-8 text-center transition cursor-pointer ${
                      isDragOver ? 'border-emerald-500 bg-emerald-50/50' : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                    }`}
                  >
                    <Upload className="w-10 h-10 text-emerald-600 mx-auto mb-3" />
                    <div className="text-sm font-bold text-slate-800 mb-1">
                      Drag & Drop your bank statement here
                    </div>
                    <p className="text-slate-500 text-xs mb-4">or choose a file from your computer</p>
                    <input
                      type="file"
                      accept=".csv, .xlsx, .xls, .pdf"
                      onChange={(e) => {
                        if (e.target.files && e.target.files[0]) {
                          const f = e.target.files[0];
                          setImportFile(f);
                          handleFilePreview(f);
                        }
                      }}
                      className="hidden"
                      id="statement-file-input"
                    />
                    <label
                      htmlFor="statement-file-input"
                      className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-700 hover:bg-emerald-800 text-white font-bold rounded-xl cursor-pointer transition shadow-xs"
                    >
                      Browse Files
                    </label>
                  </div>

                  {/* Format Notice */}
                  <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-[11px] text-slate-600 space-y-1">
                    <div className="font-bold text-slate-800 flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5 text-blue-600" />
                      <span>Security & Privacy Guarantee:</span>
                    </div>
                    <div>• Bank credentials, passwords, or customer signatures are never stored or logged.</div>
                    <div>• Duplicates are automatically suppressed using stable SHA-256 fingerprints.</div>
                    <div>• Records are strictly isolated to your authenticated account.</div>
                  </div>
                </div>
              )}

              {importStep === 'preview' && previewData && (
                <div className="space-y-4">
                  {/* Summary Bar */}
                  <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-200 grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
                    <div>
                      <span className="text-[10px] text-slate-500 uppercase font-bold block">File Type</span>
                      <span className="font-bold text-slate-900">{previewData.fileType}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500 uppercase font-bold block">Rows Parsed</span>
                      <span className="font-bold text-slate-900">{previewData.validRowsCount} valid</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500 uppercase font-bold block">Duplicates</span>
                      <span className="font-bold text-amber-700">{previewData.duplicateCount} exist</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500 uppercase font-bold block">Invalid Rows</span>
                      <span className="font-bold text-slate-700">{previewData.invalidCount}</span>
                    </div>
                  </div>

                  {/* Ambiguity Warning */}
                  {previewData.isAmbiguous && (
                    <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 text-xs">
                      <div className="font-bold flex items-center gap-1.5 mb-1">
                        <AlertTriangle className="w-4 h-4 text-amber-600" />
                        <span>Column Confirmation Required</span>
                      </div>
                      <p className="text-[11px] mb-2">{previewData.ambiguityReason}</p>
                      <label className="flex items-center gap-2 cursor-pointer font-bold text-slate-800">
                        <input
                          type="checkbox"
                          checked={confirmAmbiguousCheck}
                          onChange={(e) => setConfirmAmbiguousCheck(e.target.checked)}
                          className="rounded text-blue-600 focus:ring-blue-500"
                        />
                        <span>I have verified the preview columns and confirm this is my statement.</span>
                      </label>
                    </div>
                  )}

                  {/* Preview Rows Table */}
                  <div className="border border-slate-200 rounded-xl overflow-hidden max-h-64 overflow-y-auto">
                    <table className="w-full text-left text-[11px]">
                      <thead className="bg-slate-100 text-slate-600 font-bold uppercase tracking-wider text-[9px] sticky top-0">
                        <tr>
                          <th className="py-2 px-3">Date</th>
                          <th className="py-2 px-3">Description</th>
                          <th className="py-2 px-3">Amount</th>
                          <th className="py-2 px-3">Direction</th>
                          <th className="py-2 px-3">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {previewData.previewRows.map((r, i) => (
                          <tr key={i} className={r.isDuplicate ? 'bg-amber-50/40 text-slate-400' : 'hover:bg-slate-50'}>
                            <td className="py-2 px-3 font-mono">{r.dateStr || 'N/A'}</td>
                            <td className="py-2 px-3 max-w-xs truncate">{r.description}</td>
                            <td className="py-2 px-3 font-bold">₹{Number(r.amount).toLocaleString('en-IN')}</td>
                            <td className="py-2 px-3">
                              <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${
                                r.direction === 'EXPENSE' ? 'bg-slate-100 text-slate-800' : 'bg-emerald-100 text-emerald-800'
                              }`}>
                                {r.direction}
                              </span>
                            </td>
                            <td className="py-2 px-3">
                              {r.isDuplicate ? (
                                <span className="text-amber-600 font-bold">Duplicate</span>
                              ) : (
                                <span className="text-emerald-600 font-bold">New</span>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {previewData.invalidRows && previewData.invalidRows.length > 0 && (
                    <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-900 text-[11px]">
                      <span className="font-bold block mb-1">Skipped or Invalid Rows ({previewData.invalidRows.length}):</span>
                      <ul className="list-disc pl-4 space-y-0.5">
                        {previewData.invalidRows.slice(0, 3).map((err, idx) => (
                          <li key={idx}>Row {err.rowNumber}: {err.error}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}

              {importStep === 'result' && importResult && (
                <div className="space-y-4 text-center py-4">
                  <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center mx-auto">
                    <CheckCircle2 className="w-6 h-6" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900">Import Complete</h3>
                  <p className="text-slate-600 text-xs">{importResult.message}</p>
                  
                  <div className="grid grid-cols-3 gap-3 p-4 bg-slate-50 rounded-2xl border border-slate-200 text-left max-w-sm mx-auto">
                    <div>
                      <span className="text-[10px] text-slate-400 block font-bold">Imported</span>
                      <span className="text-base font-black text-slate-900">{importResult.importedCount}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-400 block font-bold">Skipped</span>
                      <span className="text-base font-black text-amber-700">{importResult.duplicateSkippedCount}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-400 block font-bold">Flagged</span>
                      <span className="text-base font-black text-rose-700">{importResult.flaggedCount}</span>
                    </div>
                  </div>
                </div>
              )}

            </div>

            {/* Modal Footer */}
            <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2">
              {importStep === 'preview' && (
                <button
                  type="button"
                  onClick={() => setImportStep('select')}
                  className="px-4 py-2 bg-slate-200 hover:bg-slate-300 font-bold rounded-xl text-slate-700 transition"
                >
                  Back
                </button>
              )}
              
              {importStep === 'select' ? (
                <button
                  type="button"
                  onClick={() => setShowImportModal(false)}
                  className="px-4 py-2 bg-slate-200 hover:bg-slate-300 font-bold rounded-xl text-slate-700 transition"
                >
                  Cancel
                </button>
              ) : importStep === 'preview' ? (
                <button
                  type="button"
                  disabled={importLoading || (previewData?.isAmbiguous && !confirmAmbiguousCheck)}
                  onClick={handleConfirmImport}
                  className="px-5 py-2 bg-emerald-700 hover:bg-emerald-800 disabled:opacity-50 text-white font-bold rounded-xl transition flex items-center gap-1.5 shadow-xs"
                >
                  {importLoading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                  <span>Commit to Ledger</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => setShowImportModal(false)}
                  className="px-5 py-2 bg-blue-700 hover:bg-blue-800 text-white font-bold rounded-xl transition"
                >
                  Done
                </button>
              )}
            </div>

          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL 2: SMS FRAUD & ANOMALY DETECTOR (Phase 5)            */}
      {/* ========================================================= */}
      {showSmsModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-xl w-full shadow-2xl border border-slate-200 overflow-hidden animate-in zoom-in-95 max-h-[90vh] flex flex-col">
            
            {/* Header */}
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center font-bold">
                  <MessageSquare className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-slate-900">SMS Fraud & Alert Analyzer</h2>
                  <p className="text-[11px] text-slate-500">Detect banking phishing, fake debit alerts, and transaction anomalies</p>
                </div>
              </div>
              <button 
                onClick={() => setShowSmsModal(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Content */}
            <div className="p-6 overflow-y-auto space-y-4 text-xs">
              <form onSubmit={handleAnalyzeSms} className="space-y-3">
                <div>
                  <label className="font-bold text-slate-700 block mb-1">
                    Paste SMS Alert or WhatsApp Message
                  </label>
                  <textarea
                    rows="3"
                    required
                    placeholder="e.g. Rs 12,000.00 debited from A/C XX8492 to SWIGGY via UPI Ref 418291038102 on 28-Oct-26."
                    value={smsInput}
                    onChange={(e) => setSmsInput(e.target.value)}
                    className="w-full p-3 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-600 font-mono text-xs"
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div className="text-[10px] text-slate-400 flex items-center gap-1">
                    <ShieldCheck className="w-3 h-3 text-emerald-600" />
                    <span>OTPs & passwords automatically redacted on-device before analysis.</span>
                  </div>
                  <button
                    type="submit"
                    disabled={smsAnalyzing || !smsInput.trim()}
                    className="px-4 py-2 bg-blue-700 hover:bg-blue-800 disabled:opacity-50 text-white font-bold rounded-xl transition flex items-center gap-1.5 shadow-xs"
                  >
                    {smsAnalyzing ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Search className="w-3.5 h-3.5" />}
                    <span>Analyze Content</span>
                  </button>
                </div>
              </form>

              {/* Analysis Result */}
              {smsResult && (
                <div className={`p-4 rounded-2xl border space-y-3 animate-in fade-in duration-200 ${
                  smsResult.isSuspicious 
                    ? 'bg-rose-50/70 border-rose-200 text-rose-900' 
                    : 'bg-emerald-50/70 border-emerald-200 text-emerald-900'
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      {smsResult.isSuspicious ? (
                        <ShieldAlert className="w-5 h-5 text-rose-600 shrink-0" />
                      ) : (
                        <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                      )}
                      <div>
                        <span className="font-black text-sm block">
                          {smsResult.isSuspicious ? 'High Risk Threat Detected' : 'Legitimate Notification'}
                        </span>
                        <span className="text-[10px] font-bold uppercase opacity-80">
                          Threat Category: {smsResult.threatCategory} • Risk Score: {smsResult.riskScore}/100
                        </span>
                      </div>
                    </div>
                  </div>

                  <p className="text-xs">{smsResult.explanation}</p>

                  <div className="p-3 bg-white/80 rounded-xl border border-black/5 text-[11px] font-medium space-y-1">
                    <span className="font-bold block text-slate-900">Recommended Action:</span>
                    <span>{smsResult.recommendedAction}</span>
                  </div>

                  {/* If Extracted Transaction is available */}
                  {smsResult.extractedTransaction && !smsResult.isSuspicious && (
                    <div className="pt-2 flex items-center justify-between border-t border-emerald-200">
                      <div className="text-[11px]">
                        <span className="font-bold text-slate-900">Amount:</span> ₹{smsResult.extractedTransaction.amount} •{' '}
                        <span className="font-bold text-slate-900">Merchant:</span> {smsResult.extractedTransaction.merchant}
                      </div>
                      <button
                        onClick={handleSaveSmsTransaction}
                        className="px-3 py-1.5 bg-emerald-700 hover:bg-emerald-800 text-white font-bold rounded-xl text-xs transition shadow-xs"
                      >
                        Record to Ledger
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Android Platform Disclosure */}
              <div className="p-3 bg-slate-50 border border-slate-100 rounded-xl text-[10px] text-slate-500 leading-relaxed">
                <span className="font-bold text-slate-700 block mb-0.5">Android Platform Transparency:</span>
                Prahar strictly enforces user privacy. Automated SMS reading is governed by Android's SMS Retriever API and requires explicit user authorization. No background message harvesting is performed.
              </div>
            </div>

          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL 3: TRANSACTION RISK DETAILS DRAWER                  */}
      {/* ========================================================= */}
      {inspectTxn && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-slate-200 animate-in zoom-in-95 space-y-4">
            
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-bold text-slate-900">Transaction Details & Risk Audit</h3>
              </div>
              <button 
                onClick={() => setInspectTxn(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Merchant / Recipient</span>
                <span className="font-bold text-slate-900">{inspectTxn.merchant || 'Unknown Counterparty'}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Amount</span>
                <span className="font-bold text-slate-900">₹{Number(inspectTxn.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Category / Type</span>
                <span className="font-semibold text-slate-700 capitalize">{(inspectTxn.category || 'OTHER').toLowerCase()} ({inspectTxn.transactionType || 'UPI'})</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Reference (UTR / ID)</span>
                <span className="font-mono text-slate-700">{inspectTxn.transactionReference || 'N/A'}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Risk Severity</span>
                <span className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                  inspectTxn.status === 'FLAGGED_SUSPICIOUS' ? 'bg-rose-100 text-rose-800' :
                  inspectTxn.status === 'NEEDS_REVIEW' ? 'bg-amber-100 text-amber-800' :
                  'bg-emerald-100 text-emerald-800'
                }`}>
                  {inspectTxn.severity || (inspectTxn.status === 'FLAGGED_SUSPICIOUS' ? 'HIGH' : inspectTxn.status === 'NEEDS_REVIEW' ? 'MEDIUM' : 'LOW')}
                </span>
              </div>

              {inspectTxn.flagReason && (
                <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-amber-900 text-[11px]">
                  <span className="font-bold block mb-0.5">Flag Reason:</span>
                  <span>{inspectTxn.flagReason}</span>
                </div>
              )}

              {inspectTxn.recommendation && (
                <div className="p-3 bg-blue-50 rounded-xl border border-blue-200 text-blue-900 text-[11px]">
                  <span className="font-bold block mb-0.5">Recommended Action:</span>
                  <span>{inspectTxn.recommendation}</span>
                </div>
              )}
            </div>

            <div className="flex gap-2 pt-2">
              {inspectTxn.status !== 'CONFIRMED' && (
                <button
                  type="button"
                  onClick={() => handleVerify(inspectTxn.id)}
                  className="flex-1 py-2 bg-emerald-700 hover:bg-emerald-800 font-bold rounded-xl text-white transition text-xs shadow-xs"
                >
                  Confirm as Legitimate
                </button>
              )}
              <button
                type="button"
                onClick={() => setInspectTxn(null)}
                className="flex-1 py-2 bg-slate-100 hover:bg-slate-200 font-bold rounded-xl text-slate-700 transition text-xs"
              >
                Close
              </button>
            </div>

          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL 4: MANUAL TRANSACTION ENTRY                         */}
      {/* ========================================================= */}
      {showManualModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <h2 className="text-base font-bold text-slate-900 mb-1">Add Manual Entry</h2>
            <p className="text-xs text-slate-500 mb-4">Record a transaction to monitor against fraud and baseline anomalies.</p>

            <form onSubmit={handleManualSubmit} className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-slate-700 block mb-1">Amount (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="e.g. 1500.00"
                  value={manualForm.amount}
                  onChange={(e) => setManualForm({ ...manualForm, amount: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-600"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="font-bold text-slate-700 block mb-1">Direction</label>
                  <select
                    value={manualForm.direction}
                    onChange={(e) => setManualForm({ ...manualForm, direction: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl font-medium"
                  >
                    <option value="EXPENSE">Expense (Money Out)</option>
                    <option value="INCOME">Income (Money In)</option>
                  </select>
                </div>

                <div>
                  <label className="font-bold text-slate-700 block mb-1">Channel / Type</label>
                  <select
                    value={manualForm.type}
                    onChange={(e) => setManualForm({ ...manualForm, type: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl font-medium"
                  >
                    <option value="UPI">UPI</option>
                    <option value="CARD">Debit / Credit Card</option>
                    <option value="ATM">ATM Withdrawal</option>
                    <option value="NETBANKING">NetBanking</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="font-bold text-slate-700 block mb-1">Category</label>
                <select
                  value={manualForm.category}
                  onChange={(e) => setManualForm({ ...manualForm, category: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl font-medium"
                >
                  <option value="SHOPPING">Shopping</option>
                  <option value="FOOD">Food & Dining</option>
                  <option value="BILLS">Bills & Utilities</option>
                  <option value="TRAVEL">Travel</option>
                  <option value="CASH_WITHDRAWAL">Cash Withdrawal</option>
                  <option value="SALARY">Salary & Income</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div>
                <label className="font-bold text-slate-700 block mb-1">Merchant / Recipient</label>
                <input
                  type="text"
                  placeholder="e.g. Swiggy, Amazon, Landlord Rent"
                  value={manualForm.merchant}
                  onChange={(e) => setManualForm({ ...manualForm, merchant: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700 block mb-1">Reference Number (optional)</label>
                <input
                  type="text"
                  placeholder="12-digit UTR or txn reference"
                  value={manualForm.transactionReference}
                  onChange={(e) => setManualForm({ ...manualForm, transactionReference: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl"
                />
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowManualModal(false)}
                  className="flex-1 py-2 bg-slate-100 hover:bg-slate-200 rounded-xl font-bold text-slate-700 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2 bg-blue-700 hover:bg-blue-800 rounded-xl font-bold text-white transition shadow-xs"
                >
                  Save Entry
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
