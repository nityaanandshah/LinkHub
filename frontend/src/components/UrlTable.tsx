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
      <div className="text-center py-16 text-gray-400">
        <p className="text-lg mb-2">No URLs yet</p>
        <p className="text-sm">Create your first short URL to get started</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="border-b border-gray-200">
            <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Short URL</th>
            <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Destination</th>
            <th className="text-center py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Status</th>
            <th className="text-left py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Created</th>
            <th className="text-right py-3 px-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {urls.map((url) => (
            <tr key={url.shortCode} className="hover:bg-gray-50 transition-colors">
              <td className="py-3 px-4">
                <button
                  onClick={() => handleCopy(url.shortUrl)}
                  className="text-primary-600 hover:text-primary-700 font-medium text-sm"
                  title="Click to copy"
                >
                  {url.shortCode}
                </button>
                {url.isCustomAlias && (
                  <span className="ml-2 text-xs bg-primary-50 text-primary-600 px-1.5 py-0.5 rounded-full font-medium">
                    custom
                  </span>
                )}
              </td>
              <td className="py-3 px-4 max-w-xs">
                <a
                  href={url.longUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm text-gray-600 hover:text-gray-900 truncate block"
                  title={url.longUrl}
                >
                  {url.longUrl}
                </a>
              </td>
              <td className="py-3 px-4 text-center">
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                    url.isActive
                      ? 'bg-green-50 text-green-700'
                      : 'bg-gray-100 text-gray-500'
                  }`}
                >
                  {url.isActive ? 'Active' : 'Inactive'}
                </span>
              </td>
              <td className="py-3 px-4 text-sm text-gray-500">
                {formatDistanceToNow(new Date(url.createdAt), { addSuffix: true })}
              </td>
              <td className="py-3 px-4">
                <div className="flex items-center justify-end gap-1">
                  <button
                    onClick={() => onViewAnalytics(url.shortCode)}
                    className="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                    title="Analytics"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                    </svg>
                  </button>
                  <button
                    onClick={() => onShowQr(url)}
                    className="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                    title="QR Code"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
                    </svg>
                  </button>
                  <button
                    onClick={() => handleToggle(url.shortCode, url.isActive)}
                    className={`p-1.5 rounded-lg transition-colors ${
                      url.isActive
                        ? 'text-gray-400 hover:text-warning-500 hover:bg-yellow-50'
                        : 'text-gray-400 hover:text-success-500 hover:bg-green-50'
                    }`}
                    title={url.isActive ? 'Deactivate' : 'Activate'}
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      {url.isActive ? (
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      ) : (
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      )}
                    </svg>
                  </button>
                  <button
                    onClick={() => handleDelete(url.shortCode)}
                    className="p-1.5 text-gray-400 hover:text-danger-500 hover:bg-danger-50 rounded-lg transition-colors"
                    title="Delete"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
