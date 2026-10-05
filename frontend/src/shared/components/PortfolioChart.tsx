import { ChartNoAxesCombined } from 'lucide-react'
import { Card, CardHeader } from './ui'

const periods = ['7 dias', '30 dias', '90 dias', '12 meses', 'Tudo']

export function PortfolioChart() {
  return (
    <Card className="portfolio-history-card">
      <CardHeader
        title="Evolução da carteira"
        description="Patrimônio, resultado e aportes ao longo do tempo"
        action={
          <div className="portfolio-chart-periods" role="group" aria-label="Período do gráfico">
            {periods.map((period) => (
              <button type="button" key={period} disabled>
                {period}
              </button>
            ))}
          </div>
        }
      />
      <div className="portfolio-history-unavailable" role="status">
        <span className="portfolio-history-icon" aria-hidden="true">
          <ChartNoAxesCombined size={22} />
        </span>
        <div>
          <strong>Histórico patrimonial ainda indisponível</strong>
          <p>
            A API fornece posições e cotações atuais, mas ainda não registra
            snapshots históricos da carteira.
          </p>
        </div>
      </div>
    </Card>
  )
}