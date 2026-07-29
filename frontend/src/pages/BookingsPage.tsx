import { type ChangeEvent, useEffect, useState } from 'react'
import { RoleShell } from '../components/RoleShell'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { Booking, Turf } from '../types/domain'

export function BookingsPage({ role }: { role: PlatformRole }) {
  const [items, setItems] = useState<Booking[]>([])
  const [turfs, setTurfs] = useState<Turf[]>([])
  const [selectedTurfId, setSelectedTurfId] = useState('')
  const [notice, setNotice] = useState('')

  async function loadPlayerBookings() {
    try {
      setItems(await platformApi.myBookings())
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load bookings')
    }
  }

  async function loadOwnerData() {
    try {
      const result = await platformApi.myTurfs()
      setTurfs(result)
      const turfId = selectedTurfId || result[0]?.id || ''
      setSelectedTurfId(turfId)
      setItems(turfId ? await platformApi.turfBookings(turfId) : [])
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load turf bookings')
    }
  }

  useEffect(() => {
    if (role === 'PLAYER') void loadPlayerBookings()
    if (role === 'TURF_OWNER') void loadOwnerData()
  }, [role])

  async function selectTurf(turfId: string) {
    setSelectedTurfId(turfId)
    try {
      setItems(turfId ? await platformApi.turfBookings(turfId) : [])
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load turf bookings')
    }
  }

  return (
    <RoleShell role={role}>
      <section className="module-header">
        <p className="eyebrow">RESERVATIONS</p>
        <h2>{role === 'TURF_OWNER' ? 'Turf Bookings' : 'My Bookings'}</h2>
        <p>{role === 'TURF_OWNER' ? 'Review venue reservations, payment status, and check-in readiness.' : 'Track payment, confirmation, QR access, cancellation, and check-in.'}</p>
      </section>
      {notice && <p className="workspace-notice">{notice}</p>}

      {role === 'TURF_OWNER' && (
        <label className="panel event-selector">
          Turf
          <select value={selectedTurfId} onChange={(event: ChangeEvent<HTMLSelectElement>) => void selectTurf(event.target.value)}>
            <option value="">Select turf</option>
            {turfs.map((turf) => <option value={turf.id} key={turf.id}>{turf.name}</option>)}
          </select>
        </label>
      )}

      <section className="resource-grid">
        {items.length === 0 && <article className="panel empty-state"><h3>No bookings yet</h3><p>New reservations will appear here.</p></article>}
        {items.map((booking) => (
          <article className="panel booking-card" key={booking.id}>
            <span className="status-pill">{booking.status}</span>
            <h3>{booking.bookingCode}</h3>
            <p>Amount: ₹{booking.amount} · Payment: {booking.paymentStatus}</p>
            <small>Created {new Date(booking.createdAt).toLocaleString()}</small>
            {booking.qrToken && <code className="qr-token">{booking.qrToken}</code>}
            {role === 'PLAYER' && booking.status !== 'CANCELLED' && (
              <div className="button-row">
                <button onClick={() => void platformApi.qrBooking(booking.id).then((result) => setNotice(`QR token: ${result.qrToken}`)).catch((error: Error) => setNotice(error.message))}>Generate QR token</button>
                <button onClick={() => void platformApi.cancelBooking(booking.id, 'Cancelled by player').then(async () => { setNotice('Booking cancelled.'); await loadPlayerBookings() }).catch((error: Error) => setNotice(error.message))}>Cancel booking</button>
              </div>
            )}
          </article>
        ))}
      </section>
    </RoleShell>
  )
}
