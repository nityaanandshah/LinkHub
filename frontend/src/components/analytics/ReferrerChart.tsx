import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import type { ReferrerStats } from '../../types/api';

/* ── Palette ── */
const CHART_SECONDARY = '#F97316'; // accent orange for referrer bars

const tooltipStyle: React.CSSProperties = {
  borderRadius: 8,
  border: '1px solid #E2E8F0',
  boxShadow: '0px 4px 12px rgba(0,0,0,0.08), 0px 1px 3px rgba(0,0,0,0.06)',
  fontSize: 13,
  fontFamily: 'Source Sans 3, sans-serif',
  maxWidth: 300,
  padding: '10px 14px',
};

interface Props {
  data: ReferrerStats[];
}

const DEFAULT_REFERRERS = ['Direct', 'Google', 'LinkedIn', 'Twitter / X', 'Reddit', 'Facebook', 'Others'];

export default function ReferrerChart({ data }: Props) {
  if (data.length === 0) {
    return <p style={{ textAlign: 'center', color: '#94A3B8', padding: '40px 0', fontSize: '0.875rem' }}>No referrer data</p>;
  }

  // Map API data to display names
  const dataMap = new Map<string, number>();
  const otherSources: { name: string; clicks: number }[] = [];

  for (const d of data) {
    const raw = (d.referrer || 'Direct').toLowerCase();
    if (!d.referrer || raw === 'direct') {
      dataMap.set('Direct', (dataMap.get('Direct') || 0) + d.clicks);
    } else if (raw.includes('google')) {
      dataMap.set('Google', (dataMap.get('Google') || 0) + d.clicks);
    } else if (raw.includes('linkedin')) {
      dataMap.set('LinkedIn', (dataMap.get('LinkedIn') || 0) + d.clicks);
    } else if (raw.includes('twitter') || raw.includes('x.com') || raw.includes('t.co')) {
      dataMap.set('Twitter / X', (dataMap.get('Twitter / X') || 0) + d.clicks);
    } else if (raw.includes('reddit')) {
      dataMap.set('Reddit', (dataMap.get('Reddit') || 0) + d.clicks);
    } else if (raw.includes('facebook') || raw.includes('fb.com')) {
      dataMap.set('Facebook', (dataMap.get('Facebook') || 0) + d.clicks);
    } else {
      dataMap.set('Others', (dataMap.get('Others') || 0) + d.clicks);
      otherSources.push({ name: d.referrer, clicks: d.clicks });
    }
  }

  const formatted = DEFAULT_REFERRERS.map((name) => ({
    name,
    clicks: dataMap.get(name) || 0,
  }));

  const othersDetail = otherSources.length > 0
    ? otherSources.map((s) => `${s.name}: ${s.clicks}`).join(', ')
    : '';

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={formatted} layout="vertical" margin={{ top: 5, right: 5, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" horizontal={false} />
        <XAxis
          type="number"
          tick={{ fontSize: 12, fill: '#94A3B8', fontFamily: 'Source Sans 3, sans-serif' }}
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
        />
        <YAxis
          dataKey="name"
          type="category"
          tick={{ fontSize: 12, fill: '#475569', fontFamily: 'Source Sans 3, sans-serif' }}
          axisLine={false}
          tickLine={false}
          width={100}
        />
        <Tooltip
          contentStyle={tooltipStyle}
          formatter={(value: number, _name: string, props: { payload?: { name: string } }) => {
            if (props.payload?.name === 'Others' && othersDetail) {
              return [`${value} clicks (${othersDetail})`, 'Clicks'];
            }
            return [`${value} clicks`, 'Clicks'];
          }}
        />
        <Bar dataKey="clicks" fill={CHART_SECONDARY} radius={[0, 4, 4, 0]} barSize={20} />
      </BarChart>
    </ResponsiveContainer>
  );
}
