import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, publicApi, errorMessage } from './client'
import type {
  ApiPage,
  CategoryAllocation,
  Movement,
  Portfolio,
  PortfolioSummary,
  PublicShare,
  Quote,
  RebalanceItem,
  Share,
  Suggestion,
  Ticker,
  AssetSetting,
  CeilingHistory,
  ContributionEligibility,
} from './models'

function requireData<T>(data: unknown, error: unknown): T {
  if (error) throw new Error(errorMessage(error))
  if (data === undefined || data === null) {
    throw new Error('A API respondeu sem conteúdo.')
  }
  return data as T
}

export const queryKeys = {
  portfolios: ['portfolios'] as const,
  summary: (portfolioId: number) =>
    ['portfolio', portfolioId, 'summary'] as const,
  allocations: (portfolioId: number) =>
    ['portfolio', portfolioId, 'allocations'] as const,
  rebalance: (portfolioId: number) =>
    ['portfolio', portfolioId, 'rebalance'] as const,
  movements: (portfolioId: number, page: number) =>
    ['portfolio', portfolioId, 'movements', page] as const,
  shares: (portfolioId: number) =>
    ['portfolio', portfolioId, 'shares'] as const,
  tickerSearch: (search: string) => ['ticker-search', search] as const,
}

export function usePortfolios() {
  return useQuery({
    queryKey: queryKeys.portfolios,
    queryFn: async () => {
      const { data, error } = await api.GET('/v1/portfolio', {
        params: { query: { page: 0, size: 100 } },
      })
      return requireData<Portfolio[]>(data, error)
    },
  })
}

export function usePortfolioSummary(portfolioId: number | undefined) {
  return useQuery({
    queryKey: portfolioId
      ? queryKeys.summary(portfolioId)
      : ['portfolio', 'none'],
    enabled: portfolioId !== undefined,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/summary',
        {
          params: { path: { portfolioId: portfolioId! } },
        },
      )
      return requireData<PortfolioSummary>(data, error)
    },
  })
}

export function useCreatePortfolio() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (name: string) => {
      const { data, error } = await api.POST('/v1/portfolio', {
        body: { name },
      })
      return requireData<Portfolio>(data, error)
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.portfolios }),
  })
}

export function useSuggestion(portfolioId: number | undefined) {
  return useMutation({
    mutationFn: async (amount: string) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { data, error } = await api.POST(
        '/v1/portfolio/{portfolioId}/suggestions',
        {
          params: { path: { portfolioId } },
          body: { amount, currency: 'BRL' },
        },
      )
      return requireData<Suggestion>(data, error)
    },
  })
}

export function useTickerSearch(search: string) {
  return useQuery({
    queryKey: queryKeys.tickerSearch(search),
    enabled: search.trim().length >= 2,
    queryFn: async () => {
      const { data, error } = await api.GET('/v1/ticker/search', {
        params: { query: { symbol: search, page: 0, size: 20 } },
      })
      const page = requireData<ApiPage<Ticker>>(data, error)
      return page.content ?? []
    },
  })
}

export function useTickerQuote(
  ticker: string,
  options: { enabled?: boolean; refresh?: boolean } = {},
) {
  const enabled = options.enabled ?? true
  const refresh = options.refresh ?? true
  return useQuery({
    queryKey: ['quote', ticker],
    enabled: ticker.length > 0 && enabled,
    queryFn: async () => {
      const { data, error } = await api.GET('/v1/quotes', {
        params: { query: { ticker } },
      })
      return requireData<Quote>(data, error)
    },
    staleTime: 30_000,
    refetchInterval: refresh ? 60_000 : false,
    refetchIntervalInBackground: refresh,
  })
}

export function useAllocations(portfolioId: number | undefined) {
  return useQuery({
    queryKey: portfolioId
      ? queryKeys.allocations(portfolioId)
      : ['allocations', 'none'],
    enabled: portfolioId !== undefined,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/allocations',
        {
          params: { path: { portfolioId: portfolioId! } },
        },
      )
      return requireData<CategoryAllocation[]>(data, error)
    },
  })
}

