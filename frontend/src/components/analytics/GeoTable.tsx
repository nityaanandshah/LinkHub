import type { GeoStats } from '../../types/api';

const S = {
  primary: '#2563EB',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  trackBg: '#F1F5F9',
} as const;

interface Props {
  data: GeoStats | null;
}

export default function GeoTable({ data }: Props) {
  if (!data) {
    return <p style={{ textAlign: 'center', color: S.textMuted, padding: '40px 0', fontSize: '0.875rem' }}>No geographic data</p>;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
      {/* Countries */}
      <div>
        <h3 style={{ fontSize: '0.875rem', fontWeight: 500, color: S.textSecondary, marginBottom: '14px', fontFamily: 'var(--font-body)' }}>
          Countries
        </h3>
        {data.countries.length === 0 ? (
          <p style={{ fontSize: '0.75rem', color: S.textMuted }}>No data</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {data.countries.map((c) => (
              <div key={c.country} className="flex items-center justify-between">
                <span style={{ fontSize: '0.875rem', color: S.textPrimary }}>{c.country || 'Unknown'}</span>
                <div className="flex items-center" style={{ gap: '12px' }}>
                  <div style={{ width: '96px', height: '6px', backgroundColor: S.trackBg, borderRadius: '9999px' }}>
                    <div
                      style={{
                        width: `${Math.min(c.percentage, 100)}%`,
                        height: '6px',
                        backgroundColor: S.primary,
                        borderRadius: '9999px',
                      }}
                    />
                  </div>
                  <span style={{ fontSize: '0.75rem', color: S.textMuted, width: '64px', textAlign: 'right' }}>
                    {c.clicks} ({c.percentage.toFixed(1)}%)
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Cities */}
      <div>
        <h3 style={{ fontSize: '0.875rem', fontWeight: 500, color: S.textSecondary, marginBottom: '14px', fontFamily: 'var(--font-body)' }}>
          Top Cities
        </h3>
        {data.cities.length === 0 ? (
          <p style={{ fontSize: '0.75rem', color: S.textMuted }}>No data</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {data.cities.slice(0, 10).map((c) => (
              <div key={`${c.city}-${c.country}`} className="flex items-center justify-between">
                <span style={{ fontSize: '0.875rem', color: S.textPrimary }}>
                  {c.city || 'Unknown'}{c.country ? `, ${c.country}` : ''}
                </span>
                <span style={{ fontSize: '0.75rem', color: S.textMuted }}>{c.clicks} clicks</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
