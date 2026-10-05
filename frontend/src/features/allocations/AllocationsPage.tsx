import Big from 'big.js'
import { AlertTriangle, Plus, Save, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { usePortfolio } from '@/app/usePortfolio'
import {
  useAllocations,
  usePortfolioSummary,
  useRebalance,
  useSaveAllocations,
  useRegisterTicker,
} from '@/shared/api/queries'
import type { CategoryAllocation, RebalanceItem } from '@/shared/api/models'
import { AssetSettingsOverview } from '@/features/assets/AssetSettingsOverview'
import { CategoryComparisonTable } from './CategoryComparisonTable'
import { formatCurrency, formatPercent } from '@/shared/format/numbers'
import {
  Badge,
  Button,
  Card,
  CardHeader,
  EmptyState,
  InlineError,
  Input,
  PageHeading,
  Skeleton,
} from '@/shared/components/ui'

const categories: Array<{ id: CategoryAllocation['category']; label: string }> =
  [
    { id: 'EQUITIES', label: 'Ações' },
    { id: 'REAL_ESTATE_FUNDS', label: 'Fundos imobiliários' },
    { id: 'BDRS', label: 'BDRs' },
  ]

function initialAllocations(): CategoryAllocation[] {
  return categories.map(({ id }) => ({
    category: id,
    categoryTargetPercentage: '0.00',
    assetAllocations: [],
  }))
}

export function AllocationsPage() {
  const { selectedPortfolioId } = usePortfolio()
  return <AllocationEditor key={selectedPortfolioId ?? 'none'} />
}

function AllocationEditor() {
  const { selectedPortfolio, selectedPortfolioId } = usePortfolio()
  const allocationsQuery = useAllocations(selectedPortfolioId)
  const rebalanceQuery = useRebalance(selectedPortfolioId)
  const summary = usePortfolioSummary(selectedPortfolioId)
  const save = useSaveAllocations(selectedPortfolioId)
  const registerTicker = useRegisterTicker()
  const [dirty, setDirty] = useState(false)
  const [allocations, setAllocations] =
    useState<CategoryAllocation[]>(initialAllocations)
  const [categoryValues, setCategoryValues] = useState<Record<string, string>>(
    {},
  )
  const [newTickers, setNewTickers] = useState<Record<string, string>>({})
  const [validationError, setValidationError] = useState<string | null>(null)

  useEffect(() => {
    if (allocationsQuery.data && !dirty) {
      const byCategory = new Map(
        allocationsQuery.data.map((allocation) => [
          allocation.category,
          allocation,
        ]),
      )
      const next = categories.map(
        ({ id }) =>
          byCategory.get(id) ?? {
            category: id,
            categoryTargetPercentage: '0.00',
            assetAllocations: [],
          },
      )
      setAllocations(next)
      setCategoryValues(
        Object.fromEntries(
          next.map((allocation) => [
            allocation.category,
            String(allocation.categoryTargetPercentage),
          ]),
        ),
      )
    }
  }, [allocationsQuery.data, dirty])

  const updateCategoryTarget = (category: string, value: string) => {
    setDirty(true)
    setCategoryValues((current) => ({ ...current, [category]: value }))
    setAllocations((current) =>
      current.map((allocation) =>
        allocation.category === category
          ? { ...allocation, categoryTargetPercentage: value }
          : allocation,
      ),
    )
  }

  const updateAssetTarget = (
    category: string,
    ticker: string,
    value: string,
  ) => {
    setDirty(true)
    setAllocations((current) =>
      current.map((allocation) =>
        allocation.category !== category
          ? allocation
          : {
              ...allocation,
              assetAllocations: allocation.assetAllocations?.map((asset) =>
                asset.tickerSymbol === ticker
                  ? { ...asset, targetPercentage: value }
                  : asset,
              ),
            },
      ),
    )
  }

  const addTicker = async (category: CategoryAllocation['category']) => {
    const ticker = (newTickers[category] ?? '').trim().toUpperCase()
    if (!/^[A-Z0-9]{2,12}$/.test(ticker)) {
      setValidationError('Informe um ticker com 2 a 12 letras ou números.')
      return
    }
    const current = allocations.find(
      (allocation) => allocation.category === category,
    )
    if (
      current?.assetAllocations?.some((asset) => asset.tickerSymbol === ticker)
    ) {
      setValidationError(`${ticker} já está incluído nesta categoria.`)
      return
    }
    try {
      await registerTicker.mutateAsync({ symbol: ticker, category })
    } catch (error) {
      setValidationError(
        error instanceof Error
          ? error.message
          : 'Não foi possível cadastrar o ticker.',
      )
      return
    }
    setDirty(true)
    setAllocations((previous) =>
      previous.map((allocation) =>
        allocation.category !== category
          ? allocation
          : {
              ...allocation,
              assetAllocations: [
                ...(allocation.assetAllocations ?? []),
                { tickerSymbol: ticker, targetPercentage: '0.00' },
              ],
            },
      ),
    )
    setNewTickers((previous) => ({ ...previous, [category]: '' }))
    setValidationError(null)
  }

  const removeTicker = (category: string, ticker: string) => {
    setDirty(true)
    setAllocations((previous) =>
      previous.map((allocation) =>
        allocation.category !== category
          ? allocation
          : {
              ...allocation,
              assetAllocations: allocation.assetAllocations?.filter(
                (asset) => asset.tickerSymbol !== ticker,
              ),
            },
      ),
    )
  }

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const message = validateAllocations(allocations)
    if (message) {
      setValidationError(message)
      return
    }
    setValidationError(null)
    try {
      await save.mutateAsync(
        allocations.map((allocation) => ({
          ...allocation,
          categoryTargetPercentage: String(
            allocation.categoryTargetPercentage,
          ).replace(',', '.'),
          assetAllocations: allocation.assetAllocations?.map((asset) => ({
            ...asset,
            targetPercentage: String(asset.targetPercentage).replace(',', '.'),
          })),
        })),
      )
      setDirty(false)
      toast.success('Metas de alocação salvas.')
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Não foi possível salvar as metas.',
      )
    }
  }

  return (
    <>
      <PageHeading
        eyebrow="OBJETIVOS DA CARTEIRA"
        title="Defina seu balanceamento."
        description="As metas mostram onde sua carteira está em relação aos objetivos — separadas dos pesos da sugestão."
        action={
          <Button form="allocation-form" type="submit" busy={save.isPending}>
            <Save size={16} /> Salvar metas
          </Button>
        }
      />

      <AssetSettingsOverview portfolioId={selectedPortfolioId} />
      <p className="form-note">
        As categorias devem somar 100%. Em cada categoria com meta maior que
        zero, os ativos também devem somar 100%. Adicionar um ticker o cadastra
        no catálogo; a meta só é salva ao clicar em Salvar metas.
      </p>
      {summary.data && (
        <CategoryComparisonTable
          assets={summary.data.assetSummaries ?? []}
          portfolioValue={summary.data.currentValue}
          targets={categoryValues}
          categories={categories}
        />
      )}
      <div className="allocation-layout">
        <div className="allocation-main">
          {allocationsQuery.isLoading && (
            <Skeleton className="allocation-skeleton" />
          )}
          {allocationsQuery.isError && (
            <InlineError
              message={allocationsQuery.error.message}
              onRetry={() => void allocationsQuery.refetch()}
            />
          )}
          {!allocationsQuery.isLoading && !allocationsQuery.isError && (
            <form
              id="allocation-form"
              className="allocation-form"
              onSubmit={submit}
            >
              {allocations.map((allocation) => {
                const category = categories.find(
                  (item) => item.id === allocation.category,
                )
                const assetTargetTotal = sumAssetTargets(allocation)
                return (
                  <Card
                    className="allocation-category-card"
                    key={allocation.category}
                  >
                    <CardHeader
                      title={category?.label ?? allocation.category}
                      description="Percentual da carteira destinado a esta categoria."
                    />
                    <div className="allocation-category-target">
                      <Input
                        label={`Meta de ${category?.label ?? allocation.category} (%)`}
                        disabled={save.isPending}
                        inputMode="decimal"
                        value={categoryValues[allocation.category] ?? ''}
                        onChange={(event) =>
                          updateCategoryTarget(
                            allocation.category,
                            event.target.value,
                          )
                        }
                        placeholder="0,00"
                      />
                      <span className="allocation-unit">%</span>
                    </div>
                    <div className="asset-target-heading">
                      <div>
                        <strong>Ativos dentro da categoria</strong>
                        <span>Meta percentual dentro da categoria.</span>
                      </div>
                      <Badge>
                        {allocation.assetAllocations?.length ?? 0} ativos
                      </Badge>
                    </div>
                    {allocation.assetAllocations?.map((asset) => (
                      <div
                        className="asset-target-row"
                        key={asset.tickerSymbol}
                      >
                        <span className="asset-ticker-label">
                          {asset.tickerSymbol}
                        </span>
                        <Input
                          label={`Meta de ${asset.tickerSymbol} (%)`}
                          disabled={save.isPending}
                          className="compact-input"
                          inputMode="decimal"
                          value={String(asset.targetPercentage)}
                          onChange={(event) =>
                            updateAssetTarget(
                              allocation.category,
                              asset.tickerSymbol,
                              event.target.value,
                            )
                          }
                        />
                        <span className="allocation-unit">%</span>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          aria-label={`Remover ${asset.tickerSymbol}`}
                          disabled={save.isPending || registerTicker.isPending}
                          onClick={() =>
                            removeTicker(
                              allocation.category,
                              asset.tickerSymbol,
                            )
                          }
                        >
                          <Trash2 size={16} />
                        </Button>
                      </div>
                    ))}
                    <div className="add-asset-row">
                      <label
                        className="screen-reader-only"
                        htmlFor={`ticker-${allocation.category}`}
                      >
                        Ticker para adicionar em {category?.label}
                      </label>
                      <input
                        id={`ticker-${allocation.category}`}
                        disabled={save.isPending || registerTicker.isPending}
                        className="input"
                        value={newTickers[allocation.category] ?? ''}
                        onChange={(event) =>
                          setNewTickers((previous) => ({
                            ...previous,
                            [allocation.category]:
                              event.target.value.toUpperCase(),
                          }))
                        }
                        placeholder="Ex.: ITUB4"
                        maxLength={12}
                      />
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => addTicker(allocation.category)}
                        disabled={registerTicker.isPending || save.isPending}
                      >
                        <Plus size={15} /> Adicionar ativo
                      </Button>
                    </div>
                    <p className="allocation-subtotal">
                      Soma das metas dos ativos:{' '}
                      <strong>
                        {assetTargetTotal === null
                          ? 'Não foi possível calcular'
                          : formatPercent(assetTargetTotal, 2)}
                      </strong>
                    </p>
                  </Card>
                )
              })}
              {validationError && (
                <div className="service-error" role="alert">
                  <AlertTriangle size={18} />
                  <p>{validationError}</p>
                </div>
              )}
              <Button
                type="submit"
                busy={save.isPending}
                className="mobile-save-button"
              >
                <Save size={16} /> Salvar metas
              </Button>
            </form>
          )}
        </div>

        <aside className="allocation-aside">
          <Card>
            <CardHeader
              title="Como ler o desvio"
              description={selectedPortfolio?.name ?? 'Carteira selecionada'}
            />
            {!summary.data ? (
              <p className="form-note">
                O resumo da carteira ainda não está disponível.
              </p>
            ) : (
              <div className="rebalance-list">
                {(rebalanceQuery.data ?? []).map((item: RebalanceItem) => {
                  const asset = summary.data.assetSummaries.find(
                    (entry) => entry.tickerSymbol === item.tickerSymbol,
                  )
                  const currentPercentage = new Big(
                    String(item.currentPercentage),
                  )
                  const progressWidth = currentPercentage.lt(0)
                    ? '0'
                    : currentPercentage.gt(100)
                      ? '100'
                      : currentPercentage.toString()
                  return (
                    <div className="rebalance-row" key={item.tickerSymbol}>
                      <div className="rebalance-row-heading">
                        <strong>{item.tickerSymbol}</strong>
                        <Badge
                          tone={item.action === 'BUY' ? 'positive' : 'warning'}
                        >
                          {item.action === 'BUY' ? 'Comprar' : 'Reduzir'}
                        </Badge>
                      </div>
                      <div className="rebalance-progress">
                        <span style={{ width: `${progressWidth}%` }} />
                      </div>
                      <div className="rebalance-meta">
                        <span>
                          Atual {formatPercent(item.currentPercentage)}
                        </span>
                        <span>Meta {formatPercent(item.targetPercentage)}</span>
                      </div>
                      <small>
                        {item.action === 'BUY'
                          ? 'Sugestão de compra'
                          : 'Excesso em relação à meta'}
                        : {item.quantity} cotas
                        {asset
                          ? ` · posição ${formatCurrency(asset.currentValue)}`
                          : ''}
                      </small>
                    </div>
                  )
                })}
                {rebalanceQuery.isLoading && (
                  <Skeleton className="rebalance-skeleton" />
                )}
                {rebalanceQuery.isError && (
                  <InlineError
                    message={rebalanceQuery.error.message}
                    onRetry={() => void rebalanceQuery.refetch()}
                  />
                )}
                {!rebalanceQuery.isLoading &&
                  !rebalanceQuery.isError &&
                  rebalanceQuery.data?.length === 0 && (
                    <EmptyState
                      icon={<AlertTriangle size={20} />}
                      title="Sem desvios para mostrar"
                      description="Defina metas e registre posições para comparar sua carteira."
                    />
                  )}
              </div>
            )}
          </Card>
          <p className="freshness-note">
            Balanceamento descreve suas metas. O score de recomendação combina
            sinais separados, configurados pelo servidor.
          </p>
        </aside>
      </div>
    </>
  )
}

