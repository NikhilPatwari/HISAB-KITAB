import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import type {
  AttendanceStatus,
  AttendanceView,
  CreateEntryRequest,
  Dashboard,
  DayRoster,
  EmployeeDetail,
  EmployeeMonth,
  EmployeeRequest,
  EmployeeSummary,
  EmployerView,
  EntryView,
  OrganizationView,
  PagedEntries,
  StatementResponse,
  WagePreview,
  WageRunView,
} from './types'

/** Anything that can shift a balance invalidates these. */
const LEDGER_KEYS = ['dashboard', 'employees', 'employee', 'statement', 'transactions', 'employers']

function useLedgerInvalidation() {
  const client = useQueryClient()
  return () => {
    LEDGER_KEYS.forEach((key) => client.invalidateQueries({ queryKey: [key] }))
  }
}

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: async () => (await api.get<Dashboard>('/dashboard')).data,
  })
}

export function useEmployees(params?: { status?: string; search?: string }) {
  return useQuery({
    queryKey: ['employees', params?.status ?? 'all', params?.search ?? ''],
    queryFn: async () =>
      (await api.get<EmployeeSummary[]>('/employees', { params })).data,
  })
}

export function useEmployee(id: number | undefined) {
  return useQuery({
    queryKey: ['employee', id],
    enabled: id != null,
    queryFn: async () => (await api.get<EmployeeDetail>(`/employees/${id}`)).data,
  })
}

/** Omitting `from`/`to` lets the backend fall back to its five-year window. */
export function useStatement(
  id: number | undefined,
  options?: { from?: string; to?: string; includeVoided?: boolean },
) {
  const { from, to, includeVoided = false } = options ?? {}
  return useQuery({
    queryKey: ['statement', id, from ?? null, to ?? null, includeVoided],
    enabled: id != null,
    queryFn: async () =>
      (await api.get<StatementResponse>(`/transactions/statement/${id}`, {
        params: { from, to, includeVoided },
      })).data,
  })
}

export function useTransactions(params: {
  employeeId?: number
  employerId?: number
  entryType?: string
  page?: number
  size?: number
}) {
  return useQuery({
    queryKey: ['transactions', params],
    queryFn: async () => (await api.get<PagedEntries>('/transactions', { params })).data,
  })
}

export function useEmployers() {
  return useQuery({
    queryKey: ['employers'],
    queryFn: async () => (await api.get<EmployerView[]>('/employers')).data,
  })
}

export function useOrganization() {
  return useQuery({
    queryKey: ['organization'],
    queryFn: async () => (await api.get<OrganizationView>('/organization')).data,
  })
}

export function useRoster(date: string) {
  return useQuery({
    queryKey: ['roster', date],
    queryFn: async () =>
      (await api.get<DayRoster>('/attendance/roster', { params: { date } })).data,
  })
}

export function useEmployeeMonth(employeeId: number | undefined, month: string) {
  return useQuery({
    queryKey: ['attendance-month', employeeId, month],
    enabled: employeeId != null,
    queryFn: async () =>
      (await api.get<EmployeeMonth>(`/attendance/employee/${employeeId}`, {
        params: { month },
      })).data,
  })
}

export function useWagePreview(month: string) {
  return useQuery({
    queryKey: ['wage-preview', month],
    queryFn: async () =>
      (await api.get<WagePreview>('/wage-runs/preview', { params: { month } })).data,
  })
}

export function useWageRuns() {
  return useQuery({
    queryKey: ['wage-runs'],
    queryFn: async () => (await api.get<WageRunView[]>('/wage-runs')).data,
  })
}

export function useCreateEntry() {
  const invalidate = useLedgerInvalidation()
  return useMutation({
    mutationFn: async (body: CreateEntryRequest) =>
      (await api.post<EntryView>('/transactions', body)).data,
    onSuccess: invalidate,
  })
}

export function useVoidEntry() {
  const invalidate = useLedgerInvalidation()
  return useMutation({
    mutationFn: async (id: number) =>
      (await api.post<EntryView>(`/transactions/${id}/void`)).data,
    onSuccess: invalidate,
  })
}

export function useSaveEmployee(id?: number) {
  const invalidate = useLedgerInvalidation()
  return useMutation({
    mutationFn: async (body: EmployeeRequest) =>
      id
        ? (await api.put<EmployeeDetail>(`/employees/${id}`, body)).data
        : (await api.post<EmployeeDetail>('/employees', body)).data,
    onSuccess: invalidate,
  })
}

export function useMarkAttendance() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: async (body: {
      employeeId: number
      workDate: string
      status: AttendanceStatus | null
      note?: string | null
    }) => (await api.post<AttendanceView>('/attendance/mark', body)).data,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['roster'] })
      client.invalidateQueries({ queryKey: ['attendance-month'] })
      client.invalidateQueries({ queryKey: ['wage-preview'] })
    },
  })
}

export function usePostWages() {
  const client = useQueryClient()
  const invalidate = useLedgerInvalidation()
  return useMutation({
    mutationFn: async (period: string) =>
      (await api.post<WageRunView>('/wage-runs', { period })).data,
    onSuccess: () => {
      invalidate()
      client.invalidateQueries({ queryKey: ['wage-runs'] })
      client.invalidateQueries({ queryKey: ['wage-preview'] })
    },
  })
}

export function useVoidWageRun() {
  const client = useQueryClient()
  const invalidate = useLedgerInvalidation()
  return useMutation({
    mutationFn: async (id: number) =>
      (await api.post<WageRunView>(`/wage-runs/${id}/void`)).data,
    onSuccess: () => {
      invalidate()
      client.invalidateQueries({ queryKey: ['wage-runs'] })
      client.invalidateQueries({ queryKey: ['wage-preview'] })
    },
  })
}

export function useSaveEmployer(id?: number) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: async (body: { name: string; phone?: string | null; notes?: string | null }) =>
      id
        ? (await api.put<EmployerView>(`/employers/${id}`, body)).data
        : (await api.post<EmployerView>('/employers', body)).data,
    onSuccess: () => client.invalidateQueries({ queryKey: ['employers'] }),
  })
}
