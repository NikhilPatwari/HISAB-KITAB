import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Check, Info } from 'lucide-react'
import { useChangeWage, useEmployee } from '@/lib/queries'
import { errorMessage } from '@/lib/api'
import { fullDate, money, todayIso } from '@/lib/format'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { EmptyState, ErrorNote, Field, SectionTitle, Spinner } from '@/components/ui'

/**
 * Wage rates are dated history, not a single column, so a raise agreed last
 * month can be recorded on the date it was actually agreed and every day is
 * priced at the rate that applied then.
 */
export default function WageChangePage() {
  const { id } = useParams()
  const employeeId = id ? Number(id) : undefined
  const navigate = useNavigate()

  const employee = useEmployee(employeeId)
  const changeWage = useChangeWage(employeeId)

  const [dailyRate, setDailyRate] = useState('')
  const [effectiveFrom, setEffectiveFrom] = useState(todayIso())
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  if (employee.isLoading) return <Spinner />
  if (!employee.data) return <EmptyState title="Worker not found" />

  const person = employee.data
  const backdated = effectiveFrom < todayIso()
  const future = effectiveFrom > todayIso()

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    const rate = Number(dailyRate)
    if (!Number.isFinite(rate) || rate < 0) {
      setError('Enter a valid daily wage')
      return
    }

    try {
      await changeWage.mutateAsync({
        dailyRate: rate,
        effectiveFrom,
        note: note.trim() || null,
      })
      navigate(`/employees/${employeeId}`, { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not save the wage change'))
    }
  }

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title="Change daily wage"
        subtitle={person.name}
        left={<BackButton onClick={() => navigate(-1)} />}
      />

      <div className="card mx-4 mt-4 flex items-center justify-between p-4">
        <span className="text-sm text-slate-500">Current rate</span>
        <span className="text-lg font-bold text-slate-900">
          {money(person.dailyWageRate, true)}/day
        </span>
      </div>

      <form onSubmit={onSubmit} className="space-y-4 p-4">
        {error && <ErrorNote message={error} />}

        <Field label="New daily wage" required>
          <div className="relative">
            <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-lg font-semibold text-slate-400">
              ₹
            </span>
            <input
              className="input pl-8 text-2xl font-bold"
              type="number"
              inputMode="decimal"
              step="1"
              min="0"
              placeholder="500"
              value={dailyRate}
              onChange={(e) => setDailyRate(e.target.value)}
              required
              autoFocus
            />
          </div>
        </Field>

        <Field
          label="Effective from"
          required
          hint="Days before this date keep the old rate. A past date is fine."
        >
          <input
            className="input"
            type="date"
            value={effectiveFrom}
            onChange={(e) => setEffectiveFrom(e.target.value)}
            min={person.joinedOn}
            required
          />
        </Field>

        {backdated && (
          <div className="flex gap-2.5 rounded-xl bg-amber-50 px-3.5 py-3 text-sm text-amber-900 ring-1 ring-inset ring-amber-100">
            <Info className="mt-0.5 h-4 w-4 shrink-0" />
            <p>
              This is back-dated. Months that have already closed keep the wages they were
              paid at — reopen them from Settings if they need repricing.
            </p>
          </div>
        )}

        {future && (
          <div className="flex gap-2.5 rounded-xl bg-slate-100 px-3.5 py-3 text-sm text-slate-600">
            <Info className="mt-0.5 h-4 w-4 shrink-0" />
            <p>Starts in the future. Earnings keep using the current rate until then.</p>
          </div>
        )}

        <Field label="Reason">
          <input
            className="input"
            placeholder="Yearly increase, took on tractor work…"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            maxLength={255}
          />
        </Field>

        <button type="submit" className="btn-primary w-full" disabled={changeWage.isPending}>
          <Check className="h-5 w-5" />
          {changeWage.isPending ? 'Saving…' : 'Save wage change'}
        </button>
      </form>

      <SectionTitle>Rate history</SectionTitle>
      <ul className="divide-y divide-slate-100 bg-white">
        {person.rateHistory.map((rate) => (
          <li key={rate.id} className="flex items-center justify-between gap-3 px-4 py-3">
            <div className="min-w-0">
              <p className="text-sm font-medium text-slate-900">
                from {fullDate(rate.effectiveFrom)}
              </p>
              {rate.note && <p className="truncate text-xs text-slate-500">{rate.note}</p>}
            </div>
            <span className="shrink-0 font-semibold text-slate-900">
              {money(rate.dailyRate, true)}/day
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
