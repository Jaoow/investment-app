import Big from 'big.js'
import type { AssetSetting, AssetSummary, SuggestionItem } from '@/shared/api/models'
import type { AllocationChartSlice } from '@/shared/components/AllocationChart'

const categoryLabels: Record<string, string> = {
  EQUITIES: 'Ações',
  REAL_ESTATE_FUNDS: 'FIIs',
  BDRS: 'BDRs',
  OTHER: 'Outros',
}

export function buildPortfolioComparison(
  assets: AssetSummary[],
  settings: AssetSetting[],
  suggestions: Pick<SuggestionItem, 'tickerSymbol' | 'estimatedValue'>[],
) {
  const values = new Map<string, Big>()
  const categoryByTicker = new Map<string, string>()

  for (const asset of assets) {
    const category = asset.tickerFields?.category ?? 'OTHER'
    categoryByTicker.set(asset.tickerSymbol, categoryLabels[category] ? category : 'OTHER')
    values.set(
      categoryLabels[category] ? category : 'OTHER',
      (values.get(categoryLabels[category] ? category : 'OTHER') ?? new Big(0)).plus(
        String(asset.currentValue),
      ),
    )
  }
  for (const setting of settings) {
    categoryByTicker.set(setting.tickerSymbol, setting.category)
  }

  for (const item of suggestions) {
    const category = categoryByTicker.get(item.tickerSymbol)
    if (!category) return null
    values.set(
      category,
      (values.get(category) ?? new Big(0)).plus(String(item.estimatedValue)),
    )
  }

  const toSlices = (source: Map<string, Big>): AllocationChartSlice[] => {
    const total = [...source.values()].reduce(
      (sum, value) => sum.plus(value),
      new Big(0),
    )
    if (total.eq(0)) return []
    return [...source.entries()]
      .filter(([, value]) => value.gt(0))
      .map(([category, value]) => ({
        label: categoryLabels[category] ?? 'Outros',
        percentage: value.div(total).times(100),
      }))
      .sort((left, right) => right.percentage.cmp(left.percentage))
  }

  const beforeValues = new Map<string, Big>()
  for (const asset of assets) {
    const category = asset.tickerFields?.category ?? 'OTHER'
    const normalizedCategory = categoryLabels[category] ? category : 'OTHER'
    beforeValues.set(
      normalizedCategory,
      (beforeValues.get(normalizedCategory) ?? new Big(0)).plus(
        String(asset.currentValue),
      ),
    )
  }

  return {
    before: toSlices(beforeValues),
    after: toSlices(values),
    beforeTotal: [...beforeValues.values()].reduce(
      (sum, value) => sum.plus(value),
      new Big(0),
    ),
    afterTotal: [...values.values()].reduce(
      (sum, value) => sum.plus(value),
      new Big(0),
    ),
  }
}