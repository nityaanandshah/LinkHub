import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import AppLayout from '../components/layout/AppLayout';
import UrlTable from '../components/UrlTable';
import CreateUrlModal from '../components/CreateUrlModal';
import QrCodeModal from '../components/QrCodeModal';
import { useUrls } from '../hooks/useUrls';
import type { UrlItem } from '../types/api';

const S = {
  cardShadow: '0px 1px 2px rgba(0,0,0,0.05), 0px 4px 8px rgba(0,0,0,0.04)',
  btnShadow: '0px 2px 4px rgba(0,0,0,0.08)',
  border: '#E2E8F0',
  accent: '#F97316',
  accentHover: '#EA580C',
  textPrimary: '#0F172A',
  textMuted: '#94A3B8',
} as const;

export default function Dashboard() {
  const { urls, loading, totalPages, currentPage, fetchUrls, createUrl, deleteUrl, toggleUrl } =
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6 mb-10">
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: S.textPrimary, margin: 0 }}>
          My URLs
        </h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2.5 transition-colors"
          style={{
            padding: '10px 20px',
            backgroundColor: S.accent,
            color: '#FFFFFF',
            fontSize: '0.875rem',
            fontWeight: 600,
            borderRadius: '10px',
            border: 'none',
            cursor: 'pointer',
            boxShadow: S.btnShadow,
          }}
          onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = S.accentHover; }}
          onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = S.accent; }}
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" />
          </svg>
          Create URL
        </button>
      </div>

      {/* URL Table */}
      <div
        style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: `1px solid ${S.border}`,
          boxShadow: S.cardShadow,
        }}
      >
        {loading && urls.length === 0 ? (
          <div className="py-20 text-center" style={{ color: S.textMuted }}>
            <div
              className="inline-block w-6 h-6 rounded-full animate-spin"
              style={{ border: '2px solid #DBEAFE', borderTopColor: '#2563EB' }}
            />
            <p className="mt-4 text-sm">Loading URLs...</p>
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
        <div className="flex items-center justify-center gap-3 mt-8">
          <button
            onClick={() => fetchUrls(currentPage - 1)}
            disabled={currentPage === 0}
            className="transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            style={{
              padding: '8px 16px',
              fontSize: '0.875rem',
              fontWeight: 500,
              color: '#475569',
              backgroundColor: '#FFFFFF',
              border: `1px solid ${S.border}`,
              borderRadius: '10px',
              cursor: currentPage === 0 ? 'not-allowed' : 'pointer',
              boxShadow: S.btnShadow,
            }}
          >
            Previous
          </button>
          <span style={{ fontSize: '0.875rem', color: S.textMuted, padding: '0 8px' }}>
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            onClick={() => fetchUrls(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            style={{
              padding: '8px 16px',
              fontSize: '0.875rem',
              fontWeight: 500,
              color: '#475569',
              backgroundColor: '#FFFFFF',
              border: `1px solid ${S.border}`,
              borderRadius: '10px',
              cursor: currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer',
              boxShadow: S.btnShadow,
            }}
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
