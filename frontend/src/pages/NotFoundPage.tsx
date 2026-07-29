import { Link } from 'react-router-dom'
import { ArenaBackground } from '../components/ArenaBackground'
import { Brand } from '../components/Brand'

export function NotFoundPage() {
  return <div className="center-screen"><ArenaBackground /><div className="verification-card"><Brand /><div className="verification-symbol error"><span>404</span></div><p className="eyebrow">OUT OF BOUNDS</p><h1>That route is not in play.</h1><p>Return to the PlaySphere home page and choose another path.</p><Link className="button button-primary full-width" to="/">Back to home →</Link></div></div>
}
