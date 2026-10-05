import { describe, expect, it } from 'vitest'
import type { AssetSetting, AssetSummary, SuggestionItem } from '@/shared/api/models'
import { buildPortfolioComparison } from './portfolio-comparison'

function asset(tickerSymbol: string, currentValue: string, category: string): AssetSummary {
  return {
    tickerSymbol,
    quantity: '1',
    averagePrice: currentValue,
    totalInvested: currentValue,
    currentValue,
    profitOrLoss: '0',
    percentageChange: '0',
    tickerFields: { category },
  }
}

function setting(tickerSymbol: string, category: AssetSetting['category']): AssetSetting {
  return {
    tickerSymbol,
    category,
    categoryTargetPercentage: '50',
    assetTargetPercentage: '100',
    allocationPercentage: '50',
    priceCeiling: '50',
  }
}

describe('buildPortfolioComparison', () => {
  it('projects category weights using only allocated recommendation values', () => {
    const suggestions: Pick<SuggestionItem, 'tickerSymbol' | 'estimatedValue'>[] = [
      { tickerSymbol: 'PETR4', estimatedValue: '100' },
    ]
    const result = buildPortfolioComparison(
      [
        asset('PETR4', '400', 'EQUITIES'),
        asset('MXRF11', '600', 'REAL_ESTATE_FUNDS'),
      ],
      [setting('PETR4', 'EQUITIES')],
      suggestions,
    )

    expect(result?.beforeTotal.toString()).toBe('1000')
    expect(result?.afterTotal.toString()).toBe('1100')
    expect(
      result?.after.map(({ label, percentage }) => [label, percentage.round(2).toString()]),
    ).toEqual([
      ['FIIs', '54.55'],
      ['Ações', '45.45'],
    ])
  })

  it('declines to project if a recommendation has no known class', () => {
    expect(
      buildPortfolioComparison(
        [],
        [],
        [{ tickerSymbol: 'UNKNOWN', estimatedValue: '10' }],
      ),
    ).toBeNull()
  })
})