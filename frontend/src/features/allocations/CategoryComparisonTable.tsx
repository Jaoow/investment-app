import type { AssetSummary, DecimalValue } from '@/shared/api/models'
import { formatCurrency, formatPercent } from '@/shared/format/numbers'
import { Card, CardHeader } from '@/shared/components/ui'
import Big from 'big.js'
import { buildCategoryComparison, type AllocationCategory } from './category-comparison'

function progressWidth(value: Big): string {
  if (value.lt(0)) return '0'
  if (value.gt(100)) return '100'
  return value.toString()
}

export function CategoryComparisonTable({
  assets,
  portfolioValue,
  targets,
  categories,
}: {
  assets: AssetSummary[]
  portfolioValue: DecimalValue
  targets: Record<string, string>
  categories: AllocationCategory[]
}) {
  const rows = buildCategoryComparison(assets, portfolioValue, targets, categories)

  return (
    <Card className="category-comparison-card">
      <CardHeader
        title="Carteira atual x metas"
        description="A comparação acompanha as alterações em andamento antes de salvar."
      />
      <div className="category-comparison-table-wrap">
        <table className="category-comparison-table">
          <thead>
            <tr>
              <th>Classe</th>
              <th>Valor atual</th>
              <th>Atual</th>
              <th>Meta</th>
              <th>Diferença</th>
              <th>Progresso</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const difference = row.difference
              const state =
                difference === null
                  ? 'target-missing'
                  : difference.gt(0)
                    ? 'above-target'
                    : difference.lt(0)
                      ? 'below-target'
                      : 'on-target'
              const currentWidth = progressWidth(row.currentPercentage)
              const targetPosition = row.targetPercentage
                ? progressWidth(row.targetPercentage)
                : undefined

              return (
                <tr key={row.id}>
                  <th scope="row">{row.label}</th>
                  <td>{formatCurrency(row.currentValue.toString())}</td>
                  <td>{formatPercent(row.currentPercentage.toString())}</td>
                  <td>
                    {row.targetPercentage === null
                      ? '—'
                      : formatPercent(row.targetPercentage.toString())}
                  </td>
                  <td className={state}>
                    {difference === null
                      ? 'Defina a meta'
                      : `${difference.gt(0) ? '+' : ''}${formatPercent(difference.toString())}`}
                  </td>
                  <td>
                    <div
                      className="category-comparison-track"
                      aria-label={`${row.label}: atual ${formatPercent(row.currentPercentage.toString())}, meta ${row.targetPercentage === null ? 'não definida' : formatPercent(row.targetPercentage.toString())}`}
                    >
                      <span style={{ width: `${currentWidth}%` }} />
                      {targetPosition !== undefined && (
                        <i style={{ left: `${targetPosition}%` }} />
                      )}
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </Card>
  )
}