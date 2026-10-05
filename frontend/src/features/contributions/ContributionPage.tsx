import {
  ArrowDownRight,
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Check,
  CircleHelp,
  Info,
  Minus,
  Plus,
  RefreshCw,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { usePortfolio } from '@/app/usePortfolio'
import { ApiRequestError } from '@/shared/api/client'
import { useAssetSettings, usePortfolioSummary, useSuggestion } from '@/shared/api/queries'
import type { Suggestion, SuggestionItem } from '@/shared/api/models'
import { ScoreBadge } from './ScoreBadge'
import { rankSuggestions } from './recommendation-ranking'
import { buildPortfolioComparison } from './portfolio-comparison'
import { PortfolioComparison } from './PortfolioComparison'
import {
  formatCurrency,
  formatDateTime,
  formatDecimal,
  formatPercent,
  isNegative,
  parseLocalizedDecimal,
} from '@/shared/format/numbers'
import {
  Badge,
  Button,
  Card,
  CardHeader,
  Dialog,
  EmptyState,
} from '@/shared/components/ui'

export function ContributionPage() {
  const { selectedPortfolio, selectedPortfolioId } = usePortfolio()
  const suggestion = useSuggestion(selectedPortfolioId)
  const portfolioSummary = usePortfolioSummary(selectedPortfolioId)
  const assetSettings = useAssetSettings(selectedPortfolioId)
  const [amountInput, setAmountInput] = useState('')
  const [amountError, setAmountError] = useState<string | undefined>()
  const [serviceError, setServiceError] = useState<string | null>(null)
  const [reviewOpen, setReviewOpen] = useState(false)
  const [lastReviewed, setLastReviewed] = useState(false)
  const [expandedTicker, setExpandedTicker] = useState<string | null>(null)

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const normalized = parseLocalizedDecimal(amountInput)
    if (!normalized) {
      setAmountError('Informe um valor positivo, como 1.250,00.')
      return
    }
    setAmountError(undefined)
    setServiceError(null)
    setLastReviewed(false)
    try {
      await suggestion.mutateAsync(normalized)
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 503) {
        setServiceError(
          'As cotações necessárias estão indisponíveis ou desatualizadas. Tente novamente quando os dados de mercado forem atualizados.',
        )
      } else {
        setServiceError(
          error instanceof Error
            ? error.message
            : 'Não foi possível gerar a simulação. Tente novamente.',
        )
      }
    }
  }

  const result = suggestion.data
  const portfolioComparison =
    result && portfolioSummary.data && assetSettings.data
      ? buildPortfolioComparison(
          portfolioSummary.data.assetSummaries ?? [],
          assetSettings.data,
          result.items,
        )
      : null
  const assetNames = Object.fromEntries(
    (portfolioSummary.data?.assetSummaries ?? []).map((asset) => [
      asset.tickerSymbol,
      asset.brapiFields?.longName ?? asset.brapiFields?.shortName ?? asset.tickerSymbol,
    ]),
  )

  return (
    <>
      <div className="contribution-simulation">
        <header className="contribution-simulation-header">
          <Button asChild variant="ghost" size="icon" aria-label="Voltar para aportes">
            <Link to="/aporte"><ArrowLeft size={20} /></Link>
          </Button>
          <div className="contribution-simulation-title">
            <p className="eyebrow">{selectedPortfolio?.name ?? 'SUA CARTEIRA'}</p>
            <h1>Sugestão de aporte</h1>
          </div>
          <Button
            type="submit"
            form="contribution-simulation-form"
            busy={suggestion.isPending}
            disabled={!selectedPortfolio || suggestion.isPending || amountInput.trim().length === 0}
            variant="ghost"
          >
            <RefreshCw size={16} aria-hidden="true" />
            Atualizar
          </Button>
        </header>

        {selectedPortfolio ? (
          <form
            id="contribution-simulation-form"
            className="contribution-simulation-budget"
            onSubmit={submit}
            noValidate
          >
            <div className="contribution-simulation-budget-heading">
              <label htmlFor="contribution-amount">Valor em reais</label>
              {result && (
                <span>
                  Restante: <strong>{formatCurrency(result.remainingAmount, result.currency)}</strong>
                </span>
              )}
            </div>
            <div className="contribution-amount-input">
              <span aria-hidden="true">R$</span>
              <input
                id="contribution-amount"
                name="contributionAmount"
                inputMode="decimal"
                autoComplete="off"
                placeholder="0,00"
                value={amountInput}
                onChange={(event) => setAmountInput(event.target.value)}
                aria-invalid={amountError !== undefined}
                aria-describedby={amountError ? 'contribution-amount-error' : undefined}
              />
            </div>
            {amountError && <p className="field-error" id="contribution-amount-error">{amountError}</p>}
            {serviceError && (
              <div className="service-error" role="alert">
                <Info size={19} aria-hidden="true" />
                <div>
                  <strong>Não foi possível atualizar sua simulação</strong>
                  <p>{serviceError}</p>
                </div>
              </div>
            )}
            {!result && !suggestion.isPending && (
              <p className="contribution-simulation-hint">
                A simulação considera cotas inteiras, preço teto e as cotações disponíveis.
              </p>
            )}
          </form>
        ) : (
          <EmptyState
            icon={<CircleHelp size={22} />}
            title="Selecione uma carteira"
            description="Escolha ou crie uma carteira para simular um aporte."
          />
        )}

        {result && (
          <SuggestionResult
            suggestion={result}
            assetNames={assetNames}
            expandedTicker={expandedTicker}
            onToggleDetails={(ticker) => setExpandedTicker((current) => current === ticker ? null : ticker)}
            onReview={() => setReviewOpen(true)}
            reviewed={lastReviewed}
            comparison={portfolioComparison}
          />
        )}
      </div>

      <Dialog
        open={reviewOpen}
        onOpenChange={setReviewOpen}
        title="Revise sua simulação"
        description={`${selectedPortfolio?.name ?? 'Carteira'} · ${result ? formatCurrency(result.requestedAmount, result.currency) : ''}`}
      >
        {result && (
          <div className="review-content">
            <div className="review-summary-row">
              <span>Valor sugerido</span>
              <strong>
                {formatCurrency(result.allocatedAmount, result.currency)}
              </strong>
            </div>
            <div className="review-summary-row">
              <span>Saldo não alocado</span>
              <strong>
                {formatCurrency(result.remainingAmount, result.currency)}
              </strong>
            </div>
            {result.items.map((item) => (
              <div className="review-item" key={item.tickerSymbol}>
                <strong>{item.tickerSymbol}</strong>
                <span>
                  {item.quantity} un. ·{' '}
                  {formatCurrency(item.estimatedValue, result.currency)}
                </span>
              </div>
            ))}
            <p className="review-no-order">
              Confirmar registra apenas que você revisou esta simulação. Nenhuma
              ordem ou movimentação será criada.
            </p>
            <div className="dialog-actions">
              <Button variant="secondary" onClick={() => setReviewOpen(false)}>
                Voltar
              </Button>
              <Button
                onClick={() => {
                  setLastReviewed(true)
                  setReviewOpen(false)
                  toast.success(
                    'Revisão confirmada. Sua carteira não foi alterada.',
                  )
                }}
              >
                <Check size={17} /> Confirmar revisão
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </>
  )
}

function SuggestionResult({
  suggestion,
  assetNames,
  expandedTicker,
  onToggleDetails,
  onReview,
  reviewed,
  comparison,
}: {
  suggestion: Suggestion
  assetNames: Record<string, string>
  expandedTicker: string | null
  onToggleDetails: (ticker: string) => void
  onReview: () => void
  reviewed: boolean
  comparison: ReturnType<typeof buildPortfolioComparison> | null
}) {
  return (
    <section className="suggestion-result" aria-live="polite">
      <div className="result-heading">
        <div>
          <h2>
            Ativos em reais <span>({formatCurrency(suggestion.allocatedAmount, suggestion.currency)})</span>
          </h2>
        </div>
        {reviewed && (
          <Badge tone="positive">
            <Check size={13} /> Revisada
          </Badge>
        )}
      </div>

      {suggestion.items.length ? (
        <div className="suggestion-list">
          {rankSuggestions(suggestion.items).map((item, index) => (
            <SuggestionCard
              key={item.tickerSymbol}
              item={item}
              companyName={assetNames[item.tickerSymbol] ?? item.tickerSymbol}
              currency={suggestion.currency}
              rank={index + 1}
              expanded={expandedTicker === item.tickerSymbol}
              onToggleDetails={() => onToggleDetails(item.tickerSymbol)}
            />
          ))}
        </div>
      ) : (
        <Card>
          <EmptyState
            icon={<CircleHelp size={22} />}
            title="Nenhuma compra sugerida"
            description="O valor pode ser insuficiente para uma cota inteira ou não há ativos elegíveis no momento. Confira os tetos e as cotações."
          />
        </Card>
      )}

      {suggestion.excludedAssets.length > 0 && (
        <Card className="excluded-card">
          <CardHeader
            title="Ativos não considerados"
            description="A simulação explica por que estes ativos ficaram de fora."
          />
          <div className="excluded-list">
            {suggestion.excludedAssets.map((asset) => (
              <div
                key={`${asset.tickerSymbol}-${asset.reason}`}
                className="excluded-row"
              >
                <Badge tone="warning">{asset.tickerSymbol}</Badge>
                <span>{asset.reason}</span>
              </div>
            ))}
          </div>
        </Card>
      )}

      {comparison && <PortfolioComparison comparison={comparison} />}

      <div className="suggestion-disclaimer">
        <Info size={17} aria-hidden="true" />
        <p>{suggestion.disclaimer}</p>
      </div>
      {suggestion.items.length > 0 && (
        <div className="result-actions">
          <p>Valor informado: {formatCurrency(suggestion.requestedAmount, suggestion.currency)}. Sem ordens automáticas.</p>
          <Button onClick={onReview}>
            Revisar sugestão <ArrowRight size={16} />
          </Button>
        </div>
      )}
    </section>
  )
}

function SuggestionCard({
  item,
  companyName,
  currency,
  rank,
  expanded,
  onToggleDetails,
}: {
  item: SuggestionItem
  companyName: string
  currency: string
  rank: number
  expanded: boolean
  onToggleDetails: () => void
}) {
  const dailyDrop =
    item.dailyChangePercent !== undefined && isNegative(item.dailyChangePercent)
  return (
    <article className="suggestion-asset">
      <div className="suggestion-asset-row">
        <div className="suggestion-asset-name">
          <span className="asset-avatar" aria-hidden="true">
            {item.tickerSymbol.slice(0, 1)}
          </span>
          <div>
            <strong>{item.tickerSymbol}</strong>
            <span>{companyName}</span>
          </div>
        </div>
        <div className="suggestion-asset-value">
          <strong>{formatCurrency(item.estimatedValue, currency)}</strong>
          <span>
            {formatDecimal(item.quantity, 0)} × {formatCurrency(item.unitPrice, currency)}
          </span>
        </div>
        <button
          className="suggestion-details-button"
          type="button"
          onClick={onToggleDetails}
          aria-expanded={expanded}
          aria-label={`${expanded ? 'Ocultar' : 'Ver'} detalhes de ${item.tickerSymbol}`}
          title={`${expanded ? 'Ocultar' : 'Ver'} score e justificativa`}
        >
          {expanded ? <Minus size={21} /> : <Plus size={21} />}
        </button>
      </div>
      {expanded && (
        <div className="suggestion-asset-details">
          <div className="suggestion-asset-score">
            <span>Prioridade #{rank}</span>
            <ScoreBadge score={item.score} />
          </div>
          {item.reasons.length > 0 && (
            <ul>
              {item.reasons.map((reason) => <li key={reason}>{reason}</li>)}
            </ul>
          )}
          <div className="signal-chips">
            {item.ceilingPrice !== undefined && (
              <Badge tone="info">Teto {formatCurrency(item.ceilingPrice, currency)}</Badge>
            )}
            {item.dailyChangePercent !== undefined && (
              <Badge tone={dailyDrop ? 'negative' : 'neutral'}>
                {dailyDrop ? <ArrowDownRight size={13} /> : <ArrowUpRight size={13} />}
                {formatPercent(item.dailyChangePercent)} no dia
              </Badge>
            )}
            {item.targetPercentage !== undefined && (
              <Badge tone="warning">Meta {formatPercent(item.targetPercentage)}</Badge>
            )}
          </div>
          <p className="suggestion-asset-source">
            {item.quoteProvider} · Cotação observada {formatDateTime(item.quoteObservedAt)}
          </p>
        </div>
      )}
    </article>
  )
}
