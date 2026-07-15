import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { LoginRequest } from '@/types/Auth'
import type { UserRequest, UserResponse } from '@/types/User'
import { authProvider, AuthError, type AuthSession } from '@/lib/authProvider'
import { authService } from '@/services/authService'
import { userService } from '@/services/userService'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(null)
  const currentUser = ref<UserResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const awaitingEmailConfirmation = ref(false)
  const passwordResetEmailSent = ref(false)
  // Single-flight: the router guard and main.ts both await init(); concurrent
  // callers must share the in-flight restore instead of resolving early.
  let initPromise: Promise<void> | null = null

  const isAuthenticated = computed(() => !!session.value)
  const restaurantName = computed(() => currentUser.value?.merchantName ?? '')

  /** Loads the persisted session and subscribes to auth changes. Idempotent. */
  function init(): Promise<void> {
    if (!initPromise) initPromise = doInit()
    return initPromise
  }

  async function doInit() {
    session.value = await authProvider.init()
    authProvider.onAuthChange(async (newSession) => {
      const wasAuthenticated = !!session.value
      session.value = newSession
      if (!newSession) {
        currentUser.value = null
      } else if (!wasAuthenticated) {
        // New sign-in via external event (e.g. email confirmation redirect): provision
        // and load the merchant just like a manual login would.
        try {
          await ensureProvisionedAndLoad()
        } catch {
          // best-effort — views can retry via fetchCurrentUser
        }
      }
    })
    // Page refresh with an existing session: load the merchant so restaurantName etc.
    // are available. Best-effort — never block app mount on a network error.
    if (session.value) {
      try {
        await ensureProvisionedAndLoad()
      } catch {
        // ignored — views can retry via fetchCurrentUser
      }
    }
  }

  async function login(request: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      session.value = await authProvider.signIn(request.email, request.password)
      await ensureProvisionedAndLoad()
    } catch (e: unknown) {
      if (e instanceof AuthError) {
        error.value =
          e.code === 'email_not_confirmed'
            ? 'Confirme seu email antes de entrar.'
            : e.code === 'invalid_credentials'
              ? 'Email ou senha inválidos'
              : 'Erro ao entrar. Tente novamente.'
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  async function register(request: UserRequest) {
    loading.value = true
    error.value = null
    awaitingEmailConfirmation.value = false
    try {
      const { session: newSession } = await authProvider.signUp({
        email: request.email,
        password: request.password,
        merchantName: request.merchantName,
        // Digits only: the backend duplicate check compares raw strings, so a
        // masked CNPJ would slip past it.
        cnpj: request.cnpj.replace(/\D/g, ''),
        phone: request.phone ?? null,
      })
      if (newSession) {
        // No email confirmation (local dev): session is available immediately.
        session.value = newSession
        await ensureProvisionedAndLoad()
      } else {
        // Email confirmation ON (Supabase): no session yet. Provision happens on first login.
        awaitingEmailConfirmation.value = true
      }
    } catch (e: unknown) {
      if (e instanceof AuthError) {
        error.value =
          e.code === 'email_exists'
            ? 'Email já cadastrado'
            : 'Erro ao criar conta. Tente novamente.'
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  // Single-flight: login() and the onAuthChange callback can race here (the local
  // provider notifies listeners synchronously during signIn); concurrent getMe(403)
  // + provision calls would hit the backend's check-then-insert twice.
  let ensureInFlight: Promise<UserResponse> | null = null

  function ensureProvisionedAndLoad(): Promise<UserResponse> {
    if (!ensureInFlight) {
      ensureInFlight = doEnsureProvisionedAndLoad().finally(() => {
        ensureInFlight = null
      })
    }
    return ensureInFlight
  }

  /**
   * Loads the current merchant. On the first authenticated access the merchant does
   * not exist yet (backend returns 403), so we provision it from the business data
   * stored in the Supabase user metadata, then load it.
   */
  async function doEnsureProvisionedAndLoad(): Promise<UserResponse> {
    try {
      currentUser.value = await userService.getMe()
      return currentUser.value
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } }).response?.status
      if (status !== 403) {
        error.value = 'Erro ao carregar perfil'
        throw e
      }
      const user = session.value?.user
      const meta = (user?.user_metadata ?? {}) as {
        merchantName?: string
        cnpj?: string
        phone?: string | null
      }
      try {
        await authService.provision({
          merchantName: meta.merchantName ?? '',
          cnpj: meta.cnpj ?? '',
          email: user?.email ?? '',
          phone: meta.phone ?? undefined,
        })
      } catch (provisionError: unknown) {
        const provisionStatus = (provisionError as { response?: { status?: number } }).response
          ?.status
        error.value =
          provisionStatus === 409
            ? 'Já existe um cadastro com este email ou CNPJ.'
            : 'Erro ao concluir seu cadastro. Tente novamente.'
        throw provisionError
      }
      currentUser.value = await userService.getMe()
      return currentUser.value
    }
  }

  async function requestPasswordReset(email: string) {
    loading.value = true
    error.value = null
    passwordResetEmailSent.value = false
    try {
      await authProvider.requestPasswordReset(email)
      passwordResetEmailSent.value = true
    } catch (e: unknown) {
      if (e instanceof AuthError) {
        error.value =
          e.code === 'not_supported'
            ? 'Recuperação de senha não está disponível no ambiente de desenvolvimento.'
            : 'Erro ao enviar o email de recuperação. Tente novamente.'
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updatePassword(password: string) {
    loading.value = true
    error.value = null
    try {
      await authProvider.updatePassword(password)
    } catch (e: unknown) {
      error.value = 'Erro ao redefinir a senha. Tente novamente.'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    await authProvider.signOut()
    session.value = null
    currentUser.value = null
  }

  async function fetchCurrentUser() {
    if (!isAuthenticated.value) return null
    loading.value = true
    error.value = null
    try {
      currentUser.value = await userService.getMe()
      return currentUser.value
    } catch (e: unknown) {
      error.value = 'Erro ao carregar perfil'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateAnotaAIKey(key: string | null) {
    loading.value = true
    error.value = null
    try {
      const response = await userService.updateAnotaAIKey({ anotaAiApiKey: key })
      currentUser.value = response
      return response
    } catch (e: unknown) {
      error.value = 'Erro ao salvar a chave do Anota.AI'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateOpeningHours(hours: import('@/types/User').OpeningHour[]) {
    loading.value = true
    error.value = null
    try {
      const response = await userService.updateMe({ openingHours: hours })
      currentUser.value = response
      return response
    } catch (e: unknown) {
      error.value = 'Erro ao salvar os horários'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    session,
    currentUser,
    loading,
    error,
    awaitingEmailConfirmation,
    passwordResetEmailSent,
    isAuthenticated,
    restaurantName,
    init,
    login,
    register,
    requestPasswordReset,
    updatePassword,
    logout,
    fetchCurrentUser,
    updateAnotaAIKey,
    updateOpeningHours,
  }
})
