export interface AnotaAISyncResult {
  ordersImported: number
  ordersSkipped: number
  categoriesCreated: number
  categoriesUpdated: number
  productsCreated: number
  productsUpdated: number
  errors: string[]
}
