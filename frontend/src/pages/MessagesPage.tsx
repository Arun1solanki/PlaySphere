import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { RoleShell } from '../components/RoleShell'
import { getAccessToken } from '../lib/api'
import { platformApi } from '../lib/platformApi'
import type { PlatformRole } from '../types/auth'
import type { ChatMessage, Conversation } from '../types/domain'

function websocketUrl(): string {
  const configured = import.meta.env.VITE_WS_URL as string | undefined
  if (configured) return configured
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

export function MessagesPage({ role }: { role: PlatformRole }) {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [selected, setSelected] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [notice, setNotice] = useState('')
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    void platformApi.conversations()
      .then((result) => {
        setConversations(result)
        if (result[0]) setSelected(result[0].id)
      })
      .catch((error: Error) => setNotice(error.message))
  }, [])

  useEffect(() => {
    if (!selected) return

    let cancelled = false
    void platformApi.messages(selected)
      .then((result) => { if (!cancelled) setMessages(result) })
      .catch((error: Error) => setNotice(error.message))

    const token = getAccessToken()
    if (token) {
      const client = new Client({
        brokerURL: websocketUrl(),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 4_000,
        heartbeatIncoming: 10_000,
        heartbeatOutgoing: 10_000,
        onConnect: () => {
          client.subscribe(`/topic/conversations/${selected}`, (frame) => {
            const incoming = JSON.parse(frame.body) as ChatMessage
            setMessages((current) => current.some((item) => item.id === incoming.id) ? current : [...current, incoming])
          })
        },
        onStompError: () => setNotice('Live chat connection was interrupted. REST fallback remains available.'),
      })
      client.activate()
      clientRef.current = client
    }

    const fallback = window.setInterval(() => {
      void platformApi.messages(selected).then(setMessages).catch(() => undefined)
    }, 15_000)

    return () => {
      cancelled = true
      window.clearInterval(fallback)
      void clientRef.current?.deactivate()
      clientRef.current = null
    }
  }, [selected])

  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selected) return
    const form = new FormData(event.currentTarget)
    try {
      const sent = await platformApi.sendMessage(selected, String(form.get('body')))
      setMessages((current) => current.some((item) => item.id === sent.id) ? current : [...current, sent])
      event.currentTarget.reset()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Unable to send message')
    }
  }

  return (
    <RoleShell role={role}>
      <section className="module-header"><p className="eyebrow">COMMUNICATION</p><h2>Messages</h2><p>Authenticated STOMP updates with a REST history and recovery fallback.</p></section>
      {notice && <p className="workspace-notice">{notice}</p>}
      <div className="chat-layout">
        <aside className="panel conversation-list">
          {conversations.length === 0 && <p className="muted-copy">Team and event conversations are created automatically when those workflows begin.</p>}
          {conversations.map((conversation) => <button className={selected === conversation.id ? 'active' : ''} onClick={() => setSelected(conversation.id)} key={conversation.id}>{conversation.title}<small>{conversation.conversationType}</small></button>)}
        </aside>
        <section className="panel chat-panel">
          {selected ? <><div className="chat-messages">{messages.map((message) => <article key={message.id}><b>{message.senderDisplayName || message.senderUserId.slice(0, 8)}</b><p>{message.body}</p><small>{new Date(message.createdAt).toLocaleTimeString()}</small></article>)}</div><form onSubmit={send} className="chat-compose"><input name="body" required maxLength={1200} placeholder="Write a message..." /><button>Send</button></form></> : <p>Select a conversation.</p>}
        </section>
      </div>
    </RoleShell>
  )
}
