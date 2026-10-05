import Big from 'big.js'
import type { DecimalValue } from '@/shared/api/models'

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

function decimalToLocalized(
  value: DecimalValue,
  fractionDigits: number,
): string {
  const rounded = new Big(String(value)).round(fractionDigits, Big.roundHalfUp)
  const [integerPart = '0', fractionPart = ''] = rounded
    .toFixed(fractionDigits)
    .split('.')
  const sign = integerPart.startsWith('-') ? '-' : ''
  const unsignedInteger = sign ? integerPart.slice(1) : integerPart
  const localizedInteger = unsignedInteger.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  const decimalSeparator =
    new Intl.NumberFormat('pt-BR')
      .formatToParts(1.1)
      .find((part) => part.type === 'decimal')?.value ?? ','

  if (fractionDigits === 0) return `${sign}${localizedInteger}`
  return `${sign}${localizedInteger}${decimalSeparator}${fractionPart}`
}

export function formatCurrency(value: DecimalValue, currency = 'BRL'): string {
  if (currency !== 'BRL') {
    const formatted = decimalToLocalized(value, 2)
    return `${currency} ${formatted}`
  }
  const localized = decimalToLocalized(value, 2)
  const parts = currencyFormatter.formatToParts(0)
  const symbol = parts.find((part) => part.type === 'currency')?.value ?? 'R$'
  return `${symbol}\u00a0${localized}`
}

export function formatDecimal(value: DecimalValue, fractionDigits = 2): string {
  return decimalToLocalized(value, fractionDigits)
}

export function formatPercent(value: DecimalValue, fractionDigits = 2): string {
  return `${decimalToLocalized(value, fractionDigits)}%`
}

export function formatDateTime(value?: string): string {
  if (!value) return 'Horário indisponível'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Horário indisponível'
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Sao_Paulo',
  }).format(date)
}

export function isNegative(value: DecimalValue): boolean {
  return new Big(String(value)).lt(0)
}

export function parseLocalizedDecimal(value: string): string | null {
  const trimmed = value.trim().replace(/\s/g, '').replace(/^R\$/, '')
  if (!trimmed || trimmed.startsWith('-')) return null

  let normalized: string
  if (trimmed.includes(',')) {
    if (!/^\d{1,3}(?:\.\d{3})*(?:,\d+)?$|^\d+(?:,\d+)?$/.test(trimmed)) {
      return null
    }
    normalized = trimmed.replace(/\./g, '').replace(',', '.')
  } else {
    if (!/^\d+(?:\.\d+)?$/.test(trimmed)) return null
    normalized = trimmed
  }

  try {
    const decimal = new Big(normalized)
    return decimal.gt(0) ? decimal.toString() : null
  } catch {
    return null
  }
}
