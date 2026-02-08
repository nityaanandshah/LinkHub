import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Login() {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, register } = useAuth();
  const navigate = useNavigate();

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

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: 24 }}>
      <h1 style={{ textAlign: 'center', marginBottom: 8 }}>🔗 LinkHub</h1>
      <p style={{ textAlign: 'center', color: '#64748b', marginBottom: 32 }}>
        Production-grade URL Shortener
      </p>

      <h2>{isRegister ? 'Create Account' : 'Sign In'}</h2>

      {error && (
        <div style={{ color: '#ef4444', background: '#fef2f2', padding: 12, borderRadius: 8, marginTop: 16 }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ marginTop: 24 }}>
        {isRegister && (
          <div style={{ marginBottom: 16 }}>
            <label>Display Name</label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              style={{ width: '100%', padding: 10, border: '1px solid #d1d5db', borderRadius: 6, marginTop: 4 }}
            />
          </div>
        )}

        <div style={{ marginBottom: 16 }}>
          <label>Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            style={{ width: '100%', padding: 10, border: '1px solid #d1d5db', borderRadius: 6, marginTop: 4 }}
          />
        </div>

        <div style={{ marginBottom: 24 }}>
          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            style={{ width: '100%', padding: 10, border: '1px solid #d1d5db', borderRadius: 6, marginTop: 4 }}
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            width: '100%', padding: 12, background: '#3b82f6', color: 'white',
            border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: 16, fontWeight: 600
          }}
        >
          {loading ? 'Loading...' : isRegister ? 'Register' : 'Sign In'}
        </button>
      </form>

      <p style={{ textAlign: 'center', marginTop: 24 }}>
        {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
        <button
          onClick={() => setIsRegister(!isRegister)}
          style={{ background: 'none', border: 'none', color: '#3b82f6', cursor: 'pointer', fontWeight: 600 }}
        >
          {isRegister ? 'Sign In' : 'Register'}
        </button>
      </p>
    </div>
  );
}
