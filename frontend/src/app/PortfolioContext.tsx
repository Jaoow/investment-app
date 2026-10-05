import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { PortfolioContext } from './portfolio-context'
import { usePortfolios } from '@/shared/api/queries'

export function PortfolioProvider({ children }: { children: ReactNode }) {
  const { data, isLoading, error, refetch } = usePortfolios()
  const portfolios = useMemo(() => data ?? [], [data])
  const [selectedId, setSelectedId] = useState<number | undefined>(() => {
    const stored = sessionStorage.getItem('investize.selectedPortfolio')
    return stored ? Number(stored) : undefined
  })

  useEffect(() => {
    if (portfolios.length === 0) {
      setSelectedId(undefined)
      sessionStorage.removeItem('investize.selectedPortfolio')
      return
    }
    if (
      selectedId === undefined ||
      !portfolios.some((portfolio) => portfolio.id === selectedId)
    ) {
      setSelectedId(portfolios[0]?.id)
      if (portfolios[0]) {
        sessionStorage.setItem(
          'investize.selectedPortfolio',
          String(portfolios[0].id),
        )
      }
    }
  }, [portfolios, selectedId])

  const setSelectedPortfolioId = useCallback((id: number) => {
    setSelectedId(id)
    sessionStorage.setItem('investize.selectedPortfolio', String(id))
  }, [])
  const selectedPortfolio =
    portfolios.find((portfolio) => portfolio.id === selectedId) ?? null
  const refresh = useCallback(() => void refetch(), [refetch])

  const value = useMemo(
    () => ({
      portfolios,
      selectedPortfolio,
      selectedPortfolioId: selectedPortfolio?.id,
      setSelectedPortfolioId,
      loading: isLoading,
      error,
      refresh,
    }),
    [
      portfolios,
      selectedPortfolio,
      setSelectedPortfolioId,
      isLoading,
      error,
      refresh,
    ],
  )

  return (
    <PortfolioContext.Provider value={value}>
      {children}
    </PortfolioContext.Provider>
  )
}
