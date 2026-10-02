# Plano de continuidade do Investize

> O diagnóstico e plano originais registram a linha de base anterior à implementação. A seção de status abaixo descreve o estado atual; as seções de diagnóstico são históricas e devem ser lidas como evidência do ponto de partida, não como inventário atualizado.

## 1. Resumo executivo

- A API Java 23 / Spring Boot 3.3.1 agora protege operações por proprietário e usa Flyway para schema inicial/versionado.
- Preferências e histórico de preço teto por carteira/ativo, cadastro manual de ticker e balanceamento com `BigDecimal` e unidades inteiras estão implementados.
- O provider brapi v2 fornece timestamp/fonte e cache; quote ausente ou stale bloqueia sugestões, mas não há garantia de tempo real nem integração direta B3.
- `POST /v1/portfolio/{portfolioId}/suggestions` simula aporte com score, quantidades inteiras, residual, motivos e gate de preço teto.
- Compartilhamento read-only com link expirável/revogável existe na API; frontend e rate limiting operacional continuam pendentes.
- Pesos são globais por configuração e independentes dos alvos; pesos por carteira não foram implementados.
- Suite Maven passa com 23 testes; o frontend e sua automação de CI serão versionados em outro repositório.

## Estado atual da implementação

| Capacidade | Estado atual | Evidência / limite |
|---|---|---|
| Autorização por carteira | ✅ Implementada nos casos de uso existentes | Serviços de carteira, alocação, resumo, rebalanceamento e movimentos; testes de isolamento em `src/test/java/dev/jaoow/investmentapp/` |
| Preço teto | ✅ Implementado por carteira/ativo, com histórico | `PortfolioAssetPreferenceController`, service e migrations V2/V4; GET/PUT/DELETE, histórico paginado e visão consolidada `/asset-settings` |
| Cotações | 🟡 Provider brapi v2 com frescor explícito; ao vivo/B3 não garantido | `MarketQuoteProvider`, `BrapiMarketQuoteProvider`, `QuoteService`; cache, fonte e timestamps; termos e disponibilidade de produção ainda precisam ser validados |
| Balanceamento | ✅ Corrigido para posição líquida, cotação e unidades inteiras | `RebalanceService`; alvo efetivo categoria × ativo; percentuais `BigDecimal` |
| Sugestão de aporte | ✅ Simulação implementada na API | `POST /v1/portfolio/{portfolioId}/suggestions`; pontuação, teto obrigatório, unidades inteiras, justificativas, exclusões e residual; não cria movimento nem executa ordem |
| Pesos de recomendação | 🟡 Configuráveis globalmente | Properties independentes das metas; ainda não configuráveis por usuário/carteira |
| Compartilhamento | 🟡 API read-only implementada | Token aleatório, somente hash persistido, expiração/revogação e payload minimizado; UI e rate limiting continuam pendentes |
| Sincronização de custódia B3/corretora | ❌ Não implementada | Não há provedor ou contrato definido; XLSX continua sendo importação manual |
| Interface do fluxo de aporte | ❌ Não encontrada | Repositório contém API backend, sem tela para informar aporte e revisar sugestões |

**Validação atual:** `.\mvnw.cmd test` passou com 23 testes, incluindo isolamento por proprietário, registro de ticker, substituição de metas, histórico de teto e sugestões. Os arquivos de frontend e seu workflow não integram os commits desta API.

## 2. Diagnóstico

### Stack e arquitetura observadas

| Área | Evidência e estado encontrado |
|---|---|
| Stack/build | Maven (`pom.xml`), Java 23, Spring Boot 3.3.1; starters Web, JPA, Validation, Cache e Security. Persistência PostgreSQL; H2 está declarado em runtime. |
| Camadas | `domain/entity`, `domain/model`, `domain/repository`; `application/service`, DTOs, exceptions e regras; `infrastructure/web`, `client`, `config` e `security`. |
| Modelo | `Portfolio` pertence a `User` e possui `AssetMovement`; movimento contém ticker, quantidade, preço histórico, data e `BUY`/`SELL`; `Ticker` contém categoria, setor, subsetor e `priceCeiling`; `CategoryAllocation` e `AssetAllocation` guardam percentuais usando `double`. |
| HTTP | Rotas reais em `/auth`, `/v1/portfolio`, `/v1/portfolio/{portfolioId}/movement`, `/v1/portfolio/{portfolioId}/allocations`, `/v1/portfolio/{portfolioId}/rebalance`, `/v1/ticker` e `/v1/quotes`. Controllers em `src/main/java/dev/jaoow/investmentapp/infrastructure/web/`. |
| Mercado | `BrapiClient` acessa `https://brapi.dev/api/quote/{ticker}`; token vem de `BRAPI_API_TOKEN`. Não encontrei cliente, credencial, endpoint ou sincronização de dados diretamente com a B3. |
| Cache | Caffeine/Spring Cache já está configurado. `CacheConfig` define 15 min; `application.properties` também declara limite e TTL de 30 min. `BrapiClient` usa cache por ticker, sem incluir `range`, `interval`, `fundamental` e `dividends` na chave. |
| Identidade | Cadastro/login e JWT em `AuthController`, `AuthService` e `JwtService`; `SecurityConfig` exige autenticação por padrão e habilita segurança por método. `UserService.assignRoleToUser` exige ADMIN. |
| Interface | Não encontrei aplicação frontend; há apenas `src/main/resources/static/index.html`, sem evidência de telas de produto. |
| Banco/entrega | `docker-compose.yml` sobe PostgreSQL. JPA usa `ddl-auto=update`; não encontrei migrações Flyway/Liquibase, README, ADRs, documentação em `docs/`, configuração CI ou scripts específicos além do Maven wrapper. |
| Testes | `src/test/java/dev/jaoow/investmentapp/InvestmentAppApplicationTests.java` contém somente `contextLoads`; não encontrei testes de regras, API ou segurança. |

### Estado por feature

