import { type ChangeEvent, type PropsWithChildren, useMemo } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { roleThemes } from '../config/roleConfig'
import type { PlatformRole } from '../types/auth'
import { ArenaBackground } from './ArenaBackground'
import { Brand } from './Brand'

type RoleShellProps = PropsWithChildren<{ role: PlatformRole }>

export function RoleShell({ role, children }: RoleShellProps) {
  const theme = roleThemes[role]
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const availableRoles = useMemo<PlatformRole[]>(() => user?.roles ?? [role], [role, user])
  const switchRole = (nextRole: PlatformRole) => {
    const next = roleThemes[nextRole]
    navigate(`/app/${next.routeSegment}/dashboard`)
  }

  return (
    <div
      className="role-portal"
      style={
        {
          '--role-accent': theme.accent,
          '--role-accent-soft': theme.accentSoft,
          '--role-glow': theme.glow,
        } as React.CSSProperties
      }
    >
      <ArenaBackground accent={theme.accent} />
      <aside className="portal-sidebar">
        <div className="sidebar-brand"><Brand /></div>
        <div className="role-badge">
          <span className="role-pulse" />
          <div>
            <small>ACTIVE WORKSPACE</small>
            <strong>{theme.label}</strong>
          </div>
        </div>

        <nav className="portal-nav" aria-label={`${theme.label} navigation`}>
          {theme.nav.map((item) => {
            const destination = `/app/${theme.routeSegment}/${item.path}`
            return (
              <NavLink
                key={item.path}
                to={destination}
                className={({ isActive }) =>
                  `portal-nav-link ${isActive ? 'active' : ''}`
                }
                end={item.path === 'dashboard'}
              >
                <span className="portal-nav-icon">{item.icon}</span>
                <span>{item.label}</span>
              </NavLink>
            )
          })}
        </nav>

        <div className="sidebar-bottom">
          <div className="mini-system-card">
            <span className="system-dot" />
            <div><strong>System ready</strong><small>All services operational</small></div>
          </div>
          <button className="sidebar-action secondary" onClick={() => navigate('/account/sessions')}>Manage sessions</button>
          <button
            className="sidebar-action"
            onClick={() => void logout().then(() => navigate('/'))}
          >
            Sign out
          </button>
        </div>
      </aside>

      <main className="portal-main">
        <header className="portal-topbar">
          <div>
            <p className="topbar-kicker">{theme.eyebrow}</p>
            <h1>{theme.label} workspace</h1>
          </div>
          <div className="topbar-actions">
            {availableRoles.length > 1 && (
              <label className="role-switcher">
                <span>View as</span>
                <select value={role} onChange={(event: ChangeEvent<HTMLSelectElement>) => switchRole(event.target.value as PlatformRole)}>
                  {availableRoles.map((item) => (
                    <option key={item} value={item}>{roleThemes[item].label}</option>
                  ))}
                </select>
              </label>
            )}
            <button className="icon-button" aria-label="Notifications">◌<span className="notification-count">3</span></button>
            <button className="profile-chip" onClick={() => navigate('/profile/setup')}>
              <span className="profile-avatar">{(user?.displayName ?? theme.label).charAt(0)}</span>
              <span><strong>{user?.displayName}</strong><small>{theme.shortLabel}</small></span>
              <span className="chevron">⌄</span>
            </button>
          </div>
        </header>
        <div className="portal-content">{children}</div>
      </main>
    </div>
  )
}
