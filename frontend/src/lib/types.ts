// Mirrors the backend DTOs in com.hisabkitab.web.dto.

export type Role = 'OWNER' | 'MANAGER'
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE'
export type AttendanceStatus = 'ABSENT' | 'HALF_DAY' | 'PAID_LEAVE' | 'OVERTIME'

export type EntryType =
  | 'ADVANCE'
  | 'EXPENSE_ON_BEHALF'
  | 'PAYOUT'
  | 'DEDUCTION'
  | 'WAGE'
  | 'BONUS'
  | 'REPAYMENT'
  | 'ADJUSTMENT'

export interface Me {
  id: number
  username: string
  displayName: string
  role: Role
  employerId: number | null
  organizationId: number
  organizationName: string
  currencyCode: string
}

export interface LoginResponse {
  token: string
  expiresInSeconds: number
  user: Me
}

export interface EmployeeSummary {
  id: number
  code: string | null
  name: string
  phone: string | null
  village: string | null
  dailyWageRate: number
  joinedOn: string
  exitedOn: string | null
  status: EmployeeStatus
  /** Negative: the employee owes the farm. Positive: the farm owes the employee. */
  balance: number
}

export interface WageRateView {
  id: number
  dailyRate: number
  effectiveFrom: string
  note: string | null
}

export interface EmployeeDetail extends Omit<EmployeeSummary, 'balance'> {
  notes: string | null
  balance: number
  rateHistory: WageRateView[]
}

export interface EmployeeRequest {
  code?: string | null
  name: string
  phone?: string | null
  village?: string | null
  dailyWageRate: number
  joinedOn: string
  exitedOn?: string | null
  notes?: string | null
}

export interface EmployerView {
  id: number
  name: string
  phone: string | null
  notes: string | null
  active: boolean
  netOutstanding: number
}

export interface EntryView {
  id: number
  employeeId: number
  employeeName: string
  employerId: number | null
  employerName: string | null
  entryType: EntryType
  typeLabel: string
  amount: number
  signedAmount: number
  entryDate: string
  note: string | null
  voided: boolean
  wageRunId: number | null
  createdAt: string
}

export interface PagedEntries {
  content: EntryView[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface StatementRow {
  id: number
  entryDate: string
  entryType: EntryType
  typeLabel: string
  note: string | null
  employerName: string | null
  amount: number
  signedAmount: number
  runningBalance: number
  voided: boolean
  wageRunId: number | null
}

export interface StatementResponse {
  employeeId: number
  employeeName: string
  from: string
  to: string
  openingBalance: number
  closingBalance: number
  totalGivenOut: number
  totalEarned: number
  rows: StatementRow[]
}

export interface CreateEntryRequest {
  employeeId: number
  employerId?: number | null
  entryType: EntryType
  amount: number
  entryDate: string
  creditsEmployee?: boolean | null
  note?: string | null
}

export interface RosterEntry {
  employeeId: number
  employeeName: string
  code: string | null
  status: AttendanceStatus | null
  note: string | null
}

export interface DayRoster {
  workDate: string
  workingDay: boolean
  employees: RosterEntry[]
}

export interface AttendanceView {
  id: number | null
  employeeId: number
  employeeName: string
  workDate: string
  status: AttendanceStatus | null
  note: string | null
}

export interface EmployeeMonth {
  employeeId: number
  employeeName: string
  periodStart: string
  periodEnd: string
  workingDaysInPeriod: number
  payableDays: number
  absentDays: number
  halfDays: number
  paidLeaveDays: number
  overtimeDays: number
  exceptions: AttendanceView[]
}

export interface WagePreviewLine {
  employeeId: number
  employeeName: string
  code: string | null
  dailyRate: number
  eligibleDays: number
  payableDays: number
  absentDays: number
  halfDays: number
  overtimeDays: number
  amount: number
}

export interface WagePreview {
  period: string
  periodStart: string
  periodEnd: string
  workingDaysInPeriod: number
  totalAmount: number
  alreadyPosted: boolean
  postedRunId: number | null
  lines: WagePreviewLine[]
}

export interface WageRunView {
  id: number
  periodStart: string
  periodEnd: string
  status: 'POSTED' | 'VOIDED'
  totalAmount: number
  employeeCount: number
  postedAt: string | null
  postedByName: string | null
}

export interface EmployerPosition {
  employerId: number
  name: string
  outstanding: number
}

export interface TopDebtor {
  employeeId: number
  name: string
  owed: number
}

export interface Dashboard {
  activeEmployees: number
  totalReceivable: number
  totalPayable: number
  netPosition: number
  employeesInDebt: number
  employeesInCredit: number
  monthStart: string
  advancesThisMonth: number
  wagesThisMonth: number
  repaymentsThisMonth: number
  employers: EmployerPosition[]
  topDebtors: TopDebtor[]
}

export type WeekDay =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface OrganizationView {
  id: number
  name: string
  currencyCode: string
  timeZone: string
  weeklyOffDays: WeekDay[]
}
