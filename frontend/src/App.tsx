import type { ReactNode } from 'react'
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import AppLayout from './components/AppLayout'
import { Spinner } from './components/ui'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import EmployeePage from './pages/EmployeePage'
import AddEntryPage from './pages/AddEntryPage'
import AttendancePage from './pages/AttendancePage'
import WagesPage from './pages/WagesPage'
import PeoplePage from './pages/PeoplePage'
import EmployeeFormPage from './pages/EmployeeFormPage'
import EmployersPage from './pages/EmployersPage'
import SettingsPage from './pages/SettingsPage'

function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <Spinner label="Loading" />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <>{children}</>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<HomePage />} />
        <Route path="/attendance" element={<AttendancePage />} />
        <Route path="/wages" element={<WagesPage />} />
        <Route path="/people" element={<PeoplePage />} />
      </Route>

      {/* Full-screen flows without the tab bar. */}
      <Route
        element={
          <RequireAuth>
            <div className="app-shell">
              <main className="flex-1">
                <Outlet />
              </main>
            </div>
          </RequireAuth>
        }
      >
        <Route path="/employees/new" element={<EmployeeFormPage />} />
        <Route path="/employees/:id/edit" element={<EmployeeFormPage />} />
        <Route path="/employees/:id" element={<EmployeePage />} />
        <Route path="/add" element={<AddEntryPage />} />
        <Route path="/employers" element={<EmployersPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
