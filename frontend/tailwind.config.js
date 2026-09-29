/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Inter Variable"', 'Inter', 'system-ui', 'sans-serif'],
      },
      colors: {
        // Identidad LicitaWatch: azul eléctrico + índigo sobre base "ink" (azul noche)
        brand: {
          50: '#eff6ff', 100: '#dbeafe', 200: '#bfdbfe', 300: '#93c5fd', 400: '#60a5fa',
          500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8', 800: '#1e40af', 900: '#1e3a8a', 950: '#172554',
        },
        ink: {
          50: '#f6f8fb', 100: '#eef2f7', 200: '#dde4ee', 300: '#c3cedd', 400: '#8d9bb0',
          500: '#64748b', 600: '#475569', 700: '#334155', 800: '#1b2436', 900: '#111a2e', 950: '#0b1220',
        },
      },
      boxShadow: {
        card: '0 1px 2px rgba(15,23,42,.04), 0 4px 16px rgba(15,23,42,.06)',
        glow: '0 0 0 1px rgba(59,130,246,.25), 0 8px 30px rgba(37,99,235,.25)',
      },
      backgroundImage: {
        'grid-dark': 'linear-gradient(rgba(148,163,184,.07) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,.07) 1px, transparent 1px)',
        'brand-gradient': 'linear-gradient(135deg, #2563eb 0%, #4f46e5 100%)',
      },
      keyframes: {
        'fade-in': { from: { opacity: '0', transform: 'translateY(4px)' }, to: { opacity: '1', transform: 'none' } },
        'slide-in': { from: { opacity: '0', transform: 'translateX(16px)' }, to: { opacity: '1', transform: 'none' } },
        pulsedot: { '0%, 80%, 100%': { opacity: '.25' }, '40%': { opacity: '1' } },
      },
      animation: {
        'fade-in': 'fade-in .25s ease-out',
        'slide-in': 'slide-in .25s ease-out',
        pulsedot: 'pulsedot 1.2s infinite ease-in-out',
      },
    },
  },
  plugins: [],
}
