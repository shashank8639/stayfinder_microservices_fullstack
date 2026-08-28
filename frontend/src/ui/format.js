export function formatMoney(amount, city) {
  const value = Number(amount);
  if (Number.isNaN(value)) {
    return String(amount);
  }
  const currency = city === 'Dubai' ? 'AED' : 'INR';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(value);
}

export function statusLabel(status) {
  return (
    {
      PENDING: 'Awaiting confirmation',
      CONFIRMED: 'Confirmed',
      CANCELLED: 'Cancelled',
      PLACED: 'Received',
      PREPARING: 'Being prepared',
      DELIVERED: 'Delivered',
    }[status] || status
  );
}

export function hotelCover(city) {
  const covers = {
    Hyderabad:
      'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=900&q=80',
    Dubai:
      'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=900&q=80',
    Jaipur:
      'https://images.unsplash.com/photo-1477587458883-47145ed94245?auto=format&fit=crop&w=900&q=80',
  };
  return covers[city] || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80';
}

export function guestName(email) {
  if (!email) {
    return 'Guest';
  }
  const local = email.split('@')[0];
  return local.replace(/[._]/g, ' ');
}

export function formatDate(iso) {
  if (!iso) {
    return '';
  }
  return new Date(`${iso}T00:00:00`).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function roomTypeLabel(type) {
  return (
    {
      STANDARD: 'Standard room',
      DELUXE: 'Deluxe room',
      SUITE: 'Suite',
    }[type] || type
  );
}
