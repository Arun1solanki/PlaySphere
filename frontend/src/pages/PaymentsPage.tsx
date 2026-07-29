import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { Payment, Refund } from '../types/domain'

type RazorpaySuccess = {
  razorpay_order_id: string
  razorpay_payment_id: string
  razorpay_signature: string
}

type RazorpayOptions = {
  key: string
  amount: number
  currency: string
  name: string
  description: string
  order_id: string
  handler: (response: RazorpaySuccess) => void | Promise<void>
  prefill?: { name?: string; email?: string }
  theme?: { color?: string }
  modal?: { ondismiss?: () => void }
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => { open(): void }
  }
}

const razorpayKeyId = import.meta.env.VITE_RAZORPAY_KEY_ID as string | undefined

async function loadRazorpayCheckout(): Promise<void> {
  if (window.Razorpay) return
  await new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-playsphere-razorpay]')
    if (existing) {
      existing.addEventListener('load', () => resolve(), { once: true })
      existing.addEventListener('error', () => reject(new Error('Unable to load Razorpay Checkout')), { once: true })
      return
    }
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.async = true
    script.dataset.playsphereRazorpay = 'true'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Unable to load Razorpay Checkout'))
    document.head.appendChild(script)
  })
}

export function PaymentsPage({ role, mode }: { role: PlatformRole; mode: string }) {
  const [items, setItems] = useState<Payment[]>([])
  const [refunds, setRefunds] = useState<Refund[]>([])
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const earningsMode = mode === 'earnings'

  async function load() {
    try {
      if (earningsMode && role === 'ORGANIZER') setItems(await platformApi.organizerEarnings())
      else if (earningsMode && role === 'TURF_OWNER') setItems(await platformApi.ownerEarnings())
      else {
        setItems(await platformApi.payments())
        setRefunds(await platformApi.refunds())
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to load payments')
    }
  }

  useEffect(() => { void load() }, [role, mode])

  const total = useMemo(
    () => items.filter((item) => item.status === 'SUCCESS').reduce((sum, item) => sum + Number(item.amount), 0),
    [items],
  )

  async function openRazorpay(payment: Payment) {
    if (!razorpayKeyId) {
      throw new Error('VITE_RAZORPAY_KEY_ID is missing from the frontend environment')
    }
    if (!payment.providerOrderId) {
      throw new Error('Razorpay order ID was not returned by the backend')
    }
    await loadRazorpayCheckout()
    if (!window.Razorpay) throw new Error('Razorpay Checkout did not initialize')

    const checkout = new window.Razorpay({
      key: razorpayKeyId,
      amount: Math.round(Number(payment.amount) * 100),
      currency: payment.currency,
      name: 'PlaySphere',
      description: payment.purpose.replaceAll('_', ' '),
      order_id: payment.providerOrderId,
      theme: { color: '#42e8ff' },
      modal: {
        ondismiss: () => setNotice('Payment window closed. You can try again from the same reference.'),
      },
      handler: async (response) => {
        try {
          await platformApi.verifyRazorpay({
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          })
          setNotice('Razorpay payment verified successfully.')
          await load()
        } catch (error) {
          setNotice(error instanceof Error ? error.message : 'Payment verification failed')
        }
      },
    })
    checkout.open()
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setBusy(true)
    try {
      const payment = await platformApi.createPayment({
        purpose: form.get('purpose'),
        referenceId: form.get('referenceId'),
        amount: 0,
      })
      if (payment.provider === 'DEVELOPMENT') {
        setNotice('Development payment completed.')
        await load()
      } else if (payment.provider === 'RAZORPAY') {
        setNotice('Razorpay order created. Complete payment in the secure checkout window.')
        await openRazorpay(payment)
      } else {
        setNotice(`Payment order created with ${payment.provider}.`)
        await load()
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to create payment')
    } finally {
      setBusy(false)
    }
  }

  async function requestRefund(payment: Payment) {
    const reason = window.prompt('Why are you requesting this refund?', 'Booking or event cancelled')
    if (!reason) return
    try {
      await platformApi.requestRefund(payment.id, { reason, amount: payment.amount })
      setNotice('Refund request submitted for Admin review.')
      await load()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to request refund')
    }
  }

  return (
    <RoleShell role={role}>
      <section className="module-header">
        <p className="eyebrow">MONEY FLOW</p>
        <h2>{earningsMode ? 'Earnings' : 'Payments & Refunds'}</h2>
        <p>{earningsMode ? 'Verified revenue linked to your events or turf bookings.' : 'Pay booking and registration references, review transactions, and request eligible refunds.'}</p>
      </section>
      {notice && <p className="workspace-notice">{notice}</p>}

      {earningsMode && <section className="panel earnings-banner"><span>Verified gross earnings</span><strong>₹{total.toFixed(2)}</strong><small>Refunded records remain visible for reconciliation.</small></section>}

      <div className={earningsMode ? '' : 'workspace-grid'}>
        <section className="panel workspace-list">
          <h3>{earningsMode ? 'Revenue ledger' : 'Transaction history'}</h3>
          {items.length === 0 && <p className="muted-copy">No transactions are available.</p>}
          {items.map((payment) => (
            <article className="resource-card" key={payment.id}>
              <div><b>{payment.purpose}</b><span>{payment.status} · ₹{payment.amount} {payment.currency}</span><small>{payment.referenceId} · {payment.provider}</small></div>
              {!earningsMode && payment.status === 'SUCCESS' && <button onClick={() => void requestRefund(payment)}>Request refund</button>}
            </article>
          ))}
          {!earningsMode && refunds.map((refund) => <article className="resource-card refund-row" key={refund.id}><div><b>Refund · ₹{refund.requestedAmount}</b><span>{refund.status}</span><small>{refund.reason}</small></div></article>)}
        </section>

        {!earningsMode && (
          <form className="panel workspace-form" onSubmit={create}>
            <h3>Pay a reference</h3>
            <label>Purpose<select name="purpose"><option value="BOOKING">Turf booking</option><option value="EVENT_REGISTRATION">Event registration</option></select></label>
            <label>Booking or registration ID<input name="referenceId" required /></label>
            <p className="muted-copy">The backend calculates the trusted amount from the booking or event. The browser cannot change it.</p>
            <button className="primary-button" disabled={busy}>{busy ? 'Preparing payment…' : 'Continue to payment'}</button>
          </form>
        )}
      </div>
    </RoleShell>
  )
}
