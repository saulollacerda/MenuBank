# Backend gaps for the new design

This document inventories every behavior, field, or endpoint that the redesigned
frontend (handoff bundle `menubank/Sistema Completo.html`, Direction A) needs
but the backend does not yet expose. Updated after the full Phase-2 frontend
rewrite (all screens: Login, Register, Dashboard, Pedidos, Produtos,
Categorias, Ingredientes, Clientes, Taxas, Configurações).

Format per gap:

- **Where used** — screen / component in the design.
- **What's needed** — concrete contract change required.
- **Current state** — what the backend has today, if anything.

---

## 1. Dashboard

### 1.1 Period-over-period KPI deltas

- **Where used** — every KPI card in `d1-modern.jsx` (`Faturamento`, `Pedidos`,
  `Ticket médio`, `Lucro estimado`, plus `Clientes` and `Margem média` on other
  artboards). Each card shows a green/red pill with `±X,X%` "vs. Abril".
- **What's needed** — `DashboardResponse` must include a previous-period
  comparison block. Minimal shape:
  ```json
  {
    "totalSalesChangePct": 12.4,
    "orderCountChangePct": 8.2,
    "averageTicketChangePct": 2.1,
    "estimatedProfitChangePct": 15.3,
    "estimatedMarginPct": 63.4,
    "estimatedMarginChangePct": 1.2,
    "customerCount": 142,
    "customerCountChangePct": 6.5
  }
  ```
  Backend should compute against the equivalent prior period (e.g. previous
  calendar month when filter is monthly; previous `(endDate − startDate + 1)`
  window otherwise).
- **Current state** — `DashboardResponse` only returns absolute values for the
  current window; no prior-period comparison, no margin, no customer count.

### 1.2 Estimated margin %

- **Where used** — `Margem média` KPI card.
- **What's needed** — server-computed margin (estimatedProfit / totalSales).
  Could be derived client-side, but the design treats it as a first-class KPI
  with its own delta, so it should be a backend field.

### 1.3 Customer count for the period

- **Where used** — `Clientes` KPI card.
- **What's needed** — distinct customers who ordered in the window.
- **Current state** — no endpoint exposes this. `CustomerController` only lists
  all customers (no period filter, no order-based filter).

### 1.4 Peak hours (`Horário de pico`)

- **Where used** — Dashboard mid-column panel.
- **What's needed** — endpoint returning `[{ hour: 11..23, orderCount, pct }]`
  for the selected period. Suggested:
  `GET /api/dashboard/peak-hours?startDate&endDate` →
  `{ items: [{ hour, orderCount, pct }] }`.
- **Current state** — not exposed. The redesigned dashboard currently shows
  an "Em breve" placeholder for this section.

### 1.5 Channels breakdown (`Anota.AI / iFood / Balcão`)

- **Where used** — Dashboard channels panel (in design exploration).
- **What's needed** — `GET /api/dashboard/channels?startDate&endDate` →
  `[{ origin: 'ANOTA_AI'|'IFOOD'|'MENUBANK', orderCount, pct }]`.
- **Current state** — origin is on `Order`, but there is no aggregation
  endpoint.

### 1.6 Top products enrichment

- **Where used** — Dashboard "Top produtos" panel.
- **What's needed** — each top-product row in the design shows
  `name · category · qty · margin%`, plus the revenue total per product. Today
  the DTO is only `{ productName, quantitySold }`.
- **Suggested shape**:
  ```json
  {
    "productId": "uuid",
    "productName": "Açaí 500 ml",
    "categoryName": "Açaí",
    "quantitySold": 87,
    "revenue": 1913.13,
    "marginPct": 71.2
  }
  ```

### 1.7 "Pedidos recentes" with margin

- **Where used** — Dashboard right-column panel.
- **What's needed** — the design renders five recent orders with status pill,
  value, and a green "lucro" figure beside the value. The current paged orders
  endpoint already returns `estimatedProfit`, so the Vue side can feed this
  panel; **no new endpoint required**. (Implemented in Phase 1 via `/api/orders`
  page-0 fetch.) Listed here only as a reminder that the design includes the
  profit column.

---

## 2. Pedidos (Orders)

### 2.1 Status pills include `pronto` and `entregue`

