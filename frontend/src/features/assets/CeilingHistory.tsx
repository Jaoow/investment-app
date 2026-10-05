import { useState } from 'react'
import { useCeilingHistory } from '@/shared/api/queries'
import { formatCurrency, formatDateTime } from '@/shared/format/numbers'
import {
  Button,
  Card,
  CardHeader,
  InlineError,
  Skeleton,
} from '@/shared/components/ui'

const actions = {
  SET: 'Preço teto definido',
  REMOVED: 'Preço teto removido',
  BASELINE: 'Valor anterior ao histórico',
}

export function CeilingHistory({
  portfolioId,
  ticker,
}: {
  portfolioId: number | undefined
  ticker: string
}) {
  const [page, setPage] = useState(0)
  const history = useCeilingHistory(portfolioId, ticker, page)
  if (portfolioId === undefined) return null
  return (
    <Card className="ceiling-history-card">
      <CardHeader
        title={`Histórico do teto · ${ticker}`}
        description="Alterações salvas nesta carteira, da mais recente para a mais antiga."
      />
      {history.isLoading && <Skeleton className="allocation-skeleton" />}
      {history.isError && (
        <InlineError
          message={history.error.message}
          onRetry={() => void history.refetch()}
        />
      )}
      {history.data?.content.length === 0 && (
        <p className="form-note">Nenhuma alteração de preço teto registrada.</p>
      )}
      <ol className="ceiling-history-list">
        {history.data?.content.map((entry) => (
          <li key={entry.id}>
            <strong>{actions[entry.action]}</strong>
            <span>
              {entry.previousPrice === null
                ? 'Sem teto anterior'
                : formatCurrency(entry.previousPrice)}{' '}
              →{' '}
              {entry.priceCeiling === null
                ? 'Sem teto'
                : formatCurrency(entry.priceCeiling)}
            </span>
            <time dateTime={entry.changedAt}>
              {formatDateTime(entry.changedAt)}
            </time>
            {entry.action === 'BASELINE' && (
              <small>
                Data da migração; a data original de cadastro não está
                disponível.
              </small>
            )}
          </li>
        ))}
      </ol>
      {history.data && (history.data.page?.totalPages ?? 0) > 1 && (
        <div className="history-pagination">
          <Button
            variant="secondary"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Anterior
          </Button>
          <span>
            Página {page + 1} de {history.data.page?.totalPages}
          </span>
          <Button
            variant="secondary"
            disabled={page + 1 >= (history.data.page?.totalPages ?? 0)}
            onClick={() => setPage(page + 1)}
          >
            Próxima
          </Button>
        </div>
      )}
    </Card>
  )
}
