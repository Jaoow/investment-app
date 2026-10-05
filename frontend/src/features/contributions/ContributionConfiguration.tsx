import { useEffect, useMemo, useRef, useState } from 'react'
import { toast } from 'sonner'
import { ArrowLeft, Check, ChevronDown, Save } from 'lucide-react'
import { Link } from 'react-router-dom'
import type {
  AssetSummary,
  CategoryAllocation,
  DecimalValue,
} from '@/shared/api/models'
import {
  useAssetSettings,
  useContributionEligibility,
  useSaveContributionConfiguration,
  useTickerQuote,
} from '@/shared/api/queries'
import {
  formatCurrency,
  parseLocalizedDecimal,
} from '@/shared/format/numbers'
import {
  Badge,
  Button,
  EmptyState,
  InlineError,
  Skeleton,
} from '@/shared/components/ui'
import { contributionEligibility } from './contribution-eligibility'

type CategoryId = CategoryAllocation['category']

const categories: Array<{ id: CategoryId; label: string }> = [
  { id: 'EQUITIES', label: 'Ações' },
  { id: 'REAL_ESTATE_FUNDS', label: 'FIIs' },
  { id: 'BDRS', label: 'BDRs' },
]

interface ContributionAssetRow {
  tickerSymbol: string
  category: CategoryId
  name: string
  price?: DecimalValue
  ceiling: DecimalValue | null
}

function EligibilityCheckbox({
  label,
  checked,
  mixed = false,
  disabled,
  onChange,
}: {
  label: string
  checked: boolean
  mixed?: boolean
  disabled?: boolean
  onChange: () => void
}) {
  const input = useRef<HTMLInputElement>(null)
  useEffect(() => {
    if (input.current) input.current.indeterminate = mixed
  }, [mixed])

  return (
    <input
      ref={input}
      className="contribution-eligibility-checkbox"
      type="checkbox"
      aria-label={label}
      checked={checked}
      disabled={disabled}
      onChange={onChange}
    />
  )
}

function ContributionSettingAsset({
  row,
  enabled,
  ceilingValue,
  disabled,
  onToggle,
  onCeilingChange,
}: {
  row: ContributionAssetRow
  enabled: boolean
  ceilingValue: string
  disabled: boolean
  onToggle: () => void
  onCeilingChange: (value: string) => void
}) {
  const element = useRef<HTMLElement>(null)
  const [isNearViewport, setIsNearViewport] = useState(false)

  useEffect(() => {
    const current = element.current
    if (!current) return
    if (!('IntersectionObserver' in window)) {
      setIsNearViewport(true)
      return
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) {
          setIsNearViewport(true)
          observer.disconnect()
        }
      },
      { rootMargin: '160px' },
    )
    observer.observe(current)
    return () => observer.disconnect()
  }, [])

  const quote = useTickerQuote(row.tickerSymbol, {
    enabled: isNearViewport && row.price === undefined,
    refresh: false,
  })
  const currentPrice = row.price ?? quote.data?.regularMarketPrice
  const status = contributionEligibility(currentPrice, ceilingValue ? parseLocalizedDecimal(ceilingValue) : null)
  const name =
    row.name === row.tickerSymbol
      ? quote.data?.longName ?? quote.data?.shortName ?? row.name
      : row.name

  return (
    <article className="contribution-setting-asset" ref={element}>
      <EligibilityCheckbox
        label={`Incluir ${row.tickerSymbol} na sugestão de aporte`}
        checked={enabled}
        disabled={disabled}
        onChange={onToggle}
      />
      <div className="contribution-setting-main">
        <div className="contribution-setting-identity">
          <span className="asset-avatar" aria-hidden="true">{row.tickerSymbol.slice(0, 1)}</span>
          <span>
            <strong>{row.tickerSymbol}</strong>
            <small>{name}</small>
          </span>
        </div>
        <label className="contribution-ceiling-field">
          <span>Preço teto</span>
          <span className="contribution-ceiling-input">
            <span aria-hidden="true">R$</span>
            <input
              aria-label={`Preço teto de ${row.tickerSymbol}`}
              inputMode="decimal"
              placeholder="Adicionar preço teto"
              value={ceilingValue}
              onChange={(event) => onCeilingChange(event.target.value)}
              disabled={disabled}
            />
          </span>
        </label>
      </div>
      <div className="contribution-setting-quote">
        <strong>
          {currentPrice !== undefined
            ? formatCurrency(currentPrice)
            : quote.isLoading
              ? 'Consultando…'
              : '—'}
        </strong>
        <Badge tone={status.tone}>
          {quote.isError && currentPrice === undefined ? 'Sem cotação atual' : status.label}
        </Badge>
      </div>
    </article>
  )
}

