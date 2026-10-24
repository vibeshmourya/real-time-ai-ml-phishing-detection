/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        prahar: {
          navy: '#0f172a',
          navyLight: '#1e293b',
          blue: '#1e40af',
          blueHover: '#1d4ed8',
          accent: '#0284c7',
          safe: '#16a34a',
          safeBg: '#f0fdf4',
          warn: '#d97706',
          warnBg: '#fffbeb',
          danger: '#dc2626',
          dangerBg: '#fef2f2',
          surface: '#ffffff',
          surfaceSubtle: '#f8fafc',
          border: '#e2e8f0',
        },
        dark: {
          950: '#0b0f19',
          900: '#111827',
          850: '#1e293b',
          800: '#334155',
          700: '#475569',
          600: '#64748b',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'monospace'],
      },
      boxShadow: {
        'card': '0 2px 8px -2px rgba(15, 23, 42, 0.08), 0 4px 16px -4px rgba(15, 23, 42, 0.04)',
        'card-hover': '0 8px 24px -4px rgba(15, 23, 42, 0.12), 0 4px 12px -2px rgba(15, 23, 42, 0.06)',
        'modal': '0 20px 40px -15px rgba(15, 23, 42, 0.25)',
        'badge-safe': '0 0 0 1px rgba(22, 163, 74, 0.2)',
        'badge-danger': '0 0 0 1px rgba(220, 38, 38, 0.2)',
        'badge-warn': '0 0 0 1px rgba(217, 119, 6, 0.2)',
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
    },
  },
  plugins: [],
}
