import Big from 'big.js'
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Search,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { AssetSummary, DecimalValue } from '@/shared/api/models'
import { formatCurrency, formatDecimal, formatPercent, isNegative } from '@/shared/format/numbers'
import { Button, Card, CardHeader, EmptyState } from '@/shared/components/ui'

const categoryLabels: Record<string, string> = {
  EQUITIES: 'Ações',
  REAL_ESTATE_FUNDS: 'FIIs',
  BDRS: 'BDRs',
  OTHER: 'Outros',
}

const pageSize = 10

type SortField =
  | 'ticker'
  | 'name'
  | 'category'
  | 'quantity'
  | 'averagePrice'
  | 'quote'
  | 'totalInvested'
  | 'currentValue'
  | 'participation'
  | 'percentageChange'
  | 'profitOrLoss'

interface AssetRow extends AssetSummary {
  name: string
  category: string
  categoryLabel: string
  quote?: DecimalValue
  participation: Big
}

function decimal(value?: DecimalValue): Big {
  return value === undefined ? new Big(0) : new Big(String(value))
}

function compareRows(left: AssetRow, right: AssetRow, field: SortField): number {
  if (field === 'ticker') return left.tickerSymbol.localeCompare(right.tickerSymbol)
  if (field === 'name') return left.name.localeCompare(right.name)
  if (field === 'category') return left.categoryLabel.localeCompare(right.categoryLabel)

  const values: Record<Exclude<SortField, 'ticker' | 'name' | 'category'>, [Big, Big]> = {
    quantity: [decimal(left.quantity), decimal(right.quantity)],
    averagePrice: [decimal(left.averagePrice), decimal(right.averagePrice)],
    quote: [decimal(left.quote), decimal(right.quote)],
    totalInvested: [decimal(left.totalInvested), decimal(right.totalInvested)],
    currentValue: [decimal(left.currentValue), decimal(right.currentValue)],
    participation: [left.participation, right.participation],
    percentageChange: [decimal(left.percentageChange), decimal(right.percentageChange)],
    profitOrLoss: [decimal(left.profitOrLoss), decimal(right.profitOrLoss)],
  }
  return values[field][0].cmp(values[field][1])
}

