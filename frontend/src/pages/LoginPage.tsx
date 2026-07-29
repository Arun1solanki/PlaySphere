import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { FormField } from '../components/FormField'
import { StatusToast } from '../components/StatusToast'
import { ApiError } from '../lib/api'
import { primaryRole, roleThemes } from '../config/roleConfig'

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
  rememberMe: z.boolean(),
})
type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const { login, sessionNotice, clearSessionNotice } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [serverError, setServerError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { rememberMe: false },
  })

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    clearSessionNotice()
    try {
      const user = await login(values)
      const destination = (location.state as { from?: string } | null)?.from
      if (destination) navigate(destination, { replace: true })
      else if (!user.profileCompleted) navigate('/profile/setup', { replace: true })
      else navigate(`/app/${roleThemes[primaryRole(user.roles)].routeSegment}/dashboard`, { replace: true })
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Unable to log in')
    }
  })

  return (
    <AuthLayout aside={
      <div className="auth-story">
        <p className="eyebrow">WELCOME BACK</p>
        <h1>Step back into<br /><span>your arena.</span></h1>
        <p>One secure account connects your player, organizer, and venue workspaces.</p>
        <div className="auth-story-list">
          <span><i>✓</i> Verified email access</span>
          <span><i>✓</i> Role-protected dashboards</span>
          <span><i>✓</i> Secure JWT sessions</span>
        </div>
      </div>
    }>
      <div className="auth-card">
        <div className="auth-card-heading"><p>ACCOUNT ACCESS</p><h2>Login to PlaySphere</h2><span>Enter your verified email and password.</span></div>
        {sessionNotice && <StatusToast type="info" message={sessionNotice} />}
        {serverError && <StatusToast type="error" message={serverError} />}
        <form onSubmit={onSubmit} className="auth-form" noValidate>
          <FormField label="Email address" type="email" autoComplete="email" placeholder="you@example.com" error={errors.email?.message} {...register('email')} />
          <FormField label="Password" type="password" autoComplete="current-password" placeholder="Enter your password" error={errors.password?.message} {...register('password')} />
          <div className="form-row-between"><label className="check-row"><input type="checkbox" {...register('rememberMe')} /> <span>Keep me signed in</span></label><Link className="text-button" to="/forgot-password">Forgot password?</Link></div>
          <button className="button button-primary full-width" disabled={isSubmitting}>{isSubmitting ? 'Signing in…' : 'Login →'}</button>
        </form>
        <p className="auth-switch">New to PlaySphere? <Link to="/register">Create an account</Link></p>
      </div>
    </AuthLayout>
  )
}
