<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import type { UserRequest } from '@/types/User'

const router = useRouter()
const authStore = useAuthStore()

const validationError = ref<string | null>(null)

const form = ref<UserRequest>({
  merchantName: '',
  cnpj: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
})

function isValidCnpj(value: string) {
  const digits = value.replace(/\D/g, '')
  if (digits.length !== 14) return false
  if (/^(\d)\1{13}$/.test(digits)) return false

  const weightsFirst = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
  const weightsSecond = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]

  const calculateCheckDigit = (base: string, weights: number[]) => {
    const sum = base
      .split('')
      .reduce((total, digit, index) => total + Number(digit) * (weights[index] ?? 0), 0)
    const remainder = sum % 11
    return remainder < 2 ? 0 : 11 - remainder
  }

  const firstCheckDigit = calculateCheckDigit(digits.slice(0, 12), weightsFirst)
  const secondCheckDigit = calculateCheckDigit(digits.slice(0, 12) + firstCheckDigit, weightsSecond)

  return digits[12] === String(firstCheckDigit) && digits[13] === String(secondCheckDigit)
}

async function handleSubmit() {
  validationError.value = null

  if (!isValidCnpj(form.value.cnpj)) {
    validationError.value = 'CNPJ inválido'
    return
  }

  if (form.value.password !== form.value.confirmPassword) {
    validationError.value = 'As senhas não conferem'
    return
  }

  try {
    await authStore.register(form.value)
    router.push('/')
  } catch {
    // error is handled in the store
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-logo">Menu<span>Bank</span></h1>
        <p class="auth-subtitle">Crie sua conta para começar</p>
      </div>

      <div v-if="validationError" class="alert alert-error">
        {{ validationError }}
      </div>

      <div v-if="authStore.error" class="alert alert-error">
        {{ authStore.error }}
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="merchantName">Nome do Restaurante</label>
          <input
            id="merchantName"
            v-model="form.merchantName"
            type="text"
            class="form-control"
            placeholder="Ex: Pizzaria Napoli"
            required
          />
        </div>

        <div class="form-group">
          <label for="cnpj">CNPJ</label>
          <input
            id="cnpj"
            v-model="form.cnpj"
            type="text"
            class="form-control"
            placeholder="00000000000000"
            maxlength="14"
            inputmode="numeric"
            required
          />
        </div>

        <div class="form-group">
          <label for="email">Email</label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            class="form-control"
            placeholder="seu@email.com"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">Senha</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            class="form-control"
            placeholder="Mínimo 6 caracteres"
            minlength="6"
            required
          />
        </div>

        <div class="form-group">
          <label for="confirmPassword">Confirmar Senha</label>
          <input
            id="confirmPassword"
            v-model="form.confirmPassword"
            type="password"
            class="form-control"
            placeholder="Repita sua senha"
            minlength="6"
            required
          />
        </div>

        <div class="form-group">
          <label for="phone">Telefone <span class="optional">(opcional)</span></label>
          <input
            id="phone"
            v-model="form.phone"
            type="tel"
            class="form-control"
            placeholder="(11) 99999-9999"
          />
        </div>

        <button type="submit" class="btn btn-primary auth-btn" :disabled="authStore.loading">
          <span v-if="authStore.loading" class="spinner spinner-sm"></span>
          <span v-else>Criar Conta</span>
        </button>
      </form>

      <p class="auth-footer">
        Já tem uma conta?
        <RouterLink to="/login" class="auth-link">Faça login</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.optional {
  font-weight: 400;
  color: var(--color-gray-400);
  font-size: 0.8125rem;
}
</style>
