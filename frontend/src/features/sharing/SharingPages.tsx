import { zodResolver } from '@hookform/resolvers/zod'
import {
  Check,
  Copy,
  Link2,
  LockKeyhole,
  ShieldAlert,
  Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { z } from 'zod'
import { usePortfolio } from '@/app/usePortfolio'
import {
  useCreateShare,
  usePublicShare,
  useRevokeShare,
  useShares,
} from '@/shared/api/queries'
import type { Share } from '@/shared/api/models'
import { formatCurrency, formatDateTime } from '@/shared/format/numbers'
import {
  Badge,
  Button,
  Card,
  CardHeader,
  EmptyState,
  InlineError,
  Input,
  PageHeading,
  Skeleton,
} from '@/shared/components/ui'

const shareSchema = z.object({
  expiresAt: z.string().min(1, 'Informe quando o link deve expirar.'),
  includeValues: z.boolean(),
})

type ShareForm = z.infer<typeof shareSchema>

export function SharingPage() {
  const { selectedPortfolio, selectedPortfolioId } = usePortfolio()
  const shares = useShares(selectedPortfolioId)
  const createShare = useCreateShare(selectedPortfolioId)
  const revokeShare = useRevokeShare(selectedPortfolioId)
  const [createdLink, setCreatedLink] = useState<string | null>(null)
  const form = useForm<ShareForm>({
    resolver: zodResolver(shareSchema),
    defaultValues: {
      expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
        .toISOString()
        .slice(0, 16),
      includeValues: false,
    },
  })

  const submit = form.handleSubmit(async (values) => {
    const expiration = new Date(values.expiresAt)
    if (Number.isNaN(expiration.getTime()) || expiration <= new Date()) {
      form.setError('expiresAt', {
        message: 'A expiração precisa estar no futuro.',
      })
      return
    }
    try {
      const result = await createShare.mutateAsync({
        expiresAt: expiration.toISOString(),
        includeValues: values.includeValues,
      })
      setCreatedLink(
        new URL(result.accessPath, window.location.origin).toString(),
      )
      toast.success(
        'Link criado. Copie-o agora: ele não será mostrado novamente.',
      )
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Não foi possível criar o link.',
      )
    }
  })

  const copyLink = async () => {
    if (!createdLink) return
    try {
      await navigator.clipboard.writeText(createdLink)
      toast.success('Link copiado.')
    } catch {
      toast.error(
        'Não foi possível copiar automaticamente. Selecione e copie o link abaixo.',
      )
    }
  }

  const revoke = async (share: Share) => {
    if (share.revokedAt || isExpired(share.expiresAt)) return
    if (
      !window.confirm('Este link deixará de funcionar imediatamente. Revogar?')
    )
      return
    try {
      await revokeShare.mutateAsync(share.id)
      toast.success('Link revogado.')
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : 'Não foi possível revogar o link.',
      )
    }
  }

  return (
    <>
      <PageHeading
        eyebrow="VISUALIZAÇÃO CONTROLADA"
        title="Compartilhe uma visão da carteira."
        description={`Crie links somente leitura${selectedPortfolio ? ` para ${selectedPortfolio.name}` : ''}. Cada link tem validade e pode ser revogado.`}
      />

      <div className="sharing-grid">
        <Card className="share-create-card">
          <CardHeader
            title="Novo link de acesso"
            description="O endereço funciona como uma chave: compartilhe apenas com quem deve ver esta carteira."
          />
          <div className="share-scope-warning">
            <LockKeyhole size={18} aria-hidden="true" />
            <p>
              Por padrão, a página pública mostra apenas ticker e quantidade.
              Valores e cotações ficam ocultos, a menos que você ative a opção
              abaixo.
            </p>
          </div>
          <form className="form-stack" onSubmit={submit} noValidate>
            <Input
              label="Expira em"
              type="datetime-local"
              {...form.register('expiresAt')}
              error={form.formState.errors.expiresAt?.message}
            />
            <label className="checkbox-field">
              <input type="checkbox" {...form.register('includeValues')} />
              <span>
                <strong>Incluir valores e cotações</strong>
                <small>
                  Quem tiver o link poderá ver preços e valores estimados da
                  posição.
                </small>
              </span>
            </label>
            {form.formState.errors.includeValues?.message && (
              <p className="field-error">
                {form.formState.errors.includeValues.message}
              </p>
            )}
            <Button
              type="submit"
              busy={createShare.isPending}
              disabled={!selectedPortfolioId}
            >
              <Link2 size={17} /> Criar link somente leitura
            </Button>
          </form>
          {createdLink && (
            <div className="created-link-panel" role="status">
              <div className="created-link-heading">
                <Check size={17} />
                <strong>Link pronto — copie agora</strong>
              </div>
              <p>
                Por segurança, o link completo não será exibido novamente nesta
                tela.
              </p>
              <div className="created-link-copy">
                <input
                  aria-label="Link público criado"
                  readOnly
                  value={createdLink}
                  onFocus={(event) => event.currentTarget.select()}
                />
                <Button size="small" onClick={() => void copyLink()}>
                  <Copy size={15} /> Copiar
                </Button>
              </div>
            </div>
          )}
        </Card>

        <Card className="share-list-card">
          <CardHeader
            title="Links existentes"
            description="Revogue links que não devem mais dar acesso."
          />
          {shares.isLoading && <Skeleton className="share-skeleton" />}
          {shares.isError && (
            <InlineError
              message={shares.error.message}
              onRetry={() => void shares.refetch()}
            />
          )}
          {shares.data?.length ? (
            <div className="share-list">
              {shares.data.map((share) => {
                const expired = isExpired(share.expiresAt)
                const revoked = Boolean(share.revokedAt)
                return (
                  <div className="share-row" key={share.id}>
                    <span
                      className={`share-row-icon ${revoked || expired ? 'disabled' : ''}`}
                    >
                      <Link2 size={18} />
                    </span>
                    <div className="share-row-main">
                      <strong>
                        Link criado em {formatDateTime(share.createdAt)}
                      </strong>
                      <span>Expira em {formatDateTime(share.expiresAt)}</span>
                      <div className="share-badges">
                        {revoked ? (
                          <Badge tone="negative">Revogado</Badge>
                        ) : expired ? (
                          <Badge tone="warning">Expirado</Badge>
                        ) : (
                          <Badge tone="positive">Ativo</Badge>
                        )}
                        <Badge
                          tone={share.includeValues ? 'warning' : 'neutral'}
                        >
                          {share.includeValues
                            ? 'Inclui valores'
                            : 'Sem valores'}
                        </Badge>
                      </div>
                      {share.revokedAt && (
                        <small>
                          Revogado em {formatDateTime(share.revokedAt)}
                        </small>
                      )}
                    </div>
                    {!revoked && !expired && (
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label="Revogar link"
                        onClick={() => void revoke(share)}
                        busy={revokeShare.isPending}
                      >
                        <Trash2 size={16} />
                      </Button>
                    )}
                  </div>
                )
              })}
            </div>
          ) : !shares.isLoading && !shares.isError ? (
            <EmptyState
              icon={<Link2 size={22} />}
              title="Nenhum link criado"
              description="Os links aparecem aqui para você acompanhar validade e revogar quando quiser."
            />
          ) : null}
        </Card>
      </div>
      <div className="privacy-note">
        <ShieldAlert size={17} />
        <p>
          Não compartilhe o link em locais públicos. O acesso é baseado em posse
          do endereço; a página não identifica o dono nem mostra o histórico de
          movimentações.
        </p>
      </div>
    </>
  )
}

