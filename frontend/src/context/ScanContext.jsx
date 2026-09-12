import React, { createContext, useContext, useState, useEffect } from 'react';
import { scannerApi } from '../api/scannerApi';
import { getPreventionDetails } from '../api/heuristicsEngine';

const ScanContext = createContext();

const INITIAL_HISTORY = [
  {
    scanId: 101,
    url: 'http://secure-login-hdfcbank.account-verify.tk/login.php',
    normalizedUrl: 'secure-login-hdfcbank.account-verify.tk/login.php',
    domain: 'secure-login-hdfcbank.account-verify.tk',
    scanType: 'URL',
    riskScore: 86,
    riskLevel: 'HIGH_RISK',
    threatCategory: 'BANKING_PHISHING',
    status: 'COMPLETED',
    detection: {
      confidenceScore: 0.95,
      explanationSummary: 'The web address mimics official HDFC Bank login pages on a high-risk free domain (.tk) without valid credentials encryption.',
      recommendedActions: 'Do not enter your password, PIN, or OTP. Close the web page immediately.',
      indicators: [
        {
          indicatorType: 'DOMAIN',
          indicatorName: 'High-Risk Free Domain (.tk)',
          description: 'The domain uses a free registrar commonly associated with disposable phishing campaigns.',
          severity: 'HIGH',
          weight: 25
        },
        {
          indicatorType: 'BRAND_IMPERSONATION',
          indicatorName: 'Brand Spoofing: HDFC Bank',
          description: 'The link references HDFC Bank but is hosted on an unauthorized website address.',
          severity: 'CRITICAL',
          weight: 35
        }
      ]
    },
    preventionGuide: getPreventionDetails('BANKING_PHISHING'),
    createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString()
  },
  {
    scanId: 102,
    url: 'https://www.google.com/search?q=cyber+security',
    normalizedUrl: 'www.google.com/search',
    domain: 'google.com',
    scanType: 'URL',
    riskScore: 4,
    riskLevel: 'SAFE',
    threatCategory: 'BENIGN',
    status: 'COMPLETED',
    detection: {
      confidenceScore: 0.99,
      explanationSummary: 'Verified official Google search URL with valid HTTPS certificate and standard domain structure.',
      recommendedActions: 'This link is verified safe.',
      indicators: []
    },
    preventionGuide: getPreventionDetails('BENIGN'),
    createdAt: new Date(Date.now() - 1000 * 60 * 55).toISOString()
  },
  {
    scanId: 103,
    url: 'URGENT: Your SBI KYC has expired. Update now at http://sbi-kyc-reactivate.xyz to avoid card freeze.',
    normalizedUrl: 'sbi-kyc-reactivate.xyz',
    domain: 'Message Analysis',
    scanType: 'MESSAGE',
    riskScore: 91,
    riskLevel: 'HIGH_RISK',
    threatCategory: 'FAKE_KYC',
    status: 'COMPLETED',
    detection: {
      confidenceScore: 0.94,
      explanationSummary: 'Coercive KYC deactivation scam message attempting to panic users into visiting an unverified portal.',
      recommendedActions: 'Do not click the link or provide Aadhaar, PAN, or OTP.',
      indicators: [
        {
          indicatorType: 'CONTENT',
          indicatorName: 'Fake KYC / Account Freeze Panic',
          description: 'Coercive message threatening immediate card freeze.',
          severity: 'CRITICAL',
          weight: 35
        },
        {
          indicatorType: 'DOMAIN',
          indicatorName: 'Suspicious Domain (.xyz)',
          description: 'Embedded link uses unverified disposable registrar.',
          severity: 'HIGH',
          weight: 25
        }
      ]
    },
    preventionGuide: getPreventionDetails('FAKE_KYC'),
    createdAt: new Date(Date.now() - 1000 * 60 * 140).toISOString()
  }
];

