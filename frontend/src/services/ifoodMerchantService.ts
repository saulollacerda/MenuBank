import api from './api'

const BASE = '/integrations/ifood/merchant'

export type MerchantState = 'OK' | 'WARNING' | 'CLOSED' | 'ERROR'

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface MerchantDetails {
  id: string
  name: string
  corporateName: string
}

export interface MerchantMessage {
  title: string
  subtitle: string
}

export interface MerchantValidation {
  id: string
  code: string
  state: string
  message: MerchantMessage | null
}

export interface MerchantStatus {
  operation: string
  salesChannel: string
  available: boolean
  state: MerchantState
  message: MerchantMessage | null
  validations: MerchantValidation[]
}

export interface MerchantInterruption {
  id: string
  description: string
  /** ISO date-time. */
  start: string
  /** ISO date-time. */
  end: string
}

export interface InterruptionInput {
  description: string
  start: string
  end: string
}

export interface MerchantShift {
  id: string | null
  dayOfWeek: DayOfWeek
  /** "HH:mm:ss". */
  start: string
  /** Duration in minutes. */
  duration: number
}

export interface ShiftInput {
  dayOfWeek: DayOfWeek
  start: string
  duration: number
}

export interface OpeningHours {
  shifts: MerchantShift[]
}

/**
 * Maps a merchant API failure to user-facing pt-BR copy. The backend already
 * returns an RFC-7807 ProblemDetail whose `detail` is a ready-to-show pt-BR
 * message (e.g. the interruption overlap 409, or the not-connected 409), so we
 * surface it directly and only fall back when it is missing.
 */
export function merchantErrorMessage(
  err: unknown,
  fallback = 'Não foi possível concluir a operação. Tente novamente em instantes.',
): string {
  const detail = (err as { response?: { data?: { detail?: unknown } } })?.response?.data?.detail
  if (typeof detail === 'string' && detail.trim().length > 0) {
    return detail
  }
  return fallback
}

export const ifoodMerchantService = {
  async getDetails(): Promise<MerchantDetails> {
    const { data } = await api.get<MerchantDetails>(`${BASE}/details`)
    return data
  },

  async getStatus(): Promise<MerchantStatus[]> {
    const { data } = await api.get<MerchantStatus[]>(`${BASE}/status`)
    return data
  },

  async getInterruptions(): Promise<MerchantInterruption[]> {
    const { data } = await api.get<MerchantInterruption[]>(`${BASE}/interruptions`)
    return data
  },

  async createInterruption(input: InterruptionInput): Promise<MerchantInterruption> {
    const { data } = await api.post<MerchantInterruption>(`${BASE}/interruptions`, input)
    return data
  },

  async deleteInterruption(id: string): Promise<void> {
    await api.delete(`${BASE}/interruptions/${id}`)
  },

  async getOpeningHours(): Promise<OpeningHours> {
    const { data } = await api.get<OpeningHours>(`${BASE}/opening-hours`)
    return data
  },

  async updateOpeningHours(shifts: ShiftInput[]): Promise<OpeningHours> {
    const { data } = await api.put<OpeningHours>(`${BASE}/opening-hours`, { shifts })
    return data
  },
}
