import { useState, useEffect } from 'react';
import Modal from './ui/Modal';
import apiClient from '../api/client';

interface Props {
  open: boolean;
  onClose: () => void;
  shortCode: string;
  shortUrl: string;
}

const S = {
  primary: '#2563EB',
  primaryHover: '#1D4ED8',
  textMuted: '#94A3B8',
  btnShadow: '0px 2px 4px rgba(0,0,0,0.08)',
} as const;

export default function QrCodeModal({ open, onClose, shortCode, shortUrl }: Props) {
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open || !shortCode) return;
    setLoading(true);
    apiClient
      .get(`/urls/${shortCode}/qr`, { responseType: 'arraybuffer', params: { size: 400 } })
      .then((res) => {
        const blob = new Blob([res.data], { type: 'image/png' });
        setQrDataUrl(URL.createObjectURL(blob));
      })
      .catch(() => setQrDataUrl(null))
      .finally(() => setLoading(false));
  }, [open, shortCode]);

  const handleDownload = () => {
    if (!qrDataUrl) return;
    const a = document.createElement('a');
    a.href = qrDataUrl;
    a.download = `${shortCode}-qr.png`;
    a.click();
  };

  return (
    <Modal open={open} onClose={onClose} title="QR Code" maxWidth="380px">
      <div className="flex flex-col items-center" style={{ gap: '20px' }}>
        {loading ? (
          <div
            style={{
              width: '192px',
              height: '192px',
              backgroundColor: '#F1F5F9',
              borderRadius: '8px',
            }}
            className="animate-pulse"
          />
        ) : qrDataUrl ? (
          <img
            src={qrDataUrl}
            alt="QR Code"
            style={{
              width: '192px',
              height: '192px',
              borderRadius: '8px',
              border: '1px solid #E2E8F0',
            }}
          />
        ) : (
          <div
            style={{
              width: '192px',
              height: '192px',
              backgroundColor: '#F1F5F9',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: S.textMuted,
              fontSize: '0.875rem',
            }}
          >
            Failed to load
          </div>
        )}

        <p style={{ fontSize: '0.875rem', color: S.textMuted, textAlign: 'center', wordBreak: 'break-all' }}>
          {shortUrl}
        </p>

        <button
          onClick={handleDownload}
          disabled={!qrDataUrl}
          className="transition-colors"
          style={{
            width: '100%',
            padding: '12px',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: '#FFFFFF',
            backgroundColor: !qrDataUrl ? '#93c5fd' : S.primary,
            border: 'none',
            borderRadius: '10px',
            cursor: !qrDataUrl ? 'not-allowed' : 'pointer',
            boxShadow: S.btnShadow,
          }}
          onMouseEnter={(e) => { if (qrDataUrl) e.currentTarget.style.backgroundColor = S.primaryHover; }}
          onMouseLeave={(e) => { if (qrDataUrl) e.currentTarget.style.backgroundColor = S.primary; }}
        >
          Download PNG
        </button>
      </div>
    </Modal>
  );
}
