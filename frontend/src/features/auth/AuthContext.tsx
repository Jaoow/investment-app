import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { AuthContext } from './auth-context'
import type { AuthUser } from './auth-context'
import {
  api,
  errorMessage,
  getAccessToken,
  setAccessToken,
  setUnauthorizedHandler,
} from '@/shared/api/client'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(Boolean(getAccessToken()))

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null)
      setLoading(false)
    })
    return () => setUnauthorizedHandler(undefined)
  }, [])

  const loadCurrentUser = useCallback(async () => {
    const { data, error } = await api.GET('/auth/me')
    if (error || !data) {
      setAccessToken(null)
      setUser(null)
      throw new Error(errorMessage(error))
    }
    const currentUser = data as { name?: unknown; email?: unknown }
    setUser({
      name:
        typeof currentUser.name === 'string' ? currentUser.name : 'Investidor',
      email: typeof currentUser.email === 'string' ? currentUser.email : '',
    })
  }, [])

  const signIn = useCallback(
    async (email: string, password: string) => {
      const { data, error } = await api.POST('/auth/login', {
        body: { email, password },
      })
      if (error || !data?.token) throw new Error(errorMessage(error))

      setAccessToken(data.token)
      try {
        await loadCurrentUser()
      } catch (cause) {
        setAccessToken(null)
        throw cause
      } finally {
        setLoading(false)
      }
    },
    [loadCurrentUser],
  )

  const register = useCallback(
    async (name: string, email: string, password: string) => {
      const { error } = await api.POST('/auth/register', {
        body: { name, email, password },
      })
      if (error) throw new Error(errorMessage(error))
      await signIn(email, password)
    },
    [signIn],
  )

  const signOut = useCallback(() => {
    setAccessToken(null)
    setUser(null)
    setLoading(false)
  }, [])

  const value = useMemo(
    () => ({ user, loading, signIn, register, signOut }),
    [user, loading, signIn, register, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