| Feature do esboço | Estado | Evidência e lacuna |
|---|---|---|
| Cadastro de preço teto | 🟡 Parcial | `Ticker.priceCeiling` é `BigDecimal` e está exposto em `TickerRequest`/`TickerResponse`. Porém é um atributo global do cadastro do ativo, sem configuração por carteira/usuário identificada, regras específicas de validação ou uso no rebalanceamento/recomendação. Operações de escrita em `TickerService` são administrativas. |
| Sincronização com a B3 | ❌ Inexistente | Não encontrei integração B3, importação por conta/custodiante, sincronização de posições ou transação. Importação Excel de movimentações não é sincronização B3: `AssetMovementImportService` e `AssetMovementFileProcessor`. |
| Cotações ao vivo | 🟡 Parcial | `GET /v1/quotes?ticker=...` consulta brapi.dev via `BrapiClient`; não encontrei stream/websocket nem garantia de atualização ao vivo, persistência de preço e horário de coleta ou distinção entre dado atual e defasado. Parâmetros da resposta afetam a consulta, mas não a chave de cache. |
| Balanceamento de carteira/ativos | ⚠️ Existe, mas com problemas | Há cadastro em `POST/GET /v1/portfolio/{portfolioId}/allocations` e consulta em `/rebalance`, com metas por categoria/ativo. Em `RebalanceService`, a quantidade é calculada dividindo o valor pela posição atual da categoria, em vez do preço unitário; `amountToAllocate` compara alocação do ativo com o valor da categoria inteira; o valor total usa preço histórico dos movimentos, não cotação atual. O resultado não representa compra correta em unidades. |
| Compartilhamento de carteira | ❌ Inexistente | `Portfolio` tem dono (`User`); não encontrei associação de leitores, link, token ou política de compartilhamento. |
| Sugestões de compras dado aporte X | ❌ Inexistente | Não encontrei endpoint ou serviço que aceite orçamento, selecione ativos, calcule quantidades inteiras, devolva saldo residual e explique a recomendação. |
| Sistema de pesos de recomendação | ❌ Inexistente | Não encontrei perfil/configuração de recomendação, sinais ou pontuação. As metas de alocação existentes não são pesos de recomendação e devem continuar sendo conceitos separados. |

### Funcionalidades fora do esboço que afetam o plano

- Autenticação JWT, cadastro de usuário, papéis `ROLE_USER`/`ROLE_ADMIN` e autorização por proprietário em parte das operações.
- CRUD de carteiras e ativos, busca/paginação de tickers, categorias/setores, resumo consolidado de carteira e movimentações.
- Movimentações de compra/venda e importação XLSX. Reaproveitar o histórico para calcular posição, mas verificar casos de venda, eventos societários e importações parciais antes de confiar no saldo de ativos.
- O histórico guarda `BigDecimal` para quantidade/preço, base útil para dinheiro; alvos percentuais ainda usam `double`.
- `AssetMovementFileProcessor` ignora linhas inválidas após log e pode concluir importação parcial; `AssetMovementImportService` converte falhas em exceção genérica. Melhorar a apresentação de erros e atomicidade antes de ampliar a ingestão.
- Resumo de carteira e rebalanceamento consultam ticker/posição repetidamente; revisar N+1 e carga de coleções quando a funcionalidade for testada com carteiras maiores.

### Avaliação de qualidade

- **Pontos favoráveis:** camadas discerníveis, DTOs, repositórios Spring Data, `BigDecimal` já usado em preços/quantidades, validação declarativa em algumas requisições, handler global de exceções e BCrypt para senhas.
- **Correção:** balanceamento calcula quantidades com denominador sem unidade de preço e base de valor inconsistente. O retorno omite saldo e explicação. Requer reimplementação coberta por testes antes de reutilizar.
- **Precisão:** `double` em percentuais de alocação; importar célula XLSX via `getNumericCellValue()` e `BigDecimal.valueOf(double)` introduz caminho binário. Usar decimal consistente para percentuais/dinheiro, quantidades inteiras quando o ativo exigir.
- **Validação:** `CategoryAllocationRequest` tem limites numéricos, mas `AllocationController` não aplica `@Valid`; não há validação relacional visível de somas, categorias duplicadas, ticker da categoria, nem teto positivo. DTOs de alocação aceitam `double`.
- **Testes:** somente smoke test de contexto; não há cobertura de regra de negócio, integração com provedor, autorização, importação ou precisão.
- **Erros:** `AuthService.login` captura falha ao gerar JWT e retorna `null`; importação captura exceções amplas e oculta causa/linha inválida. Corrigir respostas explícitas nos pontos tocados.
- **Cache/rede:** TTL declarado diverge (15 vs 30 min), chave omite parâmetros de consulta, `RestTemplate` não exibe timeout configurado e falhas do provedor não têm fallback explícito. A chave de token na query também exige cuidado para que URLs não sejam registradas.
- **Autorização:** `PortfolioService` protege algumas operações e `AssetMovementService` protege operações por dono. Não encontrei checagem equivalente em `AllocationService`, `RebalanceService` ou `PortfolioSummaryService`; seus controllers aceitam apenas `portfolioId`. Como `SecurityConfig` só garante usuário autenticado globalmente, IDs de outra pessoa podem expor/alterar carteira se essas rotas forem acessíveis. Verificar e corrigir antes de expor o motor.
- **Configuração/segredos:** tokens e segredo JWT são injetados por variáveis (`BRAPI_API_TOKEN`, `JWT_SECRET_KEY`), prática adequada; `docker-compose.yml` contém credenciais locais padrão e deve ser limitado ao ambiente de desenvolvimento ou externalizado. `application.properties` usa `ddl-auto=update` e logs SQL/bind em DEBUG/TRACE: não é política segura de produção.
- **Padrões/manutenção:** `AllocationController` recebe `RebalanceService` no construtor sem uso aparente; `AppConfig` tem import duplicado. São débitos pequenos, não justificam reescrita.
- **Performance:** carteira carregada e percorrida em memória em cálculos; consultas a ticker dentro de loops em rebalance/resumo podem escalar mal. Medir com teste de integração e otimizar consultas em conjunto com cálculo correto.
- **CI/docs:** não encontrei workflow de CI, README ou guia de ambiente; a continuidade depende de documentar inicialização, variáveis necessárias e comandos Maven.

## 3. Lacunas, riscos e decisões em aberto

### Lacunas para chegar ao valor central

