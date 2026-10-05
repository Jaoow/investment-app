import { Moon, Sun, UserRound, Weight } from 'lucide-react'
import { useAuth } from '@/features/auth/useAuth'
import {
  Badge,
  Button,
  Card,
  CardHeader,
  PageHeading,
} from '@/shared/components/ui'

export function SettingsPage() {
  const { user, signOut } = useAuth()
  const dark = document.documentElement.dataset.theme === 'dark'

  const toggleTheme = () => {
    const next = dark ? 'light' : 'dark'
    document.documentElement.dataset.theme = next
    localStorage.setItem('investize.theme', next)
  }

  return (
    <>
      <PageHeading
        eyebrow="PREFERÊNCIAS"
        title="Configurações."
        description="Gerencie sua sessão e preferências de visualização."
      />
      <div className="settings-grid">
        <Card>
          <CardHeader
            title="Sua conta"
            description="Dados retornados pela sessão autenticada."
          />
          <div className="account-setting">
            <span className="settings-icon">
              <UserRound size={20} />
            </span>
            <div>
              <strong>{user?.name ?? 'Investidor'}</strong>
              <span>{user?.email}</span>
            </div>
          </div>
          <Button variant="secondary" onClick={signOut}>
            Sair da conta
          </Button>
        </Card>
        <Card>
          <CardHeader
            title="Aparência"
            description="Escolha uma apresentação confortável para você."
          />
          <div className="theme-setting-row">
            <div className="settings-icon">
              {dark ? <Moon size={20} /> : <Sun size={20} />}
            </div>
            <div>
              <strong>Tema {dark ? 'escuro' : 'claro'}</strong>
              <span>A preferência fica neste navegador.</span>
            </div>
            <Button variant="secondary" onClick={toggleTheme}>
              Alternar
            </Button>
          </div>
        </Card>
        <Card className="settings-wide-card">
          <CardHeader
            title="Como as sugestões são priorizadas"
            description="Pesos de recomendação são diferentes das metas de balanceamento."
            action={<Badge tone="warning">Configurado pelo servidor</Badge>}
          />
          <div className="settings-explanation">
            <span className="settings-icon">
              <Weight size={20} />
            </span>
            <p>
              O motor combina distância do preço teto, variação diária e desvio
              em relação à meta da carteira. Os pesos atuais são configurações
              globais da API. Esta versão não permite alterá-los por usuário;
              não exibimos controles que não seriam salvos.
            </p>
          </div>
        </Card>
        <Card className="settings-wide-card legal-card">
          <CardHeader title="Informação financeira" />
          <p>
            As sugestões do Investize são simulações informativas, baseadas nos
            dados disponíveis no momento da consulta. Não constituem consultoria
            financeira, recomendação personalizada ou ordem de compra. Cotações
            podem estar sujeitas a atraso; custos e impostos não estão
            incluídos.
          </p>
        </Card>
      </div>
    </>
  )
}
