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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div className="flex items-center gap-3">
          <Link
            to="/dashboard"
            className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
            <p className="text-sm text-gray-500 mt-0.5">
              <span className="font-mono text-primary-600">{shortCode}</span>
            </p>
          </div>
        </div>

        {/* Time range selector */}
        <div className="flex items-center bg-white border border-gray-200 rounded-lg p-0.5">
          {TIME_RANGES.map(({ label, days }) => (
            <button
              key={label}
              onClick={() => setSelectedRange(days)}
              className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                selectedRange === days
                  ? 'bg-primary-600 text-white'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Analytics lag warning */}
      {lagInfo?.delayed && (
        <div className="mb-6 p-4 bg-warning-50 border border-amber-200 text-amber-700 rounded-lg text-sm flex items-center gap-2">
          <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
          {lagInfo.message}
        </div>
      )}

      {error && (
        <div className="mb-6 p-4 bg-danger-50 border border-red-200 text-danger-600 rounded-lg text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center">
          <div className="inline-block w-8 h-8 border-2 border-primary-300 border-t-primary-600 rounded-full animate-spin" />
          <p className="mt-3 text-gray-400">Loading analytics...</p>
        </div>
      ) : (
        <>
          {/* Summary cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <p className="text-sm font-medium text-gray-500 mb-1">Total Clicks</p>
              <p className="text-3xl font-bold text-gray-900">
                {summary?.totalClicks.toLocaleString() ?? '—'}
              </p>
            </div>
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <p className="text-sm font-medium text-gray-500 mb-1">Unique Visitors</p>
              <p className="text-3xl font-bold text-gray-900">
                {summary?.uniqueVisitors.toLocaleString() ?? '—'}
              </p>
            </div>
          </div>

          {/* Two-column: Timeseries + Referrers */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Clicks Over Time</h2>
              <ClickChart data={timeseries} />
            </div>
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Top Referrers</h2>
              <ReferrerChart data={referrers} />
            </div>
          </div>

          {/* Device Breakdown — full width, horizontal layout */}
          <div className="bg-white rounded-xl border border-gray-200 p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Device Breakdown</h2>
            <DeviceCharts data={devices} />
          </div>

          {/* Geo */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Geographic Breakdown</h2>
            <GeoTable data={geo} />
          </div>
        </>
      )}
    </AppLayout>
  );
}
