import type { SessionResponse } from "./api";

const SESSION_KEY = "bank-simulator-session";

export function saveSession(session: SessionResponse) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function loadSession(): SessionResponse | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as SessionResponse;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}
