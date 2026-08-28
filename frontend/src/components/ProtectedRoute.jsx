import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, staffOnly = false, guestOnly = false }) {
  const { isAuthenticated, isStaff, isCustomer } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (staffOnly && !isStaff) {
    return <Navigate to="/hotels" replace />;
  }
  if (guestOnly && !isCustomer) {
    return <Navigate to="/admin" replace />;
  }
  return children;
}
