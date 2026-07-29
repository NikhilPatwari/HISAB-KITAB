import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { CalendarClock, ChevronRight, ClipboardList, HandCoins, LogOut } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useOrganization } from '@/lib/queries'
import { api, errorMessage } from '@/lib/api'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { ErrorNote, Field, SectionTitle, Spinner } from '@/components/ui'
import type { OrganizationView, WeekDay } from '@/lib/types'

const WEEK: { value: WeekDay; label: string }[] = [
  { value: 'MONDAY', label: 'Mon' },
  { value: 'TUESDAY', label: 'Tue' },
  { value: 'WEDNESDAY', label: 'Wed' },
  { value: 'THURSDAY', label: 'Thu' },
  { value: 'FRIDAY', label: 'Fri' },
  { value: 'SATURDAY', label: 'Sat' },
  { value: 'SUNDAY', label: 'Sun' },
]

export default function SettingsPage() {
  const navigate = useNavigate()
  const { user, isOwner, logout } = useAuth()
  const organization = useOrganization()
  const client = useQueryClient()

  const [name, setName] = useState('')
  const [offDays, setOffDays] = useState<WeekDay[]>([])
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (!organization.data) return
    setName(organization.data.name)
    setOffDays(organization.data.weeklyOffDays)
  }, [organization.data])

  const save = useMutation({
    mutationFn: async () =>
      (
        await api.put<OrganizationView>('/organization', {
          name: name.trim(),
          currencyCode: organization.data?.currencyCode,
          timeZone: organization.data?.timeZone,
          weeklyOffDays: offDays,
        })
      ).data,
    onSuccess: () => {
      setSaved(true)
      client.invalidateQueries({ queryKey: ['organization'] })
      client.invalidateQueries({ queryKey: ['wage-preview'] })
      client.invalidateQueries({ queryKey: ['roster'] })
    },
  })

  function toggleDay(day: WeekDay) {
    setSaved(false)
    setOffDays((prev) => (prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]))
  }

  async function onSave() {
    setError(null)
    setSaved(false)
    try {
      await save.mutateAsync()
    } catch (err) {
      setError(errorMessage(err, 'Could not save settings'))
    }
  }

  if (organization.isLoading) return <Spinner />

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader title="Settings" left={<BackButton onClick={() => navigate(-1)} />} />

      <div className="card mx-4 mt-4 p-4">
        <p className="text-sm font-semibold text-slate-900">{user?.displayName}</p>
        <p className="text-xs text-slate-500">
          {user?.username} · {user?.role === 'OWNER' ? 'Owner' : 'Manager'}
        </p>
      </div>

      <SectionTitle>Farm</SectionTitle>
      <div className="card mx-4 space-y-4 p-4">
        {error && <ErrorNote message={error} />}

        <Field label="Farm name">
          <input
            className="input"
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              setSaved(false)
            }}
            disabled={!isOwner}
          />
        </Field>

        <div>
          <span className="label">Weekly off days</span>
          <p className="mb-2 text-xs text-slate-500">
            Nothing is earned on these days. Leave all unselected if work runs every day.
          </p>
          <div className="flex flex-wrap gap-1.5">
            {WEEK.map((day) => {
              const active = offDays.includes(day.value)
              return (
                <button
                  key={day.value}
                  type="button"
                  onClick={() => toggleDay(day.value)}
                  disabled={!isOwner}
                  className={`h-10 w-11 rounded-lg text-xs font-bold transition disabled:opacity-50 ${
                    active
                      ? 'bg-brand-600 text-white'
                      : 'bg-white text-slate-500 ring-1 ring-inset ring-slate-200'
                  }`}
                >
                  {day.label}
                </button>
              )
            })}
          </div>
        </div>

        {isOwner && (
          <button type="button" className="btn-primary w-full" onClick={onSave} disabled={save.isPending}>
            {save.isPending ? 'Saving…' : saved ? 'Saved' : 'Save settings'}
          </button>
        )}
      </div>

      <SectionTitle>Manage</SectionTitle>
      <ul className="divide-y divide-slate-100 bg-white">
        <li>
          <button type="button" className="list-row" onClick={() => navigate('/employers')}>
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-50 text-brand-700">
              <HandCoins className="h-5 w-5" />
            </span>
            <span className="flex-1 font-medium text-slate-900">Employers</span>
            <ChevronRight className="h-4 w-4 text-slate-300" />
          </button>
        </li>
        <li>
          <button type="button" className="list-row" onClick={() => navigate('/tasks')}>
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-50 text-brand-700">
              <ClipboardList className="h-5 w-5" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block font-medium text-slate-900">Tasks</span>
              <span className="block text-xs text-slate-500">
                Piece-rate jobs and their prices
              </span>
            </span>
            <ChevronRight className="h-4 w-4 shrink-0 text-slate-300" />
          </button>
        </li>
        <li>
          <button type="button" className="list-row" onClick={() => navigate('/wage-runs')}>
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-50 text-brand-700">
              <CalendarClock className="h-5 w-5" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block font-medium text-slate-900">Correct a past month</span>
              <span className="block text-xs text-slate-500">
                Reopen a closed month to fix its attendance
              </span>
            </span>
            <ChevronRight className="h-4 w-4 shrink-0 text-slate-300" />
          </button>
        </li>
      </ul>

      <div className="p-4">
        <button
          type="button"
          className="btn-danger w-full"
          onClick={() => {
            logout()
            navigate('/login', { replace: true })
          }}
        >
          <LogOut className="h-5 w-5" /> Sign out
        </button>
      </div>

      <p className="pb-6 text-center text-xs text-slate-400">Hisab Kitab · v0.1.0</p>
    </div>
  )
}
