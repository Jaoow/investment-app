import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  Check,
  CircleHelp,
  Clock3,
  Info,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { usePortfolio } from '@/app/usePortfolio'
import { ApiRequestError } from '@/shared/api/client'
import { useAssetSettings, usePortfolioSummary, useSuggestion } from '@/shared/api/queries'
import type { Suggestion, SuggestionItem } from '@/shared/api/models'
import { ScoreBadge } from './ScoreBadge'
import { RecommendationRankingTable } from './RecommendationRankingTable'
import { rankSuggestions } from './recommendation-ranking'
import { ContributionConfiguration } from './ContributionConfiguration'
import { buildPortfolioComparison } from './portfolio-comparison'
import { PortfolioComparison } from './PortfolioComparison'
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
  Dialog,
  EmptyState,
  Input,
  PageHeading,
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

  return (
    <>
      <PageHeading
        eyebrow="DECISÃO CONSCIENTE, UM APORTE POR VEZ"
        title="O que fazer com seu próximo aporte?"
        description="Informe o valor disponível. Vamos mostrar uma simulação baseada nos seus tetos, metas e cotações."
      />

      {selectedPortfolio && (
        <div className="contribution-configuration">
          <ContributionConfiguration
            portfolioId={selectedPortfolioId}
            assets={portfolioSummary.data?.assetSummaries ?? []}
            portfolioLoading={portfolioSummary.isLoading}
          />
        </div>
      )}

      <div className="contribution-layout">
        <div className="contribution-main">
          <Card className="contribution-form-card">
            <div className="contribution-step">
              <span className="step-number">01</span>
              <div>
                <p className="eyebrow">VALOR DO APORTE</p>
                <h2>Quanto você quer investir?</h2>
              </div>
            </div>
            {selectedPortfolio ? (
              <form className="contribution-form" onSubmit={submit} noValidate>
                <Input
                  label="Valor disponível"
                  name="contributionAmount"
                  inputMode="decimal"
                  autoComplete="off"
                  placeholder="Ex.: 1.000,00"
                  value={amountInput}
                  onChange={(event) => setAmountInput(event.target.value)}
                  error={amountError}
                  hint="Digite em reais. A simulação considera cotas inteiras."
                  className="money-input"
                />
                <span className="currency-suffix" aria-hidden="true">
                  BRL
                </span>
                {serviceError && (
                  <div className="service-error" role="alert">
                    <Info size={19} aria-hidden="true" />
                    <div>
                      <strong>Não foi possível atualizar sua simulação</strong>
                      <p>{serviceError}</p>
                    </div>
                  </div>
                )}
                <Button
                  type="submit"
                  busy={suggestion.isPending}
                  disabled={
                    suggestion.isPending || amountInput.trim().length === 0
                  }
                >
                  <Sparkles size={17} aria-hidden="true" />
                  {suggestion.isPending
                    ? 'Analisando carteira…'
                    : 'Ver sugestão de aporte'}
                </Button>
              </form>
            ) : (
              <EmptyState
                icon={<CircleHelp size={22} />}
                title="Selecione uma carteira"
                description="Escolha ou crie uma carteira para simular um aporte."
              />
            )}
          </Card>

          {result && (
            <SuggestionResult
              suggestion={result}
              portfolioName={selectedPortfolio?.name ?? 'Sua carteira'}
              onReview={() => setReviewOpen(true)}
              reviewed={lastReviewed}
              comparison={portfolioComparison}
            />
          )}
        </div>

        <aside className="contribution-aside">
          <Card className="how-it-works-card">
            <div className="aside-heading">
              <span className="aside-icon">
                <Sparkles size={18} />
              </span>
              <h2>Como a sugestão funciona</h2>
            </div>
            <ol className="signal-list">
              <li>
                <span className="signal-marker">1</span>
                <div>
                  <strong>Preço teto</strong>
                  <p>Prioriza ativos abaixo do limite que você definiu.</p>
                </div>
              </li>
              <li>
                <span className="signal-marker">2</span>
                <div>
                  <strong>Movimento do dia</strong>
                  <p>Considera a variação diária informada pela fonte.</p>
                </div>
              </li>
              <li>
                <span className="signal-marker">3</span>
                <div>
                  <strong>Seu balanceamento</strong>
                  <p>Observa a distância entre a posição atual e a meta.</p>
                </div>
              </li>
            </ol>
            <div className="aside-note">
              <ShieldCheck size={16} aria-hidden="true" />
              <span>
                As metas de carteira e os pesos da recomendação são conceitos
                separados.
              </span>
            </div>
          </Card>
          <Card className="disclaimer-card">
            <div className="disclaimer-title">
              <Info size={17} /> Informação importante
            </div>
            <p>
              Esta simulação é informativa, não é consultoria financeira e não
              envia ordens à corretora. Preços podem mudar e custos ou impostos
              não estão incluídos.
            </p>
          </Card>
        </aside>
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
  portfolioName,
  onReview,
  reviewed,
  comparison,
}: {
  suggestion: Suggestion
  portfolioName: string
  onReview: () => void
  reviewed: boolean
  comparison: ReturnType<typeof buildPortfolioComparison> | null
}) {
  return (
    <section className="suggestion-result" aria-live="polite">
      <div className="result-heading">
        <div>
          <p className="eyebrow">
            SIMULAÇÃO PARA {portfolioName.toUpperCase()}
          </p>
          <h2>Uma proposta para o seu aporte</h2>
        </div>
        {reviewed && (
          <Badge tone="positive">
            <Check size={13} /> Revisada
          </Badge>
        )}
      </div>

      <div className="result-total-grid">
        <Card className="result-total-card result-total-primary">
          <span>Valor do aporte</span>
          <strong>
            {formatCurrency(suggestion.requestedAmount, suggestion.currency)}
          </strong>
        </Card>
        <Card className="result-total-card">
          <span>Alocado na simulação</span>
          <strong>
            {formatCurrency(suggestion.allocatedAmount, suggestion.currency)}
          </strong>
        </Card>
        <Card className="result-total-card">
          <span>Saldo restante</span>
          <strong>
            {formatCurrency(suggestion.remainingAmount, suggestion.currency)}
          </strong>
        </Card>
      </div>

      {suggestion.items.length ? (
        <>
          <RecommendationRankingTable
            items={suggestion.items}
            currency={suggestion.currency}
          />
          <div className="suggestion-list">
            {rankSuggestions(suggestion.items).map((item) => (
            <SuggestionCard
              key={item.tickerSymbol}
              item={item}
              currency={suggestion.currency}
            />
            ))}
          </div>
        </>
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
          <p>Sem ordens automáticas. Revise antes de tomar qualquer decisão.</p>
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
  currency,
}: {
  item: SuggestionItem
  currency: string
}) {
  const dailyDrop =
    item.dailyChangePercent !== undefined && isNegative(item.dailyChangePercent)
  return (
    <Card className="suggestion-card">
      <div className="suggestion-card-top">
        <div className="suggested-asset-id">
          <span className="asset-avatar">{item.tickerSymbol.slice(0, 1)}</span>
          <div>
            <strong>{item.tickerSymbol}</strong>
            <span>Ativo sugerido</span>
          </div>
        </div>
        <ScoreBadge score={item.score} />
      </div>
      <div className="suggestion-numbers">
        <div>
          <span>Quantidade</span>
          <strong>
            {item.quantity} <small>un.</small>
          </strong>
        </div>
        <div>
          <span>Preço por unidade</span>
          <strong>{formatCurrency(item.unitPrice, currency)}</strong>
        </div>
        <div className="suggestion-value">
          <span>Valor estimado</span>
          <strong>{formatCurrency(item.estimatedValue, currency)}</strong>
        </div>
      </div>
      <div className="reason-panel">
        <div className="reason-title">
          <Sparkles size={15} aria-hidden="true" />
          <strong>Por que este ativo?</strong>
        </div>
        {item.reasons.length > 0 ? (
          <ul>
            {item.reasons.map((reason) => (
              <li key={reason}>{reason}</li>
            ))}
          </ul>
        ) : (
          <p>A pontuação considerou os sinais disponíveis para este ativo.</p>
        )}
        <div className="signal-chips">
          {item.ceilingDistancePercentage !== undefined && (
            <Badge tone="info">
              {formatPercent(item.ceilingDistancePercentage)} abaixo do teto
            </Badge>
          )}
          {item.dailyChangePercent !== undefined && (
            <Badge tone={dailyDrop ? 'negative' : 'neutral'}>
              {dailyDrop ? (
                <ArrowDownRight size={13} />
              ) : (
                <ArrowUpRight size={13} />
              )}
              {formatPercent(item.dailyChangePercent)} no dia
            </Badge>
          )}
          {item.targetPercentage !== undefined && (
            <Badge tone="warning">
              Meta {formatPercent(item.targetPercentage)}
            </Badge>
          )}
        </div>
      </div>
      <div className="quote-meta">
        <span>
          <Clock3 size={14} /> Cotação observada{' '}
          {formatDateTime(item.quoteObservedAt)}
        </span>
        <span>Fonte: {item.quoteProvider}</span>
      </div>
    </Card>
  )
}
