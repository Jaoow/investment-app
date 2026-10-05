import { Card, CardHeader } from '@/shared/components/ui'
import { formatCurrency, formatPercent } from '@/shared/format/numbers'
import type { SuggestionItem } from '@/shared/api/models'
import { ScoreBadge } from './ScoreBadge'
import { rankSuggestions } from './recommendation-ranking'

export function RecommendationRankingTable({
  items,
  currency,
}: {
  items: SuggestionItem[]
  currency: string
}) {
  const rankedItems = rankSuggestions(items)
  return (
    <Card className="recommendation-ranking-card">
      <CardHeader
        title="Ranking de recomendação"
        description="Maior prioridade primeiro; sinais ausentes são indicados como indisponíveis."
      />
      <div className="recommendation-table-scroll">
        <table className="recommendation-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Ativo</th>
              <th>Score</th>
              <th>Atual</th>
              <th>Meta</th>
              <th>Preço</th>
              <th>Teto</th>
              <th>Variação dia</th>
              <th>Valor sugerido</th>
              <th>Quantidade</th>
            </tr>
          </thead>
          <tbody>
            {rankedItems.map((item, index) => (
              <tr key={item.tickerSymbol}>
                <td>{index + 1}</td>
                <th scope="row">{item.tickerSymbol}</th>
                <td><ScoreBadge score={item.score} /></td>
                <td>{item.currentPercentage === undefined ? '—' : formatPercent(item.currentPercentage)}</td>
                <td>{item.targetPercentage === undefined ? '—' : formatPercent(item.targetPercentage)}</td>
                <td>{formatCurrency(item.unitPrice, currency)}</td>
                <td>{item.ceilingPrice === undefined ? '—' : formatCurrency(item.ceilingPrice, currency)}</td>
                <td>{item.dailyChangePercent === undefined ? '—' : formatPercent(item.dailyChangePercent)}</td>
                <td>{formatCurrency(item.estimatedValue, currency)}</td>
                <td>{item.quantity} un.</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}