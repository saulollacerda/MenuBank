export type UserStatus = 'ACTIVE' | 'INACTIVE'

export interface UserRequest {
  merchantName: string
  cnpj: string
  email: string
  password: string
  confirmPassword: string
  phone?: string
}

export interface UserResponse {
  id: string
  merchantName: string
  cnpj: string
  email: string
  phone: string | null
  status: UserStatus
  createdAt: string
  anotaAiApiKey?: string | null
}

export interface AnotaAIKeyRequest {
  anotaAiApiKey: string | null
}
