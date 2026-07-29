export function ArenaBackground({ accent }: { accent?: string }) {
  return (
    <div
      className="arena-bg"
      style={accent ? ({ '--role-accent': accent } as React.CSSProperties) : undefined}
      aria-hidden="true"
    >
      <div className="arena-grid" />
      <div className="arena-radial arena-radial-one" />
      <div className="arena-radial arena-radial-two" />
      <div className="arena-pitch">
        <span className="pitch-line pitch-center" />
        <span className="pitch-circle" />
        <span className="pitch-box pitch-box-left" />
        <span className="pitch-box pitch-box-right" />
      </div>
      <div className="orbit orbit-one" />
      <div className="orbit orbit-two" />
      <div className="spark spark-one" />
      <div className="spark spark-two" />
      <div className="spark spark-three" />
      <div className="noise-layer" />
    </div>
  )
}
