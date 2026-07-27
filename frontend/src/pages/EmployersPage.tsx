import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { HandCoins, Plus } from 'lucide-react'
import { useEmployers, useSaveEmployer } from '@/lib/queries'
import { errorMessage } from '@/lib/api'
import { absMoney } from '@/lib/format'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { EmptyState, ErrorNote, Field, Spinner } from '@/components/ui'

/** The partners whose own money funds the advances. */
export default function EmployersPage() {
  const navigate = useNavigate()
  const employers = useEmployers()

  const [editingId, setEditingId] = useState<number | null>(null)
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  const save = useSaveEmployer(editingId ?? undefined)

  function startNew() {
    setEditingId(null)
    setName('')
    setPhone('')
    setNotes('')
    setError(null)
    setOpen(true)
  }

  function startEdit(id: number, current: { name: string; phone: string | null; notes: string | null }) {
    setEditingId(id)
    setName(current.name)
    setPhone(current.phone ?? '')
    setNotes(current.notes ?? '')
    setError(null)
    setOpen(true)
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await save.mutateAsync({
        name: name.trim(),
        phone: phone.trim() || null,
        notes: notes.trim() || null,
      })
      setOpen(false)
    } catch (err) {
      setError(errorMessage(err, 'Could not save'))
    }
  }

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title="Employers"
        subtitle="Whose money goes out"
        left={<BackButton onClick={() => navigate(-1)} />}
        right={
          <button
            type="button"
            onClick={startNew}
            className="flex h-9 w-9 items-center justify-center rounded-full transition active:bg-black/10"
            aria-label="Add employer"
          >
            <Plus className="h-5 w-5" />
          </button>
        }
      />

      {open && (
        <form onSubmit={onSubmit} className="space-y-3 border-b border-slate-200 bg-white p-4">
          {error && <ErrorNote message={error} />}
          <Field label="Name" required>
            <input
              className="input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
            />
          </Field>
          <Field label="Phone">
            <input
              className="input"
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </Field>
          <Field label="Notes">
            <input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </Field>
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

      {employers.isLoading ? (
        <Spinner />
      ) : (employers.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={<HandCoins className="h-10 w-10" />}
          title="No employers yet"
          hint="Add each partner who lends money, so you can see whose funds are out."
        />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {employers.data?.map((employer) => (
            <li key={employer.id}>
              <button
                type="button"
                className="list-row"
                onClick={() =>
                  startEdit(employer.id, {
                    name: employer.name,
                    phone: employer.phone,
                    notes: employer.notes,
                  })
                }
              >
                <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-50 text-brand-700">
                  <HandCoins className="h-5 w-5" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-slate-900">{employer.name}</p>
                  <p className="text-xs text-slate-500">
                    {employer.active ? 'Lending' : 'Inactive'}
                    {employer.phone && ` · ${employer.phone}`}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-[11px] uppercase tracking-wide text-slate-400">out</p>
                  <p className="font-bold text-credit-600">{absMoney(employer.netOutstanding)}</p>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
