import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import type { AssetSummary } from '@/shared/api/models'
import { AssetTable } from './AssetTable'

function asset(
  tickerSymbol: string,
  currentValue: number,
  category = 'EQUITIES',
): AssetSummary {
  return {
    tickerSymbol,
    quantity: '10',
    averagePrice: '12.50',
    totalInvested: '125.00',
    currentValue: String(currentValue),
    profitOrLoss: String(currentValue - 125),
    percentageChange: '2.5',
    brapiFields: { longName: `${tickerSymbol} company`, regularMarketPrice: '15' },
    tickerFields: { category },
  }
}

function renderTable(assets: AssetSummary[]) {
  return render(
    <MemoryRouter>
      <AssetTable assets={assets} portfolioValue="1000" />
    </MemoryRouter>,
  )
}

describe('AssetTable', () => {
  afterEach(cleanup)

  it('searches by ticker and filters by class', async () => {
    const user = userEvent.setup()
    renderTable([
      asset('PETR4', 300),
      asset('MXRF11', 200, 'REAL_ESTATE_FUNDS'),
      asset('VALE3', 100),
    ])

    await user.type(screen.getByRole('searchbox', { name: 'Buscar ativos' }), 'MXRF')
    expect(screen.getByRole('link', { name: 'MXRF11' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'PETR4' })).not.toBeInTheDocument()

    await user.clear(screen.getByRole('searchbox', { name: 'Buscar ativos' }))
    await user.selectOptions(screen.getByLabelText('Filtrar por classe'), 'EQUITIES')
    expect(screen.getByRole('link', { name: 'PETR4' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'MXRF11' })).not.toBeInTheDocument()
  })

  it('sorts by ticker and paginates ten rows at a time', async () => {
    const user = userEvent.setup()
    const assets = Array.from({ length: 12 }, (_, index) =>
      asset(`ATIVO${String(index).padStart(2, '0')}`, 100 + index),
    )
    renderTable(assets)

    await user.click(screen.getByRole('button', { name: 'Ordenar por Ticker' }))
    const table = screen.getByRole('table')
    const visibleTickers = within(table)
      .getAllByRole('row')
      .slice(1)
      .map((row) => within(row).getAllByRole('cell')[0]?.textContent)
    expect(visibleTickers[0]).toBe('ATIVO00')

    await user.click(screen.getByRole('button', { name: 'Próxima página' }))
    expect(screen.getByText('11-12 de 12')).toBeInTheDocument()
    expect(within(table).getAllByRole('row')).toHaveLength(3)
  })
})