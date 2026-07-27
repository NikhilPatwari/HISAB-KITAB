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
        // Money the farm is owed.
        credit: {
          50: '#f0fdf4',
          500: '#16a34a',
          600: '#15803d',
          700: '#166534',
        },
        // Money the farm owes out.
        debit: {
          50: '#fff7ed',
          500: '#ea580c',
          600: '#c2410c',
          700: '#9a3412',
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
