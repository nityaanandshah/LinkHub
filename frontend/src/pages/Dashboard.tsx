import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import AppLayout from '../components/layout/AppLayout';
import UrlTable from '../components/UrlTable';
import CreateUrlModal from '../components/CreateUrlModal';
import QrCodeModal from '../components/QrCodeModal';
import { useUrls } from '../hooks/useUrls';
import type { UrlItem } from '../types/api';

export default function Dashboard() {
  const { urls, loading, totalPages, totalElements, currentPage, fetchUrls, createUrl, deleteUrl, toggleUrl } =
    useUrls();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [qrTarget, setQrTarget] = useState<UrlItem | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchUrls(0);
  }, [fetchUrls]);

  const handleRefresh = useCallback(() => {
    fetchUrls(currentPage);
  }, [fetchUrls, currentPage]);

  return (
    <AppLayout>
      <Toaster position="top-right" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My URLs</h1>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white text-sm font-semibold rounded-lg transition-colors shadow-sm"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Create URL
        </button>
      </div>

      {/* URL Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm">
        {loading && urls.length === 0 ? (
          <div className="py-16 text-center text-gray-400">
            <div className="inline-block w-6 h-6 border-2 border-primary-300 border-t-primary-600 rounded-full animate-spin" />
            <p className="mt-3 text-sm">Loading URLs...</p>
          </div>
        ) : (
          <UrlTable
            urls={urls}
            onDelete={deleteUrl}
            onToggle={toggleUrl}
            onRefresh={handleRefresh}
            onViewAnalytics={(shortCode) => navigate(`/analytics/${shortCode}`)}
            onShowQr={(url) => setQrTarget(url)}
          />
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-6">
          <button
            onClick={() => fetchUrls(currentPage - 1)}
            disabled={currentPage === 0}
            className="px-3 py-1.5 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Previous
          </button>
          <span className="text-sm text-gray-500">
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            onClick={() => fetchUrls(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="px-3 py-1.5 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Next
          </button>
        </div>
      )}

      {/* Modals */}
      <CreateUrlModal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreate={createUrl}
        onSuccess={handleRefresh}
      />

      {qrTarget && (
        <QrCodeModal
          open={!!qrTarget}
          onClose={() => setQrTarget(null)}
          shortCode={qrTarget.shortCode}
          shortUrl={qrTarget.shortUrl}
        />
      )}
    </AppLayout>
  );
}
