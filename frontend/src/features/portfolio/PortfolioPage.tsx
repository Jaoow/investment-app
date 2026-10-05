import Big from 'big.js'
import { ArrowRight, BriefcaseBusiness } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { usePortfolio } from '@/app/usePortfolio'
import { useAllocations, usePortfolioSummary } from '@/shared/api/queries'
import type { AssetSummary } from '@/shared/api/models'
import { AllocationChart } from '@/shared/components/AllocationChart'
import {
  Badge,
  Card,
  EmptyState,
  InlineError,
  PageHeading,
  Skeleton,
} from '@/shared/components/ui'
import { formatCurrency, formatPercent, isNegative } from '@/shared/format/numbers'
import { AssetTable } from '@/features/dashboard/AssetTable'
import { buildPortfolioComposition } from '@/features/dashboard/portfolio-composition'

const portfolioClasses = [
  { id: 'EQUITIES', label: 'Ações', supported: true },
  { id: 'REAL_ESTATE_FUNDS', label: 'FIIs', supported: true },
  { id: 'ETFS', label: 'ETFs', supported: false },
  { id: 'BDRS', label: 'BDRs', supported: true },
  { id: 'FIXED_INCOME', label: 'Renda fixa', supported: false },
  { id: 'TREASURY', label: 'Tesouro Direto', supported: false },
]

function sumValue(assets: AssetSummary[], field: 'currentValue' | 'totalInvested') {
  return assets.reduce(
    (total, asset) => total.plus(String(asset[field])),
    new Big(0),
  )
}

