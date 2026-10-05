import { describe, expect, it } from 'vitest'
import type { SuggestionItem } from '@/shared/api/models'
import { rankSuggestions } from './recommendation-ranking'
import { presentScore } from './score-presentation'

describe('presentScore', () => {
  it.each([
    ['0.80', '80', 'Forte compra', 'positive'],
    ['0.60', '60', 'Compra moderada', 'info'],
    ['0.40', '40', 'Compra neutra', 'warning'],
    ['0.3999', '39', 'Baixa prioridade', 'neutral'],
  ])('maps normalized score %s to its classification', (score, points, label, tone) => {
    expect(presentScore(score)).toEqual({ points, label, tone })
  })

  it('ranks suggestions from highest normalized score to lowest', () => {
    const makeItem = (tickerSymbol: string, score: string): SuggestionItem => ({
      tickerSymbol,
      quantity: '1',
      unitPrice: '10',
      estimatedValue: '10',
      score,
      reasons: [],
      quoteProvider: 'test',
      quoteObservedAt: '2026-01-01T00:00:00Z',
    })

    expect(
      rankSuggestions([
        makeItem('LOW', '0.25'),
        makeItem('HIGH', '0.92'),
        makeItem('MID', '0.61'),
      ]).map(({ tickerSymbol }) => tickerSymbol),
    ).toEqual(['HIGH', 'MID', 'LOW'])
  })
})