import { apiRequest } from './api'
import type {
  AuditLog,
  Booking,
  ChatMessage,
  Conversation,
  EventRegistration,
  Match,
  Notification,
  Payment,
  PlayerDiscovery,
  RecruitmentPost,
  Refund,
  Report,
  Review,
  SportsEvent,
  Team,
  Turf,
  TurfSlot,
  TurfEquipment,
} from '../types/domain'

export const platformApi = {
  players: (filters: { q?: string; city?: string; sport?: string } = {}) => {
    const params = new URLSearchParams()
    if (filters.q) params.set('q', filters.q)
    if (filters.city) params.set('city', filters.city)
    if (filters.sport) params.set('sport', filters.sport)
    const suffix = params.toString() ? `?${params.toString()}` : ''
    return apiRequest<PlayerDiscovery[]>(`/profile/players${suffix}`)
  },

  teams: () => apiRequest<Team[]>('/teams'),
  myTeams: () => apiRequest<Team[]>('/teams/mine'),
  team: (id: string) => apiRequest<Team>(`/teams/${id}`),
  createTeam: (body: unknown) => apiRequest<Team>('/teams', { method: 'POST', body: JSON.stringify(body) }),
  joinTeam: (id: string, message = '') => apiRequest(`/teams/${id}/join-requests`, { method: 'POST', body: JSON.stringify({ message }) }),
  teamRequests: (id: string) => apiRequest<any[]>(`/teams/${id}/join-requests`),
  decideTeamRequest: (id: string, accept: boolean) => apiRequest(`/team-join-requests/${id}`, { method: 'PATCH', body: JSON.stringify({ accept }) }),
  updateTeam: (id: string, body: unknown) => apiRequest<Team>(`/teams/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  archiveTeam: (id: string) => apiRequest(`/teams/${id}`, { method: 'DELETE' }),
  leaveTeam: (id: string) => apiRequest(`/teams/${id}/members/me`, { method: 'DELETE' }),
  removeTeamMember: (teamId: string, userId: string) => apiRequest(`/teams/${teamId}/members/${userId}`, { method: 'DELETE' }),
  transferTeamCaptain: (teamId: string, nextCaptainUserId: string) => apiRequest<Team>(`/teams/${teamId}/captain`, { method: 'PATCH', body: JSON.stringify({ nextCaptainUserId }) }),

  posts: () => apiRequest<RecruitmentPost[]>('/recruitment-posts'),
  createPost: (body: unknown) => apiRequest<RecruitmentPost>('/recruitment-posts', { method: 'POST', body: JSON.stringify(body) }),
  applyPost: (id: string, message = '') => apiRequest(`/recruitment-posts/${id}/applications`, { method: 'POST', body: JSON.stringify({ message }) }),
  postApplications: (id: string) => apiRequest<any[]>(`/recruitment-posts/${id}/applications`),
  decideApplication: (id: string, accept: boolean) => apiRequest(`/recruitment-applications/${id}`, { method: 'PATCH', body: JSON.stringify({ accept }) }),

  turfs: () => apiRequest<Turf[]>('/turfs'),
  myTurfs: () => apiRequest<Turf[]>('/turfs/mine'),
  createTurf: (body: unknown) => apiRequest<Turf>('/turfs', { method: 'POST', body: JSON.stringify(body) }),
  slots: (id: string) => apiRequest<TurfSlot[]>(`/turfs/${id}/slots`),
  createSlot: (id: string, body: unknown) => apiRequest<TurfSlot>(`/turfs/${id}/slots`, { method: 'POST', body: JSON.stringify(body) }),
  ownerSlots: (id: string) => apiRequest<TurfSlot[]>(`/turfs/${id}/slots/manage`),
  setSlotStatus: (turfId: string, slotId: string, status: 'AVAILABLE' | 'BLOCKED') => apiRequest<TurfSlot>(`/turfs/${turfId}/slots/${slotId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  equipment: (id: string) => apiRequest<TurfEquipment[]>(`/turfs/${id}/equipment`),
  ownerEquipment: (id: string) => apiRequest<TurfEquipment[]>(`/turfs/${id}/equipment/manage`),
  addEquipment: (id: string, body: unknown) => apiRequest<TurfEquipment>(`/turfs/${id}/equipment`, { method: 'POST', body: JSON.stringify(body) }),
  updateEquipment: (turfId: string, equipmentId: string, body: unknown) => apiRequest<TurfEquipment>(`/turfs/${turfId}/equipment/${equipmentId}`, { method: 'PATCH', body: JSON.stringify(body) }),
  book: (slotId: string) => apiRequest<Booking>(`/bookings?slotId=${encodeURIComponent(slotId)}`, { method: 'POST' }),
  myBookings: () => apiRequest<Booking[]>('/bookings/mine'),
  turfBookings: (turfId: string) => apiRequest<Booking[]>(`/turfs/${turfId}/bookings`),
  qrBooking: (id: string) => apiRequest<Booking>(`/bookings/${id}/qr`, { method: 'POST' }),
  cancelBooking: (id: string, reason: string) => apiRequest(`/bookings/${id}/cancel`, { method: 'PATCH', body: JSON.stringify({ reason }) }),
  checkIn: (bookingCode: string, qrToken: string) => apiRequest<Booking>('/bookings/check-in', { method: 'POST', body: JSON.stringify({ bookingCode, qrToken }) }),

  events: () => apiRequest<SportsEvent[]>('/events'),
  myEvents: () => apiRequest<SportsEvent[]>('/events/mine'),
  createEvent: (body: unknown) => apiRequest<SportsEvent>('/events', { method: 'POST', body: JSON.stringify(body) }),
  cancelEvent: (id: string, reason: string) => apiRequest<SportsEvent>(`/events/${id}/cancel`, { method: 'PATCH', body: JSON.stringify({ reason }) }),
  registerEvent: (id: string, teamId?: string) => apiRequest<EventRegistration>(`/events/${id}/registrations`, { method: 'POST', body: JSON.stringify({ teamId }) }),
  leaveEvent: (id: string) => apiRequest(`/events/${id}/registrations/mine`, { method: 'DELETE' }),
  myRegistrations: () => apiRequest<EventRegistration[]>('/event-registrations/mine'),
  eventRegistrations: (id: string) => apiRequest<EventRegistration[]>(`/events/${id}/registrations`),
  matches: (id: string) => apiRequest<Match[]>(`/events/${id}/matches`),
  createMatch: (id: string, body: unknown) => apiRequest<Match>(`/events/${id}/matches`, { method: 'POST', body: JSON.stringify(body) }),
  scoreMatch: (id: string, homeScore: number, awayScore: number) => apiRequest<Match>(`/matches/${id}/score`, { method: 'PATCH', body: JSON.stringify({ homeScore, awayScore }) }),

  payments: () => apiRequest<Payment[]>('/payments/mine'),
  organizerEarnings: () => apiRequest<Payment[]>('/payments/earnings/organizer'),
  ownerEarnings: () => apiRequest<Payment[]>('/payments/earnings/turf-owner'),
  createPayment: (body: unknown) => apiRequest<Payment>('/payments', { method: 'POST', body: JSON.stringify(body) }),
  verifyRazorpay: (body: unknown) => apiRequest<Payment>('/payments/razorpay/verify', { method: 'POST', body: JSON.stringify(body) }),
  refunds: () => apiRequest<Refund[]>('/refunds/mine'),
  requestRefund: (id: string, body: unknown) => apiRequest<Refund>(`/payments/${id}/refunds`, { method: 'POST', body: JSON.stringify(body) }),

  notifications: () => apiRequest<Notification[]>('/notifications'),
  readNotification: (id: string) => apiRequest(`/notifications/${id}/read`, { method: 'PATCH' }),

  conversations: () => apiRequest<Conversation[]>('/conversations'),
  createConversation: (body: unknown) => apiRequest<Conversation>('/conversations', { method: 'POST', body: JSON.stringify(body) }),
  messages: (id: string) => apiRequest<ChatMessage[]>(`/conversations/${id}/messages`),
  sendMessage: (id: string, body: string) => apiRequest<ChatMessage>(`/conversations/${id}/messages`, { method: 'POST', body: JSON.stringify({ body }) }),

  reviews: (targetType: string, targetId: string) => apiRequest<Review[]>(`/reviews?targetType=${encodeURIComponent(targetType)}&targetId=${encodeURIComponent(targetId)}`),
  createReview: (body: unknown) => apiRequest<Review>('/reviews', { method: 'POST', body: JSON.stringify(body) }),
  createReport: (body: unknown) => apiRequest<Report>('/reports', { method: 'POST', body: JSON.stringify(body) }),
  myReports: () => apiRequest<Report[]>('/reports/mine'),

  pendingTurfs: () => apiRequest<Turf[]>('/admin/turfs/pending'),
  decideTurf: (id: string, approve: boolean, reason = '') => apiRequest<Turf>(`/admin/turfs/${id}`, { method: 'PATCH', body: JSON.stringify({ approve, reason }) }),
  pendingRefunds: () => apiRequest<Refund[]>('/admin/refunds'),
  decideRefund: (id: string, approve: boolean, note: string) => apiRequest<Refund>(`/admin/refunds/${id}`, { method: 'PATCH', body: JSON.stringify({ approve, note }) }),
  reports: () => apiRequest<Report[]>('/admin/reports'),
  resolveReport: (id: string, status: string, note: string) => apiRequest<Report>(`/admin/reports/${id}`, { method: 'PATCH', body: JSON.stringify({ status, note }) }),
  audit: () => apiRequest<AuditLog[]>('/admin/audit-logs'),
  users: () => apiRequest<any[]>('/admin/users'),
  adminTeams: () => apiRequest<Team[]>('/admin/teams'),
  adminTurfs: () => apiRequest<Turf[]>('/admin/turfs'),
  adminEvents: () => apiRequest<SportsEvent[]>('/admin/events'),
  adminBookings: () => apiRequest<Booking[]>('/admin/bookings'),
  adminPayments: () => apiRequest<Payment[]>('/admin/payments'),
  adminReviews: () => apiRequest<Review[]>('/admin/reviews'),
  moderateReview: (id: string, status: string) => apiRequest<Review>(`/reviews/${id}`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  updateUserStatus: (id: string, status: string) => apiRequest(`/admin/users/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),

  uploadImage: async (file: File, purpose: string) => {
    const data = new FormData()
    data.append('file', file)
    data.append('purpose', purpose)
    return apiRequest<{ secureUrl: string }>('/media/images', { method: 'POST', body: data })
  },
}