export function useSaveAllocations(portfolioId: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (allocations: CategoryAllocation[]) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { error, response } = await api.POST(
        '/v1/portfolio/{portfolioId}/allocations',
        {
          params: { path: { portfolioId } },
          body: allocations,
        },
      )
      if (!response.ok) throw new Error(errorMessage(error))
    },
    onSuccess: async () => {
      if (portfolioId !== undefined) {
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: ['portfolio', portfolioId, 'asset-settings'],
          }),
          queryClient.invalidateQueries({
            queryKey: queryKeys.allocations(portfolioId),
          }),
          queryClient.invalidateQueries({
            queryKey: queryKeys.rebalance(portfolioId),
          }),
        ])
      }
    },
  })
}

export function useRebalance(portfolioId: number | undefined) {
  return useQuery({
    queryKey: portfolioId
      ? queryKeys.rebalance(portfolioId)
      : ['rebalance', 'none'],
    enabled: portfolioId !== undefined,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/rebalance',
        {
          params: { path: { portfolioId: portfolioId! } },
        },
      )
      return requireData<RebalanceItem[]>(data, error)
    },
  })
}

export function useMovements(portfolioId: number | undefined, page: number) {
  return useQuery({
    queryKey: portfolioId
      ? queryKeys.movements(portfolioId, page)
      : ['movements', 'none'],
    enabled: portfolioId !== undefined,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/movement',
        {
          params: {
            path: { portfolioId: portfolioId! },
            query: { page, size: 20 },
          },
        },
      )
      return requireData<ApiPage<Movement>>(data, error)
    },
  })
}

export function useCreateMovement(portfolioId: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (movement: Omit<Movement, 'id'>) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { data, error } = await api.POST(
        '/v1/portfolio/{portfolioId}/movement',
        {
          params: { path: { portfolioId } },
          body: movement,
        },
      )
      return requireData<Movement>(data, error)
    },
    onSuccess: () => {
      if (portfolioId !== undefined) {
        queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId] })
      }
    },
  })
}

export function useImportMovements(portfolioId: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (file: File) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { data, error } = await api.POST(
        '/v1/portfolio/{portfolioId}/movement/import',
        {
          params: { path: { portfolioId } },
          body: { file: file.name },
          bodySerializer: (body) => {
            const form = new FormData()
            form.set('file', file, body.file)
            return form
          },
        },
      )
      return requireData<Movement[]>(data, error)
    },
    onSuccess: () => {
      if (portfolioId !== undefined) {
        queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId] })
      }
    },
  })
}

export function useDeleteMovement(portfolioId: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (movementId: number) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { error, response } = await api.DELETE(
        '/v1/portfolio/{portfolioId}/movement/{movementId}',
        { params: { path: { portfolioId, movementId } } },
      )
      if (!response.ok) throw new Error(errorMessage(error))
    },
    onSuccess: () => {
      if (portfolioId !== undefined) {
        queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId] })
      }
    },
  })
}

export function useCeiling(portfolioId: number | undefined, ticker: string) {
  return useQuery({
    queryKey: ['portfolio', portfolioId, 'ceiling', ticker],
    enabled: portfolioId !== undefined && ticker.length > 0,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling',
        {
          params: { path: { portfolioId: portfolioId!, tickerSymbol: ticker } },
        },
      )
      return requireData<{
        tickerSymbol: string
        priceCeiling: string | number
      }>(data, error)
    },
  })
}

export function useSaveCeiling(
  portfolioId: number | undefined,
  ticker: string,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (priceCeiling: string) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { data, error } = await api.PUT(
        '/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling',
        {
          params: { path: { portfolioId, tickerSymbol: ticker } },
          body: { priceCeiling },
        },
      )
      return requireData<{
        tickerSymbol: string
        priceCeiling: string | number
      }>(data, error)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['portfolio', portfolioId, 'ceiling', ticker],
      })
      queryClient.invalidateQueries({
        queryKey: ['portfolio', portfolioId, 'ceiling-history', ticker],
      })
      queryClient.invalidateQueries({
        queryKey: ['portfolio', portfolioId, 'asset-settings'],
      })
    },
  })
}

export function useRegisterTicker() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (values: {
      symbol: string
      category: CategoryAllocation['category']
    }) => {
      const { data, error } = await api.POST('/v1/ticker/register', {
        body: values,
      })
      return requireData<Ticker>(data, error)
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['ticker-search'] }),
  })
}

