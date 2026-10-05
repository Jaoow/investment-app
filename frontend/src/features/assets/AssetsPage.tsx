import { Search, TrendingDown, TrendingUp } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { usePortfolio } from '@/app/usePortfolio'
import { ApiRequestError } from '@/shared/api/client'
import {
  useCeiling,
  useSaveCeiling,
  useTickerQuote,
  useTickerSearch,
  useRegisterTicker,
} from '@/shared/api/queries'
import type { CategoryAllocation } from '@/shared/api/models'
import { AssetSettingsOverview } from './AssetSettingsOverview'
import { CeilingHistory } from './CeilingHistory'
import {
  formatCurrency,
  formatDateTime,
  formatPercent,
  isNegative,
  parseLocalizedDecimal,
} from '@/shared/format/numbers'
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

export function AssetsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [search, setSearch] = useState(searchParams.get('ticker') ?? '')
  const [selectedTicker, setSelectedTicker] = useState(
    searchParams.get('ticker') ?? '',
  )
  const [ceilingInput, setCeilingInput] = useState('')
  const [ceilingError, setCeilingError] = useState<string>()
  const { selectedPortfolioId } = usePortfolio()
  const tickers = useTickerSearch(search.trim())
  const quote = useTickerQuote(selectedTicker)
  const ceiling = useCeiling(selectedPortfolioId, selectedTicker)
  const saveCeiling = useSaveCeiling(selectedPortfolioId, selectedTicker)
  const registerTicker = useRegisterTicker()
  const [category, setCategory] =
    useState<CategoryAllocation['category']>('EQUITIES')

  const submitTicker = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const symbol = search.trim().toUpperCase()
    if (!/^[A-Z0-9]{2,12}$/.test(symbol)) {
      toast.error('Informe um ticker com 2 a 12 letras ou números.')
      return
    }
    try {
      await registerTicker.mutateAsync({ symbol, category })
      selectTicker(symbol)
      toast.success(
        `${symbol} cadastrado no catálogo. Agora configure o preço teto.`,
      )
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Não foi possível cadastrar o ticker.',
      )
    }
  }

  useEffect(() => {
    if (ceiling.data?.priceCeiling !== undefined) {
      setCeilingInput(String(ceiling.data.priceCeiling).replace('.', ','))
    } else if (
      ceiling.error instanceof ApiRequestError &&
      ceiling.error.status === 404
    ) {
      setCeilingInput('')
    }
  }, [ceiling.data, ceiling.error])

  const selectTicker = (symbol: string) => {
    setCeilingInput('')
    setCeilingError(undefined)
    setSelectedTicker(symbol)
    setSearch(symbol)
    setSearchParams({ ticker: symbol })
  }

  const submitCeiling = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const normalized = parseLocalizedDecimal(ceilingInput)
    if (!normalized) {
      setCeilingError('Informe um preço positivo, como 32,50.')
      return
    }
    setCeilingError(undefined)
    try {
      await saveCeiling.mutateAsync(normalized)
      toast.success(`Preço teto de ${selectedTicker} atualizado.`)
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Não foi possível salvar o preço teto.',
      )
    }
  }

  return (
    <>
      <PageHeading
        eyebrow="ATIVOS DA SUA CARTEIRA"
        title="Conheça cada posição."
        description="Consulte cotações e defina o preço teto que orienta suas simulações."
      />
      <div className="asset-explorer">
        <AssetSettingsOverview
          portfolioId={selectedPortfolioId}
          onSelect={selectTicker}
        />
        <Card className="asset-search-card">
          <label className="search-field">
            <Search size={18} aria-hidden="true" />
            <span className="screen-reader-only">Buscar ativo pelo ticker</span>
            <input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value.toUpperCase())
                setSelectedTicker('')
                setSearchParams({})
              }}
              placeholder="Busque por ticker, ex.: ITUB4"
              autoComplete="off"
            />
            {search.trim().length > 0 && (
              <span className="search-count">
                {tickers.isLoading
                  ? 'Buscando…'
                  : `${tickers.data?.length ?? 0} encontrados`}
              </span>
            )}
          </label>
          {tickers.isError && (
            <InlineError
              message={tickers.error.message}
              onRetry={() => void tickers.refetch()}
            />
          )}
          {tickers.data && tickers.data.length > 0 && !selectedTicker && (
            <div
              className="ticker-results"
              role="listbox"
              aria-label="Ativos encontrados"
            >
              {tickers.data.map((ticker) => {
                const symbol = ticker.symbol ?? ticker.tickerSymbol ?? ''
                return (
                  <button
                    type="button"
                    role="option"
                    aria-selected={false}
                    key={symbol}
                    className="ticker-result"
                    onClick={() => selectTicker(symbol)}
                  >
                    <span className="asset-avatar">{symbol.slice(0, 1)}</span>
                    <span>
                      <strong>{symbol}</strong>
                      <small>
                        {ticker.sector ?? ticker.category ?? 'Ativo'}
                      </small>
                    </span>
                    <span className="ticker-result-arrow">Ver ativo →</span>
                  </button>
                )
              })}
            </div>
          )}
          <details className="ticker-registration">
            <summary>Cadastrar ticker no catálogo</summary>
            <form className="form-stack" onSubmit={submitTicker}>
              <p className="form-note">
                Informe o ticker no campo de busca e selecione a categoria. O
                cadastro manual não valida a disponibilidade na bolsa nem altera
                a categoria de um ativo existente.
              </p>
              <label>
                Categoria do ticker
                <select
                  className="input"
                  value={category}
                  onChange={(event) =>
                    setCategory(
                      event.target.value as CategoryAllocation['category'],
                    )
                  }
                >
                  <option value="EQUITIES">Ações</option>
                  <option value="REAL_ESTATE_FUNDS">Fundos imobiliários</option>
                  <option value="BDRS">BDRs</option>
                </select>
              </label>
              <Button type="submit" busy={registerTicker.isPending}>
                Cadastrar ticker
              </Button>
            </form>
          </details>
        </Card>

        {!selectedTicker && (
          <Card className="asset-empty-card">
            <EmptyState
              icon={<Search size={24} />}
              title="Encontre um ativo"
              description="Digite ao menos duas letras ou números para buscar no catálogo."
            />
          </Card>
        )}

        {selectedTicker && (
          <div className="asset-detail-grid">
            <div className="asset-detail-main">
              {quote.isLoading && <Skeleton className="quote-skeleton" />}
              {quote.isError && (
                <InlineError
                  message={quote.error.message}
                  onRetry={() => void quote.refetch()}
                />
              )}
              {quote.data && (
                <Card className="quote-card">
                  <div className="quote-card-heading">
                    <div className="asset-title">
                      <span className="asset-avatar large">
                        {selectedTicker.slice(0, 1)}
                      </span>
                      <div>
                        <p className="eyebrow">ATIVO</p>
                        <h2>
                          {quote.data.longName ??
                            quote.data.shortName ??
                            selectedTicker}
                        </h2>
                        <span>{selectedTicker}</span>
                      </div>
                    </div>
                    <Badge tone="info">
                      {quote.data.currency ?? 'Moeda não informada'}
                    </Badge>
                  </div>
                  <strong className="quote-last-price">
                    {quote.data.regularMarketPrice !== undefined
                      ? formatCurrency(
                          quote.data.regularMarketPrice,
                          quote.data.currency ?? 'BRL',
                        )
                      : 'Cotação indisponível'}
                  </strong>
                  {quote.data.regularMarketChangePercent !== undefined && (
                    <p
                      className={`quote-change ${isNegative(quote.data.regularMarketChangePercent) ? 'text-loss' : 'text-gain'}`}
                    >
                      {isNegative(quote.data.regularMarketChangePercent) ? (
                        <TrendingDown size={17} />
                      ) : (
                        <TrendingUp size={17} />
                      )}
                      {formatPercent(quote.data.regularMarketChangePercent)} no
                      dia
                      {quote.data.regularMarketChange !== undefined &&
                        ` · ${formatCurrency(quote.data.regularMarketChange, quote.data.currency ?? 'BRL')}`}
                    </p>
                  )}
                  <div className="quote-meta quote-meta-separated">
                    <span>
                      Mercado observado:{' '}
                      {formatMarketTime(quote.data.regularMarketTime)}
                    </span>
                    <span>
                      Consulta realizada: {formatDateTime(quote.data.fetchedAt)}
                    </span>
                  </div>
                  <p className="freshness-note">
                    Atualização periódica enquanto esta tela está aberta. A
                    fonte não garante cotação em tempo real.
                  </p>
                  {quote.data.historicalDataPrice &&
                    quote.data.historicalDataPrice.length > 0 && (
                      <div className="quote-history">
                        <h3>Histórico disponível</h3>
                        <div className="history-table">
                          <div className="history-header">
                            <span>Data</span>
                            <span>Fechamento</span>
                          </div>
                          {quote.data.historicalDataPrice
                            .slice(-6)
                            .reverse()
                            .map((point) => (
                              <div className="history-row" key={point.date}>
                                <span>
                                  {point.date
                                    ? formatDateTime(
                                        new Date(
                                          point.date * 1000,
                                        ).toISOString(),
                                      )
                                    : '—'}
                                </span>
                                <strong>
                                  {point.close !== undefined
                                    ? formatCurrency(
                                        point.close,
                                        quote.data.currency ?? 'BRL',
                                      )
                                    : '—'}
                                </strong>
                              </div>
                            ))}
                        </div>
                      </div>
                    )}
                </Card>
              )}
            </div>
            <Card className="ceiling-card">
              <CardHeader
                title="Seu preço teto"
                description={`Defina o valor máximo que você considera adequado para ${selectedTicker}.`}
              />
              {!selectedPortfolioId ? (
                <p className="form-note">
                  Selecione uma carteira para configurar o preço teto.
                </p>
              ) : (
                <form
                  className="form-stack"
                  onSubmit={submitCeiling}
                  noValidate
                >
                  <Input
                    label="Preço teto (BRL)"
                    name="priceCeiling"
                    inputMode="decimal"
                    placeholder="Ex.: 32,50"
                    value={ceilingInput}
                    disabled={ceiling.isLoading || saveCeiling.isPending}
                    onChange={(event) => setCeilingInput(event.target.value)}
                    error={ceilingError}
                    hint={
                      ceiling.isLoading
                        ? 'Carregando valor salvo…'
                        : 'Aplicado somente a esta carteira.'
                    }
                  />
                  {ceiling.isError &&
                    !(
                      ceiling.error instanceof ApiRequestError &&
                      ceiling.error.status === 404
                    ) && (
                      <p className="field-error" role="alert">
                        {ceiling.error.message}
                      </p>
                    )}
                  <Button
                    type="submit"
                    busy={saveCeiling.isPending}
                    disabled={!selectedTicker || ceiling.isLoading}
                  >
                    Salvar preço teto
                  </Button>
                </form>
              )}
              <div className="ceiling-explainer">
                <span className="ceiling-explainer-icon">
                  <TrendingUp size={16} />
                </span>
                <p>
                  O motor de sugestões não indica compras acima do teto
                  cadastrado.
                </p>
              </div>
            </Card>
          </div>
        )}
        {selectedTicker && (
          <CeilingHistory
            key={`${selectedPortfolioId}-${selectedTicker}`}
            portfolioId={selectedPortfolioId}
            ticker={selectedTicker}
          />
        )}
      </div>
    </>
  )
}

function formatMarketTime(value?: string): string {
  if (!value) return 'Não informado pela fonte'
  const parsed = Date.parse(value)
  return Number.isNaN(parsed)
    ? value
    : formatDateTime(new Date(parsed).toISOString())
}
