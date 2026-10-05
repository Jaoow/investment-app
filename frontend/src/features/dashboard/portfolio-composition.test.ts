import { describe, expect, it } from 'vitest'
import type { AssetSummary } from '@/shared/api/models'
import { buildPortfolioComposition } from './portfolio-composition'

function asset(
  tickerSymbol: string,
  currentValue: string,
  category?: string,
): AssetSummary {
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

describe('buildPortfolioComposition', () => {
  it('groups current market value by category and asset', () => {
    const composition = buildPortfolioComposition([
      asset('PETR4', '300', 'EQUITIES'),
      asset('MXRF11', '200', 'REAL_ESTATE_FUNDS'),
      asset('UNKNOWN', '0'),
    ])

    expect(composition.total.toString()).toBe('500')
    expect(
      composition.categories.map(({ label, percentage }) => [
        label,
        percentage.toString(),
      ]),
    ).toEqual([
      ['Ações', '60'],
      ['Fundos imobiliários', '40'],
    ])
    expect(composition.assets.map(({ label }) => label)).toEqual([
      'PETR4',
      'MXRF11',
    ])
  })

  it('limits the asset legend to five positions plus an others slice', () => {
    const composition = buildPortfolioComposition(
      Array.from({ length: 7 }, (_, index) =>
        asset(`ASSET${index}`, `${70 - index * 10}`, 'BDRS'),
      ),
    )

    expect(composition.assets.map(({ label }) => label)).toEqual([
      'ASSET0',
      'ASSET1',
      'ASSET2',
      'ASSET3',
      'ASSET4',
      'Outros',
    ])
    expect(Number(composition.assets.at(-1)?.percentage)).toBeCloseTo(
      (30 / 280) * 100,
      6,
    )
  })
})