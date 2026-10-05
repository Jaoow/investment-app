import Big from 'big.js'
import type { DecimalValue } from '@/shared/api/models'

export type ScoreTone = 'positive' | 'info' | 'warning' | 'neutral'

export interface ScorePresentation {
  points: string
  label: string
  tone: ScoreTone
}

export function presentScore(score: DecimalValue): ScorePresentation {
  const points = new Big(String(score)).times(100)
  const displayPoints = points.round(0, Big.roundDown).toString()
  if (points.gte(80)) {
    return { points: displayPoints, label: 'Forte compra', tone: 'positive' }
  }
  if (points.gte(60)) {
    return { points: displayPoints, label: 'Compra moderada', tone: 'info' }
  }
  if (points.gte(40)) {
    return { points: displayPoints, label: 'Compra neutra', tone: 'warning' }
  }
  return { points: displayPoints, label: 'Baixa prioridade', tone: 'neutral' }
}