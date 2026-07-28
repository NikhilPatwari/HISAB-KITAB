import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Check, X } from 'lucide-react'
import { useCreateEntry, useEmployees, useEmployers } from '@/lib/queries'
import { absMoney, todayIso } from '@/lib/format'
import { errorMessage } from '@/lib/api'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { Avatar, ErrorNote, Field, Spinner } from '@/components/ui'
import type { EntryType } from '@/lib/types'

/**
 * WAGE is missing on purpose — wages are posted by the monthly run so they stay
 * reconcilable with attendance.
 */
const TYPES: { value: EntryType; label: string; hint: string; debit: boolean }[] = [
  { value: 'ADVANCE', label: 'Advance', hint: 'Cash lent to the worker', debit: true },
  { value: 'REPAYMENT', label: 'Repayment', hint: 'Worker returned cash', debit: false },
  { value: 'PAYOUT', label: 'Wage paid', hint: 'Paid out earned wages', debit: true },
  { value: 'EXPENSE_ON_BEHALF', label: 'Paid on behalf', hint: 'Hospital, shop, fees', debit: true },
  { value: 'BONUS', label: 'Bonus', hint: 'Extra on top of wages', debit: false },
  { value: 'DEDUCTION', label: 'Deduction', hint: 'Penalty or recovery', debit: true },
  { value: 'ADJUSTMENT', label: 'Adjustment', hint: 'Manual correction', debit: true },
]

