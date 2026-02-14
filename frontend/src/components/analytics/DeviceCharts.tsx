import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import type { DeviceStats } from '../../types/api';

const COLORS = ['#3b82f6', '#8b5cf6', '#f59e0b', '#10b981', '#ef4444', '#6366f1', '#ec4899', '#14b8a6'];

interface Props {
  data: DeviceStats | null;
}

export default function DeviceCharts({ data }: Props) {
  if (!data) {
    return <p className="text-center text-gray-400 py-8 text-sm">No device data</p>;
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      <ChartSection title="Device Types" items={data.deviceTypes} />
      <ChartSection title="Browsers" items={data.browsers} />
      <ChartSection title="Operating Systems" items={data.operatingSystems} />
    </div>
  );
}

function ChartSection({ title, items }: { title: string; items: { name: string; clicks: number; percentage: number }[] }) {
  if (items.length === 0) {
    return (
      <div>
        <h3 className="text-sm font-medium text-gray-600 mb-2">{title}</h3>
        <p className="text-xs text-gray-400">No data</p>
      </div>
    );
  }

  return (
    <div>
      <h3 className="text-sm font-medium text-gray-600 mb-3">{title}</h3>
      <div className="flex items-center gap-4">
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
                <Cell key={index} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{ borderRadius: 8, border: '1px solid #e2e8f0', fontSize: 12 }}
              formatter={(value: number) => [`${value} clicks`]}
            />
          </PieChart>
        </ResponsiveContainer>
        <div className="flex-1 space-y-1.5">
          {items.slice(0, 5).map((item, i) => (
            <div key={item.name} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span
                  className="w-2.5 h-2.5 rounded-full"
                  style={{ backgroundColor: COLORS[i % COLORS.length] }}
                />
                <span className="text-gray-700">{item.name || 'Unknown'}</span>
              </div>
              <span className="text-gray-500">{item.percentage.toFixed(1)}%</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
