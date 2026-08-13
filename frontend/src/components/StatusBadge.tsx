import { statusLabel } from '../lib/format';

interface StatusBadgeProps {
  value?: string;
}

export function StatusBadge({ value }: StatusBadgeProps) {
  const status = value ?? 'UNKNOWN';
  return <span className={`status status-${status.toLowerCase()}`}>{statusLabel(value)}</span>;
}
