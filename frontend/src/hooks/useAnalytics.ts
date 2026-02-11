import { useState, useCallback } from 'react';
import apiClient from '../api/client';
import type {
  ClickStats,
  TimeseriesPoint,
  ReferrerStats,
  DeviceStats,
  GeoStats,
} from '../types/api';

export function useAnalytics(shortCode: string) {
  const [summary, setSummary] = useState<ClickStats | null>(null);
  const [timeseries, setTimeseries] = useState<TimeseriesPoint[]>([]);
  const [referrers, setReferrers] = useState<ReferrerStats[]>([]);
  const [devices, setDevices] = useState<DeviceStats | null>(null);
  const [geo, setGeo] = useState<GeoStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = useCallback(async (days?: number) => {
    if (!shortCode) return;
    setLoading(true);
    setError(null);

    const params = days ? { days } : {};

    try {
      const [summaryRes, tsRes, refRes, devRes, geoRes] = await Promise.all([
        apiClient.get<ClickStats>(`/analytics/${shortCode}/summary`, { params }),
        apiClient.get<TimeseriesPoint[]>(`/analytics/${shortCode}/timeseries`, { params }),
        apiClient.get<ReferrerStats[]>(`/analytics/${shortCode}/referrers`, { params }),
        apiClient.get<DeviceStats>(`/analytics/${shortCode}/devices`, { params }),
        apiClient.get<GeoStats>(`/analytics/${shortCode}/geo`, { params }),
      ]);

      setSummary(summaryRes.data);
      setTimeseries(tsRes.data);
      setReferrers(refRes.data);
      setDevices(devRes.data);
      setGeo(geoRes.data);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Failed to load analytics');
    } finally {
      setLoading(false);
    }
  }, [shortCode]);

  return { summary, timeseries, referrers, devices, geo, loading, error, fetchAll };
}
