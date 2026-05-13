<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import type { LoginRequest } from '@/types/Auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref<LoginRequest>({ email: '', password: '' })

async function handleSubmit() {
  try {
    await authStore.login(form.value)
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
        <p class="auth-subtitle">Faça login para acessar o sistema</p>
      </div>

      <div v-if="authStore.error" class="alert alert-error">
        {{ authStore.error }}
      </div>

      <form @submit.prevent="handleSubmit">
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
            placeholder="Sua senha"
            required
          />
        </div>

        <button type="submit" class="btn btn-primary auth-btn" :disabled="authStore.loading">
          <span v-if="authStore.loading" class="spinner spinner-sm"></span>
          <span v-else>Entrar</span>
        </button>
      </form>

      <p class="auth-footer">
        Não tem uma conta?
        <RouterLink to="/register" class="auth-link">Cadastre-se</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped></style>

