import type Big from 'big.js'
import { formatCurrency, formatPercent } from '@/shared/format/numbers'
import { Card, CardHeader } from './ui'

const colors = [
  'var(--composition-1)',
  'var(--composition-2)',
  'var(--composition-3)',
  'var(--composition-4)',
  'var(--composition-5)',
  'var(--composition-6)',
]

export interface AllocationChartSlice {
  label: string
  percentage: Big
}

export function AllocationChart({
  title,
  description,
  slices,
  total,
}: {
  title: string
  description: string
  slices: AllocationChartSlice[]
  total: string
}) {
  let end = 0
  const gradient = slices
    .map((slice, index) => {
      const start = end
      end += Number(slice.percentage)
      return `${colors[index % colors.length]} ${start}% ${end}%`
    })
    .join(', ')

  return (
    <Card className="composition-card">
      <CardHeader title={title} description={description} />
      <div className="composition-content">
        <div
          className="composition-donut"
          role="img"
          aria-label={`${title}: ${slices.map((slice) => `${slice.label} ${formatPercent(slice.percentage.toString(), 1)}`).join(', ')}`}
          style={{ background: `conic-gradient(${gradient})` }}
        >
          <span className="composition-donut-hole">
            <strong>{formatCurrency(total)}</strong>
            <small>valor atual</small>
          </span>
        </div>
        {slices.length > 1 && (
          <ul className="composition-legend">
            {slices.map((slice, index) => (
              <li key={slice.label}>
                <span
                  className="composition-swatch"
                  style={{ backgroundColor: colors[index % colors.length] }}
                  aria-hidden="true"
                />
                <span className="composition-label" title={slice.label}>
                  {slice.label}
                </span>
                <strong>{formatPercent(slice.percentage.toString(), 1)}</strong>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Card>
  )
}