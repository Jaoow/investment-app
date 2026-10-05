import { Link } from 'react-router-dom'
import type { AssetSetting, AssetSummary } from '@/shared/api/models'
import { useAssetSettings } from '@/shared/api/queries'
import { formatCurrency } from '@/shared/format/numbers'
import { CircleHelp } from 'lucide-react'
import { Badge, Card, CardHeader, EmptyState, InlineError, Skeleton } from '@/shared/components/ui'
import { contributionEligibility } from './contribution-eligibility'

const categoryLabels: Record<string, string> = {
  EQUITIES: 'Ações',
  REAL_ESTATE_FUNDS: 'FIIs',
  BDRS: 'BDRs',
}

export function ContributionConfiguration({
  portfolioId,
  assets,
  portfolioLoading,
}: {
  portfolioId: number | undefined
  assets: AssetSummary[]
  portfolioLoading: boolean
}) {
  const settings = useAssetSettings(portfolioId)
  const assetByTicker = new Map(assets.map((asset) => [asset.tickerSymbol, asset]))
  const rows = [...(settings.data ?? [])].sort((left, right) =>
    left.tickerSymbol.localeCompare(right.tickerSymbol),
  )

  return (
    <Card className="contribution-configuration-card">
      <CardHeader
        title="Configuração dos ativos"
        description="A simulação exige preço teto por ativo e cotação dentro do limite."
      />
      <p className="form-note contribution-configuration-note">
        A elegibilidade por classe ainda não é configurável pela API. A situação
        abaixo é informativa; o motor também valida a atualização da cotação.
      </p>
      {(settings.isLoading || portfolioLoading) && (
        <Skeleton className="allocation-skeleton" />
      )}
      {settings.isError && (
        <InlineError
          message={settings.error.message}
          onRetry={() => void settings.refetch()}
        />
      )}
      {!settings.isLoading && !portfolioLoading && !settings.isError && rows.length === 0 && (
        <EmptyState
          icon={<CircleHelp size={22} />}
          title="Nenhum ativo configurado"
          description="Defina preço teto e metas para os ativos que deseja avaliar."
          action={
            <Link className="text-link" to="/ativos">
              Configurar ativos
            </Link>
          }
        />
      )}
      {rows.length > 0 && !settings.isLoading && !portfolioLoading && (
        <div className="contribution-settings-scroll">
          <table className="contribution-settings-table">
            <thead>
              <tr>
                <th>Ativo</th>
                <th>Classe</th>
                <th>Preço atual</th>
                <th>Preço teto</th>
                <th>Situação</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((setting: AssetSetting) => {
                const asset = assetByTicker.get(setting.tickerSymbol)
                const price = asset?.brapiFields?.regularMarketPrice
                const status = contributionEligibility(price, setting.priceCeiling)
                return (
                  <tr key={setting.tickerSymbol}>
                    <th scope="row">{setting.tickerSymbol}</th>
                    <td>{categoryLabels[setting.category] ?? setting.category}</td>
                    <td>{price === undefined ? '—' : formatCurrency(price)}</td>
                    <td>
                      {setting.priceCeiling === null
                        ? '—'
                        : formatCurrency(setting.priceCeiling)}
                    </td>
                    <td><Badge tone={status.tone}>{status.label}</Badge></td>
                    <td>
                      <Link to={`/ativos?ticker=${encodeURIComponent(setting.tickerSymbol)}`}>
                        Editar teto
                      </Link>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}