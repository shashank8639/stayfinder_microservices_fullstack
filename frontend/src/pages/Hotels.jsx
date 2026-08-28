import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiErrorMessage } from '../api/axiosClient';
import { listHotels } from '../api/bookingApi';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import { useAuth } from '../context/AuthContext';
import { hotelCover } from '../ui/format';

export default function Hotels() {
  const { isStaff } = useAuth();
  const [hotels, setHotels] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listHotels()
      .then(setHotels)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <section>
      {isStaff && (
        <p className="staff-note">
          You are signed in as hotel staff. Use Front desk: (1) add rooms and menu, or (2) book a guest and take
          their food order. <Link to="/admin">Open front desk</Link>
        </p>
      )}
      <div className="hero">
        <h1>Book a stay for yourself</h1>
        <p>
          This is the guest website. Choose a hotel, pick dates, and reserve a room in your name. Hotel staff do not
          book rooms for you here.
        </p>
      </div>
      {loading && <Loading label="Finding hotels…" />}
      <ErrorMessage message={error} />
      {!loading && hotels.length === 0 && !error && (
        <p className="empty">No hotels are listed right now. Please check back shortly.</p>
      )}
      <div className="grid">
        {hotels.map((hotel) => (
          <article key={hotel.id} className="card hotel-card">
            <div className="hotel-cover" style={{ backgroundImage: `url(${hotelCover(hotel.city)})` }} />
            <div className="hotel-body">
              <h2>{hotel.name}</h2>
              <p className="muted">{hotel.city}</p>
              <p>{hotel.description}</p>
              <div className="row">
                <Link className="btn btn-primary" to={`/hotels/${hotel.id}`}>
                  {isStaff ? 'View rooms' : 'Book a room'}
                </Link>
                <Link className="btn btn-ghost" to={`/hotels/${hotel.id}/menu`}>
                  Dining menu
                </Link>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
