import {
  ArrowDownToLine,
  Bell,
  ChartNoAxesCombined,
  ChevronDown,
  CircleHelp,
  Coins,
  X,
  LayoutDashboard,
  LogOut,
  Menu,
  PieChart,
  Settings2,
  Share2,
  SunMoon,
  WalletCards,
} from 'lucide-react'
import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/useAuth'
import { usePortfolio } from './usePortfolio'
import { Button, SideSheet } from '@/shared/components/ui'

const navigation = [
  { to: '/', label: 'Visão geral', icon: LayoutDashboard, end: true },
  { to: '/carteira', label: 'Carteira', icon: WalletCards },
  { to: '/alocacao', label: 'Metas', icon: PieChart },
  { to: '/aporte', label: 'Aportes', icon: Coins },
  { to: '/ativos', label: 'Ativos', icon: ChartNoAxesCombined },
  { to: '/movimentacoes', label: 'Movimentações', icon: ArrowDownToLine },
]

const secondaryNavigation = [
  { to: '/compartilhar', label: 'Compartilhar carteira', icon: Share2 },
  { to: '/configuracoes', label: 'Configurações', icon: Settings2 },
]

const titles: Record<string, string> = {
  '/': 'Visão geral',
  '/carteira': 'Carteira',
  '/aporte': 'Aportes',
  '/ativos': 'Ativos',
  '/alocacao': 'Metas',
  '/movimentacoes': 'Movimentações',
  '/compartilhar': 'Compartilhamento',
  '/configuracoes': 'Configurações',
}

export function AppShell() {
  const { user, signOut } = useAuth()
  const {
    portfolios,
    selectedPortfolio,
    setSelectedPortfolioId,
    loading,
    error,
    refresh,
  } = usePortfolio()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const location = useLocation()
  const title = titles[location.pathname] ?? 'Investize'

  const toggleTheme = () => {
    const next =
      document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'
    document.documentElement.dataset.theme = next
    localStorage.setItem('investize.theme', next)
  }

  const navItems = (className = '') => (
    <>
      {navigation.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) =>
            `nav-link ${isActive ? 'nav-link-active' : ''} ${className}`.trim()
          }
          onClick={() => setMobileMenuOpen(false)}
        >
          <Icon size={19} strokeWidth={1.8} aria-hidden="true" />
          <span>{label}</span>
          {to === '/aporte' && <span className="nav-new">Novo</span>}
        </NavLink>
      ))}
      <div className="nav-divider" />
      {secondaryNavigation.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            `nav-link ${isActive ? 'nav-link-active' : ''} ${className}`.trim()
          }
          onClick={() => setMobileMenuOpen(false)}
        >
          <Icon size={19} strokeWidth={1.8} aria-hidden="true" />
          <span>{label}</span>
        </NavLink>
      ))}
    </>
  )

  return (
    <div className="app-shell">
      <aside className="desktop-sidebar" aria-label="Navegação principal">
        <NavLink to="/" className="brand-lockup">
          <span className="brand-mark">i</span>
          <span>investize</span>
        </NavLink>
        <div className="sidebar-section-label">CARTEIRA</div>
        <label className="portfolio-select-wrap">
          <WalletCards size={18} aria-hidden="true" />
          <span className="screen-reader-only">Carteira selecionada</span>
          <select
            value={selectedPortfolio?.id ?? ''}
            onChange={(event) =>
              setSelectedPortfolioId(Number(event.target.value))
            }
            disabled={loading || portfolios.length === 0}
            aria-label="Selecionar carteira"
          >
            {portfolios.length === 0 && (
              <option value="">Nenhuma carteira</option>
            )}
            {portfolios.map((portfolio) => (
              <option key={portfolio.id} value={portfolio.id}>
                {portfolio.name}
              </option>
            ))}
          </select>
          <ChevronDown size={15} aria-hidden="true" />
        </label>
        {error && (
          <button className="sidebar-retry" type="button" onClick={refresh}>
            Não foi possível carregar carteiras · tentar novamente
          </button>
        )}
        <nav className="sidebar-nav">{navItems()}</nav>
        <div className="sidebar-bottom">
          <div className="help-card">
            <span className="help-icon">
              <CircleHelp size={17} />
            </span>
            <div>
              <strong>Invista com clareza</strong>
              <p>As sugestões são simulações, não ordens.</p>
            </div>
          </div>
          <div className="sidebar-user">
            <span className="avatar" aria-hidden="true">
              {user?.name?.slice(0, 1).toUpperCase() ?? 'I'}
            </span>
            <div className="sidebar-user-copy">
              <strong>{user?.name ?? 'Investidor'}</strong>
              <span>{user?.email}</span>
            </div>
            <button
              className="icon-button"
              type="button"
              onClick={signOut}
              aria-label="Sair"
            >
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>

      <SideSheet
        open={mobileMenuOpen}
        onOpenChange={setMobileMenuOpen}
        title="Navegação"
      >
        <aside className="mobile-drawer" aria-label="Menu">
          <div className="drawer-header">
            <NavLink
              to="/"
              className="brand-lockup"
              onClick={() => setMobileMenuOpen(false)}
            >
              <span className="brand-mark">i</span>
              <span>investize</span>
            </NavLink>
            <Button
              variant="ghost"
              size="icon"
              aria-label="Fechar menu"
              onClick={() => setMobileMenuOpen(false)}
            >
              <X size={19} />
            </Button>
          </div>
          <nav className="sidebar-nav">{navItems()}</nav>
          <button className="drawer-signout" onClick={signOut}>
            <LogOut size={18} /> Sair da conta
          </button>
        </aside>
      </SideSheet>

      <div className="main-column">
        <header className="topbar">
          <div className="topbar-title-wrap">
            <Button
              variant="ghost"
              size="icon"
              className="mobile-menu-button"
              aria-label="Abrir menu"
              onClick={() => setMobileMenuOpen(true)}
            >
              <Menu size={20} />
            </Button>
            <div>
              <p className="topbar-context">
                {selectedPortfolio?.name ?? 'Sua carteira'}
              </p>
              <h1 className="topbar-title">{title}</h1>
            </div>
          </div>
          <div className="topbar-actions">
            <span className="market-status">
              <span className="status-dot" />
              Cotações sob consulta
            </span>
            <Button
              variant="ghost"
              size="icon"
              aria-label="Alternar tema claro ou escuro"
              onClick={toggleTheme}
            >
              <SunMoon size={19} />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              aria-label="Notificações indisponíveis"
            >
              <Bell size={18} />
            </Button>
            <span className="topbar-avatar" aria-label={user?.name}>
              {user?.name?.slice(0, 1).toUpperCase() ?? 'I'}
            </span>
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
        <footer className="app-footer">
          <span>Investize · informação para decisões mais conscientes</span>
          <span>Não constitui recomendação de investimento.</span>
        </footer>
      </div>

      <nav className="mobile-bottom-nav" aria-label="Navegação mobile">
        {navigation.slice(0, 4).map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `mobile-nav-item ${isActive ? 'active' : ''}`
            }
          >
            <Icon size={20} aria-hidden="true" />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
