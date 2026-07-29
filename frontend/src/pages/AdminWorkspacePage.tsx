import { type ChangeEvent, useEffect, useState } from 'react'
import { RoleShell } from '../components/RoleShell'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { Refund, Report, Review, Turf } from '../types/domain'

export function AdminWorkspacePage({ role, module }: { role: PlatformRole; module: string }) {
  const [data, setData] = useState<any[]>([])
  const [notice, setNotice] = useState('')

  async function load() {
    try {
      if (module === 'approvals') setData(await platformApi.pendingTurfs())
      else if (module === 'turfs') setData(await platformApi.adminTurfs())
      else if (module === 'teams') setData(await platformApi.adminTeams())
      else if (module === 'events') setData(await platformApi.adminEvents())
      else if (module === 'bookings') setData(await platformApi.adminBookings())
      else if (module === 'payments') setData(await platformApi.pendingRefunds())
      else if (module === 'transactions') setData(await platformApi.adminPayments())
      else if (module === 'reviews') setData(await platformApi.adminReviews())
      else if (module === 'reports') setData(await platformApi.reports())
      else if (module === 'audit') setData(await platformApi.audit())
      else if (module === 'users' || module === 'administrators') setData(await platformApi.users())
      else setData([])
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load Admin data')
    }
  }

  useEffect(() => { void load() }, [module])

  const title = module.replaceAll('-', ' ')

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">PLATFORM CONTROL</p><h2>{title}</h2><p>Administrative decisions are protected by server-side role checks and recorded in audit logs.</p></section>
      {notice && <p className="workspace-notice">{notice}</p>}
      <section className="panel admin-table">
        {data.length === 0 && <p className="muted-copy">No records are currently available in this queue.</p>}
        {data.map((item: any) => (
          <article key={item.id}>
            <div>
              <b>{item.name || item.title || item.email || item.action || item.reason || item.bookingCode || item.id}</b>
              <span>{item.status || item.targetType || item.purpose || item.roles?.join(', ') || ''}</span>
              <small>{item.description || item.details || item.decisionNote || item.createdAt || item.referenceId || ''}</small>
            </div>
            {(module === 'approvals' || module === 'turfs') && item.status === 'PENDING_APPROVAL' && <div className="button-row"><button onClick={() => void platformApi.decideTurf((item as Turf).id, true).then(load)}>Approve</button><button onClick={() => void platformApi.decideTurf(item.id, false, 'Please correct the submitted details.').then(load)}>Reject</button></div>}
            {module === 'payments' && <div className="button-row"><button onClick={() => void platformApi.decideRefund((item as Refund).id, true, 'Approved after review').then(load)}>Approve</button><button onClick={() => void platformApi.decideRefund(item.id, false, 'Not eligible under the refund policy').then(load)}>Reject</button></div>}
            {module === 'reports' && <button onClick={() => void platformApi.resolveReport((item as Report).id, 'RESOLVED', 'Resolved by Admin').then(load)}>Resolve</button>}
            {module === 'reviews' && <select defaultValue={(item as Review).status} onChange={(event: ChangeEvent<HTMLSelectElement>) => void platformApi.moderateReview(item.id, event.target.value).then(load)}><option value="PUBLISHED">Published</option><option value="UNDER_REVIEW">Under review</option><option value="HIDDEN">Hidden</option><option value="REMOVED">Removed</option></select>}
            {(module === 'users' || module === 'administrators') && <select defaultValue={item.status} onChange={(event: ChangeEvent<HTMLSelectElement>) => void platformApi.updateUserStatus(item.id, event.target.value).then(load)}><option value="ACTIVE">Active</option><option value="SUSPENDED">Suspended</option><option value="BLOCKED">Blocked</option></select>}
          </article>
        ))}
      </section>
    </RoleShell>
  )
}
