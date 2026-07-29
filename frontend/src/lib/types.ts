// Mirrors the backend DTOs in com.hisabkitab.web.dto.

export type Role = 'OWNER' | 'MANAGER'
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE'

/** How a worker earns. Governs attendance and the wage engine alike. */
export type EmployeeType = 'PERMANENT' | 'TEMPORARY' | 'CONTRACT'

export const EMPLOYEE_TYPES: {
  value: EmployeeType
  label: string
  description: string
  usesDailyWage: boolean
}[] = [
  {
    value: 'PERMANENT',
    label: 'Permanent',
    description: 'Present by default, paid a daily wage',
    usesDailyWage: true,
  },
  {
    value: 'TEMPORARY',
    label: 'Temporary',
    description: 'Only paid for days marked present',
    usesDailyWage: true,
  },
  {
    value: 'CONTRACT',
    label: 'Contract',
    description: 'Paid per unit of work done',
    usesDailyWage: false,
  },
]
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'HALF_DAY' | 'PAID_LEAVE' | 'OVERTIME'

export type EntryType =
  | 'ADVANCE'
  | 'EXPENSE_ON_BEHALF'
  | 'PAYOUT'
  | 'DEDUCTION'
  | 'WAGE'
  | 'PIECE_WORK'
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
  employeeType: EmployeeType
  /** Zero for contract workers, who are paid per unit instead. */
  dailyWageRate: number
  joinedOn: string
  exitedOn: string | null
  status: EmployeeStatus
  /**
   * Live and signed. Negative: the employee owes the farm. Positive: the farm
   * owes the employee. Already includes `unpostedWages`.
   */
  balance: number
  /** The same figure counting only entries written to the ledger. */
  postedBalance: number
  /** Earned since the last closed month, derived from attendance. */
  unpostedWages: number
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
  employeeType: EmployeeType
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
  /** Running total of the rows shown — ledger only. */
  closingBalance: number
  totalGivenOut: number
  totalEarned: number
  /** Earned since the last closed month, not yet a ledger row. */
  unpostedWages: number
  unpostedSince: string
  /** closingBalance + unpostedWages: what the worker stands at today. */
  liveBalance: number
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
  /** Decides whether an unmarked day means "present" or "did not work". */
  employeeType: EmployeeType
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

/** A reusable piece-rate job: "Cotton picking, Field 3, per kg, ₹12". */
export interface TaskView {
  id: number
  name: string
  location: string | null
  unitOfWork: string
  pricePerUnit: number
  notes: string | null
  active: boolean
  /** How much work has been logged, so the UI can explain archiving. */
  recordCount: number
}

export interface TaskRequest {
  name: string
  location?: string | null
  unitOfWork: string
  pricePerUnit: number
  notes?: string | null
  active?: boolean
}

export interface WorkRecordView {
  id: number
  employeeId: number
  employeeName: string
  workTaskId: number
  taskName: string
  location: string | null
  unitOfWork: string
  workDate: string
  quantity: number
  /** The task price snapshotted when this was entered. */
  unitPrice: number
  amount: number
  note: string | null
  /**
   * Everything this worker has logged on this task for this date, including
   * the entry just saved. Null when not computed.
   */
  dayTotalQuantity: number | null
}

export interface WorkSummary {
  from: string
  to: string
  totalAmount: number
  records: WorkRecordView[]
}

/** One worker's running total against a single task. */
export interface TaskWorkerTotal {
  employeeId: number
  employeeName: string
  quantity: number
  amount: number
  entries: number
  lastWorkedOn: string
}

export interface TaskSummary {
  task: TaskView
  totalQuantity: number
  totalAmount: number
  workers: TaskWorkerTotal[]
}

export interface LogWorkRequest {
  workTaskId: number
  employeeId: number
  workDate: string
  quantity: number
  unitPrice?: number | null
  note?: string | null
}

export interface OrganizationView {
  id: number
  name: string
  currencyCode: string
  timeZone: string
  weeklyOffDays: WeekDay[]
}
