import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { RequireAuth } from './components/RequireAuth'
import { primaryRole, roleFromSegment, roleThemes } from './config/roleConfig'
import type { PlatformRole } from './types/auth'
import { DashboardPage } from './pages/DashboardPage'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { ModulePage } from './pages/ModulePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProfileSetupPage } from './pages/ProfileSetupPage'
import { RegisterPage } from './pages/RegisterPage'
import { SessionManagementPage } from './pages/SessionManagementPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'

function resolveRequestedRole(segment: string | undefined, roles: PlatformRole[]): PlatformRole | null {
  const requested = roleFromSegment(segment)
  if (requested === 'ADMIN' && roles.includes('SUPER_ADMIN')) return 'SUPER_ADMIN'
  return requested
}

function PortalRoute() {
  const { role: roleSegment, module } = useParams()
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />

  const requestedRole = resolveRequestedRole(roleSegment, user.roles)
  if (!requestedRole || !user.roles.includes(requestedRole)) {
    const allowedRole = primaryRole(user.roles)
    return <Navigate to={`/app/${roleThemes[allowedRole].routeSegment}/dashboard`} replace />
  }
  return module === 'dashboard'
    ? <DashboardPage resolvedRole={requestedRole} />
    : <ModulePage resolvedRole={requestedRole} />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/profile/setup" element={<ProfileSetupPage />} />
        <Route path="/account/sessions" element={<SessionManagementPage />} />
        <Route path="/app/:role/:module" element={<PortalRoute />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