1. Posições atuais precisam ser calculadas corretamente e valorizadas com cotação rastreável, não preço histórico de aquisição.
2. Falta uma cotação de referência confiável com instante, fonte, moeda, condição de mercado e estado de defasagem.
3. O preço teto é global e não participa de uma decisão de aporte; é preciso definir escopo por carteira/usuário.
4. Os alvos atuais têm semântica ambígua e o cálculo de rebalanceamento está incorreto.
5. Falta perfil de pesos independente dos alvos, pontuação, algoritmo inteiro de distribuição e justificativas retornáveis.
6. Falta UX/API para informar aporte, ajustar proposta e visualizar saldo não alocado.
7. Compartilhamento, sincronização B3 e garantias de preço em tempo real não existem.
8. Há autorização incompleta por carteira, cobertura de testes pequena e política de evolução de schema ausente.

### Riscos e controles propostos

| Risco | Impacto | Controle no plano |
|---|---|---|
| Fonte/licenciamento de mercado | Uma fonte pode fornecer dados atrasados, ter uso restrito/comercial, mudar termos, cobrar, limitar tráfego ou não oferecer o instrumento/campo necessário. O código confirma somente a URL e uso atuais de brapi.dev; não confirma preço, quota, licença, latência, SLA ou contrato. | Antes de adotar em produção, validar documentação/termos/plano do provedor, atraso intraday, timestamps, universo coberto, quota, política de armazenamento/exibição e fallback. Não afirmar “ao vivo” sem comprovação. B3 direta e dados licenciados devem ser avaliados; sincronização de custódia é projeto separado. |
| Disponibilidade/defasagem | Sugestão com preço antigo pode alocar em ativo inadequado. | Guardar fonte, `observedAt`, moeda e estado; TTL configurável; interromper sugestão ou marcar claramente quando não houver cotação fresca conforme política configurada. Não silenciosamente reutilizar cache expirado. |
| Cache | Cache atual pode retornar parâmetros divergentes e envelhecer além do desejado. | Definir uma única configuração e chave completa por ativo/consulta; separar cache de cotação para recomendação do histórico detalhado; métricas e teste de idade. Ajustar TTL após contrato e medições. |
| Dinheiro/quantidades | Arredondamento incorreto muda valor investido e sugere unidades impossíveis. | `BigDecimal` para valores e percentuais; escala/moeda explícitas; `RoundingMode.DOWN` para quantidades inteiras; residual não negativo e explicável. Não considerar custos/taxas sem fonte e regra definida. |
| Importação/posição | XLSX parcial ou eventos não suportados podem causar posição errada. | Testar compras, vendas e posição zero; reporte por linha; transação atômica por padrão; verificação/reconciliação antes de automatizar sincronização. |
| Autorização e vazamento | Endpoints por `portfolioId` podem permitir IDOR; link público pode vazar valores/posições. | Proprietário aplicado em cada caso de uso, testes com usuário A/B e compartilhamento privado por padrão com campos explícitos. |
| Compartilhamento | Link persistente pode circular sem controle. | Link somente leitura, token aleatório de alta entropia armazenado como hash, expiração e revogação imediata; reduzir campos por escopo; não expor e-mail/dados de movimentos sem opt-in. |
| Recomendação e risco regulatório | Usuário interpretar ranking como recomendação profissional/garantia de retorno. | Linguagem de apoio à decisão baseada em regra configurável, exibição da justificativa, data/fonte, riscos e aviso “não constitui consultoria financeira”; validar texto e enquadramento legal com responsável jurídico/regulatório. |
| Complexidade/UX | Pesos configuráveis podem produzir resultado opaco ou surpreendente. | Pontuação simples normalizada, pesos visíveis, prévia dos sinais e razões; configuração inicial padrão e explicação de como alterar; manter balanceamento separado. |

### Decisões abertas (premissas recomendadas)

| Decisão | Opções e trade-offs | Recomendação assumida para o plano |
|---|---|---|
| Fonte para MVP | Manter brapi.dev (menor mudança, contrato/atraso a verificar); contratar fonte licenciada (custo e SLA mais claros, validar preço); cotação manual/importada (simples, sem automação). | Manter brapi.dev atrás de interface `QuoteProvider` apenas para protótipo, sujeito a validação de licença, atraso, campos e limites antes de qualquer lançamento. Sem assumir que seja live. |
| Sincronização B3 | Dados manuais/XLSX; integração com corretora/Open Finance quando disponível; feed de mercado da B3/terceiro. | Separar sincronização de custódia do feed de cotação; primeiro validar formatos/dados e continuar com XLSX, sem credenciais bancárias no MVP. |
| Escopo do teto | Global por `Ticker`; pessoal por usuário; por carteira/ativo. | **Confirmado pelo usuário:** preferência por carteira/ativo para permitir decisões pessoais, mantendo `Ticker.priceCeiling` somente como legado/default durante transição. |
| Semântica de alvos | Percentual de cada ativo sobre carteira toda; ou percentual do ativo dentro da categoria, combinada com percentual da categoria. | Preservar hierarquia existente: categoria é % da carteira, ativo é % dentro da categoria; alvo efetivo do ativo = categoria × ativo. Validar soma de categorias e soma de ativos por categoria (100% para conjunto definido). |
| Elegibilidade sem preço teto | Permitir ativos sem teto, usar apenas outros sinais; excluir até cadastrar teto. | MVP conservador: exige teto positivo para elegibilidade e nunca propõe preço acima do teto. Configuração futura, explícita, pode permitir exceção com confirmação. |
| Frequência e limite de defasagem | Atualização em cada chamada (mais custo/latência); cache configurável; cotações manuais. | Cache configurável, TTL inicial curto como parâmetro operacional (não promessa), condicionado a limites do fornecedor. Ao ultrapassar idade máxima, bloquear sugestão ou indicar claramente indisponibilidade, conforme configuração definida com produto. |
| Precisão de quantidade | Fracionário por ativos/mercados; inteiro como padrão de ações/cotas brasileiras. | Quantidade inteira positiva no primeiro MVP; lotes fracionários exigem modo de negociação explicitamente modelado e testado. |
| Perfil de pesos | Pesos globais fixos; editáveis por usuário/carteira; versões por estratégia. | Defaults documentados e editáveis por carteira numa fase posterior; persistir perfil separado dos alvos. Validar pesos não negativos e normalizar soma. |
| Compartilhamento | Convite para conta identificada; link público; nenhum compartilhamento. | Depois do fluxo principal, link privado, somente leitura, revogável/expirável e com seleção do que mostrar; convite por conta fica como extensão. |
| Persistência/migração | Continuar `ddl-auto=update` (mais simples, risco de schema implícito); adotar ferramenta de migration (histórico reproduzível, exige baseline). | Fazer inventário do schema existente e introduzir migrações versionadas antes de alterações persistentes relevantes. Não executar baseline sem verificar dados de ambientes existentes. |
| Composição do aporte | Alocar só por score; respeitar lacuna de alvo; incorporar custos/impostos. | Começar sem tarifas/impostos (declarar na UI), respeitar alvo como limite/orientação, e manter residual quando orçamento não compra outra unidade. |

