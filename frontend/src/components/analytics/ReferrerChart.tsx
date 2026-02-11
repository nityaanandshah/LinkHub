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

export default function ReferrerChart({ data }: Props) {
  if (data.length === 0) {
    return <p className="text-center text-gray-400 py-8 text-sm">No referrer data</p>;
  }

  const formatted = data.map((d) => ({
    ...d,
    name: d.referrer || 'Direct',
  }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={formatted} layout="vertical" margin={{ top: 5, right: 5, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" horizontal={false} />
        <XAxis type="number" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
        <YAxis
          dataKey="name"
          type="category"
          tick={{ fontSize: 12, fill: '#64748b' }}
          axisLine={false}
          tickLine={false}
          width={120}
        />
        <Tooltip
          contentStyle={{
            borderRadius: 8,
            border: '1px solid #e2e8f0',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)',
            fontSize: 13,
          }}
          formatter={(value: number, _name: string) => [`${value} clicks`, 'Clicks']}
        />
        <Bar dataKey="clicks" fill="#8b5cf6" radius={[0, 4, 4, 0]} barSize={20} />
      </BarChart>
    </ResponsiveContainer>
  );
}
