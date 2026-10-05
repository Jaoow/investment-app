import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import type {
  CategoryAllocation,
  CeilingHistory,
} from '../src/shared/api/models'

async function login(page: Page) {
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({ json: { token: 'test-token' } }),
  )
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ json: { name: 'Ana', email: 'ana@example.com' } }),
  )
  await page.route('**/api/v1/portfolio?*', (route) =>
    route.fulfill({ json: [{ id: 1, name: 'Carteira', totalAssets: 0 }] }),
  )
  await page.route('**/api/v1/portfolio/1/summary', (route) =>
    route.fulfill({
      json: {
        portfolioId: 1,
        totalInvested: '0',
        currentValue: '0',
        profitOrLoss: '0',
        percentageChange: '0',
        assetSummaries: [],
      },
    }),
  )
  await page.goto('/login')
  await page.getByLabel('E-mail').fill('ana@example.com')
  await page.getByLabel('Senha', { exact: true }).fill('senha-segura')
  await page.getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(
    page.getByRole('heading', { name: 'Bom ter você por aqui.' }),
  ).toBeVisible()
}

test('cadastra ticker, salva e altera teto, exibe histórico e visão consolidada', async ({
  page,
}) => {
  let registered = false
  let ceiling: string | null = null
  const history: CeilingHistory[] = []
  await page.route('**/api/v1/ticker/search?*', (route) =>
    route.fulfill({
      json: {
        content: registered ? [{ symbol: 'TEST4', category: 'EQUITIES' }] : [],
      },
    }),
  )
  await page.route('**/api/v1/ticker/register', async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      symbol: 'TEST4',
      category: 'EQUITIES',
    })
    registered = true
    await route.fulfill({ json: { symbol: 'TEST4', category: 'EQUITIES' } })
  })
  await page.route('**/api/v1/quotes?*', (route) =>
    route.fulfill({ status: 503, json: { message: 'Cotações indisponíveis' } }),
  )
  await page.route('**/api/v1/portfolio/1/asset-settings', (route) =>
    route.fulfill({
      json: ceiling
        ? [
            {
              tickerSymbol: 'TEST4',
              category: 'EQUITIES',
              allocationPercentage: '0',
              priceCeiling: ceiling,
            },
          ]
        : [],
    }),
  )
  await page.route(
    '**/api/v1/portfolio/1/assets/TEST4/ceiling',
    async (route) => {
      if (route.request().method() === 'PUT') {
        expect(registered).toBe(true)
        const price = (
          route.request().postDataJSON() as { priceCeiling: string }
        ).priceCeiling
        history.unshift({
          id: history.length + 1,
          tickerSymbol: 'TEST4',
          previousPrice: ceiling,
          priceCeiling: price,
          action: 'SET',
          changedAt: new Date().toISOString(),
        })
        ceiling = price
        await route.fulfill({
          json: { tickerSymbol: 'TEST4', priceCeiling: ceiling },
        })
      } else {
        await route.fulfill(
          ceiling
            ? { json: { tickerSymbol: 'TEST4', priceCeiling: ceiling } }
            : { status: 404, json: { message: 'Sem teto' } },
        )
      }
    },
  )
  await page.route(
    '**/api/v1/portfolio/1/assets/TEST4/ceiling/history?*',
    (route) =>
      route.fulfill({
        json: {
          content: history,
          page: { totalPages: 1, totalElements: history.length },
        },
      }),
  )
  await login(page)
  await page.getByRole('link', { name: 'Ativos', exact: true }).click()
  await page
    .getByRole('textbox', { name: 'Buscar ativo pelo ticker' })
    .fill('TEST4')
  await page.getByText('Cadastrar ticker no catálogo', { exact: true }).click()
  await page.getByLabel('Categoria do ticker').selectOption('EQUITIES')
  await page
    .getByRole('button', { name: 'Cadastrar ticker', exact: true })
    .click()
  await page.getByLabel('Preço teto (BRL)').fill('32,50')
  await page.getByRole('button', { name: 'Salvar preço teto' }).click()
  const historyCard = page.locator('section').filter({
    has: page.getByRole('heading', { name: 'Histórico do teto · TEST4' }),
  })
  await expect(
    historyCard.getByText('Sem teto anterior → R$ 32,50'),
  ).toBeVisible()
  await page.getByLabel('Preço teto (BRL)').fill('35,25')
  await page.getByRole('button', { name: 'Salvar preço teto' }).click()
  await expect(historyCard.getByText('R$ 32,50 → R$ 35,25')).toBeVisible()
  const overview = page.locator('section').filter({
    has: page.getByRole('heading', { name: 'Tickers, metas e preço teto' }),
  })
  await expect(overview.getByText('R$ 35,25')).toBeVisible()
  await expect(
    overview.getByRole('button', { name: 'Editar TEST4' }),
  ).toBeVisible()
  expect(history).toHaveLength(2)
})