## 4. Desenho proposto das features-chave

### 4.1 Preço teto por ativo

- **Modelo:** adicionar `PortfolioAssetPreference` (nome indicativo) associado a `Portfolio` e símbolo; campo `priceCeiling` em `BigDecimal`, moeda e timestamps. Único por `(portfolio_id, ticker_symbol)`. Não confundir teto com `AssetAllocation.targetPercentage` nem com peso de recomendação.
- **Transição:** `Ticker.priceCeiling` existe como valor global. Definir regra de inicialização explícita para os registros atuais (copiar como valor sugerido/default, sem sobrescrever uma escolha pessoal) e depois descontinuar a escrita global para uso do usuário. Fazer migração de schema versionada.
- **Regras:** `tickerSymbol` cadastrado e elegível; valor obrigatório, finito por tipo decimal, `> 0`; moeda compatível com cotação; alterações autorizadas ao proprietário da carteira. `@Valid` no request e validação de regra no serviço.
- **Superfícies:** novo recurso sob `/v1/portfolio/{portfolioId}/assets/{ticker}/preference` (GET/PUT/DELETE) ou equivalente consistente com API; proteger `PortfolioSecurity`. No futuro, formulário de preferência no ativo da carteira.
- **Uso:** selecionar ativo somente quando teto conhecido e `quote.price <= ceiling`, exceto configuração explícita. Exibir `(ceiling - price) / ceiling` como distância positiva abaixo do teto, sem apresentar isso como previsão de retorno.
- **Aceite:** preferência isolada entre duas carteiras do mesmo ticker; valores nulos/zero/negativos recusados; usuário não dono recebe 403/404 sem leitura ou escrita; alteração aparece nos sinais e na explicação da próxima sugestão.

### 4.2 Cotações, histórico e integração

- **Abstração:** interface de aplicação `QuoteProvider` com operação para obter snapshot normalizado. Adaptador brapi em `infrastructure/client/`; serviço de aplicação escolhe provedor/configuração. DTO normalizado inclui símbolo, preço `BigDecimal`, moeda, preço anterior/variação quando suportado, `observedAt`, fetchedAt, nome da fonte e indicador de frescor. Campos não suportados ficam ausentes e não se inferem.
- **Cache:** cache por provedor + símbolo + parâmetros que alteram resposta; apenas cotação snapshot entra no caminho do motor. Configuração central única por ambiente. Telemetria de hit/miss/erro/idade e invalidação. Nunca retornar cotação cacheada sem marcar timestamp/idade.
- **Falhas:** timeouts explícitos, erros de provedor traduzidos a erro de dependência/indisponibilidade; retry limitado apenas a erros transitórios, com backoff; fallback para outro provedor somente após validar contrato/fonte e marcar qual foi usado. Sem fallback de sucesso falso. Uma cotação antiga pode servir para histórico, não necessariamente para nova sugestão.
- **B3:** não chamar de integração B3 nem “tempo real” até confirmar fonte, contrato e licença. Se houver sincronização de posições no futuro, criar adaptador separado de importação de custódia, com autorização, consentimento, reconciliação e trilha de auditoria.
- **Superfícies:** preservar `GET /v1/quotes` durante migração; acrescentar endpoint de snapshot da carteira/ativos ou compor no endpoint de sugestão. Atualizar `BrapiQuoteDto`, `QuoteService`, `BrapiClient`, `CacheConfig` e `application.properties`.
- **Aceite:** teste assegura chave completa, campos/timestamp, stale status, erro e timeout; provedor fake permite testes sem rede; nenhum teste depende de API externa. Operação informa fonte/instante e não chama resposta atrasada de live.

### 4.3 Balanceamento

- **Semântica:** `CategoryAllocation.targetPercentage` = participação da categoria na carteira; `AssetAllocation.targetPercentage` = participação do ativo dentro da categoria; alvo total do ativo = produto dos dois percentuais. Rebalanceamento compara valores de mercado por ativo a estes alvos, não usa custo histórico como valor atual.
- **Posição:** consolidar `BUY` e `SELL` por símbolo em `AssetConsolidationService`; validar posição resultante e quantidade disponível. Valorar por snapshot de cotação atual/fresco; movimentos permanecem como custo e histórico.
- **Validação:** categoria única; categoria entre 0 e 100%; soma de categorias 100% (ou 100% das categorias habilitadas); soma dos ativos da categoria 100%; ticker existe e pertence à categoria. Decidir e validar a regra de ativos sem alvo. Aplicar `@Valid`.
- **Cálculo:** `currentValue_i = position_i × currentQuote_i`; `portfolioMarketValue = sum(currentValue_i)`; `targetValue_i = portfolioMarketValue × categoryTarget × assetTarget / 10000`; `gap_i = targetValue_i - currentValue_i`. Percentuais calculados com `BigDecimal` e arredondamento explícito só no retorno.
- **Superfícies:** corrigir `RebalanceService` e `RebalanceRecommendationResponse`; preservar endpoint `/rebalance` por compatibilidade, incorporar proteção de dono; não misturar score de compra com target allocation.
- **Aceite:** posições 0, abaixo, acima e exatamente no alvo; vendas reduzem posição; falta de quote retorna erro/sinal explícito; quantidades e valores conferem com cálculo manual; recomendações de balanceamento permanecem informacionais até motor de aporte.

### 4.4 Motor de sugestões e pesos

