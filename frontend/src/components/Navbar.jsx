import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { guestName } from '../ui/format';
import ConfirmDialog from './ConfirmDialog';

export default function Navbar() {
  const { user, isAuthenticated, isStaff, isCustomer, logout } = useAuth();
  const navigate = useNavigate();
  const [confirmLogout, setConfirmLogout] = useState(false);

  const onLogout = () => {
    logout();
    setConfirmLogout(false);
    navigate('/hotels');
  };

  return (
    <header className={`nav ${isStaff ? 'nav-staff' : ''}`}>
      <NavLink to={isStaff ? '/admin' : '/hotels'} className="brand">
        StayFinder
        {isStaff && <small>Staff</small>}
      </NavLink>
      <nav className="nav-links">
        {isStaff ? (
          <NavLink to="/admin">Front desk</NavLink>
        ) : (
          <>
            <NavLink to="/hotels" end>
              Find hotels
            </NavLink>
            {isCustomer && <NavLink to="/bookings">My bookings</NavLink>}
            {isCustomer && <NavLink to="/orders">My room service</NavLink>}
          </>
        )}
      </nav>
      <div className="nav-user">
        {isAuthenticated ? (
          <>
            <span className={`role-pill ${isStaff ? 'role-staff' : 'role-guest'}`}>
              {isStaff ? 'Hotel staff' : 'Guest'}
            </span>
            <span className="guest">{guestName(user?.email)}</span>
            <button type="button" className="btn btn-ghost" onClick={() => setConfirmLogout(true)}>
              Sign out
            </button>
          </>
        ) : (
          <>
            <NavLink to="/login">Guest sign in</NavLink>
            <NavLink to="/login?as=staff" className="muted-link">
              Hotel staff
            </NavLink>
            <NavLink to="/register" className="btn btn-primary">
              Book as guest
            </NavLink>
          </>
        )}
      </div>
      <ConfirmDialog
        open={confirmLogout}
        title="Sign out?"
        message="You can sign back in anytime."
        confirmLabel="Sign out"
        onCancel={() => setConfirmLogout(false)}
        onConfirm={onLogout}
      />
    </header>
  );
}
