import { useMemo, useState } from 'react'
import { Check, ChevronDown, Search, X } from 'lucide-react'

export interface PickerOption {
  id: number
  title: string
  subtitle?: string
  /** Extra text matched by the search box but not displayed. */
  keywords?: string
}

/**
 * A select replacement for lists too long to scroll: opens full screen with a
 * search box. A farm with hundreds of workers and jobs cannot use a dropdown.
 */
export default function PickerField({
  label,
  placeholder,
  options,
  value,
  onChange,
  required,
  emptyHint,
}: {
  label: string
  placeholder: string
  options: PickerOption[]
  value: number | null
  onChange: (id: number) => void
  required?: boolean
  emptyHint?: string
}) {
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')

  const selected = options.find((o) => o.id === value) ?? null

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    if (!needle) return options
    return options.filter((o) =>
      [o.title, o.subtitle, o.keywords]
        .filter(Boolean)
        .some((field) => field!.toLowerCase().includes(needle)),
    )
  }, [options, search])

  return (
    <div>
      <span className="label">
        {label}
        {required && <span className="ml-0.5 text-red-500">*</span>}
      </span>

      <button
        type="button"
        onClick={() => {
          setSearch('')
          setOpen(true)
        }}
        className="input flex w-full items-center justify-between gap-2 text-left"
      >
        <span className="min-w-0 flex-1">
          {selected ? (
            <>
              <span className="block truncate text-slate-900">{selected.title}</span>
              {selected.subtitle && (
                <span className="block truncate text-xs text-slate-500">{selected.subtitle}</span>
              )}
            </>
          ) : (
            <span className="text-slate-400">{placeholder}</span>
          )}
        </span>
        <ChevronDown className="h-4 w-4 shrink-0 text-slate-400" />
      </button>

      {open && (
        <div className="fixed inset-0 z-50 mx-auto flex w-full max-w-md flex-col bg-white">
          <div className="safe-top flex items-center gap-2 border-b border-slate-200 px-3 py-3">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                className="input pl-9"
                placeholder={`Search ${label.toLowerCase()}`}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                type="search"
                autoFocus
              />
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-slate-500 transition active:bg-slate-100"
              aria-label="Close"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <ul className="flex-1 divide-y divide-slate-100 overflow-y-auto">
            {filtered.length === 0 ? (
              <li className="px-4 py-10 text-center text-sm text-slate-500">
                {emptyHint ?? 'Nothing matches that.'}
              </li>
            ) : (
              filtered.map((option) => (
                <li key={option.id}>
                  <button
                    type="button"
                    className="list-row"
                    onClick={() => {
                      onChange(option.id)
                      setOpen(false)
                    }}
                  >
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-medium text-slate-900">
                        {option.title}
                      </span>
                      {option.subtitle && (
                        <span className="block truncate text-xs text-slate-500">
                          {option.subtitle}
                        </span>
                      )}
                    </span>
                    {option.id === value && (
                      <Check className="h-4 w-4 shrink-0 text-brand-600" />
                    )}
                  </button>
                </li>
              ))
            )}
          </ul>

          <p className="safe-bottom border-t border-slate-100 px-4 py-2 text-center text-xs text-slate-400">
            {filtered.length} of {options.length}
          </p>
        </div>
      )}
    </div>
  )
}
