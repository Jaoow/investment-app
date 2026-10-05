import type { DecimalValue } from '@/shared/api/models'
import { presentScore } from './score-presentation'

export function ScoreBadge({ score }: { score: DecimalValue }) {
  const presentation = presentScore(score)
  return (
    <span
      className={`badge badge-${presentation.tone} score-badge`}
      aria-label={`Score ${presentation.points}: ${presentation.label}`}
    >
      <strong>{presentation.points}</strong>
      <span>{presentation.label}</span>
    </span>
  )
}