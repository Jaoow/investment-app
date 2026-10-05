import { describe, expect, it } from 'vitest'
import { parseApiJson } from './decimal-json'

describe('lossless API JSON parsing', () => {
  it('preserves decimal values as strings while keeping safe integer identifiers numeric', () => {
    expect(
      parseApiJson('{"id":17,"price":0.10,"amount":9007199254740993.01}'),
    ).toEqual({
      id: 17,
      price: '0.10',
      amount: '9007199254740993.01',
    })
  })

  it('preserves integers outside the safe JavaScript range', () => {
    expect(parseApiJson('{"quantity":9007199254740993}')).toEqual({
      quantity: '9007199254740993',
    })
  })
})
