import { useEffect, useMemo, useState } from 'react';
export function useCountdown(expiresAt?: string | null) {
  const deadline = useMemo(() => (expiresAt ? new Date(expiresAt).getTime() : 0), [expiresAt]);
  const [remainingMs, setRemainingMs] = useState(() => Math.max(0, deadline - Date.now()));
  useEffect(() => {
    const update = () => setRemainingMs(Math.max(0, deadline - Date.now()));
    update();
    if (!deadline) return undefined;
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, [deadline]);
  const totalSeconds = Math.ceil(remainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return {
    expired: deadline > 0 && remainingMs <= 0,
    remainingMs,
    label: `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`,
  };
}
