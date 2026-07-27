import { useState } from 'react'
import { CheckCircle2, History, Lock, Undo2 } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { usePostWages, useVoidWageRun, useWagePreview, useWageRuns } from '@/lib/queries'
import { absMoney, currentYearMonth, days, fullDate, money, monthLabel } from '@/lib/format'
import { errorMessage } from '@/lib/api'
import { EmptyState, ErrorNote, SectionTitle, Spinner } from '@/components/ui'

/**
 * The monthly settlement: preview what each worker earned, then post it once.
 * Posting writes the wage entries that eat into their advances.
 */
export default function WagesPage() {
  const { isOwner } = useAuth()
  const [month, setMonth] = useState(currentYearMonth())
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const preview = useWagePreview(month)
  const runs = useWageRuns()
  const postWages = usePostWages()
  const voidRun = useVoidWageRun()

  async function onPost() {
    if (!preview.data) return
    const confirmed = window.confirm(
      `Post ${absMoney(preview.data.totalAmount)} of wages for ${monthLabel(month)} across ` +
        `${preview.data.lines.length} workers? This adds a wage entry to each statement.`,
    )
    if (!confirmed) return

    setError(null)
    setNotice(null)
    try {
      await postWages.mutateAsync(month)
      setNotice(`${monthLabel(month)} posted.`)
    } catch (err) {
      setError(errorMessage(err, 'Could not post wages'))
    }
  }

  async function onVoid(id: number) {
    if (!window.confirm('Void this wage run? Every wage entry it created will be cancelled.')) return
    setError(null)
    setNotice(null)
    try {
      await voidRun.mutateAsync(id)
      setNotice('Wage run voided. You can correct attendance and post again.')
    } catch (err) {
      setError(errorMessage(err, 'Could not void that run'))
    }
  }

  return (
    <div className="bg-slate-50 pb-6">
      <header className="safe-top bg-brand-600 px-4 pb-5 pt-4 text-white">
        <h1 className="text-lg font-bold">Wages</h1>
        <p className="text-xs text-brand-100">Working days minus absences, at each worker's rate</p>

        <input
          type="month"
          value={month}
          onChange={(e) => setMonth(e.target.value)}
          className="mt-3 w-full rounded-xl bg-white/15 px-3 py-2.5 text-center text-sm
                     font-semibold text-white outline-none [color-scheme:dark]"
        />

        {preview.data && (
          <div className="mt-4 text-center">
            <p className="text-xs text-brand-100">Total for {monthLabel(month)}</p>
            <p className="text-3xl font-bold tracking-tight">
              {absMoney(preview.data.totalAmount)}
            </p>
            <p className="mt-0.5 text-xs text-brand-100">
              {preview.data.lines.length} workers · {preview.data.workingDaysInPeriod} working days
            </p>
          </div>
        )}
      </header>

      <div className="space-y-3 p-4">
        {error && <ErrorNote message={error} />}
        {notice && (
          <div className="rounded-xl bg-credit-50 px-3.5 py-3 text-sm font-medium text-credit-700 ring-1 ring-inset ring-green-100">
            {notice}
          </div>
        )}

        {preview.data?.alreadyPosted ? (
          <div className="card flex items-start gap-3 p-4">
            <Lock className="mt-0.5 h-5 w-5 shrink-0 text-slate-400" />
            <div>
              <p className="font-semibold text-slate-900">{monthLabel(month)} is posted</p>
              <p className="mt-0.5 text-sm text-slate-500">
                The figures below are what was calculated. Void the run under History to change
                attendance and post it again.
              </p>
            </div>
          </div>
        ) : isOwner ? (
          <button
            type="button"
            className="btn-primary w-full"
            onClick={onPost}
            disabled={postWages.isPending || !preview.data || preview.data.lines.length === 0}
          >
            <CheckCircle2 className="h-5 w-5" />
            {postWages.isPending ? 'Posting…' : `Post ${monthLabel(month)} wages`}
          </button>
        ) : (
          <p className="rounded-xl bg-white px-3.5 py-3 text-sm text-slate-500 shadow-card">
            Only an owner can post wages.
          </p>
        )}
      </div>

      {preview.isLoading ? (
        <Spinner />
      ) : !preview.data || preview.data.lines.length === 0 ? (
        <EmptyState
          title="Nothing to calculate"
          hint="No workers were on the books during this month."
        />
      ) : (
        <>
          <SectionTitle>Breakdown</SectionTitle>
          <ul className="divide-y divide-slate-100 bg-white">
            {preview.data.lines.map((line) => (
              <li key={line.employeeId} className="flex items-center gap-3 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-slate-900">{line.employeeName}</p>
                  <p className="text-xs text-slate-500">
                    {days(line.payableDays)} of {line.eligibleDays} days · {money(line.dailyRate)}
                    /day
                    {line.absentDays > 0 && ` · ${line.absentDays} absent`}
                    {line.halfDays > 0 && ` · ${line.halfDays} half`}
                  </p>
                </div>
                <p className="shrink-0 font-bold text-credit-600">{absMoney(line.amount)}</p>
              </li>
            ))}
          </ul>
        </>
      )}

      <SectionTitle>History</SectionTitle>
      {runs.isLoading ? (
        <Spinner />
      ) : (runs.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={<History className="h-9 w-9" />}
          title="No wage runs yet"
          hint="Once you post a month it will be listed here."
        />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {runs.data?.map((run) => (
            <li key={run.id} className="flex items-center gap-3 px-4 py-3">
              <div className="min-w-0 flex-1">
                <p className="font-semibold text-slate-900">
                  {monthLabel(run.periodStart.slice(0, 7))}
                  {run.status === 'VOIDED' && (
                    <span className="chip ml-2 bg-slate-100 text-slate-500">voided</span>
                  )}
                </p>
                <p className="text-xs text-slate-500">
                  {run.employeeCount} workers
                  {run.postedAt && ` · posted ${fullDate(run.postedAt.slice(0, 10))}`}
                  {run.postedByName && ` by ${run.postedByName}`}
                </p>
              </div>

              <p
                className={`shrink-0 font-bold ${
                  run.status === 'VOIDED' ? 'text-slate-400 line-through' : 'text-slate-900'
                }`}
              >
                {absMoney(run.totalAmount)}
              </p>

              {isOwner && run.status === 'POSTED' && (
                <button
                  type="button"
                  onClick={() => onVoid(run.id)}
                  className="shrink-0 text-slate-300 transition active:text-red-500"
                  aria-label="Void wage run"
                >
                  <Undo2 className="h-4 w-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
