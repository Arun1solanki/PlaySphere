import { useParams } from 'react-router-dom'
import { RoleShell } from '../components/RoleShell'
import type { PlatformRole } from '../types/auth'
import { AdminWorkspacePage } from './AdminWorkspacePage'
import { BookingsPage } from './BookingsPage'
import { EventsPage } from './EventsPage'
import { DiscoverPage } from './DiscoverPage'
import { MessagesPage } from './MessagesPage'
import { NotificationsPage } from './NotificationsPage'
import { PaymentsPage } from './PaymentsPage'
import { RecruitmentPage } from './RecruitmentPage'
import { ReviewsReportsPage } from './ReviewsReportsPage'
import { TeamsPage } from './TeamsPage'
import { TurfsPage } from './TurfsPage'

export function ModulePage({ resolvedRole: role }: { resolvedRole: PlatformRole }) {
  const { module = 'dashboard' } = useParams()

  if (module === 'notifications') return <NotificationsPage role={role} />
  if (module === 'messages') return <MessagesPage role={role} />

  if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
    return <AdminWorkspacePage role={role} module={module} />
  }

  if (module === 'discover') return <DiscoverPage role={role} />
  if (module === 'teams') return <TeamsPage role={role} />
  if (module === 'need-players') return <RecruitmentPage role={role} />
  if (['turfs', 'add-turf', 'availability', 'pricing', 'check-in', 'equipment'].includes(module)) {
    return <TurfsPage role={role} mode={module} />
  }
  if (['events', 'create-event', 'registrations', 'participants', 'fixtures', 'scores', 'venues'].includes(module)) {
    return <EventsPage role={role} mode={module} />
  }
  if (module === 'bookings') return <BookingsPage role={role} />
  if (module === 'payments' || module === 'earnings') return <PaymentsPage role={role} mode={module} />
  if (module === 'reviews') return <ReviewsReportsPage role={role} />

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">PLAYSPHERE</p><h2>{module.replaceAll('-', ' ')}</h2><p>This workspace is connected to the shared role-aware platform foundation.</p></section>
      <section className="panel empty-state"><h3>Operational workspace</h3><p>Use Teams, Turfs, Events, Bookings, Payments, Messages, Reviews, and Notifications from the sidebar.</p></section>
    </RoleShell>
  )
}
