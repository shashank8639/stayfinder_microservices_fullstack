import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiErrorMessage } from '../api/axiosClient';
import { cancelBooking, confirmBooking, confirmPayment, myBookings } from '../api/bookingApi';
import ConfirmDialog from '../components/ConfirmDialog';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../context/ToastContext';
import { formatDate } from '../ui/format';

export default function MyBookings() {
  const toast = useToast();
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [dialog, setDialog] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = () =>
    myBookings()
      .then(setBookings)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));

  useEffect(() => {
    load();
  }, []);

  const closeDialog = () => setDialog(null);

  const run = async () => {
    if (!dialog) {
      return;
    }
    setBusy(true);
    setError('');
    try {
      await dialog.action();
      toast.success(dialog.success);
      closeDialog();
      await load();
    } catch (err) {
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
      closeDialog();
    } finally {
      setBusy(false);
    }
  };

  return (
    <section>
      <div className="hero">
        <h1>My bookings</h1>
        <p>These are rooms you reserved for yourself. Confirm, pay, or cancel your own stay.</p>
      </div>
      {loading && <Loading label="Loading your stays…" />}
      <ErrorMessage message={error} />
      {bookings.map((b) => (
        <article key={b.id} className="card stay-card">
          <div>
            <h2>{b.hotelName}</h2>
            <p className="muted">
              Room {b.roomNumber} · {formatDate(b.checkIn)} – {formatDate(b.checkOut)}
            </p>
            <StatusBadge status={b.status} />
          </div>
          <div className="stay-actions">
            {b.status === 'PENDING' && (
              <>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() =>
                    setDialog({
                      title: 'Confirm your stay?',
                      message: `We'll hold Room ${b.roomNumber} at ${b.hotelName} in your name from ${formatDate(b.checkIn)} to ${formatDate(b.checkOut)}.`,
                      confirmLabel: 'Confirm my stay',
                      success: 'Your stay is confirmed. You can now order food to your room.',
                      action: () => confirmBooking(b.id),
                    })
                  }
                >
                  Confirm stay
                </button>
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() =>
                    setDialog({
                      title: 'Pay for your stay?',
                      message: 'This charges the demo guest account. No real card is used. Hotel staff cannot pay on your behalf here.',
                      confirmLabel: 'Pay now',
                      success: 'Payment recorded.',
                      action: () => confirmPayment(b.id),
                    })
                  }
                >
                  Pay now
                </button>
              </>
            )}
            {b.status === 'CONFIRMED' && (
              <Link
                className="btn btn-primary"
                to={`/hotels/${b.hotelId}/menu?bookingId=${b.id}&roomNumber=${encodeURIComponent(b.roomNumber)}`}
              >
                Order food
              </Link>
            )}
            {b.status !== 'CANCELLED' && (
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() =>
                  setDialog({
                    title: 'Cancel this stay?',
                    message: `Room ${b.roomNumber} at ${b.hotelName} will be released.`,
                    confirmLabel: 'Cancel stay',
                    danger: true,
                    success: 'Stay cancelled.',
                    action: () => cancelBooking(b.id),
                  })
                }
              >
                Cancel
              </button>
            )}
          </div>
        </article>
      ))}
      {bookings.length === 0 && !loading && (
        <p className="empty">
          No bookings yet. <Link to="/hotels">Find a hotel</Link> and book a room for yourself.
        </p>
      )}
      <ConfirmDialog
        open={Boolean(dialog)}
        title={dialog?.title}
        message={dialog?.message}
        confirmLabel={dialog?.confirmLabel}
        danger={dialog?.danger}
        busy={busy}
        onCancel={closeDialog}
        onConfirm={run}
      />
    </section>
  );
}
