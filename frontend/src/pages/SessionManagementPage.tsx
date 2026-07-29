import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { authApi } from '../lib/api'
import type { SessionSummary } from '../types/auth'

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function SessionManagementPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { logoutAll } = useAuth()
  const sessions = useQuery({
    queryKey: ['auth-sessions'],
    queryFn: authApi.sessions,
  })
  const revoke = useMutation({
    mutationFn: authApi.revokeSession,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['auth-sessions'] }),
  })

  return (
    <main className="sessions-page">
      <section className="sessions-card">
        <div className="sessions-heading">
          <div>
            <p className="eyebrow">ACCOUNT SECURITY</p>
            <h1>Active sessions</h1>
            <span>Review browsers and devices currently signed in to your PlaySphere account.</span>
          </div>
          <button className="button button-secondary" onClick={() => navigate(-1)}>← Back</button>
        </div>

        {sessions.isLoading && <p className="session-message">Loading active sessions…</p>}
        {sessions.isError && <p className="session-message error">Unable to load sessions.</p>}

        <div className="session-list">
          {sessions.data?.map((session: SessionSummary) => (
            <article className="session-row" key={session.id}>
              <div className="session-device-icon">◇</div>
              <div className="session-details">
                <div className="session-title">
                  <strong>{session.device}</strong>
                  {session.current && <span className="current-session">CURRENT SESSION</span>}
                </div>
                <p>IP: {session.ipAddress ?? 'Unavailable'}</p>
                <small>Last active {formatDate(session.lastUsedAt)} · Signed in {formatDate(session.createdAt)}</small>
              </div>
              {session.current ? (
                <span className="session-safe">Protected</span>
              ) : (
                <button
                  className="session-revoke"
                  disabled={revoke.isPending}
                  onClick={() => revoke.mutate(session.id)}
                >
                  Revoke
                </button>
              )}
            </article>
          ))}
        </div>

        <div className="sessions-danger-zone">
          <div><strong>Sign out everywhere</strong><span>This immediately revokes all Player, Organizer, Turf Owner, and Admin sessions for this account.</span></div>
          <button className="danger-button" onClick={() => void logoutAll()}>Log out all devices</button>
        </div>
      </section>
    </main>
  )
}
