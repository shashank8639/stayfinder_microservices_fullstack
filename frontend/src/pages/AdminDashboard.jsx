import { useEffect, useMemo, useRef, useState } from 'react';
import { apiErrorMessage } from '../api/axiosClient';
import {
  checkAvailability,
  confirmBooking,
  createBooking,
  createRoom,
  hotelBookings,
  listHotels,
  listRooms,
} from '../api/bookingApi';
import { createMenuItem, deleteMenuItem, hotelMenu, hotelOrders, placeOrder, updateOrderStatus } from '../api/foodApi';
import ConfirmDialog from '../components/ConfirmDialog';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../context/ToastContext';
import { formatDate, formatMoney, roomTypeLabel } from '../ui/format';

function defaultDates() {
  const checkIn = new Date();
  const checkOut = new Date();
  checkOut.setDate(checkOut.getDate() + 1);
  const iso = (d) => d.toISOString().slice(0, 10);
  return { checkIn: iso(checkIn), checkOut: iso(checkOut) };
}

export default function AdminDashboard() {
  const toast = useToast();
  const submitting = useRef(false);
  const [view, setView] = useState('setup');
  const [hotels, setHotels] = useState([]);
  const [hotelId, setHotelId] = useState('');
  const [rooms, setRooms] = useState([]);
  const [menu, setMenu] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const [roomForm, setRoomForm] = useState({ roomNumber: '', roomType: 'DELUXE', pricePerNight: '120' });
  const [menuForm, setMenuForm] = useState({ name: '', price: '8' });
  const [deleteItem, setDeleteItem] = useState(null);

  const [guestEmail, setGuestEmail] = useState('');
  const [dates, setDates] = useState(defaultDates);
  const [pendingRoom, setPendingRoom] = useState(null);
  const [qty, setQty] = useState({});
  const [orderBookingId, setOrderBookingId] = useState('');
  const [confirmOrder, setConfirmOrder] = useState(false);

  const selectedHotel = hotels.find((h) => String(h.id) === String(hotelId));
  const confirmedStays = bookings.filter((b) => b.status === 'CONFIRMED');
  const selectedStay = confirmedStays.find((b) => String(b.id) === String(orderBookingId));
  const stayOrders = orders.filter((o) => String(o.bookingId) === String(orderBookingId));
  const selectedDishes = menu.filter((item) => (qty[item.id] || 0) > 0);
  const orderTotal = selectedDishes.reduce((sum, item) => sum + Number(item.price) * qty[item.id], 0);
  const orderSummary = useMemo(
    () =>
      selectedDishes.map((item) => `${item.name} × ${qty[item.id]}`).join(', ') +
      (selectedDishes.length ? ` · ${formatMoney(orderTotal, selectedHotel?.city)}` : ''),
    [selectedDishes, qty, orderTotal, selectedHotel]
  );

  useEffect(() => {
    listHotels()
      .then((list) => {
        setHotels(list);
        setHotelId((current) => current || (list[0] ? String(list[0].id) : ''));
      })
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const refresh = () => {
    if (!hotelId) {
      return Promise.resolve();
    }
    const id = Number(hotelId);
    return Promise.all([listRooms(id), hotelMenu(id), hotelBookings(id), hotelOrders(id)]).then(([r, m, b, o]) => {
      setRooms(r);
      setMenu(m);
      setBookings(b);
      setOrders(o);
    });
  };

  useEffect(() => {
    if (!hotelId) {
      return;
    }
    setError('');
    refresh().catch((err) => setError(apiErrorMessage(err)));
  }, [hotelId]);

  const wrap = async (fn, successMessage) => {
    setError('');
    try {
      await fn();
      await refresh();
      if (successMessage) {
        toast.success(successMessage);
      }
    } catch (err) {
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
    }
  };

  const onDeleteMenu = async () => {
    if (!deleteItem) {
      return;
    }
    setBusy(true);
    try {
      await deleteMenuItem(deleteItem.id);
      toast.success(`${deleteItem.name} removed from the menu.`);
      setDeleteItem(null);
      await refresh();
    } catch (err) {
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
      setDeleteItem(null);
    } finally {
      setBusy(false);
    }
  };

  const requestBook = async (room) => {
    setError('');
    if (!guestEmail.trim() || !guestEmail.includes('@')) {
      const message = "Enter the guest's email before booking a room.";
      setError(message);
      toast.error(message);
      return;
    }
    try {
      const avail = await checkAvailability(room.id, dates.checkIn, dates.checkOut);
      if (!avail.available) {
        const message = avail.message || 'That room is not free for these dates.';
        setError(message);
        toast.error(message);
        return;
      }
    } catch (err) {
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
      return;
    }
    setPendingRoom(room);
  };

  const onBookGuest = async () => {
    if (!pendingRoom || submitting.current) {
      return;
    }
    submitting.current = true;
    setBusy(true);
    setError('');
    try {
      const created = await createBooking(pendingRoom.id, dates.checkIn, dates.checkOut, guestEmail.trim());
      const confirmed = await confirmBooking(created.id);
      toast.success(`Room ${pendingRoom.roomNumber} is confirmed. Take the food order below.`);
      setPendingRoom(null);
      setQty({});
      setOrderBookingId(String(confirmed.id));
      await refresh();
    } catch (err) {
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
      setPendingRoom(null);
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  };

  const requestOrder = () => {
    setError('');
    if (!selectedStay) {
      const message = 'Book a room first, then take the food order.';
      setError(message);
      toast.error(message);
      return;
    }
    if (selectedDishes.length === 0) {
      const message = 'Add at least one dish.';
      setError(message);
      toast.error(message);
      return;
    }
    setConfirmOrder(true);
  };

  const onTakeOrder = async () => {
    if (submitting.current || !selectedStay) {
      return;
    }
    submitting.current = true;
    setBusy(true);
    try {
      await placeOrder({
        hotelId: Number(hotelId),
        bookingId: selectedStay.id,
        roomNumber: selectedStay.roomNumber,
        items: selectedDishes.map((item) => ({ menuItemId: item.id, quantity: qty[item.id] })),
      });
      toast.success(`Food order sent to room ${selectedStay.roomNumber}.`);
      setQty({});
      setConfirmOrder(false);
      await refresh();
    } catch (err) {
      const message = apiErrorMessage(err);
      setError(message);
      toast.error(message);
      setConfirmOrder(false);
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  };

  if (loading) {
    return <Loading label="Opening front desk…" />;
  }

  return (
    <section>
      <div className="desk-head">
        <div>
          <h1>Front desk</h1>
          <p className="muted">Hotel staff only</p>
        </div>
        {view === 'dashboard' ? (
          <button type="button" className="btn btn-ghost" onClick={() => setView('setup')}>
            Back to desk
          </button>
        ) : (
          <button type="button" className="btn btn-primary" onClick={() => setView('dashboard')}>
            View dashboard
          </button>
        )}
      </div>
      <ErrorMessage message={error} />
      <label>
        Hotel
        <select value={hotelId} onChange={(e) => setHotelId(e.target.value)}>
          {hotels.map((h) => (
            <option key={h.id} value={h.id}>
              {h.name} · {h.city}
            </option>
          ))}
        </select>
      </label>

      {view !== 'dashboard' && (
        <div className="tabs">
          <button type="button" className={`btn ${view === 'setup' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setView('setup')}>
            Add rooms & menu
          </button>
          <button type="button" className={`btn ${view === 'serve' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setView('serve')}>
            Book room & take order
          </button>
        </div>
      )}

      {view === 'setup' && (
        <div className="desk-split">
          <div className="card">
            <h2>Rooms</h2>
            <form
              className="inline-form"
              onSubmit={(e) => {
                e.preventDefault();
                wrap(
                  () =>
                    createRoom({
                      hotelId: Number(hotelId),
                      roomNumber: roomForm.roomNumber,
                      roomType: roomForm.roomType,
                      pricePerNight: Number(roomForm.pricePerNight),
                    }).then(() => setRoomForm({ roomNumber: '', roomType: 'DELUXE', pricePerNight: '120' })),
                  `Room ${roomForm.roomNumber} added.`
                );
              }}
            >
              <input
                placeholder="Room number"
                value={roomForm.roomNumber}
                onChange={(e) => setRoomForm({ ...roomForm, roomNumber: e.target.value })}
                required
              />
              <select value={roomForm.roomType} onChange={(e) => setRoomForm({ ...roomForm, roomType: e.target.value })}>
                <option value="STANDARD">Standard</option>
                <option value="DELUXE">Deluxe</option>
                <option value="SUITE">Suite</option>
              </select>
              <input
                type="number"
                min="0"
                step="0.01"
                value={roomForm.pricePerNight}
                onChange={(e) => setRoomForm({ ...roomForm, pricePerNight: e.target.value })}
                required
              />
              <button className="btn btn-primary" type="submit">
                Add room
              </button>
            </form>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Room</th>
                    <th>Type</th>
                    <th>Price / night</th>
                  </tr>
                </thead>
                <tbody>
                  {rooms.map((r) => (
                    <tr key={r.id}>
                      <td>{r.roomNumber}</td>
                      <td>{roomTypeLabel(r.roomType)}</td>
                      <td>{formatMoney(r.pricePerNight, selectedHotel?.city)}</td>
                    </tr>
                  ))}
                  {rooms.length === 0 && (
                    <tr>
                      <td colSpan={3} className="muted">
                        No rooms yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="card">
            <h2>Food menu</h2>
            <form
              className="inline-form"
              onSubmit={(e) => {
                e.preventDefault();
                wrap(
                  () =>
                    createMenuItem({
                      hotelId: Number(hotelId),
                      name: menuForm.name,
                      price: Number(menuForm.price),
                      available: true,
                    }).then(() => setMenuForm({ name: '', price: '8' })),
                  `${menuForm.name} added to the menu.`
                );
              }}
            >
              <input
                placeholder="Dish name"
                value={menuForm.name}
                onChange={(e) => setMenuForm({ ...menuForm, name: e.target.value })}
                required
              />
              <input
                type="number"
                min="0"
                step="0.01"
                value={menuForm.price}
                onChange={(e) => setMenuForm({ ...menuForm, price: e.target.value })}
                required
              />
              <button className="btn btn-primary" type="submit">
                Add dish
              </button>
            </form>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Dish</th>
                    <th>Price</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {menu.map((item) => (
                    <tr key={item.id}>
                      <td>{item.name}</td>
                      <td>{formatMoney(item.price, selectedHotel?.city)}</td>
                      <td>
                        <button type="button" className="btn btn-ghost" onClick={() => setDeleteItem(item)}>
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                  {menu.length === 0 && (
                    <tr>
                      <td colSpan={3} className="muted">
                        No dishes yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {view === 'serve' && (
        <div className="desk-split">
          <div className="card">
            <h2>Book a room</h2>
            <form className="form" onSubmit={(e) => e.preventDefault()}>
              <label>
                Guest email
                <input
                  type="email"
                  placeholder="guest@email.com"
                  value={guestEmail}
                  onChange={(e) => setGuestEmail(e.target.value)}
                  required
                />
              </label>
              <div className="dates">
                <label>
                  Check-in
                  <input type="date" value={dates.checkIn} onChange={(e) => setDates((d) => ({ ...d, checkIn: e.target.value }))} />
                </label>
                <label>
                  Check-out
                  <input type="date" value={dates.checkOut} onChange={(e) => setDates((d) => ({ ...d, checkOut: e.target.value }))} />
                </label>
              </div>
            </form>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Room</th>
                    <th>Type</th>
                    <th>Price</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {rooms.map((room) => (
                    <tr key={room.id}>
                      <td>{room.roomNumber}</td>
                      <td>{roomTypeLabel(room.roomType)}</td>
                      <td>{formatMoney(room.pricePerNight, selectedHotel?.city)}</td>
                      <td>
                        <button type="button" className="btn btn-primary" disabled={busy} onClick={() => requestBook(room)}>
                          Book
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="card">
            <h2>Take food order</h2>
            {selectedStay ? (
              <>
                <article className="confirmed-stay">
                  <p className="muted">Confirmed room</p>
                  <h2>Room {selectedStay.roomNumber}</h2>
                  <p>
                    {selectedStay.guestEmail} · {formatDate(selectedStay.checkIn)} – {formatDate(selectedStay.checkOut)}
                  </p>
                  <StatusBadge status={selectedStay.status} />
                </article>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Dish</th>
                        <th>Price</th>
                        <th>Qty</th>
                      </tr>
                    </thead>
                    <tbody>
                      {menu.map((item) => (
                        <tr key={item.id}>
                          <td>{item.name}</td>
                          <td>{formatMoney(item.price, selectedHotel?.city)}</td>
                          <td>
                            {item.available ? (
                              <div className="qty">
                                <button type="button" onClick={() => setQty((prev) => ({ ...prev, [item.id]: Math.max(0, (prev[item.id] || 0) - 1) }))}>
                                  −
                                </button>
                                <span>{qty[item.id] || 0}</span>
                                <button type="button" onClick={() => setQty((prev) => ({ ...prev, [item.id]: (prev[item.id] || 0) + 1 }))}>
                                  +
                                </button>
                              </div>
                            ) : (
                              <span className="muted">Unavailable</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p className="muted">{selectedDishes.length ? orderSummary : 'Choose dishes for this confirmed room.'}</p>
                <button className="btn btn-primary" type="button" disabled={busy || selectedDishes.length === 0} onClick={requestOrder}>
                  Send food to room {selectedStay.roomNumber}
                </button>
                {stayOrders.length > 0 && (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>This room’s orders</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {stayOrders.map((o) => (
                          <tr key={o.id}>
                            <td>{o.items?.map((i) => `${i.itemName} × ${i.quantity}`).join(', ')}</td>
                            <td>
                              <StatusBadge status={o.status} />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            ) : (
              <p className="muted">Book a room on the left. The confirmed stay and menu will appear here.</p>
            )}
          </div>
        </div>
      )}

      {view === 'dashboard' && (
        <div className="desk-split">
          <div className="card">
            <h2>Bookings</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Guest</th>
                    <th>Room</th>
                    <th>Dates</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map((b) => (
                    <tr key={b.id}>
                      <td>{b.guestEmail}</td>
                      <td>{b.roomNumber}</td>
                      <td>
                        {formatDate(b.checkIn)} – {formatDate(b.checkOut)}
                      </td>
                      <td>
                        <StatusBadge status={b.status} />
                      </td>
                    </tr>
                  ))}
                  {bookings.length === 0 && (
                    <tr>
                      <td colSpan={4} className="muted">
                        No bookings yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
          <div className="card">
            <h2>Food orders</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Room</th>
                    <th>Items</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((o) => (
                    <tr key={o.id}>
                      <td>{o.roomNumber}</td>
                      <td>{o.items?.map((i) => `${i.itemName} × ${i.quantity}`).join(', ')}</td>
                      <td>
                        <StatusBadge status={o.status} />
                      </td>
                      <td>
                        {o.status === 'PLACED' && (
                          <button type="button" className="btn btn-primary" onClick={() => wrap(() => updateOrderStatus(o.id, 'PREPARING'), 'Kitchen started this order.')}>
                            Preparing
                          </button>
                        )}
                        {o.status === 'PREPARING' && (
                          <button type="button" className="btn btn-primary" onClick={() => wrap(() => updateOrderStatus(o.id, 'DELIVERED'), 'Order marked delivered.')}>
                            Delivered
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {orders.length === 0 && (
                    <tr>
                      <td colSpan={4} className="muted">
                        No food orders yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(deleteItem)}
        title="Remove this dish?"
        message={deleteItem ? `${deleteItem.name} will no longer appear on the menu.` : ''}
        confirmLabel="Remove dish"
        danger
        busy={busy}
        onCancel={() => setDeleteItem(null)}
        onConfirm={onDeleteMenu}
      />
      <ConfirmDialog
        open={Boolean(pendingRoom)}
        title="Book this room for the guest?"
        message={
          pendingRoom
            ? `Room ${pendingRoom.roomNumber} for ${guestEmail} from ${formatDate(dates.checkIn)} to ${formatDate(dates.checkOut)}. The food menu will open on the right after confirm.`
            : ''
        }
        confirmLabel="Book and confirm"
        busy={busy}
        onCancel={() => !busy && setPendingRoom(null)}
        onConfirm={onBookGuest}
      />
      <ConfirmDialog
        open={confirmOrder}
        title="Send this food order?"
        message={selectedStay ? `Room ${selectedStay.roomNumber}. ${orderSummary}` : ''}
        confirmLabel="Send to room"
        busy={busy}
        onCancel={() => !busy && setConfirmOrder(false)}
        onConfirm={onTakeOrder}
      />
    </section>
  );
}
