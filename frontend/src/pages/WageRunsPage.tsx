import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CalendarX2, History, Undo2 } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useVoidWageRun, useWageRuns } from '@/lib/queries'
import { absMoney, fullDate, monthLabel } from '@/lib/format'
import { errorMessage } from '@/lib/api'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { EmptyState, ErrorNote, Spinner } from '@/components/ui'

/**
 * Months close themselves, so there is no posting here. This screen exists for
 * the one thing automation cannot do: reopen a month whose attendance was wrong,
 * because a posted wage entry does not recalculate on its own.
 */
export default function WageRunsPage() {
  const navigate = useNavigate()
  const { isOwner } = useAuth()
  const runs = useWageRuns()
  const voidRun = useVoidWageRun()

  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  async function onVoid(id: number, label: string) {
    const confirmed = window.confirm(
      `Reopen ${label}?\n\nIts wage entries will be cancelled. Correct the attendance ` +
        `next — the month re-closes itself with the corrected figures.`,
    )
    if (!confirmed) return

    setError(null)
    setNotice(null)
    try {
      await voidRun.mutateAsync(id)
      setNotice(`${label} is reopened. Fix the attendance, then open Home to re-close it.`)
    } catch (err) {
      setError(errorMessage(err, 'Could not reopen that month'))
    }
  }

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title="Correct a past month"
        subtitle="Reopen a closed month to fix its attendance"
        left={<BackButton onClick={() => navigate(-1)} />}
      />

      <div className="space-y-3 p-4">
        {error && <ErrorNote message={error} />}
        {notice && (
          <div className="rounded-xl bg-credit-50 px-3.5 py-3 text-sm font-medium text-credit-700 ring-1 ring-inset ring-green-100">
            {notice}
          </div>
        )}

        <div className="card p-4 text-sm text-slate-600">
          <p className="font-semibold text-slate-900">You should rarely need this</p>
          <p className="mt-1">
            Wages update as you mark attendance, and each month closes on its own once it
            ends. Reopen a month only when you find that its attendance was wrong after the
            fact — a closed month's wages do not recalculate by themselves.
          </p>
        </div>
      </div>

      {runs.isLoading ? (
        <Spinner />
      ) : (runs.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={<History className="h-9 w-9" />}
          title="No months closed yet"
          hint="A month appears here once it has ended and been closed."
        />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {runs.data?.map((run) => {
            const label = monthLabel(run.periodStart.slice(0, 7))
            const reopened = run.status === 'VOIDED'
            return (
              <li key={run.id} className="flex items-center gap-3 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-slate-900">
                    {label}
                    {reopened && (
                      <span className="chip ml-2 bg-amber-50 text-amber-700">reopened</span>
                    )}
                  </p>
                  <p className="text-xs text-slate-500">
                    {reopened
                      ? 'Fix the attendance, then open Home to re-close it'
                      : `${run.employeeCount} workers${
                          run.postedAt ? ` · closed ${fullDate(run.postedAt.slice(0, 10))}` : ''
                        }`}
                  </p>
                </div>

                <p
                  className={`shrink-0 font-bold ${
                    reopened ? 'text-slate-400 line-through' : 'text-slate-900'
                  }`}
                >
                  {absMoney(run.totalAmount)}
                </p>

                {isOwner && !reopened && (
                  <button
                    type="button"
                    onClick={() => onVoid(run.id, label)}
                    disabled={voidRun.isPending}
                    className="shrink-0 rounded-lg p-2 text-slate-400 transition active:bg-red-50 active:text-red-600 disabled:opacity-40"
                    aria-label={`Reopen ${label}`}
                  >
                    <Undo2 className="h-4 w-4" />
                  </button>
                )}
              </li>
            )
          })}
        </ul>
      )}

      {!isOwner && (
        <p className="px-4 py-4 text-center text-xs text-slate-500">
          <CalendarX2 className="mx-auto mb-1 h-4 w-4" />
          Only an owner can reopen a closed month.
        </p>
      )}
    </div>
  )
}
