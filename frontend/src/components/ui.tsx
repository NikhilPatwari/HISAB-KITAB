import type { ReactNode } from 'react'
import { Loader2 } from 'lucide-react'
import { absMoney, initials } from '@/lib/format'

const AVATAR_TONES = [
  'bg-rose-100 text-rose-700',
  'bg-amber-100 text-amber-700',
  'bg-emerald-100 text-emerald-700',
  'bg-sky-100 text-sky-700',
  'bg-violet-100 text-violet-700',
  'bg-orange-100 text-orange-700',
  'bg-teal-100 text-teal-700',
]

/** Colour is derived from the name so a person keeps the same circle everywhere. */
function toneFor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i += 1) hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  return AVATAR_TONES[hash % AVATAR_TONES.length]
}

export function Avatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' }) {
  const dimensions =
    size === 'lg' ? 'h-16 w-16 text-xl' : size === 'sm' ? 'h-8 w-8 text-xs' : 'h-11 w-11 text-sm'
  return (
    <span
      className={`flex shrink-0 items-center justify-center rounded-full font-bold ${dimensions} ${toneFor(name)}`}
      aria-hidden="true"
    >
      {initials(name)}
    </span>
  )
}

/**
 * The signed balance in Splitwise's phrasing: green when the employee owes the
 * farm, orange when the farm owes the employee.
 */
export function BalanceText({
  balance,
  align = 'right',
  size = 'md',
}: {
  balance: number
  align?: 'left' | 'right'
  size?: 'sm' | 'md' | 'lg'
}) {
  const settled = Math.abs(balance) < 0.005
  const owesYou = balance < 0
  const tone = settled ? 'text-slate-400' : owesYou ? 'text-credit-600' : 'text-debit-600'
  const amountSize = size === 'lg' ? 'text-2xl' : size === 'sm' ? 'text-sm' : 'text-base'

  return (
    <div className={`${align === 'right' ? 'text-right' : 'text-left'} ${tone} leading-tight`}>
      <div className="text-[11px] font-medium uppercase tracking-wide opacity-80">
        {settled ? 'settled up' : owesYou ? 'owes you' : 'you owe'}
      </div>
      {!settled && <div className={`font-bold ${amountSize}`}>{absMoney(balance)}</div>}
    </div>
  )
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-12 text-slate-400">
      <Loader2 className="h-6 w-6 animate-spin" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  )
}

export function EmptyState({
  icon,
  title,
  hint,
  action,
}: {
  icon?: ReactNode
  title: string
  hint?: string
  action?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center">
      {icon && <div className="text-slate-300">{icon}</div>}
      <p className="font-semibold text-slate-700">{title}</p>
      {hint && <p className="max-w-xs text-sm text-slate-500">{hint}</p>}
      {action}
    </div>
  )
}

export function ErrorNote({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="rounded-xl bg-red-50 px-3.5 py-3 text-sm font-medium text-red-700 ring-1 ring-inset ring-red-100"
    >
      {message}
    </div>
  )
}

export function Field({
  label,
  hint,
  children,
  required,
}: {
  label: string
  hint?: string
  children: ReactNode
  required?: boolean
}) {
  return (
    <label className="block">
      <span className="label">
        {label}
        {required && <span className="ml-0.5 text-red-500">*</span>}
      </span>
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    </label>
  )
}

export function SectionTitle({ children, action }: { children: ReactNode; action?: ReactNode }) {
  return (
    <div className="mb-2 mt-5 flex items-center justify-between px-4">
      <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500">{children}</h2>
      {action}
    </div>
  )
}
