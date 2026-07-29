import { Link } from 'react-router-dom'
import { ArenaBackground } from '../components/ArenaBackground'
import { PublicHeader } from '../components/PublicHeader'
import { publicPreviewRoles, roleThemes } from '../config/roleConfig'

const platformFeatures = [
  { id: 'teams', icon: '◉', title: 'Build the right team', text: 'Create a team, accept join requests, or answer a Need Players post.' },
  { id: 'turfs', icon: '⌖', title: 'Book verified turfs', text: 'Find open slots, complete payment, and receive a secure booking QR.' },
  { id: 'events', icon: '◫', title: 'Compete and organize', text: 'Discover matches or manage registrations, fixtures, scores, and results.' },
]

export function LandingPage() {
  return (
    <div className="landing-page">
      <ArenaBackground />
      <PublicHeader />
      <main>
        <section className="hero" id="discover">
          <div className="hero-copy">
            <p className="eyebrow"><span>⌁</span> ONE PLATFORM. EVERY ARENA.</p>
            <h1>Meet your squad.<br /><span>Own the moment.</span></h1>
            <p className="hero-description">
              Discover sports and esports, find reliable players, book verified turfs,
              and turn the next match into something bigger.
            </p>
            <div className="hero-actions">
              <Link className="button button-primary" to="/register">Start playing <span>→</span></Link>
              <a className="button button-secondary" href="#role-previews">Explore role views</a>
            </div>
            <div className="hero-metrics">
              <div><strong>1.8K</strong><span>Active players</span></div>
              <div><strong>38</strong><span>Upcoming events</span></div>
              <div><strong>24</strong><span>Partner arenas</span></div>
            </div>
          </div>

          <div className="hero-visual" aria-label="Live PlaySphere activity">
            <div className="visual-orbit"><span /><span /><span /></div>
            <div className="live-label"><i /><i /><i /> LIVE ACTIVITY</div>
            <article className="activity-card activity-card-one">
              <div className="activity-card-head"><small>TEAM SEEKING PLAYERS</small><span className="live-dot" /></div>
              <h3>Vashi Strikers</h3><p>Football · 2 defenders needed</p><a href="#need-players">View request →</a>
            </article>
            <article className="activity-card activity-card-two">
              <small>UP NEXT</small><h3>Friday Night Clash</h3><p>5v5 Football · 3 days left</p><a href="#events">Join event →</a>
            </article>
            <div className="score-ribbon"><span>LIVE</span><strong>2 — 1</strong><small>67'</small></div>
          </div>
        </section>

        <section className="feature-section" id="teams">
          <div className="section-heading"><p>PLAYSPHERE CORE</p><h2>Less searching. More playing.</h2><span>Every feature is built around a clear next action.</span></div>
          <div className="feature-grid">
            {platformFeatures.map((feature) => (
              <article key={feature.id} className="feature-card" id={feature.id}>
                <span className="feature-icon">{feature.icon}</span><h3>{feature.title}</h3><p>{feature.text}</p><a href={`#${feature.id}`}>Explore module →</a>
              </article>
            ))}
          </div>
        </section>

        <section className="role-preview-section" id="role-previews">
          <div className="section-heading"><p>ROLE-SPECIFIC EXPERIENCES</p><h2>One platform. Four focused workspaces.</h2><span>The structure stays familiar while colors, navigation, and priorities change by role.</span></div>
          <div className="role-preview-grid">
            {publicPreviewRoles.map((role) => {
              const theme = roleThemes[role]
              return (
                <article
                  key={role}
                  className="role-preview-card"
                  style={{ '--preview-accent': theme.accent, '--preview-soft': theme.accentSoft } as React.CSSProperties}
                >
                  <div className="role-preview-top"><span>{theme.eyebrow}</span><i /></div>
                  <h3>{theme.label}</h3><p>{theme.subtitle}</p>
                  <div className="role-mini-nav">{theme.nav.slice(0, 4).map((item) => <span key={item.path}>{item.icon} {item.label}</span>)}</div>
                  <strong>Login required →</strong>
                </article>
              )
            })}
          </div>
        </section>

        <section className="cta-band" id="need-players">
          <div><p>READY FOR THE NEXT MATCH?</p><h2>Your team could be one request away.</h2></div>
          <Link className="button button-primary" to="/register">Create PlaySphere account →</Link>
        </section>
      </main>
      <footer className="landing-footer"><span>© 2026 PlaySphere</span><span>Teams · Turfs · Events · Community</span></footer>
    </div>
  )
}
