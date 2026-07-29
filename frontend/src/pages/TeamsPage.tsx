import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { useAuth } from '../auth/AuthContext'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { Team, TeamPlayerSummary } from '../types/domain'

type JoinRequest = {
  id: string
  teamId: string
  applicantUserId: string
  message?: string
  status: string
  createdAt: string
  applicant?: TeamPlayerSummary
}

export function TeamsPage({ role }: { role: PlatformRole }) {
  const { user } = useAuth()
  const [items, setItems] = useState<Team[]>([])
  const [mine, setMine] = useState<Team[]>([])
  const [requests, setRequests] = useState<Record<string, JoinRequest[]>>({})
  const [notice, setNotice] = useState('')
  const [teamLogoUrl, setTeamLogoUrl] = useState('')
  const [uploadingLogo, setUploadingLogo] = useState(false)

  async function load() {
    try {
      setItems(await platformApi.teams())
      if (role === 'PLAYER') setMine(await platformApi.myTeams())
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load teams')
    }
  }

  useEffect(() => { void load() }, [role])

  async function uploadLogo(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    setUploadingLogo(true)
    setNotice('')
    try {
      const asset = await platformApi.uploadImage(file, 'teams')
      setTeamLogoUrl(asset.secureUrl)
      setNotice('Team logo uploaded. Create the team to save it.')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to upload team logo')
    } finally {
      setUploadingLogo(false)
      event.target.value = ''
    }
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      await platformApi.createTeam({
        name: String(form.get('name') ?? ''),
        sport: String(form.get('sport') ?? ''),
        city: String(form.get('city') ?? ''),
        locality: String(form.get('locality') ?? ''),
        skillLevel: String(form.get('skillLevel') ?? ''),
        description: String(form.get('description') ?? ''),
        logoUrl: teamLogoUrl || null,
        maxMembers: Number(form.get('maxMembers')),
        visibility: String(form.get('visibility') ?? 'PUBLIC'),
        joinMode: String(form.get('joinMode') ?? 'REQUEST_APPROVAL'),
      })
      event.currentTarget.reset()
      setTeamLogoUrl('')
      setNotice('Team created. You are now the Team Captain.')
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to create team')
    }
  }

  async function loadRequests(teamId: string) {
    try {
      const result = await platformApi.teamRequests(teamId) as JoinRequest[]
      setRequests((current) => ({ ...current, [teamId]: result }))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load requests')
    }
  }

  async function decide(requestId: string, teamId: string, accept: boolean) {
    try {
      await platformApi.decideTeamRequest(requestId, accept)
      setNotice(accept ? 'Player added to the team.' : 'Join request rejected.')
      await Promise.all([loadRequests(teamId), load()])
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to update request')
    }
  }

  async function leaveTeam(teamId: string) {
    try {
      await platformApi.leaveTeam(teamId)
      setNotice('You left the team.')
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to leave team')
    }
  }

  async function removeMember(teamId: string, userId: string) {
    try {
      await platformApi.removeTeamMember(teamId, userId)
      setNotice('Team member removed.')
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to remove team member')
    }
  }

  async function transferCaptain(teamId: string, userId: string) {
    try {
      await platformApi.transferTeamCaptain(teamId, userId)
      setNotice('Captaincy transferred successfully.')
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to transfer captaincy')
    }
  }

  const captainTeams = mine.filter((team) => team.captainUserId === user?.id)

  return (
    <RoleShell role={role}>
      <section className="module-header">
        <p className="eyebrow">TEAM NETWORK</p>
        <h2>Teams</h2>
        <p>Create a team, join another team, and manage recruitment in one place.</p>
      </section>
      {notice && <p className="workspace-notice">{notice}</p>}

      {role === 'PLAYER' && captainTeams.length > 0 && (
        <section className="panel workspace-list captain-zone">
          <h3>Captain workspace</h3>
          {captainTeams.map((team) => (
            <article className="captain-team" key={team.id}>
              <div>
                <b>{team.name}</b>
                <span>{team.sport} · {team.city} · {team.memberCount}/{team.maxMembers} members</span>
              </div>
              <button onClick={() => void loadRequests(team.id)}>Review join requests</button>
              {requests[team.id]?.length === 0 && <p className="muted-copy">No join requests yet.</p>}
              {requests[team.id]?.map((request) => (
                <div className="request-row" key={request.id}>
                  <span>
                    <b>{request.applicant?.displayName ?? request.applicantUserId.slice(0, 8)}</b>
                    <small>{request.applicant?.playingPosition || request.applicant?.skillLevel || request.message || 'No player details provided'}</small>
                  </span>
                  <i>{request.status}</i>
                  {request.status === 'PENDING' && <div><button onClick={() => void decide(request.id, team.id, true)}>Accept</button><button onClick={() => void decide(request.id, team.id, false)}>Reject</button></div>}
                </div>
              ))}
              {team.members.filter((member) => member.userId !== user?.id).map((member) => (
                <div className="request-row" key={member.id}>
                  <span><b>{member.user.displayName}</b><small>{member.user.playingPosition || member.user.skillLevel || 'Team member'}</small></span>
                  <i>{member.memberRole}</i>
                  <div>
                    <button onClick={() => void transferCaptain(team.id, member.userId)}>Make captain</button>
                    <button onClick={() => void removeMember(team.id, member.userId)}>Remove</button>
                  </div>
                </div>
              ))}
            </article>
          ))}
        </section>
      )}

      <div className="workspace-grid">
        <section className="panel workspace-list">
          <h3>Discover active teams</h3>
          {items.length === 0 && <p className="muted-copy">No active teams yet.</p>}
          {items.map((team) => {
            const myTeam = mine.find((owned) => owned.id === team.id)
            return (
              <article className="resource-card" key={team.id}>
                <div>
                  <b>{team.name}</b>
                  <span>{team.sport} · {team.city}, {team.locality} · {team.skillLevel}</span>
                  <small>{team.description || 'A PlaySphere team looking for the right players.'}</small>
                  <small>{team.memberCount}/{team.maxMembers} members · Captain: {team.captain?.displayName ?? 'Player'}</small>
                </div>
                {role === 'PLAYER' && !myTeam && (
                  <button onClick={() => void platformApi.joinTeam(team.id, 'I would like to join your team.').then(() => setNotice('Join request sent.')).catch((error: Error) => setNotice(error.message))}>Request to join</button>
                )}
                {role === 'PLAYER' && myTeam && myTeam.captainUserId !== user?.id && <div><span className="status-pill">MEMBER</span><button onClick={() => void leaveTeam(team.id)}>Leave team</button></div>}
                {role === 'PLAYER' && myTeam?.captainUserId === user?.id && <span className="status-pill">CAPTAIN</span>}
              </article>
            )
          })}
        </section>

        {role === 'PLAYER' && (
          <form className="panel workspace-form" onSubmit={create}>
            <h3>Create your team</h3>
            <label>Team name<input name="name" required maxLength={100} /></label>
            <div className="form-two-column">
              <label>Sport<input name="sport" required defaultValue="Football" /></label>
              <label>Skill level<select name="skillLevel"><option>BEGINNER</option><option>INTERMEDIATE</option><option>ADVANCED</option><option>ALL</option></select></label>
            </div>
            <div className="form-two-column">
              <label>City<input name="city" required defaultValue="Navi Mumbai" /></label>
              <label>Locality<input name="locality" required defaultValue="Vashi" /></label>
            </div>
            <label>Team logo<input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event: ChangeEvent<HTMLInputElement>) => void uploadLogo(event)} /><small>{uploadingLogo ? 'Uploading logo…' : teamLogoUrl ? 'Logo uploaded and ready.' : 'JPG, PNG or WebP, maximum 5 MB.'}</small></label>
            <label>Description<textarea name="description" maxLength={600} /></label>
            <div className="form-two-column">
              <label>Visibility<select name="visibility"><option>PUBLIC</option><option>PRIVATE</option></select></label>
              <label>Join method<select name="joinMode"><option>REQUEST_APPROVAL</option><option>OPEN</option><option>INVITE_ONLY</option></select></label>
            </div>
            <label>Maximum members<input name="maxMembers" type="number" min="2" max="100" defaultValue="10" /></label>
            <button className="primary-button" disabled={uploadingLogo}>Create team</button>
          </form>
        )}
      </div>
    </RoleShell>
  )
}
