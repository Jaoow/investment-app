import { Link } from 'react-router-dom'
import { useAssetSettings } from '@/shared/api/queries'
import { formatCurrency, formatPercent } from '@/shared/format/numbers'
import {
  Button,
  Card,
  CardHeader,
  InlineError,
  Skeleton,
} from '@/shared/components/ui'

export function AssetSettingsOverview({
  portfolioId,
  onSelect,
}: {
  portfolioId: number | undefined
  onSelect?: (ticker: string) => void
}) {
  const settings = useAssetSettings(portfolioId)
  if (portfolioId === undefined) return null
  return (
    <Card className="asset-settings-overview">
      <CardHeader
        title="Tickers, metas e preço teto"
        description="Meta efetiva na carteira = meta da categoria × meta do ativo dentro dela. Não é a alocação atual nem o peso de recomendação."
      />
      {settings.isLoading && <Skeleton className="allocation-skeleton" />}
      {settings.isError && (
        <InlineError
          message={settings.error.message}
          onRetry={() => void settings.refetch()}
        />
      )}
      {settings.data?.length === 0 && (
        <p className="form-note">
          Cadastre um preço teto ou uma meta para visualizar os ativos aqui.
        </p>
      )}
      <div className="asset-settings-list">
        {settings.data?.map((asset) => (
          <div className="asset-setting-row" key={asset.tickerSymbol}>
            <strong>{asset.tickerSymbol}</strong>
            <div>
              <small>Meta na carteira</small>
              <strong>{formatPercent(asset.allocationPercentage, 2)}</strong>
            </div>
            <div>
              <small>Preço teto</small>
              <strong>
                {asset.priceCeiling === null
                  ? 'Não definido'
                  : formatCurrency(asset.priceCeiling)}
              </strong>
            </div>
            {onSelect ? (
              <Button
                variant="secondary"
                size="small"
                onClick={() => onSelect(asset.tickerSymbol)}
              >
                Editar {asset.tickerSymbol}
              </Button>
            ) : (
              <Link
                to={`/ativos?ticker=${encodeURIComponent(asset.tickerSymbol)}`}
              >
                Teto e histórico
              </Link>
            )}
          </div>
        ))}
      </div>
    </Card>
  )
}
