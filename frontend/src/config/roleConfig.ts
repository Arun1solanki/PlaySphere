import type { PlatformRole } from '../types/auth'

export type RoleTheme = {
  role: PlatformRole
  label: string
  shortLabel: string
  routeSegment: string
  accent: string
  accentSoft: string
  glow: string
  title: string
  subtitle: string
  eyebrow: string
  nav: Array<{ label: string; path: string; icon: string }>
  cards: Array<{ label: string; value: string; hint: string; icon: string }>
  actions: Array<{ label: string; description: string; icon: string }>
  activity: Array<{ title: string; detail: string; tag: string }>
}

export const roleThemes: Record<PlatformRole, RoleTheme> = {
  PLAYER: {
    role: 'PLAYER', label: 'Player', shortLabel: 'Player mode', routeSegment: 'player',
    accent: '#42e8ff', accentSoft: 'rgba(66,232,255,.14)', glow: 'rgba(39,204,255,.34)',
    eyebrow: 'PLAYER HUB', title: 'Your next game starts here.',
    subtitle: 'Find the right people, reserve a turf, and keep your team moving.',
    nav: [
      { label: 'Dashboard', path: 'dashboard', icon: '⌁' },
      { label: 'Discover', path: 'discover', icon: '◎' },
      { label: 'Turfs', path: 'turfs', icon: '⌖' },
      { label: 'Teams', path: 'teams', icon: '◉' },
      { label: 'Need Players', path: 'need-players', icon: '⊕' },
      { label: 'Events', path: 'events', icon: '◫' },
      { label: 'Bookings', path: 'bookings', icon: '▣' },
      { label: 'Messages', path: 'messages', icon: '◌' },
      { label: 'Payments', path: 'payments', icon: '₹' },
      { label: 'Reviews & Reports', path: 'reviews', icon: '★' },
      { label: 'Notifications', path: 'notifications', icon: '•' },
    ],
    cards: [
      { label: 'Upcoming bookings', value: '02', hint: 'Next: Friday, 7:00 PM', icon: '▣' },
      { label: 'Team requests', value: '04', hint: 'Two need your reply', icon: '◉' },
      { label: 'Events joined', value: '06', hint: 'This season', icon: '◫' },
      { label: 'Player rating', value: '4.8', hint: 'From verified games', icon: '★' },
    ],
    actions: [
      { label: 'Book a turf', description: 'See open slots near you', icon: '⌖' },
      { label: 'Create a team', description: 'Become Team Captain', icon: '◉' },
      { label: 'Post need players', description: 'Recruit the missing positions', icon: '⊕' },
    ],
    activity: [
      { title: 'Vashi Strikers need 2 defenders', detail: 'Football · Vashi · Intermediate', tag: 'TEAM REQUEST' },
      { title: 'Friday Night Clash', detail: '5v5 Football · Registration open', tag: 'UP NEXT' },
    ],
  },
  ORGANIZER: {
    role: 'ORGANIZER', label: 'Organizer', shortLabel: 'Organizer mode', routeSegment: 'organizer',
    accent: '#a98bff', accentSoft: 'rgba(169,139,255,.15)', glow: 'rgba(132,84,255,.34)',
    eyebrow: 'EVENT OPERATIONS', title: 'Run every event with clarity.',
    subtitle: 'Coordinate registrations, venues, fixtures, announcements, and results.',
    nav: [
      { label: 'Dashboard', path: 'dashboard', icon: '⌁' },
      { label: 'My Events', path: 'events', icon: '◫' },
      { label: 'Create Event', path: 'create-event', icon: '＋' },
      { label: 'Registrations', path: 'registrations', icon: '◎' },
      { label: 'Participants', path: 'participants', icon: '◉' },
      { label: 'Fixtures', path: 'fixtures', icon: '⌗' },
      { label: 'Scores', path: 'scores', icon: '↗' },
      { label: 'Venue Bookings', path: 'venues', icon: '⌖' },
      { label: 'Earnings', path: 'earnings', icon: '₹' },
      { label: 'Reviews & Reports', path: 'reviews', icon: '★' },
      { label: 'Messages', path: 'messages', icon: '◌' },
      { label: 'Notifications', path: 'notifications', icon: '•' },
    ],
    cards: [
      { label: 'Active events', value: '05', hint: 'Three this week', icon: '◫' },
      { label: 'Pending registrations', value: '18', hint: 'Awaiting review', icon: '◎' },
      { label: 'Upcoming fixtures', value: '12', hint: 'Across four events', icon: '⌗' },
      { label: 'Event revenue', value: '₹24K', hint: 'Current month', icon: '₹' },
    ],
    actions: [
      { label: 'Create an event', description: 'Set rules, capacity, and fee', icon: '＋' },
      { label: 'Review registrations', description: 'Approve players and teams', icon: '◎' },
      { label: 'Publish fixtures', description: 'Build the match schedule', icon: '⌗' },
    ],
    activity: [
      { title: 'Monsoon Cup registration is 72% full', detail: '18 teams confirmed · 7 pending', tag: 'REGISTRATION' },
      { title: 'Venue request approved', detail: 'Arena One · 2 August · 8:00 AM', tag: 'VENUE' },
    ],
  },
  TURF_OWNER: {
    role: 'TURF_OWNER', label: 'Turf Owner', shortLabel: 'Owner mode', routeSegment: 'turf-owner',
    accent: '#61f2aa', accentSoft: 'rgba(97,242,170,.14)', glow: 'rgba(31,211,133,.34)',
    eyebrow: 'VENUE CONTROL', title: 'Keep every slot in play.',
    subtitle: 'Manage turfs, availability, pricing, bookings, equipment, and check-ins.',
    nav: [
      { label: 'Dashboard', path: 'dashboard', icon: '⌁' },
      { label: 'My Turfs', path: 'turfs', icon: '⌖' },
      { label: 'Add Turf', path: 'add-turf', icon: '＋' },
      { label: 'Availability', path: 'availability', icon: '▦' },
      { label: 'Pricing', path: 'pricing', icon: '₹' },
      { label: 'Bookings', path: 'bookings', icon: '▣' },
      { label: 'Check-In', path: 'check-in', icon: '⌁' },
      { label: 'Equipment', path: 'equipment', icon: '◇' },
      { label: 'Earnings', path: 'earnings', icon: '↗' },
      { label: 'Reviews & Reports', path: 'reviews', icon: '★' },
      { label: 'Messages', path: 'messages', icon: '◌' },
      { label: 'Notifications', path: 'notifications', icon: '•' },
    ],
    cards: [
      { label: "Today's bookings", value: '14', hint: 'Across two turfs', icon: '▣' },
      { label: 'Open slots', value: '09', hint: 'Available today', icon: '▦' },
      { label: 'Occupancy', value: '76%', hint: 'Current week', icon: '⌖' },
      { label: 'Monthly earnings', value: '₹38K', hint: 'Verified payments', icon: '₹' },
    ],
    actions: [
      { label: 'Create slots', description: 'Open the calendar for bookings', icon: '▦' },
      { label: 'Block maintenance', description: 'Protect unavailable time', icon: '◇' },
      { label: 'Verify check-in', description: 'Scan booking QR or code', icon: '⌁' },
    ],
    activity: [
      { title: 'Peak-hour slots are filling fast', detail: 'Friday · 6:00 PM to 10:00 PM', tag: 'OCCUPANCY' },
      { title: 'New booking confirmed', detail: 'Football Turf A · ₹1,200', tag: 'BOOKING' },
    ],
  },
  ADMIN: {
    role: 'ADMIN', label: 'Admin', shortLabel: 'Admin mode', routeSegment: 'admin',
    accent: '#ffb454', accentSoft: 'rgba(255,180,84,.14)', glow: 'rgba(255,116,63,.28)',
    eyebrow: 'PLATFORM CONTROL', title: 'See risk before it spreads.',
    subtitle: 'Approve, moderate, investigate, and keep every role accountable.',
    nav: [
      { label: 'Dashboard', path: 'dashboard', icon: '⌁' },
      { label: 'Users', path: 'users', icon: '◉' },
      { label: 'Approvals', path: 'approvals', icon: '✓' },
      { label: 'Turfs', path: 'turfs', icon: '⌖' },
      { label: 'Teams', path: 'teams', icon: '◉' },
      { label: 'Events', path: 'events', icon: '◫' },
      { label: 'Bookings', path: 'bookings', icon: '▣' },
      { label: 'Refunds', path: 'payments', icon: '₹' },
      { label: 'Transactions', path: 'transactions', icon: '↗' },
      { label: 'Reviews', path: 'reviews', icon: '★' },
      { label: 'Notifications', path: 'notifications', icon: '•' },
      { label: 'Reports', path: 'reports', icon: '!' },
      { label: 'Audit Logs', path: 'audit', icon: '≡' },
    ],
    cards: [
      { label: 'Pending approvals', value: '11', hint: 'Owners and turfs', icon: '✓' },
      { label: 'Open reports', value: '07', hint: 'Two high priority', icon: '!' },
      { label: 'Refund queue', value: '05', hint: '₹8,450 requested', icon: '₹' },
      { label: 'Active users', value: '1.8K', hint: 'Last 30 days', icon: '◉' },
    ],
    actions: [
      { label: 'Review approvals', description: 'Verify owner and venue details', icon: '✓' },
      { label: 'Handle reports', description: 'Investigate flagged content', icon: '!' },
      { label: 'Process refunds', description: 'Review payment evidence', icon: '₹' },
    ],
    activity: [
      { title: 'Two reports require urgent review', detail: 'Harassment and fraudulent listing', tag: 'RISK' },
      { title: 'Turf approval queue updated', detail: '4 verified · 3 need corrections', tag: 'APPROVALS' },
    ],
  },
  SUPER_ADMIN: {
    role: 'SUPER_ADMIN', label: 'Super Admin', shortLabel: 'Super Admin', routeSegment: 'super-admin',
    accent: '#ff7a6b', accentSoft: 'rgba(255,122,107,.14)', glow: 'rgba(255,78,78,.30)',
    eyebrow: 'SYSTEM AUTHORITY', title: 'Control the control plane.',
    subtitle: 'Manage administrators, security configuration, and sensitive audit actions.',
    nav: [
      { label: 'Dashboard', path: 'dashboard', icon: '⌁' },
      { label: 'Administrators', path: 'administrators', icon: '◉' },
      { label: 'Security', path: 'security', icon: '◇' },
      { label: 'Audit Logs', path: 'audit', icon: '≡' },
      { label: 'Configuration', path: 'configuration', icon: '⌘' },
      { label: 'Notifications', path: 'notifications', icon: '•' },
    ],
    cards: [
      { label: 'Administrators', value: '04', hint: 'All active', icon: '◉' },
      { label: 'Security alerts', value: '00', hint: 'No open incidents', icon: '◇' },
      { label: 'Sensitive actions', value: '09', hint: 'This week', icon: '≡' },
      { label: 'System health', value: '99.9%', hint: 'Last 30 days', icon: '⌘' },
    ],
    actions: [
      { label: 'Manage admins', description: 'Grant or revoke authority', icon: '◉' },
      { label: 'Review security', description: 'Inspect sensitive changes', icon: '◇' },
      { label: 'Platform settings', description: 'Control global configuration', icon: '⌘' },
    ],
    activity: [
      { title: 'No active security incidents', detail: 'All systems operating normally', tag: 'SECURITY' },
      { title: 'Configuration review due', detail: 'Payment and email secrets', tag: 'GOVERNANCE' },
    ],
  },
}

export const publicPreviewRoles: PlatformRole[] = ['PLAYER', 'ORGANIZER', 'TURF_OWNER', 'ADMIN']

export function primaryRole(roles: PlatformRole[]): PlatformRole {
  return roles[0] ?? 'PLAYER'
}


export function roleFromSegment(segment?: string): PlatformRole | null {
  if (!segment) return null
  const match = Object.values(roleThemes).find((theme) => theme.routeSegment === segment)
  return match?.role ?? null
}
