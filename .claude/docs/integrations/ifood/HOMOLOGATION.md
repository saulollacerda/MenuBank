# Homologação iFood — Critérios por Módulo

Este documento consolida os critérios de homologação exigidos pelo iFood para cada módulo da integração. Deve ser usado como checklist antes de qualquer solicitação de homologação junto ao iFood.

## Índice

- [Módulo Order](#módulo-order)
- [Módulo Catalog](#módulo-catalog)
- [Módulo Merchant](#módulo-merchant)

---

## Módulo Order

### Pré-requisitos

- Conta Profissional: registrada com CNPJ válido.
- Aplicação integrada: código em produção testado localmente contra a sandbox.
- Credenciais de teste: Client ID e Client Secret gerados no Portal do Desenvolvedor.
- Loja de testes: uma loja configurada no ambiente de teste iFood.
- Conectividade: rede estável com acesso aos endpoints da API.

### Requisitos funcionais

#### Recebimento e confirmação de pedidos

- Consumo de eventos: implementar polling em `/events:polling` (recomendado a cada 30 segundos) ou processar webhooks.
- Reconhecimento de eventos: confirmar o recebimento de todos os eventos via `POST /acknowledgment` para evitar reprocessamento.
- Confirmação de pedidos: confirmar pedidos dentro do SLA máximo de 8 minutos para pedidos `DELIVERY` e `TAKEOUT`, independente do `orderTiming` (imediato ou agendado).

#### Cancelamento e motivos

- Exibição de motivos: consultar `GET /cancellationReasons` e exibir os motivos disponíveis ao usuário.
- Processamento: processar solicitações de cancelamento iniciadas pelo cliente ou pela plataforma.
- Resposta: retornar status apropriado (aceito, rejeitado) via endpoint de cancelamento.

#### Informações de pagamento e pedido

- Pagamento: exibir a bandeira do cartão utilizado e calcular corretamente o troco em dinheiro quando aplicável.
- Cupons e descontos: exibir o valor do cupom e indicar claramente quem arca com o custo (iFood ou Loja).
- Observações: exibir observações de itens, instruções especiais e código de coleta quando presentes.
- Dados do cliente: exibir `CPF` ou `CNPJ` quando fornecidos na nota fiscal eletrônica.

#### Confiabilidade e sincronização

- Deduplicação: implementar lógica para detectar e descartar eventos duplicados.
- Sincronização de status: se outro sistema alterar o status do pedido, a aplicação deve atualizar seu estado interno.
- Plataforma de Negociação: processar corretamente eventos da [Plataforma de Negociação](https://developer.ifood.com.br/pt-BR/docs/guides/modules/order/handshake-platform?category=FOOD).

#### Notificação de status de entrega

- Pedidos para retirada (`TAKEOUT`): enviar `PUT /orders/{id}/readyToPickup` quando o pedido estiver pronto para que o cliente receba notificação.
- Entrega própria (`DELIVERY`): enviar `PUT /orders/{id}/dispatch` quando o pedido sair para entrega.

### Requisitos específicos por categoria

#### Restaurantes

- Exibir observações de entrega claramente na comanda.
- Implementar fluxo completo de retirada para pedidos `TAKEOUT` via `/readyToPickup`.
- Implementar fluxo de despacho para pedidos com entrega própria via `/dispatch`.

#### Mercados, farmácias, pet shops e outros

- Importar pedidos via [virtual-bag API](https://developer.ifood.com.br/pt-BR/docs/guides/modules/order/details?category=GROCERIES) somente após o pedido receber o evento `READY_FOR_INVOICE` (RFI), evitando erros de modificação concorrente durante a separação de itens.
- Consultar detalhes completos do pedido após a separação ter sido concluída, garantindo contagens precisas de itens e substituições realizadas.
- Limitar requisições de acknowledgment a no máximo `2000` IDs por chamada para respeitar limites de requisição.
- Processar `/dispatch` apenas após toda a separação estar concluída para finalizar o fluxo.

### Requisitos técnicos

- Gestão de tokens: renovar tokens de autenticação apenas quando expirados, não antecipadamente.
- Rate limit: respeitar os [limites de requisição](https://developer.ifood.com.br/pt-BR/docs/getting-started/documentation/rate-limit?category=FOOD) documentados para cada endpoint.

---

## Módulo Catalog

### Pré-requisitos

- Aplicação pronta para produção: a homologação testa a aplicação completa, não chamadas API isoladas. É necessário demonstrar a interface final (painel administrativo, app, PDV ou qualquer produto para o lojista) criando, atualizando e exibindo dados reais do catálogo. Chamadas `curl` isoladas causam cancelamento da reunião.
- Cadastro em conta Profissional (CNPJ). Cadastros Pessoal/Estudante (CPF) não são aceitos.
- Credenciais válidas e merchant de teste: `clientId`, `clientSecret` e um `merchantId` de teste funcionando.
- Cobertura dos critérios listados abaixo, aplicáveis ao caso de uso.

### Critérios de homologação

#### 1. Fundamentos do catálogo

Base mínima para qualquer integração.

- Gerenciamento de categorias: criar e atualizar categorias com `POST /categories`.
- Criação de itens simples: criar itens com título, descrição e preço usando `PUT /items`.
- Listagem e recuperação: consultar catálogos (`GET /catalogs`) e itens (`GET /items`) existentes.

#### 2. Complementos e estruturas especiais

Necessário se o cardápio tem modificadores ou produtos compostos.

- Complementos (modificadores): itens com grupos de opções (tamanhos, molhos, extras) com preços e limites `min`/`max`.
- Pizza (se aplicável): estruturas com os grupos obrigatórios `SIZE`, `CRUST`, `EDGE` e `TOPPING`.
- Combo (se aplicável): combos com grupos principais e modificadores aninhados.

#### 3. Operações em produção

Ações que acontecem no dia a dia da loja.

- Atualização em massa de preços: `PATCH /items/price` para atualizar múltiplos itens em uma chamada.
- Atualização em massa de status: `PATCH /items/status` para alternar disponibilidade em lote.
- Customização por contexto: preços e status diferentes por canal (Delivery, Digital Menu, Dine-in) via `contextModifiers`.
- Agendamento de disponibilidade: regras de disponibilidade por período (horários, dias da semana, datas especiais).
- Multi-catálogo (se aplicável): suporte a lojas com múltiplos catálogos (multisetup).

#### 4. Qualidade e resiliência

Critérios de robustez avaliados no código e na UI.

- Validação de dados: nenhum payload inválido enviado à API — títulos ≤ 100 caracteres, descrições ≤ 500, preços como números positivos, `status` restrito a `AVAILABLE`/`UNAVAILABLE`.
- Tratamento de erros: tratar erros específicos da API (`CONFLICT`, `NOT_FOUND`, `VALIDATION_ERROR`) e exibir mensagens compreensíveis ao usuário. Evitar falhas silenciosas.
- Lógica de retry: implementar retries com backoff para falhas transitórias (timeouts, `5xx`). Não retentar em `4xx`.
- Sincronização em tempo real: refletir atualizações de preço e status em até 2 segundos.
- Performance em massa: concluir atualizações de 100+ itens em até 10 segundos, sem requisições duplicadas ou polling excessivo.

### Ciclo de vida do item

Todo item tem dois estados:

- `AVAILABLE` — clientes podem comprar.
- `UNAVAILABLE` — item pausado. Não aparece no app até retornar para `AVAILABLE`.

Use `PATCH /items/status` para alternar entre os estados sem reenviar o item completo.

### Escolha o endpoint certo

A mudança que você quer fazer determina o endpoint:

| Mudança | Endpoint | Notas |
|---|---|---|
| Criar ou reescrever um item completo | `PUT /items` | Envia item, produtos, grupos e opções em uma chamada. Idempotente. |
| Alterar preço | `PATCH /items/price` | Muda apenas preço de itens. Suporta lote. Assíncrono — retorna `batchId`. |
| Alterar preço de complementos | `PATCH /options/price` | Muda apenas preço de opções (complementos). Suporta lote. |
| Pausar ou reativar um item | `PATCH /items/status` | Muda apenas status. Suporta lote. Assíncrono — retorna `batchId`. |
| Pausar uma opção de complemento | `PATCH /options/status` | Afeta uma única opção. |
| Pausar todas as opções de um grupo | `PATCH /optionGroups/status` | Afeta o grupo inteiro. |
| Ajustar estoque | `POST /inventory` | Define a quantidade máxima vendível. |

`PUT /items` substitui o item completo — campos omitidos são removidos. Use os endpoints `PATCH` para mudanças pontuais.

### Idempotência

`PUT /items` é idempotente: chamar duas vezes com o mesmo payload não cria duplicatas. A segunda chamada sobrescreve a primeira, simplificando retries em falhas transitórias.

### Padrões comuns

Guias de tarefas típicas ao integrar a Catalog API. Para conceitos, ver [Como funciona](https://developer.ifood.com.br/pt-BR/docs/food/guides/modules/catalog/workflow); para referência de campos, ver [Fundamentos](https://developer.ifood.com.br/pt-BR/docs/food/guides/modules/catalog/fundamentals).

#### Criar um item simples

`PUT /merchants/{merchantId}/items` sem complementos:

```json
{
  "item": {
    "id": "item-burger-001",
    "type": "DEFAULT",
    "categoryId": "cat-lanches-001",
    "status": "AVAILABLE",
    "price": { "value": 25.00 },
    "externalCode": "BURGER_001"
  },
  "products": [
    { "id": "prod-burger-001", "name": "X-Burger", "description": "Pão, carne, queijo e alface" }
  ],
  "optionGroups": [],
  "options": []
}
```

Validação: `status` do item é `AVAILABLE`; `price.value` está definido; `externalCode` é único por merchant.

#### Adicionar complementos

`PUT /items` com grupos de complementos (tamanhos, bebidas, extras) referenciados em `products[].optionGroups` e detalhados nos arrays de nível superior `optionGroups`/`options`:

```json
{
  "item": {
    "id": "item-burger-001", "type": "DEFAULT", "categoryId": "cat-lanches-001",
    "status": "AVAILABLE", "price": { "value": 25.00 }, "externalCode": "BURGER_001"
  },
  "products": [
    {
      "id": "prod-burger-001", "name": "X-Burger", "description": "Pão, carne, queijo e alface",
      "optionGroups": [
        { "id": "og-bebida", "min": 0, "max": 1 },
        { "id": "og-extras", "min": 0, "max": 3 }
      ]
    },
    { "id": "prod-coke", "name": "Coca-Cola" },
    { "id": "prod-suco", "name": "Suco natural" },
    { "id": "prod-bacon", "name": "Bacon extra" },
    { "id": "prod-cheese", "name": "Queijo extra" }
  ],
  "optionGroups": [
    { "id": "og-bebida", "name": "Escolha uma bebida", "status": "AVAILABLE", "optionGroupType": "OFFER_UNIT", "optionIds": ["opt-coke", "opt-suco"] },
    { "id": "og-extras", "name": "Adicione extras", "status": "AVAILABLE", "optionGroupType": "INGREDIENTS", "optionIds": ["opt-bacon", "opt-cheese"] }
  ],
  "options": [
    { "id": "opt-coke", "productId": "prod-coke", "status": "AVAILABLE", "price": { "value": 8.00 } },
    { "id": "opt-suco", "productId": "prod-suco", "status": "AVAILABLE", "price": { "value": 12.00 } },
    { "id": "opt-bacon", "productId": "prod-bacon", "status": "AVAILABLE", "price": { "value": 5.00 } },
    { "id": "opt-cheese", "productId": "prod-cheese", "status": "AVAILABLE", "price": { "value": 3.00 } }
  ]
}
```

Validação: cada `optionGroup` tem `min` ≤ `max`; cada `option` tem `price` definido; todos os `optionIds` existem em `options`; `productId` em cada opção existe em `products`.

#### Oferecer em múltiplos canais

Use `contextModifiers` no item para oferecer o mesmo item em canais diferentes (Entrega, Cardápio Digital, Consumo no Local) com preço e status distintos:

```json
{
  "item": {
    "id": "item-burger-001", "type": "DEFAULT", "categoryId": "cat-lanches-001",
    "status": "AVAILABLE", "price": { "value": 25.00 }, "externalCode": "BURGER_001",
    "contextModifiers": [
      { "catalogContext": "WHITELABEL", "price": { "value": 28.00 }, "externalCode": "BURGER_WL_001" },
      { "catalogContext": "INDOOR", "price": { "value": 22.00 }, "status": "UNAVAILABLE" }
    ]
  },
  "products": [{ "id": "prod-burger-001", "name": "X-Burger" }],
  "optionGroups": [],
  "options": []
}
```

Contextos disponíveis: `DEFAULT` (Entrega), `WHITELABEL` (Cardápio Digital), `INDOOR` (Consumo no Local).

Validação: `catalogContext` é um dos valores válidos; contextos não listados herdam valores da raiz; é possível sobrescrever `price`, `status` e `externalCode` por contexto.

#### Atualizar preços em lote

`PATCH /merchants/{merchantId}/items/price` atualiza múltiplos itens sem reenviar a estrutura completa:

```json
{
  "prices": [
    { "productId": "prod-burger-001", "price": 26.50 },
    { "productId": "prod-pizza-001", "price": 35.00 },
    { "productId": "prod-coke", "price": 8.50 }
  ]
}
```

A resposta retorna um `batchId`. Acompanhar com `GET /merchants/{merchantId}/batch/{batchId}`:

```json
{
  "batchId": "batch-123",
  "status": "COMPLETED",
  "successCount": 3,
  "failureCount": 0,
  "results": [
    { "resourceId": "prod-burger-001", "result": "SUCCESS" },
    { "resourceId": "prod-pizza-001", "result": "SUCCESS" },
    { "resourceId": "prod-coke", "result": "SUCCESS" }
  ]
}
```

Validação: consultar `GET /batch/{batchId}` até `status: COMPLETED`; verificar `result: SUCCESS` para cada item; se algum falhou, confirmar que o `productId` existe.

#### Pausar e reativar itens

`PATCH /merchants/{merchantId}/items/status` pausa ou reativa itens em lote:

```json
{
  "items": [
    { "id": "item-burger-001", "status": "UNAVAILABLE" },
    { "id": "item-pizza-001", "status": "AVAILABLE" },
    { "id": "item-soda-001", "status": "UNAVAILABLE" }
  ]
}
```

Validação: `status` é `AVAILABLE` ou `UNAVAILABLE`; acompanhar o `batchId` retornado (operação é assíncrona, mesmo padrão de `GET /batch/{batchId}` acima).

### Checklist de testes

Executar os cenários abaixo antes de agendar a homologação e guardar os resultados. O analista pode pedir para reproduzir qualquer um deles ao vivo.

#### Integração com a API

- [ ] Autenticação bem-sucedida usando credenciais OAuth 2.0.
- [ ] Listar catálogos existentes com `GET /catalogs`.
- [ ] Recuperar itens com `GET /items`.
- [ ] Criar uma nova categoria e verificar se aparece no catálogo.
- [ ] Criar um item simples (sem complementos) e verificar se todos os campos sincronizam.
- [ ] Criar um item com modificadores (mínimo 2 grupos).
- [ ] Atualizar preço do item e verificar se a mudança se propaga em até 2 segundos.
- [ ] Atualizar status do item e verificar se a mudança aparece no catálogo.
- [ ] Criar item com `contextModifiers` para múltiplos canais.
- [ ] Lidar com requisições inválidas e verificar se respostas de erro contêm `error_code` apropriado.

#### Contexto e multi-setup

- [ ] Criar itens com preços diferentes para contextos diferentes.
- [ ] Verificar atualizações de disponibilidade específicas do contexto.
- [ ] Testar operações multisetup se a loja usa múltiplos catálogos.
- [ ] Confirmar que itens criados em um contexto não afetam outros inesperadamente.

#### Estrutura de itens

- [ ] Criar itens com todos os tipos de dados suportados (texto, número, booleano).
- [ ] Criar grupos de modificadores aninhados (grupos contendo modificadores com opções).
- [ ] Testar itens com 50+ caracteres em título/descrição.
- [ ] Verificar que itens com caracteres especiais são renderizados corretamente.

#### Casos extremos e tratamento de erros

- [ ] Tentar criar item com campos obrigatórios faltando (título, categoria).
- [ ] Verificar erro `400` ao enviar JSON inválido.
- [ ] Tentar atualizar item inexistente e verificar resposta `404`.
- [ ] Testar requisições concorrentes para garantir thread safety.
- [ ] Verificar comportamento de rate limiting nos limites da API.
- [ ] Confirmar tratamento de timeout para requisições acima de 30 segundos.

#### Suporte multi-idioma

- [ ] Criar itens com títulos em Português (pt-BR).
- [ ] Criar itens com títulos em Espanhol (es-CO).
- [ ] Criar itens com títulos em Inglês (en-US).
- [ ] Verificar que todos os caracteres especiais e acentos são exibidos corretamente.
- [ ] Confirmar que preços específicos do idioma funcionam conforme esperado.

---

## Módulo Merchant

### Pré-requisitos

- Aplicativo completamente pronto para teste.
- Conta Profissional (CNPJ). Contas Pessoal/Estudante (CPF) não são aceitas.
- Token de acesso válido fornecido durante onboarding.

### Checklist de endpoints

Validar que cada endpoint funciona corretamente:

| Endpoint | Método | Critério de aprovação |
|---|---|---|
| `/merchants` | GET | Retorna array de lojas com `id`, `name`, `corporateName` |
| `/merchants/{merchantId}` | GET | Retorna objeto com operações e endereço completo |
| `/merchants/{merchantId}/status` | GET | Retorna `state` (`OK`/`WARNING`/`CLOSED`/`ERROR`) com validações |
| `/merchants/{merchantId}/interruptions` | GET | Retorna array vazio ou com interrupções ativas |
| `/merchants/{merchantId}/interruptions` | POST | Cria pausa com `id`, `start`, `end` e retorna `201` |
| `/merchants/{merchantId}/interruptions/{id}` | DELETE | Remove pausa e retorna `204` sem conteúdo |
| `/merchants/{merchantId}/opening-hours` | GET | Retorna array de turnos com `dayOfWeek`, `start`, `duration` |
| `/merchants/{merchantId}/opening-hours` | PUT | Atualiza horários e retorna `201` com turnos criados |

### Cenários de teste

#### Autenticação

| Teste | Expected | Pass |
|---|---|---|
| Requisição sem token | `401 Unauthorized` com mensagem clara | Erro retornado com código `401` |
| Token inválido | `401 Unauthorized` | Erro retornado com código `401` |
| Token válido | `200` com dados da loja | Resposta bem-sucedida |

#### Listagem de lojas

| Teste | Expected | Pass |
|---|---|---|
| `GET /merchants` sem parâmetros | Array com todas as lojas | Mínimo 1 loja retornada com `id` válido |
| `GET /merchants?page=1&size=10` | Máximo 10 lojas por página | Resposta contém até 10 lojas |

#### Status da loja

| Teste | Expected | Pass |
|---|---|---|
| `GET /merchants/{merchantId}/status` com loja ABERTA | `state: OK` ou `WARNING` com `available: true` | Validação is-connected retorna `OK` |
| `GET /merchants/{merchantId}/status` com loja FECHADA | `state: CLOSED` com `available: false` | Validação opening-hours retorna `CLOSED` |

#### Pausas (Interrupções)

| Teste | Expected | Pass |
|---|---|---|
| Criar pausa válida com `POST /interruptions` | Response `201` com ID da pausa criada | Pausa aparece em `GET /interruptions` |
| Criar pausa com sobreposição | `409 InterruptionOverlap` | Erro retornado |
| Remover pausa com `DELETE /interruptions/{id}` | Response `204` (sem conteúdo) | Pausa não aparece em listagem posterior |

#### Horários

| Teste | Expected | Pass |
|---|---|---|
| Consultar horários com `GET /opening-hours` | Array de turnos configurados | Cada turno tem `dayOfWeek`, `start`, `duration` |
| Atualizar horários com múltiplos turnos | Response `201` com turnos criados | `GET /opening-hours` retorna novos valores |
| Tentar turnos sobrepostos | `400 BadRequest` | Erro retornado |

### Tratamento de erros

Verificar respostas de erro para:

| Código | Cenário | Verificação |
|---|---|---|
| `400` | Parâmetros inválidos | Corpo inclui `code` e `message` |
| `401` | Token inválido/expirado | Mensagem clara sobre autenticação |
| `403` | Sem acesso à loja | Erro indica permissão insuficiente |
| `409` | Recurso em conflito | Código específico (ex: `InterruptionOverlap`) |
| `429` | Rate limit | Header `Retry-After` presente |
| `500` | Erro do servidor | Mensagem genérica, sem expor detalhes |

### Considerações finais

- Implementar retry com backoff exponencial para erros `5xx`.
- Respeitar limite de 1000 requisições por segundo.
- Usar polling mínimo de 30 segundos para status.
- Validar tokens antes de usar em produção.
- Testar em ambiente de homologação antes de subir para produção.
