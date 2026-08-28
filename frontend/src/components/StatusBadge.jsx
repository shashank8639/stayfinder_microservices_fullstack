import { statusLabel } from '../ui/format';

export default function StatusBadge({ status }) {
  return <span className={`badge badge-${String(status || '').toLowerCase()}`}>{statusLabel(status)}</span>;
}
