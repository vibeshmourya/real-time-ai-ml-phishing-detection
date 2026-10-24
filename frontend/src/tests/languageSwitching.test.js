import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const localesDir = path.resolve(__dirname, '../locales');

const en = JSON.parse(fs.readFileSync(path.join(localesDir, 'en.json'), 'utf8'));
const hi = JSON.parse(fs.readFileSync(path.join(localesDir, 'hi.json'), 'utf8'));
const kn = JSON.parse(fs.readFileSync(path.join(localesDir, 'kn.json'), 'utf8'));

const translations = { en, hi, kn };
const SUPPORTED_CODES = ['en', 'hi', 'kn'];

// Translation engine replicating LanguageContext logic
function createTranslator(currentLang, customTranslations = translations) {
  const sanitize = (code) => (code && SUPPORTED_CODES.includes(code) ? code : 'en');
  let lang = sanitize(currentLang);

  const setLang = (newCode) => {
    lang = sanitize(newCode);
    return lang;
  };

  const t = (keyPath, paramsOrFallback = '', maybeFallback = '') => {
    if (!keyPath) return '';
    let params = null;
    let fallback = '';

    if (typeof paramsOrFallback === 'object' && paramsOrFallback !== null) {
      params = paramsOrFallback;
      fallback = maybeFallback || '';
    } else if (typeof paramsOrFallback === 'string') {
      fallback = paramsOrFallback;
    }

    const keys = keyPath.split('.');
    const traverse = (obj) => {
      let curr = obj;
      for (const k of keys) {
        if (curr && typeof curr === 'object' && k in curr) {
          curr = curr[k];
        } else {
          return null;
        }
      }
      return typeof curr === 'string' ? curr : null;
    };

    let result = traverse(customTranslations[lang]);
    if (result === null && lang !== 'en') {
      result = traverse(customTranslations.en);
    }
    if (result === null) {
      result = fallback || keyPath;
    }

    if (params && typeof result === 'string') {
      for (const [pKey, pVal] of Object.entries(params)) {
        result = result.replaceAll(`{${pKey}}`, String(pVal));
      }
    }
    return result;
  };

  return { getLang: () => lang, setLang, t };
}

function getAllKeys(obj, prefix = '') {
  return Object.keys(obj).flatMap((key) => {
    const val = obj[key];
    const full = prefix ? `${prefix}.${key}` : key;
    return typeof val === 'object' && val !== null ? getAllKeys(val, full) : [full];
  });
}

test('1. Default language resolves to English when storage is empty or invalid', () => {
  const tNull = createTranslator(null);
  assert.equal(tNull.getLang(), 'en');

  const tInvalid = createTranslator('invalid_lang_code');
  assert.equal(tInvalid.getLang(), 'en');

  const tXss = createTranslator('<script>alert(1)</script>');
  assert.equal(tXss.getLang(), 'en');
});

test('2. Supported languages validation: en, hi, and kn are accepted', () => {
  const tr = createTranslator('en');
  assert.equal(tr.setLang('hi'), 'hi');
  assert.equal(tr.getLang(), 'hi');

  assert.equal(tr.setLang('kn'), 'kn');
  assert.equal(tr.getLang(), 'kn');

  assert.equal(tr.setLang('fr'), 'en'); // Unsupported falls back to en
  assert.equal(tr.getLang(), 'en');
});

test('3. Switching to Hindi updates navbar, hero, scanner, and dashboard content', () => {
  const tr = createTranslator('en');
  assert.equal(tr.t('nav.home'), 'Home');
  assert.equal(tr.t('hero.title'), 'Check before you trust.');
  assert.equal(tr.t('scanner.tabLink'), 'Link');
  assert.equal(tr.t('dashboard.recentChecks'), 'Recent Checks');

  tr.setLang('hi');
  assert.equal(tr.t('nav.home'), 'होम');
  assert.equal(tr.t('hero.title'), 'भरोसा करने से पहले जांचें।');
  assert.equal(tr.t('scanner.tabLink'), 'लिंक');
  assert.equal(tr.t('dashboard.recentChecks'), 'हाल की सुरक्षा जांचें');
});

