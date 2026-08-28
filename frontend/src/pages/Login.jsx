import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { apiErrorMessage } from '../api/axiosClient';
import ErrorMessage from '../components/ErrorMessage';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const DEMO = {
  guest: { email: 'customer@stayfinder.local', password: 'Customer@123' },
  staff: { email: 'admin@stayfinder.local', password: 'Admin@123' },
};

export default function Login() {
  const { login, isAuthenticated, isStaff } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [mode, setMode] = useState(params.get('as') === 'staff' ? 'staff' : 'guest');
  const [email, setEmail] = useState(mode === 'staff' ? DEMO.staff.email : DEMO.guest.email);
  const [password, setPassword] = useState(mode === 'staff' ? DEMO.staff.password : DEMO.guest.password);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (isAuthenticated) {
    return <Navigate to={isStaff ? '/admin' : location.state?.from || '/hotels'} replace />;
  }

  const switchMode = (next) => {
    setMode(next);
    setError('');
    setEmail(DEMO[next].email);
    setPassword(DEMO[next].password);
  };

  const signIn = async (nextEmail, nextPassword) => {
    setError('');
    setBusy(true);
    try {
      const user = await login(nextEmail, nextPassword);
      const staff = (user?.roles || []).includes('ADMIN') || (user?.roles || []).includes('HOTEL_OWNER');
      if (staff) {
        toast.success('Signed in at the front desk. Add rooms and menu, or book a guest and take their food order.');
        navigate('/admin');
      } else {
        toast.success('Signed in as a guest. You are booking rooms for yourself.');
        navigate(location.state?.from || '/hotels');
      }
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    await signIn(email, password);
  };

  return (
    <section className="card auth-card">
      <h1>{mode === 'staff' ? 'Hotel staff' : 'Guest sign in'}</h1>
      <p className="muted">
        {mode === 'staff'
          ? 'Front desk. First add rooms and menu. Then switch to book a walk-in guest and take their food order.'
          : 'You are booking a room in your own name — this is not a hotel receptionist desk.'}
      </p>
      <div className="mode-tabs">
        <button type="button" className={`btn ${mode === 'guest' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => switchMode('guest')}>
          I am a guest
        </button>
        <button type="button" className={`btn ${mode === 'staff' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => switchMode('staff')}>
          I work at the hotel
        </button>
      </div>
      <form onSubmit={onSubmit} className="form">
        <label>
          Email
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" autoComplete="username" required />
        </label>
        <label>
          Password
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            autoComplete="current-password"
            required
          />
        </label>
        <ErrorMessage message={error} />
        <button className="btn btn-primary" type="submit" disabled={busy}>
          {busy ? 'Signing in…' : mode === 'staff' ? 'Open front desk' : 'Sign in to book'}
        </button>
      </form>
      <button
        type="button"
        className="btn btn-ghost"
        disabled={busy}
        onClick={() => signIn(DEMO[mode].email, DEMO[mode].password)}
      >
        {mode === 'staff' ? 'Try the staff demo' : 'Try the guest demo'}
      </button>
      {mode === 'guest' && (
        <p className="muted">
          New guest? <Link to="/register">Create a guest account</Link>
        </p>
      )}
    </section>
  );
}
