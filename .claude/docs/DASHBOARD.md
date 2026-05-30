# Dashboard Specifications

Read-only aggregation view. All data filterable by date range (default: today).

## KPI Cards

| KPI (pt-BR) | KPI (EN) | Description |
|---|---|---|
| Total de Vendas | Total Sales | Sum of all order totals (R$) within the selected date range |
| Quantidade de Pedidos | Order Count | Number of orders within the selected date range |
| Ticket Médio | Average Ticket | Total Sales ÷ Order Count |
| Lucro Estimado | Estimated Profit | Sum of estimated profit across all orders in the date range |

## Charts & Widgets

| Widget (pt-BR) | Widget (EN) | Description |
|---|---|---|
| Vendas por Dia | Sales by Day | Line/bar chart — daily sales totals within the date range |
| Top 5 Produtos | Top 5 Products | Ranked list of 5 best-selling products within the date range |

## Endpoint

`GET /api/dashboard?from=<date>&to=<date>`