test('4. Switching to Kannada updates navbar, hero, scanner, and dashboard content', () => {
  const tr = createTranslator('en');
  tr.setLang('kn');
  assert.equal(tr.t('nav.home'), 'ಮುಖಪುಟ');
  assert.equal(tr.t('hero.title'), 'ನಂಬುವ ಮುನ್ನ ಪರೀಕ್ಷಿಸಿ.');
  assert.equal(tr.t('scanner.tabLink'), 'ಲಿಂಕ್');
  assert.equal(tr.t('dashboard.recentChecks'), 'ಇತ್ತೀಚಿನ ತಪಾಸಣೆಗಳು');
});

test('5. Switching back to English restores original strings accurately', () => {
  const tr = createTranslator('hi');
  assert.equal(tr.t('nav.home'), 'होम');

  tr.setLang('en');
  assert.equal(tr.t('nav.home'), 'Home');
  assert.equal(tr.t('scanner.checkButton'), 'Check with Prahar');
});

test('6. Translation fallback gracefully uses English when key is missing in target locale', () => {
  const partialTranslations = {
    en: { sample: { title: 'English Title', onlyInEnglish: 'Unique English String' } },
    hi: { sample: { title: 'Hindi Title' } }, // missing onlyInEnglish
    kn: {}
  };

  const tr = createTranslator('hi', partialTranslations);
  assert.equal(tr.t('sample.title'), 'Hindi Title');
  // Should fall back to English string
  assert.equal(tr.t('sample.onlyInEnglish'), 'Unique English String');
  // Missing in both falls back to fallback string or keyPath
  assert.equal(tr.t('sample.nonExistent', 'Fallback Text'), 'Fallback Text');
  assert.equal(tr.t('sample.anotherMissing'), 'sample.anotherMissing');
});

test('7. Parameter interpolation replaces dynamic tokens in all languages', () => {
  const tr = createTranslator('en');
  assert.equal(tr.t('dashboard.viewAllScans', { count: 7 }), 'View all scans (7)');

  tr.setLang('hi');
  assert.equal(tr.t('dashboard.viewAllScans', { count: 7 }), 'सभी जांचें देखें (7)');

  tr.setLang('kn');
  assert.equal(tr.t('dashboard.viewAllScans', { count: 7 }), 'ಎಲ್ಲಾ ತಪಾಸಣೆಗಳನ್ನು ವೀಕ್ಷಿಸಿ (7)');
});

test('8. Full parity between English, Hindi, and Kannada translation dictionaries', () => {
  const enKeys = getAllKeys(en);
  const hiKeys = getAllKeys(hi);
  const knKeys = getAllKeys(kn);

  assert.equal(enKeys.length, hiKeys.length, 'Key counts between en and hi must match');
  assert.equal(enKeys.length, knKeys.length, 'Key counts between en and kn must match');

  const missingHi = enKeys.filter(k => !hiKeys.includes(k));
  const missingKn = enKeys.filter(k => !knKeys.includes(k));

  assert.deepEqual(missingHi, [], 'Hindi dictionary must have no missing keys');
  assert.deepEqual(missingKn, [], 'Kannada dictionary must have no missing keys');
});

test('9. Rapid language switching produces deterministic and non-stale results', () => {
  const tr = createTranslator('en');
  const sequence = ['hi', 'kn', 'en', 'hi', 'en', 'kn', 'hi', 'kn', 'en'];

  for (const lang of sequence) {
    tr.setLang(lang);
    if (lang === 'en') assert.equal(tr.t('nav.home'), 'Home');
    if (lang === 'hi') assert.equal(tr.t('nav.home'), 'होम');
    if (lang === 'kn') assert.equal(tr.t('nav.home'), 'ಮುಖಪುಟ');
  }

  assert.equal(tr.getLang(), 'en');
});

test('10. Never renders raw undefined or [object Object]', () => {
  const tr = createTranslator('en');
  assert.notEqual(tr.t('nav'), '[object Object]');
  assert.notEqual(tr.t('nonexistent.nested.key'), 'undefined');
  assert.equal(tr.t(''), '');
  assert.equal(tr.t(null), '');
});
