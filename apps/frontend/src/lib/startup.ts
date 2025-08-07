'use client';

export function notifyBackendStarting(statusCode?: number) {
  if (typeof window === 'undefined') return;

  try {
    const current = window.location.pathname + window.location.search;
    const alreadyStarting = window.location.pathname.startsWith('/starting');
    if (!alreadyStarting) {
      localStorage.setItem('shipkit_prev_path', current);
    }
    localStorage.setItem('shipkit_backend_starting', '1');
    localStorage.setItem('shipkit_backend_starting_code', String(statusCode ?? ''));
    if (!alreadyStarting) {
      window.location.href = '/starting';
    }
  } catch {}
}

export function clearBackendStartingFlag() {
  if (typeof window === 'undefined') return;
  try {
    localStorage.removeItem('shipkit_backend_starting');
    localStorage.removeItem('shipkit_backend_starting_code');
  } catch {}
}

export function getPreviousPath(): string | null {
  if (typeof window === 'undefined') return null;
  try {
    return localStorage.getItem('shipkit_prev_path');
  } catch {
    return null;
  }
}


