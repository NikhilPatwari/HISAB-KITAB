import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Check } from 'lucide-react'
import { useEmployee, useSaveEmployee } from '@/lib/queries'
import { errorMessage } from '@/lib/api'
import { todayIso } from '@/lib/format'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { ErrorNote, Field, Spinner } from '@/components/ui'

interface FormState {
  code: string
  name: string
  phone: string
  village: string
  dailyWageRate: string
  joinedOn: string
  notes: string
}

const EMPTY: FormState = {
  code: '',
  name: '',
  phone: '',
  village: '',
  dailyWageRate: '',
  joinedOn: todayIso(),
  notes: '',
}

export default function EmployeeFormPage() {
  const { id } = useParams()
  const employeeId = id ? Number(id) : undefined
  const navigate = useNavigate()

  const existing = useEmployee(employeeId)
  const save = useSaveEmployee(employeeId)

  const [form, setForm] = useState<FormState>(EMPTY)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const person = existing.data
    if (!person) return
    setForm({
      code: person.code ?? '',
      name: person.name,
      phone: person.phone ?? '',
      village: person.village ?? '',
      dailyWageRate: String(person.dailyWageRate),
      joinedOn: person.joinedOn,
      notes: person.notes ?? '',
    })
  }, [existing.data])

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    const rate = Number(form.dailyWageRate)
    if (!Number.isFinite(rate) || rate < 0) {
      setError('Enter a valid daily wage')
      return
    }

    try {
      const saved = await save.mutateAsync({
        code: form.code.trim() || null,
        name: form.name.trim(),
        phone: form.phone.trim() || null,
        village: form.village.trim() || null,
        dailyWageRate: rate,
        joinedOn: form.joinedOn,
        notes: form.notes.trim() || null,
      })
      navigate(`/employees/${saved.id}`, { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not save the worker'))
    }
  }

  if (employeeId && existing.isLoading) return <Spinner />

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title={employeeId ? 'Edit worker' : 'New worker'}
        left={<BackButton onClick={() => navigate(-1)} />}
      />

      <form onSubmit={onSubmit} className="space-y-4 p-4">
        {error && <ErrorNote message={error} />}

        <Field label="Full name" required>
          <input
            className="input"
            value={form.name}
            onChange={(e) => set('name', e.target.value)}
            placeholder="Ramesh Yadav"
            required
            autoFocus={!employeeId}
          />
        </Field>

        <Field label="Daily wage" required hint="Used to calculate each month's earnings">
          <div className="relative">
            <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 font-semibold text-slate-400">
              ₹
            </span>
            <input
              className="input pl-8"
              type="number"
              inputMode="decimal"
              step="1"
              min="0"
              value={form.dailyWageRate}
              onChange={(e) => set('dailyWageRate', e.target.value)}
              placeholder="450"
              required
            />
          </div>
        </Field>

        <Field label="Joined on" required hint="Wages are only counted from this date">
          <input
            className="input"
            type="date"
            value={form.joinedOn}
            max={todayIso()}
            onChange={(e) => set('joinedOn', e.target.value)}
            required
          />
        </Field>

        <Field label="Code" hint="The number you already use on paper">
          <input
            className="input"
            value={form.code}
            onChange={(e) => set('code', e.target.value)}
            placeholder="E01"
            maxLength={32}
          />
        </Field>

        <Field label="Phone">
          <input
            className="input"
            type="tel"
            inputMode="tel"
            value={form.phone}
            onChange={(e) => set('phone', e.target.value)}
            placeholder="98xxxxxxxx"
            maxLength={32}
          />
        </Field>

        <Field label="Village">
          <input
            className="input"
            value={form.village}
            onChange={(e) => set('village', e.target.value)}
            maxLength={160}
          />
        </Field>

        <Field label="Notes">
          <textarea
            className="input min-h-24 resize-y"
            value={form.notes}
            onChange={(e) => set('notes', e.target.value)}
            maxLength={1000}
          />
        </Field>

        <button type="submit" className="btn-primary w-full" disabled={save.isPending}>
          <Check className="h-5 w-5" />
          {save.isPending ? 'Saving…' : employeeId ? 'Save changes' : 'Add worker'}
        </button>

        {employeeId && (
          <p className="text-center text-xs text-slate-500">
            Changing the wage here applies from today. Past months already posted keep the rate they
            were paid at.
          </p>
        )}
      </form>
    </div>
  )
}
