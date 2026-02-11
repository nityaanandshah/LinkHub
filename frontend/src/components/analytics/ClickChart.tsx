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

interface Props {
  data: TimeseriesPoint[];
}

export default function ClickChart({ data }: Props) {
  if (data.length === 0) {
    return <p className="text-center text-gray-400 py-8 text-sm">No click data available</p>;
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
            <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.2} />
            <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
        <XAxis
          dataKey="label"
          tick={{ fontSize: 12, fill: '#94a3b8' }}
          axisLine={{ stroke: '#e2e8f0' }}
          tickLine={false}
        />
        <YAxis
          tick={{ fontSize: 12, fill: '#94a3b8' }}
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
        />
        <Tooltip
          contentStyle={{
            borderRadius: 8,
            border: '1px solid #e2e8f0',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)',
            fontSize: 13,
          }}
        />
        <Area
          type="monotone"
          dataKey="clicks"
          stroke="#3b82f6"
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
    // If parsing fails, try treating it as just a date string
    return ts.slice(0, 10);
  }
}