export function ScanProvider({ children }) {
  const [history, setHistory] = useState(() => {
    const saved = localStorage.getItem('prahar_scans');
    return saved ? JSON.parse(saved) : INITIAL_HISTORY;
  });

  const [activeVerdict, setActiveVerdict] = useState(null);
  const [isVerdictModalOpen, setIsVerdictModalOpen] = useState(false);
  const [isEmergencyOpen, setIsEmergencyOpen] = useState(false);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [isAwarenessOpen, setIsAwarenessOpen] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState('BANKING_PHISHING');
  const [selectedEmergencyScenario, setSelectedEmergencyScenario] = useState(null);
  const [isScanning, setIsScanning] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  useEffect(() => {
    localStorage.setItem('prahar_scans', JSON.stringify(history));
  }, [history]);

  const showToast = (msg, type = 'info') => {
    setToastMessage({ text: msg, type });
    setTimeout(() => setToastMessage(null), 3500);
  };

  const executeScan = async (input, scanType = 'URL', extraData = {}) => {
    setIsScanning(true);
    try {
      let res;
      if (scanType === 'MESSAGE') {
        res = await scannerApi.scanMessage(input);
      } else if (scanType === 'EMAIL') {
        res = await scannerApi.scanEmail(input);
      } else if (scanType === 'SCREENSHOT') {
        res = await scannerApi.scanScreenshot(extraData.file, input);
      } else if (scanType === 'QR') {
        res = await scannerApi.scanQr(input, extraData.file);
      } else {
        res = await scannerApi.scanUrl(input);
      }

      const scanResult = res.data;
      setActiveVerdict(scanResult);
      setIsVerdictModalOpen(true);

      // Prepend to history (unique by scanId)
      setHistory(prev => [scanResult, ...prev.filter(item => item.scanId !== scanResult.scanId)]);
      
      const toastType = scanResult.riskLevel === 'SAFE' 
        ? 'safe' 
        : scanResult.riskLevel === 'HIGH_RISK' 
        ? 'danger' 
        : 'warn';
        
      showToast(
        scanResult.riskLevel === 'SAFE'
          ? 'Check Completed: Verified Safe 🛡️'
          : scanResult.riskLevel === 'HIGH_RISK'
          ? '⚠️ High Risk Phishing Flagged by Prahar'
          : '⚠️ Suspicious Characteristics Detected',
        toastType
      );
      return scanResult;
    } catch (err) {
      showToast(err.message || 'Prahar could not complete the analysis. Please try again.', 'danger');
      throw err;
    } finally {
      setIsScanning(false);
    }
  };

  const deleteScan = (scanId) => {
    setHistory(prev => prev.filter(item => item.scanId !== scanId));
    scannerApi.deleteScan(scanId);
    showToast('Scan record deleted from history.', 'info');
  };

  const openEmergencyPlaybook = (category = 'BANKING_PHISHING', scenario = null) => {
    setSelectedCategory(category);
    setSelectedEmergencyScenario(scenario);
    setIsEmergencyOpen(true);
  };

  const openAwarenessGuide = (category = 'BANKING_PHISHING') => {
    setSelectedCategory(category);
    setIsAwarenessOpen(true);
  };

  const clearHistory = () => {
    setHistory([]);
    localStorage.removeItem('prahar_scans');
    showToast('Scan history cleared.', 'info');
  };

  return (
    <ScanContext.Provider
      value={{
        history,
        activeVerdict,
        setActiveVerdict,
        isVerdictModalOpen,
        setIsVerdictModalOpen,
        isEmergencyOpen,
        setIsEmergencyOpen,
        isReportModalOpen,
        setIsReportModalOpen,
        isAwarenessOpen,
        setIsAwarenessOpen,
        selectedCategory,
        setSelectedCategory,
        selectedEmergencyScenario,
        setSelectedEmergencyScenario,
        isScanning,
        executeScan,
        deleteScan,
        openEmergencyPlaybook,
        openAwarenessGuide,
        clearHistory,
        toastMessage,
        showToast
      }}
    >
      {children}
    </ScanContext.Provider>
  );
}

export function useScan() {
  return useContext(ScanContext);
}
