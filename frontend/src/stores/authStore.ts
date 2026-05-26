import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { LoginRequest, LoginResponse } from '@/types/Auth'
import type { UserRequest, UserResponse } from '@/types/User'
import { authService } from '@/services/authService'
import { userService } from '@/services/userService'

const TOKEN_KEY = 'menubank_token'
const USER_KEY = 'menubank_user'

interface StoredUser {
  merchantId: string
  email: string
  merchantName: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<StoredUser | null>(loadUser())
  const currentUser = ref<UserResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const restaurantName = computed(() => user.value?.merchantName ?? '')

  function loadUser(): StoredUser | null {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as StoredUser
    } catch {
      return null
    }
  }

  function saveSession(response: LoginResponse) {
    token.value = response.token
    user.value = {
      merchantId: response.merchantId,
      email: response.email,
      merchantName: response.merchantName,
    }
    localStorage.setItem(TOKEN_KEY, response.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function clearSession() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function login(request: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authService.login(request)
      saveSession(response)
      return response
    } catch (e: unknown) {
      const err = e as { response?: { status?: number } }
      if (err.response?.status === 401) {
        error.value = 'Email ou senha inválidos'
      } else if (err.response?.status === 403) {
        error.value = 'Conta de usuário inativa'
      } else {
        error.value = 'Erro ao fazer login. Tente novamente.'
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  async function register(request: UserRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authService.register(request)
      saveSession(response)
      return response
    } catch (e: unknown) {
      const err = e as { response?: { status?: number } }
      if (err.response?.status === 409) {
        error.value = 'Email ou CNPJ já cadastrado'
      } else {
        error.value = 'Erro ao criar conta. Tente novamente.'
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  function logout() {
    clearSession()
    currentUser.value = null
  }

  async function fetchCurrentUser() {
    if (!user.value) return null
    loading.value = true
    error.value = null
    try {
      const response = await userService.getById(user.value.merchantId)
      currentUser.value = response
      return response
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

  return {
    token,
    user,
    currentUser,
    loading,
    error,
    isAuthenticated,
    restaurantName,
    login,
    register,
    logout,
    clearSession,
    fetchCurrentUser,
    updateAnotaAIKey,
  }
})