- **Where used** — `screens-pedidos.jsx` (`STATUS_PILL`): `pendente`, `pronto`,
  `entregue`, `cancelado`. Filter pills also show counts for "Prontos" /
  "Entregues".
- **Current state** — `OrderStatus` enum is `PENDING | PAID | CANCELLED`. No
  notion of `READY` or `DELIVERED`. The redesigned frontend currently maps
  backend statuses to design pills as best it can but cannot represent the
  full delivery lifecycle.
- **What's needed** — extend the enum to a full lifecycle, e.g.
  `PENDING → READY → DELIVERED → (PAID?) | CANCELLED`. Decide whether `PAID`
  remains independent of delivery state. Update DB migration, services and
  exports.

### 2.2 Per-status counts for filter pills

- **Where used** — top filter row of the pedidos list (`Todos`, `Pendentes`,
  `Prontos`, `Entregues`, `Cancelados`, each with a count).
- **What's needed** — either:
  - extend the orders page response with `statusCounts: { PENDING: n, ... }`,
    or
  - add `GET /api/orders/status-counts?startDate&endDate&search`.
- **Current state** — the redesigned Orders screen counts only the rows in the
  *current page*, which is misleading once pagination kicks in.

### 2.3 Server-side status filter

- **Where used** — clicking a status pill should narrow the query.
- **What's needed** — `GET /api/orders?status=PENDING&...`. Currently the
  redesigned screen filters in-memory on the current page, which produces wrong
  results when totals span more than one page.

### 2.4 Margin column

- **Where used** — last data column in the list table.
- **What's needed** — `OrderResponse.marginPct` (server-computed).
  - Today the frontend derives `estimatedProfit / totalValue`, which is fine
    when both fields are populated, but loses precision for orders where
    `totalValue` was rounded or fee-adjusted server-side.

### 2.5 "Exportar PDF" on order detail

- **Where used** — `ModalDetalhesPedido` footer.
- **What's needed** — `GET /api/orders/{id}/pdf` returning a PDF blob.
- **Current state** — no per-order export. There is `GET /api/export/dashboard`
  but only as XLSX and only at aggregate level.

### 2.6 Order timestamps with explicit time zone

- **Where used** — design rows use times like `17:54`; the time column should
  reflect the merchant's local time, not UTC.
- **What's needed** — confirm `Order.dateTime` is stored / returned in the
  merchant's TZ, or include a TZ offset / zoned datetime in the DTO. (Today
  the frontend assumes the browser's local TZ matches the merchant.)

---

## 3. Produtos / Ficha técnica

### 3.1 Custo e margem inline na lista

- **Where used** — `Custo` and `Margem` columns of the products list table.
- **What's needed** — `ProductResponse.unitCost` (sum of the product's includes)
  and `ProductResponse.marginPct` ((price − unitCost) / price). Today the
  redesigned list shows `—` for both columns because `ProductResponse` only
  carries `{ id, name, price, status, categoryId, categoryName }`.

### 3.2 Ficha-técnica "color stripe" / category color

- **Where used** — vertical color stripe at the left of each product / category
  row, and the band on top of the category cards.
- **What's needed** — server-side `colorHex` field on `CategoryResponse` (and
  optionally `ProductResponse` if products override). The redesigned frontend
  currently hashes the category name to derive a deterministic palette color
  client-side, which works but doesn't let the user customize.

### 3.3 Toggle ativo/inativo for products

- **Where used** — `Status` column shows `Ativo`/`Inativo`. Design implies
  toggling via the edit modal.
- **Current state** — `ProductStatus` exists in DB and `ProductResponse`, but
  `ProductRequest` has no `status` field — there is no way to flip a product to
  `INACTIVE` short of deleting it. Need either a `status` field in the request
  DTO or a dedicated `PATCH /api/products/{id}/status` endpoint.

### 3.4 Ficha-técnica typed kinds (ingrediente / embalagem)

- **Where used** — design shows each ficha item with a `Tipo` pill
  (`ingrediente` / `embalagem`).
- **What's needed** — `Include` would gain a `kind` enum
  (`INGREDIENT | PACKAGING`) or a free-form `tag` field. Not modelled today.

### 3.5 Ficha-técnica reorder (drag handle)