export function ContributionConfiguration({
  portfolioId,
  assets,
  portfolioLoading,
}: {
  portfolioId: number | undefined
  assets: AssetSummary[]
  portfolioLoading: boolean
}) {
  const settings = useAssetSettings(portfolioId)
  const eligibility = useContributionEligibility(portfolioId)
  const save = useSaveContributionConfiguration(portfolioId)
  const [eligibleByTicker, setEligibleByTicker] = useState<Record<string, boolean>>({})
  const [ceilingByTicker, setCeilingByTicker] = useState<Record<string, string>>({})
  const [originalCeilingByTicker, setOriginalCeilingByTicker] = useState<Record<string, string | null>>({})
  const [dirtyCeilings, setDirtyCeilings] = useState<Record<string, boolean>>({})
  const [expandedCategories, setExpandedCategories] = useState<Record<string, boolean>>({
    EQUITIES: true,
    REAL_ESTATE_FUNDS: true,
    BDRS: true,
  })
  const [dirty, setDirty] = useState(false)
  const [validationError, setValidationError] = useState<string | null>(null)

  const rows = useMemo(() => {
    const assetByTicker = new Map(assets.map((asset) => [asset.tickerSymbol, asset]))
    const settingByTicker = new Map(
      (settings.data ?? []).map((setting) => [setting.tickerSymbol, setting]),
    )
    const tickers = new Set([...assetByTicker.keys(), ...settingByTicker.keys()])

    return [...tickers]
      .map((tickerSymbol): ContributionAssetRow | null => {
        const asset = assetByTicker.get(tickerSymbol)
        const setting = settingByTicker.get(tickerSymbol)
        const category = setting?.category ?? asset?.tickerFields?.category
        if (!categories.some((item) => item.id === category)) return null
        return {
          tickerSymbol,
          category: category as CategoryId,
          name:
            asset?.brapiFields?.longName ??
            asset?.brapiFields?.shortName ??
            tickerSymbol,
          price: asset?.brapiFields?.regularMarketPrice,
          ceiling: setting?.priceCeiling ?? null,
        }
      })
      .filter((row): row is ContributionAssetRow => row !== null)
      .sort((left, right) => left.tickerSymbol.localeCompare(right.tickerSymbol))
  }, [assets, settings.data])

  useEffect(() => {
    if (!settings.data || !eligibility.data || dirty) return
    setEligibleByTicker(
      Object.fromEntries(
        rows.map((row) => [
          row.tickerSymbol,
          eligibility.data.assets[row.tickerSymbol] ?? true,
        ]),
      ),
    )
    setCeilingByTicker(
      Object.fromEntries(
        rows.map((row) => [
          row.tickerSymbol,
          row.ceiling === null ? '' : String(row.ceiling).replace('.', ','),
        ]),
      ),
    )
    setOriginalCeilingByTicker(
      Object.fromEntries(
        rows.map((row) => [row.tickerSymbol, row.ceiling === null ? null : String(row.ceiling)]),
      ),
    )
  }, [dirty, eligibility.data, rows, settings.data])

  const isLoading = settings.isLoading || eligibility.isLoading || portfolioLoading
  const isError = settings.isError || eligibility.isError
  const toggleAsset = (tickerSymbol: string) => {
    setEligibleByTicker((current) => ({
      ...current,
      [tickerSymbol]: !(current[tickerSymbol] ?? true),
    }))
    setDirty(true)
  }

  const toggleCategory = (categoryId: CategoryId, categoryRows: ContributionAssetRow[]) => {
    const allEnabled = categoryRows.every((row) => eligibleByTicker[row.tickerSymbol] ?? true)
    setEligibleByTicker((current) => ({
      ...current,
      ...Object.fromEntries(categoryRows.map((row) => [row.tickerSymbol, !allEnabled])),
    }))
    setDirty(true)
    setExpandedCategories((current) => ({ ...current, [categoryId]: true }))
  }

  const updateCeiling = (tickerSymbol: string, value: string) => {
    setCeilingByTicker((current) => ({ ...current, [tickerSymbol]: value }))
    setDirtyCeilings((current) => ({ ...current, [tickerSymbol]: true }))
    setDirty(true)
    setValidationError(null)
  }

  const saveSettings = async () => {
    const ceilings: Record<string, string | null> = {}
    for (const row of rows) {
      if (!dirtyCeilings[row.tickerSymbol]) continue
      const value = ceilingByTicker[row.tickerSymbol]?.trim() ?? ''
      if (value.length === 0) {
        if (originalCeilingByTicker[row.tickerSymbol] !== null) {
          ceilings[row.tickerSymbol] = null
        }
        continue
      }
      const normalized = parseLocalizedDecimal(value)
      if (!normalized) {
        setValidationError(`Informe um preço teto positivo para ${row.tickerSymbol}.`)
        return
      }
      ceilings[row.tickerSymbol] = normalized
    }

    setValidationError(null)
    try {
      await save.mutateAsync({ eligibility: eligibleByTicker, ceilings })
      setDirty(false)
      setDirtyCeilings({})
      toast.success('Seleção e preços teto salvos.')
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Não foi possível salvar as configurações.')
    }
  }

  return (
    <>
      <div className="contribution-settings-heading">
        <Button asChild variant="ghost" size="icon" aria-label="Voltar para aportes">
          <Link to="/aporte"><ArrowLeft size={20} /></Link>
        </Button>
        <h1>Seleção de ativos</h1>
        <Button onClick={() => void saveSettings()} busy={save.isPending} disabled={isLoading || isError || !dirty}>
          <Save size={16} /> Salvar
        </Button>
      </div>

      {isLoading && <Skeleton className="allocation-skeleton" />}
      {settings.isError && (
        <InlineError message={settings.error.message} onRetry={() => void settings.refetch()} />
      )}
      {eligibility.isError && (
        <InlineError message={eligibility.error.message} onRetry={() => void eligibility.refetch()} />
      )}
      {validationError && <InlineError message={validationError} />}
      {!isLoading && !isError && rows.length === 0 && (
        <EmptyState
          icon={<Check size={22} />}
          title="Nenhum ativo nesta carteira"
          description="Registre posições e defina metas para escolher ativos para as sugestões."
        />
      )}

      {!isLoading && !isError && rows.length > 0 && (
        <section className="contribution-class-list" aria-label="Elegibilidade por classe e ativo">
          {categories.map(({ id, label }) => {
            const categoryRows = rows.filter((row) => row.category === id)
            if (categoryRows.length === 0) return null
            const enabledCount = categoryRows.filter((row) => eligibleByTicker[row.tickerSymbol] ?? true).length
            const allEnabled = enabledCount === categoryRows.length
            const expanded = expandedCategories[id] ?? false

            return (
              <section className="contribution-class-group" key={id}>
                <div className="contribution-class-heading">
                  <EligibilityCheckbox
                    label={`Incluir todos os ativos de ${label}`}
                    checked={allEnabled}
                    mixed={enabledCount > 0 && !allEnabled}
                    onChange={() => toggleCategory(id, categoryRows)}
                  />
                  <button
                    type="button"
                    className="contribution-class-toggle"
                    aria-expanded={expanded}
                    onClick={() => setExpandedCategories((current) => ({ ...current, [id]: !expanded }))}
                  >
                    <strong>{label}</strong>
                    <span>{enabledCount}/{categoryRows.length}</span>
                    <ChevronDown className={expanded ? 'expanded' : ''} size={20} />
                  </button>
                </div>
                {expanded && (
                  <div className="contribution-class-assets">
                    {categoryRows.map((row) => {
                      return (
                        <ContributionSettingAsset
                          key={row.tickerSymbol}
                          row={row}
                          enabled={eligibleByTicker[row.tickerSymbol] ?? true}
                          ceilingValue={ceilingByTicker[row.tickerSymbol] ?? ''}
                          disabled={save.isPending}
                          onToggle={() => toggleAsset(row.tickerSymbol)}
                          onCeilingChange={(value) => updateCeiling(row.tickerSymbol, value)}
                        />
                      )
                    })}
                  </div>
                )}
              </section>
            )
          })}
        </section>
      )}
      <p className="contribution-settings-note">
        Um ativo precisa estar selecionado, ter meta de alocação e preço teto para participar. Preços acima do teto são sempre excluídos.
      </p>
    </>
  )
}