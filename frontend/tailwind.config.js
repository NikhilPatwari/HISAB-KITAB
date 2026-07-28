/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Splitwise-ish teal for the app chrome.
        brand: {
          50: '#eefbf7',
          100: '#d3f5eb',
          200: '#a9ead8',
          300: '#73d9c1',
          400: '#3fc0a5',
          500: '#1fa78c',
          600: '#158671',
          700: '#146b5c',
          800: '#14554a',
          900: '#13473f',
        },
        // Green: the farm owes the worker. Nothing to chase.
        credit: {
          50: '#f0fdf4',
          500: '#16a34a',
          600: '#15803d',
          700: '#166534',
        },
        // Red: the worker owes the farm — an advance still to be worked off.
        debit: {
          50: '#fef2f2',
          500: '#ef4444',
          600: '#dc2626',
          700: '#b91c1c',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(16, 24, 40, 0.06), 0 1px 3px rgba(16, 24, 40, 0.10)',
        fab: '0 6px 16px rgba(21, 134, 113, 0.35)',
      },
    },
  },
  plugins: [],
}
