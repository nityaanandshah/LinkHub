import { useState, useCallback } from 'react';
import apiClient from '../api/client';
import type { UrlItem, CreateUrlRequest, CreateUrlResponse, PageResponse } from '../types/api';

export function useUrls() {
  const [urls, setUrls] = useState<UrlItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);

  const fetchUrls = useCallback(async (page = 0, size = 10) => {
    setLoading(true);
    try {
      const response = await apiClient.get<PageResponse<UrlItem>>('/urls', {
        params: { page, size },
      });
      setUrls(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
      setCurrentPage(response.data.number);
    } catch {
      // Error handled by interceptor
    } finally {
      setLoading(false);
    }
  }, []);

  const createUrl = useCallback(async (request: CreateUrlRequest): Promise<CreateUrlResponse> => {
    const response = await apiClient.post<CreateUrlResponse>('/urls', request);
    return response.data;
  }, []);

  const deleteUrl = useCallback(async (shortCode: string) => {
    await apiClient.delete(`/urls/${shortCode}`);
  }, []);

  const toggleUrl = useCallback(async (shortCode: string, isActive: boolean) => {
    await apiClient.patch(`/urls/${shortCode}`, { isActive: !isActive });
  }, []);

  return {
    urls,
    loading,
    totalPages,
    totalElements,
    currentPage,
    fetchUrls,
    createUrl,
    deleteUrl,
    toggleUrl,
  };
}
