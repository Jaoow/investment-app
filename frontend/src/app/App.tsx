import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { lazy, Suspense, useEffect } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth/AuthContext'
import { ProtectedRoute } from './ProtectedRoute'
import { PortfolioProvider } from './PortfolioContext'
import { AppShell } from './AppShell'
import { Toaster } from 'sonner'

const LoginPage = lazy(() =>
  import('@/features/auth/AuthPages').then((module) => ({
    default: module.LoginPage,
  })),
)
const RegisterPage = lazy(() =>
  import('@/features/auth/AuthPages').then((module) => ({
    default: module.RegisterPage,
  })),
)
const DashboardPage = lazy(() =>
  import('@/features/dashboard/DashboardPage').then((module) => ({
    default: module.DashboardPage,
  })),
)
const PortfolioPage = lazy(() =>
  import('@/features/portfolio/PortfolioPage').then((module) => ({
    default: module.PortfolioPage,
  })),
)
const ContributionPage = lazy(() =>
  import('@/features/contributions/ContributionPage').then((module) => ({
    default: module.ContributionPage,
  })),
)
const AssetsPage = lazy(() =>
  import('@/features/assets/AssetsPage').then((module) => ({
    default: module.AssetsPage,
  })),
)
const AllocationsPage = lazy(() =>
  import('@/features/allocations/AllocationsPage').then((module) => ({
    default: module.AllocationsPage,
  })),
)
const MovementsPage = lazy(() =>
  import('@/features/movements/MovementsPage').then((module) => ({
    default: module.MovementsPage,
  })),
)
const SharingPage = lazy(() =>
  import('@/features/sharing/SharingPages').then((module) => ({
    default: module.SharingPage,
  })),
)
const PublicSharePage = lazy(() =>
  import('@/features/sharing/SharingPages').then((module) => ({
    default: module.PublicSharePage,
  })),
)
const SettingsPage = lazy(() =>
  import('@/features/settings/SettingsPage').then((module) => ({
    default: module.SettingsPage,
  })),
)

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) =>
        !(error instanceof Error && /401|403|503/.test(error.message)) &&
        failureCount < 1,
      refetchOnWindowFocus: true,
    },
  },
})

function ThemeSetup() {
  useEffect(() => {
    const stored = localStorage.getItem('investize.theme')
    const prefersDark = window.matchMedia(
      '(prefers-color-scheme: dark)',
    ).matches
    document.documentElement.dataset.theme =
      stored ?? (prefersDark ? 'dark' : 'light')
  }, [])
  return null
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <ThemeSetup />
          <Suspense
            fallback={
              <div className="route-loading" role="status">
                Carregando tela...
              </div>
            }
          >
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/share/:token" element={<PublicSharePage />} />
              <Route element={<ProtectedRoute />}>
                <Route
                  element={
                    <PortfolioProvider>
                      <AppShell />
                    </PortfolioProvider>
                  }
                >
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/carteira" element={<PortfolioPage />} />
                  <Route path="/aporte" element={<ContributionPage />} />
                  <Route path="/ativos" element={<AssetsPage />} />
                  <Route path="/alocacao" element={<AllocationsPage />} />
                  <Route path="/movimentacoes" element={<MovementsPage />} />
                  <Route path="/compartilhar" element={<SharingPage />} />
                  <Route path="/configuracoes" element={<SettingsPage />} />
                </Route>
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
          <Toaster position="top-center" richColors />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
