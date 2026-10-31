// ============================================================================
// Tailwind 3.4 theme extension - Smart Food Delivery Management System
// ============================================================================
//
// How to import (tailwind.config.js, ESM project):
//
//   import theme from './design/tokens/tailwind.theme.js';
//
//   export default {
//     content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
//     theme: { extend: theme },
//     plugins: [],
//   };
//
// This object is an extension, not a replacement: Tailwind's defaults stay
// available. The keys below intentionally override a default where the token
// value differs (spacing is px, not rem, so it matches tokens.css exactly).
//
// SOURCE OF TRUTH: design/tokens/tokens.css. Every value here mirrors a custom
// property in that file. Edit tokens.css first, then this file, then
// design/tokens/tokens.json - in the same commit. Never hardcode a hex value
// in a component; if a colour is missing, add a token instead.
// ============================================================================

const theme = {
  colors: {
    brand: {
      50: '#FEF2F3',
      100: '#FDE2E4',
      200: '#FBC5C9',
      500: '#E23744',
      600: '#D02B38',
      700: '#B01F2B',
      900: '#6E1119',
    },
    ink: {
      900: '#111827',
      800: '#1C1C1C',
      700: '#374151',
      600: '#4B5563',
    },
    muted: {
      500: '#6B7280',
      400: '#9CA3AF',
    },
    line: {
      200: '#E5E7EB',
      100: '#F3F4F6',
    },
    surface: {
      DEFAULT: '#FFFFFF',
      0: '#FFFFFF',
    },
    canvas: {
      DEFAULT: '#F9FAFB',
      50: '#F9FAFB',
      100: '#F3F4F6',
    },
    success: {
      DEFAULT: '#16A34A',
      600: '#16A34A',
      50: '#F0FDF4',
    },
    warning: {
      DEFAULT: '#D97706',
      600: '#D97706',
      50: '#FFFBEB',
    },
    danger: {
      DEFAULT: '#DC2626',
      600: '#DC2626',
      50: '#FEF2F2',
    },
    info: {
      DEFAULT: '#2563EB',
      600: '#2563EB',
      50: '#EFF6FF',
    },
    // One colour per order state. Read only through <StatusBadge />.
    // Use as bg-status-preparing/10 + text-status-preparing for a pill.
    status: {
      placed: '#6B7280',
      confirmed: '#2563EB',
      preparing: '#D97706',
      readyForPickup: '#7C3AED',
      assigned: '#4F46E5',
      pickedUp: '#0891B2',
      outForDelivery: '#0D9488',
      delivered: '#16A34A',
      cancelled: '#DC2626',
      rejected: '#B91C1C',
    },
  },

  fontFamily: {
    sans: ['Inter', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
    mono: ['JetBrains Mono', 'Cascadia Mono', 'Consolas', 'monospace'],
  },

  // [size, { lineHeight }] - line heights come from the tight/snug/normal tokens.
  fontSize: {
    '2xs': ['11px', { lineHeight: '1.35' }],
    xs: ['12px', { lineHeight: '1.35' }],
    sm: ['13px', { lineHeight: '1.55' }],
    base: ['14px', { lineHeight: '1.55' }],
    md: ['16px', { lineHeight: '1.55' }],
    lg: ['18px', { lineHeight: '1.35' }],
    xl: ['20px', { lineHeight: '1.35' }],
    '2xl': ['24px', { lineHeight: '1.2' }],
    '3xl': ['30px', { lineHeight: '1.2' }],
    '4xl': ['36px', { lineHeight: '1.2' }],
  },

  lineHeight: {
    tight: '1.2',
    snug: '1.35',
    normal: '1.55',
  },

  fontWeight: {
    regular: '400',
    medium: '500',
    semi: '600',
    bold: '700',
  },

  // 4px scale. Tailwind's defaults are the same steps in rem; these are px so
  // they line up 1:1 with --space-* and never depend on the root font size.
  spacing: {
    0: '0px',
    1: '4px',
    2: '8px',
    3: '12px',
    4: '16px',
    5: '20px',
    6: '24px',
    8: '32px',
    10: '40px',
    12: '48px',
    16: '64px',
  },

  borderRadius: {
    xs: '4px',
    sm: '6px',
    md: '8px',
    lg: '12px',
    xl: '16px',
    '2xl': '24px',
    full: '9999px',
  },

  boxShadow: {
    xs: '0 1px 2px rgba(17, 24, 39, 0.05)',
    sm: '0 1px 3px rgba(17, 24, 39, 0.08), 0 1px 2px rgba(17, 24, 39, 0.04)',
    md: '0 4px 12px rgba(17, 24, 39, 0.08)',
    lg: '0 12px 28px rgba(17, 24, 39, 0.12)',
    focus: '0 0 0 3px rgba(226, 55, 68, 0.28)',
  },

  transitionDuration: {
    fast: '120ms',
    base: '200ms',
    slow: '320ms',
  },

  transitionTimingFunction: {
    standard: 'cubic-bezier(0.2, 0, 0, 1)',
  },

  zIndex: {
    dropdown: '1000',
    sticky: '1020',
    drawer: '1040',
    modal: '1050',
    toast: '1060',
  },

  screens: {
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px',
    '2xl': '1536px',
  },

  keyframes: {
    'fade-in': {
      from: { opacity: '0' },
      to: { opacity: '1' },
    },
    'slide-up': {
      from: { opacity: '0', transform: 'translateY(8px)' },
      to: { opacity: '1', transform: 'translateY(0)' },
    },
    // Live-tracking dot: gentle breathing ring, never a hard blink.
    'pulse-soft': {
      '0%, 100%': { opacity: '1', transform: 'scale(1)' },
      '50%': { opacity: '0.55', transform: 'scale(0.85)' },
    },
  },

  animation: {
    'fade-in': 'fade-in 200ms cubic-bezier(0.2, 0, 0, 1) both',
    'slide-up': 'slide-up 200ms cubic-bezier(0.2, 0, 0, 1) both',
    'pulse-soft': 'pulse-soft 1800ms cubic-bezier(0.2, 0, 0, 1) infinite',
  },
};

export default theme;
