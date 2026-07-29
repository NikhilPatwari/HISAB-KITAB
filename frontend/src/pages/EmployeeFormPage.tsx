import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Check } from 'lucide-react'
import { useEmployee, useSaveEmployee } from '@/lib/queries'
import { errorMessage } from '@/lib/api'
import { todayIso } from '@/lib/format'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { ErrorNote, Field, Spinner } from '@/components/ui'
import { EMPLOYEE_TYPES, type EmployeeType } from '@/lib/types'

interface FormState {
  code: string
  name: string
  phone: string
  village: string
  employeeType: EmployeeType
  dailyWageRate: string
  joinedOn: string
  notes: string
}

const EMPTY: FormState = {
  code: '',
  name: '',
  phone: '',
  village: '',
  employeeType: 'PERMANENT',
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
      employeeType: person.employeeType,
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

    // Contract workers earn per unit, so no rate is asked for or sent.
    const usesDailyWage = form.employeeType !== 'CONTRACT'
    const rate = usesDailyWage ? Number(form.dailyWageRate) : 0
    if (usesDailyWage && (!Number.isFinite(rate) || rate < 0)) {
      setError('Enter a valid daily wage')
      return
    }

    try {
      const saved = await save.mutateAsync({
        code: form.code.trim() || null,
        name: form.name.trim(),
        phone: form.phone.trim() || null,
        village: form.village.trim() || null,
        employeeType: form.employeeType,
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

        <div>
          <span className="label">
            How are they paid?<span className="ml-0.5 text-red-500">*</span>
          </span>
          <div className="space-y-2">
            {EMPLOYEE_TYPES.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => set('employeeType', option.value)}
                className={`w-full rounded-xl border px-3.5 py-3 text-left transition ${
                  form.employeeType === option.value
                    ? 'border-brand-500 bg-brand-50 ring-2 ring-brand-100'
                    : 'border-slate-200 bg-white'
                }`}
              >
                <span className="block text-sm font-semibold text-slate-900">{option.label}</span>
                <span className="block text-xs leading-tight text-slate-500">
                  {option.description}
                </span>
              </button>
            ))}
          </div>
        </div>

        {form.employeeType !== 'CONTRACT' ? (
          <Field
            label="Daily wage"
            required
            hint={
              form.employeeType === 'TEMPORARY'
                ? 'Earned only on days you mark them present'
                : "Used to calculate each month's earnings"
            }
          >
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
        ) : (
          <p className="rounded-xl bg-slate-100 px-3.5 py-3 text-sm text-slate-600">
            Contract workers have no daily wage and do not appear on the attendance roster.
            They earn from the units of work they complete.
          </p>
        )}

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
            Changing the wage here applies from today.{' '}
            <button
              type="button"
              className="font-semibold text-brand-700 underline"
              onClick={() => navigate(`/employees/${employeeId}/wage`)}
            >
              Use a dated change
            </button>{' '}
            to back-date a raise or start one later.
          </p>
        )}
      </form>
    </div>
  )
}
