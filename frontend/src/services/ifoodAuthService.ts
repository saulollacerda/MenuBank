import api from './api'

export interface IfoodUserCodeResponse {
  userCode: string
  verificationUrl: string
  verificationUrlComplete: string
  expiresIn: number
}

export interface ConnectError {
  message: string
  expired: boolean
}

/**
 * Maps a /connect failure to user-facing pt-BR copy. 409 means the pending
 * authorization was lost on the backend (e.g. restart) — only a new userCode
 * can recover, so callers should offer "Gerar novo código".
 */
export function connectErrorMessage(err: unknown): ConnectError {
  const status = (err as { response?: { status?: number } })?.response?.status
  if (status === 409) {
    return {
      message: 'Este código não está mais ativo. Gere um novo código e refaça a autorização no portal.',
      expired: true,
    }
  }
  if (status === 400) {
    return {
      message: 'Código de autorização inválido ou expirado. Confira o código copiado do portal do iFood.',
      expired: false,
    }
  }
  return {
    message: 'Não foi possível conectar ao iFood. Tente novamente em instantes.',
    expired: false,
  }
}

export interface IfoodStatusResponse {
  connected: boolean
}

export const ifoodAuthService = {
  async status(): Promise<IfoodStatusResponse> {
    const { data } = await api.get<IfoodStatusResponse>('/integrations/ifood/auth/status')
    return data
  },

  async start(): Promise<IfoodUserCodeResponse> {
    const { data } = await api.post<IfoodUserCodeResponse>('/integrations/ifood/auth/start')
    return data
  },

  async connect(authorizationCode: string): Promise<void> {
    await api.post('/integrations/ifood/auth/connect', { authorizationCode })
  },

  async revoke(): Promise<void> {
    await api.delete('/integrations/ifood/auth/revoke')
  },
}
