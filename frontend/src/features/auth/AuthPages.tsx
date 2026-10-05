import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRight, Eye, EyeOff, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import type { ReactNode } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from './useAuth'
import { Button, Input } from '@/shared/components/ui'

const loginSchema = z.object({
  email: z.email('Informe um e-mail válido.'),
  password: z.string().min(6, 'A senha deve ter pelo menos 6 caracteres.'),
})
const registerSchema = z.object({
  name: z.string().min(3, 'Informe pelo menos 3 caracteres.').max(255),
  email: z.email('Informe um e-mail válido.'),
  password: z
    .string()
    .min(6, 'A senha deve ter pelo menos 6 caracteres.')
    .max(255),
})

type LoginValues = z.infer<typeof loginSchema>
type RegisterValues = z.infer<typeof registerSchema>

function AuthLayout({
  title,
  description,
  children,
}: {
  title: string
  description: string
  children: ReactNode
}) {
  return (
    <main className="auth-page">
      <div className="auth-brand-panel">
        <Link to="/login" className="brand-lockup">
          <span className="brand-mark">i</span>
          <span>investize</span>
        </Link>
        <div className="auth-promise">
          <p className="eyebrow">INVESTIR COM INTENÇÃO</p>
          <h1>Clareza para cada próximo passo.</h1>
          <p>
            Organize sua carteira e transforme o próximo aporte em uma decisão
            mais consciente.
          </p>
        </div>
        <div className="auth-footnote">
          <ShieldCheck size={17} aria-hidden="true" />
          <span>Suas carteiras são privadas e protegidas por conta.</span>
        </div>
      </div>
      <section className="auth-form-panel">
        <div className="auth-form-wrap">
          <p className="eyebrow">BEM-VINDO AO INVESTIZE</p>
          <h2>{title}</h2>
          <p className="auth-description">{description}</p>
          {children}
          <p className="auth-disclaimer">
            O Investize é uma ferramenta informativa e não oferece consultoria
            financeira.
          </p>
        </div>
      </section>
    </main>
  )
}

export function LoginPage() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [showPassword, setShowPassword] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema) })

  if (user) return <Navigate to="/" replace />

  const submit = form.handleSubmit(async ({ email, password }) => {
    setSubmitError(null)
    try {
      await signIn(email, password)
      const from = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(from, { replace: true })
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : 'Confira seu e-mail e senha.',
      )
    }
  })

  return (
    <AuthLayout
      title="Acesse sua conta"
      description="Entre para acompanhar sua carteira e planejar seus aportes."
    >
      <form className="form-stack" onSubmit={submit} noValidate>
        <Input
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@email.com"
          {...form.register('email')}
          error={form.formState.errors.email?.message}
        />
        <div className="password-field">
          <Input
            label="Senha"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Sua senha"
            {...form.register('password')}
            error={form.formState.errors.password?.message}
          />
          <button
            type="button"
            className="password-toggle"
            aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
            onClick={() => setShowPassword((current) => !current)}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
        {submitError && (
          <p className="field-error auth-submit-error" role="alert">
            {submitError}
          </p>
        )}
        <Button
          type="submit"
          busy={form.formState.isSubmitting}
          className="button-full"
        >
          Entrar <ArrowRight size={17} aria-hidden="true" />
        </Button>
      </form>
      <p className="auth-switch">
        Ainda não tem conta? <Link to="/register">Criar conta</Link>
      </p>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const form = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
  })

  if (user) return <Navigate to="/" replace />

  const submit = form.handleSubmit(async (values) => {
    setSubmitError(null)
    try {
      await register(values.name, values.email, values.password)
      navigate('/', { replace: true })
    } catch (error) {
      setSubmitError(
        error instanceof Error
          ? error.message
          : 'Não foi possível criar sua conta.',
      )
    }
  })

  return (
    <AuthLayout
      title="Crie sua conta"
      description="Comece organizando seus investimentos em um só lugar."
    >
      <form className="form-stack" onSubmit={submit} noValidate>
        <Input
          label="Nome"
          autoComplete="name"
          placeholder="Como podemos chamar você?"
          {...form.register('name')}
          error={form.formState.errors.name?.message}
        />
        <Input
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@email.com"
          {...form.register('email')}
          error={form.formState.errors.email?.message}
        />
        <Input
          label="Senha"
          type="password"
          autoComplete="new-password"
          placeholder="Mínimo de 6 caracteres"
          {...form.register('password')}
          error={form.formState.errors.password?.message}
        />
        {submitError && (
          <p className="field-error auth-submit-error" role="alert">
            {submitError}
          </p>
        )}
        <Button
          type="submit"
          busy={form.formState.isSubmitting}
          className="button-full"
        >
          Criar conta <ArrowRight size={17} aria-hidden="true" />
        </Button>
      </form>
      <p className="auth-switch">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </AuthLayout>
  )
}
