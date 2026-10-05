import Big from 'big.js'
import type { AssetSummary } from '@/shared/api/models'

const categoryLabels: Record<string, string> = {
  EQUITIES: 'Ações',
  REAL_ESTATE_FUNDS: 'Fundos imobiliários',
  BDRS: 'BDRs',
}

export interface CompositionSlice {
  label: string
  percentage: Big
}

export function buildPortfolioComposition(assets: AssetSummary[]) {
  const validAssets = assets.flatMap((asset) => {
    const value = new Big(String(asset.currentValue))
    return value.gt(0) ? [{ asset, value }] : []
  })
  const total = validAssets.reduce(
    (sum, { value }) => sum.plus(value),
    new Big(0),
  )

  if (total.eq(0)) return { total, categories: [], assets: [] }

  const categoryValues = new Map<string, Big>()
  for (const { asset, value } of validAssets) {
    const category = asset.tickerFields?.category ?? 'OTHER'
    const label = categoryLabels[category] ?? 'Outros'
    categoryValues.set(label, (categoryValues.get(label) ?? new Big(0)).plus(value))
  }

  const toSlices = (values: Array<[string, Big]>): CompositionSlice[] =>
    values.map(([label, value]) => ({
      label,
      percentage: value.div(total).times(100),
    }))

  const categories = [...categoryValues.entries()]
    .sort((left, right) => right[1].cmp(left[1]))
  const rankedAssets = validAssets
    .map(({ asset, value }) => [asset.tickerSymbol, value] as [string, Big])
    .sort((left, right) => right[1].cmp(left[1]))
  const visibleAssets = rankedAssets.slice(0, 5)
  const remainingAssets = rankedAssets.slice(5)
  if (remainingAssets.length > 0) {
    visibleAssets.push([
      'Outros',
      remainingAssets.reduce((sum, [, value]) => sum.plus(value), new Big(0)),
    ])
  }

  return {
    total,
    categories: toSlices(categories),
    assets: toSlices(visibleAssets),
  }
}