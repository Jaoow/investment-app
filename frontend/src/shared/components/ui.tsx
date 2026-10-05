import { Slot } from '@radix-ui/react-slot'
import { useId } from 'react'
import type {
  ButtonHTMLAttributes,
  HTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
} from 'react'
import { LoaderCircle } from 'lucide-react'
import * as DialogPrimitive from '@radix-ui/react-dialog'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'default' | 'small' | 'icon'
  asChild?: boolean
  busy?: boolean
}

export function Button({
  variant = 'primary',
  size = 'default',
  asChild,
  busy,
  className = '',
  children,
  disabled,
  ...props
}: ButtonProps) {
  const buttonClass =
    `button button-${variant} button-${size} ${className}`.trim()
  if (asChild) {
    return (
      <Slot
        className={buttonClass}
        aria-disabled={disabled || busy || undefined}
        aria-busy={busy || undefined}
        {...props}
      >
        {children}
      </Slot>
    )
  }
  return (
    <button
      className={buttonClass}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      {...props}
    >
      {busy && <LoaderCircle aria-hidden="true" className="spin" size={16} />}
      {children}
    </button>
  )
}

export function Card({
  children,
  className = '',
  ...props
}: HTMLAttributes<HTMLElement>) {
  return (
    <section className={`card ${className}`.trim()} {...props}>
      {children}
    </section>
  )
}

export function CardHeader({
  title,
  description,
  action,
}: {
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="card-header">
      <div>
        <h2>{title}</h2>
        {description && <p>{description}</p>}
      </div>
      {action}
    </div>
  )
}

export function Input({
  label,
  error,
  hint,
  className = '',
  id,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
  hint?: string
}) {
  const generatedId = useId()
  const inputId = id ?? props.name ?? generatedId
  const hintId = inputId ? `${inputId}-hint` : undefined
  const errorId = inputId ? `${inputId}-error` : undefined
  return (
    <div className="field">
      <label htmlFor={inputId}>{label}</label>
      <input
        id={inputId}
        className={`input ${error ? 'input-error' : ''} ${className}`.trim()}
        aria-invalid={Boolean(error)}
        aria-describedby={
          [hint ? hintId : undefined, error ? errorId : undefined]
            .filter(Boolean)
            .join(' ') || undefined
        }
        {...props}
      />
      {hint && !error && (
        <span id={hintId} className="field-hint">
          {hint}
        </span>
      )}
      {error && (
        <span id={errorId} className="field-error" role="alert">
          {error}
        </span>
      )}
    </div>
  )
}

export function Badge({
  children,
  tone = 'neutral',
}: {
  children: ReactNode
  tone?: 'neutral' | 'positive' | 'negative' | 'warning' | 'info'
}) {
  return <span className={`badge badge-${tone}`}>{children}</span>
}

export function PageHeading({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>
      {action && <div className="page-heading-action">{action}</div>}
    </div>
  )
}

export function InlineError({
  message,
  onRetry,
}: {
  message: string
  onRetry?: () => void
}) {
  return (
    <div className="inline-error" role="alert">
      <div>
        <strong>Não foi possível carregar estes dados.</strong>
        <p>{message}</p>
      </div>
      {onRetry && (
        <Button variant="secondary" size="small" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  )
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon: ReactNode
  title: string
  description: string
  action?: ReactNode
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon" aria-hidden="true">
        {icon}
      </span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`skeleton ${className}`.trim()} aria-hidden="true" />
}

export function FieldLabel({ children }: { children: ReactNode }) {
  return <span className="field-label">{children}</span>
}

export function Dialog({
  open,
  onOpenChange,
  title,
  description,
  children,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="dialog-overlay" />
        <DialogPrimitive.Content className="dialog-content">
          <DialogPrimitive.Title className="dialog-title">
            {title}
          </DialogPrimitive.Title>
          {description && (
            <DialogPrimitive.Description className="dialog-description">
              {description}
            </DialogPrimitive.Description>
          )}
          {children}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}

export function SideSheet({
  open,
  onOpenChange,
  title,
  children,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  children: ReactNode
}) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="dialog-overlay" />
        <DialogPrimitive.Content className="sheet-content">
          <DialogPrimitive.Title className="screen-reader-only">
            {title}
          </DialogPrimitive.Title>
          {children}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
