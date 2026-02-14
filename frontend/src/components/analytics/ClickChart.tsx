import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { format, parseISO } from 'date-fns';
import type { TimeseriesPoint } from '../../types/api';

/* ── Palette ── */
const CHART_PRIMARY = '#2563EB';

const tooltipStyle: React.CSSProperties = {
  borderRadius: 8,
  border: '1px solid #E2E8F0',
  boxShadow: '0px 4px 12px rgba(0,0,0,0.08), 0px 1px 3px rgba(0,0,0,0.06)',
  fontSize: 13,
  fontFamily: 'Source Sans 3, sans-serif',
  padding: '10px 14px',
};

interface Props {
  data: TimeseriesPoint[];
}

export default function ClickChart({ data }: Props) {
  if (data.length === 0) {
    return <p style={{ textAlign: 'center', color: '#94A3B8', padding: '40px 0', fontSize: '0.875rem' }}>No click data available</p>;
  }

  const formatted = data.map((d) => ({
    ...d,
    label: formatTimestamp(d.timestamp),
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={formatted} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
        <defs>
          <linearGradient id="clickGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={CHART_PRIMARY} stopOpacity={0.12} />
            <stop offset="95%" stopColor={CHART_PRIMARY} stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
        <XAxis
          dataKey="label"
          tick={{ fontSize: 12, fill: '#94A3B8', fontFamily: 'Source Sans 3, sans-serif' }}
          axisLine={{ stroke: '#E2E8F0' }}
          tickLine={false}
        />
        <YAxis
          tick={{ fontSize: 12, fill: '#94A3B8', fontFamily: 'Source Sans 3, sans-serif' }}
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
        />
        <Tooltip contentStyle={tooltipStyle} />
        <Area
          type="monotone"
          dataKey="clicks"
          stroke={CHART_PRIMARY}
          strokeWidth={2}
          fill="url(#clickGradient)"
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}

function formatTimestamp(ts: string): string {
  try {
    const date = parseISO(ts);
    return format(date, 'MMM d');
  } catch {
    return ts.slice(0, 10);
  }
}
