import { useMemo, useState } from 'react'
import { addDays, format, parseISO, subDays } from 'date-fns'
import { ChevronLeft, ChevronRight, Search } from 'lucide-react'
import { useMarkAttendance, useRoster } from '@/lib/queries'
import { errorMessage } from '@/lib/api'
import { todayIso } from '@/lib/format'
import { Avatar, ErrorNote, Spinner } from '@/components/ui'
import type { AttendanceStatus } from '@/lib/types'

const OPTIONS: { value: AttendanceStatus | null; short: string; classes: string }[] = [
  { value: null, short: 'P', classes: 'bg-credit-500 text-white' },
  { value: 'ABSENT', short: 'A', classes: 'bg-red-500 text-white' },
  { value: 'HALF_DAY', short: '½', classes: 'bg-amber-500 text-white' },
  { value: 'OVERTIME', short: 'OT', classes: 'bg-sky-500 text-white' },
]

/**
 * Presence is the default, so this screen is about marking the exceptions.
 * Everyone starts on P and the owner only taps the few who were not there.
 */
export default function AttendancePage() {
  const [date, setDate] = useState(todayIso())
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState<number | null>(null)

  const roster = useRoster(date)
  const mark = useMarkAttendance()

  const visible = useMemo(() => {
    const rows = roster.data?.employees ?? []
    const needle = search.trim().toLowerCase()
    return needle
      ? rows.filter((e) =>
          [e.employeeName, e.code].filter(Boolean).some((f) => f!.toLowerCase().includes(needle)),
        )
      : rows
  }, [roster.data, search])

  const counts = useMemo(() => {
    const rows = roster.data?.employees ?? []
    return {
      present: rows.filter((e) => e.status === null || e.status === 'OVERTIME').length,
      absent: rows.filter((e) => e.status === 'ABSENT').length,
      half: rows.filter((e) => e.status === 'HALF_DAY').length,
    }
  }, [roster.data])

  async function setStatus(employeeId: number, status: AttendanceStatus | null) {
    setError(null)
    setPending(employeeId)
    try {
      await mark.mutateAsync({ employeeId, workDate: date, status })
    } catch (err) {
      setError(errorMessage(err, 'Could not save attendance'))
    } finally {
      setPending(null)
    }
  }

  const isFuture = date >= todayIso()

  return (
    <div className="bg-slate-50">
      <header className="safe-top bg-brand-600 px-4 pb-4 pt-4 text-white">
        <h1 className="text-lg font-bold">Attendance</h1>
        <p className="text-xs text-brand-100">Everyone is present unless you mark them</p>

        <div className="mt-3 flex items-center gap-2">
          <button
            type="button"
            onClick={() => setDate(format(subDays(parseISO(date), 1), 'yyyy-MM-dd'))}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 transition active:bg-white/25"
            aria-label="Previous day"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>

          <input
            type="date"
            value={date}
            max={todayIso()}
            onChange={(e) => setDate(e.target.value)}
            className="flex-1 rounded-xl bg-white/15 px-3 py-2.5 text-center text-sm font-semibold
                       text-white outline-none [color-scheme:dark]"
          />

          <button
            type="button"
            onClick={() => setDate(format(addDays(parseISO(date), 1), 'yyyy-MM-dd'))}
            disabled={isFuture}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 transition
                       active:bg-white/25 disabled:opacity-30"
            aria-label="Next day"
          >
            <ChevronRight className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-3 flex gap-2 text-xs font-semibold">
          <span className="rounded-full bg-white/15 px-2.5 py-1">{counts.present} present</span>
          <span className="rounded-full bg-white/15 px-2.5 py-1">{counts.absent} absent</span>
          <span className="rounded-full bg-white/15 px-2.5 py-1">{counts.half} half day</span>
        </div>
      </header>

      {roster.data && !roster.data.workingDay && (
        <p className="bg-amber-50 px-4 py-2.5 text-center text-xs font-medium text-amber-800">
          This is a weekly off. Nothing is earned on this day.
        </p>
      )}

      <div className="sticky top-0 z-10 bg-slate-50/95 px-4 py-2 backdrop-blur">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            className="input pl-9"
            placeholder="Find a worker"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            type="search"
          />
        </div>
      </div>

      {error && (
        <div className="px-4 pb-2">
          <ErrorNote message={error} />
        </div>
      )}

      {roster.isLoading ? (
        <Spinner />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {visible.map((entry) => (
            <li key={entry.employeeId} className="flex items-center gap-3 px-4 py-2.5">
              <Avatar name={entry.employeeName} size="sm" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-slate-900">
                  {entry.employeeName}
                </p>
                {entry.code && <p className="text-[11px] text-slate-400">{entry.code}</p>}
              </div>

              <div
                className={`flex overflow-hidden rounded-lg ring-1 ring-inset ring-slate-200 ${
                  pending === entry.employeeId ? 'opacity-50' : ''
                }`}
              >
                {OPTIONS.map((option) => {
                  const active = entry.status === option.value
                  return (
                    <button
                      key={option.short}
                      type="button"
                      disabled={pending === entry.employeeId}
                      onClick={() => setStatus(entry.employeeId, option.value)}
                      className={`h-9 w-9 text-xs font-bold transition ${
                        active ? option.classes : 'bg-white text-slate-400'
                      }`}
                      aria-label={option.value ?? 'Present'}
                      aria-pressed={active}
                    >
                      {option.short}
                    </button>
                  )
                })}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
