import { format, isThisYear, isToday, isYesterday, parseISO } from 'date-fns'

const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
})

const inrPrecise = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Whole rupees for list rows; paise only where the exact figure matters. */
export function money(value: number | string | null | undefined, precise = false): string {
  const n = typeof value === 'string' ? Number(value) : (value ?? 0)
  if (!Number.isFinite(n)) return '—'
  return precise ? inrPrecise.format(n) : inr.format(Math.round(n))
}

/** Magnitude only — the label carries the direction. */
export function absMoney(value: number | null | undefined, precise = false): string {
  return money(Math.abs(value ?? 0), precise)
}

export function shortDate(iso: string): string {
  return format(parseISO(iso), 'd MMM')
}

export function fullDate(iso: string): string {
  return format(parseISO(iso), 'd MMM yyyy')
}

/** DD/MM/YYYY, the way the dates are written on paper here. */
export function slashDate(iso: string): string {
  return format(parseISO(iso), 'dd/MM/yyyy')
}

export function relativeDate(iso: string): string {
  const date = parseISO(iso)
  if (isToday(date)) return 'Today'
  if (isYesterday(date)) return 'Yesterday'
  return format(date, isThisYear(date) ? 'd MMM' : 'd MMM yyyy')
}

export function monthLabel(yearMonth: string): string {
  return format(parseISO(`${yearMonth}-01`), 'MMMM yyyy')
}

export function todayIso(): string {
  return format(new Date(), 'yyyy-MM-dd')
}

export function currentYearMonth(): string {
  return format(new Date(), 'yyyy-MM')
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).slice(0, 2)
  return parts.map((p) => p[0]?.toUpperCase() ?? '').join('') || '?'
}

/**
 * Turns a signed balance into the phrasing the owner reads.
 * Negative means the employee owes the farm, which is money coming back.
 */
export function balanceLabel(balance: number): {
  text: string
  amount: string
  tone: 'credit' | 'debit' | 'settled'
} {
  if (Math.abs(balance) < 0.005) {
    return { text: 'Settled up', amount: '', tone: 'settled' }
  }
  if (balance < 0) {
    return { text: 'owes you', amount: absMoney(balance), tone: 'debit' }
  }
  return { text: 'you owe', amount: absMoney(balance), tone: 'credit' }
}

/** Days worked, shown as 25 or 25.5 rather than 25.0. */
export function days(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
}
