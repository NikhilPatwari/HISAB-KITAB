import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Archive, ClipboardList, Plus } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useArchiveTask, useSaveTask, useTasks } from '@/lib/queries'
import { errorMessage } from '@/lib/api'
import { money } from '@/lib/format'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { EmptyState, ErrorNote, Field, Spinner } from '@/components/ui'
import type { TaskView } from '@/lib/types'

interface Draft {
  name: string
  location: string
  unitOfWork: string
  pricePerUnit: string
  notes: string
}

const EMPTY: Draft = { name: '', location: '', unitOfWork: '', pricePerUnit: '', notes: '' }

/** Piece-rate job definitions, reused by every worker who logs against them. */
export default function TasksPage() {
  const navigate = useNavigate()
  const { isOwner } = useAuth()
  const tasks = useTasks(false)
  const archive = useArchiveTask()

  const [editingId, setEditingId] = useState<number | null>(null)
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState<Draft>(EMPTY)
  const [error, setError] = useState<string | null>(null)

  const save = useSaveTask(editingId ?? undefined)

  function set<K extends keyof Draft>(key: K, value: Draft[K]) {
    setDraft((prev) => ({ ...prev, [key]: value }))
  }

  function startNew() {
    setEditingId(null)
    setDraft(EMPTY)
    setError(null)
    setOpen(true)
  }

  function startEdit(task: TaskView) {
    setEditingId(task.id)
    setDraft({
      name: task.name,
      location: task.location ?? '',
      unitOfWork: task.unitOfWork,
      pricePerUnit: String(task.pricePerUnit),
      notes: task.notes ?? '',
    })
    setError(null)
    setOpen(true)
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    const price = Number(draft.pricePerUnit)
    if (!Number.isFinite(price) || price < 0) {
      setError('Enter a valid price per unit')
      return
    }

    try {
      await save.mutateAsync({
        name: draft.name.trim(),
        location: draft.location.trim() || null,
        unitOfWork: draft.unitOfWork.trim(),
        pricePerUnit: price,
        notes: draft.notes.trim() || null,
      })
      setOpen(false)
    } catch (err) {
      setError(errorMessage(err, 'Could not save the task'))
    }
  }

  async function onArchive(task: TaskView) {
    if (
      !window.confirm(
        `Archive "${task.name}"? It stops appearing when logging work, but the ` +
          `${task.recordCount} record(s) already against it stay intact.`,
      )
    ) {
      return
    }
    setError(null)
    try {
      await archive.mutateAsync(task.id)
    } catch (err) {
      setError(errorMessage(err, 'Could not archive that task'))
    }
  }

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title="Tasks"
        subtitle="Piece-rate jobs and their prices"
        left={<BackButton onClick={() => navigate(-1)} />}
        right={
          isOwner ? (
            <button
              type="button"
              onClick={startNew}
              className="flex h-9 w-9 items-center justify-center rounded-full transition active:bg-black/10"
              aria-label="Add task"
            >
              <Plus className="h-5 w-5" />
            </button>
          ) : undefined
        }
      />

      {open && (
        <form onSubmit={onSubmit} className="space-y-3 border-b border-slate-200 bg-white p-4">
          {error && <ErrorNote message={error} />}

          <Field label="Task name" required>
            <input
              className="input"
              value={draft.name}
              onChange={(e) => set('name', e.target.value)}
              placeholder="Cotton picking"
              required
              autoFocus
            />
          </Field>

          <Field label="Location">
            <input
              className="input"
              value={draft.location}
              onChange={(e) => set('location', e.target.value)}
              placeholder="Field 3"
            />
          </Field>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Unit of work" required>
              <input
                className="input"
                value={draft.unitOfWork}
                onChange={(e) => set('unitOfWork', e.target.value)}
                placeholder="kg"
                maxLength={32}
                required
              />
            </Field>

            <Field label="Price per unit" required>
              <div className="relative">
                <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 font-semibold text-slate-400">
                  ₹
                </span>
                <input
                  className="input pl-7"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min="0"
                  value={draft.pricePerUnit}
                  onChange={(e) => set('pricePerUnit', e.target.value)}
                  placeholder="12"
                  required
                />
              </div>
            </Field>
          </div>

          <Field label="Notes">
            <input
              className="input"
              value={draft.notes}
              onChange={(e) => set('notes', e.target.value)}
            />
          </Field>

          {editingId && (
            <p className="text-xs text-slate-500">
              Changing the price affects work logged from now on. Records already entered keep
              the price they were agreed at.
            </p>
          )}

          <div className="flex gap-2">
            <button type="button" className="btn-ghost flex-1" onClick={() => setOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn-primary flex-1" disabled={save.isPending}>
              {save.isPending ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      )}

      {!open && error && (
        <div className="p-4">
          <ErrorNote message={error} />
        </div>
      )}

      {tasks.isLoading ? (
        <Spinner />
      ) : (tasks.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={<ClipboardList className="h-10 w-10" />}
          title="No tasks yet"
          hint="Add a job like 'Cotton picking, Field 3, per kg' with its price, then log the units each worker completes."
          action={
            isOwner ? (
              <button className="btn-primary mt-1" onClick={startNew}>
                <Plus className="h-4 w-4" /> Add a task
              </button>
            ) : undefined
          }
        />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {tasks.data?.map((task) => (
            <li key={task.id} className="flex items-center gap-3 px-4 py-3">
              <button
                type="button"
                className="min-w-0 flex-1 text-left"
                onClick={() => isOwner && startEdit(task)}
              >
                <p className="truncate font-semibold text-slate-900">
                  {task.name}
                  {!task.active && (
                    <span className="chip ml-2 bg-slate-100 text-slate-500">archived</span>
                  )}
                </p>
                <p className="truncate text-xs text-slate-500">
                  {task.location ? `${task.location} · ` : ''}
                  {money(task.pricePerUnit)} per {task.unitOfWork}
                  {task.recordCount > 0 && ` · ${task.recordCount} logged`}
                </p>
              </button>

              {isOwner && task.active && (
                <button
                  type="button"
                  onClick={() => onArchive(task)}
                  className="shrink-0 rounded-lg p-2 text-slate-300 transition active:bg-slate-100 active:text-slate-600"
                  aria-label={`Archive ${task.name}`}
                >
                  <Archive className="h-4 w-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
