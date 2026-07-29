export function StatCard({ label, value, hint, icon }: { label: string; value: string; hint: string; icon: string }) {
  return (
    <article className="stat-card">
      <div className="stat-card-top"><span className="stat-icon">{icon}</span><span className="trend-pill">↗ 8%</span></div>
      <p>{label}</p>
      <strong>{value}</strong>
      <small>{hint}</small>
      <span className="stat-line" />
    </article>
  )
}
