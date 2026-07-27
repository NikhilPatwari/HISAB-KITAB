import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronRight, HandCoins, UserPlus, Users } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useEmployees, useEmployers } from '@/lib/queries'
import { absMoney, money, relativeDate } from '@/lib/format'
import { Avatar, BalanceText, EmptyState, SectionTitle, Spinner } from '@/components/ui'

/** Directory of everyone in the ledger: workers, and the partners who fund advances. */
export default function PeoplePage() {
  const navigate = useNavigate()
  const { isOwner } = useAuth()
  const [showInactive, setShowInactive] = useState(false)

  const employees = useEmployees(showInactive ? {} : { status: 'ACTIVE' })
  const employers = useEmployers()

  return (
    <div className="bg-slate-50 pb-6">
      <header className="safe-top flex items-center justify-between bg-brand-600 px-4 pb-4 pt-4 text-white">
        <div>
          <h1 className="text-lg font-bold">People</h1>
          <p className="text-xs text-brand-100">
            {employees.data?.length ?? 0} workers · {employers.data?.length ?? 0} employers
          </p>
        </div>
        {isOwner && (
          <button
            type="button"
            onClick={() => navigate('/employees/new')}
            className="flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-2 text-xs font-semibold transition active:bg-white/25"
          >
            <UserPlus className="h-4 w-4" /> Add
          </button>
        )}
      </header>

      <SectionTitle
        action={
          <button
            type="button"
            onClick={() => setShowInactive((v) => !v)}
            className="text-xs font-semibold text-brand-700"
          >
            {showInactive ? 'Hide past workers' : 'Show past workers'}
          </button>
        }
      >
        Workers
      </SectionTitle>

      {employees.isLoading ? (
        <Spinner />
      ) : (employees.data?.length ?? 0) === 0 ? (
        <EmptyState
          icon={<Users className="h-10 w-10" />}
          title="No workers yet"
          hint="Add your first worker to start a statement for them."
          action={
            isOwner ? (
              <button className="btn-primary mt-1" onClick={() => navigate('/employees/new')}>
                <UserPlus className="h-4 w-4" /> Add a worker
              </button>
            ) : undefined
          }
        />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {employees.data?.map((employee) => (
            <li key={employee.id}>
              <Link to={`/employees/${employee.id}`} className="list-row">
                <Avatar name={employee.name} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-slate-900">
                    {employee.name}
                    {employee.status === 'INACTIVE' && (
                      <span className="chip ml-2 bg-slate-100 text-slate-500">left</span>
                    )}
                  </p>
                  <p className="truncate text-xs text-slate-500">
                    {money(employee.dailyWageRate)}/day · since {relativeDate(employee.joinedOn)}
                  </p>
                </div>
                <BalanceText balance={employee.balance} size="sm" />
                <ChevronRight className="h-4 w-4 shrink-0 text-slate-300" />
              </Link>
            </li>
          ))}
        </ul>
      )}

      <SectionTitle
        action={
          isOwner ? (
            <Link to="/employers" className="text-xs font-semibold text-brand-700">
              Manage
            </Link>
          ) : undefined
        }
      >
        Employers
      </SectionTitle>

      {employers.isLoading ? (
        <Spinner />
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {employers.data?.map((employer) => (
            <li key={employer.id} className="flex items-center gap-3 px-4 py-3">
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
                <p className="text-[11px] uppercase tracking-wide text-slate-400">out with workers</p>
                <p className="font-bold text-credit-600">{absMoney(employer.netOutstanding)}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
