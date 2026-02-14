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

interface Props {
  data: ReferrerStats[];
}

const DEFAULT_REFERRERS = ['Direct', 'Google', 'LinkedIn', 'Twitter / X', 'Reddit', 'Facebook', 'Others'];

export default function ReferrerChart({ data }: Props) {
  if (data.length === 0) {
    return <p className="text-center text-gray-400 py-8 text-sm">No referrer data</p>;
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

  // Build chart data with all default referrers (0 if no data)
  const formatted = DEFAULT_REFERRERS.map((name) => ({
    name,
    clicks: dataMap.get(name) || 0,
  }));

  // Build "Others" tooltip detail
  const othersDetail = otherSources.length > 0
    ? otherSources.map((s) => `${s.name}: ${s.clicks}`).join(', ')
    : '';

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={formatted} layout="vertical" margin={{ top: 5, right: 5, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" horizontal={false} />
        <XAxis type="number" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} allowDecimals={false} />
        <YAxis
          dataKey="name"
          type="category"
          tick={{ fontSize: 12, fill: '#64748b' }}
          axisLine={false}
          tickLine={false}
          width={100}
        />
        <Tooltip
          contentStyle={{
            borderRadius: 8,
            border: '1px solid #e2e8f0',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)',
            fontSize: 13,
            maxWidth: 300,
          }}
          formatter={(value: number, _name: string, props: { payload?: { name: string } }) => {
            if (props.payload?.name === 'Others' && othersDetail) {
              return [`${value} clicks (${othersDetail})`, 'Clicks'];
            }
            return [`${value} clicks`, 'Clicks'];
          }}
        />
        <Bar dataKey="clicks" fill="#8b5cf6" radius={[0, 4, 4, 0]} barSize={20} />
      </BarChart>
    </ResponsiveContainer>
  );
}
