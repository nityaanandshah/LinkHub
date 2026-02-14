import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import type { DeviceStats } from '../../types/api';

/* ── Fixed 5-color chart palette ── */
const CHART_COLORS = ['#2563EB', '#F97316', '#10B981', '#8B5CF6', '#06B6D4'];

/* ── Per-section color offsets so each donut looks distinct ── */
const DEVICE_COLORS  = [CHART_COLORS[0], CHART_COLORS[3], CHART_COLORS[4], CHART_COLORS[1], CHART_COLORS[2]]; // blue first
const BROWSER_COLORS = [CHART_COLORS[1], CHART_COLORS[0], CHART_COLORS[3], CHART_COLORS[4], CHART_COLORS[2]]; // orange first
const OS_COLORS      = [CHART_COLORS[2], CHART_COLORS[0], CHART_COLORS[1], CHART_COLORS[3], CHART_COLORS[4]]; // green first

const tooltipStyle: React.CSSProperties = {
  borderRadius: 8,
  border: '1px solid #E2E8F0',
  boxShadow: '0px 4px 12px rgba(0,0,0,0.08), 0px 1px 3px rgba(0,0,0,0.06)',
  fontSize: 12,
  fontFamily: 'Source Sans 3, sans-serif',
  padding: '8px 12px',
};

interface Props {
  data: DeviceStats | null;
}

export default function DeviceCharts({ data }: Props) {
  if (!data) {
    return <p style={{ textAlign: 'center', color: '#94A3B8', padding: '40px 0', fontSize: '0.875rem' }}>No device data</p>;
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
      <ChartSection title="Device Types" items={data.deviceTypes} colors={DEVICE_COLORS} />
      <ChartSection title="Browsers" items={data.browsers} colors={BROWSER_COLORS} />
      <ChartSection title="Operating Systems" items={data.operatingSystems} colors={OS_COLORS} />
    </div>
  );
}

function ChartSection({ title, items, colors }: { title: string; items: { name: string; clicks: number; percentage: number }[]; colors: string[] }) {
  if (items.length === 0) {
    return (
      <div>
        <h3 style={{ fontSize: '0.875rem', fontWeight: 500, color: '#475569', marginBottom: '12px', fontFamily: 'var(--font-body)' }}>{title}</h3>
        <p style={{ fontSize: '0.75rem', color: '#94A3B8' }}>No data</p>
      </div>
    );
  }

  return (
    <div>
      <h3 style={{ fontSize: '0.875rem', fontWeight: 500, color: '#475569', marginBottom: '14px', fontFamily: 'var(--font-body)' }}>{title}</h3>
      <div className="flex items-center gap-5">
        <ResponsiveContainer width={140} height={140}>
          <PieChart>
            <Pie
              data={items}
              dataKey="clicks"
              nameKey="name"
              cx="50%"
              cy="50%"
              outerRadius={60}
              innerRadius={35}
              strokeWidth={2}
            >
              {items.map((_entry, index) => (
                <Cell key={index} fill={colors[index % colors.length]} />
              ))}
            </Pie>
            <Tooltip contentStyle={tooltipStyle} formatter={(value: number) => [`${value} clicks`]} />
          </PieChart>
        </ResponsiveContainer>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {items.slice(0, 5).map((item, i) => (
            <div key={item.name} className="flex items-center justify-between" style={{ fontSize: '0.75rem' }}>
              <div className="flex items-center" style={{ gap: '8px' }}>
                <span
                  style={{
                    width: '10px',
                    height: '10px',
                    borderRadius: '50%',
                    backgroundColor: colors[i % colors.length],
                    display: 'inline-block',
                  }}
                />
                <span style={{ color: '#0F172A' }}>{item.name || 'Unknown'}</span>
              </div>
              <span style={{ color: '#94A3B8' }}>{item.percentage.toFixed(1)}%</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
