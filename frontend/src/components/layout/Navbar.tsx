import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

const NAV_BG = '#334155'; // dark slate from the poster header

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!isAuthenticated) return null;

  return (
    <nav
      className="sticky top-0 z-50"
      style={{
        backgroundColor: NAV_BG,
        boxShadow: '0px 2px 8px rgba(0,0,0,0.15)',
      }}
    >
      <div className="max-w-7xl mx-auto px-6 sm:px-8 lg:px-10">
        <div className="flex justify-between h-14 items-center">
          <Link to="/dashboard" className="flex items-center" style={{ textDecoration: 'none' }}>
            <span
              style={{
                fontFamily: 'var(--font-heading)',
                fontWeight: 700,
                fontSize: '1.25rem',
                color: '#FFFFFF',
              }}
            >
              LinkHub
            </span>
          </Link>
          <div className="flex items-center gap-5">
            <span
              className="hidden sm:inline"
              style={{ fontSize: '0.875rem', color: '#CBD5E1', fontWeight: 400 }}
            >
              {user?.displayName || user?.email}
            </span>
            <button
              onClick={handleLogout}
              className="transition-colors"
              style={{
                padding: '6px 16px',
                fontSize: '0.875rem',
                fontWeight: 500,
                color: '#FCA5A5',
                borderRadius: '10px',
                backgroundColor: 'transparent',
                border: 'none',
                cursor: 'pointer',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = 'rgba(239,68,68,0.15)'; e.currentTarget.style.color = '#FCA5A5'; }}
              onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'transparent'; }}
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}
