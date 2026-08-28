import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiErrorMessage } from '../api/axiosClient';
import { myOrders } from '../api/foodApi';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import StatusBadge from '../components/StatusBadge';
import { formatMoney } from '../ui/format';

export default function MyOrders() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    myOrders()
      .then(setOrders)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <section>
      <div className="hero">
        <h1>My room service</h1>
        <p>Food you ordered to your own room during a confirmed stay.</p>
      </div>
      {loading && <Loading label="Loading orders…" />}
      <ErrorMessage message={error} />
      {orders.map((o) => (
        <article key={o.id} className="card stay-card">
          <div>
            <h2>Room {o.roomNumber}</h2>
            <p>
              {o.items?.map((i) => `${i.itemName} × ${i.quantity}`).join(', ') || 'Order'}
            </p>
            <p className="muted">
              {formatMoney(
                (o.items || []).reduce((sum, i) => sum + Number(i.unitPrice || 0) * Number(i.quantity || 0), 0)
              )}
            </p>
            <StatusBadge status={o.status} />
          </div>
        </article>
      ))}
      {orders.length === 0 && !loading && (
        <p className="empty">
          No room-service orders yet. Open a confirmed stay and <Link to="/hotels">choose a hotel menu</Link>.
        </p>
      )}
    </section>
  );
}
