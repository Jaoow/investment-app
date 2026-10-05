# Investize Frontend UX Wireframes

## Architecture and product constraints

The current frontend is a React 19, TypeScript, Vite SPA using React Router,
TanStack Query, shared UI primitives, and CSS variables. Keep that running
architecture for this iteration; a Next.js migration is a separate decision and
does not improve the existing authenticated API workflows by itself. Use live
API responses for product screens. Fixtures are for tests and isolated visual
development only, never a substitute for unavailable portfolio history.

The API currently supports equities, real estate funds, and BDRs. It does not
provide portfolio time series, fixed income/ETF/treasury positions, per-class
contribution eligibility, or projected allocation snapshots. Screens must show
only supported data and explain unavailable capabilities instead of fabricating
values.

## Navigation and flow

```text
Visao geral
  |-- Carteira -> classe -> ativos da classe -> ativo/preco teto
  |-- Metas -> alocacao por classe -> alocacao por ativo
  |-- Aportes -> configuracao de ativos -> simulacao -> revisao informativa
  |-- Configuracoes
```

Use the primary navigation labels Visao geral, Carteira, Metas, Aportes, and
Configuracoes. Movimentacoes and Compartilhamento remain secondary destinations.
The existing routes `/`, `/ativos`, `/alocacao`, `/aporte`, and
`/configuracoes` can be preserved while their visible labels and page roles are
aligned. Asset detail links continue to open the selected ticker and its ceiling
settings.

## Wireframes

### 1. Visao geral

Desktop:

```text
+----------------------+-----------------------------------------------+
| Sidebar              | Portfolio / period context                  |
|                      | Visao geral                  [Planejar aporte]|
|                      +-----------------------------------------------+
|                      | [Patrimonio] [Investido] [Lucro] [Ativos]    |
|                      +------------------------+----------------------+
|                      | Alocacao por classe    | Alocacao por ativo   |
|                      +------------------------+----------------------+
|                      | Evolucao patrimonial: unavailable until API   |
|                      +-----------------------------------------------+
|                      | Busca [____] Classe [Todas]                   |
|                      | Ticker | Nome | Classe | Qtd | Medio | Cotacao |
|                      | Investido | Atual | Peso | Lucro % | Lucro R$  |
|                      | paginacao                                   |
+----------------------+-----------------------------------------------+
```

Mobile:

```text
[Menu] Visao geral
[Patrimonio]
[Investido] [Lucro]
[Ativos]
[Alocacao por classe]
[Alocacao por ativo]
[Busca] [Classe]
[Tabela em rolagem horizontal, com ticker fixo como primeira coluna]
[Paginacao]
```

The allocation charts use positive current market values, disclose their total,
and group small asset positions under `Outros`. The historical chart needs a
portfolio valuation-history API; until then, show a clear unavailable state and
do not derive account history from a single current quote.

### 2. Carteira

Desktop:

```text
Carteira                                      [Selecionar periodo, future]
[Acoes] [FIIs] [ETFs indisponivel] [BDRs] [Renda fixa indisponivel] [...]
Classe selecionada > Acoes
[Valor atual] [Investido] [Resultado] [Rentabilidade]
[Composicao dos ativos da classe]
Ticker | Nome | Qtd | Medio | Cotacao | Atual | Peso na classe | Resultado
```

Mobile:

```text
Carteira
[Classe selecionada v]
[Valor atual] [Investido]
[Resultado] [Rentabilidade]
[Composicao]
[Ativos em lista compacta; abrir detalhes para colunas restantes]
```

Render a class card only when the API can represent it, or mark it unavailable
without an actionable zero balance. Selecting a supported class filters its
summary positions locally; asset detail opens the existing ticker search and
ceiling workflow.

### 3. Metas

Desktop:

```text
Metas / Alocacao                         [Salvar metas]
[Por classe] [Por ativo]
Classe | Atual | Meta editavel | Diferenca | Estado
---------------------------------------------------
Acoes  | 42%   | [35%]         | +7%       | Acima
FIIs   | 25%   | [20%]         | +5%       | Acima
[Total de metas: 100%] [Validacao / erro]
Ativos da classe selecionada
Ticker | Participacao atual | Meta editavel | Barra atual x meta
```

Mobile:

```text
Metas                         [Salvar]
[Classe] [Ativo]
[Classe selecionada v]
Atual 42%   Meta [35%]   Diferenca +7%
[Barra atual x meta]
[Ativo selecionado / meta]
[Total + validacao]
```

Keep class targets as portfolio percentages and asset targets as percentages
inside their class. Show the effective target only where both levels exist.
Saving is explicit; invalid totals remain visible and block submission.

### 4. Aportes

Desktop:

```text
Aportes
+--------------------------------------+-------------------------------+
| Ativos elegiveis e preco teto        | Valor do aporte [________]    |
| Ativo | Elegivel | Cotacao | Teto    | [Gerar sugestao]              |
| Status do teto                       | Fonte e horario da cotacao    |
+--------------------------------------+-------------------------------+
| Valor informado | Alocado | Residual                                 |
| Ranking: score, sinal, valor, unidades, justificativa                    |
| Tabela completa de recomendacoes                                        |
| Carteira atual x projetada: unavailable until API response support       |
| [Revisar simulacao; nao envia ordem]                                    |
```

Mobile:

```text
Aportes
[Elegibilidade por ativo / teto]
[Valor do aporte]
[Gerar sugestao]
[Informado] [Alocado] [Residual]
[Cards de recomendacao ordenados por score]
[Revisar simulacao]
```

The API currently supports per-asset ceiling configuration and an informational
contribution simulation. Class-level eligibility and a post-contribution
portfolio projection require explicit API support; do not imply that review
executes an order or mutates holdings.

## Component structure

```text
AppShell
  PageHeading
  SummaryCard
  AllocationChart
  PortfolioChart (requires historical portfolio API)
  AssetTable (search, class filter, sort, pagination)
  RecommendationCard
  ScoreBadge
  AllocationProgress
  ContributionSimulator
  PortfolioComparison (requires projected summary API)
```

Keep display components independent from fetching. Route pages own query and
mutation state; shared components receive typed values and callbacks. Reuse
existing Card, Input, Button, Badge, EmptyState, InlineError, and Skeleton
primitives before adding new UI dependencies.

## Design system and interaction

- Preserve the existing forest-green and neutral palette, CSS-variable themes,
  compact typography, and light/dark mode preference.
- Use a four-column KPI grid on wide screens, two columns on mobile, and stack
  chart panels and form/table regions below tablet width.
- Keep data-dense tables horizontally scrollable on narrow screens; never hide
  financial values without a path to the full row.
- Use green and red only for positive and negative financial performance. Use
  distinct chart colors for categories, with text labels and percentages in a
  legend so meaning is not color-only.
- Every query has loading, error/retry, empty, and populated states. Mutations
  show pending state and concise success/failure feedback.
- Search, sorting, filters, and pagination are deterministic and operate on
  the currently selected portfolio. Financial arithmetic uses decimal strings
  and `Big.js`, not binary floating-point values.

## Delivery order

1. Complete the overview KPIs and consolidated, searchable/sortable/filtered,
   paginated asset table using the existing portfolio-summary response.
2. Add category navigation and class summaries for API-supported categories.
3. Align allocation comparison visuals with the existing allocation contract.
4. Refine contribution configuration and ranking without inventing class
   eligibility or projected portfolio data.
5. Add portfolio history and projected-summary API contracts before enabling
   the evolution chart and before/after comparison panels.
