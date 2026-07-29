import { useMemo, useState, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronRight, Plus, Search, Settings, UserPlus, Users } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useDashboard, useEmployees } from '@/lib/queries'
import { absMoney, money } from '@/lib/format'
import { Avatar, BalanceText, EmptyState, Spinner } from '@/components/ui'
import type { EmployeeSummary } from '@/lib/types'

type Filter = 'all' | 'owing' | 'settled'

export default function HomePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<Filter>('all')

  const dashboard = useDashboard()
  const employees = useEmployees({ status: 'ACTIVE' })

  const visible = useMemo(() => {
    const rows = employees.data ?? []
    const needle = search.trim().toLowerCase()
    return rows
      .filter((e) => (needle ? matches(e, needle) : true))
      .filter((e) => {
        if (filter === 'owing') return e.balance < -0.005
        if (filter === 'settled') return Math.abs(e.balance) < 0.005
        return true
      })
      .sort((a, b) => a.balance - b.balance)
  }, [employees.data, search, filter])

  return (
    <div className="bg-slate-50">
      <header className="safe-top bg-brand-600 px-4 pb-6 pt-4 text-white">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-brand-100">
              {user?.organizationName}
            </p>
            <h1 className="text-xl font-bold">Hisab Kitab</h1>
          </div>
          <Link
            to="/settings"
            className="flex h-9 w-9 items-center justify-center rounded-full bg-white/15 transition active:bg-white/25"
            aria-label="Settings"
          >
            <Settings className="h-5 w-5" />
          </Link>
        </div>

        {dashboard.data && (
          <div className="mt-5">
            <p className="text-sm text-brand-100">Total outstanding with workers</p>
            <p className="mt-0.5 text-4xl font-bold tracking-tight">
              {absMoney(dashboard.data.totalReceivable)}
            </p>
            <p className="mt-1 text-sm text-brand-100">
              across {dashboard.data.employeesInDebt} of {dashboard.data.activeEmployees} workers
              {dashboard.data.totalPayable > 0 && (
                <> · {absMoney(dashboard.data.totalPayable)} unpaid wages</>
              )}
            </p>
          </div>
        )}
      </header>

      {dashboard.data && (
        <div className="-mt-4 grid grid-cols-3 gap-2 px-3">
          <StatTile label="Advances" value={dashboard.data.advancesThisMonth} tone="debit" />
          <StatTile label="Wages" value={dashboard.data.wagesThisMonth} tone="credit" />
          <StatTile label="Repaid" value={dashboard.data.repaymentsThisMonth} tone="credit" />
        </div>
      )}

      {/* Inline rather than a floating button, so it never covers a worker row. */}
      <div className="p-4 pb-1">
        <button type="button" className="btn-primary w-full" onClick={() => navigate('/add')}>
          <Plus className="h-5 w-5" /> Add entry
        </button>
      </div>

      <div className="sticky top-0 z-10 mt-2 bg-slate-50/95 px-4 pb-2 pt-2 backdrop-blur">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            className="input pl-9"
            placeholder="Search worker by name, code or village"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            type="search"
          />
        </div>

        <div className="mt-2 flex gap-2">
          <FilterChip active={filter === 'all'} onClick={() => setFilter('all')}>
            Everyone
          </FilterChip>
          <FilterChip active={filter === 'owing'} onClick={() => setFilter('owing')}>
            Owing
          </FilterChip>
          <FilterChip active={filter === 'settled'} onClick={() => setFilter('settled')}>
            Settled
          </FilterChip>
        </div>
      </div>

      {employees.isLoading ? (
        <Spinner label="Loading workers" />
      ) : visible.length === 0 ? (
        <EmptyState
          icon={<Users className="h-10 w-10" />}
          title={search ? 'No one matches that' : 'No workers yet'}
          hint={
            search
              ? 'Try a different name, code or village.'
              : 'Add your workers to start recording advances and wages.'
          }
          action={
            !search ? (
              <button className="btn-primary mt-1" onClick={() => navigate('/employees/new')}>
                <UserPlus className="h-4 w-4" /> Add a worker
              </button>
            ) : undefined
          }
        />
      ) : (
        <ul className="mt-2 divide-y divide-slate-100 bg-white">
          {visible.map((employee) => (
            <li key={employee.id}>
              <Link to={`/employees/${employee.id}`} className="list-row">
                <Avatar name={employee.name} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-slate-900">{employee.name}</p>
                  <p className="truncate text-xs text-slate-500">
                    {[employee.code, employee.village].filter(Boolean).join(' · ') ||
                      `${money(employee.dailyWageRate)}/day`}
                  </p>
                </div>
                <BalanceText balance={employee.balance} />
                <ChevronRight className="h-4 w-4 shrink-0 text-slate-300" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function matches(employee: EmployeeSummary, needle: string): boolean {
  return [employee.name, employee.code, employee.village, employee.phone]
    .filter(Boolean)
    .some((field) => field!.toLowerCase().includes(needle))
}

function StatTile({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone: 'credit' | 'debit'
}) {
  return (
    <div className="card px-3 py-2.5">
      <p className="text-[11px] font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p
        className={`mt-0.5 truncate text-sm font-bold ${
          tone === 'credit' ? 'text-credit-600' : 'text-debit-600'
        }`}
      >
        {money(value)}
      </p>
      <p className="text-[10px] text-slate-400">this month</p>
    </div>
  )
}

function FilterChip({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`chip transition ${
        active
          ? 'bg-brand-600 text-white'
          : 'bg-white text-slate-600 ring-1 ring-inset ring-slate-200'
      }`}
    >
      {children}
    </button>
  )
}