- **Where used** — design shows a drag handle on each ficha item.
- **What's needed** — `sortOrder` on `Include` and `PUT
  /api/products/{productId}/includes/reorder` (array of `{ id, sortOrder }`).
  Not modelled today.

### 3.6 Product image / thumbnail

- The design renders product thumbnails as a color stripe placeholder; the
  thumbnail position hints at imagery support down the road. Backend has no
  image storage. Defer until explicitly needed.

---

## 4. Categorias

- **Card grid** — design shows category cards with `produto count` and revenue
  this month. The redesigned frontend renders cards but omits both numbers
  because `CategoryResponse` is only `{ id, name }`.
- **What's needed**:
  - `productCount` on `CategoryResponse` (cheap to compute server-side).
  - `colorHex` for the visual tag at the top of each card (today the frontend
    hashes the name).
  - period revenue: `GET /api/categories/revenue?startDate&endDate` returning
    `[{ categoryId, revenue }]`, or include `revenuePeriod` on
    `CategoryResponse` when a date filter is supplied.

---

## 5. Ingredientes

### 5.1 Toggle ativo/inativo

- **Where used** — `Status` column with `Ativo`/`Inativo` pill and a filter pill
  in the toolbar.
- **Current state** — `IngredientResponse.status` exists, but
  `IngredientRequest` has no `status` field. Need either a `status` field in the
  request DTO or a `PATCH /api/ingredients/{id}/status` endpoint. Today the
  redesigned screen renders the badge but offers no way to flip it.

### 5.2 Stock tracking

- **Where used** — design references stock metrics on the rows footer ("Custo
  total estoque"). The frontend doesn't render this today because the field
  isn't modelled.
- **What's needed** — `Ingredient.stockQuantity`, `Ingredient.lastReplenishedAt`,
  `Ingredient.lowStockThreshold`; new aggregate field
  `IngredientResponse.totalStockCost`.

### 5.3 Usage count inline

- The redesigned modal already calls `GET /api/ingredients/{id}/usages` to
  populate the per-product grammage list — keep that endpoint. But the
  ingredient list table would benefit from a `usageCount` aggregate inline so
  it doesn't require an N+1 call per row.

### 5.4 CSV import

- **Where used** — `Importar CSV` button in the design. Not implemented in the
  current redesign because backend has no endpoint.
- **What's needed** — `POST /api/ingredients/import-csv` accepting `multipart/form-data`.

---

## 6. Clientes

### 6.1 Per-customer aggregates

- **Where used** — design shows `Pedidos` count, `LTV`, `Último pedido`, plus
  a `Tag` pill (VIP / Frequente / Recorrente / Novo) for every row. KPI summary
  at the top of the page also uses these aggregates.
- **Current state** — `CustomerResponse` is only profile fields
  (`id, name, phone, email`). The redesigned screen shows only those columns and
  omits the rest.
- **What's needed** — extend `CustomerResponse` with `orderCount`,
  `lifetimeValue`, `lastOrderAt`, `preferredOrigin`, or expose a dedicated
  `GET /api/customers/{id}/stats`. Tagging logic (VIP threshold etc.) can be
  derived from those aggregates client-side.

### 6.2 Optional address + observations

- **Where used** — design's "Novo Cliente" modal asks for address (for
  per-bairro fee matching) and free-form observations.
- **What's needed** — `Customer.address` and `Customer.notes` columns;
  `CustomerRequest` / `CustomerResponse` extended accordingly. Not modelled
  today, so the redesigned modal only collects name / phone / email.

---

## 7. Taxas

### 7.1 Typed fees (bairro / distância / valor / adicional)

- **Where used** — design splits fees by `Tipo` (`Por bairro`, `Por distância`,
  `Por valor`, `Adicional`), each with type-specific config (bairro list,
  distance range, minimum order value, percentage vs fixed).
- **Current state** — `Fee` is only `{ name, feeRate }` (percentage). The
  redesigned screen renders only those two columns and applies the rate as a
  generic percentage.
- **What's needed** — `Fee` gains:
  - `kind` enum: `BAIRRO | DISTANCE | MIN_ORDER_VALUE | ADDITIONAL`
  - `applyAs` enum: `FIXED | PERCENT`
  - `amount` (BigDecimal, replaces / extends `feeRate`)
  - `neighborhoods: string[]` (for `BAIRRO`)
  - `minDistanceKm`, `maxDistanceKm` (for `DISTANCE`)
  - `minOrderValue` (for `MIN_ORDER_VALUE`)
  - `active` boolean
  - `availableForExternalOrders` boolean (the Anota.AI checkbox in the design)

### 7.2 Toggle ativa/inativa

- Currently no `active` flag on `Fee`; status pill in the design can't be
  honored without one.

### 7.3 Fee usage stats

- Design hints at "qty pedidos aplicada" / last applied date per fee. Need a
  `GET /api/fees/{id}/stats` returning `{ orderCount, lastAppliedAt, totalRevenue }`.

---

## 8. Configurações

The design's sub-nav has 7 sections; the redesigned `SettingsView` renders all
of them but only `Perfil da loja` and `Integrações` carry real data — the rest
are placeholders pending backend work.

### 8.1 Perfil da loja — editing

- Today `Merchant` has `merchantName, cnpj, email, phone` and no edit endpoint.
  The redesigned screen renders the four fields as **read-only inputs** with an
  amber notice.
- **What's needed** — `PUT /api/merchants/me` accepting an
  `MerchantUpdateRequest`. Also missing: `address`, `openingHours`, `logoUrl`
  fields + a logo upload endpoint.

### 8.2 Integrações — additional channels

- ✅ Anota.AI key works (`PUT /api/merchants/me/anota-ai-key`). The redesigned
  screen wires this up.
- Missing: iFood key/toggle, WhatsApp Business token, Mercado Pago credentials.
  Today the screen renders these as "Em breve" cards.

### 8.3 Horários (opening hours)

- No DB model. Need `Merchant.openingHours: Json | OpeningHours[]` shape like
  `{ dayOfWeek, openTime, closeTime, closed }`.

### 8.4 Alertas / preferences

- Design has toggle list for "Calcular margem em tempo real", "Alertar margem
  abaixo de 50%", "Avisar sobre ingredientes não cadastrados", "Incluir taxa de
  embalagem no custo". None of these preferences are modelled today.
- **What's needed** — `MerchantPreferences` block (likely a single JSON column
  or a dedicated table) + `GET / PUT /api/merchants/me/preferences`.

### 8.5 Equipe (team / role-based access)

- No `User` ↔ `Merchant` many-to-many. Today each merchant *is* a user. Need:
  - `User` entity distinct from `Merchant`
  - `MerchantUser` join with role (`OWNER | ADMIN | STAFF`)
  - Invite flow: `POST /api/merchants/me/invitations`, `POST
    /api/invitations/{token}/accept`.

### 8.6 Plano e pagamento (billing)

- Not modelled at all. Requires a subscription / billing integration.

### 8.7 Zona perigosa

- "Excluir conta" / "Cancelar plano" / "Exportar todos os dados" — none exist.

---

## 9. Notificações (sino)

The existing `MISSING_INGREDIENT` notification type already drives the bell
panel. The design also surfaces:

- New order notifications (`type: 'NEW_ORDER'` with deep link).
- Daily closing reminders.
- Low-stock alerts (depends on ingredient stock tracking, see §5).

Backend `NotificationType` enum currently has only `MISSING_INGREDIENT`. Adding
new types requires DB migration + producer code paths.

---

## 10. Autenticação

- "Esqueceu a senha?" link on login — backend has no password-reset flow.
  Need `POST /api/auth/forgot-password` and `POST /api/auth/reset-password`,
  plus an email-sending capability.
- "Manter conectado" / refresh tokens — currently the JWT lives in
  `localStorage` until expiry; no refresh-token endpoint.
- "Entrar com Google" — design has the button; backend has no OAuth flow.
- Registration accepts a `phone` field but no opt-in flag for ToS; design
  shows a required ToS checkbox. Either persist the consent timestamp on
  `Merchant` or store separately.

---

## 11. Anota.AI sync — clearer dry-run + result UX

The current `POST /api/integrations/anotaai/catalog` accepts `clearRecipes` and
returns counts. Two small gaps the redesign exposes:

- No dry-run mode (`?dryRun=true`) to preview what would change before applying.
- The `lastResult.missingIngredientNames` array is surfaced as an amber alert in
  Orders; great. The same data is not surfaced for catalog sync, so a missing
  ingredient referenced by a synced product is silent until the next order
  import.
