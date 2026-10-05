import { describe, expect, it } from 'vitest'
import {
  formatCurrency,
  formatDateTime,
  formatPercent,
  parseLocalizedDecimal,
} from './numbers'

describe('decimal input and formatting', () => {
  it('normalizes Brazilian decimal input without floating point arithmetic', () => {
    expect(parseLocalizedDecimal('1.234,56')).toBe('1234.56')
    expect(parseLocalizedDecimal('0,01')).toBe('0.01')
    expect(parseLocalizedDecimal('9007199254740993,01')).toBe(
      '9007199254740993.01',
    )
  })

  it('rejects ambiguous, negative, and non-positive amounts', () => {
    expect(parseLocalizedDecimal('1,234.56')).toBeNull()
    expect(parseLocalizedDecimal('-12,00')).toBeNull()
    expect(parseLocalizedDecimal('0,00')).toBeNull()
    expect(parseLocalizedDecimal('')).toBeNull()
  })

  it('formats money and percentages using pt-BR separators', () => {
    expect(formatCurrency('1234.56')).toBe('R$\u00a01.234,56')
    expect(formatPercent('-2.5')).toBe('-2,50%')
  })

  it('formats timestamps in the Brazilian time zone', () => {
    expect(formatDateTime('2026-01-01T15:00:00Z')).toBe('01/01/2026, 12:00')
    expect(formatDateTime('not a date')).toBe('Horário indisponível')
  })
})
