import { zodResolver } from '@hookform/resolvers/zod'
import { type ChangeEvent, useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../auth/AuthContext'
import { ArenaBackground } from '../components/ArenaBackground'
import { Brand } from '../components/Brand'
import { FormField } from '../components/FormField'
import { StatusToast } from '../components/StatusToast'
import { primaryRole, roleThemes } from '../config/roleConfig'
import { ApiError, apiRequest } from '../lib/api'
import { platformApi } from '../lib/platformApi'

type ProfileResponse = {
  id: string
  fullName: string
  phoneNumber: string
  city: string
  locality: string
  locationDescription?: string
  bio?: string
  profileImageUrl?: string
  preferredSports?: string
  skillLevel?: string
  playingPosition?: string
  availabilitySummary?: string
  organizationName?: string
  businessName?: string
  discoverable: boolean
}

const schema = z.object({
  fullName: z.string().trim().min(2, 'Full name is required').max(80),
  phoneNumber: z.string().regex(/^\+91[6-9]\d{9}$/, 'Use +91 followed by a valid 10-digit mobile number'),
  city: z.string().trim().min(2, 'City is required').max(80),
  locality: z.string().trim().min(2, 'Locality is required').max(80),
  locationDescription: z.string().max(180).optional(),
  bio: z.string().max(500).optional(),
  preferredSports: z.string().max(300).optional(),
  skillLevel: z.string().max(32).optional(),
  playingPosition: z.string().max(80).optional(),
  availabilitySummary: z.string().max(180).optional(),
  organizationName: z.string().max(140).optional(),
  businessName: z.string().max(140).optional(),
  discoverable: z.boolean(),
})
type FormValues = z.infer<typeof schema>

export function ProfileSetupPage() {
  const { user, updateUser } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const [uploadingImage, setUploadingImage] = useState(false)
  const role = primaryRole(user?.roles ?? ['PLAYER'])
  const theme = roleThemes[role]
  const roleSet = useMemo(() => new Set(user?.roles ?? []), [user])
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      fullName: user?.displayName ?? '', phoneNumber: '+91', city: '', locality: '',
      locationDescription: '', bio: '', preferredSports: '', skillLevel: '',
      playingPosition: '', availabilitySummary: '', organizationName: '', businessName: '', discoverable: true,
    },
  })
  const [imageUrl, setImageUrl] = useState('')

  useEffect(() => {
    if (!user) return
    const wasIncomplete = !user.profileCompleted
    let cancelled = false
    void apiRequest<ProfileResponse>('/profile')
      .then((profile) => {
        if (cancelled) return
        reset({
          fullName: profile.fullName ?? user.displayName,
          phoneNumber: profile.phoneNumber ?? '+91',
          city: profile.city ?? '',
          locality: profile.locality ?? '',
          locationDescription: profile.locationDescription ?? '',
          bio: profile.bio ?? '',
          preferredSports: profile.preferredSports ?? '',
          skillLevel: profile.skillLevel ?? '',
          playingPosition: profile.playingPosition ?? '',
          availabilitySummary: profile.availabilitySummary ?? '',
          organizationName: profile.organizationName ?? '',
          businessName: profile.businessName ?? '',
          discoverable: profile.discoverable,
        })
        setImageUrl(profile.profileImageUrl ?? '')
        if (wasIncomplete) {
          updateUser({ ...user, displayName: profile.fullName ?? user.displayName, profileCompleted: true })
          navigate(`/app/${theme.routeSegment}/dashboard`, { replace: true })
        }
      })
      .catch((error) => {
        if (!cancelled && (!(error instanceof ApiError) || error.status !== 404)) {
          setServerError(error instanceof ApiError ? error.message : 'Unable to load your profile')
        }
      })
    return () => { cancelled = true }
  }, [navigate, reset, theme.routeSegment, updateUser, user])

  async function uploadProfileImage(file?: File) {
    if (!file) return
    setUploadingImage(true)
    setServerError(null)
    try {
      const asset = await platformApi.uploadImage(file, 'profiles')
      setImageUrl(asset.secureUrl)
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Unable to upload profile image')
    } finally {
      setUploadingImage(false)
    }
  }

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    if (roleSet.has('PLAYER') && (!values.preferredSports || !values.skillLevel)) {
      setServerError('Preferred sports and skill level are required for Player profiles.')
      return
    }
    if (roleSet.has('ORGANIZER') && !values.organizationName) {
      setServerError('Organization name is required for Organizer profiles.')
      return
    }
    if (roleSet.has('TURF_OWNER') && !values.businessName) {
      setServerError('Business name is required for Turf Owner profiles.')
      return
    }

    try {
      await apiRequest<ProfileResponse>('/profile', {
        method: 'PUT',
        body: JSON.stringify({ ...values, profileImageUrl: imageUrl || null }),
      })
      if (user) updateUser({ ...user, displayName: values.fullName, profileCompleted: true })
      navigate(`/app/${theme.routeSegment}/dashboard`, { replace: true })
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Unable to save your profile')
    }
  })

  return (
    <div
      className="profile-setup-screen"
      style={{ '--role-accent': theme.accent, '--role-glow': theme.glow } as React.CSSProperties}
    >
      <ArenaBackground accent={theme.accent} />
      <header className="profile-setup-header"><Brand /><span>{theme.label} onboarding</span></header>
      <main className="profile-setup-layout">
        <aside className="profile-progress">
          <p className="eyebrow">PROFILE SETUP</p>
          <h1>Show the community<br /><span>how you play.</span></h1>
          <p>Complete the information needed for your active roles. You can edit it later.</p>
          <ol>
            <li className="done"><b>1</b><span><strong>Email verified</strong><small>Account ownership confirmed</small></span></li>
            <li className="active"><b>2</b><span><strong>Complete profile</strong><small>Identity, location, and role details</small></span></li>
            <li><b>3</b><span><strong>Enter workspace</strong><small>Open the {theme.label} dashboard</small></span></li>
          </ol>
        </aside>
        <section className="profile-form-card">
          <div className="profile-form-heading">
            <div className="profile-image-preview">
              {imageUrl ? <img src={imageUrl} alt="Profile preview" /> : <span>{(user?.displayName ?? 'P').charAt(0)}</span>}
            </div>
            <div><p>{theme.eyebrow}</p><h2>Complete your profile</h2><span>Fields marked by your role are required before dashboard access.</span></div>
          </div>
          {serverError && <StatusToast type="error" message={serverError} />}
          <form className="profile-form" onSubmit={onSubmit} noValidate>
            <div className="form-section-title"><span>01</span><div><strong>Identity</strong><small>Public name and contact validation</small></div></div>
            <div className="form-two-column">
              <FormField label="Full name" placeholder="Your full name" error={errors.fullName?.message} {...register('fullName')} />
              <FormField label="Indian mobile number" placeholder="+919876543210" hint="Must begin with +91 and contain 10 digits." error={errors.phoneNumber?.message} {...register('phoneNumber')} />
            </div>
            <label className="form-field"><span className="form-label">Profile picture</span><span className="input-wrap"><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event: ChangeEvent<HTMLInputElement>) => void uploadProfileImage(event.target.files?.[0])} /></span><small className="form-hint">{uploadingImage ? 'Uploading image…' : 'JPG, PNG or WebP, maximum 5 MB. Uses local storage or Cloudinary based on backend configuration.'}</small></label>

            <div className="form-section-title"><span>02</span><div><strong>Location</strong><small>Used for turf, event, and player discovery</small></div></div>
            <div className="form-two-column">
              <FormField label="City" placeholder="Navi Mumbai" error={errors.city?.message} {...register('city')} />
              <FormField label="Locality" placeholder="Vashi" error={errors.locality?.message} {...register('locality')} />
            </div>
            <FormField label="Location details" placeholder="Landmark or preferred playing area" error={errors.locationDescription?.message} {...register('locationDescription')} />

            {roleSet.has('PLAYER') && <>
              <div className="form-section-title"><span>03</span><div><strong>Player details</strong><small>Help teams and players find a compatible match</small></div></div>
              <div className="form-two-column">
                <FormField label="Preferred sports" placeholder="Football, Cricket" error={errors.preferredSports?.message} {...register('preferredSports')} />
                <label className="form-field"><span className="form-label">Skill level</span><span className="input-wrap"><select {...register('skillLevel')}><option value="">Select level</option><option>BEGINNER</option><option>INTERMEDIATE</option><option>ADVANCED</option><option>PROFESSIONAL</option></select></span></label>
              </div>
              <div className="form-two-column">
                <FormField label="Playing position" placeholder="Defender, All-rounder…" error={errors.playingPosition?.message} {...register('playingPosition')} />
                <FormField label="Availability" placeholder="Weekends after 5 PM" error={errors.availabilitySummary?.message} {...register('availabilitySummary')} />
              </div>
            </>}

            {roleSet.has('ORGANIZER') && <><div className="form-section-title"><span>03</span><div><strong>Organizer details</strong><small>Event identity shown to participants</small></div></div><FormField label="Organization name" placeholder="Your organization or organizer brand" error={errors.organizationName?.message} {...register('organizationName')} /></>}
            {roleSet.has('TURF_OWNER') && <><div className="form-section-title"><span>03</span><div><strong>Business details</strong><small>Venue management identity</small></div></div><FormField label="Business name" placeholder="Your turf or business name" error={errors.businessName?.message} {...register('businessName')} /></>}

            <FormField multiline label="Short bio" placeholder="Tell players what you enjoy, organize, or operate…" rows={4} error={errors.bio?.message} {...register('bio')} />
            <label className="check-row profile-discovery"><input type="checkbox" {...register('discoverable')} /><span><strong>Allow profile discovery</strong><small>Show approved profile fields in player and team search.</small></span></label>
            <button className="button button-primary full-width" disabled={isSubmitting}>{isSubmitting ? 'Saving profile…' : `Save and open ${theme.label} workspace →`}</button>
          </form>
        </section>
      </main>
    </div>
  )
}
