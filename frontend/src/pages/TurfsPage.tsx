import { type ChangeEvent, useEffect, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { Turf, TurfEquipment, TurfSlot } from '../types/domain'

export function TurfsPage({ role, mode }: { role: PlatformRole; mode: string }) {
  const [items, setItems] = useState<Turf[]>([])
  const [slots, setSlots] = useState<Record<string, TurfSlot[]>>({})
  const [notice, setNotice] = useState('')
  const [equipment, setEquipment] = useState<Record<string, TurfEquipment[]>>({})
  const [selectedTurfId, setSelectedTurfId] = useState('')
  const [coverImageUrl, setCoverImageUrl] = useState('')
  const [uploadingCover, setUploadingCover] = useState(false)

  async function load() {
    try {
      setItems(role === 'TURF_OWNER' ? await platformApi.myTurfs() : await platformApi.turfs())
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load turfs')
    }
  }

  useEffect(() => { void load() }, [role])

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      await platformApi.createTurf({
        name: form.get('name'),
        description: form.get('description'),
        addressLine: form.get('addressLine'),
        city: form.get('city'),
        locality: form.get('locality'),
        latitude: form.get('latitude') ? Number(form.get('latitude')) : null,
        longitude: form.get('longitude') ? Number(form.get('longitude')) : null,
        sports: form.get('sports'),
        amenities: form.get('amenities'),
        basePrice: Number(form.get('basePrice')),
        coverImageUrl: coverImageUrl || null,
      })
      setNotice('Turf submitted for Admin approval.')
      event.currentTarget.reset()
      setCoverImageUrl('')
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to submit turf')
    }
  }


  async function uploadCoverImage(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    setUploadingCover(true)
    setNotice('')
    try {
      const asset = await platformApi.uploadImage(file, 'turfs')
      setCoverImageUrl(asset.secureUrl)
      setNotice('Turf cover image uploaded and ready.')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to upload turf image')
    } finally {
      setUploadingCover(false)
      event.target.value = ''
    }
  }

  async function createSlot(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const turfId = String(form.get('turfId'))
      await platformApi.createSlot(turfId, {
        startAt: new Date(String(form.get('startAt'))).toISOString(),
        endAt: new Date(String(form.get('endAt'))).toISOString(),
        price: Number(form.get('price')),
      })
      setNotice('Availability slot created.')
      event.currentTarget.reset()
      await showSlots(turfId, true)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to create slot')
    }
  }

  async function loadEquipment(turfId: string) {
    if (!turfId) return
    try {
      const result = role === 'TURF_OWNER'
        ? await platformApi.ownerEquipment(turfId)
        : await platformApi.equipment(turfId)
      setEquipment((current) => ({ ...current, [turfId]: result }))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load equipment')
    }
  }

  async function createEquipment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const turfId = String(form.get('turfId'))
    try {
      await platformApi.addEquipment(turfId, {
        name: form.get('name'),
        description: form.get('description'),
        quantity: Number(form.get('quantity')),
        pricePerBooking: Number(form.get('pricePerBooking')),
      })
      setNotice('Equipment item added.')
      event.currentTarget.reset()
      setSelectedTurfId(turfId)
      await loadEquipment(turfId)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to add equipment')
    }
  }

  async function toggleEquipment(item: TurfEquipment) {
    try {
      await platformApi.updateEquipment(item.turfId, item.id, {
        quantity: item.quantity,
        pricePerBooking: item.pricePerBooking,
        active: !item.active,
      })
      setNotice(item.active ? 'Equipment hidden from booking.' : 'Equipment made available.')
      await loadEquipment(item.turfId)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to update equipment')
    }
  }

  async function checkIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const booking = await platformApi.checkIn(String(form.get('bookingCode')), String(form.get('qrToken')))
      setNotice(`Check-in completed for ${booking.bookingCode}.`)
      event.currentTarget.reset()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to check in')
    }
  }

  async function showSlots(id: string, ownerView = false) {
    try {
      const result = ownerView ? await platformApi.ownerSlots(id) : await platformApi.slots(id)
      setSlots((current) => ({ ...current, [id]: result }))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load slots')
    }
  }

  async function setSlotStatus(turfId: string, slotId: string, status: 'AVAILABLE' | 'BLOCKED') {
    try {
      await platformApi.setSlotStatus(turfId, slotId, status)
      setNotice(status === 'BLOCKED' ? 'Slot blocked for maintenance.' : 'Slot reopened for booking.')
      await showSlots(turfId, true)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to update slot')
    }
  }

  if (mode === 'add-turf' && role === 'TURF_OWNER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">VENUE ONBOARDING</p><h2>Add Turf</h2><p>Submit venue, location, facilities, pricing, and media for Admin approval.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <form className="panel wide-form" onSubmit={create}>
          <h3>Submit a new turf</h3>
          <div className="form-two-column"><label>Name<input name="name" required /></label><label>Sports<input name="sports" required placeholder="Football, Cricket" /></label></div>
          <label>Address<input name="addressLine" required /></label>
          <div className="form-two-column"><label>City<input name="city" required /></label><label>Locality<input name="locality" required /></label></div>
          <div className="form-two-column"><label>Latitude<input name="latitude" type="number" step="0.0000001" /></label><label>Longitude<input name="longitude" type="number" step="0.0000001" /></label></div>
          <div className="form-two-column"><label>Base price<input name="basePrice" type="number" min="0" required /></label><label>Cover image<input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event: ChangeEvent<HTMLInputElement>) => void uploadCoverImage(event)} /><small>{uploadingCover ? 'Uploading image…' : coverImageUrl ? 'Image uploaded and ready.' : 'JPG, PNG or WebP, maximum 5 MB.'}</small></label></div>
          <label>Amenities<input name="amenities" placeholder="Parking, Lights, Washroom" /></label>
          <label>Description<textarea name="description" /></label>
          <button className="primary-button" disabled={uploadingCover}>Submit for approval</button>
        </form>
      </RoleShell>
    )
  }

  if (mode === 'availability' && role === 'TURF_OWNER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">SLOT CONTROL</p><h2>Availability</h2><p>Create bookable slots only for approved turfs.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <form className="panel wide-form" onSubmit={createSlot}>
          <div className="form-two-column">
            <label>Turf<select name="turfId" required><option value="">Select approved turf</option>{items.filter((turf) => turf.status === 'APPROVED').map((turf) => <option value={turf.id} key={turf.id}>{turf.name}</option>)}</select></label>
            <label>Price<input name="price" type="number" min="0" required /></label>
          </div>
          <div className="form-two-column"><label>Start<input name="startAt" type="datetime-local" required /></label><label>End<input name="endAt" type="datetime-local" required /></label></div>
          <button className="primary-button">Create slot</button>
        </form>
        <section className="resource-grid">{items.map((turf) => <article className="panel turf-card" key={turf.id}><span className="status-pill">{turf.status}</span><h3>{turf.name}</h3><button onClick={() => void showSlots(turf.id, true)}>Refresh slots</button>{slots[turf.id]?.map((slot) => <div className="slot-row" key={slot.id}><span>{new Date(slot.startAt).toLocaleString()}</span><b>₹{slot.price}</b><i>{slot.status}</i>{slot.status === 'AVAILABLE' && <button onClick={() => void setSlotStatus(turf.id, slot.id, 'BLOCKED')}>Block</button>}{slot.status === 'BLOCKED' && <button onClick={() => void setSlotStatus(turf.id, slot.id, 'AVAILABLE')}>Reopen</button>}</div>)}</article>)}</section>
      </RoleShell>
    )
  }

  if (mode === 'equipment' && role === 'TURF_OWNER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">EQUIPMENT & ADD-ONS</p><h2>Equipment</h2><p>Manage rentable kits and optional equipment for each turf.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <div className="workspace-grid">
          <form className="panel workspace-form" onSubmit={createEquipment}>
            <h3>Add equipment</h3>
            <label>Turf<select name="turfId" required value={selectedTurfId} onChange={(event: ChangeEvent<HTMLSelectElement>) => { setSelectedTurfId(event.target.value); void loadEquipment(event.target.value) }}><option value="">Select turf</option>{items.map((turf) => <option value={turf.id} key={turf.id}>{turf.name}</option>)}</select></label>
            <label>Name<input name="name" required maxLength={120} placeholder="Football kit" /></label>
            <label>Description<textarea name="description" maxLength={500} /></label>
            <div className="form-two-column"><label>Quantity<input name="quantity" type="number" min="0" defaultValue="1" required /></label><label>Price per booking<input name="pricePerBooking" type="number" min="0" step="0.01" defaultValue="0" required /></label></div>
            <button className="primary-button">Add equipment</button>
          </form>
          <section className="panel workspace-list">
            <h3>Inventory</h3>
            {!selectedTurfId && <p className="muted-copy">Select a turf to review its equipment.</p>}
            {selectedTurfId && (equipment[selectedTurfId] ?? []).map((item) => <article className="resource-card" key={item.id}><div><b>{item.name}</b><span>{item.quantity} available · ₹{item.pricePerBooking} per booking</span><small>{item.description || 'No description'} · {item.active ? 'ACTIVE' : 'HIDDEN'}</small></div><button onClick={() => void toggleEquipment(item)}>{item.active ? 'Hide' : 'Activate'}</button></article>)}
          </section>
        </div>
      </RoleShell>
    )
  }

  if (mode === 'check-in' && role === 'TURF_OWNER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">VENUE ACCESS</p><h2>Booking Check-In</h2><p>Validate the booking code and the player’s one-time QR token.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <form className="panel workspace-form checkin-form" onSubmit={checkIn}>
          <label>Booking code<input name="bookingCode" required placeholder="PS-..." /></label>
          <label>QR token<textarea name="qrToken" required /></label>
          <button className="primary-button">Verify and check in</button>
        </form>
      </RoleShell>
    )
  }

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">VENUES</p><h2>Turfs</h2><p>Approved venues, live availability, slot booking, and owner operations.</p></section>
      {notice && <p className="workspace-notice">{notice}</p>}
      <section className="resource-grid">
        {items.map((turf) => (
          <article className="panel turf-card" key={turf.id}>
            {turf.coverImageUrl && <img src={turf.coverImageUrl} alt="" />}
            <span className="status-pill">{turf.status}</span>
            <h3>{turf.name}</h3>
            <p>{turf.sports} · {turf.city}, {turf.locality}</p>
            <strong>₹{turf.basePrice} base price</strong>
            <small>{turf.amenities}</small>
            {turf.rejectionReason && <p className="warning-copy">{turf.rejectionReason}</p>}
            <button onClick={() => void showSlots(turf.id)}>View slots</button>
            {slots[turf.id]?.map((slot) => (
              <div className="slot-row" key={slot.id}>
                <span>{new Date(slot.startAt).toLocaleString()}</span>
                <b>₹{slot.price}</b>
                {role === 'PLAYER' ? <button onClick={() => void platformApi.book(slot.id).then((booking) => setNotice(`Booking ${booking.bookingCode} created. Pay to confirm.`)).catch((error: Error) => setNotice(error.message))}>Book</button> : <i>{slot.status}</i>}
              </div>
            ))}
          </article>
        ))}
      </section>
    </RoleShell>
  )
}