export function AssetTable({
  assets,
  portfolioValue,
}: {
  assets: AssetSummary[]
  portfolioValue: DecimalValue
}) {
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('ALL')
  const [sortField, setSortField] = useState<SortField>('currentValue')
  const [sortDescending, setSortDescending] = useState(true)
  const [page, setPage] = useState(1)
  const totalValue = decimal(portfolioValue)

  const rows: AssetRow[] = assets.map((asset) => {
    const category = asset.tickerFields?.category ?? 'OTHER'
    return {
      ...asset,
      name:
        asset.brapiFields?.longName ??
        asset.brapiFields?.shortName ??
        asset.tickerSymbol,
      category,
      categoryLabel: categoryLabels[category] ?? 'Outros',
      quote: asset.brapiFields?.regularMarketPrice,
      participation: totalValue.gt(0)
        ? decimal(asset.currentValue).div(totalValue).times(100)
        : new Big(0),
    }
  })

  const query = search.trim().toLocaleLowerCase('pt-BR')
  const filteredRows = rows
    .filter((row) => categoryFilter === 'ALL' || row.category === categoryFilter)
    .filter(
      (row) =>
        !query ||
        `${row.tickerSymbol} ${row.name} ${row.categoryLabel}`
          .toLocaleLowerCase('pt-BR')
          .includes(query),
    )
    .sort((left, right) => {
      const comparison = compareRows(left, right, sortField)
      return sortDescending ? -comparison : comparison
    })

  const pageCount = Math.max(1, Math.ceil(filteredRows.length / pageSize))
  const currentPage = Math.min(page, pageCount)
  const visibleRows = filteredRows.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  )
  const availableCategories = [...new Set(rows.map((row) => row.category))]
    .sort((left, right) => (categoryLabels[left] ?? left).localeCompare(categoryLabels[right] ?? right))

  const sortBy = (field: SortField) => {
    setSortDescending(sortField === field ? !sortDescending : false)
    setSortField(field)
    setPage(1)
  }

  const sortButton = (label: string, field: SortField) => {
    const Icon = sortField !== field ? ArrowUpDown : sortDescending ? ArrowDown : ArrowUp
    return (
      <button
        type="button"
        className="asset-table-sort"
        onClick={() => sortBy(field)}
        aria-label={`Ordenar por ${label}`}
      >
        {label}<Icon size={13} aria-hidden="true" />
      </button>
    )
  }

  return (
    <Card className="asset-table-card">
      <CardHeader
        title="Carteira consolidada"
        description={`${filteredRows.length} de ${assets.length} ativos`}
      />
      <div className="asset-table-controls">
        <label className="asset-table-search">
          <Search size={16} aria-hidden="true" />
          <span className="screen-reader-only">Buscar ativos</span>
          <input
            type="search"
            aria-label="Buscar ativos"
            placeholder="Buscar ticker ou nome"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value)
              setPage(1)
            }}
          />
        </label>
        <label className="asset-table-filter">
          <span>Classe</span>
          <select
            aria-label="Filtrar por classe"
            value={categoryFilter}
            onChange={(event) => {
              setCategoryFilter(event.target.value)
              setPage(1)
            }}
          >
            <option value="ALL">Todas</option>
            {availableCategories.map((category) => (
              <option value={category} key={category}>
                {categoryLabels[category] ?? 'Outros'}
              </option>
            ))}
          </select>
        </label>
      </div>

      {assets.length === 0 ? (
        <EmptyState
          icon={<Search size={22} />}
          title="Sua carteira ainda não tem ativos"
          description="Registre uma operação para acompanhar suas posições aqui."
          action={
            <Button asChild variant="secondary" size="small">
              <Link to="/movimentacoes">Registrar operação</Link>
            </Button>
          }
        />
      ) : filteredRows.length === 0 ? (
        <div className="asset-table-no-results" role="status">
          Nenhum ativo corresponde à busca e ao filtro selecionados.
        </div>
      ) : (
        <>
          <div className="asset-table-scroll">
            <table className="asset-table">
              <thead>
                <tr>
                  <th>{sortButton('Ticker', 'ticker')}</th>
                  <th>{sortButton('Nome', 'name')}</th>
                  <th>{sortButton('Classe', 'category')}</th>
                  <th>{sortButton('Quantidade', 'quantity')}</th>
                  <th>{sortButton('Preço médio', 'averagePrice')}</th>
                  <th>{sortButton('Cotação', 'quote')}</th>
                  <th>{sortButton('Investido', 'totalInvested')}</th>
                  <th>{sortButton('Valor atual', 'currentValue')}</th>
                  <th>{sortButton('Participação', 'participation')}</th>
                  <th>{sortButton('Lucro %', 'percentageChange')}</th>
                  <th>{sortButton('Lucro R$', 'profitOrLoss')}</th>
                </tr>
              </thead>
              <tbody>
                {visibleRows.map((asset) => {
                  const negative = isNegative(asset.profitOrLoss)
                  return (
                    <tr key={asset.tickerSymbol}>
                      <td>
                        <Link to={`/ativos?ticker=${encodeURIComponent(asset.tickerSymbol)}`}>
                          {asset.tickerSymbol}
                        </Link>
                      </td>
                      <td className="asset-table-name" title={asset.name}>{asset.name}</td>
                      <td>{asset.categoryLabel}</td>
                      <td>{formatDecimal(asset.quantity, 4)}</td>
                      <td>{formatCurrency(asset.averagePrice)}</td>
                      <td>{asset.quote === undefined ? '—' : formatCurrency(asset.quote)}</td>
                      <td>{formatCurrency(asset.totalInvested)}</td>
                      <td>{formatCurrency(asset.currentValue)}</td>
                      <td>{formatPercent(asset.participation.toString())}</td>
                      <td className={negative ? 'text-loss' : 'text-gain'}>
                        {formatPercent(asset.percentageChange)}
                      </td>
                      <td className={negative ? 'text-loss' : 'text-gain'}>
                        {formatCurrency(asset.profitOrLoss)}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="asset-table-pagination">
            <span>
              {filteredRows.length === 0
                ? '0 ativos'
                : `${(currentPage - 1) * pageSize + 1}-${Math.min(currentPage * pageSize, filteredRows.length)} de ${filteredRows.length}`}
            </span>
            <div>
              <Button
                variant="secondary"
                size="icon"
                aria-label="Página anterior"
                disabled={currentPage <= 1}
                onClick={() => setPage(currentPage - 1)}
              >
                <ChevronLeft size={16} />
              </Button>
              <span>Página {currentPage} de {pageCount}</span>
              <Button
                variant="secondary"
                size="icon"
                aria-label="Próxima página"
                disabled={currentPage >= pageCount}
                onClick={() => setPage(currentPage + 1)}
              >
                <ChevronRight size={16} />
              </Button>
            </div>
          </div>
        </>
      )}
    </Card>
  )
}