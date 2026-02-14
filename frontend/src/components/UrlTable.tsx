import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';
import type { UrlItem } from '../types/api';

interface Props {
  urls: UrlItem[];
  onDelete: (shortCode: string) => Promise<void>;
  onToggle: (shortCode: string, isActive: boolean) => Promise<void>;
  onRefresh: () => void;
  onViewAnalytics: (shortCode: string) => void;
  onShowQr: (url: UrlItem) => void;
}

const S = {
  primary: '#2563EB',
  primaryHover: '#1D4ED8',
  primaryLight: '#DBEAFE',
  accent: '#F97316',
  border: '#E2E8F0',
  borderLight: '#F1F5F9',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  success: '#10B981',
  successLight: '#D1FAE5',
  warning: '#F59E0B',
  warningLight: '#FEF3C7',
  error: '#EF4444',
  errorLight: '#FEE2E2',
  hoverBg: '#F1F5F9',
} as const;

export default function UrlTable({ urls, onDelete, onToggle, onRefresh, onViewAnalytics, onShowQr }: Props) {
  const handleCopy = (shortUrl: string) => {
    navigator.clipboard.writeText(shortUrl);
    toast.success('Copied to clipboard!');
  };

  const handleDelete = async (shortCode: string) => {
    if (!confirm('Are you sure you want to delete this URL?')) return;
    try {
      await onDelete(shortCode);
      toast.success('URL deleted');
      onRefresh();
    } catch {
      toast.error('Failed to delete URL');
    }
  };

  const handleToggle = async (shortCode: string, isActive: boolean) => {
    try {
      await onToggle(shortCode, isActive);
      toast.success(isActive ? 'URL deactivated' : 'URL activated');
      onRefresh();
    } catch {
      toast.error('Failed to update URL');
    }
  };

  if (urls.length === 0) {
    return (
      <div className="text-center py-20" style={{ color: S.textMuted }}>
        <p style={{ fontSize: '1.1rem', marginBottom: '8px', fontWeight: 500 }}>No URLs yet</p>
        <p style={{ fontSize: '0.875rem' }}>Create your first short URL to get started</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr style={{ borderBottom: `1px solid ${S.border}` }}>
            {['Short URL', 'Destination', 'Status', 'Created', 'Actions'].map((h, i) => (
              <th
                key={h}
                style={{
                  textAlign: i === 2 ? 'center' : i === 4 ? 'right' : 'left',
                  padding: '14px 24px',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  color: S.textMuted,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  fontFamily: 'var(--font-body)',
                }}
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {urls.map((url, rowIdx) => (
            <tr
              key={url.shortCode}
              className="transition-colors"
              style={{
                borderBottom: rowIdx < urls.length - 1 ? `1px solid ${S.borderLight}` : 'none',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = S.hoverBg; }}
              onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'transparent'; }}
            >
              <td style={{ padding: '14px 24px' }}>
                <button
                  onClick={() => handleCopy(url.shortUrl)}
                  style={{ color: S.primary, fontWeight: 500, fontSize: '0.875rem', background: 'none', border: 'none', cursor: 'pointer' }}
                  title="Click to copy"
                  onMouseEnter={(e) => { e.currentTarget.style.color = S.primaryHover; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = S.primary; }}
                >
                  {url.shortCode}
                </button>
                {url.isCustomAlias && (
                  <span
                    style={{
                      marginLeft: '8px',
                      fontSize: '0.7rem',
                      backgroundColor: S.primaryLight,
                      color: S.primary,
                      padding: '2px 8px',
                      borderRadius: '9999px',
                      fontWeight: 500,
                    }}
                  >
                    custom
                  </span>
                )}
              </td>
              <td style={{ padding: '14px 24px', maxWidth: '280px' }}>
                <a
                  href={url.longUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="truncate block"
                  style={{ fontSize: '0.875rem', color: S.textSecondary, textDecoration: 'none' }}
                  title={url.longUrl}
                  onMouseEnter={(e) => { e.currentTarget.style.color = S.textPrimary; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = S.textSecondary; }}
                >
                  {url.longUrl}
                </a>
              </td>
              <td style={{ padding: '14px 24px', textAlign: 'center' }}>
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    padding: '3px 10px',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: 500,
                    backgroundColor: url.isActive ? S.successLight : '#F1F5F9',
                    color: url.isActive ? '#065F46' : S.textMuted,
                  }}
                >
                  {url.isActive ? 'Active' : 'Inactive'}
                </span>
              </td>
              <td style={{ padding: '14px 24px', fontSize: '0.875rem', color: S.textMuted }}>
                {formatDistanceToNow(new Date(url.createdAt), { addSuffix: true })}
              </td>
              <td style={{ padding: '14px 24px' }}>
                <div className="flex items-center justify-end gap-1">
                  {/* Analytics */}
                  <ActionBtn
                    title="Analytics"
                    hoverColor={S.primary}
                    hoverBg={S.primaryLight}
                    onClick={() => onViewAnalytics(url.shortCode)}
                  >
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v7a1 1 0 01-1 1H3a1 1 0 01-1-1v-7zm6-4a1 1 0 011-1h2a1 1 0 011 1v11a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-4a1 1 0 011-1h2a1 1 0 011 1v15a1 1 0 01-1 1h-2a1 1 0 01-1-1V3z" />
                    </svg>
                  </ActionBtn>
                  {/* QR Code */}
                  <ActionBtn
                    title="QR Code"
                    hoverColor={S.primary}
                    hoverBg={S.primaryLight}
                    onClick={() => onShowQr(url)}
                  >
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M3 4a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm2 0v3h1V4H5zm8-1a1 1 0 00-1 1v3a1 1 0 001 1h3a1 1 0 001-1V4a1 1 0 00-1-1h-3zm0 1v3h3V4h-3zM3 13a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1v-3zm2 0v3h1v-3H5zm8 0a1 1 0 011-1h1v2h-2v-1zm3 0h1v4h-4v-1h3v-3zm-3 4v1h1v-1h-1zm-1-1h-1v2h1v1h2v-1h-1v-1h-1v-1zm-5-2v2H6v-2h1z" clipRule="evenodd" />
                    </svg>
                  </ActionBtn>
                  {/* Toggle */}
                  <ActionBtn
                    title={url.isActive ? 'Deactivate' : 'Activate'}
                    hoverColor={url.isActive ? S.warning : S.success}
                    hoverBg={url.isActive ? S.warningLight : S.successLight}
                    onClick={() => handleToggle(url.shortCode, url.isActive)}
                  >
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                      {url.isActive ? (
                        <path fillRule="evenodd" d="M13.477 14.89A6 6 0 015.11 6.524l8.367 8.368zm1.414-1.414L6.524 5.11a6 6 0 018.367 8.367zM18 10a8 8 0 11-16 0 8 8 0 0116 0z" clipRule="evenodd" />
                      ) : (
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                      )}
                    </svg>
                  </ActionBtn>
                  {/* Delete */}
                  <ActionBtn
                    title="Delete"
                    hoverColor={S.error}
                    hoverBg={S.errorLight}
                    onClick={() => handleDelete(url.shortCode)}
                  >
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                  </ActionBtn>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/* ── Small action button with hover effect ── */
function ActionBtn({
  children,
  title,
  hoverColor,
  hoverBg,
  onClick,
}: {
  children: React.ReactNode;
  title: string;
  hoverColor: string;
  hoverBg: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      title={title}
      className="transition-colors"
      style={{
        padding: '6px',
        color: '#94A3B8',
        borderRadius: '8px',
        background: 'transparent',
        border: 'none',
        cursor: 'pointer',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.color = hoverColor;
        e.currentTarget.style.backgroundColor = hoverBg;
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.color = '#94A3B8';
        e.currentTarget.style.backgroundColor = 'transparent';
      }}
    >
      {children}
    </button>
  );
}
