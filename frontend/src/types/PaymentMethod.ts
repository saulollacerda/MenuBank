export interface PaymentMethodRequest {
  name: string
  feeRate: number
}

export interface PaymentMethodResponse {
  id: string
  name: string
  feeRate: number
}
