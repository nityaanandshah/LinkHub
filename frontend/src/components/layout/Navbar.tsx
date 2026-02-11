import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!isAuthenticated) return null;

  const isActive = (path: string) =>
    location.pathname === path
      ? 'text-primary-600 border-b-2 border-primary-600'
      : 'text-gray-500 hover:text-gray-700';

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center gap-8">
            <Link to="/dashboard" className="flex items-center gap-2 text-xl font-bold text-gray-900">
              <span className="text-primary-600">&#x1f517;</span> LinkHub
            </Link>
            <div className="hidden sm:flex items-center gap-1">
              <Link
                to="/dashboard"
                className={`px-3 py-2 text-sm font-medium transition-colors ${isActive('/dashboard')}`}
              >
                Dashboard
              </Link>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-500 hidden sm:inline">
              {user?.displayName || user?.email}
            </span>
            <button
              onClick={handleLogout}
              className="px-3 py-1.5 text-sm font-medium text-danger-600 hover:bg-danger-50 rounded-lg transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}
