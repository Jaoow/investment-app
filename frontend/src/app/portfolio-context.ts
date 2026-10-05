import { createContext } from 'react'
import type { Portfolio } from '@/shared/api/models'

export interface PortfolioContextValue {
  portfolios: Portfolio[]
  selectedPortfolio: Portfolio | null
  selectedPortfolioId: number | undefined
  setSelectedPortfolioId: (id: number) => void
  loading: boolean
  error: Error | null
  refresh: () => void
}

export const PortfolioContext = createContext<PortfolioContextValue | null>(
  null,
)
