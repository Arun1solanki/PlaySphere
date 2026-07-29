import type { PropsWithChildren, ReactNode } from 'react'
import { ArenaBackground } from './ArenaBackground'
import { Brand } from './Brand'

export function AuthLayout({ children, aside }: PropsWithChildren<{ aside: ReactNode }>) {
  return (
    <div className="auth-screen">
      <ArenaBackground />
      <header className="auth-header"><Brand /><span>Secure account access</span></header>
      <section className="auth-aside">{aside}</section>
      <main className="auth-main">{children}</main>
    </div>
  )
}
