import { ReactNode, useEffect } from 'react';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  maxWidth?: string;
}

export default function Modal({ open, onClose, title, children, maxWidth = '480px' }: ModalProps) {
  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (open) document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0"
        style={{ backgroundColor: 'rgba(15, 23, 42, 0.4)', backdropFilter: 'blur(4px)' }}
        onClick={onClose}
      />
      {/* Panel */}
      <div
        className="relative w-full mx-4"
        style={{
          maxWidth,
          maxHeight: '90vh',
          overflowY: 'auto',
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          border: '1px solid #E2E8F0',
          boxShadow: '0px 4px 12px rgba(0,0,0,0.08), 0px 1px 3px rgba(0,0,0,0.06)',
        }}
      >
        <div
          className="flex items-center justify-between"
          style={{ padding: '18px 28px', borderBottom: '1px solid #F1F5F9' }}
        >
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#0F172A', margin: 0 }}>{title}</h3>
          <button
            onClick={onClose}
            className="transition-colors"
            style={{
              color: '#94A3B8',
              fontSize: '1.25rem',
              lineHeight: 1,
              padding: '4px 8px',
              borderRadius: '6px',
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.color = '#475569'; e.currentTarget.style.backgroundColor = '#F1F5F9'; }}
            onMouseLeave={(e) => { e.currentTarget.style.color = '#94A3B8'; e.currentTarget.style.backgroundColor = 'transparent'; }}
          >
            &times;
          </button>
        </div>
        <div style={{ padding: '24px 28px' }}>{children}</div>
      </div>
    </div>
  );
}
