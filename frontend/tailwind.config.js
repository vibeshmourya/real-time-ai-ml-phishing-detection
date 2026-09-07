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
        dark: {
          950: '#070a13',
          900: '#0c1222',
          850: '#111936',
          800: '#162044',
          700: '#23305e',
          600: '#34457e',
        },
        cyber: {
          blue: '#3b82f6',
          cyan: '#06b6d4',
          emerald: '#10b981',
          amber: '#f59e0b',
          rose: '#f43f5e',
          purple: '#8b5cf6',
          indigo: '#6366f1',
        },
      },
      fontFamily: {
        sans: ['Outfit', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      boxShadow: {
        'glow-safe': '0 0 25px -5px rgba(16, 185, 129, 0.4)',
        'glow-warn': '0 0 25px -5px rgba(245, 158, 11, 0.4)',
        'glow-danger': '0 0 25px -5px rgba(244, 63, 94, 0.45)',
        'glow-cyan': '0 0 25px -5px rgba(6, 182, 212, 0.4)',
        'glow-indigo': '0 0 25px -5px rgba(99, 102, 241, 0.4)',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'scan-line': 'scanLine 2.5s ease-in-out infinite',
      },
      keyframes: {
        scanLine: {
          '0%, 100%': { transform: 'translateY(0%)', opacity: '0.2' },
          '50%': { transform: 'translateY(100%)', opacity: '0.8' },
        },
      },
    },
  },
  plugins: [],
}
