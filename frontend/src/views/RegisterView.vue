<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import type { UserRequest } from '@/types/User'

const router = useRouter()
const authStore = useAuthStore()

const form = ref<UserRequest>({
  restaurantName: '',
  cnpj: '',
  email: '',
  password: '',
  phone: '',
})

async function handleSubmit() {
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

      <div v-if="authStore.error" class="alert alert-error">
        {{ authStore.error }}
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="restaurantName">Nome do Restaurante</label>
          <input
            id="restaurantName"
            v-model="form.restaurantName"
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

