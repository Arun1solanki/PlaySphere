import { useEffect, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { PlayerDiscovery } from '../types/domain'

export function DiscoverPage({ role }: { role: PlatformRole }) {
  const [players, setPlayers] = useState<PlayerDiscovery[]>([])
  const [notice, setNotice] = useState('')

  async function search(filters: { q?: string; city?: string; sport?: string } = {}) {
    try {
      setPlayers(await platformApi.players(filters))
      setNotice('')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to search players')
    }
  }

  useEffect(() => { void search() }, [])

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    void search({
      q: String(form.get('q') ?? '').trim() || undefined,
      city: String(form.get('city') ?? '').trim() || undefined,
      sport: String(form.get('sport') ?? '').trim() || undefined,
    })
  }

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">COMMUNITY DISCOVERY</p><h2>Find Players</h2><p>Search discoverable Player profiles by name, city, sport, skill, or position.</p></section>
      {notice && <p className="workspace-notice">{notice}</p>}
      <form className="panel discovery-search" onSubmit={submit}>
        <div className="form-three-column">
          <label>Name or position<input name="q" placeholder="Defender or player name" /></label>
          <label>City<input name="city" placeholder="Navi Mumbai" /></label>
          <label>Sport<input name="sport" placeholder="Football" /></label>
        </div>
        <button className="primary-button">Search players</button>
      </form>
      <section className="resource-grid">
        {players.map((player) => (
          <article className="panel resource-card" key={player.userId}>
            {player.profileImageUrl ? <img src={player.profileImageUrl} alt={`${player.fullName} profile`} /> : <span className="profile-avatar-fallback">{player.fullName.charAt(0)}</span>}
            <div>
              <h3>{player.fullName}</h3>
              <p>{player.city}, {player.locality}</p>
              <strong>{player.preferredSports || 'Sports not listed'}</strong>
              <small>{[player.skillLevel, player.playingPosition].filter(Boolean).join(' · ') || 'Player details not listed'}</small>
              {player.availabilitySummary && <small>{player.availabilitySummary}</small>}
              {player.bio && <p>{player.bio}</p>}
            </div>
          </article>
        ))}
        {players.length === 0 && <article className="panel empty-state"><h3>No matching players</h3><p>Try a broader city, sport, or position search.</p></article>}
      </section>
    </RoleShell>
  )
}
