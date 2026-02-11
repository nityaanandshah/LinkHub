import type { GeoStats } from '../../types/api';

interface Props {
  data: GeoStats | null;
}

export default function GeoTable({ data }: Props) {
  if (!data) {
    return <p className="text-center text-gray-400 py-8 text-sm">No geographic data</p>;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
      {/* Countries */}
      <div>
        <h3 className="text-sm font-medium text-gray-600 mb-3">Countries</h3>
        {data.countries.length === 0 ? (
          <p className="text-xs text-gray-400">No data</p>
        ) : (
          <div className="space-y-2">
            {data.countries.map((c) => (
              <div key={c.country} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-700">{c.country || 'Unknown'}</span>
                </div>
                <div className="flex items-center gap-3">
                  <div className="w-24 bg-gray-100 rounded-full h-1.5">
                    <div
                      className="bg-primary-500 h-1.5 rounded-full"
                      style={{ width: `${Math.min(c.percentage, 100)}%` }}
                    />
                  </div>
                  <span className="text-xs text-gray-500 w-16 text-right">
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
        <h3 className="text-sm font-medium text-gray-600 mb-3">Top Cities</h3>
        {data.cities.length === 0 ? (
          <p className="text-xs text-gray-400">No data</p>
        ) : (
          <div className="space-y-2">
            {data.cities.slice(0, 10).map((c) => (
              <div key={`${c.city}-${c.country}`} className="flex items-center justify-between">
                <span className="text-sm text-gray-700">
                  {c.city || 'Unknown'}{c.country ? `, ${c.country}` : ''}
                </span>
                <span className="text-xs text-gray-500">{c.clicks} clicks</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
