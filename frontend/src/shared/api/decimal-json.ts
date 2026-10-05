import { LosslessNumber, parse } from 'lossless-json'

type JsonValue =
  null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue }

function normalizeLosslessNumbers(value: unknown): JsonValue {
  if (value instanceof LosslessNumber) {
    const raw = value.toString()
    if (/[.eE]/.test(raw)) return raw

    const integer = Number(raw)
    return Number.isSafeInteger(integer) ? integer : raw
  }

  if (Array.isArray(value)) return value.map(normalizeLosslessNumbers)

  if (value !== null && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [
        key,
        normalizeLosslessNumbers(entry),
      ]),
    )
  }

  if (
    value === null ||
    typeof value === 'string' ||
    typeof value === 'number' ||
    typeof value === 'boolean'
  ) {
    return value
  }

  throw new TypeError('Unexpected value in API JSON response.')
}

export function parseApiJson(text: string): JsonValue {
  return normalizeLosslessNumbers(parse(text))
}
