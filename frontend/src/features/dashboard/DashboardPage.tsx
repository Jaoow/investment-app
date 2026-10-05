import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  BriefcaseBusiness,
  CircleDollarSign,
  Layers3,
  Plus,
  Sparkles,
  TrendingUp,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { useCreatePortfolio, usePortfolioSummary } from '@/shared/api/queries'
import {
  formatCurrency,
  formatPercent,
  isNegative,
} from '@/shared/format/numbers'
import {
  Badge,
  Button,
  Card,
  Dialog,
  EmptyState,
  InlineError,
  Input,
  PageHeading,
  Skeleton,
} from '@/shared/components/ui'
import { usePortfolio } from '@/app/usePortfolio'
import { buildPortfolioComposition } from './portfolio-composition'
import { SummaryCard } from './SummaryCard'
import { AssetTable } from './AssetTable'
import { AllocationChart } from '@/shared/components/AllocationChart'
import { PortfolioChart } from '@/shared/components/PortfolioChart'

function PortfolioCreateDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [name, setName] = useState('')
  const createPortfolio = useCreatePortfolio()

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (name.trim().length === 0) return
    try {
      await createPortfolio.mutateAsync(name.trim())
      setName('')
      onOpenChange(false)
      toast.success('Sua carteira foi criada.')
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Não foi possível criar a carteira.',
      )
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Criar carteira"
      description="Dê um nome para organizar seus investimentos."
    >
      <form className="form-stack dialog-form" onSubmit={submit}>
        <Input
          label="Nome da carteira"
          value={name}
          onChange={(event) => setName(event.target.value)}
          maxLength={120}
          autoFocus
          required
        />
        <div className="dialog-actions">
          <Button
            type="button"
            variant="secondary"
            onClick={() => onOpenChange(false)}
          >
            Cancelar
          </Button>
          <Button
            type="submit"
            busy={createPortfolio.isPending}
            disabled={!name.trim()}
          >
            Criar carteira
          </Button>
        </div>
      </form>
    </Dialog>
  )
}

export function DashboardPage() {
  const {
    portfolios,
    selectedPortfolio,
    selectedPortfolioId,
    loading,
    error,
    refresh,
  } = usePortfolio()
  const summary = usePortfolioSummary(selectedPortfolioId)
  const [createOpen, setCreateOpen] = useState(false)
  const assets = summary.data?.assetSummaries ?? []
  const composition = buildPortfolioComposition(assets)

  return (
    <>
      <PageHeading
        eyebrow="SEU PATRIMÔNIO, EM PERSPECTIVA"
        title="Bom ter você por aqui."
        description="Acompanhe sua carteira e decida com mais clareza o próximo passo."
        action={
          <Button asChild>
            <Link to="/aporte">
              <Sparkles size={17} aria-hidden="true" />
              Planejar aporte
            </Link>
          </Button>
        }
      />

      {loading && (
        <div className="summary-grid" aria-label="Carregando suas carteiras">
          <Skeleton className="summary-skeleton" />
          <Skeleton className="summary-skeleton" />
          <Skeleton className="summary-skeleton" />
          <Skeleton className="summary-skeleton" />
        </div>
      )}

      {!loading && error && (
        <InlineError message={error.message} onRetry={refresh} />
      )}

      {!loading && !error && portfolios.length === 0 && (
        <Card>
          <EmptyState
            icon={<BriefcaseBusiness size={25} />}
            title="Sua primeira carteira começa aqui"
            description="Crie uma carteira para acompanhar seus ativos e receber simulações de aporte."
            action={
              <Button onClick={() => setCreateOpen(true)}>
                <Plus size={17} /> Criar carteira
              </Button>
            }
          />
        </Card>
      )}

      {selectedPortfolio && (
        <>
          {summary.isLoading && (
            <div className="summary-grid">
              <Skeleton className="summary-skeleton" />
              <Skeleton className="summary-skeleton" />
              <Skeleton className="summary-skeleton" />
              <Skeleton className="summary-skeleton" />
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
              <div className="summary-grid">
                <SummaryCard
                  label="Patrimônio total"
                  value={formatCurrency(summary.data.currentValue)}
                  caption="Valor estimado dos ativos da carteira"
                  icon={<CircleDollarSign size={18} />}
                  iconTone="neutral"
                />
                <SummaryCard
                  label="Valor investido"
                  value={formatCurrency(summary.data.totalInvested)}
                  caption="Com base nas movimentações registradas"
                  icon={<BriefcaseBusiness size={18} />}
                  iconTone="neutral"
                />
                <SummaryCard
                  label="Lucro acumulado"
                  value={formatCurrency(summary.data.profitOrLoss)}
                  valueTone={isNegative(summary.data.profitOrLoss) ? 'text-loss' : 'text-gain'}
                  icon={<TrendingUp size={18} />}
                  iconTone={isNegative(summary.data.profitOrLoss) ? 'negative' : 'positive'}
                  caption={
                    <>
                      <span
                        className={
                          isNegative(summary.data.percentageChange)
                            ? 'text-loss'
                            : 'text-gain'
                        }
                      >
                        {isNegative(summary.data.percentageChange) ? (
                          <ArrowUpRight size={14} />
                        ) : (
                          <ArrowDownRight size={14} />
                        )}
                        {formatPercent(summary.data.percentageChange)}
                      </span>{' '}
                      sobre o total investido
                    </>
                  }
                />
                <SummaryCard
                  label="Total de ativos"
                  value={String(assets.length)}
                  caption="Posições na carteira"
                  icon={<Layers3 size={18} />}
                  iconTone="neutral"
                />
              </div>

              {composition.total.gt(0) && (
                <section className="composition-grid" aria-label="Composição da carteira">
                  <AllocationChart
                    title="Alocação por classe"
                    description="Participação no patrimônio atual"
                    slices={composition.categories}
                    total={composition.total.toString()}
                  />
                  <AllocationChart
                    title="Alocação por ativo"
                    description="Maiores posições da carteira"
                    slices={composition.assets}
                    total={composition.total.toString()}
                  />
                </section>
              )}

              <PortfolioChart />

              <div className="dashboard-side">
                  <Card className="contribution-prompt">
                    <span className="prompt-icon">
                      <Sparkles size={20} />
                    </span>
                    <p className="eyebrow">SEU PRÓXIMO APORTE</p>
                    <h2>Transforme o valor disponível em um plano.</h2>
                    <p>
                      Compare ativos abaixo do seu teto e veja como o aporte
                      pode aproximar sua carteira dos seus objetivos.
                    </p>
                    <Button asChild className="button-full">
                      <Link to="/aporte">
                        Simular um aporte <ArrowRight size={16} />
                      </Link>
                    </Button>
                    <small>
                      Simulação informativa. Nenhuma ordem será enviada.
                    </small>
                  </Card>
                  <Card className="tip-card">
                    <Badge tone="info">Dica Investize</Badge>
                    <h3>Uma carteira alinhada começa com metas claras.</h3>
                    <p>
                      Defina seus objetivos de alocação para dar contexto às
                      próximas sugestões.
                    </p>
                    <Button asChild variant="ghost" size="small">
                      <Link to="/alocacao">
                        Ver balanceamento <ArrowRight size={15} />
                      </Link>
                    </Button>
                  </Card>
              </div>

              <AssetTable
                assets={assets}
                portfolioValue={summary.data.currentValue}
              />
            </>
          )}
        </>
      )}
      <PortfolioCreateDialog open={createOpen} onOpenChange={setCreateOpen} />
    </>
  )
}
