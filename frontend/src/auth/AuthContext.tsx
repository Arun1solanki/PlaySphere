import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import {
  authApi,
  configureSessionCallbacks,
  getTabSessionId,
  setAccessToken,
  setTabSessionId,
} from '../lib/api'
import type { AuthPayload, PlatformRole, UserSummary } from '../types/auth'

type LoginInput = { email: string; password: string; rememberMe: boolean }
type RegisterInput = { displayName: string; email: string; password: string; role: string }
type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

type AuthContextValue = {
  user: UserSummary | null
  accessToken: string | null
  status: AuthStatus
  sessionNotice: string | null
  login: (input: LoginInput) => Promise<UserSummary>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
  logoutAll: () => Promise<void>
  updateUser: (user: UserSummary) => void
  clearSessionNotice: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)
const AUTH_CHANNEL = 'playsphere-auth'
const AUTH_EVENT_KEY = 'playsphere.auth-event'
const LAST_ACTIVITY_KEY = 'playsphere.last-activity'

const idleMinutes: Record<PlatformRole, number> = {
  PLAYER: 60,
  ORGANIZER: 45,
  TURF_OWNER: 45,
  ADMIN: 20,
  SUPER_ADMIN: 15,
}

function effectiveIdleMinutes(roles: PlatformRole[]): number {
  return Math.min(...roles.map((role) => idleMinutes[role]))
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<UserSummary | null>(null)
  const [accessTokenState, setAccessTokenState] = useState<string | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [sessionNotice, setSessionNotice] = useState<string | null>(null)
  const logoutRunning = useRef(false)
  const activeUserIdRef = useRef<string | null>(null)
  const channelRef = useRef<BroadcastChannel | null>(null)

  const applyPayload = useCallback((payload: AuthPayload) => {
    setTabSessionId(payload.sessionId)
    setAccessToken(payload.accessToken)
    setAccessTokenState(payload.accessToken)
    activeUserIdRef.current = payload.user.id
    setUser(payload.user)
    setStatus('authenticated')
  }, [])

  const clearLocalSession = useCallback((notice?: string) => {
    setAccessToken(null)
    setTabSessionId(null)
    sessionStorage.removeItem(LAST_ACTIVITY_KEY)
    setAccessTokenState(null)
    activeUserIdRef.current = null
    setUser(null)
    setStatus('anonymous')
    if (notice) setSessionNotice(notice)
  }, [])

  const broadcastLogoutAll = useCallback((userId: string) => {
    const event = { type: 'LOGOUT_USER', userId, at: Date.now() }
    channelRef.current?.postMessage(event)
    localStorage.setItem(AUTH_EVENT_KEY, JSON.stringify(event))
  }, [])

  useEffect(() => {
    const cleanupCallbacks = configureSessionCallbacks({
      onRefreshed: applyPayload,
      onExpired: () => clearLocalSession('Your session expired. Please log in again.'),
    })

    const channel = 'BroadcastChannel' in window ? new BroadcastChannel(AUTH_CHANNEL) : null
    channelRef.current = channel

    const handleEvent = (event?: { type?: string; userId?: string }) => {
      if (event?.type === 'LOGOUT_USER' && event.userId && event.userId === activeUserIdRef.current) {
        clearLocalSession('This account was signed out from every device.')
      }
    }

    if (channel) channel.onmessage = (event) => handleEvent(event.data)
    const storageHandler = (event: StorageEvent) => {
      if (event.key !== AUTH_EVENT_KEY || !event.newValue) return
      try {
        handleEvent(JSON.parse(event.newValue) as { type?: string; userId?: string })
      } catch {
        // Ignore malformed cross-tab events.
      }
    }
    window.addEventListener('storage', storageHandler)

    if (!getTabSessionId()) {
      setStatus('anonymous')
    } else {
      void authApi.refresh().then(applyPayload).catch(() => clearLocalSession())
    }

    return () => {
      cleanupCallbacks()
      channel?.close()
      channelRef.current = null
      window.removeEventListener('storage', storageHandler)
    }
  }, [applyPayload, clearLocalSession])

  const login = useCallback(async (input: LoginInput) => {
    // Login creates a fresh server session and switches only this tab to it.
    // We intentionally do not revoke the previous session here: a duplicated
    // browser tab may still be using it. Users can revoke old sessions from
    // Manage Sessions or by choosing Log out from every device.
    const payload = await authApi.login(input)
    applyPayload(payload)
    sessionStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()))
    return payload.user
  }, [applyPayload])

  const register = useCallback(async (input: RegisterInput) => {
    await authApi.register(input)
  }, [])

  const logout = useCallback(async () => {
    if (logoutRunning.current) return
    logoutRunning.current = true
    try {
      await authApi.logout().catch(() => undefined)
    } finally {
      clearLocalSession()
      logoutRunning.current = false
    }
  }, [clearLocalSession])

  const logoutAll = useCallback(async () => {
    const userId = user?.id
    await authApi.logoutAll()
    clearLocalSession('You have been logged out from every device.')
    if (userId) broadcastLogoutAll(userId)
  }, [broadcastLogoutAll, clearLocalSession, user?.id])

  const updateUser = useCallback((nextUser: UserSummary) => {
    setUser(nextUser)
  }, [])

  useEffect(() => {
    if (status !== 'authenticated' || !user) return

    let lastRecorded = 0
    const recordActivity = () => {
      const now = Date.now()
      if (now - lastRecorded < 15_000) return
      lastRecorded = now
      sessionStorage.setItem(LAST_ACTIVITY_KEY, String(now))
    }

    const events: Array<keyof WindowEventMap> = ['pointerdown', 'keydown', 'scroll', 'touchstart']
    events.forEach((event) => window.addEventListener(event, recordActivity, { passive: true }))
    recordActivity()

    const timer = window.setInterval(() => {
      const saved = Number(sessionStorage.getItem(LAST_ACTIVITY_KEY) ?? Date.now())
      const timeoutMs = effectiveIdleMinutes(user.roles) * 60_000
      if (Date.now() - saved >= timeoutMs) {
        setSessionNotice('You were signed out after a period of inactivity.')
        void logout()
      }
    }, 30_000)

    const heartbeat = window.setInterval(() => {
      const saved = Number(sessionStorage.getItem(LAST_ACTIVITY_KEY) ?? 0)
      const recentlyActive = Date.now() - saved < 5 * 60_000
      if (document.visibilityState === 'visible' && recentlyActive) {
        void authApi.me().then(setUser).catch(() => undefined)
      }
    }, 3 * 60_000)

    return () => {
      window.clearInterval(timer)
      window.clearInterval(heartbeat)
      events.forEach((event) => window.removeEventListener(event, recordActivity))
    }
  }, [logout, status, user])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken: accessTokenState,
      status,
      sessionNotice,
      login,
      register,
      logout,
      logoutAll,
      updateUser,
      clearSessionNotice: () => setSessionNotice(null),
    }),
    [user, accessTokenState, status, sessionNotice, login, register, logout, logoutAll, updateUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside AuthProvider')
  return value
}
