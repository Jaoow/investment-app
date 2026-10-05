import { AllocationChart } from '@/shared/components/AllocationChart'
import { Card, EmptyState } from '@/shared/components/ui'
import type { buildPortfolioComparison } from './portfolio-comparison'

export function PortfolioComparison({
  comparison,
}: {
  comparison: NonNullable<ReturnType<typeof buildPortfolioComparison>>
}) {
  return (
    <section className="portfolio-comparison">
      <div className="portfolio-comparison-heading">
        <div>
          <p className="eyebrow">CARTEIRA PROJETADA</p>
          <h3>Como a alocação pode ficar</h3>
        </div>
        <p>Estimativa sem custos e impostos; nenhuma posição é alterada.</p>
      </div>
      <div className="portfolio-comparison-grid">
        {comparison.before.length > 0 ? (
          <AllocationChart
            title="Antes do aporte"
            description="Composição atual"
            slices={comparison.before}
            total={comparison.beforeTotal.toString()}
          />
        ) : (
          <Card className="portfolio-comparison-empty">
            <EmptyState
              icon={<span aria-hidden="true">—</span>}
              title="Sem posições atuais"
              description="A simulação considera apenas as compras sugeridas."
            />
          </Card>
        )}
        <AllocationChart
          title="Depois do aporte"
          description="Composição estimada após as compras"
          slices={comparison.after}
          total={comparison.afterTotal.toString()}
        />
      </div>
    </section>
  )
}