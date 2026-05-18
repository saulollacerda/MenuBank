<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()

const apiKey = ref('')
const showKey = ref(false)
const successMessage = ref<string | null>(null)
const loadError = ref<string | null>(null)

const inputType = computed(() => (showKey.value ? 'text' : 'password'))

async function loadProfile() {
  loadError.value = null
  try {
    const user = await authStore.fetchCurrentUser()
    apiKey.value = user?.anotaAiApiKey ?? ''
  } catch {
    loadError.value = 'Não foi possível carregar suas configurações.'
  }
}

async function handleSubmit() {
  successMessage.value = null
  try {
    const trimmed = apiKey.value.trim()
    await authStore.updateAnotaAIKey(trimmed.length > 0 ? trimmed : null)
    successMessage.value = 'Chave do Anota.AI salva com sucesso.'
  } catch {
    // erro fica em authStore.error
  }
}

function toggleVisibility() {
  showKey.value = !showKey.value
}

onMounted(loadProfile)
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Configurações</h1>
    </div>

    <div v-if="loadError" class="alert alert-error">{{ loadError }}</div>
    <div v-if="authStore.error" class="alert alert-error">{{ authStore.error }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <section class="settings-section">
      <h2>Integração Anota.AI</h2>
      <p class="settings-description">
        Cole aqui o token de integração fornecido pelo Anota.AI. Ele será usado para importar
        pedidos do dia e sincronizar o cardápio (categorias e produtos).
      </p>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="anotaAiApiKey">Token de integração</label>
          <div class="input-with-action">
            <input
              id="anotaAiApiKey"
              :type="inputType"
              class="form-control"
              v-model="apiKey"
              placeholder="Cole o token do Anota.AI"
              autocomplete="off"
            />
            <button
              type="button"
              class="btn btn-secondary btn-sm"
              @click="toggleVisibility"
            >
              {{ showKey ? 'Ocultar' : 'Mostrar' }}
            </button>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="authStore.loading"
          >
            <span v-if="authStore.loading" class="spinner spinner-sm"></span>
            <span v-else>Salvar</span>
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.settings-section {
  background: var(--color-white, #fff);
  border: 1px solid var(--color-gray-200, #e5e7eb);
  border-radius: var(--border-radius, 8px);
  padding: 1.5rem;
  max-width: 720px;
}

.settings-section h2 {
  margin-top: 0;
  margin-bottom: 0.5rem;
  font-size: 1.125rem;
}

.settings-description {
  color: var(--color-gray-600, #4b5563);
  margin-bottom: 1.25rem;
}

.input-with-action {
  display: flex;
  gap: 0.5rem;
  align-items: stretch;
}

.input-with-action .form-control {
  flex: 1;
}

.form-actions {
  margin-top: 1.25rem;
  display: flex;
  justify-content: flex-end;
}
</style>
