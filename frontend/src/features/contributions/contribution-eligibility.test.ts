import { describe, expect, it } from 'vitest'
import { contributionEligibility } from './contribution-eligibility'

describe('contributionEligibility', () => {
  it.each([
    ['34.90', '40', 'Dentro do teto', 'positive'],
    ['41', '40', 'Acima do teto', 'warning'],
    [undefined, '40', 'Sem cotação atual', 'neutral'],
    ['34.90', null, 'Sem preço teto', 'warning'],
  ])('describes price %s against ceiling %s', (price, ceiling, label, tone) => {
    expect(contributionEligibility(price, ceiling)).toEqual({ label, tone })
  })
})