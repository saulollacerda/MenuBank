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
  salesByDay: DailySales[]
  topProducts: TopProduct[]
}

