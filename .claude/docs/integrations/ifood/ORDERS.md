# iFood — Orders

MenuBank consome pedidos do iFood via **polling de eventos** (não webhook). O único evento processado é
`CONCLUDED` — os demais (`PLACED`, `CONFIRMED`, `DISPATCHED`, `READY_TO_PICKUP`, `CANCELLED`, etc.) são
apenas reconhecidos (acknowledged) para não voltarem na fila, sem nenhuma ação de negócio.

**Base URL:** `https://merchant-api.ifood.com.br/order/v1.0`

## Por que só `CONCLUDED`

O iFood marca o pedido como `CONCLUDED` **automaticamente** após um prazo (entrega confirmada, ou timeout
de 4h/6h/13h dependendo do tipo de loja/entrega, somado a `deliveryTimeInSeconds`) — independente de o
lojista ter chamado `confirm`/`startPreparation`/`dispatch`. Ou seja, o evento chega sozinho mesmo que o
MenuBank nunca reaja aos eventos anteriores do ciclo de vida. Por isso não é necessário implementar máquina
de estados local nem as ações de confirmar/preparar/despachar — só reagir ao `CONCLUDED` e importar.

## Polling de eventos

```
GET /events:polling
Authorization: Bearer {accessToken}
Header: x-polling-merchants: {ifoodMerchantId1},{ifoodMerchantId2},...
```

Resposta (200 OK) — array puro, sem wrapper `events`:

```json
[
  {
    "id": "evt_123",
    "code": "CON",
    "fullCode": "CONCLUDED",
    "orderId": "ord_456",
    "merchantId": "df8eb84a-...",
    "createdAt": "2024-04-25T19:00:00Z",
    "salesChannel": "IFOOD"
  }
]
```

> `code` vem **abreviado** (ex.: `CON` para concluído, `PLC` para `PLACED`) — o filtro de negócio deve
> comparar contra `fullCode`, não `code`. Confirmado via chamada real à API em 2026-07-04; a doc anterior
> (`GET /orders:polling`, wrapper `{"events":[...]}`, `code` por extenso) estava desatualizada e retorna
> `404 {"message":"no Route matched with those values"}` (erro de gateway do Kong, não de negócio).

Chamado a cada 30s (limite de rate do iFood). Um único merchant-app token cobre todos os `ifoodMerchantId`
autorizados — não é necessário chamar por merchant.

## Reconhecimento de eventos (acknowledgment)

```
POST /events/acknowledgment
Authorization: Bearer {accessToken}
Content-Type: application/json

[{ "id": "evt_123" }, { "id": "evt_124" }]
```

Resposta: `202 Accepted`. Corpo é array puro de objetos `{"id": ...}` — não o wrapper `{"acknowledgedEventIds": [...]}`
da doc anterior. **Todo** evento recebido no polling deve ser reconhecido, mesmo os ignorados (diferentes de
`CONCLUDED`) — senão o iFood reenvia os mesmos eventos no próximo polling.

## Detalhe do pedido — `GET /orders/{id}`

```
GET /orders/{id}
Authorization: Bearer {token}
```

Retorna `200 OK` com a estrutura completa do pedido. `404` se o detalhe ainda não estiver disponível (raro
para `CONCLUDED`, já que o pedido percorreu todo o ciclo) ou se o pedido tiver mais de 7 dias — nesses
casos, logar e pular (sem retry infinito).

### Informações gerais

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | uuid | Identificador único do pedido |
| `displayId` | string | ID amigável para exibição |
| `orderType` | enum | `DELIVERY`, `TAKEOUT`, `DINE_IN` |
| `orderTiming` | enum | `IMMEDIATE` ou `SCHEDULED` |
| `salesChannel` | string | `IFOOD`, `DIGITAL_CATALOG`, `POS`, `TOTEM`, `IFOOD_SHOP`, `GROCERY_WHITELABEL` |
| `category` | string | `FOOD`, `GROCERY`, `ANOTAI`, `FOOD_SELF_SERVICE` — **MenuBank só processa `category == "FOOD"`**; demais categorias são ignoradas na importação |
| `createdAt` | date | Data/hora de criação (UTC) |
| `preparationStartDateTime` | date | Horário recomendado para início do preparo |
| `isTest` | boolean | Pedido de teste — **MenuBank pula a importação quando `true`** |
| `extraInfo` | string | Informações adicionais livres (ex.: `"Pago Online. NÃO LEVAR MÁQUINA"`) — **persistido em `orders.extra_info` para uso futuro** (ex.: impressão de comanda), sem consumo funcional na v1 |

