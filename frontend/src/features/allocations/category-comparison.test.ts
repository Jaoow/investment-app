import { describe, expect, it } from 'vitest'
import type { AssetSummary } from '@/shared/api/models'
import { buildCategoryComparison } from './category-comparison'

function asset(tickerSymbol: string, value: string, category: string): AssetSummary {
  return {
    tickerSymbol,
    quantity: '1',
    averagePrice: value,
    totalInvested: value,
    currentValue: value,
    profitOrLoss: '0',
    percentageChange: '0',
    tickerFields: { category },
  }
}

describe('buildCategoryComparison', () => {
  it('calculates class weight and deviation from the editable target', () => {
    const rows = buildCategoryComparison(
      [
        asset('PETR4', '420', 'EQUITIES'),
        asset('MXRF11', '250', 'REAL_ESTATE_FUNDS'),
      ],
      '1000',
      { EQUITIES: '35', REAL_ESTATE_FUNDS: '25', BDRS: '10' },
      [
        { id: 'EQUITIES', label: 'Ações' },
        { id: 'REAL_ESTATE_FUNDS', label: 'FIIs' },
        { id: 'BDRS', label: 'BDRs' },
      ],
    )

    expect(rows.map(({ currentPercentage, difference }) => [
      currentPercentage.toString(),
      difference?.toString(),
    ])).toEqual([
      ['42', '7'],
      ['25', '0'],
      ['0', '-10'],
    ])
  })
})