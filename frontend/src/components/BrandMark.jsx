export function BrandMark({ className = "brand-icon" }) {
  return (
    <svg
      className={className}
      viewBox="0 0 64 64"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id="dd-base" x1="8" y1="6" x2="56" y2="58" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#f7fbff" />
          <stop offset="1" stopColor="#e2efff" />
        </linearGradient>
        <linearGradient id="dd-orb" x1="30" y1="18" x2="48" y2="46" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#2aa8ff" />
          <stop offset="1" stopColor="#005fd1" />
        </linearGradient>
        <mask id="dd-gate-mask">
          <rect width="64" height="64" fill="white" />
          <path d="M30 21L42 32L30 43V37H21V27H30V21Z" fill="black" />
        </mask>
      </defs>

      <rect x="4" y="4" width="56" height="56" rx="16" fill="url(#dd-base)" />
      <rect x="4" y="4" width="56" height="56" rx="16" fill="none" stroke="#c9d9f0" strokeWidth="1" />

      <g mask="url(#dd-gate-mask)">
        <rect x="13.5" y="15" width="24" height="24" rx="9.5" fill="rgba(255,255,255,0.72)" stroke="rgba(255,255,255,0.95)" strokeWidth="1.2" />
        <circle cx="38.5" cy="32" r="13.5" fill="url(#dd-orb)" />
      </g>

      <path d="M30 21L42 32L30 43V37H21V27H30V21Z" fill="none" stroke="rgba(255,255,255,0.8)" strokeWidth="1" />
    </svg>
  );
}
