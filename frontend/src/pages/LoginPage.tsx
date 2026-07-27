import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Sprout } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { errorMessage } from '@/lib/api'
import { ErrorNote, Field, Spinner } from '@/components/ui'

export default function LoginPage() {
  const { user, loading, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (loading) return <Spinner />
  if (user) {
    const from = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={from} replace />
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not sign in'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="app-shell justify-center bg-white px-6">
      <div className="mb-10 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-fab">
          <Sprout className="h-9 w-9" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight">Hisab Kitab</h1>
        <p className="mt-1 text-sm text-slate-500">Advances and wages, all in one place</p>
      </div>

      <form onSubmit={onSubmit} className="space-y-4">
        {error && <ErrorNote message={error} />}

        <Field label="Username" required>
          <input
            className="input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            required
          />
        </Field>

        <Field label="Password" required>
          <input
            className="input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </Field>

        <button type="submit" className="btn-primary w-full" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p className="mt-8 text-center text-xs text-slate-400">
        Only owners and managers sign in. Employees do not have accounts.
      </p>
    </div>
  )
}
