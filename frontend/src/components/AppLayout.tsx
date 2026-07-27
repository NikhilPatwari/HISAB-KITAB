import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { CalendarCheck, Home, Plus, Users, Wallet } from 'lucide-react'
import type { ReactNode } from 'react'

const TABS = [
  { to: '/', label: 'Home', icon: Home, end: true },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck, end: false },
  { to: '/wages', label: 'Wages', icon: Wallet, end: false },
  { to: '/people', label: 'People', icon: Users, end: false },
]

/** Phone-shaped shell with a bottom tab bar and a floating add button. */
export default function AppLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const showFab = !pathname.startsWith('/add')

  return (
    <div className="app-shell">
      <main className="flex-1 pb-24">
        <Outlet />
      </main>

      {showFab && (
        <button
          type="button"
          onClick={() => navigate('/add')}
          className="fixed bottom-20 left-1/2 z-20 flex h-14 w-14 -translate-x-1/2 items-center
                     justify-center rounded-full bg-brand-600 text-white shadow-fab
                     transition active:scale-95 md:left-auto md:ml-[9.5rem] md:translate-x-0"
          style={{ marginBottom: 'env(safe-area-inset-bottom)' }}
          aria-label="Add an entry"
        >
          <Plus className="h-7 w-7" strokeWidth={2.5} />
        </button>
      )}

      <nav className="safe-bottom fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-md border-t border-slate-200 bg-white/95 backdrop-blur">
        <ul className="flex">
          {TABS.map(({ to, label, icon: Icon, end }) => (
            <li key={to} className="flex-1">
              <NavLink
                to={to}
                end={end}
                className={({ isActive }) =>
                  `flex flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium transition ${
                    isActive ? 'text-brand-600' : 'text-slate-400'
                  }`
                }
              >
                <Icon className="h-5 w-5" />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}

/** Sticky page header used by the inner screens. */
export function PageHeader({
  title,
  subtitle,
  left,
  right,
  tone = 'brand',
}: {
  title: string
  subtitle?: string
  left?: ReactNode
  right?: ReactNode
  tone?: 'brand' | 'plain'
}) {
  const brand = tone === 'brand'
  return (
    <header
      className={`safe-top sticky top-0 z-10 ${
        brand ? 'bg-brand-600 text-white' : 'border-b border-slate-200 bg-white text-slate-900'
      }`}
    >
      <div className="flex items-center gap-2 px-3 py-3">
        {left}
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-lg font-bold leading-tight">{title}</h1>
          {subtitle && (
            <p className={`truncate text-xs ${brand ? 'text-brand-100' : 'text-slate-500'}`}>
              {subtitle}
            </p>
          )}
        </div>
        {right}
      </div>
    </header>
  )
}

export function BackButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="-ml-1 flex h-9 w-9 items-center justify-center rounded-full transition active:bg-black/10"
      aria-label="Go back"
    >
      <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth={2}>
        <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </button>
  )
}
