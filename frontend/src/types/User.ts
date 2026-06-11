export type UserStatus = 'ACTIVE' | 'INACTIVE'

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface OpeningHour {
  dayOfWeek: DayOfWeek
  openTime: string | null
  closeTime: string | null
  closed: boolean
}

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
  openingHours?: OpeningHour[] | null
}

export interface AnotaAIKeyRequest {
  anotaAiApiKey: string | null
}
