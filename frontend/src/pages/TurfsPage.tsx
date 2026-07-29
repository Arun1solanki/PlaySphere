import { type ChangeEvent, useEffect, useMemo, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { GeocodingResult, NearbyTurf, Turf, TurfEquipment, TurfSlot } from '../types/domain'

type SelectedLocation = {
  latitude: number
  longitude: number
  displayName: string
}

const localDate = (days: number) => {
  const value = new Date()
  value.setDate(value.getDate() + days)
  value.setMinutes(value.getMinutes() - value.getTimezoneOffset())
  return value.toISOString().slice(0, 10)
}

function osmEmbedUrl(latitude: number, longitude: number) {
  const delta = 0.006
  const bbox = [longitude - delta, latitude - delta, longitude + delta, latitude + delta]
    .map((value) => value.toFixed(6))
    .join('%2C')
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${latitude}%2C${longitude}`
}

export function TurfsPage({ role, mode }: { role: PlatformRole; mode: string }) {
  const [items, setItems] = useState<Turf[]>([])
  const [slots, setSlots] = useState<Record<string, TurfSlot[]>>({})
  const [slotLoaded, setSlotLoaded] = useState<Record<string, boolean>>({})
  const [notice, setNotice] = useState('')
  const [equipment, setEquipment] = useState<Record<string, TurfEquipment[]>>({})
  const [selectedTurfId, setSelectedTurfId] = useState('')
  const [coverImageUrl, setCoverImageUrl] = useState('')
  const [uploadingCover, setUploadingCover] = useState(false)
  const [locationQuery, setLocationQuery] = useState('')
  const [locationResults, setLocationResults] = useState<GeocodingResult[]>([])
  const [selectedLocation, setSelectedLocation] = useState<SelectedLocation | null>(null)
  const [addressLine, setAddressLine] = useState('')
  const [city, setCity] = useState('')
  const [locality, setLocality] = useState('')
  const [mapBusy, setMapBusy] = useState(false)
  const [nearby, setNearby] = useState<NearbyTurf[] | null>(null)
  const [nearbyBusy, setNearbyBusy] = useState(false)

  const approvedTurfs = useMemo(
    () => items.filter((turf) => turf.status === 'APPROVED'),
    [items],
  )

  async function load() {
    try {
      const result = role === 'TURF_OWNER' ? await platformApi.myTurfs() : await platformApi.turfs()
      setItems(result)
      if (mode === 'availability' && role === 'TURF_OWNER') {
        await Promise.all(
          result
            .filter((turf) => turf.status === 'APPROVED')
            .map((turf) => showSlots(turf.id, true)),
        )
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load turfs')
    }
  }

  useEffect(() => { void load() }, [role, mode])

  function applyLocation(result: GeocodingResult) {
    setSelectedLocation({
      latitude: Number(result.latitude),
      longitude: Number(result.longitude),
      displayName: result.displayName,
    })
    setAddressLine(result.addressLine || result.displayName)
    setCity(result.city || '')
    setLocality(result.locality || '')
    setLocationQuery(result.displayName)
    setLocationResults([])
    setNotice('Map location selected. Review the address details before submitting.')
  }

  async function searchMap() {
    if (locationQuery.trim().length < 3) {
      setNotice('Enter at least 3 characters to search for the turf location.')
      return
    }
    setMapBusy(true)
    setNotice('')
    try {
      const results = await platformApi.searchLocations(locationQuery.trim())
      setLocationResults(results)
      if (results.length === 0) setNotice('No map results found. Try a landmark, road, or locality name.')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to search the map')
    } finally {
      setMapBusy(false)
    }
  }

  function useCurrentLocation(forNearby = false) {
    if (!navigator.geolocation) {
      setNotice('Location access is not supported by this browser.')
      return
    }
    if (forNearby) setNearbyBusy(true)
    else setMapBusy(true)
    setNotice('Requesting your location…')

    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        try {
          if (forNearby) {
            const result = await platformApi.nearbyTurfs(coords.latitude, coords.longitude, 20)
            setNearby(result)
            setNotice(result.length
              ? `Showing ${result.length} approved turf(s) within 20 km.`
              : 'No approved turfs were found within 20 km. Showing all turfs below.')
          } else {
            // Preserve the browser coordinates immediately. Reverse geocoding is
            // helpful for the address, but it must not make location selection fail.
            setSelectedLocation({
              latitude: coords.latitude,
              longitude: coords.longitude,
              displayName: 'Current browser location',
            })
            setLocationResults([])
            try {
              const result = await platformApi.reverseLocation(coords.latitude, coords.longitude)
              applyLocation(result)
            } catch {
              setLocationQuery('Current browser location')
              setNotice('Location selected. Enter the address, city, and locality before submitting.')
            }
          }
        } catch (error) {
          setNotice(error instanceof Error ? error.message : 'Unable to use this location')
        } finally {
          setNearbyBusy(false)
          setMapBusy(false)
        }
      },
      (error) => {
        setNearbyBusy(false)
        setMapBusy(false)
        setNotice(error.code === error.PERMISSION_DENIED
          ? 'Location permission was denied. Search for the address instead.'
          : 'Unable to read your current location. Search for the address instead.')
      },
      { enableHighAccuracy: true, timeout: 12_000, maximumAge: 60_000 },
    )
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    if (!selectedLocation) {
      setNotice('Select the turf location from the map before submitting.')
      return
    }
    try {
      await platformApi.createTurf({
        name: form.get('name'),
        description: form.get('description'),
        addressLine,
        city,
        locality,
        latitude: selectedLocation.latitude,
        longitude: selectedLocation.longitude,
        sports: form.get('sports'),
        amenities: form.get('amenities'),
        basePrice: Number(form.get('basePrice')),
        coverImageUrl: coverImageUrl || null,
      })
      setNotice('Turf submitted for Admin approval. The Admin team and your notification centre were updated.')
      formElement.reset()
      setCoverImageUrl('')
      setSelectedLocation(null)
      setAddressLine('')
      setCity('')
      setLocality('')
      setLocationQuery('')
      setLocationResults([])
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
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      const turfId = String(form.get('turfId'))
      await platformApi.createSlot(turfId, {
        startAt: new Date(String(form.get('startAt'))).toISOString(),
        endAt: new Date(String(form.get('endAt'))).toISOString(),
        price: Number(form.get('price')),
      })
      setNotice('Availability slot created and visible to Players.')
      formElement.reset()
      await showSlots(turfId, true)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to create slot')
    }
  }

  async function generateSlots(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const turfId = String(form.get('turfId'))
    try {
      const created = await platformApi.generateSlots(turfId, {
        startDate: form.get('startDate'),
        endDate: form.get('endDate'),
        openingTime: form.get('openingTime'),
        closingTime: form.get('closingTime'),
        slotMinutes: Number(form.get('slotMinutes')),
        price: Number(form.get('price')),
      })
      setNotice(`${created.length} bookable slot(s) generated.`)
      formElement.reset()
      await showSlots(turfId, true)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to generate slots')
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
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const turfId = String(form.get('turfId'))
    try {
      await platformApi.addEquipment(turfId, {
        name: form.get('name'),
        description: form.get('description'),
        quantity: Number(form.get('quantity')),
        pricePerBooking: Number(form.get('pricePerBooking')),
      })
      setNotice('Equipment item added.')
      formElement.reset()
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
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      const booking = await platformApi.checkIn(String(form.get('bookingCode')), String(form.get('qrToken')))
      setNotice(`Check-in completed for ${booking.bookingCode}.`)
      formElement.reset()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to check in')
    }
  }

  async function showSlots(id: string, ownerView = false) {
    try {
      const result = ownerView ? await platformApi.ownerSlots(id) : await platformApi.slots(id)
      setSlots((current) => ({ ...current, [id]: result }))
      setSlotLoaded((current) => ({ ...current, [id]: true }))
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
        <section className="module-header"><p className="eyebrow">VENUE ONBOARDING</p><h2>Add Turf</h2><p>Submit venue, map location, facilities, pricing, and media for Admin approval.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <form className="panel wide-form" onSubmit={create}>
          <h3>Submit a new turf</h3>
          <div className="form-two-column"><label>Name<input name="name" required /></label><label>Sports<input name="sports" required placeholder="Football, Cricket" /></label></div>

          <div className="map-location-panel">
            <div className="map-search-row">
              <label>Search location<input value={locationQuery} onChange={(event: ChangeEvent<HTMLInputElement>) => setLocationQuery(event.target.value)} placeholder="Turf name, road, landmark, or locality" /></label>
              <button type="button" onClick={() => void searchMap()} disabled={mapBusy}>{mapBusy ? 'Searching…' : 'Search map'}</button>
              <button type="button" className="secondary-button" onClick={() => useCurrentLocation(false)} disabled={mapBusy}>Use current location</button>
            </div>
            {locationResults.length > 0 && <div className="map-search-results">{locationResults.map((result) => <button type="button" key={`${result.latitude}-${result.longitude}`} onClick={() => applyLocation(result)}><strong>{result.displayName}</strong><small>{result.city}{result.locality ? ` · ${result.locality}` : ''}</small></button>)}</div>}
            {selectedLocation && <div className="map-preview"><iframe title="Selected turf location" src={osmEmbedUrl(selectedLocation.latitude, selectedLocation.longitude)} loading="lazy" /><div><strong>Selected location</strong><span>{selectedLocation.displayName}</span><small>Coordinates are stored automatically and are not entered manually.</small></div></div>}
          </div>

          <label>Address<input name="addressLine" required value={addressLine} onChange={(event: ChangeEvent<HTMLInputElement>) => setAddressLine(event.target.value)} /></label>
          <div className="form-two-column"><label>City<input name="city" required value={city} onChange={(event: ChangeEvent<HTMLInputElement>) => setCity(event.target.value)} /></label><label>Locality<input name="locality" required value={locality} onChange={(event: ChangeEvent<HTMLInputElement>) => setLocality(event.target.value)} /></label></div>
          <div className="form-two-column"><label>Base price<input name="basePrice" type="number" min="0" required /></label><label>Cover image<input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event: ChangeEvent<HTMLInputElement>) => void uploadCoverImage(event)} /><small>{uploadingCover ? 'Uploading image…' : coverImageUrl ? 'Image uploaded and ready.' : 'JPG, PNG or WebP, maximum 5 MB.'}</small></label></div>
          {coverImageUrl && <div className="uploaded-image-preview"><img src={coverImageUrl} alt="Turf cover preview" /></div>}
          <label>Amenities<input name="amenities" placeholder="Parking, Lights, Washroom" /></label>
          <label>Description<textarea name="description" /></label>
          <button className="primary-button" disabled={uploadingCover || mapBusy}>Submit for approval</button>
        </form>
      </RoleShell>
    )
  }

  if (mode === 'availability' && role === 'TURF_OWNER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">SLOT CONTROL</p><h2>Availability</h2><p>Create one slot or generate a complete schedule for approved turfs.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        {approvedTurfs.length === 0 && <section className="panel empty-state-panel"><h3>No approved turf yet</h3><p>Create a turf and wait for Admin approval before publishing availability.</p></section>}
        {approvedTurfs.length > 0 && <div className="workspace-grid availability-workspace">
          <form className="panel wide-form" onSubmit={generateSlots}>
            <h3>Generate regular slots</h3>
            <label>Turf<select name="turfId" required><option value="">Select approved turf</option>{approvedTurfs.map((turf) => <option value={turf.id} key={turf.id}>{turf.name}</option>)}</select></label>
            <div className="form-two-column"><label>From date<input name="startDate" type="date" min={localDate(0)} defaultValue={localDate(1)} required /></label><label>To date<input name="endDate" type="date" min={localDate(0)} defaultValue={localDate(7)} required /></label></div>
            <div className="form-three-column"><label>Opening time<input name="openingTime" type="time" defaultValue="06:00" required /></label><label>Closing time<input name="closingTime" type="time" defaultValue="23:00" required /></label><label>Slot length<select name="slotMinutes" defaultValue="60"><option value="30">30 minutes</option><option value="60">60 minutes</option><option value="90">90 minutes</option><option value="120">120 minutes</option></select></label></div>
            <label>Price per slot<input name="price" type="number" min="0" defaultValue="1000" required /></label>
            <button className="primary-button">Generate availability</button>
          </form>
          <form className="panel wide-form" onSubmit={createSlot}>
            <h3>Create one custom slot</h3>
            <div className="form-two-column"><label>Turf<select name="turfId" required><option value="">Select approved turf</option>{approvedTurfs.map((turf) => <option value={turf.id} key={turf.id}>{turf.name}</option>)}</select></label><label>Price<input name="price" type="number" min="0" required /></label></div>
            <div className="form-two-column"><label>Start<input name="startAt" type="datetime-local" required /></label><label>End<input name="endAt" type="datetime-local" required /></label></div>
            <button className="primary-button">Create slot</button>
          </form>
        </div>}
        <section className="resource-grid">{items.map((turf) => <article className="panel turf-card" key={turf.id}><span className="status-pill">{turf.status}</span><h3>{turf.name}</h3>{turf.status !== 'APPROVED' && <p className="muted-copy">Availability can be published after Admin approval.</p>}{turf.status === 'APPROVED' && <button onClick={() => void showSlots(turf.id, true)}>Refresh slots</button>}{slotLoaded[turf.id] && (slots[turf.id]?.length ?? 0) === 0 && <p className="empty-slot-copy">No slots yet. Generate availability above.</p>}{slots[turf.id]?.map((slot) => <div className="slot-row" key={slot.id}><span>{new Date(slot.startAt).toLocaleString()}</span><b>₹{slot.price}</b><i>{slot.status}</i>{slot.status === 'AVAILABLE' && <button onClick={() => void setSlotStatus(turf.id, slot.id, 'BLOCKED')}>Block</button>}{slot.status === 'BLOCKED' && <button onClick={() => void setSlotStatus(turf.id, slot.id, 'AVAILABLE')}>Reopen</button>}</div>)}</article>)}</section>
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

  const displayedItems = nearby && nearby.length > 0 ? nearby.map((entry) => entry.turf) : items
  const distanceByTurf = new Map((nearby ?? []).map((entry) => [entry.turf.id, entry.distanceKm]))

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">VENUES</p><h2>Turfs</h2><p>Approved venues, nearby discovery, live availability, and slot booking.</p></section>
      {notice && <p className="workspace-notice">{notice}</p>}
      {role === 'PLAYER' && <div className="nearby-toolbar"><button className="primary-button" onClick={() => useCurrentLocation(true)} disabled={nearbyBusy}>{nearbyBusy ? 'Finding nearby turfs…' : 'Find turfs near me'}</button>{nearby && <button onClick={() => { setNearby(null); setNotice('Showing all approved turfs.') }}>Show all turfs</button>}</div>}
      <section className="resource-grid">
        {displayedItems.map((turf) => (
          <article className="panel turf-card" key={turf.id}>
            {turf.coverImageUrl && <img src={turf.coverImageUrl} alt={`${turf.name} cover`} />}
            <span className="status-pill">{turf.status}</span>
            {distanceByTurf.has(turf.id) && <span className="distance-pill">{distanceByTurf.get(turf.id)?.toFixed(1)} km away</span>}
            <h3>{turf.name}</h3>
            <p>{turf.sports} · {turf.city}, {turf.locality}</p>
            <strong>₹{turf.basePrice} base price</strong>
            <small>{turf.amenities}</small>
            {turf.rejectionReason && <p className="warning-copy">{turf.rejectionReason}</p>}
            {turf.latitude != null && turf.longitude != null && <a className="inline-map-link" href={`https://www.openstreetmap.org/?mlat=${turf.latitude}&mlon=${turf.longitude}#map=17/${turf.latitude}/${turf.longitude}`} target="_blank" rel="noreferrer">Open location on map</a>}
            <button onClick={() => void showSlots(turf.id)}>View available slots</button>
            {slotLoaded[turf.id] && (slots[turf.id]?.length ?? 0) === 0 && <p className="empty-slot-copy">No bookable slots have been published by the Turf Owner yet.</p>}
            {slots[turf.id]?.map((slot) => (
              <div className="slot-row" key={slot.id}>
                <span>{new Date(slot.startAt).toLocaleString()}</span>
                <b>₹{slot.price}</b>
                {role === 'PLAYER' ? <button onClick={() => void platformApi.book(slot.id).then((booking) => setNotice(`Booking ${booking.bookingCode} created. Complete payment to confirm it.`)).catch((error: Error) => setNotice(error.message))}>Book</button> : <i>{slot.status}</i>}
              </div>
            ))}
          </article>
        ))}
      </section>
    </RoleShell>
  )
}
