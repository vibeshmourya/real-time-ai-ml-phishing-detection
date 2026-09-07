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
    riskScore: 94,
    riskLevel: 'HIGH_RISK',
    threatCategory: 'BANKING_PHISHING',
    status: 'COMPLETED',
    detection: {
      confidenceScore: 0.96,
      explanationSummary: 'High-risk banking phishing attempt impersonating HDFC Bank on a suspicious .tk domain.',
      recommendedActions: 'Do not enter passwords or OTPs. Close page immediately.',
      indicators: [
        {
          indicatorType: 'DOMAIN',
          indicatorName: 'High-Risk Free TLD (.tk)',
          description: 'Domain uses a free high-abuse top-level domain frequently associated with phishing.',
          severity: 'HIGH',
          weight: 30
        },
        {
          indicatorType: 'BRAND_IMPERSONATION',
          indicatorName: 'Brand Spoofing: HDFC Bank',
          description: 'URL contains brand name without being hosted on official hdfcbank.com.',
          severity: 'CRITICAL',
          weight: 40
        }
      ]
    },
    preventionGuide: getPreventionDetails('BANKING_PHISHING'),
    createdAt: new Date(Date.now() - 1000 * 60 * 18).toISOString()
  },
  {
    scanId: 102,
    url: 'https://www.google.com/search?q=cyber+security',
    normalizedUrl: 'www.google.com/search',
    domain: 'google.com',
    scanType: 'URL',
    riskScore: 4,
    riskLevel: 'SAFE',
    threatCategory: 'LEGITIMATE_VERIFIED',
    status: 'COMPLETED',
    detection: {
      confidenceScore: 0.99,
      explanationSummary: 'Verified official Google search URL with valid HTTPS certificate.',
      recommendedActions: 'This link is verified safe.',
      indicators: [
        {
          indicatorType: 'DOMAIN_REPUTATION',
          indicatorName: 'Official Recognized Domain',
          description: 'Matched against global authoritative domain whitelist.',
          severity: 'INFO',
          weight: 0
        }
      ]
    },
    preventionGuide: getPreventionDetails('SAFE_URL'),
    createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString()
  },
  {
    scanId: 103,
    url: 'URGENT: Your SBI KYC has expired. Update now at http://sbi-kyc-reactivate.xyz to avoid card freeze.',
    normalizedUrl: 'sbi-kyc-reactivate.xyz',
    domain: 'SMS / Message Analysis',
    scanType: 'MESSAGE',
    riskScore: 91,
    riskLevel: 'HIGH_RISK',
    threatCategory: 'KYC_FRAUD',
    status: 'COMPLETED',
    detection: {
      confidenceScore: 0.93,
      explanationSummary: 'Coercive KYC deactivation scam attempting to harvest NetBanking credentials.',
      recommendedActions: 'Do not click the link or provide Aadhaar/PAN/OTP.',
      indicators: [
        {
          indicatorType: 'SOCIAL_ENGINEERING',
          indicatorName: 'Fake KYC / Identity Update Trap',
          description: 'Coercive message threatening immediate card freeze.',
          severity: 'CRITICAL',
          weight: 35
        },
        {
          indicatorType: 'EMBEDDED_LINK',
          indicatorName: 'High-Abuse Domain (.xyz)',
          description: 'Embedded link uses unverified disposable registrar.',
          severity: 'HIGH',
          weight: 25
        }
      ]
    },
    preventionGuide: getPreventionDetails('KYC_FRAUD'),
    createdAt: new Date(Date.now() - 1000 * 60 * 120).toISOString()
  }
];

export function ScanProvider({ children }) {
  const [history, setHistory] = useState(() => {
    const saved = localStorage.getItem('phishshield_scans');
    return saved ? JSON.parse(saved) : INITIAL_HISTORY;
  });

  const [activeVerdict, setActiveVerdict] = useState(null);
  const [isVerdictModalOpen, setIsVerdictModalOpen] = useState(false);
  const [isEmergencyOpen, setIsEmergencyOpen] = useState(false);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [isAwarenessOpen, setIsAwarenessOpen] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState('BANKING_PHISHING');
  const [isScanning, setIsScanning] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  useEffect(() => {
    localStorage.setItem('phishshield_scans', JSON.stringify(history));
  }, [history]);

  const showToast = (msg, type = 'info') => {
    setToastMessage({ text: msg, type });
    setTimeout(() => setToastMessage(null), 3500);
  };

  const executeScan = async (input, scanType = 'URL') => {
    setIsScanning(true);
    try {
      let res;
      if (scanType === 'MESSAGE') {
        res = await scannerApi.scanMessage(input);
      } else if (scanType === 'EMAIL') {
        res = await scannerApi.scanEmail(input);
      } else {
        res = await scannerApi.scanUrl(input);
      }

      const scanResult = res.data;
      setActiveVerdict(scanResult);
      setIsVerdictModalOpen(true);

      // Prepend to history (unique by scanId)
      setHistory(prev => [scanResult, ...prev.filter(item => item.scanId !== scanResult.scanId)]);
      showToast(
        scanResult.riskLevel === 'SAFE'
          ? 'Scan Completed: Verified Safe 🛡️'
          : scanResult.riskLevel === 'HIGH_RISK'
          ? '🚨 HIGH RISK Phishing Threat Detected!'
          : '⚠️ Suspicious Threat Indicators Detected',
        scanResult.riskLevel === 'SAFE' ? 'safe' : scanResult.riskLevel === 'HIGH_RISK' ? 'danger' : 'warn'
      );
      return scanResult;
    } catch (err) {
      showToast(err.message || 'Scan error occurred', 'danger');
      throw err;
    } finally {
      setIsScanning(false);
    }
  };

  const openEmergencyPlaybook = (category = 'BANKING_PHISHING') => {
    setSelectedCategory(category);
    setIsEmergencyOpen(true);
  };

  const openAwarenessGuide = (category = 'BANKING_PHISHING') => {
    setSelectedCategory(category);
    setIsAwarenessOpen(true);
  };

  const clearHistory = () => {
    setHistory([]);
    localStorage.removeItem('phishshield_scans');
    showToast('Scan history cleared', 'info');
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
        isScanning,
        executeScan,
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
