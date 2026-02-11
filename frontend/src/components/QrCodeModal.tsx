import { useState, useEffect } from 'react';
import Modal from './ui/Modal';
import apiClient from '../api/client';

interface Props {
  open: boolean;
  onClose: () => void;
  shortCode: string;
  shortUrl: string;
}

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
    <Modal open={open} onClose={onClose} title="QR Code" maxWidth="max-w-sm">
      <div className="flex flex-col items-center gap-4">
        {loading ? (
          <div className="w-48 h-48 bg-gray-100 rounded-lg animate-pulse" />
        ) : qrDataUrl ? (
          <img src={qrDataUrl} alt="QR Code" className="w-48 h-48 rounded-lg border border-gray-200" />
        ) : (
          <div className="w-48 h-48 bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-sm">
            Failed to load
          </div>
        )}

        <p className="text-sm text-gray-500 text-center break-all">{shortUrl}</p>

        <button
          onClick={handleDownload}
          disabled={!qrDataUrl}
          className="w-full py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-primary-300 rounded-lg transition-colors"
        >
          Download PNG
        </button>
      </div>
    </Modal>
  );
}
