export interface DailySales {
  date: string
  total: number
}

export interface TopProduct {
  productName: string
  quantitySold: number
}

export interface DashboardResponse {
  totalSales: number
  orderCount: number
  averageTicket: number
  estimatedProfit: number
  averageMarginPct?: number | null
  salesByDay: DailySales[]
  topProducts: TopProduct[]
}

