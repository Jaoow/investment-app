import { expect, test } from '@playwright/test'

const summary = {
  portfolioId: 1,
  totalInvested: 8000.25,
  currentValue: 9100.5,
  profitOrLoss: 1100.25,
  percentageChange: 13.7539,
  assetSummaries: [],
}

async function mockAuthenticatedApi(
  page: import('@playwright/test').Page,
  suggestionHandler: (route: import('@playwright/test').Route) => Promise<void>,
) {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({ json: { token: 'test-access-token' } })
  })
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      json: { id: 7, name: 'Ana Investidora', email: 'ana@example.com' },
    })
  })
  await page.route('**/api/v1/portfolio?*', async (route) => {
    await route.fulfill({
      json: [{ id: 1, name: 'Minha carteira', totalAssets: 0 }],
    })
  })
  await page.route('**/api/v1/portfolio/1/summary', async (route) => {
    await route.fulfill({ json: summary })
  })
  await page.route('**/api/v1/portfolio/1/suggestions', suggestionHandler)
}

async function signIn(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByLabel('E-mail').fill('ana@example.com')
  await page.getByLabel('Senha', { exact: true }).fill('senha-segura')
  await page.getByRole('button', { name: 'Entrar' }).click()
}

test('simula o aporte e confirma a revisão sem registrar movimentação', async ({
  page,
}) => {
  let requestAmount = ''
  let movementCalls = 0

  await mockAuthenticatedApi(page, async (route) => {
    requestAmount = (route.request().postDataJSON() as { amount: string })
      .amount
    await route.fulfill({
      json: {
        requestedAmount: '1234.56',
        allocatedAmount: '1098.00',
        remainingAmount: '136.56',
        currency: 'BRL',
        items: [
          {
            tickerSymbol: 'ITUB4',
            quantity: '20',
            unitPrice: '54.90',
            estimatedValue: '1098.00',
            score: '0.74',
            ceilingPrice: '60.00',
            ceilingDistancePercentage: '8.50',
            dailyChangePercent: '-1.20',
            currentPercentage: '12.00',
            targetPercentage: '20.00',
            reasons: [
              'ITUB4 está 8,50% abaixo do preço teto.',
              'A posição está abaixo da meta.',
            ],
            quoteProvider: 'brapi',
            quoteObservedAt: '2026-10-02T17:30:00Z',
          },
        ],
        excludedAssets: [],
        disclaimer: 'Simulação informativa. Não é consultoria financeira.',
      },
    })
  })
  await page.route('**/api/v1/portfolio/1/movement**', async (route) => {
    movementCalls += 1
    await route.fulfill({ status: 201, json: {} })
  })

  await signIn(page)
  await page.getByRole('link', { name: 'Planejar aporte', exact: true }).click()
  await page.getByLabel('Valor disponível').fill('1.234,56')
  await page.getByRole('button', { name: 'Ver sugestão de aporte' }).click()

  await expect(
    page.getByRole('heading', { name: 'Uma proposta para o seu aporte' }),
  ).toBeVisible()
  await expect(page.getByText('ITUB4', { exact: true }).first()).toBeVisible()
  await expect(page.getByText('R$ 1.098,00').first()).toBeVisible()
  await expect(
    page.getByText('ITUB4 está 8,50% abaixo do preço teto.'),
  ).toBeVisible()
  await expect(page.getByText('R$ 136,56')).toBeVisible()
  expect(requestAmount).toBe('1234.56')

  await page.getByRole('button', { name: 'Revisar sugestão' }).click()
  await expect(page.getByRole('dialog')).toContainText(
    'Nenhuma ordem ou movimentação será criada.',
  )
  await page.getByRole('button', { name: 'Confirmar revisão' }).click()
  await expect(
    page.getByText('Revisão confirmada. Sua carteira não foi alterada.'),
  ).toBeVisible()
  expect(movementCalls).toBe(0)
})

test('não mostra recomendação antiga quando o serviço de mercado retorna 503', async ({
  page,
}) => {
  await mockAuthenticatedApi(page, async (route) => {
    await route.fulfill({
      status: 503,
      json: {
        message: 'Market Data Unavailable',
        details: 'Quotes are stale.',
      },
    })
  })
  await signIn(page)
  await page.getByRole('link', { name: 'Planejar aporte', exact: true }).click()
  await page.getByLabel('Valor disponível').fill('500,00')
  await page.getByRole('button', { name: 'Ver sugestão de aporte' }).click()
  await expect(page.getByRole('alert')).toContainText(
    'cotações necessárias estão indisponíveis',
  )
  await expect(
    page.getByRole('heading', { name: 'Uma proposta para o seu aporte' }),
  ).not.toBeVisible()
})
