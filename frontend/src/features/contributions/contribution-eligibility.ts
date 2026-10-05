import Big from 'big.js'
import type { DecimalValue } from '@/shared/api/models'

export type ContributionEligibility = {
  label: string
  tone: 'positive' | 'warning' | 'neutral'
}

export function contributionEligibility(
  currentPrice: DecimalValue | undefined,
  ceiling: DecimalValue | null,
): ContributionEligibility {
  if (ceiling === null) return { label: 'Sem preço teto', tone: 'warning' }
  if (currentPrice === undefined) {
    return { label: 'Cotação indisponível', tone: 'neutral' }
  }
  if (new Big(String(currentPrice)).gt(String(ceiling))) {
    return { label: 'Acima do teto', tone: 'warning' }
  }
  return { label: 'Dentro do teto', tone: 'positive' }
}