export function PublicSharePage() {
  const { token } = useParams()
  const query = usePublicShare(token)

  return (
    <main className="public-share-page">
      <div className="public-share-header">
        <span className="brand-mark">i</span>
        <strong>investize</strong>
        <Badge tone="info">
          <LockKeyhole size={13} /> Somente leitura
        </Badge>
      </div>
      <div className="public-share-content">
        <PageHeading
          eyebrow="CARTEIRA COMPARTILHADA"
          title="Uma visão de investimentos."
          description="Este retrato é informativo e não constitui recomendação financeira."
        />
        {query.isLoading && <Skeleton className="public-share-skeleton" />}
        {query.isError && (
          <Card>
            <EmptyState
              icon={<ShieldAlert size={23} />}
              title="Este link não está disponível"
              description="Ele pode ter expirado, sido revogado ou não ser válido."
            />
          </Card>
        )}
        {query.data && (
          <>
            <div className="public-share-meta">
              <span>Gerado em {formatDateTime(query.data.generatedAt)}</span>
              <span>Expira em {formatDateTime(query.data.expiresAt)}</span>
            </div>
            {!query.data.assets.length ? (
              <Card>
                <EmptyState
                  icon={<Link2 size={21} />}
                  title="Sem posições"
                  description="Esta carteira não tem posições para exibir."
                />
              </Card>
            ) : (
              <Card className="public-assets-card">
                {query.data.assets.map((asset) => (
                  <div className="public-asset-row" key={asset.tickerSymbol}>
                    <div>
                      <strong>{asset.tickerSymbol}</strong>
                      <span>{asset.quantity} unidades</span>
                    </div>
                    {query.data.includesValues && (
                      <div className="public-asset-values">
                        <strong>
                          {asset.marketValue !== undefined
                            ? formatMoney(asset.marketValue, asset.currency)
                            : '—'}
                        </strong>
                        <span>
                          {asset.quoteProvider ?? 'Fonte não informada'}
                          {asset.quoteObservedAt
                            ? ` · ${formatDateTime(asset.quoteObservedAt)}`
                            : ''}
                        </span>
                      </div>
                    )}
                  </div>
                ))}
              </Card>
            )}
            <p className="public-share-disclaimer">
              Compartilhamento autorizado pelo titular. Os valores são
              estimativas e podem mudar. Investize não oferece consultoria
              financeira.
            </p>
          </>
        )}
      </div>
    </main>
  )
}

function isExpired(value: string): boolean {
  return new Date(value).getTime() <= Date.now()
}

function formatMoney(value: string | number, currency?: string): string {
  return formatCurrency(value, currency ?? 'BRL')
}
