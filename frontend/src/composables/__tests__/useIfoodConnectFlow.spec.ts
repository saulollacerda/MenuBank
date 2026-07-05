import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  useIfoodConnectFlow,
  readPendingIfoodAuth,
  hasPendingIfoodAuth,
  clearPendingIfoodAuth,
  IFOOD_PENDING_KEY,
} from '@/composables/useIfoodConnectFlow'
import { ifoodAuthService } from '@/services/ifoodAuthService'

vi.mock('@/services/ifoodAuthService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/ifoodAuthService')>()
  return {
    ...original,
    ifoodAuthService: {
      status: vi.fn(),
      start: vi.fn(),
      connect: vi.fn(),
      revoke: vi.fn(),
    },
  }
})

const mockedService = vi.mocked(ifoodAuthService)

const START_RESPONSE = {
  userCode: 'HJLX-LPSQ',
  verificationUrl: 'https://portal.ifood.com.br/apps/code',
  verificationUrlComplete: 'https://portal.ifood.com.br/apps/code?c=HJLX-LPSQ',
  expiresIn: 600,
}

function seedPending(expiresAt: number) {
  sessionStorage.setItem(
    IFOOD_PENDING_KEY,
    JSON.stringify({
      v: 1,
      userCode: 'OLD-CODE',
      verificationUrl: 'https://portal.ifood.com.br/apps/code',
      verificationUrlComplete: 'https://portal.ifood.com.br/apps/code?c=OLD-CODE',
      expiresAt,
    }),
  )
}

describe('useIfoodConnectFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-04T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('start() popula o estado, persiste no sessionStorage e arma o countdown', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    const flow = useIfoodConnectFlow()

    await flow.start()

    expect(flow.step.value).toBe('code')
    expect(flow.userCode.value).toBe('HJLX-LPSQ')
    expect(flow.verificationUrlComplete.value).toBe(START_RESPONSE.verificationUrlComplete)
    expect(flow.remainingSeconds.value).toBe(600)

    const stored = readPendingIfoodAuth()
    expect(stored).not.toBeNull()
    expect(stored?.userCode).toBe('HJLX-LPSQ')
    expect(stored?.expiresAt).toBe(Date.now() + 600_000)
  })

  it('start() com falha define erro e não persiste nada', async () => {
    mockedService.start.mockRejectedValue(new Error('boom'))
    const flow = useIfoodConnectFlow()

    await flow.start()

    expect(flow.error.value).toContain('Tente novamente')
    expect(hasPendingIfoodAuth()).toBe(false)
  })

  it('countdown decresce por tick e vira expired em 0 sem auto-renovar', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    const flow = useIfoodConnectFlow()
    await flow.start()

    await vi.advanceTimersByTimeAsync(1000)
    expect(flow.remainingSeconds.value).toBe(599)

    await vi.advanceTimersByTimeAsync(600_000)
    expect(flow.step.value).toBe('expired')
    expect(flow.remainingSeconds.value).toBe(0)
    // sem auto-renew silencioso: start só foi chamado 1 vez
    expect(mockedService.start).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(60_000)
    expect(mockedService.start).toHaveBeenCalledTimes(1)
  })

  it('countdown é derivado do timestamp absoluto (sem drift)', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    const flow = useIfoodConnectFlow()
    await flow.start()

    // simula aba em background: relógio avança 90s mas só ocorre 1 tick
    vi.setSystemTime(new Date('2026-07-04T12:01:30Z'))
    await vi.advanceTimersByTimeAsync(1000)

    expect(flow.remainingSeconds.value).toBeLessThanOrEqual(509)
  })

  it('resume() com registro válido restaura código e tempo restante', async () => {
    seedPending(Date.now() + 120_000)
    const flow = useIfoodConnectFlow()

    const resumed = flow.resume()

    expect(resumed).toBe(true)
    expect(flow.step.value).toBe('code')
    expect(flow.userCode.value).toBe('OLD-CODE')
    expect(flow.remainingSeconds.value).toBe(120)
    expect(mockedService.start).not.toHaveBeenCalled()
  })

  it('resume() com registro expirado abre em expired mantendo o storage', () => {
    seedPending(Date.now() - 1000)
    const flow = useIfoodConnectFlow()

    const resumed = flow.resume()

    expect(resumed).toBe(true)
    expect(flow.step.value).toBe('expired')
    expect(flow.userCode.value).toBe('OLD-CODE')
    expect(hasPendingIfoodAuth()).toBe(true)
  })

  it('resume() com JSON corrompido limpa a chave e retorna false', () => {
    sessionStorage.setItem(IFOOD_PENDING_KEY, '{not-json')
    const flow = useIfoodConnectFlow()

    expect(flow.resume()).toBe(false)
    expect(sessionStorage.getItem(IFOOD_PENDING_KEY)).toBeNull()
  })

  it('connect() com sucesso vai para done, limpa storage e envia o código trimado', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    mockedService.connect.mockResolvedValue()
    const flow = useIfoodConnectFlow()
    await flow.start()
    flow.authCode.value = '  auth-123  '

    await flow.connect()

    expect(mockedService.connect).toHaveBeenCalledWith('auth-123')
    expect(flow.step.value).toBe('done')
    expect(hasPendingIfoodAuth()).toBe(false)
  })

  it('connect() com 409 mostra orientação de código expirado; 400 mostra código inválido', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    const flow = useIfoodConnectFlow()
    await flow.start()
    flow.authCode.value = 'auth-123'

    mockedService.connect.mockRejectedValue({ response: { status: 409 } })
    await flow.connect()
    expect(flow.error.value).toContain('novo código')
    expect(flow.step.value).toBe('code')

    mockedService.connect.mockRejectedValue({ response: { status: 400 } })
    await flow.connect()
    expect(flow.error.value).toContain('inválido')
  })

  it('regenerate() após expirar chama start de novo e rearma o fluxo', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    const flow = useIfoodConnectFlow()
    await flow.start()
    await vi.advanceTimersByTimeAsync(601_000)
    expect(flow.step.value).toBe('expired')

    await flow.regenerate()

    expect(mockedService.start).toHaveBeenCalledTimes(2)
    expect(flow.step.value).toBe('code')
    expect(readPendingIfoodAuth()?.expiresAt).toBe(Date.now() + 600_000)
  })

  it('finish() limpa storage e encerra o fluxo', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    const flow = useIfoodConnectFlow()
    await flow.start()

    flow.finish()

    expect(hasPendingIfoodAuth()).toBe(false)
    expect(flow.flowInProgress.value).toBe(false)
  })

  it('flowInProgress é true em code/expired e false em loading/done', async () => {
    mockedService.start.mockResolvedValue(START_RESPONSE)
    mockedService.connect.mockResolvedValue()
    const flow = useIfoodConnectFlow()

    expect(flow.flowInProgress.value).toBe(false) // loading inicial

    await flow.start()
    expect(flow.flowInProgress.value).toBe(true) // code

    await vi.advanceTimersByTimeAsync(601_000)
    expect(flow.flowInProgress.value).toBe(true) // expired

    await flow.regenerate()
    flow.authCode.value = 'x'
    await flow.connect()
    expect(flow.flowInProgress.value).toBe(false) // done
  })

  it('clearPendingIfoodAuth remove o registro', () => {
    seedPending(Date.now() + 120_000)

    clearPendingIfoodAuth()

    expect(hasPendingIfoodAuth()).toBe(false)
  })
})
