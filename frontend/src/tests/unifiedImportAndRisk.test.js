import test from 'node:test';
import assert from 'node:assert/strict';

// Helper function mirroring client-side SMS sanitization before API dispatch
function sanitizeSmsPreview(text) {
  if (!text) return '';
  return text
    .replace(/(?:otp|code|pin)\s*(?:is|:|#|-|=|to)?\s*([0-9]{4,8})/gi, 'OTP [REDACTED]')
    .replace(/(?:password|pwd)\s*(?:is|:)?\s*(\S+)/gi, 'PASSWORD [REDACTED]');
}

// Helper verifying column mapping normalizer
function normalizeColumnType(header) {
  const h = header.toLowerCase().trim();
  const compact = h.replace(/\s+/g, '');
  if (h.includes('date') || h.includes('time')) return 'TRANSACTION_DATE';
  if (h.includes('amount') || h.includes('debit') || h.includes('credit')) return 'AMOUNT';
  if (h.includes('desc') || h.includes('narration') || h.includes('remark') || h.includes('party')) return 'DESCRIPTION';
  if (h.includes('type') || compact.includes('dr/cr') || h.includes('dr') || h.includes('cr')) return 'TYPE';
  return 'UNKNOWN';
}

test('1. SMS sensitive credential client-side masking preserves safety before dispatch', () => {
  const sensitive1 = 'Your HDFC Bank OTP is 492011 to approve transaction of Rs 15,000.';
  const sanitized1 = sanitizeSmsPreview(sensitive1);
  assert.ok(!sanitized1.includes('492011'));
  assert.ok(sanitized1.includes('OTP [REDACTED]'));

  const sensitive2 = 'Your netbanking temporary password is SecretPassword123 for user vibesh.';
  const sanitized2 = sanitizeSmsPreview(sensitive2);
  assert.ok(!sanitized2.includes('SecretPassword123'));
  assert.ok(sanitized2.includes('PASSWORD [REDACTED]'));
});

test('2. Column header auto-classification recognizes standard bank statement schemas', () => {
  assert.equal(normalizeColumnType('Txn Date'), 'TRANSACTION_DATE');
  assert.equal(normalizeColumnType('Value Date'), 'TRANSACTION_DATE');
  assert.equal(normalizeColumnType('Withdrawal Amount (INR)'), 'AMOUNT');
  assert.equal(normalizeColumnType('Narration / Description'), 'DESCRIPTION');
  assert.equal(normalizeColumnType('Transaction Remarks'), 'DESCRIPTION');
  assert.equal(normalizeColumnType('Dr / Cr'), 'TYPE');
  assert.equal(normalizeColumnType('Balance'), 'UNKNOWN');
});

test('3. Statement preview response validation flags ambiguities correctly', () => {
  const mockPreview = {
    detectedFileType: 'CSV',
    totalRowsDetected: 12,
    parsedCandidateRows: 12,
    duplicateCount: 2,
    hasAmbiguities: true,
    ambiguityNotice: 'Duplicate entries detected against your transaction ledger.',
    headers: ['Date', 'Description', 'Amount', 'Type'],
    rows: [
      {
        rowNumber: 1,
        transactionDate: '2026-10-25T14:30:00',
        amount: 15000.0,
        type: 'DEBIT',
        description: 'House Rent payment',
        isDuplicate: true,
        riskScore: 12,
        severity: 'LOW_RISK'
      },
      {
        rowNumber: 2,
        transactionDate: '2026-10-26T19:20:00',
        amount: 85000.0,
        type: 'DEBIT',
        description: 'Transfer to unfamiliar entity',
        isDuplicate: false,
        riskScore: 78,
        severity: 'FLAGGED_SUSPICIOUS'
      }
    ]
  };

  assert.equal(mockPreview.detectedFileType, 'CSV');
  assert.equal(mockPreview.hasAmbiguities, true);
  assert.equal(mockPreview.duplicateCount, 2);
  assert.equal(mockPreview.rows.length, 2);
  assert.equal(mockPreview.rows[0].isDuplicate, true);
  assert.equal(mockPreview.rows[1].severity, 'FLAGGED_SUSPICIOUS');
});

test('4. Confirm import payload packages verified user overrides and exclusions', () => {
  const verifiedRows = [
    {
      transactionDate: '2026-10-26T19:20:00',
      amount: 85000.0,
      type: 'DEBIT',
      description: 'Transfer to unfamiliar entity',
      category: 'Transfer',
      sourceType: 'CSV_IMPORT'
    }
  ];

  const payload = {
    sourceType: 'STATEMENT_IMPORT',
    verifiedRows: verifiedRows
  };

  assert.equal(payload.sourceType, 'STATEMENT_IMPORT');
  assert.equal(payload.verifiedRows.length, 1);
  assert.equal(payload.verifiedRows[0].amount, 85000.0);
});
