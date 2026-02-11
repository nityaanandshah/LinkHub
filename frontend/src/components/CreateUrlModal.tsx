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

  return (
    <Modal open={open} onClose={onClose} title="Create Short URL">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Long URL <span className="text-danger-500">*</span>
          </label>
          <input
            type="url"
            value={longUrl}
            onChange={(e) => setLongUrl(e.target.value)}
            required
            placeholder="https://example.com/very/long/path"
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none text-sm"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Custom Alias <span className="text-gray-400">(optional)</span>
          </label>
          <input
            type="text"
            value={customAlias}
            onChange={(e) => setCustomAlias(e.target.value)}
            placeholder="my-brand"
            pattern="^[a-zA-Z0-9\-_]{4,10}$"
            title="4–10 alphanumeric characters, hyphens, underscores"
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none text-sm"
          />
          <p className="mt-1 text-xs text-gray-400">4–10 chars: letters, numbers, hyphens, underscores</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Expires At <span className="text-gray-400">(optional)</span>
          </label>
          <input
            type="datetime-local"
            value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none text-sm"
          />
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-primary-300 rounded-lg transition-colors"
          >
            {loading ? 'Creating...' : 'Create URL'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
