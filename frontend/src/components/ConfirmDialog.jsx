import { useRef } from 'react';

export default function ConfirmDialog({ open, title, message, confirmLabel = 'Confirm', danger = false, busy = false, onCancel, onConfirm }) {
  const clicked = useRef(false);

  if (!open) {
    clicked.current = false;
    return null;
  }

  const handleConfirm = () => {
    if (busy || clicked.current) {
      return;
    }
    clicked.current = true;
    onConfirm();
  };

  return (
    <div className="modal-backdrop" role="presentation" onClick={() => !busy && onCancel()}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-title">{title}</h2>
        <p>{message}</p>
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          <button
            type="button"
            className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
            onClick={handleConfirm}
            disabled={busy}
          >
            {busy ? 'Please wait…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
