import { useEffect, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { Report, Review } from '../types/domain'

export function ReviewsReportsPage({ role }: { role: PlatformRole }) {
  const [reviews, setReviews] = useState<Review[]>([])
  const [reports, setReports] = useState<Report[]>([])
  const [notice, setNotice] = useState('')

  useEffect(() => {
    void platformApi.myReports().then(setReports).catch((error: Error) => setNotice(error.message))
  }, [])

  async function findReviews(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      setReviews(await platformApi.reviews(String(form.get('targetType')), String(form.get('targetId'))))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load reviews')
    }
  }

  async function createReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      await platformApi.createReview({
        targetType: form.get('targetType'),
        targetId: form.get('targetId'),
        rating: Number(form.get('rating')),
        comment: form.get('comment'),
      })
      setNotice('Review published after participation verification.')
      formElement.reset()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to publish review')
    }
  }

  async function createReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      await platformApi.createReport({
        targetType: form.get('targetType'),
        targetId: form.get('targetId'),
        reason: form.get('reason'),
        description: form.get('description'),
        priority: form.get('priority'),
      })
      setNotice('Report submitted to the moderation queue.')
      formElement.reset()
      setReports(await platformApi.myReports())
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to submit report')
    }
  }

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">TRUST & SAFETY</p><h2>Reviews & Reports</h2><p>Verified participants can review completed activities, and every authenticated user can report misuse.</p></section>
      {notice && <p className="workspace-notice">{notice}</p>}
      <div className="workspace-grid">
        <form className="panel workspace-form" onSubmit={createReview}>
          <h3>Write a verified review</h3>
          <label>Target<select name="targetType"><option value="TURF">Turf</option><option value="EVENT">Event</option></select></label>
          <label>Target ID<input name="targetId" required /></label>
          <label>Rating<select name="rating"><option value="5">5 — Excellent</option><option value="4">4 — Good</option><option value="3">3 — Average</option><option value="2">2 — Poor</option><option value="1">1 — Very poor</option></select></label>
          <label>Comment<textarea name="comment" maxLength={1000} /></label>
          <button className="primary-button">Publish review</button>
        </form>

        <form className="panel workspace-form" onSubmit={createReport}>
          <h3>Report content or activity</h3>
          <label>Target type<input name="targetType" required placeholder="USER, TEAM, TURF, EVENT, MESSAGE..." /></label>
          <label>Target ID<input name="targetId" required /></label>
          <label>Reason<input name="reason" required maxLength={120} /></label>
          <label>Priority<select name="priority"><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option><option value="URGENT">Urgent</option></select></label>
          <label>Description<textarea name="description" required maxLength={1000} /></label>
          <button className="primary-button">Submit report</button>
        </form>
      </div>

      <div className="workspace-grid">
        <section className="panel workspace-list">
          <form className="inline-search" onSubmit={findReviews}><select name="targetType"><option value="TURF">Turf</option><option value="EVENT">Event</option></select><input name="targetId" required placeholder="Target ID" /><button>View reviews</button></form>
          <h3>Published reviews</h3>
          {reviews.map((review) => <article className="resource-card" key={review.id}><div><b>{'★'.repeat(review.rating)}</b><span>{review.status}</span><small>{review.comment || 'No written comment'}</small></div></article>)}
        </section>
        <section className="panel workspace-list"><h3>My reports</h3>{reports.map((report) => <article className="resource-card" key={report.id}><div><b>{report.reason}</b><span>{report.status} · {report.priority}</span><small>{report.description}</small></div></article>)}</section>
      </div>
    </RoleShell>
  )
}