#### Entradas e modelo

- Entrada: `portfolioId`, aporte `X` (decimal > 0), moeda, preferência opcional de perfil/configuração e política explícita de preço acima do teto (default `false`).
- Por ativo: posição atual, cotação atual/fresca, teto, alvo percentual efetivo e dados de mercado opcionais. Fonte e instante são obrigatórios para saber validade.
- Persistir configuração independente, por exemplo `RecommendationProfile`/`RecommendationWeight`: pesos não negativos para `ceilingDistance`, `dailyDrop`, `allocationGap`; thresholds/caps versionados. Não armazenar pesos nos objetos de alocação.
- Resultado por ativo: símbolo, quantidade inteira, preço unitário usado, valor estimado, pontuação e sinais calculados, justificativas legíveis, fonte/horário. Resultado geral: aporte, valor alocado, saldo residual e observação sobre tarifas não incluídas.

#### Sinais e pontuação inicial

Normalizar sinais em `[0,1]`:

- **Abaixo do teto** `T = clamp(((ceiling - price) / ceiling) / 0.30, 0, 1)`. Se preço acima do teto ou teto ausente, o ativo é inelegível no MVP (não somente score zero).
- **Queda diária** `D = clamp(max(0, -dailyChangePct) / 10, 0, 1)`. Só usar se variação for suportada pelo provedor e calculada sobre referência adequada; não disponível significa sinal ausente, não queda zero.
- **Subalocação** `A = clamp(max(0, targetPct - currentPct) / 20pp, 0, 1)`, onde `targetPct` e `currentPct` referem-se ao peso efetivo na carteira, projetando compras do próprio cálculo.
- Pontuação de exemplo: `score = 0.50 × T + 0.20 × D + 0.30 × A`. Pesos são configuração de recomendação e não alteram o alvo. Pesos devem somar 1 após validação/normalização. Se sinal opcional faltar, reponderar somente pelos pesos de sinais disponíveis; teto e cotação continuam requisitos duros.
- Limitar os sinais e expor threshold e valor bruto no retorno para a pontuação ser auditável. Não prometer desempenho preditivo.

**Exemplo numérico:** ticker ABC com teto 100, preço 88, queda diária 3% e subalocação de 8 pontos percentuais. `T = 0.12 / 0.30 = 0.40`; `D = 3/10 = 0.30`; `A = 8/20 = 0.40`; score = `0.50×0.40 + 0.20×0.30 + 0.30×0.40 = 0.38`. A resposta pode explicar: “priorizado: preço 12% abaixo do teto, queda diária de 3% e alocação 8 p.p. abaixo do alvo; score 0,38”. Se a fonte não prover queda diária validada, omitir esse trecho e recalcular com os sinais disponíveis.

#### Distribuição inteira do aporte

1. Validar `X > 0`, moeda, autorização, cotação fresca para cada candidato e qualidade/reconciliação da posição. Se faltar requisito, retornar erro explícito ou excluir candidato com razão verificável; não inventar cotação.
2. Computar posições/valores atuais e score. Excluir ativos sem preferência/teto ou com preço acima do teto, salvo escolha explícita registrada na requisição/configuração.
3. Estimar distribuição proporcional aos scores apenas entre ativos com subalocação positiva, limitando cada alocação ao gap para seu alvo; comprar `floor(valorDisponível / preçoUnitário)` unidades (quantidade inteira no MVP). Score não substitui alvo: se nenhum ativo tiver gap, não comprar automaticamente fora do alvo.
4. Atualizar valores projetados e recalcular subalocação/score. Redistribuir orçamento restante entre os candidatos com maior pontuação atual, respeitando gap restante, teto, orçamento e uma unidade inteira por vez na etapa residual.
5. Encerrar quando nenhum candidato elegível puder receber uma unidade sem exceder saldo/alvo/regra, ou o aporte estiver gasto. Nunca produzir valor alocado maior que X; manter saldo residual, inclusive quando for menor que qualquer unidade disponível. Desempate estável: maior subalocação, depois símbolo. Custos e impostos ficam fora até que haja política validada, e a UI declara essa limitação.
6. Devolver itens ordenados por prioridade, unidades, valor baseado no preço de referência, sinais explicativos, `observedAt`/fonte, saldo residual e razão para candidatos excluídos relevantes.

**Nota de implementação:** primeiro entregar versão determinística e transparente com carteira pequena em mente; estabelecer limites/medir antes de otimizações. Evitar loop não limitado por unidade para aportes muito grandes: usar alocação em blocos até o gap, reavaliando após cada rodada, e limitar a rodada residual ao orçamento restante e conjunto de candidatos.

#### Casos de teste com resultado esperado

Valores em unidade monetária abstrata, sem taxas; alvos/quotes já válidos.

| Caso | Entrada | Esperado |
|---|---|---|
| Uma unidade exata | X=100; ABC quote=100, teto=110; sem outro candidato; saldo e alvos permitem compra | ABC 1 unidade, valor 100, residual 0; nunca excede teto nem aporte. |
| Residual por preço | X=90; ABC quote=100, teto=110; elegível e subalocado | Lista vazia, alocado 0, residual 90 com razão “aporte insuficiente para 1 unidade”. |
| Proteção teto | X=500; ABC quote=101, teto=100 | ABC excluído, quantidade 0, motivo preço acima do teto; alocado 0. |
| Dois ativos e residual | X=150; ABC quote=100 score=.8; XYZ quote=50 score=.6; ambos elegíveis e com gap | ABC 1 + XYZ 1, total 150, residual 0, sob arredondamento e desempate definidos. |
| Sem ultrapassar alvo | X=200; XYZ quote=50; gap de alvo restante=60 | No máximo XYZ 1 unidade/50; nunca compra segunda unidade para além do gap; residual 150. |
| Quote velha | X=200; quote ultrapassa idade máxima configurada | Sem recomendação baseada nessa quote; erro/resultado indisponível explícito com timestamp e motivo. |
| Movimentos líquidos | Compra 5 e venda 2, quote=10 | Posição=3 e currentValue=30; o cálculo de subalocação usa posição líquida. |
| Peso separado | Mesmos alvos, dois perfis de score | Targets não mudam; ordem/pontuação muda conforme pesos; explicações refletem sinais e configuração. |
| Decimal e arredondamento | X=100.00, preço=33.33 | Quantidade inteira 3, valor 99.99, residual 0.01; BigDecimal sem valor acima de X. |
| Entrada inválida | X=0, negativo ou moeda incompatível | Requisição 400 com campo/motivo; nenhum cálculo ou persistência parcial. |

