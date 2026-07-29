import { NavLink } from 'react-router-dom'
import { Brand } from './Brand'

const nav = [
  ['Discover', '/#discover'],
  ['Events', '/#events'],
  ['Teams', '/#teams'],
  ['Need Players', '/#need-players'],
  ['Turfs', '/#turfs'],
  ['Scores', '/#scores'],
]

export function PublicHeader() {
  return (
    <header className="public-header">
      <Brand />
      <nav className="public-nav" aria-label="Main navigation">
        {nav.map(([label, path], index) => (
          <a href={path} key={label} className={index === 0 ? 'active' : ''}>
            <span className="nav-dot">{index === 0 ? '◎' : '·'}</span>
            {label}
          </a>
        ))}
      </nav>
      <NavLink className="header-login" to="/login">
        <span>↪</span> Login
      </NavLink>
    </header>
  )
}