test('salva metas fracionárias repetidamente e não perde rascunho ao atualizar dados', async ({
  page,
}) => {
  let allocations: CategoryAllocation[] = [
    {
      category: 'EQUITIES',
      categoryTargetPercentage: '60',
      assetAllocations: [
        { tickerSymbol: 'TEST4', targetPercentage: '25' },
        { tickerSymbol: 'TEST3', targetPercentage: '75' },
      ],
    },
    {
      category: 'REAL_ESTATE_FUNDS',
      categoryTargetPercentage: '40',
      assetAllocations: [{ tickerSymbol: 'TEST11', targetPercentage: '100' }],
    },
    { category: 'BDRS', categoryTargetPercentage: '0', assetAllocations: [] },
  ]
  let saves = 0
  await page.route('**/api/v1/portfolio/1/allocations', async (route) => {
    if (route.request().method() === 'POST') {
      allocations = route.request().postDataJSON() as CategoryAllocation[]
      saves++
      await route.fulfill({
        status: 201,
        body: '',
        headers: { 'content-type': 'application/json' },
      })
    } else {
      await route.fulfill({ json: allocations })
    }
  })
  await page.route('**/api/v1/portfolio/1/rebalance', (route) =>
    route.fulfill({ json: [] }),
  )
  await page.route('**/api/v1/portfolio/1/asset-settings', (route) =>
    route.fulfill({ json: [] }),
  )
  await login(page)
  await page.getByRole('link', { name: 'Metas', exact: true }).click()
  const equities = page.getByLabel('Meta de Ações (%)', { exact: true })
  await equities.fill('62,50')
  await page
    .getByLabel('Meta de Fundos imobiliários (%)', { exact: true })
    .fill('37,50')
  await page.getByLabel('Meta de TEST4 (%)').fill('20,25')
  await page.getByLabel('Meta de TEST3 (%)').fill('79,75')
  await expect(page.getByLabel('Meta de TEST4 (%)')).toHaveValue('20,25')
  // A focus-triggered server refetch must not replace the unsaved editor state.
  await page.evaluate(() => window.dispatchEvent(new Event('visibilitychange')))
  await expect(equities).toHaveValue('62,50')
  await page
    .getByRole('button', { name: 'Salvar metas', exact: true })
    .first()
    .click()
  await expect(
    page.getByText('Metas de alocação salvas.', { exact: true }),
  ).toBeVisible()
  expect(allocations[0].categoryTargetPercentage).toBe('62.50')
  expect(allocations[0].assetAllocations?.[0].targetPercentage).toBe('20.25')
  await equities.fill('60,00')
  await page
    .getByLabel('Meta de Fundos imobiliários (%)', { exact: true })
    .fill('40,00')
  await page
    .getByRole('button', { name: 'Salvar metas', exact: true })
    .first()
    .click()
  await expect(equities).toHaveValue('60.00')
  expect(saves).toBe(2)
  await page.reload()
  await login(page)
  await page.getByRole('link', { name: 'Metas', exact: true }).click()
  await expect(
    page.getByLabel('Meta de Ações (%)', { exact: true }),
  ).toHaveValue('60.00')
})