#### API e critérios de aceite

- Endpoint indicativo: `POST /v1/portfolio/{portfolioId}/suggestions`, request com `amount`, `currency` e opções permitidas; protegido por dono da carteira.
- Resposta contém `requestedAmount`, `allocatedAmount`, `remainingAmount`, `items[]`, signals/reasons, quote provenance/freshness e disclaimer.
- Mesma entrada, posições, quotes e configuração produz mesmo resultado e ordenação.
- Soma dos valores sugeridos ≤ aporte; quantidades inteiras positivas; nunca acima do teto por padrão; cada sugestão explica “por quê”; pesos não modificam balanceamento.
- Cliente pode recalcular sem persistir transações. Somente execução/importação explícita cria movimentos, em fluxo separado para não confundir simulação e operação.

### 4.5 Compartilhamento de carteira

- **Modelo:** `PortfolioShare` contendo carteira, hash do token secreto, escopo dos campos (snapshot total, posições/tickers, ocultar valores; configurável), validade, criação, revogação e estado. Identificar autor/criador; nunca armazenar token puro.
- **Permissões:** owner administra/revoga; visitante somente leitura; nenhum acesso a movimentações detalhadas, dados pessoais, e-mail ou alteração por padrão. Links privados não devem indexar em buscadores nem aparecer em logs.
- **Formato:** após motor de sugestões, link aleatório expirável e revogável como MVP; compartilhar para usuário cadastrado pode vir depois. Emissão/revogação exige dono autenticado; leitura pública só por token válido, hash encontrado e não expirado.
- **Superfícies:** criar/listar/revogar em `/v1/portfolio/{portfolioId}/shares`; leitura em endpoint público isolado e DTO de snapshot sem entidade JPA serializada. Rate limit/telemetria e resposta neutra para token inválido.
- **Aceite:** token puro não persiste em banco/log; revogação/expiração bloqueia na requisição seguinte; testes não dono, token expirado, campos omitidos e tentativa de alteração; compartilhar não revela movimentos e dados pessoais sem opt-in.

## 5. Roadmap incremental

As tarefas abaixo são atômicas; os caminhos são prováveis e podem mudar conforme o primeiro teste de integração revelar dependências. A sequência mantém uma versão utilizável em cada etapa.

### Fase 0 — Confiabilidade e proteção das carteiras — ✅ concluída

**Estado:** autorização por proprietário, respostas de erro, testes de isolamento e migrations Flyway foram implementados. A migration V1 presume banco vazio, conforme premissa confirmada pelo dono; não aplicar diretamente em banco legado sem baseline/migração planejada.

**Objetivo/valor:** evitar vazamento entre usuários e ter testes executáveis para cálculo/ownership antes de acrescentar recomendação.  
**Tarefas/dependências:**

1. Confirmar ambiente de execução, schema atual e variáveis obrigatórias; documentar execução/testes em `README.md` (não encontrado) e restringir credenciais locais em `docker-compose.yml`. **P, risco baixo.**
2. Cobrir acesso de dono versus não dono em portfolio, movements, summary, allocations e rebalance; provável `src/test/...` com MockMvc e H2/Testcontainers a decidir. **M, risco alto se falhar; bloqueia recursos novos.**
3. Aplicar a política de dono aos casos de uso em `PortfolioSummaryService`, `AllocationService` e `RebalanceService`, corrigir rota de acesso do portfolio sem sucesso-shaped `null`; verificar cobertura dos controllers. **M, risco médio.**
4. Criar testes unitários de posição líquida BUY/SELL e de alvos atuais antes de corrigir cálculo; testar import parcial separadamente. **M, depende de 2.**
5. Definir migração de schema versionada e baseline depois de inventariar bancos; remover `ddl-auto=update` somente com plano para ambientes existentes. **M, risco médio/alto.**

**Aceite verificável:** usuário A não obtém nem altera carteiras de B por nenhuma rota testada; endpoints próprios seguem funcionando; testes Maven passam com ambiente reproduzível; schema não sofre mudança não versionada.

**Testes:** unitários de segurança/casos de uso, MockMvc com usuários distintos, integração de persistência; executar `./mvnw test` ou `mvnw.cmd test` conforme shell Windows.

### Fase 1 — Posições e cotações confiáveis — 🟡 concluída para o fluxo de recomendações

**Estado:** `MarketQuoteProvider` e adapter brapi v2, cache com chave completa, timestamps/fonte e regra de frescor estão implementados. `RebalanceService` usa posição líquida e preço unitário. Falta validar plano/licença/termos e disponibilidade fora de teste; não há integração direta B3.

**Objetivo/valor:** carteira exibe valor atual baseado em cotação datada, explicitando origem/idade; sem isso não há sugestão confiável.  
**Tarefas/dependências:**

1. Especificar contrato de mercado e verificar documentação, termos/licença, atraso, campo de preço, variação, universo, custo/limites e SLA de brapi.dev ou alternativa; registrar decisão e fallback. **M, bloqueia lançamento público.**
2. Criar modelo normalizado `QuoteSnapshot` e interface `QuoteProvider`; encapsular brapi atual no adaptador existente. Prováveis `application/...` e `infrastructure/client/BrapiClient.java`, DTOs, `QuoteService`. **M.**
3. Configurar timeout e erros tipados; tirar token de logs/URL observável quando suportado pelo contrato; não converter falha de provedor em quote vazia. **M, depende de 2.**
4. Unificar cache, completar chave e idade; expor fonte/timestamp/stale; testar parâmetros e expiração. `CacheConfig`, `application.properties`, `BrapiClient`. **M, depende de 2.**
5. Atualizar resumo de carteira para valorizar posição líquida pelo quote fresco, mantendo custo/preço histórico e retorno separados; decidir resposta quando quote indisponível. `AssetSummaryService`, `PortfolioSummaryService`, DTOs. **G, depende de 1–4.**
6. Adicionar testes do adaptador com servidor HTTP mockado e testes do serviço com `QuoteProvider` fake; sem rede em suite. **M, junto a 2–5.**

