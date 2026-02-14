import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAnalytics } from '../hooks/useAnalytics';
import { useAnalyticsLag } from '../hooks/useAnalyticsLag';
import AppLayout from '../components/layout/AppLayout';
import ClickChart from '../components/analytics/ClickChart';
import ReferrerChart from '../components/analytics/ReferrerChart';
import DeviceCharts from '../components/analytics/DeviceCharts';
import GeoTable from '../components/analytics/GeoTable';

const TIME_RANGES = [
  { label: '7d', days: 7 },
  { label: '30d', days: 30 },
  { label: '90d', days: 90 },
  { label: 'All', days: undefined },
];

const S = {
  cardShadow: '0px 1px 2px rgba(0,0,0,0.05), 0px 4px 8px rgba(0,0,0,0.04)',
  border: '#E2E8F0',
  primary: '#2563EB',
  primaryHover: '#1D4ED8',
  accent: '#F97316',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  warningBg: '#FEF3C7',
  warningText: '#92400E',
  errorBg: '#FEE2E2',
  errorText: '#EF4444',
} as const;

export default function Analytics() {
  const { shortCode } = useParams<{ shortCode: string }>();
  const { summary, timeseries, referrers, devices, geo, loading, error, fetchAll } =
    useAnalytics(shortCode || '');
  const lagInfo = useAnalyticsLag();
  const [selectedRange, setSelectedRange] = useState<number | undefined>(30);

  useEffect(() => {
    fetchAll(selectedRange);
  }, [fetchAll, selectedRange]);

  if (!shortCode) return null;

  return (
    <AppLayout>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6 mb-10">
        <div className="flex items-center gap-4">
          <Link
            to="/dashboard"
            className="transition-colors"
            style={{
              padding: '8px',
              color: S.textMuted,
              borderRadius: '8px',
              display: 'inline-flex',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.color = S.textSecondary; e.currentTarget.style.backgroundColor = '#F1F5F9'; }}
            onMouseLeave={(e) => { e.currentTarget.style.color = S.textMuted; e.currentTarget.style.backgroundColor = 'transparent'; }}
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z" clipRule="evenodd" />
            </svg>
          </Link>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: S.textPrimary, margin: 0 }}>
              Analytics
            </h1>
            <p style={{ fontSize: '0.875rem', color: S.textMuted, marginTop: '4px' }}>
              <span style={{ fontFamily: 'monospace', color: S.primary }}>{shortCode}</span>
            </p>
          </div>
        </div>

        {/* Time range selector */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            backgroundColor: '#FFFFFF',
            borderRadius: '10px',
            border: `1px solid ${S.border}`,
            padding: '3px',
            boxShadow: S.cardShadow,
          }}
        >
          {TIME_RANGES.map(({ label, days }) => (
            <button
              key={label}
              onClick={() => setSelectedRange(days)}
              style={{
                padding: '6px 16px',
                fontSize: '0.875rem',
                fontWeight: 500,
                borderRadius: '8px',
                border: 'none',
                cursor: 'pointer',
                transition: 'all 0.15s',
                backgroundColor: selectedRange === days ? S.primary : 'transparent',
                color: selectedRange === days ? '#FFFFFF' : S.textMuted,
              }}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Analytics lag warning */}
      {lagInfo?.delayed && (
        <div
          style={{
            marginBottom: '28px',
            padding: '14px 18px',
            backgroundColor: S.warningBg,
            borderRadius: '8px',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: S.warningText,
            border: `1px solid #FDE68A`,
          }}
        >
          <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
          {lagInfo.message}
        </div>
      )}

      {error && (
        <div
          style={{
            marginBottom: '28px',
            padding: '14px 18px',
            backgroundColor: S.errorBg,
            color: S.errorText,
            borderRadius: '8px',
            fontSize: '0.875rem',
            border: `1px solid ${S.errorText}33`,
          }}
        >
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-24 text-center">
          <div
            className="inline-block w-8 h-8 rounded-full animate-spin"
            style={{ border: '2px solid #DBEAFE', borderTopColor: '#2563EB' }}
          />
          <p style={{ marginTop: '16px', color: S.textMuted }}>Loading analytics...</p>
        </div>
      ) : (
        <>
          {/* Summary cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-10">
            <SummaryCard label="Total Clicks" value={summary?.totalClicks.toLocaleString() ?? '—'} />
            <SummaryCard label="Unique Visitors" value={summary?.uniqueVisitors.toLocaleString() ?? '—'} />
          </div>

          {/* Two-column: Timeseries + Referrers */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <Card title="Clicks Over Time"><ClickChart data={timeseries} /></Card>
            <Card title="Top Referrers"><ReferrerChart data={referrers} /></Card>
          </div>

          {/* Device Breakdown */}
          <div className="mb-6">
            <Card title="Device Breakdown"><DeviceCharts data={devices} /></Card>
          </div>

          {/* Geo */}
          <Card title="Geographic Breakdown"><GeoTable data={geo} /></Card>
        </>
      )}
    </AppLayout>
  );
}

/* ── Reusable summary card ── */
function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '12px',
        border: `1px solid ${S.border}`,
        boxShadow: S.cardShadow,
        padding: '28px',
      }}
    >
      <p style={{ fontSize: '0.875rem', fontWeight: 500, color: S.textSecondary, marginBottom: '8px' }}>{label}</p>
      <p style={{ fontSize: '2rem', fontWeight: 700, color: S.textPrimary, margin: 0 }}>{value}</p>
    </div>
  );
}

/* ── Reusable card wrapper ── */
function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div
      style={{
        backgroundColor: '#FFFFFF',
        borderRadius: '12px',
        border: `1px solid ${S.border}`,
        boxShadow: S.cardShadow,
        padding: '28px',
      }}
    >
      <h2 style={{ fontSize: '1.1rem', fontWeight: 600, color: S.textPrimary, marginBottom: '20px' }}>{title}</h2>
      {children}
    </div>
  );
}
