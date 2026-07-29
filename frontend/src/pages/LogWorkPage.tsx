import { useNavigate, useSearchParams } from 'react-router-dom'
import { ClipboardList } from 'lucide-react'
import { useTasks } from '@/lib/queries'
import { BackButton, PageHeader } from '@/components/AppLayout'
import { EmptyState, Spinner } from '@/components/ui'
import LogWorkForm from '@/components/LogWorkForm'

/**
 * Logging without a task already in hand — reached from a worker's page or
 * directly. The task page embeds the same form with its task fixed.
 */
export default function LogWorkPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const tasks = useTasks(true)

  const taskId = params.get('taskId') ? Number(params.get('taskId')) : undefined
  const employeeId = params.get('employeeId') ? Number(params.get('employeeId')) : undefined

  if (tasks.isLoading) return <Spinner />

  if ((tasks.data?.length ?? 0) === 0) {
    return (
      <div className="bg-slate-50">
        <PageHeader title="Log work" left={<BackButton onClick={() => navigate(-1)} />} />
        <EmptyState
          icon={<ClipboardList className="h-10 w-10" />}
          title="No tasks to log against"
          hint="Create a task first — a job with a unit of work and a price per unit."
          action={
            <button className="btn-primary mt-1" onClick={() => navigate('/tasks')}>
              Set up tasks
            </button>
          }
        />
      </div>
    )
  }

  return (
    <div className="bg-slate-50 pb-10">
      <PageHeader
        title="Log work"
        subtitle="Units completed"
        left={<BackButton onClick={() => navigate(-1)} />}
      />
      <LogWorkForm fixedTaskId={taskId} initialEmployeeId={employeeId} />
    </div>
  )
}
