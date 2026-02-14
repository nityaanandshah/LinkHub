import { useState, FormEvent } from 'react';
import Modal from './ui/Modal';
import toast from 'react-hot-toast';
import type { CreateUrlRequest, CreateUrlResponse } from '../types/api';

interface Props {
  open: boolean;
  onClose: () => void;
  onCreate: (req: CreateUrlRequest) => Promise<CreateUrlResponse>;
  onSuccess: () => void;
}

const S = {
  primary: '#2563EB',
  primaryHover: '#1D4ED8',
  primaryLight: '#DBEAFE',
  border: '#E2E8F0',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  btnShadow: '0px 2px 4px rgba(0,0,0,0.08)',
} as const;

export default function CreateUrlModal({ open, onClose, onCreate, onSuccess }: Props) {
  const [longUrl, setLongUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const request: CreateUrlRequest = {
        longUrl,
        customAlias: customAlias || undefined,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      };
      const result = await onCreate(request);
      toast.success(`Short URL created: ${result.shortCode}`);
      setLongUrl('');
      setCustomAlias('');
      setExpiresAt('');
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      toast.error(axiosError.response?.data?.message || 'Failed to create URL');
    } finally {
      setLoading(false);
    }
  };

  const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '10px 16px',
    border: `1px solid ${S.border}`,
    borderRadius: '8px',
    fontSize: '0.875rem',
    color: '#0F172A',
    backgroundColor: '#FFFFFF',
    outline: 'none',
    transition: 'border-color 0.15s, box-shadow 0.15s',
  };

  const handleFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    e.target.style.borderColor = S.primary;
    e.target.style.boxShadow = `0 0 0 3px ${S.primaryLight}`;
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    e.target.style.borderColor = S.border;
    e.target.style.boxShadow = 'none';
  };

  return (
    <Modal open={open} onClose={onClose} title="Create Short URL">
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, color: S.textSecondary, marginBottom: '6px' }}>
            Long URL <span style={{ color: '#EF4444' }}>*</span>
          </label>
          <input
            type="url"
            value={longUrl}
            onChange={(e) => setLongUrl(e.target.value)}
            required
            placeholder="https://example.com/very/long/path"
            style={inputStyle}
            onFocus={handleFocus}
            onBlur={handleBlur}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, color: S.textSecondary, marginBottom: '6px' }}>
            Custom Alias <span style={{ color: S.textMuted }}>(optional)</span>
          </label>
          <input
            type="text"
            value={customAlias}
            onChange={(e) => setCustomAlias(e.target.value)}
            placeholder="my-brand"
            pattern="^[a-zA-Z0-9\-_]{4,10}$"
            title="4–10 alphanumeric characters, hyphens, underscores"
            style={inputStyle}
            onFocus={handleFocus}
            onBlur={handleBlur}
          />
          <p style={{ marginTop: '6px', fontSize: '0.75rem', color: S.textMuted }}>
            4–10 chars: letters, numbers, hyphens, underscores
          </p>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, color: S.textSecondary, marginBottom: '6px' }}>
            Expires At <span style={{ color: S.textMuted }}>(optional)</span>
          </label>
          <input
            type="datetime-local"
            value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)}
            style={inputStyle}
            onFocus={handleFocus}
            onBlur={handleBlur}
          />
        </div>

        <div className="flex justify-end gap-3" style={{ paddingTop: '8px' }}>
          <button
            type="button"
            onClick={onClose}
            className="transition-colors"
            style={{
              padding: '8px 20px',
              fontSize: '0.875rem',
              fontWeight: 500,
              color: S.textSecondary,
              backgroundColor: '#F1F5F9',
              border: 'none',
              borderRadius: '10px',
              cursor: 'pointer',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#E2E8F0'; }}
            onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = '#F1F5F9'; }}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="transition-colors"
            style={{
              padding: '8px 20px',
              fontSize: '0.875rem',
              fontWeight: 600,
              color: '#FFFFFF',
              backgroundColor: loading ? '#93c5fd' : S.primary,
              border: 'none',
              borderRadius: '10px',
              cursor: loading ? 'not-allowed' : 'pointer',
              boxShadow: S.btnShadow,
            }}
            onMouseEnter={(e) => { if (!loading) e.currentTarget.style.backgroundColor = S.primaryHover; }}
            onMouseLeave={(e) => { if (!loading) e.currentTarget.style.backgroundColor = S.primary; }}
          >
            {loading ? 'Creating...' : 'Create URL'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
