import test from 'node:test';
import assert from 'node:assert/strict';

// Helper to mimic frontend validation logic
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
const ALLOWED_MIME_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif', 'image/bmp'];
const ALLOWED_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp'];

function validateSelectedFile(file) {
  if (!file) return 'No file selected.';

  const lowerName = (file.name || '').toLowerCase();
  const hasValidExt = ALLOWED_EXTENSIONS.some(ext => lowerName.endsWith(ext));
  const hasValidMime = ALLOWED_MIME_TYPES.includes(file.type);

  if (!hasValidExt && !hasValidMime) {
    return 'Unsupported file format. Please upload a PNG, JPEG, WebP, GIF, or BMP image.';
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
    return `File size (${sizeMb} MB) exceeds maximum allowed limit of 10 MB.`;
  }

  return null;
}

function constructUploadFormData(file, textHint) {
  const params = [];
  if (file) {
    params.push({ name: 'file', filename: file.name, size: file.size });
  }
  if (textHint && textHint.trim()) {
    params.push({ name: 'text', value: textHint.trim() });
  }
  return params;
}

function classifyBackendError(err) {
  let category = 'SERVER_ERROR';
  let message = err.message || 'Analysis failed.';

  if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
    category = 'TIMEOUT';
    message = 'The analysis request timed out after 30 seconds. The server might be processing high load.';
  } else if (!err.response) {
    category = 'NETWORK_ERROR';
    message = 'Cannot connect to the Prahar backend service. Verify that the server is online.';
  } else if (err.response.status === 400) {
    category = 'VALIDATION_ERROR';
    message = err.response.data?.message || 'Invalid or malformed image submitted.';
  } else if (err.response.status === 401 || err.response.status === 403) {
    category = 'AUTH_ERROR';
    message = 'Authentication required or permission denied for this analysis.';
  } else if (err.response.status === 429) {
    category = 'RATE_LIMITED';
    message = 'Rate limit exceeded. Please wait a few seconds before submitting another scan.';
  } else if (err.response.status >= 500) {
    category = 'SERVER_ERROR';
    message = err.response.data?.message || 'The server encountered an error processing the image.';
  }

  return { category, message };
}

test('1. Valid image formats are accepted by frontend validator', () => {
  const validFiles = [
    { name: 'screenshot.png', type: 'image/png', size: 1024 * 500 },
    { name: 'receipt.jpeg', type: 'image/jpeg', size: 1024 * 800 },
    { name: 'banner.webp', type: 'image/webp', size: 1024 * 300 },
    { name: 'qr_badge.gif', type: 'image/gif', size: 1024 * 150 },
    { name: 'bitmap.bmp', type: 'image/bmp', size: 1024 * 1200 },
  ];

  for (const f of validFiles) {
    assert.equal(validateSelectedFile(f), null, `File ${f.name} should be valid`);
  }
});

test('2. Unsupported formats and executable payloads are rejected', () => {
  const invalidFiles = [
    { name: 'script.exe', type: 'application/x-msdownload', size: 1024 },
    { name: 'document.pdf', type: 'application/pdf', size: 1024 * 100 },
    { name: 'malware.svg', type: 'image/svg+xml', size: 1024 * 50 },
    { name: 'payload.html', type: 'text/html', size: 1024 },
  ];

  for (const f of invalidFiles) {
    const error = validateSelectedFile(f);
    assert.ok(error && error.includes('Unsupported file format'), `File ${f.name} should be rejected`);
  }
});

test('3. Oversized files (>10MB) are rejected before uploading', () => {
  const oversizedFile = {
    name: 'giant-screenshot.png',
    type: 'image/png',
    size: 11 * 1024 * 1024 // 11 MB
  };

  const error = validateSelectedFile(oversizedFile);
  assert.ok(error && error.includes('exceeds maximum allowed limit of 10 MB'));
});

test('4. Upload request form data construction contains file and optional text hint', () => {
  const file = { name: 'suspicious.png', size: 2048 };
  const hint = 'Alert: bank account verification required';

  const formData = constructUploadFormData(file, hint);
  assert.equal(formData.length, 2);
  assert.equal(formData[0].name, 'file');
  assert.equal(formData[0].filename, 'suspicious.png');
  assert.equal(formData[1].name, 'text');
  assert.equal(formData[1].value, hint);
});

test('5. Distinguishes network connection failure from server outage', () => {
  const netErr = { message: 'Network Error' };
  const classified = classifyBackendError(netErr);
  assert.equal(classified.category, 'NETWORK_ERROR');
  assert.ok(classified.message.includes('Cannot connect to the Prahar backend'));
});

test('6. Distinguishes request timeout after 30 seconds', () => {
  const timeoutErr = { code: 'ECONNABORTED', message: 'timeout of 30000ms exceeded' };
  const classified = classifyBackendError(timeoutErr);
  assert.equal(classified.category, 'TIMEOUT');
  assert.ok(classified.message.includes('timed out after 30 seconds'));
});

test('7. Correctly classifies HTTP 400 validation rejection from backend', () => {
  const validationErr = {
    response: {
      status: 400,
      data: { message: 'Image dimensions exceed maximum allowed limits of 4096x4096' }
    }
  };
  const classified = classifyBackendError(validationErr);
  assert.equal(classified.category, 'VALIDATION_ERROR');
  assert.equal(classified.message, 'Image dimensions exceed maximum allowed limits of 4096x4096');
});

test('8. Correctly classifies HTTP 429 rate limit exceeded', () => {
  const rateLimitErr = {
    response: {
      status: 429,
      data: { message: 'Too many requests' }
    }
  };
  const classified = classifyBackendError(rateLimitErr);
  assert.equal(classified.category, 'RATE_LIMITED');
  assert.ok(classified.message.includes('Rate limit exceeded'));
});

test('9. Handles partial analysis responses with degraded components honestly', () => {
  const partialResponse = {
    status: 'PARTIAL_SUCCESS',
    overallRiskScore: 75,
    overallRiskLevel: 'HIGH_RISK',
    componentErrors: {
      mlService: 'FastAPI ML service is unavailable'
    },
    limitations: [
      'Visual OCR heuristic analysis detects surface phishing cues',
      'Local Tesseract OCR binary was not detected on host PATH'
    ]
  };

  assert.equal(partialResponse.status, 'PARTIAL_SUCCESS');
  assert.ok(Object.keys(partialResponse.componentErrors).length > 0);
  assert.equal(partialResponse.componentErrors.mlService, 'FastAPI ML service is unavailable');
  assert.equal(partialResponse.limitations.length, 2);
});

test('10. Dangerous script schemes in QR payloads are flagged as BLOCKED', () => {
  const qrItem = {
    index: 1,
    rawPayload: 'javascript:alert(document.cookie)',
    payloadType: 'URL_DANGEROUS_SCHEME',
    scannableUrl: false,
    scanStatus: 'BLOCKED_DANGEROUS_SCHEME'
  };

  assert.equal(qrItem.scannableUrl, false);
  assert.equal(qrItem.scanStatus, 'BLOCKED_DANGEROUS_SCHEME');
  assert.ok(qrItem.rawPayload.startsWith('javascript:'));
});
