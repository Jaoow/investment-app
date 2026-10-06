import createClient from 'openapi-fetch'
import type { paths } from './schema'
import { parseApiJson } from './decimal-json'

let accessToken: string | null = null
let unauthorizedHandler: (() => void) | undefined

export class ApiRequestError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly details?: unknown,
  ) {
    super(message)
    this.name = 'ApiRequestError'
  }
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function getAccessToken(): string | null {
  return accessToken
}

export function setUnauthorizedHandler(
  handler: (() => void) | undefined,
): void {
  unauthorizedHandler = handler
}

async function apiFetch(
  request: Request,
  includeAuth: boolean,
): Promise<Response> {
  const headers = new Headers(request.headers)
  if (includeAuth && accessToken)
    headers.set('Authorization', `Bearer ${accessToken}`)
  else headers.delete('Authorization')

  const response = await fetch(new Request(request, { headers }))
  if (
    response.status === 204 ||
    !response.headers.get('content-type')?.includes('application/json')
  ) {
    return response
  }

  const body = await response.text()
  if (!body) {
    const emptyHeaders = new Headers(response.headers)
    emptyHeaders.set('Content-Length', '0')
    return new Response(null, {
      status: response.status,
      statusText: response.statusText,
      headers: emptyHeaders,
    })
  }
  const parsedBody = parseApiJson(body)
  if (!response.ok) {
    const payload =
      typeof parsedBody === 'object' &&
      parsedBody !== null &&
      !Array.isArray(parsedBody)
        ? parsedBody
        : null
    const message =
      payload && typeof payload.details === 'string'
        ? payload.details
        : payload && typeof payload.message === 'string'
          ? payload.message
          : `A solicitação falhou (${response.status}).`
    if (response.status === 401) {
      accessToken = null
      unauthorizedHandler?.()
    }
    throw new ApiRequestError(message, response.status, payload?.details)
  }
  return new Response(JSON.stringify(parsedBody), {
    status: response.status,
    statusText: response.statusText,
    headers: response.headers,
  })
}

export function normalizeApiBaseUrl(configuredBaseUrl: string): string {
  return configuredBaseUrl.replace(/\/+$/, '').replace(/\/v1$/, '')
}

const baseUrl = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL ?? '/api')

export const api = createClient<paths>({
  baseUrl,
  fetch: (request) => apiFetch(request, true),
})
export const publicApi = createClient<paths>({
  baseUrl,
  fetch: (request) => apiFetch(request, false),
})

export interface ApiError {
  message: string
  details?: unknown
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) return error.message
  if (typeof error === 'object' && error !== null && 'message' in error) {
    const message = (error as { message: unknown }).message
    if (typeof message === 'string') return message
  }
  return 'Não foi possível concluir a solicitação. Tente novamente.'
}
