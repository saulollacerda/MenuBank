<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notificationStore'

const router = useRouter()
const store = useNotificationStore()

const open = ref(false)

async function toggle() {
  open.value = !open.value
  if (open.value) {
    await store.fetchAll()
  }
}

function close() {
  open.value = false
}

async function handleAction(id: string, referenceDisplay: string | null) {
  await store.markRead(id)
  close()
  if (referenceDisplay) {
    router.push({ name: 'ingredients', query: { createName: referenceDisplay } })
  } else {
    router.push({ name: 'ingredients' })
  }
}

async function handleDismiss(id: string) {
  await store.dismiss(id)
}

function formatDate(isoString: string): string {
  return new Date(isoString).toLocaleString('pt-BR')
}

onMounted(() => {
  store.refreshCount()
})
</script>

<template>
  <div class="notification-bell">
    <button
      type="button"
      class="bell-button"
      data-testid="notification-bell-button"
      :aria-label="`Notificações (${store.unreadCount} não lidas)`"
      @click="toggle"
    >
      <span class="bell-icon">🔔</span>
      <span
        v-if="store.unreadCount > 0"
        class="bell-badge"
        data-testid="notification-bell-badge"
      >
        {{ store.unreadCount > 99 ? '99+' : store.unreadCount }}
      </span>
    </button>

    <div v-if="open" class="bell-overlay" @click.self="close">
      <div class="bell-dropdown" data-testid="notification-bell-dropdown">
        <div class="bell-header">
          <strong>Notificações</strong>
          <button class="bell-close" @click="close">✕</button>
        </div>
        <div v-if="store.loading" class="bell-loading">Carregando...</div>
        <div v-else-if="store.items.length === 0" class="bell-empty">
          Nenhuma notificação.
        </div>
        <ul v-else class="bell-list">
          <li
            v-for="n in store.items"
            :key="n.id"
            class="bell-item"
            :class="{ 'bell-item-unread': n.status === 'UNREAD' }"
            :data-testid="`notification-item-${n.id}`"
          >
            <div class="bell-item-title">{{ n.title }}</div>
            <div class="bell-item-message">{{ n.message }}</div>
            <div class="bell-item-meta">{{ formatDate(n.createdAt) }}</div>
            <div class="bell-item-actions">
              <button
                v-if="n.type === 'MISSING_INGREDIENT' && n.status !== 'RESOLVED'"
                type="button"
                class="btn btn-primary btn-sm"
                :data-testid="`notification-${n.id}-action-button`"
                @click="handleAction(n.id, n.referenceDisplay)"
              >
                Cadastrar ingrediente
              </button>
              <button
                type="button"
                class="btn btn-secondary btn-sm"
                :data-testid="`notification-${n.id}-dismiss-button`"
                @click="handleDismiss(n.id)"
              >
                Descartar
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-bell {
  position: relative;
}

.bell-button {
  position: relative;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bell-icon {
  font-size: 1.25rem;
}

.bell-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #dc2626;
  color: #fff;
  border-radius: 999px;
  font-size: 0.625rem;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.bell-overlay {
  position: fixed;
  inset: 0;
  background: transparent;
  z-index: 100;
}

.bell-dropdown {
  position: absolute;
  top: 48px;
  right: 0;
  width: 360px;
  max-height: 480px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.bell-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.bell-close {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 0.875rem;
  color: #64748b;
}

.bell-loading,
.bell-empty {
  padding: 1.5rem 1rem;
  text-align: center;
  color: #64748b;
}

.bell-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
}

.bell-item {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.bell-item-unread {
  background: #eff6ff;
}

.bell-item-title {
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.bell-item-message {
  font-size: 0.8125rem;
  color: #475569;
  margin-bottom: 0.5rem;
}

.bell-item-meta {
  font-size: 0.6875rem;
  color: #94a3b8;
  margin-bottom: 0.5rem;
}

.bell-item-actions {
  display: flex;
  gap: 0.5rem;
}
</style>
