import { ArrowRight, CircleDollarSign, SlidersHorizontal } from 'lucide-react'
import { Link } from 'react-router-dom'
import { PageHeading } from '@/shared/components/ui'
import { usePortfolio } from '@/app/usePortfolio'

const contributionActions = [
  {
    to: '/aporte/simular',
    title: 'Sugestão de aporte',
    description:
      'Informe quanto deseja investir e compare uma distribuição por ativo, score e saldo residual.',
    icon: CircleDollarSign,
  },
  {
    to: '/aporte/configuracao',
    title: 'Configuração de ativos e preço teto',
    description:
      'Consulte cotações, confira elegibilidade informativa e edite o preço teto por ativo.',
    icon: SlidersHorizontal,
  },
]

export function ContributionHubPage() {
  const { selectedPortfolio } = usePortfolio()

  return (
    <>
      <PageHeading
        eyebrow={selectedPortfolio?.name ?? 'SUA CARTEIRA'}
        title="Aportes"
        description="Escolha entre planejar uma simulação ou revisar as configurações que orientam o motor."
      />
      <section className="contribution-hub-grid" aria-label="Ações de aporte">
        {contributionActions.map(({ to, title, description, icon: Icon }) => (
          <Link className="contribution-hub-option" to={to} key={to}>
            <span className="contribution-hub-icon" aria-hidden="true">
              <Icon size={21} />
            </span>
            <span className="contribution-hub-copy">
              <strong>{title}</strong>
              <span>{description}</span>
            </span>
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
        ))}
      </section>
    </>
  )
}