**Aceite:** preço tem fonte/instante/moeda; estado stale reproduzível; erro de rede visível; cache não cruza parâmetro/provedor; resumo distingue custo de mercado; suite determinística.

**Testes:** unitários de conversão e idade; integração HTTP mockada; testes de cache; teste de contrato de quote com campos efetivamente autorizados pelo provedor.

### Fase 2 — Preferências de ativos e balanceamento correto — ✅ concluída na API

**Estado:** teto por carteira/ativo, validação de alvos com `BigDecimal` e rebalanceamento de unidades inteiras foram implementados; migrations V2/V3 cobrem preferências e compartilhamento.

**Objetivo/valor:** usuário define teto e meta; visualiza diferença real entre posição e alvo em valores e percentuais.  
**Tarefas/dependências:**

1. Definir semântica e validação de alvo/categoria e estratégia de migração de `Ticker.priceCeiling`; versionar novo modelo de preferência por carteira/ativo. **M, depende de Fase 0.**
2. Criar API e DTOs de preferências de ativo/teto protegidos por dono; formulário somente se o frontend existente for confirmado (não encontrei um app frontend). **M.**
3. Converter percentuais de `double` para `BigDecimal`; adicionar `@Valid` e validação de soma/categoria/ticker. **M, depende de 1.**
4. Corrigir consolidação de movimento líquido e rebalanceamento com quote atual, preço unitário, gap e quantidade coerentes; manter nomes/rota atuais quando compatível. **G, depende da Fase 1 e tarefas 1–3.**
5. Incluir testes de validação, alvos combinados e posições acima/abaixo/zero. **M, em paralelo com 2–4.**

**Aceite:** preferências isoladas por carteira; alvos totalizam segundo regra documentada; rebalance usa preço atual/fresco e valores corretos; movimento SELL reduz posição; autorização por proprietário; endpoint anterior não quebra clientes fora de mudanças incompatíveis.

**Testes:** cálculos com fixtures BigDecimal, MockMvc de validação/ownership, integração repository/migration.

### Fase 3 — Aporte X e motor explicável (MVP central) — 🟡 API implementada; evolução pendente

**Estado:** a API de sugestão está implementada e separa alvo de alocação dos pesos. Os pesos atuais vêm de properties globais, não de um perfil editável por usuário/carteira; a UI de aporte não existe.

**Objetivo/valor:** usuário informa valor e recebe lista de compra (ativo, unidades, preço/valor) com saldo e justificativa.  
**Tarefas/dependências:**

1. Avaliar se pesos globais por properties bastam para MVP ou criar `RecommendationProfile` editável/versionado por usuário/carteira. **M, decisão pendente; defaults atuais são globais.**
2. Continuar validando `SuggestionService` e `RecommendationAllocator` com invariantes e casos de borda; sinais existentes são normalizados, têm gate de teto e razões estruturadas. **M.**
3. Criar request/response e `POST /v1/portfolio/{portfolioId}/suggestions`; exigir dono, quote atual e validação decimal; não persistir movimentação automaticamente. **✅ concluído.**
5. Apresentar UI para simular aporte e editar parâmetros/destacar stale (frontend a localizar/criar em tarefa separada conforme direção do produto). **G, depende de API; não foi identificado framework cliente.**
6. Mostrar disclaimer, premissas e dados usados; revisar conteúdo com responsável jurídico antes de lançamento. **P, depende da primeira tela.**

**Aceite:** casos numéricos da seção 4.4 passam; total sugerido ≤ X; unidades inteiras; residual explicado; teto respeitado; saída explicável, determinística, segura por carteira; falha por dados vencidos explícita; simulação não gera movimento.

**Testes:** alta cobertura unitária de scorer/alocador, property tests para invariantes (sem ultrapassar X/teto/alvo, não negativo), API/auth integration, regressão dos exemplos.

### Fase 4 — Dados de mercado robustos, UX e compartilhamento — 🟡 API de link implementada

**Estado:** compartilhamento somente leitura está implementado na API com token hash, expiração e revogação; validação de termos do provedor, rate limiting e interface continuam pendentes.

**Objetivo/valor:** aumentar confiança e permitir compartilhar uma visão controlada após validar a experiência principal.  
**Tarefas/dependências:**

1. Medir custo/limites/latência e volume; definir TTL, rate limit, observabilidade e alertas. **M, depende da Fase 1/3.**
2. Selecionar provedor licenciado/fallback após due diligence; implementar segundo adaptador apenas se operação/termos justificarem. **G, depende de contrato.**
3. Avaliar integração de custódia/sincronização separadamente; implementar consentimento, reconciliação, idempotência e auditoria somente após decisão de provedor. **G, produto/contrato.**
4. Implementar `PortfolioShare`, token hash, escopo, validade/revogação e endpoint somente leitura. **M, depende de Fase 0 e de decisões de privacidade.**
5. Criar tela de compartilhar e preferências visíveis de privacidade; teste de acesso e logs. **M, depende de 4 e existência/seleção do frontend.**
6. Revisar acessibilidade, linguagem, estados vazios/erro/stale e experiência de recalcular/ajustar aporte. **M, contínua após fase 3.**

**Aceite:** fonte/fallback e termos documentados; compartilhamento só revela campos escolhidos, é revogável e expirável; sincronização não duplica posições; métricas mostram freshness e falhas.

**Testes:** contrato de provider; testes de limites e falha; integração de share com expiração/revogação/IDOR; teste de reconciliação e idempotência para sincronização.

### Trilha paralela de qualidade, encaixada nas entregas

- Fase 0: autorização por proprietário, testes mínimos de regressão, tratamento de erro explícito, guia de execução e CI Maven (validar plataforma do projeto).
- Fase 1: clock injetável, mocks sem rede, observabilidade de provedor/cache e remoção de divergência de TTL.
- Fase 2: validação de entrada/relacional, BigDecimal, consultas em lote para evitar N+1; medir em vez de otimizar prematuramente.
- Fase 3: invariantes/property tests e documentação da fórmula; UI acessível e explicação de cálculo.
- Fase 4: threat model para links, métricas, rate limiting, retenção de dados e revisão legal.
- Ao tocar configuração de produção: migrar logs verbosos para configuração por perfil e tirar credenciais padrão do compose de qualquer ambiente compartilhado.

