import { useMemo, useState, type FormEvent } from 'react'
import { Check } from 'lucide-react'
import { useEmployees, useLogWork, useTasks } from '@/lib/queries'
import { errorMessage } from '@/lib/api'
import { absMoney, money, todayIso } from '@/lib/format'
import { ErrorNote, Field } from '@/components/ui'
import PickerField from '@/components/PickerField'

/**
 * Recording units completed. Shared between the standalone Log work screen and
 * the task page, where the task is already known and its picker is hidden.
 */
export default function LogWorkForm({
  fixedTaskId,
  initialEmployeeId,
}: {
  fixedTaskId?: number
  initialEmployeeId?: number
}) {
  const tasks = useTasks(true)
  const employees = useEmployees({ status: 'ACTIVE' })
  const logWork = useLogWork()

  const [workTaskId, setWorkTaskId] = useState<number | null>(fixedTaskId ?? null)
  const [employeeId, setEmployeeId] = useState<number | null>(initialEmployeeId ?? null)
  const [workDate, setWorkDate] = useState(todayIso())
  const [quantity, setQuantity] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const effectiveTaskId = fixedTaskId ?? workTaskId
  const task = tasks.data?.find((t) => t.id === effectiveTaskId) ?? null
  const employee = employees.data?.find((e) => e.id === employeeId) ?? null

  const preview = useMemo(() => {
    const qty = Number(quantity)
    if (!task || !Number.isFinite(qty) || qty <= 0) return null
    return qty * task.pricePerUnit
  }, [task, quantity])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setNotice(null)

    if (!effectiveTaskId) return setError('Choose a task')
    if (!employeeId) return setError('Choose a worker')

    const qty = Number(quantity)
    if (!Number.isFinite(qty) || qty <= 0) return setError('Enter a quantity greater than zero')

    try {
      const saved = await logWork.mutateAsync({
        workTaskId: effectiveTaskId,
        employeeId,
        workDate,
        quantity: qty,
        note: note.trim() || null,
      })
      // Entries accumulate, so show the day's running total when this was not
      // the first batch — otherwise a second entry looks like it replaced one.
      const isSecondBatch =
        saved.dayTotalQuantity != null && saved.dayTotalQuantity > saved.quantity
      setNotice(
        isSecondBatch
          ? `Added ${saved.quantity} ${saved.unitOfWork} for ${saved.employeeName} — now ${saved.dayTotalQuantity} ${saved.unitOfWork} for this date.`
          : `Logged ${saved.quantity} ${saved.unitOfWork} for ${saved.employeeName}.`,
      )
      // Keep the task and date so several workers can be logged in a row.
      setQuantity('')
      setNote('')
      setEmployeeId(null)
    } catch (err) {
      setError(errorMessage(err, 'Could not log that work'))
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4 p-4">
      {error && <ErrorNote message={error} />}
      {notice && (
        <div className="rounded-xl bg-credit-50 px-3.5 py-3 text-sm font-medium text-credit-700 ring-1 ring-inset ring-green-100">
          {notice}
        </div>
      )}

      {!fixedTaskId && (
        <PickerField
          label="Task"
          placeholder="Choose a task"
          required
          value={workTaskId}
          onChange={setWorkTaskId}
          emptyHint="No task matches that."
          options={(tasks.data ?? []).map((t) => ({
            id: t.id,
            title: t.name,
            subtitle: `${t.location ? `${t.location} · ` : ''}${money(t.pricePerUnit)} per ${t.unitOfWork}`,
            keywords: t.unitOfWork,
          }))}
        />
      )}

      <PickerField
        label="Worker"
        placeholder="Choose a worker"
        required
        value={employeeId}
        onChange={setEmployeeId}
        emptyHint="No worker matches that."
        options={(employees.data ?? []).map((e) => ({
          id: e.id,
          title: e.name,
          subtitle: [e.code, e.village].filter(Boolean).join(' · ') || undefined,
          keywords: e.phone ?? undefined,
        }))}
      />

      <Field label={task ? `Quantity in ${task.unitOfWork}` : 'Quantity'} required>
        <input
          className="input text-2xl font-bold"
          type="number"
          inputMode="decimal"
          step="0.001"
          min="0.001"
          placeholder="0"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
          required
        />
      </Field>

      {preview !== null && task && (
        <div className="rounded-xl bg-credit-50 px-3.5 py-3 text-sm font-medium text-credit-700 ring-1 ring-inset ring-green-100">
          {quantity} {task.unitOfWork} × {money(task.pricePerUnit)} ={' '}
          <span className="font-bold">{absMoney(preview)}</span>
          {employee && ` for ${employee.name}`}
        </div>
      )}

      <Field label="Date" required>
        <input
          className="input"
          type="date"
          value={workDate}
          max={todayIso()}
          onChange={(e) => setWorkDate(e.target.value)}
          required
        />
      </Field>

      <Field label="Note">
        <input
          className="input"
          placeholder="Anything worth remembering"
          value={note}
          onChange={(e) => setNote(e.target.value)}
          maxLength={500}
        />
      </Field>

      <button type="submit" className="btn-primary w-full" disabled={logWork.isPending}>
        <Check className="h-5 w-5" />
        {logWork.isPending ? 'Saving…' : 'Log work'}
      </button>
    </form>
  )
}
