import { useQuery } from '@tanstack/react-query'
import { RoleShell } from '../components/RoleShell'
import { StatCard } from '../components/StatCard'
import { roleThemes } from '../config/roleConfig'
import type { PlatformRole } from '../types/auth'
import { apiRequest } from '../lib/api'

type BackendDashboard = {
  role: string
  title: string
  subtitle: string
  accent: string
  cards: Array<{ label: string; value: string; hint: string }>
  primaryActions: string[]
}

export function DashboardPage({ resolvedRole: role }: { resolvedRole: PlatformRole }) {
  const theme = roleThemes[role]
  const endpoint = role === 'TURF_OWNER' ? 'turf-owner' : role === 'SUPER_ADMIN' ? 'admin' : role.toLowerCase()
  const dashboardQuery = useQuery({
    queryKey: ['dashboard', role],
    queryFn: () => apiRequest<BackendDashboard>(`/dashboard/${endpoint}`),
    retry: false,
  })
  const backend = dashboardQuery.data

  return (
    <RoleShell role={role}>
      <section className="dashboard-hero">
        <div className="dashboard-hero-copy">
          <p className="eyebrow">{theme.eyebrow}</p>
          <h2>{backend?.title ?? theme.title}</h2>
          <p>{backend?.subtitle ?? theme.subtitle}</p>
          <div className="dashboard-hero-buttons"><button className="button button-role">{theme.actions[0]?.label} →</button><button className="button button-ghost">View activity</button></div>
        </div>
        <div className="dashboard-orbit-card">
          <div className="radar-ring ring-a" /><div className="radar-ring ring-b" /><div className="radar-cross horizontal" /><div className="radar-cross vertical" />
          <span className="radar-center">{theme.nav[1]?.icon ?? '⌁'}</span>
          <i className="radar-point point-a" /><i className="radar-point point-b" /><i className="radar-point point-c" />
          <div className="radar-label"><small>LIVE WORKSPACE</small><strong>{dashboardQuery.isError ? 'Local UI ready' : 'Connected'}</strong></div>
        </div>
      </section>

      <section className="stats-grid">
        {theme.cards.map((card, index) => {
          const liveCard = backend?.cards[index]
          return <StatCard key={card.label} {...card} value={liveCard?.value ?? card.value} hint={liveCard?.hint ?? card.hint} />
        })}
      </section>

      <section className="dashboard-split">
        <article className="panel quick-actions-panel">
          <div className="panel-heading"><div><p>FAST TRACK</p><h3>What do you want to do?</h3></div><button>View all</button></div>
          <div className="quick-action-list">
            {theme.actions.map((action, index) => (
              <button key={action.label} className="quick-action">
                <span className="quick-action-icon">{action.icon}</span><span><strong>{backend?.primaryActions[index] ?? action.label}</strong><small>{action.description}</small></span><i>→</i>
              </button>
            ))}
          </div>
        </article>

        <article className="panel activity-panel">
          <div className="panel-heading"><div><p>LIVE SIGNALS</p><h3>Recent activity</h3></div><span className="live-pill"><i /> LIVE</span></div>
          <div className="timeline">
            {theme.activity.map((item, index) => (
              <div className="timeline-item" key={item.title}><span className="timeline-marker">{index + 1}</span><div><small>{item.tag}</small><strong>{item.title}</strong><p>{item.detail}</p></div><button>Open</button></div>
            ))}
            <div className="timeline-item muted"><span className="timeline-marker">✓</span><div><small>SYSTEM</small><strong>Your workspace is ready</strong><p>New module data will appear here as milestones are implemented.</p></div></div>
          </div>
        </article>
      </section>
    </RoleShell>
  )
}