### Fase 5 — Execução reproduzível e experiência de aporte

**Objetivo:** validar o estado integrado e entregar o fluxo de aporte para uso sem chamadas HTTP manuais.  
**Dependências:** API de aporte e autorização existentes.

| Tarefa atômica | Arquivos prováveis | Dependências | Aceite verificável | Testes | Esforço / risco |
|---|---|---|---|---|---|
| Rodar suite atual, corrigir falhas e `git diff --check` | `src/test/**`, código alterado | Nenhuma | Suite passa, sem erros de whitespace | `mvn test` e wrapper | ✅ concluído |
| Validar Maven Wrapper recém configurado | `.mvn/wrapper/maven-wrapper.properties`, `mvnw.cmd` | Nenhuma | `.\mvnw.cmd test` funciona em Windows | Executado localmente | ✅ concluído |
| Validar primeiro workflow de CI | `.github/workflows/maven.yml` | Wrapper | GitHub Actions usa Temurin 23 e conclui suite | Aguardar push/PR | P / médio |
| Conferir documentação de setup, variáveis e rotas contra código | `README.md`, controllers, `application.properties` | Suite e controllers atuais | Passo a passo reproduzível; sem segredo real; rotas e payloads coincidem com API | Revisão manual e suite | ✅ concluído; revalidar ao mudar rotas |
| Criar tela do fluxo de aporte: formulário X, resultado, motivos, quote/frescor, residual, erro e disclaimer | Não priorizado; API em `SuggestionController` | Decisão futura de interface | Usuário informa aporte e entende o que comprar, quantidade, valor, por quê e limitações | Teste de UI/API e responsivo | Adiado por decisão do dono |
| Criar UI de preço teto, alvos e compartilhamento/revogação | APIs de preferences/allocations/shares | Decisão futura de interface | Dono configura e revoga; link e dados compartilhados apresentados claramente | UI + autorização | Adiado por decisão do dono |

### Fase 6 — Operação segura, dados licenciados e sincronização

**Objetivo:** fechar riscos de produção e só iniciar sincronização depois de escolher fonte e modelo de consentimento.

| Tarefa atômica | Arquivos prováveis | Dependências | Aceite verificável | Testes | Esforço / risco |
|---|---|---|---|---|---|
| Validar com o fornecedor direitos de uso/exibição/cache, plano, limites e confiabilidade da brapi | Configuração de integração e documentação | Acesso ao fornecedor | Decisão documentada; nenhuma alegação de tempo real sem garantia | Teste controlado, sem segredo em logs | M / alto |
| Definir rate limit, observabilidade e comportamento para link/token inválido | `PortfolioShareService`, security/config e métricas | API de share | Volume abusivo limitado; falhas não enumeram links; revogação imediata | Testes de segurança e integração | M / alto |
| Escolher se “sincronizar com B3” significa importar custódia via corretora ou outra integração autorizada | ADR/contrato futuro, integração | Decisão de produto e fornecedor | Escopo, consentimento, fonte e reconciliação aprovados antes do código | Testes contratuais com sandbox oficial | M (decisão), G (implementação) / alto |
| Implementar sincronização idempotente e auditável com a fonte escolhida | Novos adapter/service/migration | Tarefa anterior, contrato e consentimento | Repetição não duplica posições; falha não corrompe a carteira; reconciliação explicável | Sandbox, idempotência, falha parcial | G / alto |
| Revisar aviso legal, privacidade e retenção dos dados | UI e documentação | Produto/jurídico | Aviso não se apresenta como consultoria; campos e retenção de shares são claros | Revisão jurídica e testes de conteúdo | M / alto |

## 6. Premissas assumidas

1. A prioridade é um MVP que responde ao aporte, não uma reescrita nem uma integração de custódia ampla.
2. O backend Java/Spring e PostgreSQL existentes continuam; nova interface `QuoteProvider` e modelo de preferências são extensões locais, não uma troca de stack.
3. Preço teto será pessoal por carteira/ativo; campo global atual serve, no máximo, de valor legado/default até migração confirmada.
4. Alvo hierárquico: categoria como percentual total e ativo como percentual dentro dela; balanceamento e pesos permanecem independentes.
5. Na primeira versão, somente ativos com teto e cotação atual/fresca participam; não compra acima do teto; sem frações; sem tarifas/impostos modelados.
6. A quote usada é referência para simulação, não ordem de execução; não há integração de execução de compra.
7. Brapi é somente a integração observada no código, não uma aprovação de cobertura, preço, disponibilidade, licença ou status “ao vivo”.
8. Compartilhamento começa em somente leitura, com opt-in, token expirável/revogável e campos minimizados.
9. O dono decidiu manter API-only por enquanto; não criar frontend nesta etapa. Interfaces futuras exigem priorização explícita.
10. A linha de base de diagnóstico é histórica e antecede a implementação resumida na seção 2.

## 7. Próximo passo recomendado

**Primeira tarefa: publicar a branch e confirmar a primeira execução do workflow Maven no GitHub Actions.**

1. Fazer push/PR da branch que contém `.github/workflows/maven.yml`.
2. Confirmar execução com Temurin 23; investigar qualquer divergência entre JDK local 25 e ambiente CI 23.
3. Se CI passar, manter API-only e escolher a primeira próxima fatia do backend (due diligence de dados de mercado ou hardening operacional de shares).
4. Antes de migrations em ambiente com dados, fazer inventário, backup e baseline explícito; a migration V1 é para schema vazio.

**Aceite:** workflow hospedado conclui os 16 testes com Java 23; alterações futuras continuam verificadas pelo mesmo comando.  
**Esforço relativo:** P. **Risco:** baixo/médio.

## Decisões ainda a confirmar pelo produto

1. O primeiro produto deve continuar com entrada manual/Excel de movimentações, ou sincronização com corretora/B3 é requisito do MVP?
2. Para compartilhamento posterior, links somente leitura revogáveis são suficientes (premissa recomendada), ou o MVP precisa de convite por conta?
