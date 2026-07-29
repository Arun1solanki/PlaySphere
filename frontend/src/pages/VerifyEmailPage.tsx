import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArenaBackground } from '../components/ArenaBackground'
import { Brand } from '../components/Brand'
import { ApiError, authApi } from '../lib/api'

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [state, setState] = useState<'working' | 'success' | 'error'>(token ? 'working' : 'error')
  const [message, setMessage] = useState(token ? 'Confirming your verification link…' : 'Verification token is missing.')

  useEffect(() => {
    if (!token) return
    void authApi.verifyEmail(token)
      .then(() => { setState('success'); setMessage('Your email is verified. You can now log in and complete your profile.') })
      .catch((error: unknown) => { setState('error'); setMessage(error instanceof ApiError ? error.message : 'Unable to verify this email') })
  }, [token])

  return (
    <div className="center-screen"><ArenaBackground /><div className="verification-card">
      <Brand />
      <div className={`verification-symbol ${state}`}><span>{state === 'working' ? '⌁' : state === 'success' ? '✓' : '!'}</span></div>
      <p className="eyebrow">EMAIL VERIFICATION</p>
      <h1>{state === 'working' ? 'Checking your link' : state === 'success' ? 'You are verified' : 'Link not accepted'}</h1>
      <p>{message}</p>
      {state === 'success' ? <Link className="button button-primary full-width" to="/login">Continue to login →</Link> : state === 'error' ? <Link className="button button-secondary full-width" to="/login">Back to login</Link> : <div className="verification-loader"><i /><i /><i /></div>}
    </div></div>
  )
}
