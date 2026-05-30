export interface LoginRequest {
  email: string
  password: string
}

/** Business data sent to POST /api/auth/provision on first authenticated access. */
export interface ProvisionRequest {
  merchantName: string
  cnpj: string
  email: string
  phone?: string
}

