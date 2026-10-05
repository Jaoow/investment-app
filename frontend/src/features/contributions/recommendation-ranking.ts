import Big from 'big.js'
import type { SuggestionItem } from '@/shared/api/models'

export function rankSuggestions(items: SuggestionItem[]) {
  return [...items].sort((left, right) =>
    new Big(String(right.score)).cmp(String(left.score)),
  )
}