function sumAssetTargets(allocation: CategoryAllocation): string | null {
  try {
    return (allocation.assetAllocations ?? [])
      .reduce(
        (total, asset) =>
          total.plus(String(asset.targetPercentage).replace(',', '.')),
        new Big(0),
      )
      .toString()
  } catch {
    return null
  }
}

function validateAllocations(allocations: CategoryAllocation[]): string | null {
  let categoryTotal = new Big(0)
  try {
    for (const allocation of allocations) {
      const target = new Big(
        String(allocation.categoryTargetPercentage).replace(',', '.'),
      )
      if (target.lt(0) || target.gt(100))
        return 'Cada meta de categoria deve ficar entre 0% e 100%.'
      categoryTotal = categoryTotal.plus(target)
      const assets = allocation.assetAllocations ?? []
      for (const asset of assets) {
        const value = String(asset.targetPercentage).replace(',', '.')
        if (!/^\d{1,3}(\.\d{1,2})?$/.test(value) || new Big(value).gt(100)) {
          return `A meta de ${asset.tickerSymbol} deve ficar entre 0% e 100%, com até duas casas decimais.`
        }
      }
      if (
        !/^\d{1,3}(\.\d{1,2})?$/.test(
          String(allocation.categoryTargetPercentage).replace(',', '.'),
        )
      ) {
        return 'Informe metas de categoria com até duas casas decimais.'
      }
      if (target.gt(0)) {
        if (assets.length === 0) {
          return `Adicione pelo menos um ativo à categoria ${allocation.category}.`
        }
        const assetTotal = assets.reduce(
          (sum, asset) =>
            sum.plus(String(asset.targetPercentage).replace(',', '.')),
          new Big(0),
        )
        if (!assetTotal.eq(100)) {
          return `As metas dos ativos em ${allocation.category} devem somar 100%.`
        }
      }
    }
    if (!categoryTotal.eq(100))
      return 'As metas das categorias devem somar 100%.'
  } catch {
    return 'Confira os percentuais: informe apenas números válidos.'
  }
  return null
}
