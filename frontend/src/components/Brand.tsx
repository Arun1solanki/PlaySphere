import { Link } from 'react-router-dom'

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link className="brand" to="/" aria-label="PlaySphere home">
      <img src="/playsphere-mark.svg" alt="" className="brand-mark" />
      {!compact && (
        <span className="brand-word">
          Play<span>Sphere</span>
        </span>
      )}
    </Link>
  )
}
