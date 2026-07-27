import { useState, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowDownLeft,
  ArrowUpRight,
  CalendarDays,
  Pencil,
  Phone,
  Plus,
  Receipt,
  Trash2,
} from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useEmployee, useEmployeeMonth, useStatement, useVoidEntry } from '@/lib/queries'
import { absMoney, currentYearMonth, days, money, monthLabel, relativeDate } from '@/lib/format'
import { errorMessage } from '@/lib/api'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { Avatar, EmptyState, ErrorNote, SectionTitle, Spinner } from '@/components/ui'
import type { StatementRow } from '@/lib/types'

type Tab = 'ledger' | 'attendance' | 'about'

export default function EmployeePage() {
  const { id } = useParams()
  const employeeId = Number(id)
  const navigate = useNavigate()
  const { isOwner } = useAuth()

  const [tab, setTab] = useState<Tab>('ledger')
  const [month, setMonth] = useState(currentYearMonth())
  const [error, setError] = useState<string | null>(null)

  const employee = useEmployee(employeeId)
  const statement = useStatement(employeeId)
  const attendance = useEmployeeMonth(employeeId, month)
  const voidEntry = useVoidEntry()

  if (employee.isLoading) return <Spinner label="Loading" />
  if (!employee.data) return <EmptyState title="Worker not found" />

  const person = employee.data
  const owesYou = person.balance < 0
  const settled = Math.abs(person.balance) < 0.005

  async function onVoid(row: StatementRow) {
    if (row.wageRunId) {
      setError('This is a posted wage entry. Void the whole wage run from the Wages tab.')
      return
    }
    if (!window.confirm(`Void this entry of ${absMoney(row.amount)}? It stays on the record as cancelled.`)) {
      return
    }
    setError(null)
    try {
      await voidEntry.mutateAsync(row.id)
    } catch (err) {
      setError(errorMessage(err, 'Could not void that entry'))
    }
  }

  return (
    <div className="bg-slate-50 pb-8">
      <PageHeader
        title={person.name}
        subtitle={[person.code, person.village].filter(Boolean).join(' · ') || undefined}
        left={<BackButton onClick={() => navigate(-1)} />}
        right={
          isOwner ? (
            <button
              type="button"
              onClick={() => navigate(`/employees/${person.id}/edit`)}
              className="flex h-9 w-9 items-center justify-center rounded-full transition active:bg-black/10"
              aria-label="Edit worker"
            >
              <Pencil className="h-5 w-5" />
            </button>
          ) : undefined
        }
      />

      <section className="bg-brand-600 px-4 pb-6 text-center text-white">
        <div className="mx-auto mb-3 w-fit rounded-full ring-4 ring-white/20">
          <Avatar name={person.name} size="lg" />
        </div>
        <p className="text-sm text-brand-100">
          {settled ? 'All settled up' : owesYou ? `${person.name} owes you` : `You owe ${person.name}`}
        </p>
        <p className="mt-0.5 text-3xl font-bold tracking-tight">{absMoney(person.balance, true)}</p>
        <p className="mt-1 text-xs text-brand-100">
          {money(person.dailyWageRate)} per day · joined {relativeDate(person.joinedOn)}
        </p>

        <div className="mt-4 flex justify-center gap-2">
          <QuickAction
            icon={<ArrowUpRight className="h-4 w-4" />}
            label="Give advance"
            onClick={() => navigate(`/add?employeeId=${person.id}&type=ADVANCE`)}
          />
          <QuickAction
            icon={<ArrowDownLeft className="h-4 w-4" />}
            label="Record repayment"
            onClick={() => navigate(`/add?employeeId=${person.id}&type=REPAYMENT`)}
          />
        </div>
      </section>

      <nav className="flex border-b border-slate-200 bg-white">
        {(['ledger', 'attendance', 'about'] as Tab[]).map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setTab(value)}
            className={`flex-1 border-b-2 py-3 text-sm font-semibold capitalize transition ${
              tab === value
                ? 'border-brand-600 text-brand-700'
                : 'border-transparent text-slate-400'
            }`}
          >
            {value}
          </button>
        ))}
      </nav>

      {error && (
        <div className="px-4 pt-3">
          <ErrorNote message={error} />
        </div>
      )}

      {tab === 'ledger' && (
        <>
          {statement.isLoading ? (
            <Spinner />
          ) : !statement.data || statement.data.rows.length === 0 ? (
            <EmptyState
              icon={<Receipt className="h-10 w-10" />}
              title="No entries yet"
              hint="Advances, repayments and monthly wages will appear here."
              action={
                <button className="btn-primary mt-1" onClick={() => navigate(`/add?employeeId=${person.id}`)}>
                  <Plus className="h-4 w-4" /> Add an entry
                </button>
              }
            />
          ) : (
            <>
              <div className="grid grid-cols-2 gap-2 p-3">
                <SummaryCard
                  label="Given out"
                  value={statement.data.totalGivenOut}
                  tone="text-debit-600"
                />
                <SummaryCard
                  label="Earned & repaid"
                  value={statement.data.totalEarned}
                  tone="text-credit-600"
                />
              </div>

              <SectionTitle>Statement</SectionTitle>
              <ul className="divide-y divide-slate-100 bg-white">
                {[...statement.data.rows].reverse().map((row) => (
                  <li key={row.id} className="flex items-start gap-3 px-4 py-3">
                    <div
                      className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${
                        row.signedAmount < 0
                          ? 'bg-debit-50 text-debit-600'
                          : 'bg-credit-50 text-credit-600'
                      }`}
                    >
                      {row.signedAmount < 0 ? (
                        <ArrowUpRight className="h-4 w-4" />
                      ) : (
                        <ArrowDownLeft className="h-4 w-4" />
                      )}
                    </div>

                    <div className="min-w-0 flex-1">
                      <p
                        className={`font-medium ${
                          row.voided ? 'text-slate-400 line-through' : 'text-slate-900'
                        }`}
                      >
                        {row.typeLabel}
                      </p>
                      <p className="truncate text-xs text-slate-500">
                        {relativeDate(row.entryDate)}
                        {row.employerName && ` · ${row.employerName}`}
                        {row.note && ` · ${row.note}`}
                      </p>
                    </div>

                    <div className="shrink-0 text-right">
                      <p
                        className={`font-semibold ${
                          row.voided
                            ? 'text-slate-400 line-through'
                            : row.signedAmount < 0
                              ? 'text-debit-600'
                              : 'text-credit-600'
                        }`}
                      >
                        {row.signedAmount < 0 ? '−' : '+'}
                        {absMoney(row.amount)}
                      </p>
                      <p className="text-[11px] text-slate-400">
                        bal {absMoney(row.runningBalance)}
                      </p>
                    </div>

                    {!row.voided && !row.wageRunId && (
                      <button
                        type="button"
                        onClick={() => onVoid(row)}
                        className="mt-1 shrink-0 text-slate-300 transition active:text-red-500"
                        aria-label="Void entry"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </>
      )}

      {tab === 'attendance' && (
        <div className="p-3">
          <div className="card mb-3 flex items-center gap-2 p-3">
            <CalendarDays className="h-5 w-5 text-slate-400" />
            <input
              type="month"
              className="input flex-1"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
            />
          </div>

          {attendance.isLoading ? (
            <Spinner />
          ) : attendance.data ? (
            <>
              <div className="card mb-3 p-4">
                <p className="text-sm font-semibold text-slate-700">{monthLabel(month)}</p>
                <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                  <Stat label="Working days" value={String(attendance.data.workingDaysInPeriod)} />
                  <Stat label="Payable days" value={days(attendance.data.payableDays)} />
                  <Stat label="Absent" value={String(attendance.data.absentDays)} />
                  <Stat label="Half days" value={String(attendance.data.halfDays)} />
                </div>
                <p className="mt-3 border-t border-slate-100 pt-3 text-xs text-slate-500">
                  Every working day counts as present unless it is marked below.
                </p>
              </div>

              {attendance.data.exceptions.length === 0 ? (
                <EmptyState
                  title="Full attendance"
                  hint="No absences or half days recorded for this month."
                />
              ) : (
                <ul className="card divide-y divide-slate-100 overflow-hidden">
                  {attendance.data.exceptions.map((row) => (
                    <li key={row.id} className="flex items-center gap-3 px-4 py-3">
                      <span
                        className={`chip ${
                          row.status === 'ABSENT'
                            ? 'bg-red-50 text-red-700'
                            : row.status === 'HALF_DAY'
                              ? 'bg-amber-50 text-amber-700'
                              : 'bg-sky-50 text-sky-700'
                        }`}
                      >
                        {row.status?.replace('_', ' ').toLowerCase()}
                      </span>
                      <span className="flex-1 text-sm text-slate-700">
                        {relativeDate(row.workDate)}
                      </span>
                      {row.note && (
                        <span className="truncate text-xs text-slate-400">{row.note}</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </>
          ) : null}
        </div>
      )}

      {tab === 'about' && (
        <div className="p-3">
          <dl className="card divide-y divide-slate-100 overflow-hidden">
            <Row label="Code" value={person.code ?? '—'} />
            <Row
              label="Phone"
              value={
                person.phone ? (
                  <a href={`tel:${person.phone}`} className="flex items-center gap-1 text-brand-700">
                    <Phone className="h-3.5 w-3.5" /> {person.phone}
                  </a>
                ) : (
                  '—'
                )
              }
            />
            <Row label="Village" value={person.village ?? '—'} />
            <Row label="Daily wage" value={money(person.dailyWageRate, true)} />
            <Row label="Joined" value={relativeDate(person.joinedOn)} />
            <Row label="Status" value={person.status === 'ACTIVE' ? 'Working' : 'Left'} />
            {person.notes && <Row label="Notes" value={person.notes} />}
          </dl>

          <SectionTitle>Wage history</SectionTitle>
          <ul className="card divide-y divide-slate-100 overflow-hidden">
            {person.rateHistory.map((rate) => (
              <li key={rate.id} className="flex items-center justify-between px-4 py-3 text-sm">
                <span className="text-slate-600">from {relativeDate(rate.effectiveFrom)}</span>
                <span className="font-semibold">{money(rate.dailyRate, true)}/day</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function QuickAction({
  icon,
  label,
  onClick,
}: {
  icon: ReactNode
  label: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-1.5 rounded-full bg-white/15 px-3.5 py-2 text-xs font-semibold
                 backdrop-blur transition active:bg-white/25"
    >
      {icon}
      {label}
    </button>
  )
}

function SummaryCard({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <div className="card p-3">
      <p className="text-[11px] font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className={`mt-0.5 text-lg font-bold ${tone}`}>{absMoney(value)}</p>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-[11px] uppercase tracking-wide text-slate-500">{label}</p>
      <p className="text-lg font-bold text-slate-900">{value}</p>
    </div>
  )
}

function Row({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 px-4 py-3">
      <dt className="text-sm text-slate-500">{label}</dt>
      <dd className="text-right text-sm font-medium text-slate-900">{value}</dd>
    </div>
  )
}
