import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div style={{ maxWidth: 1000, margin: '40px auto', padding: 24 }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32 }}>
        <h1>🔗 LinkHub Dashboard</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <span>Welcome, {user?.displayName || user?.email}</span>
          <button
            onClick={handleLogout}
            style={{
              padding: '8px 16px', background: '#ef4444', color: 'white',
              border: 'none', borderRadius: 6, cursor: 'pointer'
            }}
          >
            Logout
          </button>
        </div>
      </header>

      <div style={{
        background: 'white', borderRadius: 12, padding: 48,
        textAlign: 'center', border: '1px solid #e2e8f0'
      }}>
        <h2 style={{ color: '#64748b', marginBottom: 8 }}>Dashboard Coming in Week 4</h2>
        <p style={{ color: '#94a3b8' }}>
          URL creation, analytics, and QR code features will be added in upcoming weeks.
        </p>
      </div>
    </div>
  );
}
