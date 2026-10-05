export type DecimalValue = number | string

export interface Portfolio {
  id: number
  name: string
  totalAssets: number
}

export interface AssetSummary {
  tickerSymbol: string
  quantity: DecimalValue
  averagePrice: DecimalValue
  totalInvested: DecimalValue
  currentValue: DecimalValue
  profitOrLoss: DecimalValue
  percentageChange: DecimalValue
  brapiFields?: {
    regularMarketPrice?: DecimalValue
    regularMarketChangePercent?: DecimalValue
    regularMarketTime?: string
    fetchedAt?: string
    currency?: string
    longName?: string
    shortName?: string
  }
  tickerFields?: {
    category?: string
    sector?: string
    subSector?: string
  }
}

export interface PortfolioSummary {
  portfolioId: number
  totalInvested: DecimalValue
  currentValue: DecimalValue
  profitOrLoss: DecimalValue
  percentageChange: DecimalValue
  assetSummaries: AssetSummary[]
}

export interface SuggestionItem {
  tickerSymbol: string
  quantity: DecimalValue
  unitPrice: DecimalValue
  estimatedValue: DecimalValue
  score: DecimalValue
  ceilingPrice?: DecimalValue
  ceilingDistancePercentage?: DecimalValue
  dailyChangePercent?: DecimalValue
  currentPercentage?: DecimalValue
  targetPercentage?: DecimalValue
  reasons: string[]
  quoteProvider: string
  quoteObservedAt: string
}

export interface ExcludedAsset {
  tickerSymbol: string
  reason: string
}

export interface Suggestion {
  requestedAmount: DecimalValue
  allocatedAmount: DecimalValue
  remainingAmount: DecimalValue
  currency: string
  items: SuggestionItem[]
  excludedAssets: ExcludedAsset[]
  disclaimer: string
}

export interface Ticker {
  symbol?: string
  tickerSymbol?: string
  name?: string
  longName?: string
  category?: string
  sector?: string
  subSector?: string
}

export interface Quote {
  symbol?: string
  currency?: string
  regularMarketTime?: string
  fetchedAt?: string
  shortName?: string
  longName?: string
  regularMarketPrice?: DecimalValue
  regularMarketChange?: DecimalValue
  regularMarketChangePercent?: DecimalValue
  regularMarketDayHigh?: DecimalValue
  regularMarketDayLow?: DecimalValue
  regularMarketPreviousClose?: DecimalValue
  historicalDataPrice?: Array<{
    date?: number
    close?: DecimalValue
  }>
}

export interface Movement {
  id: number
  tickerSymbol: string
  quantity: DecimalValue
  price: DecimalValue
  type: 'BUY' | 'SELL'
  date: string
}

export interface Share {
  id: number
  expiresAt: string
  createdAt: string
  revokedAt?: string | null
  includeValues: boolean
}

export interface PublicShare {
  generatedAt: string
  expiresAt: string
  includesValues: boolean
  assets: Array<{
    tickerSymbol: string
    quantity: DecimalValue
    unitPrice?: DecimalValue
    marketValue?: DecimalValue
    currency?: string
    quoteProvider?: string
    quoteObservedAt?: string
  }>
}

export interface CategoryAllocation {
  category: 'EQUITIES' | 'REAL_ESTATE_FUNDS' | 'BDRS'
  categoryTargetPercentage: DecimalValue
  assetAllocations?: Array<{
    tickerSymbol: string
    targetPercentage: DecimalValue
  }>
}

export interface RebalanceItem {
  tickerSymbol: string
  action: 'BUY' | 'SELL'
  quantity: DecimalValue
  currentPercentage: DecimalValue
  targetPercentage: DecimalValue
}

export interface AssetSetting {
  tickerSymbol: string
  category: CategoryAllocation['category']
  categoryTargetPercentage: DecimalValue
  assetTargetPercentage: DecimalValue
  allocationPercentage: DecimalValue
  priceCeiling: DecimalValue | null
}

export interface CeilingHistory {
  id: number
  tickerSymbol: string
  previousPrice: DecimalValue | null
  priceCeiling: DecimalValue | null
  action: 'SET' | 'REMOVED' | 'BASELINE'
  changedAt: string
}

export interface ApiPage<T> {
  content: T[]
  page?: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}
