import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronRight, ClipboardList, Plus, Settings2 } from 'lucide-react'
import { format, subDays } from 'date-fns'
import { useAuth } from '@/auth/AuthContext'
import { useTasks, useWork } from '@/lib/queries'
import { absMoney, money, relativeDate, todayIso } from '@/lib/format'
import { EmptyState, SectionTitle, Spinner } from '@/components/ui'

const RECENT_DAYS = 30

/**
 * The hub for piece-rate work. Shows the jobs that have actually been worked
 * recently rather than every job ever created — a farm accumulates seasonal
 * tasks, and a list of hundreds buries the two that matter today.
 */
export default function WorkPage() {
  const navigate = useNavigate()
  const { isOwner } = useAuth()
  const [showAll, setShowAll] = useState(false)

  const since = format(subDays(new Date(), RECENT_DAYS), 'yyyy-MM-dd')
  const tasks = useTasks(true)
  const recentWork = useWork({ from: since, to: todayIso() })

  const { recent, rest } = useMemo(() => {
    const all = tasks.data ?? []
    const records = recentWork.data?.records ?? []

    // Most recently worked first — that ordering is the point of the screen.
    const lastWorked = new Map<number, string>()
    const earned = new Map<number, number>()
    for (const r of records) {
      const seen = lastWorked.get(r.workTaskId)
      if (!seen || r.workDate > seen) lastWorked.set(r.workTaskId, r.workDate)
      earned.set(r.workTaskId, (earned.get(r.workTaskId) ?? 0) + r.amount)
    }

    const active = all
      .filter((t) => lastWorked.has(t.id))
      .sort((a, b) => (lastWorked.get(b.id) ?? '').localeCompare(lastWorked.get(a.id) ?? ''))

    return {
      recent: active.map((t) => ({
        task: t,
        lastWorked: lastWorked.get(t.id)!,
        earned: earned.get(t.id) ?? 0,
      })),
      rest: all.filter((t) => !lastWorked.has(t.id)),
    }
  }, [tasks.data, recentWork.data])

  // With nothing worked recently there is no useful "recent" list to show,
  // so fall back to everything rather than an empty screen.
  const nothingRecent = recent.length === 0
  const visibleRest = showAll || nothingRecent ? rest : []

  return (
    <div className="bg-slate-50 pb-6">
      <header className="safe-top bg-brand-600 px-4 pb-5 pt-4 text-white">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-lg font-bold">Contract Work</h1>
            <p className="text-xs text-brand-100">Paid by the unit, not the day</p>
          </div>
          {isOwner && (
            <Link
              to="/tasks"
              className="flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-2 text-xs font-semibold transition active:bg-white/25"
            >
              <Settings2 className="h-4 w-4" /> Tasks
            </Link>
          )}
        </div>

        <div className="mt-4">
          <p className="text-xs text-brand-100">Earned in the last {RECENT_DAYS} days</p>
          <p className="text-3xl font-bold tracking-tight">
            {absMoney(recentWork.data?.totalAmount ?? 0)}
          </p>
          <p className="mt-0.5 text-xs text-brand-100">
            {recent.length} active {recent.length === 1 ? 'task' : 'tasks'} ·{' '}
            {tasks.data?.length ?? 0} in total
          </p>
        </div>
      </header>

      {/* No Log work button here: work is always logged against a task, so the
          route in is to open the task and log from there. */}
      <SectionTitle
        action={
          isOwner ? (
            <Link to="/tasks" className="text-xs font-semibold text-brand-700">
              Manage
            </Link>
          ) : undefined
        }
      >
        {nothingRecent ? 'Tasks' : `Worked in the last ${RECENT_DAYS} days`}
      </SectionTitle>

      {tasks.isLoading ? (
        <Spinner />
      ) : (tasks.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={<ClipboardList className="h-10 w-10" />}
          title="No tasks yet"
          hint="Set up a job like 'Cotton picking, Field 3, per kg' with its price, then log the units each worker completes."
          action={
            isOwner ? (
              <button className="btn-primary mt-1" onClick={() => navigate('/tasks')}>
                <Plus className="h-4 w-4" /> Add a task
              </button>
            ) : undefined
          }
        />
      ) : (
        <>
          <ul className="divide-y divide-slate-100 bg-white">
            {recent.map(({ task, lastWorked, earned }) => (
              <li key={task.id}>
                <button
                  type="button"
                  className="list-row"
                  onClick={() => navigate(`/tasks/${task.id}`)}
                >
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700">
                    <ClipboardList className="h-5 w-5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-semibold text-slate-900">{task.name}</span>
                    <span className="block truncate text-xs text-slate-500">
                      {task.location ? `${task.location} · ` : ''}
                      {money(task.pricePerUnit)} per {task.unitOfWork} ·{' '}
                      {relativeDate(lastWorked)}
                    </span>
                  </span>
                  <span className="shrink-0 text-sm font-bold text-slate-900">
                    {absMoney(earned)}
                  </span>
                  <ChevronRight className="h-4 w-4 shrink-0 text-slate-300" />
                </button>
              </li>
            ))}

            {visibleRest.map((task) => (
              <li key={task.id}>
                <button
                  type="button"
                  className="list-row"
                  onClick={() => navigate(`/tasks/${task.id}`)}
                >
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                    <ClipboardList className="h-5 w-5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-semibold text-slate-900">{task.name}</span>
                    <span className="block truncate text-xs text-slate-500">
                      {task.location ? `${task.location} · ` : ''}
                      {money(task.pricePerUnit)} per {task.unitOfWork}
                    </span>
                  </span>
                  <ChevronRight className="h-4 w-4 shrink-0 text-slate-300" />
                </button>
              </li>
            ))}
          </ul>

          {!nothingRecent && rest.length > 0 && (
            <button
              type="button"
              onClick={() => setShowAll((v) => !v)}
              className="w-full bg-white py-3 text-sm font-semibold text-brand-700 transition active:bg-slate-50"
            >
              {showAll ? 'Show fewer' : `Show all tasks (${rest.length} more)`}
            </button>
          )}
        </>
      )}
    </div>
  )
}