export function PortfolioPage() {
  const { selectedPortfolio } = usePortfolio()
  const summary = usePortfolioSummary(selectedPortfolio?.id)
  const allocations = useAllocations(selectedPortfolio?.id)
  const [selectedClass, setSelectedClass] = useState<string | null>(null)
  const assets = summary.data?.assetSummaries ?? []
  const portfolioValue = new Big(String(summary.data?.currentValue ?? 0))
  const selectedAssets = assets.filter(
    (asset) => asset.tickerFields?.category === selectedClass,
  )
  const selectedValue = sumValue(selectedAssets, 'currentValue')
  const selectedInvested = sumValue(selectedAssets, 'totalInvested')
  const selectedProfit = selectedValue.minus(selectedInvested)
  const composition = buildPortfolioComposition(selectedAssets)
  const selectedAllocation = allocations.data?.find(
    (allocation) => allocation.category === selectedClass,
  )
  const assetTargets = Object.fromEntries(
    (selectedAllocation?.assetAllocations ?? []).map((asset) => [
      asset.tickerSymbol,
      asset.targetPercentage,
    ]),
  )
  const profitOrLoss = summary.data?.profitOrLoss ?? '0'

  return (
    <>
      <PageHeading
        eyebrow="POSIÇÕES DA CARTEIRA"
        title="Sua carteira por classe."
        description="Compare a participação de cada classe e abra uma posição para ver seus ativos."
      />

      {summary.isLoading && (
        <div className="portfolio-class-grid" aria-label="Carregando classes">
          {portfolioClasses.map(({ id }) => (
            <Skeleton className="portfolio-class-skeleton" key={id} />
          ))}
        </div>
      )}
      {summary.isError && (
        <InlineError
          message={summary.error.message}
          onRetry={() => void summary.refetch()}
        />
      )}
      {summary.data && (
        <>
          <section className="portfolio-overview" aria-label="Resumo do patrimônio">
            <div className="portfolio-overview-chart">
              <AllocationChart
                title="Patrimônio total"
                description="Valor atual da carteira"
                slices={[{ label: 'Patrimônio', percentage: new Big(100) }]}
                total={String(summary.data.currentValue)}
              />
            </div>
            <div className="portfolio-overview-result">
              <span>Resultado acumulado</span>
              <strong className={isNegative(profitOrLoss) ? 'text-loss' : 'text-gain'}>
                {formatCurrency(profitOrLoss)}
              </strong>
              <span className={isNegative(summary.data.percentageChange) ? 'text-loss' : 'text-gain'}>
                {formatPercent(summary.data.percentageChange)} sobre o total investido
              </span>
            </div>
          </section>
          <section className="portfolio-class-grid" aria-label="Classes de investimento">
            {portfolioClasses.map((item) => {
              const classAssets = assets.filter(
                (asset) => asset.tickerFields?.category === item.id,
              )
              const currentValue = sumValue(classAssets, 'currentValue')
              const invested = sumValue(classAssets, 'totalInvested')
              const profit = currentValue.minus(invested)
              const participation = portfolioValue.gt(0)
                ? currentValue.div(portfolioValue).times(100)
                : new Big(0)
              const returnPercentage = invested.gt(0)
                ? profit.div(invested).times(100)
                : new Big(0)
              const target = allocations.data?.find(
                (allocation) => allocation.category === item.id,
              )?.categoryTargetPercentage

              return (
                <button
                  type="button"
                  key={item.id}
                  className={`portfolio-class-card ${selectedClass === item.id ? 'selected' : ''} ${item.supported ? '' : 'unavailable'}`}
                  disabled={!item.supported}
                  aria-pressed={selectedClass === item.id}
                  onClick={() => setSelectedClass(item.id)}
                >
                  <span className="portfolio-class-heading">
                    <strong>{item.label}</strong>
                    {item.supported ? (
                      <ArrowRight size={15} aria-hidden="true" />
                    ) : (
                      <Badge>Indisponível</Badge>
                    )}
                  </span>
                  {item.supported ? (
                    <>
                      <strong className="portfolio-class-value">
                        {formatCurrency(currentValue.toString())}
                      </strong>
                      <span className="portfolio-class-meta">
                        {formatPercent(participation.toString())} / {target === undefined ? 'Meta não definida' : formatPercent(target)}
                      </span>
                      <span
                        className={`portfolio-class-return ${isNegative(profit.toString()) ? 'text-loss' : 'text-gain'}`}
                      >
                        {formatPercent(returnPercentage.toString())} · {classAssets.length} ativos
                      </span>
                    </>
                  ) : (
                    <span className="portfolio-class-unavailable-copy">
                      A API ainda não fornece posições desta classe.
                    </span>
                  )}
                </button>
              )
            })}
          </section>

          {selectedClass ? (
            <section className="portfolio-class-detail">
              <p className="portfolio-breadcrumb">
                Carteira <span aria-hidden="true">/</span>{' '}
                {portfolioClasses.find((item) => item.id === selectedClass)?.label}
              </p>
              <div className="summary-grid portfolio-detail-summary">
                <Card className="summary-card">
                  <span className="summary-card-heading">Valor atual</span>
                  <strong className="summary-value">{formatCurrency(selectedValue.toString())}</strong>
                </Card>
                <Card className="summary-card">
                  <span className="summary-card-heading">Valor investido</span>
                  <strong className="summary-value">{formatCurrency(selectedInvested.toString())}</strong>
                </Card>
                <Card className="summary-card">
                  <span className="summary-card-heading">Lucro total</span>
                  <strong className={`summary-value ${isNegative(selectedProfit.toString()) ? 'text-loss' : 'text-gain'}`}>
                    {formatCurrency(selectedProfit.toString())}
                  </strong>
                </Card>
                <Card className="summary-card">
                  <span className="summary-card-heading">Rentabilidade</span>
                  <strong className={`summary-value ${isNegative(selectedProfit.toString()) ? 'text-loss' : 'text-gain'}`}>
                    {formatPercent(selectedInvested.gt(0) ? selectedProfit.div(selectedInvested).times(100).toString() : '0')}
                  </strong>
                </Card>
              </div>
              {composition.total.gt(0) && (
                <div className="portfolio-class-chart">
                  <AllocationChart
                    title="Composição da classe"
                    description="Participação de cada ativo no valor da classe"
                    slices={composition.assets}
                    total={composition.total.toString()}
                  />
                </div>
              )}
              <AssetTable
                assets={selectedAssets}
                portfolioValue={selectedValue.toString()}
                targetPercentages={assetTargets}
              />
            </section>
          ) : (
            <Card className="portfolio-class-empty">
              <EmptyState
                icon={<BriefcaseBusiness size={23} />}
                title="Selecione uma classe disponível"
                description="Abra Ações, FIIs ou BDRs para consultar suas posições e a composição interna."
                action={
                  <Link className="text-link" to="/ativos">
                    Configurar ativos <ArrowRight size={15} />
                  </Link>
                }
              />
            </Card>
          )}
        </>
      )}
    </>
  )
}