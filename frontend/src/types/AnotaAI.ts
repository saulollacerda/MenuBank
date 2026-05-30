export interface AnotaAISyncResult {
  ordersImported: number
  ordersSkipped: number
  categoriesCreated: number
  categoriesUpdated: number
  productsCreated: number
  productsUpdated: number
  /**
   * Names of ingredients referenced by imported orders that are not registered
   * locally. Surfaced in the UI as a temporary alert; persistent tracking lives
   * in the notifications inbox.
   */
  missingIngredientNames?: string[]
  errors: string[]
}