### `merchant`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | uuid | Identificador único da loja no iFood — usado para rotear o pedido ao `Merchant` local via `Merchant.ifoodMerchantId` |
| `name` | string | Nome da loja |

### `customer`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | uuid | Identificador único do cliente |
| `name` | string | Nome do cliente |
| `documentNumber` | string | CPF/documento (opcional, só para NF) |
| `phone.number` | string | Telefone do cliente — **opcional, some 3h após a entrega**; alguns pedidos podem não trazer mesmo antes disso |
| `phone.localizer` | string | Código para contato via 0800 |

### `items[]`

| Campo | Tipo | Descrição |
|---|---|---|
| `externalCode` | string | Código PDV (opcional) — 1ª tentativa de match de produto |
| `name` | string | Nome do item — usado no match por **nome canônico** contra `products.canonical_name` quando não há `externalCode` |
| `quantity` | double | Quantidade |
| `unitPrice` | double | Preço unitário |
| `optionsPrice` | double | Preço dos complementos |
| `totalPrice` | double | Preço total (item + complementos) |
| `options[]` | array | Complementos — resolvidos contra `ingredients` por **nome canônico** (`IngredientNameNormalizer`), igual ao fluxo já usado para Anota.AI; sem match → `order_item_unmatched_subitems` + notificação `MISSING_INGREDIENT` |
| `options[].customizations[]` | array | 3º nível de customização — **fora do escopo v1**, não processado |

### `total`

| Campo | Tipo | Descrição |
|---|---|---|
| `subTotal` | double | Somatório dos itens |
| `deliveryFee` | double | Taxa de entrega → `Order.deliveryFee` |
| `orderAmount` | double | Total do pedido (`subTotal + deliveryFee + additionalFees - benefits`) → `Order.totalValue` |

### Campos fora do escopo v1

`benefits`, `additionalFees`, `payments`, `delivery`/`deliveryAddress`, `takeout`, `dineIn`, `picking` —
presentes na resposta da API, mas **não persistidos** nesta entrega (a entidade `Order` atual não tem
campos para cupons, breakdown de pagamento ou endereço). Revisitar se algum dashboard precisar desses
dados no futuro.

## Mapeamento → `Order`/`OrderItem`/`OrderItemExtraIngredient`

| Campo iFood | Destino MenuBank |
|---|---|
| `id` | `Order.externalOrderId` |
| `createdAt` (UTC) | `Order.dateTime` — parse `OffsetDateTime` → converte para `America/Sao_Paulo` → `LocalDateTime` |
| `customer.name` + `customer.phone.number` | Resolução de `Customer` |
| `items[].externalCode` ou `items[].name` | Resolução de `Product` — `externalCode` primeiro, nome canônico como fallback |
| `items[].quantity` / `unitPrice` | `OrderItem.quantity` / `unitPrice` |
| — (ficha técnica local) | `OrderItem.unitCost` via `ProductCostCalculator` |
| `items[].options[]` | `OrderItemExtraIngredient` (match por nome canônico) |
| `total.orderAmount` | `Order.totalValue` |
| `total.deliveryFee` | `Order.deliveryFee` |
| `extraInfo` | `Order.extraInfo` |
| `merchant.id` | Roteamento — resolve `Order.merchant` local, não persistido como está |
| — | `Order.fee = null` (payments é lista, não mapeia 1:1 para `Fee`) |
| — | `Order.origin = OrderOrigin.IFOOD`, `Order.status = OrderStatus.DELIVERED` |
