import { ArrowDownLeft, ArrowUpRight, FileUp, Plus, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { toast } from 'sonner'
import { usePortfolio } from '@/app/usePortfolio'
import {
  useCreateMovement,
  useDeleteMovement,
  useImportMovements,
  useMovements,
} from '@/shared/api/queries'
import type { Movement } from '@/shared/api/models'
import { formatCurrency, parseLocalizedDecimal } from '@/shared/format/numbers'
import {
  Badge,
  Button,
  Card,
  CardHeader,
  Dialog,
  EmptyState,
  InlineError,
  Input,
  PageHeading,
  Skeleton,
} from '@/shared/components/ui'

export function MovementsPage() {
  const { selectedPortfolioId } = usePortfolio()
  const [page, setPage] = useState(0)
  const movements = useMovements(selectedPortfolioId, page)
  const createMovement = useCreateMovement(selectedPortfolioId)
  const deleteMovement = useDeleteMovement(selectedPortfolioId)
  const importMovements = useImportMovements(selectedPortfolioId)
  const inputRef = useRef<HTMLInputElement>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [ticker, setTicker] = useState('')
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [type, setType] = useState<'BUY' | 'SELL'>('BUY')
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))
  const [formError, setFormError] = useState<string>()

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const parsedQuantity = parseLocalizedDecimal(quantity)
    const parsedPrice = parseLocalizedDecimal(price)
    if (!ticker.trim() || !parsedQuantity || !parsedPrice) {
      setFormError('Informe ticker, quantidade e preço positivos.')
      return
    }
    if (date > new Date().toISOString().slice(0, 10)) {
      setFormError('A data da operação não pode estar no futuro.')
      return
    }
    setFormError(undefined)
    try {
      await createMovement.mutateAsync({
        tickerSymbol: ticker.trim().toUpperCase(),
        quantity: parsedQuantity,
        price: parsedPrice,
        type,
        date,
      })
      setFormOpen(false)
      setTicker('')
      setQuantity('')
      setPrice('')
      toast.success('Operação registrada na carteira.')
    } catch (error) {
      setFormError(
        error instanceof Error
          ? error.message
          : 'Não foi possível registrar a operação.',
      )
    }
  }

  const importFile = async (file?: File) => {
    if (!file) return
    try {
      const created = await importMovements.mutateAsync(file)
      toast.success(`${created.length} movimentações importadas.`)
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : 'A importação falhou.',
      )
    } finally {
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  const removeMovement = async (movement: Movement) => {
    if (
      !window.confirm(
        `Excluir a operação ${movement.type} de ${movement.tickerSymbol}?`,
      )
    )
      return
    try {
      await deleteMovement.mutateAsync(movement.id)
      toast.success('Operação removida.')
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Não foi possível excluir a operação.',
      )
    }
  }

  return (
    <>
      <PageHeading
        eyebrow="HISTÓRICO DA CARTEIRA"
        title="Movimentações."
        description="Registre compras e vendas para manter posições e sugestões alinhadas."
        action={
          <div className="heading-actions">
            <input
              ref={inputRef}
              type="file"
              accept=".xlsx,.xls"
              className="screen-reader-only"
              aria-label="Selecionar arquivo Excel de movimentações"
              onChange={(event) => void importFile(event.target.files?.[0])}
            />
            <Button
              variant="secondary"
              onClick={() => inputRef.current?.click()}
              busy={importMovements.isPending}
            >
              <FileUp size={16} /> Importar Excel
            </Button>
            <Button onClick={() => setFormOpen(true)}>
              <Plus size={17} /> Registrar operação
            </Button>
          </div>
        }
      />

      <Card className="movement-card">
        <CardHeader
          title="Operações registradas"
          description="Importações são manuais; não há sincronização automática com a B3 ou corretora."
        />
        {movements.isLoading && <Skeleton className="movement-skeleton" />}
        {movements.isError && (
          <InlineError
            message={movements.error.message}
            onRetry={() => void movements.refetch()}
          />
        )}
        {movements.data?.content?.length ? (
          <>
            <div className="movement-table">
              <div className="movement-table-head">
                <span>Ativo</span>
                <span>Tipo</span>
                <span>Quantidade</span>
                <span>Preço unitário</span>
                <span>Data</span>
                <span className="screen-reader-only">Ações</span>
              </div>
              {movements.data.content.map((movement) => (
                <div className="movement-table-row" key={movement.id}>
                  <strong>{movement.tickerSymbol}</strong>
                  <span>
                    <Badge
                      tone={movement.type === 'BUY' ? 'positive' : 'warning'}
                    >
                      {movement.type === 'BUY' ? (
                        <>
                          <ArrowDownLeft size={13} /> Compra
                        </>
                      ) : (
                        <>
                          <ArrowUpRight size={13} /> Venda
                        </>
                      )}
                    </Badge>
                  </span>
                  <span>{movement.quantity}</span>
                  <span>{formatCurrency(movement.price)}</span>
                  <time dateTime={movement.date}>
                    {new Intl.DateTimeFormat('pt-BR').format(
                      new Date(`${movement.date}T12:00:00`),
                    )}
                  </time>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Excluir operação ${movement.tickerSymbol} de ${movement.date}`}
                    onClick={() => void removeMovement(movement)}
                    busy={deleteMovement.isPending}
                  >
                    <Trash2 size={15} />
                  </Button>
                </div>
              ))}
            </div>
            <div className="pagination-actions">
              <Button
                variant="secondary"
                size="small"
                disabled={page === 0}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Anterior
              </Button>
              <span>
                Página {page + 1}
                {movements.data.page?.totalPages
                  ? ` de ${movements.data.page.totalPages}`
                  : ''}
              </span>
              <Button
                variant="secondary"
                size="small"
                disabled={
                  !movements.data.page ||
                  page + 1 >= movements.data.page.totalPages
                }
                onClick={() => setPage((current) => current + 1)}
              >
                Próxima
              </Button>
            </div>
          </>
        ) : !movements.isLoading && !movements.isError ? (
          <EmptyState
            icon={<ArrowDownLeft size={22} />}
            title="Nenhuma operação registrada"
            description="Registre sua primeira compra ou venda para atualizar as posições."
            action={
              <Button onClick={() => setFormOpen(true)}>
                <Plus size={16} /> Registrar operação
              </Button>
            }
          />
        ) : null}
      </Card>

      <Dialog
        open={formOpen}
        onOpenChange={setFormOpen}
        title="Registrar operação"
        description="Este lançamento atualiza o histórico da carteira. Não envia ordem à corretora."
      >
        <form className="form-stack dialog-form" onSubmit={submit} noValidate>
          <Input
            label="Ticker"
            value={ticker}
            onChange={(event) => setTicker(event.target.value.toUpperCase())}
            placeholder="Ex.: ITUB4"
            required
          />
          <label className="field">
            <span>Tipo de operação</span>
            <select
              className="input"
              value={type}
              onChange={(event) =>
                setType(event.target.value as 'BUY' | 'SELL')
              }
            >
              <option value="BUY">Compra</option>
              <option value="SELL">Venda</option>
            </select>
          </label>
          <div className="form-grid-two">
            <Input
              label="Quantidade"
              inputMode="decimal"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              placeholder="10"
              required
            />
            <Input
              label="Preço unitário (BRL)"
              inputMode="decimal"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              placeholder="32,50"
              required
            />
          </div>
          <Input
            label="Data da operação"
            type="date"
            value={date}
            onChange={(event) => setDate(event.target.value)}
            required
          />
          {formError && (
            <p className="field-error" role="alert">
              {formError}
            </p>
          )}
          <div className="dialog-actions">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setFormOpen(false)}
            >
              Cancelar
            </Button>
            <Button type="submit" busy={createMovement.isPending}>
              Registrar
            </Button>
          </div>
        </form>
      </Dialog>
    </>
  )
}
