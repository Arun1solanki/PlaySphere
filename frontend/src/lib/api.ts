import type { ApiEnvelope, AuthPayload, SessionSummary, UserSummary } from '../types/auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'
const TAB_SESSION_KEY = 'playsphere.tab-session-id'
const SESSION_HEADER = 'X-PlaySphere-Session'

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors?: Record<string, string>

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

let accessToken: string | null = null
let refreshPromise: Promise<AuthPayload> | null = null
let onSessionRefreshed: ((payload: AuthPayload) => void) | null = null
let onSessionExpired: (() => void) | null = null

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function getAccessToken(): string | null {
  return accessToken
}

export function getTabSessionId(): string | null {
  return sessionStorage.getItem(TAB_SESSION_KEY)
}

export function setTabSessionId(sessionId: string | null): void {
  if (sessionId) sessionStorage.setItem(TAB_SESSION_KEY, sessionId)
  else sessionStorage.removeItem(TAB_SESSION_KEY)
}

export function configureSessionCallbacks(callbacks: {
  onRefreshed: (payload: AuthPayload) => void
  onExpired: () => void
}): () => void {
  onSessionRefreshed = callbacks.onRefreshed
  onSessionExpired = callbacks.onExpired
  return () => {
    onSessionRefreshed = null
    onSessionExpired = null
  }
}

async function parseEnvelope<T>(response: Response): Promise<T> {
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null
  if (!response.ok) {
    throw new ApiError(payload?.message ?? 'Request failed', response.status, payload?.errors)
  }
  if (!payload) throw new ApiError('The server returned an empty response', response.status)
  return payload.data
}

function addSessionHeader(headers: Headers): void {
  const sessionId = getTabSessionId()
  if (sessionId && !headers.has(SESSION_HEADER)) headers.set(SESSION_HEADER, sessionId)
}

async function rawRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  addSessionHeader(headers)
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  })
  return parseEnvelope<T>(response)
}

export async function refreshBrowserSession(): Promise<AuthPayload> {
  const sessionId = getTabSessionId()
  if (!sessionId) throw new ApiError('No browser-tab session', 401)

  if (!refreshPromise) {
    refreshPromise = (async () => {
      const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          [SESSION_HEADER]: sessionId,
        },
      })
      const payload = await parseEnvelope<AuthPayload>(response)
      setAccessToken(payload.accessToken)
      setTabSessionId(payload.sessionId)
      onSessionRefreshed?.(payload)
      return payload
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

function shouldAttemptRefresh(path: string): boolean {
  return ![
    '/auth/login',
    '/auth/register',
    '/auth/verify-email',
    '/auth/resend-verification',
    '/auth/password-reset/request',
    '/auth/password-reset/confirm',
    '/auth/refresh',
    '/auth/logout',
  ].includes(path)
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  retryAfterRefresh = true,
): Promise<T> {
  try {
    return await rawRequest<T>(path, init)
  } catch (error) {
    if (
      error instanceof ApiError
      && error.status === 401
      && retryAfterRefresh
      && shouldAttemptRefresh(path)
      && getTabSessionId()
    ) {
      try {
        await refreshBrowserSession()
        return await rawRequest<T>(path, init)
      } catch (refreshError) {
        setAccessToken(null)
        setTabSessionId(null)
        onSessionExpired?.()
        throw refreshError
      }
    }
    throw error
  }
}

export const authApi = {
  register(input: { displayName: string; email: string; password: string; role: string }) {
    return apiRequest<null>('/auth/register', { method: 'POST', body: JSON.stringify(input) }, false)
  },
  verifyEmail(token: string) {
    return apiRequest<null>('/auth/verify-email', { method: 'POST', body: JSON.stringify({ token }) }, false)
  },
  resendVerification(email: string) {
    return apiRequest<null>('/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email }) }, false)
  },
  requestPasswordReset(email: string) {
    return apiRequest<null>('/auth/password-reset/request', { method: 'POST', body: JSON.stringify({ email }) }, false)
  },
  confirmPasswordReset(token: string, newPassword: string) {
    return apiRequest<null>('/auth/password-reset/confirm', { method: 'POST', body: JSON.stringify({ token, newPassword }) }, false)
  },
  login(input: { email: string; password: string; rememberMe: boolean }) {
    return apiRequest<AuthPayload>('/auth/login', { method: 'POST', body: JSON.stringify(input) }, false)
  },
  refresh() {
    return refreshBrowserSession()
  },
  me() {
    return apiRequest<UserSummary>('/auth/me')
  },
  logout() {
    return apiRequest<null>('/auth/logout', { method: 'POST' }, false)
  },
  logoutAll() {
    return apiRequest<null>('/auth/logout-all', { method: 'POST' })
  },
  sessions() {
    return apiRequest<SessionSummary[]>('/auth/sessions')
  },
  revokeSession(sessionId: string) {
    return apiRequest<null>(`/auth/sessions/${encodeURIComponent(sessionId)}`, { method: 'DELETE' })
  },
}
