import { useState, FormEvent, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/* ── Palette constants ── */
const palette = {
  primary: '#2563EB',
  primaryHover: '#1D4ED8',
  primaryLight: '#DBEAFE',
  accent: '#F97316',
  bg: '#F8FAFC',
  card: '#FFFFFF',
  border: '#E2E8F0',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  errorBg: '#FEE2E2',
  error: '#EF4444',
  cardShadow: '0px 1px 2px rgba(0,0,0,0.05), 0px 4px 8px rgba(0,0,0,0.04)',
  btnShadow: '0px 2px 4px rgba(0,0,0,0.08)',
};

export default function Login() {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, register, handleOAuthCallback } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Handle OAuth2 callback tokens in URL
  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');
    if (accessToken && refreshToken) {
      handleOAuthCallback(accessToken, refreshToken);
      navigate('/dashboard', { replace: true });
    }
  }, [searchParams, handleOAuthCallback, navigate]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (isRegister) {
        await register(email, password, displayName || undefined);
      } else {
        await login(email, password);
      }
      navigate('/dashboard');
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = '/oauth2/authorization/google';
  };

  const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '10px 16px',
    border: `1px solid ${palette.border}`,
    borderRadius: '8px',
    fontSize: '0.875rem',
    color: palette.textPrimary,
    backgroundColor: palette.card,
    outline: 'none',
    transition: 'border-color 0.15s, box-shadow 0.15s',
  };

  const handleInputFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    e.target.style.borderColor = palette.primary;
    e.target.style.boxShadow = `0 0 0 3px ${palette.primaryLight}`;
  };

  const handleInputBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    e.target.style.borderColor = palette.border;
    e.target.style.boxShadow = 'none';
  };

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4 py-12"
      style={{ backgroundColor: palette.bg }}
    >
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-10">
          <h1 style={{ fontSize: '2.25rem', fontWeight: 700, color: palette.textPrimary, marginBottom: '8px' }}>
            <span style={{ color: palette.accent }}>&#x1f517;</span> LinkHub
          </h1>
          <p style={{ color: palette.textSecondary, fontSize: '1.05rem', fontWeight: 400 }}>
            Production-grade URL Shortener
          </p>
        </div>

        {/* Card */}
        <div
          style={{
            backgroundColor: palette.card,
            borderRadius: '12px',
            border: `1px solid ${palette.border}`,
            boxShadow: palette.cardShadow,
            padding: '40px',
          }}
        >
          <h2 style={{ fontSize: '1.5rem', fontWeight: 600, color: palette.textPrimary, marginBottom: '28px' }}>
            {isRegister ? 'Create Account' : 'Welcome Back'}
          </h2>

          {error && (
            <div
              style={{
                marginBottom: '20px',
                padding: '12px 16px',
                backgroundColor: palette.errorBg,
                border: `1px solid ${palette.error}33`,
                color: palette.error,
                fontSize: '0.875rem',
                borderRadius: '8px',
              }}
            >
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {isRegister && (
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, color: palette.textSecondary, marginBottom: '6px' }}>
                  Display Name
                </label>
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  style={inputStyle}
                  placeholder="John Doe"
                  onFocus={handleInputFocus}
                  onBlur={handleInputBlur}
                />
              </div>
            )}

            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, color: palette.textSecondary, marginBottom: '6px' }}>
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                style={inputStyle}
                placeholder="you@example.com"
                onFocus={handleInputFocus}
                onBlur={handleInputBlur}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, color: palette.textSecondary, marginBottom: '6px' }}>
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={8}
                style={inputStyle}
                placeholder="Min. 8 characters"
                onFocus={handleInputFocus}
                onBlur={handleInputBlur}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="transition-colors"
              style={{
                width: '100%',
                padding: '12px',
                backgroundColor: loading ? '#93c5fd' : palette.primary,
                color: '#FFFFFF',
                fontWeight: 600,
                fontSize: '0.875rem',
                borderRadius: '10px',
                border: 'none',
                cursor: loading ? 'not-allowed' : 'pointer',
                boxShadow: palette.btnShadow,
              }}
              onMouseEnter={(e) => { if (!loading) e.currentTarget.style.backgroundColor = palette.primaryHover; }}
              onMouseLeave={(e) => { if (!loading) e.currentTarget.style.backgroundColor = palette.primary; }}
            >
              {loading ? 'Loading...' : isRegister ? 'Create Account' : 'Sign In'}
            </button>
          </form>

          {/* Divider */}
          <div style={{ position: 'relative', margin: '28px 0' }}>
            <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center' }}>
              <div style={{ width: '100%', borderTop: `1px solid ${palette.border}` }} />
            </div>
            <div style={{ position: 'relative', display: 'flex', justifyContent: 'center' }}>
              <span style={{ padding: '0 12px', backgroundColor: palette.card, color: palette.textMuted, fontSize: '0.875rem' }}>
                or continue with
              </span>
            </div>
          </div>

          {/* Google OAuth */}
          <button
            onClick={handleGoogleLogin}
            className="transition-colors"
            style={{
              width: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '12px',
              padding: '12px',
              border: `1px solid ${palette.border}`,
              borderRadius: '10px',
              backgroundColor: palette.card,
              fontSize: '0.875rem',
              fontWeight: 500,
              color: palette.textPrimary,
              cursor: 'pointer',
              boxShadow: palette.btnShadow,
            }}
            onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#F1F5F9'; }}
            onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = palette.card; }}
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            Continue with Google
          </button>

          {/* Toggle */}
          <p style={{ marginTop: '28px', textAlign: 'center', fontSize: '0.875rem', color: palette.textMuted }}>
            {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
            <button
              onClick={() => { setIsRegister(!isRegister); setError(''); }}
              style={{ color: palette.primary, fontWeight: 600, background: 'none', border: 'none', cursor: 'pointer' }}
              onMouseEnter={(e) => { e.currentTarget.style.color = palette.primaryHover; }}
              onMouseLeave={(e) => { e.currentTarget.style.color = palette.primary; }}
            >
              {isRegister ? 'Sign In' : 'Create Account'}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
