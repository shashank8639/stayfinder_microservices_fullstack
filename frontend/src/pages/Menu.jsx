import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { apiErrorMessage } from '../api/axiosClient';
import { listHotels, myBookings } from '../api/bookingApi';
import { hotelMenu, placeOrder } from '../api/foodApi';
import ConfirmDialog from '../components/ConfirmDialog';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatDate, formatMoney } from '../ui/format';

export default function Menu() {
  const { hotelId } = useParams();
  const [params] = useSearchParams();
  const { isAuthenticated, isStaff, isCustomer } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const submitting = useRef(false);
  const [items, setItems] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [qty, setQty] = useState({});
  const [bookingId, setBookingId] = useState(params.get('bookingId') || '');
  const [roomNumber, setRoomNumber] = useState(params.get('roomNumber') || '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [hotelCity, setHotelCity] = useState('');

  useEffect(() => {
    Promise.all([hotelMenu(hotelId), listHotels()])
      .then(([menu, hotels]) => {
        setItems(menu);
        const match = hotels.find((h) => String(h.id) === String(hotelId));
        if (match) {
          setHotelCity(match.city);
        }
      })
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [hotelId]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    myBookings()
      .then((list) => {
        const confirmed = list.filter((b) => b.status === 'CONFIRMED' && String(b.hotelId) === String(hotelId));
        setBookings(confirmed);
        if (!bookingId && confirmed[0]) {
          setBookingId(String(confirmed[0].id));
          setRoomNumber(confirmed[0].roomNumber);
        }
      })
      .catch(() => {});
  }, [isAuthenticated, hotelId]);

  const selected = items.filter((item) => (qty[item.id] || 0) > 0);
  const total = selected.reduce((sum, item) => sum + Number(item.price) * qty[item.id], 0);

  const changeQty = (id, next) => {
    setQty((prev) => ({ ...prev, [id]: Math.max(0, next) }));
  };

  const requestOrder = () => {
    if (busy || submitting.current) {
      return;
    }
    setError('');
    if (!bookingId || !roomNumber) {
      setError('Choose a confirmed stay at this hotel first.');
      return;
    }
    if (selected.length === 0) {
      setError('Add at least one dish to your tray.');
      return;
    }
    setConfirmOpen(true);
  };

  const onOrder = async () => {
    if (submitting.current) {
      return;
    }
    submitting.current = true;
    setBusy(true);
    setError('');
    try {
      await placeOrder({
        hotelId: Number(hotelId),
        bookingId: Number(bookingId),
        roomNumber,
        items: selected.map((item) => ({ menuItemId: item.id, quantity: qty[item.id] })),
      });
      toast.success('Payment received. Your order is on the way.');
      setConfirmOpen(false);
      navigate('/orders', { replace: true });
    } catch (err) {
      submitting.current = false;
      setBusy(false);
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
      setConfirmOpen(false);
    }
  };

  const summary = useMemo(
    () =>
      selected.map((item) => `${item.name} × ${qty[item.id]}`).join(', ') +
      (selected.length ? ` · ${formatMoney(total, hotelCity)}` : ''),
    [selected, qty, total, hotelCity]
  );

  return (
    <section>
      <p>
        <Link to="/hotels">← All hotels</Link>
      </p>
      <div className="hero">
        <h1>In-room dining</h1>
        <p>
          {isStaff
            ? 'Kitchen menu for this hotel. Guests order food to their own room after they confirm a stay.'
            : 'Order food to your own room after you have confirmed a stay. This is not a receptionist taking an order at the desk.'}
        </p>
      </div>
      {loading && <Loading label="Loading menu…" />}
      <ErrorMessage message={error} />
      {!loading && items.length === 0 && !error && <p className="empty">This kitchen has not published a menu yet.</p>}
      <div className="grid">
        {items.map((item) => (
          <article key={item.id} className="card">
            <h2>{item.name}</h2>
            <p>{formatMoney(item.price, hotelCity)}{item.available ? '' : ' · currently unavailable'}</p>
            {item.available && (
              <div className="qty">
                <button type="button" onClick={() => changeQty(item.id, (qty[item.id] || 0) - 1)} aria-label="Decrease" disabled={busy}>
                  −
                </button>
                <span>{qty[item.id] || 0}</span>
                <button type="button" onClick={() => changeQty(item.id, (qty[item.id] || 0) + 1)} aria-label="Increase" disabled={busy}>
                  +
                </button>
              </div>
            )}
          </article>
        ))}
      </div>
      {isCustomer ? (
        <div className="order-bar">
          <label>
            Deliver to stay
            <select
              value={bookingId}
              disabled={busy}
              onChange={(e) => {
                const id = e.target.value;
                setBookingId(id);
                const match = bookings.find((b) => String(b.id) === id);
                if (match) {
                  setRoomNumber(match.roomNumber);
                }
              }}
            >
              <option value="">Select a confirmed stay</option>
              {bookings.map((b) => (
                <option key={b.id} value={b.id}>
                  Room {b.roomNumber} · {formatDate(b.checkIn)} – {formatDate(b.checkOut)}
                </option>
              ))}
            </select>
          </label>
          <p className="muted">{selected.length ? summary : 'Your tray is empty.'}</p>
          <button className="btn btn-primary" type="button" disabled={busy || selected.length === 0} onClick={requestOrder}>
            {busy ? 'Placing order…' : 'Pay and place order'}
          </button>
        </div>
      ) : isStaff ? (
        <p className="staff-note">
          Staff do not place guest food orders here. <Link to="/admin">Open front desk</Link> to see kitchen tickets.
        </p>
      ) : (
        <p className="muted">
          <Link to="/login">Guest sign in</Link> to send an order to your room.
        </p>
      )}
      <ConfirmDialog
        open={confirmOpen}
        title="Pay and send this to your room?"
        message={`This order is for your stay, room ${roomNumber}. ${summary}. Demo payment — no real card is charged.`}
        confirmLabel="Pay and send"
        busy={busy}
        onCancel={() => !busy && setConfirmOpen(false)}
        onConfirm={onOrder}
      />
    </section>
  );
}
