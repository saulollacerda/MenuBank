export interface CustomerRequest {
  name: string
  phone?: string
  email?: string
}

export interface CustomerResponse {
  id: string
  name: string
  phone: string | null
  email: string | null
}

