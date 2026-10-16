export function Logo({ size = 26 }) {
    return (
        <span className="logo">
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
           strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="6" cy="6" r="3" />
        <circle cx="6" cy="18" r="3" />
        <path d="M20 4 8.12 15.88" />
        <path d="M14.47 14.48 20 20" />
        <path d="M8.12 8.12 12 12" />
      </svg>
      <span>Barber<b>App</b></span>
    </span>
    );
}

export function BarberPole() {
    return (
        <svg className="pole" viewBox="0 0 120 320" aria-hidden="true">
            <defs>
                <pattern id="listras" width="44" height="44" patternUnits="userSpaceOnUse" patternTransform="rotate(-35)">
                    <rect width="44" height="44" fill="#f2f0eb" />
                    <rect width="15" height="44" fill="#d4a24c" />
                    <rect x="22" width="15" height="44" fill="#2b2b33" />
                </pattern>
                <clipPath id="tubo">
                    <rect x="30" y="52" width="60" height="216" rx="30" />
                </clipPath>
            </defs>
            <rect x="20" y="12" width="80" height="44" rx="14" fill="#26262e" />
            <rect x="20" y="264" width="80" height="44" rx="14" fill="#26262e" />
            <g clipPath="url(#tubo)">
                <rect x="30" y="52" width="60" height="216" fill="url(#listras)" />
            </g>
            <rect x="30" y="52" width="60" height="216" rx="30" fill="none" stroke="#3a3a44" strokeWidth="3" />
            <rect x="38" y="64" width="8" height="192" rx="4" fill="#ffffff" opacity="0.16" />
        </svg>
    );
}