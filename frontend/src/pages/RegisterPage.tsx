import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { FormField } from '../components/FormField'
import { StatusToast } from '../components/StatusToast'
import { ApiError } from '../lib/api'
import type { PlatformRole } from '../types/auth'

const registerRoles: Array<{ role: PlatformRole; label: string; icon: string; description: string }> = [
  { role: 'PLAYER', label: 'Player', icon: '◉', description: 'Play, book, join, and build teams.' },
  { role: 'ORGANIZER', label: 'Organizer', icon: '◫', description: 'Create events and manage competition.' },
  { role: 'TURF_OWNER', label: 'Turf Owner', icon: '⌖', description: 'Manage venues, slots, and bookings.' },
]

const schema = z.object({
  displayName: z.string().trim().min(2, 'Name must contain at least 2 characters').max(80),
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Use at least 8 characters')
    .regex(/[A-Z]/, 'Include an uppercase letter')
    .regex(/[a-z]/, 'Include a lowercase letter')
    .regex(/\d/, 'Include a number')
    .regex(/[^A-Za-z0-9]/, 'Include a special character'),
  confirmPassword: z.string(),
  role: z.enum(['PLAYER', 'ORGANIZER', 'TURF_OWNER']),
  terms: z.boolean().refine((value: boolean) => value, 'Accept the terms to continue'),
}).refine((values: { password: string; confirmPassword: string }) => values.password === values.confirmPassword, {
  path: ['confirmPassword'], message: 'Passwords do not match',
})
type FormValues = z.infer<typeof schema>

export function RegisterPage() {
  const { register: createAccount } = useAuth()
  const [status, setStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null)
  const { register, handleSubmit, control, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { role: 'PLAYER', terms: false },
  })

  const onSubmit = handleSubmit(async ({ confirmPassword: _confirm, terms: _terms, ...values }) => {
    setStatus(null)
    try {
      await createAccount(values)
      setStatus({ type: 'success', message: 'Account created. Check your inbox—or the backend terminal in LOG mode—for the verification link.' })
    } catch (error) {
      setStatus({ type: 'error', message: error instanceof ApiError ? error.message : 'Registration failed' })
    }
  })

  return (
    <AuthLayout aside={
      <div className="auth-story">
        <p className="eyebrow">CREATE YOUR SPACE</p>
        <h1>Choose your role.<br /><span>Enter the game.</span></h1>
        <p>Each account gets a focused workspace while sharing one trusted PlaySphere identity.</p>
        <div className="verification-flow">
          <span><b>1</b> Register</span><i>→</i><span><b>2</b> Verify email</span><i>→</i><span><b>3</b> Complete profile</span>
        </div>
      </div>
    }>
      <div className="auth-card register-card">
        <div className="auth-card-heading"><p>NEW ACCOUNT</p><h2>Join PlaySphere</h2><span>Public registration is available for Players, Organizers, and Turf Owners.</span></div>
        {status && <StatusToast type={status.type} message={status.message} />}
        <form onSubmit={onSubmit} className="auth-form" noValidate>
          <Controller name="role" control={control} render={({ field }) => (
            <fieldset className="role-choice"><legend>Choose account role</legend><div className="role-choice-grid">
              {registerRoles.map((item) => (
                <label key={item.role} className={field.value === item.role ? 'selected' : ''}>
                  <input type="radio" value={item.role} checked={field.value === item.role} onChange={() => field.onChange(item.role)} />
                  <span className="role-choice-icon">{item.icon}</span><strong>{item.label}</strong><small>{item.description}</small>
                </label>
              ))}
            </div>{errors.role && <span className="field-error">{errors.role.message}</span>}</fieldset>
          )} />
          <div className="form-two-column">
            <FormField label="Full name" placeholder="Your full name" autoComplete="name" error={errors.displayName?.message} {...register('displayName')} />
            <FormField label="Email address" type="email" placeholder="you@example.com" autoComplete="email" error={errors.email?.message} {...register('email')} />
          </div>
          <div className="form-two-column">
            <FormField label="Password" type="password" placeholder="Create a strong password" autoComplete="new-password" error={errors.password?.message} {...register('password')} />
            <FormField label="Confirm password" type="password" placeholder="Repeat your password" autoComplete="new-password" error={errors.confirmPassword?.message} {...register('confirmPassword')} />
          </div>
          <label className="check-row"><input type="checkbox" {...register('terms')} /><span>I accept the Terms of Service and Privacy Policy.</span></label>
          {errors.terms && <span className="field-error standalone">{errors.terms.message}</span>}
          <button className="button button-primary full-width" disabled={isSubmitting}>{isSubmitting ? 'Creating account…' : 'Create account →'}</button>
        </form>
        <p className="auth-switch">Already registered? <Link to="/login">Login</Link></p>
      </div>
    </AuthLayout>
  )
}
