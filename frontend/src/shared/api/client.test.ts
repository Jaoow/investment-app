import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiRequestError } from './client'

afterEach(() => vi.unstubAllGlobals())

describe('API response handling', () => {
  it('accepts an empty JSON success response without consuming the body twice', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('', {
          status: 201,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    const result = await api.POST('/v1/portfolio/{portfolioId}/allocations', {
      baseUrl: 'http://localhost/api',
      params: { path: { portfolioId: 1 } },
      body: [],
    })
    expect(result.response.status).toBe(201)
    expect(result.error).toBeUndefined()
  })

  it('surfaces descriptive API errors rather than only a generic title', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            message: 'Ticker Not Found',
            details: "Ticker with symbol 'TEST4' not found",
          }),
          { status: 404, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )
    await expect(
      api.GET('/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling', {
        baseUrl: 'http://localhost/api',
        params: { path: { portfolioId: 1, tickerSymbol: 'TEST4' } },
      }),
    ).rejects.toMatchObject({
      name: ApiRequestError.name,
      message: "Ticker with symbol 'TEST4' not found",
      status: 404,
    })
  })
})
