import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { FormField } from '../components/FormField'
import { StatusToast } from '../components/StatusToast'
import { ApiError, authApi } from '../lib/api'

const passwordRule = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{10,72}$/
const schema = z.object({
  password: z.string().regex(passwordRule, 'Use 10+ characters with upper, lower, number and special character'),
  confirmPassword: z.string(),
}).refine((values: { password: string; confirmPassword: string }) => values.password === values.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' })
type FormValues = z.infer<typeof schema>

export function ResetPasswordPage() {
  const [search] = useSearchParams()
  const token = search.get('token') ?? ''
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const submit = handleSubmit(async ({ password }) => {
    setError(null)
    if (!token) { setError('The password-reset token is missing.'); return }
    try {
      await authApi.confirmPasswordReset(token, password)
      setDone(true)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'Unable to reset the password')
    }
  })

  return (
    <AuthLayout aside={<div className="auth-story"><p className="eyebrow">SECURE RESET</p><h1>Choose a strong<br /><span>new password.</span></h1><p>After the reset, every active PlaySphere session is signed out.</p></div>}>
      <div className="auth-card">
        <div className="auth-card-heading"><p>NEW PASSWORD</p><h2>Secure your account</h2><span>Use a password you have not used elsewhere.</span></div>
        {done ? <><StatusToast type="success" message="Password changed. All old sessions have been revoked." /><p className="auth-switch"><Link to="/login">Continue to login →</Link></p></> : <>
          {error && <StatusToast type="error" message={error} />}
          <form className="auth-form" onSubmit={submit} noValidate>
            <FormField label="New password" type="password" autoComplete="new-password" error={errors.password?.message} {...register('password')} />
            <FormField label="Confirm new password" type="password" autoComplete="new-password" error={errors.confirmPassword?.message} {...register('confirmPassword')} />
            <button className="button button-primary full-width" disabled={isSubmitting}>{isSubmitting ? 'Updating…' : 'Change password →'}</button>
          </form>
        </>}
      </div>
    </AuthLayout>
  )
}
