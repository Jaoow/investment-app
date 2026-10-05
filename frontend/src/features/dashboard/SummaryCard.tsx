import type { ReactNode } from 'react'
import { Card } from '@/shared/components/ui'

export function SummaryCard({
  label,
  value,
  caption,
  icon,
  valueTone = '',
  iconTone = '',
}: {
  label: string
  value: string
  caption: ReactNode
  icon: ReactNode
  valueTone?: string
  iconTone?: string
}) {
  return (
    <Card className="summary-card">
      <div className="summary-card-heading">
        <span>{label}</span>
        <span className={`summary-icon ${iconTone}`}>{icon}</span>
      </div>
      <strong className={`summary-value ${valueTone}`}>{value}</strong>
      <span className="summary-caption">{caption}</span>
    </Card>
  )
}