export default function AddEntryPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()

  const employees = useEmployees({ status: 'ACTIVE' })
  const employers = useEmployers()
  const createEntry = useCreateEntry()

  const [employeeId, setEmployeeId] = useState<number | null>(
    params.get('employeeId') ? Number(params.get('employeeId')) : null,
  )
  // The type can arrive from a quick-action link, so it is checked against the
  // list rather than trusted — WAGE in particular is not offered here.
  const [entryType, setEntryType] = useState<EntryType>(() => {
    const requested = params.get('type')
    return TYPES.some((t) => t.value === requested) ? (requested as EntryType) : 'ADVANCE'
  })
  const [amount, setAmount] = useState('')
  const [entryDate, setEntryDate] = useState(todayIso())
  const [employerId, setEmployerId] = useState<number | null>(null)
  const [creditsEmployee, setCreditsEmployee] = useState(false)
  const [note, setNote] = useState('')
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)

  const selected = employees.data?.find((e) => e.id === employeeId) ?? null
  const type = TYPES.find((t) => t.value === entryType)!

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    const rows = employees.data ?? []
    return needle
      ? rows.filter((e) =>
          [e.name, e.code, e.village].filter(Boolean).some((f) => f!.toLowerCase().includes(needle)),
        )
      : rows
  }, [employees.data, search])

  // Default the funder to the only employer on file, which is the common case.
  const soleEmployerId = employers.data?.length === 1 ? employers.data[0].id : null
  const effectiveEmployerId = employerId ?? soleEmployerId

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    if (!employeeId) {
      setError('Choose a worker first')
      return
    }
    const value = Number(amount)
    if (!Number.isFinite(value) || value <= 0) {
      setError('Enter an amount greater than zero')
      return
    }

    try {
      await createEntry.mutateAsync({
        employeeId,
        employerId: effectiveEmployerId,
        entryType,
        amount: value,
        entryDate,
        creditsEmployee: entryType === 'ADJUSTMENT' ? creditsEmployee : null,
        note: note.trim() || null,
      })
      navigate(`/employees/${employeeId}`, { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not save that entry'))
    }
  }

  if (employees.isLoading) return <Spinner label="Loading workers" />

  // Step one: pick the worker.
  if (!selected) {
    return (
      <div className="bg-slate-50">
        <PageHeader
          title="Who is this for?"
          left={<BackButton onClick={() => navigate(-1)} />}
        />
        <div className="bg-white px-4 py-3">
          <input
            className="input"
            placeholder="Search workers"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            type="search"
            autoFocus
          />
        </div>
        <ul className="divide-y divide-slate-100 bg-white">
          {filtered.map((employee) => (
            <li key={employee.id}>
              <button
                type="button"
                className="list-row"
                onClick={() => setEmployeeId(employee.id)}
              >
                <Avatar name={employee.name} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold">{employee.name}</p>
                  <p className="truncate text-xs text-slate-500">
                    {[employee.code, employee.village].filter(Boolean).join(' · ')}
                  </p>
                </div>
                <span
                  className={`text-sm font-semibold ${
                    employee.balance < 0 ? 'text-debit-600' : 'text-slate-400'
                  }`}
                >
                  {employee.balance < 0 ? absMoney(employee.balance) : '—'}
                </span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    )
  }

  // Step two: the entry itself.
  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title="New entry"
        subtitle={selected.name}
        left={
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="flex h-9 w-9 items-center justify-center rounded-full transition active:bg-black/10"
            aria-label="Cancel"
          >
            <X className="h-5 w-5" />
          </button>
        }
      />

      <form onSubmit={onSubmit} className="space-y-4 p-4">
        {error && <ErrorNote message={error} />}

        <button
          type="button"
          onClick={() => setEmployeeId(null)}
          className="card flex w-full items-center gap-3 p-3 text-left"
        >
          <Avatar name={selected.name} />
          <div className="min-w-0 flex-1">
            <p className="truncate font-semibold">{selected.name}</p>
            <p className="text-xs text-slate-500">Tap to choose someone else</p>
          </div>
        </button>

        <div>
          <span className="label">What happened?</span>
          <div className="grid grid-cols-2 gap-2">
            {TYPES.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setEntryType(option.value)}
                className={`rounded-xl border px-3 py-2.5 text-left transition ${
                  entryType === option.value
                    ? 'border-brand-500 bg-brand-50 ring-2 ring-brand-100'
                    : 'border-slate-200 bg-white'
                }`}
              >
                <span className="block text-sm font-semibold text-slate-900">{option.label}</span>
                <span className="block text-[11px] leading-tight text-slate-500">{option.hint}</span>
              </button>
            ))}
          </div>
        </div>

        <Field
          label="Amount"
          required
          hint={
            entryType === 'ADJUSTMENT'
              ? undefined
              : type.debit
                ? 'Increases what the worker owes you'
                : 'Reduces what the worker owes you'
          }
        >
          <div className="relative">
            <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-lg font-semibold text-slate-400">
              ₹
            </span>
            <input
              className="input pl-8 text-2xl font-bold"
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0.01"
              placeholder="0"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
              autoFocus
            />
          </div>
        </Field>

        {entryType === 'ADJUSTMENT' && (
          <div>
            <span className="label">Direction</span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setCreditsEmployee(false)}
                className={`rounded-xl border px-3 py-2.5 text-sm font-semibold transition ${
                  !creditsEmployee
                    ? 'border-debit-500 bg-debit-50 text-debit-700'
                    : 'border-slate-200 bg-white text-slate-600'
                }`}
              >
                Worker owes more
              </button>
              <button
                type="button"
                onClick={() => setCreditsEmployee(true)}
                className={`rounded-xl border px-3 py-2.5 text-sm font-semibold transition ${
                  creditsEmployee
                    ? 'border-credit-500 bg-credit-50 text-credit-700'
                    : 'border-slate-200 bg-white text-slate-600'
                }`}
              >
                Worker owes less
              </button>
            </div>
          </div>
        )}

        <Field label="Date" required>
          <input
            className="input"
            type="date"
            value={entryDate}
            max={todayIso()}
            onChange={(e) => setEntryDate(e.target.value)}
            required
          />
        </Field>

        {(employers.data?.length ?? 0) > 1 && (
          <Field label="Whose money?" hint="Recorded so each partner's outlay stays separate">
            <select
              className="input"
              value={effectiveEmployerId ?? ''}
              onChange={(e) => setEmployerId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Not specified</option>
              {employers.data
                ?.filter((e) => e.active)
                .map((employer) => (
                  <option key={employer.id} value={employer.id}>
                    {employer.name}
                  </option>
                ))}
            </select>
          </Field>
        )}

        <Field label="Note">
          <input
            className="input"
            placeholder="What was it for?"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            maxLength={500}
          />
        </Field>

        <button type="submit" className="btn-primary w-full" disabled={createEntry.isPending}>
          <Check className="h-5 w-5" />
          {createEntry.isPending ? 'Saving…' : 'Save entry'}
        </button>
      </form>
    </div>
  )
}
