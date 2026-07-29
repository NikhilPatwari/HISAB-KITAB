import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChevronDown, Trash2, Users } from 'lucide-react'
import { useDeleteWork, useTaskSummary, useWork } from '@/lib/queries'
import { absMoney, money, relativeDate } from '@/lib/format'
import { errorMessage } from '@/lib/api'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { Avatar, EmptyState, ErrorNote, SectionTitle, Spinner } from '@/components/ui'
import LogWorkForm from '@/components/LogWorkForm'
import type { WorkRecordView } from '@/lib/types'

/**
 * One task, and who has delivered how much against it. Expanding a worker shows
 * the date-wise breakdown, newest first, with every entry on a day listed
 * separately — several deliveries in a day are normal.
 */
export default function TaskDetailPage() {
  const { id } = useParams()
  const taskId = id ? Number(id) : undefined
  const navigate = useNavigate()

  const summary = useTaskSummary(taskId)
  const [expanded, setExpanded] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const detail = useWork({ taskId, employeeId: expanded ?? undefined }, expanded != null)
  const deleteWork = useDeleteWork()

  async function onDelete(record: WorkRecordView) {
    if (
      !window.confirm(
        `Remove ${record.quantity} ${record.unitOfWork} logged for ${record.employeeName} on ${relativeDate(record.workDate)}?`,
      )
    ) {
      return
    }
    setError(null)
    try {
      await deleteWork.mutateAsync(record.id)
    } catch (err) {
      setError(errorMessage(err, 'Could not remove that entry'))
    }
  }

  if (summary.isLoading) return <Spinner />
  if (!summary.data) return <EmptyState title="Task not found" />

  const { task, totalQuantity, totalAmount, workers } = summary.data

  // Newest first, so today is at the top of a worker's history.
  const byDate = new Map<string, WorkRecordView[]>()
  for (const record of detail.data?.records ?? []) {
    const list = byDate.get(record.workDate) ?? []
    list.push(record)
    byDate.set(record.workDate, list)
  }

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title={task.name}
        subtitle={task.location ?? undefined}
        left={<BackButton onClick={() => navigate(-1)} />}
      />

      <section className="bg-brand-600 px-4 pb-5 text-white">
        <p className="text-xs text-brand-100">Paid out so far</p>
        <p className="text-3xl font-bold tracking-tight">{absMoney(totalAmount)}</p>
        <p className="mt-0.5 text-xs text-brand-100">
          {totalQuantity} {task.unitOfWork} · {money(task.pricePerUnit)} per {task.unitOfWork} ·{' '}
          {workers.length} {workers.length === 1 ? 'worker' : 'workers'}
        </p>
      </section>

      {/* The form sits inline rather than behind a button: opening a task is
          almost always a prelude to recording against it. */}
      <LogWorkForm fixedTaskId={task.id} />

      {error && (
        <div className="px-4 pb-2">
          <ErrorNote message={error} />
        </div>
      )}

      <SectionTitle>Workers</SectionTitle>

      {workers.length === 0 ? (
        <EmptyState
          icon={<Users className="h-9 w-9" />}
          title="Nobody has worked on this yet"
          hint="Log the first delivery and it will show here."
        />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {workers.map((worker) => {
            const open = expanded === worker.employeeId
            return (
              <li key={worker.employeeId}>
                <button
                  type="button"
                  className="list-row"
                  onClick={() => setExpanded(open ? null : worker.employeeId)}
                  aria-expanded={open}
                >
                  <Avatar name={worker.employeeName} size="sm" />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-semibold text-slate-900">
                      {worker.employeeName}
                    </span>
                    <span className="block truncate text-xs text-slate-500">
                      {worker.quantity} {task.unitOfWork} · {worker.entries}{' '}
                      {worker.entries === 1 ? 'entry' : 'entries'} · last{' '}
                      {relativeDate(worker.lastWorkedOn)}
                    </span>
                  </span>
                  <span className="shrink-0 font-bold text-credit-600">
                    {absMoney(worker.amount)}
                  </span>
                  <ChevronDown
                    className={`h-4 w-4 shrink-0 text-slate-300 transition-transform ${
                      open ? 'rotate-180' : ''
                    }`}
                  />
                </button>

                {open && (
                  <div className="bg-slate-50 px-4 py-2">
                    {detail.isLoading ? (
                      <Spinner />
                    ) : byDate.size === 0 ? (
                      <p className="py-3 text-center text-sm text-slate-500">No entries.</p>
                    ) : (
                      [...byDate.entries()].map(([date, entries]) => (
                        <div key={date} className="py-2">
                          <div className="flex items-baseline justify-between">
                            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
                              {relativeDate(date)}
                            </p>
                            <p className="text-xs font-semibold text-slate-600">
                              {entries.reduce((s, r) => s + r.quantity, 0)} {task.unitOfWork}
                            </p>
                          </div>
                          <ul className="mt-1 space-y-1">
                            {entries.map((record) => (
                              <li
                                key={record.id}
                                className="flex items-center gap-2 rounded-lg bg-white px-3 py-2"
                              >
                                <span className="min-w-0 flex-1">
                                  <span className="block text-sm text-slate-900">
                                    {record.quantity} {record.unitOfWork} ×{' '}
                                    {money(record.unitPrice)}
                                  </span>
                                  {record.note && (
                                    <span className="block truncate text-xs text-slate-500">
                                      {record.note}
                                    </span>
                                  )}
                                </span>
                                <span className="shrink-0 text-sm font-semibold text-credit-600">
                                  {absMoney(record.amount)}
                                </span>
                                <button
                                  type="button"
                                  onClick={() => onDelete(record)}
                                  className="shrink-0 text-slate-300 transition active:text-red-500"
                                  aria-label="Remove entry"
                                >
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              </li>
                            ))}
                          </ul>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
