import { Navigate, Route, Routes } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ProtectedRoute from '../components/ProtectedRoute';
import AdminDashboard from '../pages/AdminDashboard';
import Hotels from '../pages/Hotels';
import Login from '../pages/Login';
import Menu from '../pages/Menu';
import MyBookings from '../pages/MyBookings';
import MyOrders from '../pages/MyOrders';
import Register from '../pages/Register';
import Rooms from '../pages/Rooms';

export default function AppRoutes() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<Navigate to="/hotels" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/hotels" element={<Hotels />} />
          <Route path="/hotels/:hotelId" element={<Rooms />} />
          <Route path="/hotels/:hotelId/menu" element={<Menu />} />
          <Route
            path="/bookings"
            element={
              <ProtectedRoute guestOnly>
                <MyBookings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute guestOnly>
                <MyOrders />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute staffOnly>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
        </Routes>
      </main>
    </>
  );
}
