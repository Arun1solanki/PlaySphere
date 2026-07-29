import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { AuthLayout } from '../components/AuthLayout'
import { FormField } from '../components/FormField'
import { StatusToast } from '../components/StatusToast'
import { ApiError, authApi } from '../lib/api'

const schema = z.object({ email: z.string().email('Enter a valid email address') })
type FormValues = z.infer<typeof schema>

export function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  })

  const submit = handleSubmit(async ({ email }) => {
    setError(null)
    try {
      await authApi.requestPasswordReset(email)
      setMessage('Check your email or the backend terminal in log mode. For privacy, the same response is shown for every address.')
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'Unable to request a password reset')
    }
  })

  return (
    <AuthLayout aside={<div className="auth-story"><p className="eyebrow">ACCOUNT RECOVERY</p><h1>Return safely to<br /><span>your arena.</span></h1><p>Reset links are short-lived and all existing sessions are revoked when the password changes.</p></div>}>
      <div className="auth-card">
        <div className="auth-card-heading"><p>RESET PASSWORD</p><h2>Find your account</h2><span>Enter the verified email used for PlaySphere.</span></div>
        {message && <StatusToast type="success" message={message} />}
        {error && <StatusToast type="error" message={error} />}
        <form className="auth-form" onSubmit={submit} noValidate>
          <FormField label="Email address" type="email" autoComplete="email" placeholder="you@example.com" error={errors.email?.message} {...register('email')} />
          <button className="button button-primary full-width" disabled={isSubmitting}>{isSubmitting ? 'Sending…' : 'Send reset link →'}</button>
        </form>
        <p className="auth-switch"><Link to="/login">← Back to login</Link></p>
      </div>
    </AuthLayout>
  )
}
