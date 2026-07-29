export function StatusToast({ type, message }: { type: 'success' | 'error' | 'info'; message: string }) {
  const symbol = type === 'success' ? '✓' : type === 'info' ? 'i' : '!'
  return (
    <div className={`status-toast ${type}`} role={type === 'error' ? 'alert' : 'status'}>
      <span>{symbol}</span>
      <p>{message}</p>
    </div>
  )
}
