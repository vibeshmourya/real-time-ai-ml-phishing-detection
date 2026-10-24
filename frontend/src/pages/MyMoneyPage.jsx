import React, { useState, useEffect } from 'react';
import { 
  DollarSign, TrendingUp, TrendingDown, AlertTriangle, CheckCircle2, 
  Upload, Plus, Search, Filter, Download, MessageSquare, ArrowUpRight, 
  ArrowDownLeft, RefreshCw, FileText, HelpCircle, ShieldCheck, Tag
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

  // Modals
  const [showManualModal, setShowManualModal] = useState(false);
  const [showCsvModal, setShowCsvModal] = useState(false);
  const [showSmsModal, setShowSmsModal] = useState(false);
  const [selectedTxnForReport, setSelectedTxnForReport] = useState(null);

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

  // SMS Text State
  const [smsText, setSmsText] = useState('');
  const [smsParsing, setSmsParsing] = useState(false);

  // CSV File State
  const [csvFile, setCsvFile] = useState(null);
  const [csvUploading, setCsvUploading] = useState(false);
  const [csvResult, setCsvResult] = useState(null);

  const [statusMessage, setStatusMessage] = useState(null);

  useEffect(() => {
    if (isAuthenticated) {
      loadData();
    } else {
      setLoading(false);
    }
  }, [isAuthenticated, currentPage, statusFilter, categoryFilter, directionFilter]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [sumData, txnData] = await Promise.all([
        transactionApi.getSummary(),
        transactionApi.getTransactions({
          page: currentPage,
          size: 10,
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
    } finally {
      setLoading(false);
    }
  };

  const handleManualSubmit = async (e) => {
    e.preventDefault();
    if (!manualForm.amount || isNaN(manualForm.amount)) {
      alert('Please enter a valid amount');
      return;
    }
    try {
      await transactionApi.createManual({
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
      setStatusMessage({ type: 'success', text: 'Transaction recorded successfully.' });
      loadData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to save transaction');
    }
  };

  const handleSmsSubmit = async (e) => {
    e.preventDefault();
    if (!smsText.trim()) return;
    setSmsParsing(true);
    try {
      await transactionApi.parseNotification(smsText.trim());
      setShowSmsModal(false);
      setSmsText('');
      setStatusMessage({ type: 'success', text: 'Notification parsed and added to your ledger.' });
      loadData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to parse notification');
    } finally {
      setSmsParsing(false);
    }
  };

  const handleCsvUpload = async (e) => {
    e.preventDefault();
    if (!csvFile) return;
    setCsvUploading(true);
    setCsvResult(null);
    try {
      const result = await transactionApi.uploadCsv(csvFile);
      setCsvResult(result);
      setStatusMessage({ 
        type: 'success', 
        text: `Imported ${result.importedCount} transactions. ${result.duplicateSkippedCount} duplicates skipped.` 
      });
      loadData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to parse CSV');
    } finally {
      setCsvUploading(false);
    }
  };

  const handleVerify = async (id) => {
    try {
      await transactionApi.verifyTransaction(id);
      loadData();
    } catch (err) {
      console.error('Verification failed', err);
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
        <div className="w-16 h-16 bg-blue-100 text-blue-700 rounded-3xl flex items-center justify-center mx-auto mb-4">
          <ShieldCheck className="w-8 h-8" />
        </div>
        <h1 className="text-2xl font-black text-slate-900 mb-2">My Money & Transactions</h1>
        <p className="text-sm text-slate-600 max-w-md mx-auto mb-6">
          Review your financial activity, spot unexpected debits early, and keep authorized transaction records private and organized.
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
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-in fade-in duration-200">
      
      {/* Header & Source Notice */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
            My Money & Transactions
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Track authorized activity, review flagged charges, and keep evidence ready.
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
            onClick={() => setShowSmsModal(true)}
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition"
          >
            <MessageSquare className="w-3.5 h-3.5 text-blue-600" />
            <span>Paste SMS Alert</span>
          </button>

          <button
            onClick={() => setShowCsvModal(true)}
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition"
          >
            <Upload className="w-3.5 h-3.5 text-emerald-600" />
            <span>Import CSV</span>
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

      {/* Coverage Disclaimer banner */}
      <div className="p-3 bg-amber-50/80 border border-amber-200/90 rounded-2xl flex items-start gap-2.5 text-xs text-amber-900">
        <HelpCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold">Data Source:</span> User-entered records and imported files only. Prahar does not connect to bank servers or access SMS without user consent.
        </div>
      </div>

      {statusMessage && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between text-xs font-bold text-emerald-800">
          <span>{statusMessage.text}</span>
          <button onClick={() => setStatusMessage(null)} className="text-emerald-600 hover:underline">Dismiss</button>
        </div>
      )}

      {/* Summary Cards */}
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
          <p className="text-[11px] text-slate-400 mt-1">Total recorded expenses</p>
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
            summary?.netCashFlow >= 0 ? 'text-emerald-700' : 'text-rose-700'
          }`}>
            ₹{summary?.netCashFlow ? Number(summary.netCashFlow).toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Inflows minus outflows</p>
        </div>

        {/* Needs Review */}
        <div className="p-5 bg-white border border-amber-200 bg-amber-50/20 rounded-2xl shadow-xs">
          <div className="flex items-center justify-between text-amber-700 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Needs Review</span>
            <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-amber-900">
            {summary?.needsReviewCount || 0}
          </div>
          <p className="text-[11px] text-amber-700 mt-1">Unconfirmed or unusual charges</p>
        </div>
      </div>

      {/* Category Expenses Breakdown */}
      {summary?.categoryExpenses && Object.keys(summary.categoryExpenses).length > 0 && (
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-1.5">
            <Tag className="w-3.5 h-3.5" />
            <span>Spending by Category</span>
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

      {/* Filters and Search Bar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
        <div className="flex flex-col md:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Search merchant, reference, or note..."
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
              <option value="CONFIRMED_USER">Confirmed</option>
              <option value="NEEDS_REVIEW">Needs Review</option>
              <option value="FLAGGED_SUSPICIOUS">Flagged</option>
              <option value="IMPORTED">Imported</option>
            </select>

            <select
              value={directionFilter}
              onChange={(e) => setDirectionFilter(e.target.value)}
              className="px-3 py-2 text-xs border border-slate-200 rounded-xl bg-white text-slate-700 font-medium"
            >
              <option value="">All Directions</option>
              <option value="EXPENSE">Expense (Money Out)</option>
              <option value="INCOME">Income (Money In)</option>
              <option value="TRANSFER">Transfer</option>
            </select>

            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="px-3 py-2 text-xs border border-slate-200 rounded-xl bg-white text-slate-700 font-medium"
            >
              <option value="">All Categories</option>
              <option value="SALARY_INCOME">Salary & Income</option>
              <option value="SHOPPING">Shopping</option>
              <option value="FOOD">Food & Dining</option>
              <option value="BILLS_UTILITIES">Bills & Utilities</option>
              <option value="TRAVEL">Travel</option>
              <option value="CASH_WITHDRAWAL">Cash Withdrawal</option>
              <option value="TRANSFERS">Transfers</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
        </div>

        {/* Transactions Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/70 text-slate-500 font-bold uppercase tracking-wider text-[10px]">
                <th className="py-2.5 px-3">Date</th>
                <th className="py-2.5 px-3">Merchant / Counterparty</th>
                <th className="py-2.5 px-3">Channel / Source</th>
                <th className="py-2.5 px-3">Category</th>
                <th className="py-2.5 px-3">Status</th>
                <th className="py-2.5 px-3 text-right">Amount</th>
                <th className="py-2.5 px-3 text-center">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan="7" className="py-8 text-center text-slate-400">Loading records...</td>
                </tr>
              ) : filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan="7" className="py-8 text-center text-slate-400">
                    No transactions match your filter criteria.
                  </td>
                </tr>
              ) : (
                filteredTransactions.map((txn) => (
                  <tr key={txn.id} className="hover:bg-slate-50/60 transition">
                    <td className="py-2.5 px-3 text-slate-500 whitespace-nowrap">
                      {txn.transactionDateTime ? txn.transactionDateTime.substring(0, 10) : '—'}
                    </td>
                    <td className="py-2.5 px-3 font-semibold text-slate-900">
                      <div>{txn.merchant || 'Unknown Counterparty'}</div>
                      {txn.flagReason && (
                        <div className="text-[10px] text-amber-700 font-medium flex items-center gap-1 mt-0.5">
                          <AlertTriangle className="w-3 h-3 text-amber-600 shrink-0" />
                          <span>{txn.flagReason}</span>
                        </div>
                      )}
                    </td>
                    <td className="py-2.5 px-3 text-slate-600 whitespace-nowrap">
                      <span className="font-bold">{txn.type}</span>
                      <span className="text-[10px] text-slate-400 block">{txn.importSource.toLowerCase()}</span>
                    </td>
                    <td className="py-2.5 px-3 whitespace-nowrap">
                      <span className="px-2 py-0.5 bg-slate-100 text-slate-700 rounded-md font-medium text-[11px]">
                        {txn.category.toLowerCase().replace('_', ' ')}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 whitespace-nowrap">
                      {txn.status === 'CONFIRMED_USER' ? (
                        <span className="inline-flex items-center gap-1 text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md font-bold text-[10px]">
                          <CheckCircle2 className="w-3 h-3 text-emerald-600" /> Confirmed
                        </span>
                      ) : txn.status === 'NEEDS_REVIEW' || txn.status === 'FLAGGED_SUSPICIOUS' ? (
                        <span className="inline-flex items-center gap-1 text-amber-800 bg-amber-100 px-2 py-0.5 rounded-md font-bold text-[10px]">
                          <AlertTriangle className="w-3 h-3 text-amber-700" /> Needs Review
                        </span>
                      ) : (
                        <span className="text-slate-500 text-[10px] bg-slate-100 px-2 py-0.5 rounded-md">
                          Imported
                        </span>
                      )}
                    </td>
                    <td className={`py-2.5 px-3 text-right font-black whitespace-nowrap ${
                      txn.direction === 'INCOME' ? 'text-emerald-600' : 'text-slate-900'
                    }`}>
                      {txn.direction === 'INCOME' ? '+' : '-'}₹{Number(txn.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-2.5 px-3 text-center whitespace-nowrap">
                      {txn.status !== 'CONFIRMED_USER' ? (
                        <button
                          onClick={() => handleVerify(txn.id)}
                          className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-bold rounded-lg text-[10px] transition"
                        >
                          Confirm
                        </button>
                      ) : (
                        <span className="text-slate-300 text-xs">✓</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalElements > 10 && (
          <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs text-slate-500">
            <span>Showing page {currentPage + 1} ({totalElements} total entries)</span>
            <div className="flex gap-2">
              <button
                disabled={currentPage === 0}
                onClick={() => setCurrentPage(p => p - 1)}
                className="px-3 py-1 bg-slate-100 rounded-lg disabled:opacity-50 font-bold"
              >
                Previous
              </button>
              <button
                disabled={(currentPage + 1) * 10 >= totalElements}
                onClick={() => setCurrentPage(p => p + 1)}
                className="px-3 py-1 bg-slate-100 rounded-lg disabled:opacity-50 font-bold"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Manual Entry Modal */}
      {showManualModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <h2 className="text-base font-bold text-slate-900 mb-1">Add Manual Transaction</h2>
            <p className="text-xs text-slate-500 mb-4">Record a payment or withdrawal to monitor against fraud.</p>

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
                    <option value="TRANSFER">Transfer</option>
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
                  <option value="BILLS_UTILITIES">Bills & Utilities</option>
                  <option value="TRAVEL">Travel</option>
                  <option value="CASH_WITHDRAWAL">Cash Withdrawal</option>
                  <option value="SALARY_INCOME">Salary & Income</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div>
                <label className="font-bold text-slate-700 block mb-1">Merchant / Recipient</label>
                <input
                  type="text"
                  placeholder="e.g. Swiggy, Amazon, Friend Name"
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
                  className="flex-1 py-2 bg-blue-700 hover:bg-blue-800 rounded-xl font-bold text-white transition"
                >
                  Save Entry
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* SMS Parser Modal */}
      {showSmsModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <h2 className="text-base font-bold text-slate-900 mb-1">Paste SMS Alert</h2>
            <p className="text-xs text-slate-500 mb-4">
              Paste the text of a bank SMS or notification. Prahar extracts the amount, channel, and counterparty locally.
            </p>

            <form onSubmit={handleSmsSubmit} className="space-y-3 text-xs">
              <textarea
                rows="4"
                required
                placeholder="e.g. Rs. 2,499.00 debited from A/C XX4819 at SWIGGY via UPI Ref 418291039817 on 22-Oct-26."
                value={smsText}
                onChange={(e) => setSmsText(e.target.value)}
                className="w-full p-3 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-600 font-mono text-xs"
              />

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowSmsModal(false)}
                  className="flex-1 py-2 bg-slate-100 hover:bg-slate-200 rounded-xl font-bold text-slate-700 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={smsParsing}
                  className="flex-1 py-2 bg-blue-700 hover:bg-blue-800 disabled:opacity-50 rounded-xl font-bold text-white transition flex items-center justify-center gap-1.5"
                >
                  {smsParsing ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : null}
                  <span>Parse & Add</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CSV Import Modal */}
      {showCsvModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-slate-200 animate-in zoom-in-95">
            <h2 className="text-base font-bold text-slate-900 mb-1">Import Bank Statement (CSV)</h2>
            <p className="text-xs text-slate-500 mb-4">
              Upload a standard bank statement in CSV format. Duplicates are automatically skipped using SHA-256 fingerprints.
            </p>

            <form onSubmit={handleCsvUpload} className="space-y-3 text-xs">
              <div className="border-2 border-dashed border-slate-200 rounded-2xl p-6 text-center hover:bg-slate-50 transition cursor-pointer">
                <Upload className="w-8 h-8 text-blue-600 mx-auto mb-2" />
                <input
                  type="file"
                  accept=".csv"
                  required
                  onChange={(e) => setCsvFile(e.target.files[0])}
                  className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                />
                {csvFile && <div className="text-[11px] font-bold text-slate-800 mt-2">{csvFile.name}</div>}
              </div>

              {csvResult && (
                <div className="p-3 bg-slate-50 rounded-xl text-[11px] text-slate-700 space-y-1">
                  <div><strong>Imported:</strong> {csvResult.importedCount}</div>
                  <div><strong>Duplicates Skipped:</strong> {csvResult.duplicateSkippedCount}</div>
                </div>
              )}

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCsvModal(false)}
                  className="flex-1 py-2 bg-slate-100 hover:bg-slate-200 rounded-xl font-bold text-slate-700 transition"
                >
                  Close
                </button>
                <button
                  type="submit"
                  disabled={csvUploading || !csvFile}
                  className="flex-1 py-2 bg-blue-700 hover:bg-blue-800 disabled:opacity-50 rounded-xl font-bold text-white transition flex items-center justify-center gap-1.5"
                >
                  {csvUploading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : null}
                  <span>Upload & Import</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
