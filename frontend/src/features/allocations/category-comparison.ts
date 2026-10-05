import Big from 'big.js'
import type { AssetSummary, DecimalValue } from '@/shared/api/models'

export interface AllocationCategory {
  id: string
  label: string
}

export interface CategoryComparison {
  id: string
  label: string
  currentValue: Big
  currentPercentage: Big
  targetPercentage: Big | null
  difference: Big | null
}

function parseTarget(value?: string): Big | null {
  if (value === undefined || value.trim() === '') return null
  try {
    return new Big(value.replace(',', '.'))
  } catch {
    return null
  }
}

export function buildCategoryComparison(
  assets: AssetSummary[],
  portfolioValue: DecimalValue,
  targets: Record<string, string>,
  categories: AllocationCategory[],
): CategoryComparison[] {
  const total = new Big(String(portfolioValue))
  return categories.map(({ id, label }) => {
    const currentValue = assets
      .filter((asset) => asset.tickerFields?.category === id)
      .reduce(
        (sum, asset) => sum.plus(String(asset.currentValue)),
        new Big(0),
      )
    const currentPercentage = total.gt(0)
      ? currentValue.div(total).times(100)
      : new Big(0)
    const targetPercentage = parseTarget(targets[id])
    return {
      id,
      label,
      currentValue,
      currentPercentage,
      targetPercentage,
      difference: targetPercentage
        ? currentPercentage.minus(targetPercentage)
        : null,
    }
  })
}