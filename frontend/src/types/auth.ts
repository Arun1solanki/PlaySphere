export type PlatformRole = 'PLAYER' | 'ORGANIZER' | 'TURF_OWNER' | 'ADMIN' | 'SUPER_ADMIN'

export type UserSummary = {
  id: string
  email: string
  displayName: string
  roles: PlatformRole[]
  emailVerified: boolean
  profileCompleted: boolean
}

export type AuthPayload = {
  accessToken: string
  accessExpiresInSeconds: number
  sessionId: string
  user: UserSummary
}

export type SessionSummary = {
  id: string
  current: boolean
  device: string
  ipAddress: string | null
  createdAt: string
  lastUsedAt: string
  expiresAt: string
}

export type ApiEnvelope<T> = {
  success: boolean
  message: string
  data: T
  errors?: Record<string, string>
}
