import { useState, useEffect } from 'react';
import apiClient from '../api/client';

interface LagResponse {
  lag: number;
  delayed: boolean;
  message: string;
}

export function useAnalyticsLag() {
  const [lagInfo, setLagInfo] = useState<LagResponse | null>(null);

  useEffect(() => {
    apiClient
      .get<LagResponse>('/system/analytics-lag')
      .then((res) => setLagInfo(res.data))
      .catch(() => setLagInfo(null));
  }, []);

  return lagInfo;
}
