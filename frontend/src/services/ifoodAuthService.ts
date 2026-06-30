import api from './api'

export interface IfoodUserCodeResponse {
  userCode: string
  verificationUrl: string
  expiresIn: number
}

export const ifoodAuthService = {
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
