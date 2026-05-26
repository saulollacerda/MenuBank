export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  merchantId: string
  email: string
  merchantName: string
}

