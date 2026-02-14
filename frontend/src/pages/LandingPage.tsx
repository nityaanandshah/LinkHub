import { useNavigate } from 'react-router-dom';

/* ── Palette ── */
const P = {
  heroBg: '#334155',
  accent: '#F97316',
  accentHover: '#EA580C',
  primary: '#2563EB',
  primaryHover: '#1D4ED8',
  primaryLight: '#DBEAFE',
  bg: '#F8FAFC',
  card: '#FFFFFF',
  border: '#E2E8F0',
  borderLight: '#F1F5F9',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  cardShadow: '0px 1px 2px rgba(0,0,0,0.05), 0px 4px 8px rgba(0,0,0,0.04)',
  btnShadow: '0px 2px 4px rgba(0,0,0,0.08)',
} as const;

export default function LandingPage() {
  const navigate = useNavigate();

  return (
    <div style={{ backgroundColor: P.bg, minHeight: '100vh' }}>
      {/* ════════ Hero ════════ */}
      <section
        style={{
          backgroundColor: P.heroBg,
          padding: '64px 24px 56px',
          textAlign: 'center',
        }}
      >
        <h1
          style={{
            fontFamily: 'var(--font-heading)',
            fontWeight: 800,
            fontSize: 'clamp(2.5rem, 5vw, 3.5rem)',
            color: '#FFFFFF',
            margin: '0 0 12px',
            lineHeight: 1.15,
          }}
        >
          LinkHub
        </h1>
        <p
          style={{
            fontFamily: 'var(--font-heading)',
            fontWeight: 500,
            fontSize: 'clamp(1rem, 2.5vw, 1.35rem)',
            color: P.accent,
            fontStyle: 'italic',
            margin: 0,
          }}
        >
          Shorten. Share. Analyze.
        </p>
      </section>

      {/* ════════ URL Input Bar ════════ */}
      <section style={{ maxWidth: '680px', margin: '-28px auto 0', padding: '0 24px', position: 'relative', zIndex: 1 }}>
        <div
          style={{
            display: 'flex',
            backgroundColor: P.card,
            borderRadius: '12px',
            border: `1px solid ${P.border}`,
            boxShadow: '0px 4px 16px rgba(0,0,0,0.08)',
            overflow: 'hidden',
          }}
        >
          <input
            type="text"
            readOnly
            placeholder="Enter your long URL here..."
            onClick={() => navigate('/login')}
            style={{
              flex: 1,
              padding: '16px 20px',
              border: 'none',
              outline: 'none',
              fontSize: '0.95rem',
              color: P.textMuted,
              backgroundColor: 'transparent',
              cursor: 'pointer',
            }}
          />
          <button
            onClick={() => navigate('/login')}
            style={{
              padding: '16px 28px',
              backgroundColor: P.accent,
              color: '#FFFFFF',
              fontWeight: 600,
              fontSize: '0.95rem',
              border: 'none',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              transition: 'background-color 0.15s',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = P.accentHover; }}
            onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = P.accent; }}
          >
            Shorten URL &rsaquo;
          </button>
        </div>
      </section>

      {/* ════════ Dashboard Preview ════════ */}
      <section style={{ maxWidth: '900px', margin: '48px auto 0', padding: '0 24px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '20px' }}>
          {/* Main preview card */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1.4fr 1fr',
              gap: '20px',
            }}
          >
            {/* Hero copy card */}
            <div
              style={{
                backgroundColor: P.card,
                borderRadius: '12px',
                border: `1px solid ${P.border}`,
                boxShadow: P.cardShadow,
                padding: '36px 32px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                gap: '16px',
              }}
            >
              <p style={{ fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em', color: P.accent, margin: 0 }}>
                Why LinkHub?
              </p>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: P.textPrimary, margin: 0, lineHeight: 1.3 }}>
                Long URLs are ugly,<br />forgettable, and untrusted.
              </h2>
              <p style={{ fontSize: '0.9rem', color: P.textSecondary, margin: 0, lineHeight: 1.6 }}>
                Short links look clean, build trust, and get more clicks. Plus, every shortened link gives you real-time data on who clicked, from where, and on what device.
              </p>
              <button
                onClick={() => navigate('/login')}
                className="transition-colors"
                style={{
                  alignSelf: 'flex-start',
                  marginTop: '4px',
                  padding: '10px 24px',
                  backgroundColor: P.primary,
                  color: '#FFFFFF',
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  border: 'none',
                  borderRadius: '10px',
                  cursor: 'pointer',
                  boxShadow: P.btnShadow,
                }}
                onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = P.primaryHover; }}
                onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = P.primary; }}
              >
                Start for free &rarr;
              </button>
            </div>

            {/* Analytics overview side card */}
            <div
              style={{
                backgroundColor: P.card,
                borderRadius: '12px',
                border: `1px solid ${P.border}`,
                boxShadow: P.cardShadow,
                padding: '28px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                gap: '8px',
              }}
            >
              <h3 style={{ fontSize: '1.05rem', fontWeight: 600, color: P.textPrimary, margin: 0 }}>Analytics Overview</h3>
              <p style={{ fontSize: '0.8rem', color: P.textMuted, margin: 0 }}>Clicks Last 7 Days</p>
              {/* Mini area chart with axes */}
              <svg viewBox="0 0 220 120" style={{ width: '100%', flex: 1, minHeight: '0' }}>
                <defs>
                  <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={P.accent} stopOpacity="0.25" />
                    <stop offset="100%" stopColor={P.accent} stopOpacity="0.02" />
                  </linearGradient>
                </defs>
                {/* Y-axis */}
                <line x1="30" y1="10" x2="30" y2="95" stroke="#E2E8F0" strokeWidth="1" />
                {/* X-axis */}
                <line x1="30" y1="95" x2="210" y2="95" stroke="#E2E8F0" strokeWidth="1" />
                {/* Horizontal grid lines */}
                {[30, 52, 73].map((y) => (
                  <line key={y} x1="30" y1={y} x2="210" y2={y} stroke="#F1F5F9" strokeWidth="0.8" />
                ))}
                {/* Y-axis labels */}
                <text x="24" y="33" textAnchor="end" style={{ fontSize: '8px', fill: '#94A3B8' }}>300</text>
                <text x="24" y="55" textAnchor="end" style={{ fontSize: '8px', fill: '#94A3B8' }}>200</text>
                <text x="24" y="76" textAnchor="end" style={{ fontSize: '8px', fill: '#94A3B8' }}>100</text>
                <text x="24" y="98" textAnchor="end" style={{ fontSize: '8px', fill: '#94A3B8' }}>0</text>
                {/* X-axis labels */}
                {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((d, i) => (
                  <text key={d} x={42 + i * 26} y="108" textAnchor="middle" style={{ fontSize: '7.5px', fill: '#94A3B8' }}>{d}</text>
                ))}
                {/* Filled area */}
                <polygon
                  points="42,80 68,60 94,65 120,38 146,44 172,22 198,30 198,95 42,95"
                  fill="url(#areaFill)"
                />
                {/* Line */}
                <polyline
                  points="42,80 68,60 94,65 120,38 146,44 172,22 198,30"
                  fill="none"
                  stroke={P.accent}
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                {/* Data points */}
                {[
                  [42, 80], [68, 60], [94, 65], [120, 38], [146, 44], [172, 22], [198, 30],
                ].map(([cx, cy], i) => (
                  <circle key={i} cx={cx} cy={cy} r="3" fill={P.accent} stroke="#FFFFFF" strokeWidth="1.5" />
                ))}
              </svg>
            </div>
          </div>

          {/* Stats row + side cards */}
          <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '20px' }}>
            {/* Stats + mini donut charts card */}
            <div
              style={{
                backgroundColor: P.card,
                borderRadius: '12px',
                border: `1px solid ${P.border}`,
                boxShadow: P.cardShadow,
                padding: '28px 32px',
                display: 'flex',
                flexDirection: 'column',
                gap: '24px',
              }}
            >
              {/* Stats row */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
                <StatItem label="Total Clicks" value="12,450" icon={<CheckCircleIcon color="#10B981" />} />
                <StatItem label="Active Links" value="58" icon={<LinkIcon color={P.primary} />} />
                <StatItem label="Click Rate" value="4.7%" icon={<TrendIcon color={P.accent} />} />
              </div>
              {/* Divider */}
              <div style={{ height: '1px', backgroundColor: P.border }} />
              {/* Mini donut charts row */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px' }}>
                <MiniDonut label="Desktop" pct={68} color={P.primary} />
                <MiniDonut label="Chrome" pct={54} color={P.accent} />
                <MiniDonut label="Mac OS" pct={42} color="#10B981" />
              </div>
            </div>

            {/* Side info cards */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <InfoCard label="Top Referrer" value="Google" icon={<BarChartIcon />} />
              <InfoCard label="Top Device" value="Mobile" icon={<DeviceIcon />} />
              <InfoCard label="Top Location" value="New York, USA" icon={<LocationIcon />} />
            </div>
          </div>
        </div>
      </section>

      {/* ════════ Features ════════ */}
      <section style={{ maxWidth: '900px', margin: '56px auto 0', padding: '0 24px 80px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
          <FeatureCard
            title="Generate QR Codes"
            icon={
              <svg width="72" height="72" viewBox="0 0 48 48" fill="none">
                <rect x="4" y="4" width="16" height="16" rx="2" stroke={P.textPrimary} strokeWidth="2.5" />
                <rect x="8" y="8" width="8" height="8" rx="1" fill={P.textPrimary} />
                <rect x="28" y="4" width="16" height="16" rx="2" stroke={P.textPrimary} strokeWidth="2.5" />
                <rect x="32" y="8" width="8" height="8" rx="1" fill={P.textPrimary} />
                <rect x="4" y="28" width="16" height="16" rx="2" stroke={P.textPrimary} strokeWidth="2.5" />
                <rect x="8" y="32" width="8" height="8" rx="1" fill={P.textPrimary} />
                <rect x="28" y="28" width="4" height="4" fill={P.textPrimary} />
                <rect x="36" y="28" width="8" height="4" fill={P.textPrimary} />
                <rect x="28" y="36" width="4" height="8" fill={P.textPrimary} />
                <rect x="36" y="40" width="8" height="4" fill={P.textPrimary} />
              </svg>
            }
          />
          <FeatureCard
            title="Secure & Reliable"
            icon={
              <svg width="72" height="72" viewBox="0 0 20 20" fill={P.textSecondary}>
                <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
            }
          />
          <FeatureCard
            title="In-Depth Analytics"
            icon={
              <svg width="72" height="72" viewBox="0 0 20 20" fill={P.accent}>
                <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v7a1 1 0 01-1 1H3a1 1 0 01-1-1v-7zm6-4a1 1 0 011-1h2a1 1 0 011 1v11a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-4a1 1 0 011-1h2a1 1 0 011 1v15a1 1 0 01-1 1h-2a1 1 0 01-1-1V3z" />
              </svg>
            }
          />
        </div>
      </section>

    </div>
  );
}

/* ──────────── Sub-components ──────────── */

function StatItem({ label, value, icon }: { label: string; value: string; icon: React.ReactNode }) {
  return (
    <div style={{ textAlign: 'center' }}>
      <p style={{ fontSize: '0.85rem', color: P.textMuted, margin: '0 0 6px', fontWeight: 500 }}>{label}</p>
      <div className="flex items-center justify-center gap-2">
        <span style={{ fontSize: '1.85rem', fontWeight: 700, color: P.textPrimary }}>{value}</span>
        <span style={{ display: 'inline-flex' }}>{icon}</span>
      </div>
    </div>
  );
}

function InfoCard({ label, value, icon }: { label: string; value: string; icon: React.ReactNode }) {
  return (
    <div
      style={{
        backgroundColor: P.card,
        borderRadius: '10px',
        border: `1px solid ${P.border}`,
        boxShadow: P.cardShadow,
        padding: '18px 22px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}
    >
      <div>
        <p style={{ fontSize: '0.8rem', color: P.textMuted, margin: '0 0 4px', fontWeight: 500 }}>{label}</p>
        <p style={{ fontSize: '1.1rem', fontWeight: 600, color: P.textPrimary, margin: 0 }}>{value}</p>
      </div>
      <span style={{ color: P.primary, display: 'inline-flex' }}>{icon}</span>
    </div>
  );
}

function FeatureCard({ title, icon }: { title: string; icon: React.ReactNode }) {
  return (
    <div
      style={{
        backgroundColor: P.card,
        borderRadius: '12px',
        border: `1px solid ${P.border}`,
        boxShadow: P.cardShadow,
        padding: '40px 24px',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '18px',
      }}
    >
      {icon}
      <p style={{ fontSize: '1.1rem', fontWeight: 600, color: P.textPrimary, margin: 0 }}>{title}</p>
    </div>
  );
}

/* ──────────── Mini donut chart ──────────── */

function MiniDonut({ label, pct, color }: { label: string; pct: number; color: string }) {
  const r = 30;
  const circ = 2 * Math.PI * r;
  const filled = (pct / 100) * circ;
  const gap = circ - filled;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
      <svg width="72" height="72" viewBox="0 0 80 80">
        {/* Track */}
        <circle cx="40" cy="40" r={r} fill="none" stroke={P.borderLight} strokeWidth="8" />
        {/* Filled arc */}
        <circle
          cx="40" cy="40" r={r}
          fill="none"
          stroke={color}
          strokeWidth="8"
          strokeDasharray={`${filled} ${gap}`}
          strokeDashoffset={circ * 0.25}
          strokeLinecap="round"
        />
        {/* Percentage text */}
        <text x="40" y="40" textAnchor="middle" dominantBaseline="central"
          style={{ fontSize: '13px', fontWeight: 700, fill: P.textPrimary, fontFamily: 'var(--font-body)' }}
        >
          {pct}%
        </text>
      </svg>
      <span style={{ fontSize: '0.75rem', fontWeight: 500, color: P.textMuted }}>{label}</span>
    </div>
  );
}

/* ──────────── Tiny inline icons ──────────── */

function CheckCircleIcon({ color }: { color: string }) {
  return (
    <svg width="26" height="26" viewBox="0 0 20 20" fill={color}>
      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
    </svg>
  );
}

function LinkIcon({ color }: { color: string }) {
  return (
    <svg width="26" height="26" viewBox="0 0 20 20" fill={color}>
      <path fillRule="evenodd" d="M12.586 4.586a2 2 0 112.828 2.828l-3 3a2 2 0 01-2.828 0 1 1 0 00-1.414 1.414 4 4 0 005.656 0l3-3a4 4 0 00-5.656-5.656l-1.5 1.5a1 1 0 101.414 1.414l1.5-1.5zm-5 5a2 2 0 012.828 0 1 1 0 101.414-1.414 4 4 0 00-5.656 0l-3 3a4 4 0 105.656 5.656l1.5-1.5a1 1 0 10-1.414-1.414l-1.5 1.5a2 2 0 11-2.828-2.828l3-3z" clipRule="evenodd" />
    </svg>
  );
}

function TrendIcon({ color }: { color: string }) {
  return (
    <svg width="26" height="26" viewBox="0 0 20 20" fill={color}>
      <path fillRule="evenodd" d="M12 7a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0V8.414l-4.293 4.293a1 1 0 01-1.414 0L8 10.414l-4.293 4.293a1 1 0 01-1.414-1.414l5-5a1 1 0 011.414 0L11 10.586 14.586 7H12z" clipRule="evenodd" />
    </svg>
  );
}

function BarChartIcon() {
  return (
    <svg width="30" height="30" viewBox="0 0 20 20" fill={P.primary}>
      <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v7a1 1 0 01-1 1H3a1 1 0 01-1-1v-7zm6-4a1 1 0 011-1h2a1 1 0 011 1v11a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-4a1 1 0 011-1h2a1 1 0 011 1v15a1 1 0 01-1 1h-2a1 1 0 01-1-1V3z" />
    </svg>
  );
}

function DeviceIcon() {
  return (
    <svg width="30" height="30" viewBox="0 0 20 20" fill={P.primary}>
      <path fillRule="evenodd" d="M7 2a2 2 0 00-2 2v12a2 2 0 002 2h6a2 2 0 002-2V4a2 2 0 00-2-2H7zm3 14a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
    </svg>
  );
}

function LocationIcon() {
  return (
    <svg width="30" height="30" viewBox="0 0 20 20" fill="#EF4444">
      <path fillRule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clipRule="evenodd" />
    </svg>
  );
}
