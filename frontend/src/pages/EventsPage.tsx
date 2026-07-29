import { type ChangeEvent, useEffect, useMemo, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { useAuth } from '../auth/AuthContext'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { EventRegistration, Match, SportsEvent, Team } from '../types/domain'

const localDateTime = (hours: number) => {
  const value = new Date(Date.now() + hours * 3_600_000)
  value.setMinutes(value.getMinutes() - value.getTimezoneOffset())
  return value.toISOString().slice(0, 16)
}

export function EventsPage({ role, mode }: { role: PlatformRole; mode: string }) {
  const { user } = useAuth()
  const [events, setEvents] = useState<SportsEvent[]>([])
  const [registrations, setRegistrations] = useState<EventRegistration[]>([])
  const [matches, setMatches] = useState<Match[]>([])
  const [selectedEventId, setSelectedEventId] = useState('')
  const [notice, setNotice] = useState('')
  const [playerTeams, setPlayerTeams] = useState<Team[]>([])
  const [myRegistrations, setMyRegistrations] = useState<EventRegistration[]>([])
  const [teamChoice, setTeamChoice] = useState<Record<string, string>>({})
  const [bannerUrl, setBannerUrl] = useState('')
  const [uploadingBanner, setUploadingBanner] = useState(false)

  const selectedEvent = useMemo(
    () => events.find((event) => event.id === selectedEventId),
    [events, selectedEventId],
  )

  async function loadEvents() {
    try {
      const result = role === 'ORGANIZER' ? await platformApi.myEvents() : await platformApi.events()
      setEvents(result)
      if (role === 'PLAYER') {
        const [teams, registrations] = await Promise.all([
          platformApi.myTeams(),
          platformApi.myRegistrations(),
        ])
        setPlayerTeams(teams)
        setMyRegistrations(registrations)
      }
      if (!selectedEventId && result[0]) setSelectedEventId(result[0].id)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load events')
    }
  }

  useEffect(() => { void loadEvents() }, [role])

  useEffect(() => {
    if (!selectedEventId) return
    if (mode === 'registrations' || mode === 'participants') {
      void platformApi.eventRegistrations(selectedEventId)
        .then(setRegistrations)
        .catch((error: Error) => setNotice(error.message))
    }
    if (mode === 'fixtures' || mode === 'scores') {
      void platformApi.matches(selectedEventId)
        .then(setMatches)
        .catch((error: Error) => setNotice(error.message))
    }
  }, [mode, selectedEventId])

  async function createEvent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      await platformApi.createEvent({
        title: form.get('title'),
        description: form.get('description'),
        sport: form.get('sport'),
        eventType: form.get('eventType'),
        registrationType: form.get('registrationType'),
        city: form.get('city'),
        locality: form.get('locality'),
        venueName: form.get('venueName') || null,
        startAt: new Date(String(form.get('startAt'))).toISOString(),
        endAt: new Date(String(form.get('endAt'))).toISOString(),
        registrationDeadline: new Date(String(form.get('registrationDeadline'))).toISOString(),
        minPlayers: Number(form.get('minPlayers')),
        maxPlayers: Number(form.get('maxPlayers')),
        entryFee: Number(form.get('entryFee')),
        bannerUrl: bannerUrl || null,
        rules: form.get('rules'),
      })
      setNotice('Event published successfully.')
      event.currentTarget.reset()
      setBannerUrl('')
      await loadEvents()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to create event')
    }
  }


  async function uploadBanner(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    setUploadingBanner(true)
    setNotice('')
    try {
      const asset = await platformApi.uploadImage(file, 'events')
      setBannerUrl(asset.secureUrl)
      setNotice('Event banner uploaded and ready.')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to upload event banner')
    } finally {
      setUploadingBanner(false)
      event.target.value = ''
    }
  }

  async function registerForEvent(item: SportsEvent) {
    try {
      const chosenTeam = item.registrationType === 'TEAM' ? teamChoice[item.id] : undefined
      if (item.registrationType === 'TEAM' && !chosenTeam) {
        setNotice('Choose one of your captain teams before registering.')
        return
      }
      const registration = await platformApi.registerEvent(item.id, chosenTeam)
      setNotice(registration.paymentStatus === 'PENDING'
        ? `Registered. Pay registration ${registration.id} to confirm.`
        : 'Event registration completed.')
      setMyRegistrations(await platformApi.myRegistrations())
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to register for event')
    }
  }

  async function leaveEvent(eventId: string) {
    try {
      await platformApi.leaveEvent(eventId)
      setNotice('You left the event.')
      setMyRegistrations(await platformApi.myRegistrations())
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to leave event')
    }
  }

  async function cancelEvent(eventId: string) {
    const reason = window.prompt('Why are you cancelling this event?', 'Operational reason')
    if (!reason) return
    try {
      await platformApi.cancelEvent(eventId, reason)
      setNotice('Event cancelled and registered players notified.')
      await loadEvents()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to cancel event')
    }
  }

  async function createMatch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedEventId) return
    const form = new FormData(event.currentTarget)
    try {
      await platformApi.createMatch(selectedEventId, {
        title: form.get('title'),
        homeName: form.get('homeName'),
        awayName: form.get('awayName'),
        scheduledAt: new Date(String(form.get('scheduledAt'))).toISOString(),
        venue: form.get('venue') || null,
      })
      setNotice('Fixture created.')
      event.currentTarget.reset()
      setMatches(await platformApi.matches(selectedEventId))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to create fixture')
    }
  }

  async function score(event: FormEvent<HTMLFormElement>, matchId: string) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      await platformApi.scoreMatch(
        matchId,
        Number(form.get('homeScore')),
        Number(form.get('awayScore')),
      )
      setNotice('Match result published.')
      setMatches(await platformApi.matches(selectedEventId))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to publish score')
    }
  }

  const eventSelector = (
    <label className="event-selector">
      Event
      <select value={selectedEventId} onChange={(event: ChangeEvent<HTMLSelectElement>) => setSelectedEventId(event.target.value)}>
        <option value="">Select event</option>
        {events.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}
      </select>
    </label>
  )

  if (mode === 'create-event' && role === 'ORGANIZER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">EVENT OPERATIONS</p><h2>Create Event</h2><p>Publish a validated event and automatically open its participant conversation.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <form className="panel wide-form" onSubmit={createEvent}>
          <h3>Event details</h3>
          <div className="form-two-column"><label>Title<input name="title" required maxLength={160} /></label><label>Sport<input name="sport" required defaultValue="Football" /></label></div>
          <div className="form-two-column"><label>Type<select name="eventType"><option value="FRIENDLY">Friendly</option><option value="TOURNAMENT">Tournament</option><option value="LEAGUE">League</option></select></label><label>Registration<select name="registrationType"><option value="INDIVIDUAL">Individual</option><option value="TEAM">Team</option></select></label></div>
          <div className="form-two-column"><label>City<input name="city" required defaultValue="Navi Mumbai" /></label><label>Locality<input name="locality" required defaultValue="Vashi" /></label></div>
          <div className="form-two-column"><label>Venue<input name="venueName" /></label><label>Event banner<input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event: ChangeEvent<HTMLInputElement>) => void uploadBanner(event)} /><small>{uploadingBanner ? 'Uploading banner…' : bannerUrl ? 'Banner uploaded and ready.' : 'JPG, PNG or WebP, maximum 5 MB.'}</small></label></div>
          <div className="form-two-column"><label>Start<input name="startAt" type="datetime-local" defaultValue={localDateTime(72)} required /></label><label>End<input name="endAt" type="datetime-local" defaultValue={localDateTime(74)} required /></label></div>
          <label>Registration deadline<input name="registrationDeadline" type="datetime-local" defaultValue={localDateTime(48)} required /></label>
          <div className="form-three-column"><label>Minimum players<input name="minPlayers" type="number" min="1" defaultValue="5" required /></label><label>Maximum players<input name="maxPlayers" type="number" min="5" defaultValue="20" required /></label><label>Entry fee<input name="entryFee" type="number" min="0" step="0.01" defaultValue="100" required /></label></div>
          <label>Description<textarea name="description" maxLength={1200} /></label>
          <label>Rules<textarea name="rules" maxLength={1500} /></label>
          <button className="primary-button" disabled={uploadingBanner}>Publish event</button>
        </form>
      </RoleShell>
    )
  }

  if ((mode === 'registrations' || mode === 'participants') && role === 'ORGANIZER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">PARTICIPATION</p><h2>{mode === 'participants' ? 'Participants' : 'Registrations'}</h2><p>Review individual or team entries and their payment status.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <section className="panel workspace-list">{eventSelector}<h3>{selectedEvent?.title ?? 'Select an event'}</h3>{registrations.length === 0 && <p className="muted-copy">No registrations are available.</p>}{registrations.map((registration) => <article className="resource-card" key={registration.id}><div><b>{registration.teamId ? `Team ${registration.teamId.slice(0, 8)}` : `Player ${registration.userId.slice(0, 8)}`}</b><span>{registration.status} · Payment {registration.paymentStatus}</span><small>{new Date(registration.createdAt).toLocaleString()}</small></div></article>)}</section>
      </RoleShell>
    )
  }

  if ((mode === 'fixtures' || mode === 'scores') && role === 'ORGANIZER') {
    return (
      <RoleShell role={role}>
        <section className="module-header"><p className="eyebrow">MATCH CONTROL</p><h2>{mode === 'scores' ? 'Scores & Results' : 'Fixtures'}</h2><p>Schedule matches and publish verified final scores.</p></section>
        {notice && <p className="workspace-notice">{notice}</p>}
        <div className="workspace-grid">
          <section className="panel workspace-list">{eventSelector}<h3>Match schedule</h3>{matches.map((match) => <article className="match-row" key={match.id}><div><b>{match.title}</b><span>{match.homeName} vs {match.awayName}</span><small>{new Date(match.scheduledAt).toLocaleString()} · {match.venue || 'Venue pending'}</small></div>{mode === 'scores' ? <form className="score-form" onSubmit={(event: FormEvent<HTMLFormElement>) => void score(event, match.id)}><input name="homeScore" type="number" min="0" defaultValue={match.homeScore ?? 0} aria-label="Home score" /><span>–</span><input name="awayScore" type="number" min="0" defaultValue={match.awayScore ?? 0} aria-label="Away score" /><button>Publish</button></form> : <strong>{match.status}</strong>}</article>)}</section>
          {mode === 'fixtures' && <form className="panel workspace-form" onSubmit={createMatch}><h3>Create fixture</h3><label>Title<input name="title" required /></label><label>Home team/player<input name="homeName" required /></label><label>Away team/player<input name="awayName" required /></label><label>Scheduled time<input name="scheduledAt" type="datetime-local" defaultValue={localDateTime(96)} required /></label><label>Venue<input name="venue" /></label><button className="primary-button" disabled={!selectedEventId}>Schedule match</button></form>}
        </div>
      </RoleShell>
    )
  }

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">EVENTS & MATCHES</p><h2>{role === 'ORGANIZER' ? 'My Events' : 'Discover Events'}</h2><p>Join activities, track event details, and view published match schedules.</p></section>
      {notice && <p className="workspace-notice">{notice}</p>}
      <section className="resource-grid">
        {events.map((item) => (
          <article className="panel event-card" key={item.id}>
            {item.bannerUrl && <img src={item.bannerUrl} alt="" />}
            <span className="status-pill">{item.status}</span>
            <h3>{item.title}</h3>
            <p>{item.sport} · {item.city}, {item.locality}</p>
            <strong>{new Date(item.startAt).toLocaleString()}</strong>
            <small>{item.registrationType} · {item.maxPlayers} capacity · ₹{item.entryFee}</small>
            <p>{item.description}</p>
            {role === 'PLAYER' && item.registrationType === 'TEAM' && !myRegistrations.some((registration) => registration.eventId === item.id && registration.status !== 'CANCELLED') && (
              <select
                value={teamChoice[item.id] ?? ''}
                onChange={(event: ChangeEvent<HTMLSelectElement>) => setTeamChoice((current) => ({ ...current, [item.id]: event.target.value }))}
                aria-label={`Team for ${item.title}`}
              >
                <option value="">Choose captain team</option>
                {playerTeams.filter((team) => team.captainUserId === user?.id && team.sport.toLowerCase() === item.sport.toLowerCase()).map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
              </select>
            )}
            {role === 'PLAYER' && item.status === 'PUBLISHED' && !myRegistrations.some((registration) => registration.eventId === item.id && registration.status !== 'CANCELLED') && <button className="primary-button" onClick={() => void registerForEvent(item)}>Join event</button>}
            {role === 'PLAYER' && myRegistrations.some((registration) => registration.eventId === item.id && registration.status !== 'CANCELLED') && <button className="danger-button" onClick={() => void leaveEvent(item.id)}>Leave event</button>}
            {role === 'ORGANIZER' && item.status === 'PUBLISHED' && <button className="danger-button" onClick={() => void cancelEvent(item.id)}>Cancel event</button>}
            <button onClick={() => void platformApi.matches(item.id).then((result) => setNotice(result.length ? `${result.length} fixture(s) published.` : 'No fixtures published yet.')).catch((error: Error) => setNotice(error.message))}>View fixtures</button>
          </article>
        ))}
      </section>
    </RoleShell>
  )
}
