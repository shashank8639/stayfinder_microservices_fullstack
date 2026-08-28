import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiErrorMessage } from '../api/axiosClient';
import { checkAvailability, createBooking, listHotels, listRooms } from '../api/bookingApi';
import ConfirmDialog from '../components/ConfirmDialog';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatDate, formatMoney, roomTypeLabel } from '../ui/format';

function defaultDates() {
  const checkIn = new Date();
  checkIn.setDate(checkIn.getDate() + 14);
  const checkOut = new Date(checkIn);
  checkOut.setDate(checkOut.getDate() + 2);
  const iso = (d) => d.toISOString().slice(0, 10);
  return { checkIn: iso(checkIn), checkOut: iso(checkOut) };
}

export default function Rooms() {
  const { hotelId } = useParams();
  const { isAuthenticated, isStaff } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [hotel, setHotel] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [dates, setDates] = useState(defaultDates);
  const [availability, setAvailability] = useState({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [pendingRoom, setPendingRoom] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([listHotels(), listRooms(hotelId)])
      .then(([hotels, roomList]) => {
        setHotel(hotels.find((h) => String(h.id) === String(hotelId)) || { id: hotelId, name: 'Hotel' });
        setRooms(roomList);
      })
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [hotelId]);

  const onCheck = async (roomId) => {
    setError('');
    try {
      const result = await checkAvailability(roomId, dates.checkIn, dates.checkOut);
      setAvailability((prev) => ({ ...prev, [roomId]: result }));
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  const requestBook = (room) => {
    if (isStaff) {
      return;
    }
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/hotels/${hotelId}` } });
      return;
    }
    setPendingRoom(room);
  };

  const onConfirmBook = async () => {
    if (!pendingRoom) {
      return;
    }
    setError('');
    setBusy(true);
    try {
      await createBooking(pendingRoom.id, dates.checkIn, dates.checkOut);
      toast.success('Your room is reserved in your name. Confirm it from My bookings.');
      setPendingRoom(null);
      navigate('/bookings');
    } catch (err) {
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
      setPendingRoom(null);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section>
      <p>
        <Link to="/hotels">← All hotels</Link>
      </p>
      <h1>{hotel?.name || 'Rooms'}</h1>
      <p className="muted">
        {hotel?.city}
        {isStaff
          ? ' · Staff can check availability, but only a guest can reserve a room in their own name.'
          : ' · You are booking this room for yourself.'}
      </p>
      {isStaff && (
        <p className="staff-note">
          Front desk does not create guest bookings here. <Link to="/admin">See guest reservations</Link>.
        </p>
      )}
      <div className="dates">
        <label>
          Check-in
          <input
            type="date"
            value={dates.checkIn}
            onChange={(e) => setDates((d) => ({ ...d, checkIn: e.target.value }))}
          />
        </label>
        <label>
          Check-out
          <input
            type="date"
            value={dates.checkOut}
            onChange={(e) => setDates((d) => ({ ...d, checkOut: e.target.value }))}
          />
        </label>
      </div>
      {loading && <Loading label="Loading rooms…" />}
      <ErrorMessage message={error} />
      <div className="grid">
        {rooms.map((room) => {
          const avail = availability[room.id];
          return (
            <article key={room.id} className="card">
              <h2>Room {room.roomNumber}</h2>
              <p>
                {roomTypeLabel(room.roomType)} · {formatMoney(room.pricePerNight, hotel?.city)} / night
              </p>
              {avail && (
                <p className={avail.available ? 'ok' : 'error-banner'}>
                  {avail.available ? 'Available for your dates' : avail.message || 'Not available for these dates'}
                </p>
              )}
              <div className="row">
                <button type="button" className="btn btn-ghost" onClick={() => onCheck(room.id)}>
                  Check dates
                </button>
                {!isStaff && (
                  <button type="button" className="btn btn-primary" onClick={() => requestBook(room)}>
                    Book this room for me
                  </button>
                )}
              </div>
            </article>
          );
        })}
      </div>
      <ConfirmDialog
        open={Boolean(pendingRoom)}
        title="Book this room for yourself?"
        message={
          pendingRoom
            ? `This reservation will be under your guest account. Room ${pendingRoom.roomNumber} at ${hotel?.name} from ${formatDate(dates.checkIn)} to ${formatDate(dates.checkOut)}.`
            : ''
        }
        confirmLabel="Book for me"
        busy={busy}
        onCancel={() => setPendingRoom(null)}
        onConfirm={onConfirmBook}
      />
    </section>
  );
}