export function useAssetSettings(portfolioId: number | undefined) {
  return useQuery({
    queryKey: ['portfolio', portfolioId, 'asset-settings'],
    enabled: portfolioId !== undefined,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/asset-settings',
        {
          params: { path: { portfolioId: portfolioId! } },
        },
      )
      return requireData<AssetSetting[]>(data, error)
    },
  })
}

export function useContributionEligibility(portfolioId: number | undefined) {
  return useQuery({
    queryKey: ['portfolio', portfolioId, 'contribution-eligibility'],
    enabled: portfolioId !== undefined,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/contribution-eligibility',
        { params: { path: { portfolioId: portfolioId! } } },
      )
      return requireData<ContributionEligibility>(data, error)
    },
  })
}

export function useSaveContributionConfiguration(portfolioId: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (configuration: {
      eligibility: Record<string, boolean>
      ceilings: Record<string, string | null>
    }) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { data, error } = await api.PUT(
        '/v1/portfolio/{portfolioId}/contribution-eligibility',
        {
          params: { path: { portfolioId } },
          body: { assets: configuration.eligibility },
        },
      )
      requireData<ContributionEligibility>(data, error)

      const ceilingUpdates = await Promise.all(
        Object.entries(configuration.ceilings).map(async ([tickerSymbol, priceCeiling]) => {
          if (priceCeiling === null) {
            const { error: deleteError, response } = await api.DELETE(
              '/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling',
              { params: { path: { portfolioId, tickerSymbol } } },
            )
            if (!response.ok && response.status !== 404) {
              throw new Error(errorMessage(deleteError))
            }
            return
          }
          const { error: saveError, response } = await api.PUT(
            '/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling',
            {
              params: { path: { portfolioId, tickerSymbol } },
              body: { priceCeiling },
            },
          )
          if (!response.ok) throw new Error(errorMessage(saveError))
        }),
      )
      return ceilingUpdates
    },
    onSuccess: async () => {
      if (portfolioId !== undefined) {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId, 'asset-settings'] }),
          queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId, 'contribution-eligibility'] }),
        ])
      }
    },
  })
}

export function useCeilingHistory(
  portfolioId: number | undefined,
  ticker: string,
  page: number,
) {
  return useQuery({
    queryKey: ['portfolio', portfolioId, 'ceiling-history', ticker, page],
    enabled: portfolioId !== undefined && ticker.length > 0,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling/history',
        {
          params: {
            path: { portfolioId: portfolioId!, tickerSymbol: ticker },
            query: { page, size: 10 },
          },
        },
      )
      return requireData<ApiPage<CeilingHistory>>(data, error)
    },
  })
}

export function useShares(portfolioId: number | undefined) {
  return useQuery({
    queryKey: portfolioId ? queryKeys.shares(portfolioId) : ['shares', 'none'],
    enabled: portfolioId !== undefined,
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/v1/portfolio/{portfolioId}/shares',
        {
          params: { path: { portfolioId: portfolioId! } },
        },
      )
      return requireData<Share[]>(data, error)
    },
  })
}

export function useCreateShare(portfolioId: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (values: {
      expiresAt: string
      includeValues: boolean
    }) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { data, error } = await api.POST(
        '/v1/portfolio/{portfolioId}/shares',
        {
          params: { path: { portfolioId } },
          body: values,
        },
      )
      return requireData<{
        id: number
        accessPath: string
        expiresAt: string
        includeValues: boolean
      }>(data, error)
    },
    onSuccess: () => {
      if (portfolioId !== undefined) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.shares(portfolioId),
        })
      }
    },
  })
}

export function useRevokeShare(portfolioId: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (shareId: number) => {
      if (portfolioId === undefined) throw new Error('Selecione uma carteira.')
      const { error, response } = await api.DELETE(
        '/v1/portfolio/{portfolioId}/shares/{shareId}',
        { params: { path: { portfolioId, shareId } } },
      )
      if (!response.ok) throw new Error(errorMessage(error))
    },
    onSuccess: () => {
      if (portfolioId !== undefined) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.shares(portfolioId),
        })
      }
    },
  })
}

export function usePublicShare(token: string | undefined) {
  return useQuery({
    queryKey: ['public-share', token],
    enabled: Boolean(token),
    retry: false,
    queryFn: async () => {
      const { data, error } = await publicApi.GET(
        '/public/portfolio-shares/{token}',
        {
          params: { path: { token: token! } },
        },
      )
      return requireData<PublicShare>(data, error)
    },
  })
}
