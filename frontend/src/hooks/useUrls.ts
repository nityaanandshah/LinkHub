import { useState, useCallback } from 'react';
import apiClient from '../api/client';

// Placeholder hook for URL operations — will be fully implemented in Week 2+4
export interface UrlItem {
  shortCode: string;
  shortUrl: string;
  longUrl: string;
  createdAt: string;
  expiresAt: string | null;
  clickCount: number;
  isActive: boolean;
}

export function useUrls() {
  const [urls, setUrls] = useState<UrlItem[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchUrls = useCallback(async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<{ content: UrlItem[] }>('/urls');
      setUrls(response.data.content);
    } catch {
      // Handle error
    } finally {
      setLoading(false);
    }
  }, []);

  return { urls, loading, fetchUrls };
}
