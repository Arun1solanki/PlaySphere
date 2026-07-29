import { useEffect, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { useAuth } from '../auth/AuthContext'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { RecruitmentPost, Team } from '../types/domain'

type Application = {
  id: string
  postId: string
  applicantUserId: string
  message?: string
  status: string
  createdAt: string
  applicant?: {
    displayName: string
    profileImageUrl?: string
    city?: string
    locality?: string
    skillLevel?: string
    playingPosition?: string
  }
}

export function RecruitmentPage({ role }: { role: PlatformRole }) {
  const { user } = useAuth()
  const [posts, setPosts] = useState<RecruitmentPost[]>([])
  const [teams, setTeams] = useState<Team[]>([])
  const [applications, setApplications] = useState<Record<string, Application[]>>({})
  const [notice, setNotice] = useState('')

  async function load() {
    try {
      setPosts(await platformApi.posts())
      if (role === 'PLAYER') setTeams(await platformApi.myTeams())
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load recruitment posts')
    }
  }

  useEffect(() => { void load() }, [role])

  async function createPost(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      await platformApi.createPost({
        teamId: form.get('teamId'),
        title: form.get('title'),
        positionsNeeded: form.get('positionsNeeded'),
        playersNeeded: Number(form.get('playersNeeded')),
        skillLevel: form.get('skillLevel'),
        description: form.get('description'),
        applicationDeadline: form.get('applicationDeadline') ? new Date(String(form.get('applicationDeadline'))).toISOString() : null,
      })
      formElement.reset()
      setNotice('Need Players post published.')
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to publish post')
    }
  }

  async function loadApplications(postId: string) {
    try {
      const result = await platformApi.postApplications(postId) as Application[]
      setApplications((current) => ({ ...current, [postId]: result }))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Only the Team Captain can view these applications')
    }
  }

  async function decide(applicationId: string, postId: string, accept: boolean) {
    try {
      await platformApi.decideApplication(applicationId, accept)
      setNotice(accept ? 'Application approved and player added.' : 'Application rejected.')
      await loadApplications(postId)
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to update application')
    }
  }

  return (
    <RoleShell role={role}>
      <section className="module-header">
        <p className="eyebrow">RECRUITMENT</p>
        <h2>Need Players</h2>
        <p>Captains publish requirements; interested players apply and wait for approval.</p>
      </section>
      {notice && <p className="workspace-notice">{notice}</p>}

      {role === 'PLAYER' && teams.some((team) => team.captainUserId === user?.id) && (
        <form className="panel wide-form recruitment-form" onSubmit={createPost}>
          <h3>Publish a Need Players post</h3>
          <div className="form-two-column">
            <label>Team<select name="teamId" required><option value="">Select a team</option>{teams.filter((team) => team.captainUserId === user?.id).map((team) => <option value={team.id} key={team.id}>{team.name}</option>)}</select></label>
            <label>Title<input name="title" required placeholder="Need two defenders for weekend league" /></label>
          </div>
          <div className="form-three-column">
            <label>Positions<input name="positionsNeeded" required placeholder="Defender, Goalkeeper" /></label>
            <label>Players needed<input name="playersNeeded" type="number" min="1" max="50" defaultValue="2" /></label>
            <label>Skill level<select name="skillLevel"><option>BEGINNER</option><option>INTERMEDIATE</option><option>ADVANCED</option><option>ALL</option></select></label>
          </div>
          <label>Application deadline<input name="applicationDeadline" type="datetime-local" /></label>
          <label>Description<textarea name="description" maxLength={700} /></label>
          <button className="primary-button">Publish requirement</button>
        </form>
      )}

      <section className="resource-grid">
        {posts.map((post) => (
          <article className="panel recruitment-card" key={post.id}>
            <span className="status-pill">{post.status}</span>
            <h3>{post.title}</h3>
            <p>{post.sport} · {post.city}, {post.locality}</p>
            <strong>{post.playersNeeded} player(s) needed</strong>
            <small>{post.positionsNeeded} · {post.skillLevel}</small>
            <p>{post.description}</p>
            {role === 'PLAYER' && !teams.some((team) => team.id === post.teamId && team.captainUserId === user?.id) && <button className="primary-button" onClick={() => void platformApi.applyPost(post.id, 'I am interested and available.').then(() => setNotice('Application submitted.')).catch((error: Error) => setNotice(error.message))}>Apply to join</button>}
            {role === 'PLAYER' && teams.some((team) => team.id === post.teamId && team.captainUserId === user?.id) && <button onClick={() => void loadApplications(post.id)}>Review applications</button>}
            {applications[post.id]?.map((application) => (
              <div className="request-row" key={application.id}>
                <span><b>{application.applicant?.displayName ?? application.applicantUserId.slice(0, 8)}</b><small>{[application.applicant?.skillLevel, application.applicant?.playingPosition, application.applicant?.locality].filter(Boolean).join(' · ') || 'Player profile'}</small><small>{application.message || 'No message'}</small></span>
                <i>{application.status}</i>
                {application.status === 'PENDING' && <div><button onClick={() => void decide(application.id, post.id, true)}>Accept</button><button onClick={() => void decide(application.id, post.id, false)}>Reject</button></div>}
              </div>
            ))}
          </article>
        ))}
      </section>
    </RoleShell>
  )
}
