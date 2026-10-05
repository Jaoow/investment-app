import { usePortfolio } from '@/app/usePortfolio'
import { usePortfolioSummary } from '@/shared/api/queries'
import { EmptyState } from '@/shared/components/ui'
import { CircleHelp } from 'lucide-react'
import { ContributionConfiguration } from './ContributionConfiguration'

export function ContributionSettingsPage() {
  const { selectedPortfolio, selectedPortfolioId } = usePortfolio()
  const summary = usePortfolioSummary(selectedPortfolioId)

  return (
    selectedPortfolio ? (
      <ContributionConfiguration
        portfolioId={selectedPortfolioId}
        assets={summary.data?.assetSummaries ?? []}
        portfolioLoading={summary.isLoading}
      />
    ) : (
      <EmptyState
        icon={<CircleHelp size={22} />}
        title="Selecione uma carteira"
        description="Escolha uma carteira para consultar as configurações de aporte."
      />
    )
  )
}
