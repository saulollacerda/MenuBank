<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getActivePinia } from 'pinia'
import { useNotificationStore } from '@/stores/notificationStore'
import { UI, UIIcon, UIBtn, UIPill } from '@/design'

// Defensive bootstrap so the topbar can render in test mounts that don't
// install a router or pinia (e.g. unit tests that mount a single view).
const router = useRouter() ?? null
const pinia = getActivePinia()
const inert = !pinia
const store = inert
  ? ({
      items: [] as Array<{
        id: string
        type: string
        title: string
        message: string
        referenceDisplay: string | null
        status: string
        createdAt: string
      }>,
      unreadCount: 0,
      loading: false,
      fetchAll: async () => {},
      refreshCount: () => {},
      markRead: async (_id: string) => {},
      dismiss: async (_id: string) => {},
    } as unknown as ReturnType<typeof useNotificationStore>)
  : useNotificationStore()

const open = ref(false)

async function toggle() {
  open.value = !open.value
  if (open.value) await store.fetchAll()
}
function close() {
  open.value = false
}

async function handleAction(id: string, referenceDisplay: string | null) {
  await store.markRead(id)
  close()
  if (!router) return
  if (referenceDisplay) {
    router.push({ name: 'ingredients', query: { createName: referenceDisplay } })
  } else {
    router.push({ name: 'ingredients' })
  }
}
async function handleDismiss(id: string) {
  await store.dismiss(id)
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR')
}

const unread = computed(() => store.items.filter((n) => n.status === 'UNREAD').length)

onMounted(() => {
  store.refreshCount()
})
</script>

<template>
  <div style="position: relative">
    <button
      type="button"
      class="bell-button"
      data-testid="notification-bell-button"
      :aria-label="`Notificações (${store.unreadCount} não lidas)`"
      :style="{
        position: 'relative',
        width: '36px',
        height: '36px',
        borderRadius: '9px',
        background: UI.bg,
        border: `1px solid ${UI.border}`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: UI.text,
        cursor: 'pointer',
        padding: 0,
      }"
      @click="toggle"
    >
      <UIIcon name="bell" :size="16" />
      <span
        v-if="store.unreadCount > 0"
        data-testid="notification-bell-badge"
        :style="{
          position: 'absolute',
          top: '-4px',
          right: '-4px',
          minWidth: '18px',
          height: '18px',
          borderRadius: '9px',
          background: UI.rose,
          color: '#fff',
          fontSize: '10px',
          fontWeight: 700,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '0 4px',
        }"
      >
        {{ store.unreadCount > 99 ? '99+' : store.unreadCount }}
      </span>
    </button>

    <div
      v-if="open"
      :style="{ position: 'fixed', inset: 0, background: 'transparent', zIndex: 100 }"
      @click.self="close"
    >
      <div
        data-testid="notification-bell-dropdown"
        :style="{
          position: 'fixed',
          top: '70px',
          right: '32px',
          width: '420px',
          maxHeight: '70vh',
          background: UI.panel,
          border: `1px solid ${UI.border}`,
          borderRadius: '14px',
          boxShadow: '0 20px 60px rgba(15,23,42,0.18)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }"
        @click.stop
      >
        <div
          :style="{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderBottom: `1px solid ${UI.border}`,
          }"
        >
          <div style="display: flex; align-items: center; gap: 10px">
            <div
              :style="{
                fontSize: '15px',
                fontWeight: 700,
                color: UI.text,
                letterSpacing: '-0.2px',
              }"
            >
              Notificações
            </div>
            <UIPill v-if="unread > 0" color="rose" size="sm">{{ unread }} novas</UIPill>
          </div>
          <div
            :style="{
              width: '28px',
              height: '28px',
              borderRadius: '7px',
              background: UI.bg,
              color: UI.textSub,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }"
            @click="close"
          >
            <UIIcon name="x" :size="13" />
          </div>
        </div>

        <div style="flex: 1; overflow: auto">
          <div
            v-if="store.loading"
            :style="{
              padding: '24px',
              textAlign: 'center',
              color: UI.textMute,
              fontSize: '13px',
            }"
          >
            Carregando…
          </div>
          <div
            v-else-if="!store.items.length"
            :style="{
              padding: '32px 24px',
              textAlign: 'center',
              color: UI.textMute,
              fontSize: '13px',
            }"
          >
            Nenhuma notificação.
          </div>
          <div
            v-for="(n, i) in store.items"
            v-else
            :key="n.id"
            :data-testid="`notification-item-${n.id}`"
            :style="{
              padding: '14px 20px',
              borderBottom: i === store.items.length - 1 ? 'none' : `1px solid ${UI.borderSub}`,
              background: n.status === 'UNREAD' ? UI.blueBg + '60' : 'transparent',
              position: 'relative',
            }"
          >
            <div
              v-if="n.status === 'UNREAD'"
              :style="{
                position: 'absolute',
                left: '8px',
                top: '22px',
                width: '6px',
                height: '6px',
                borderRadius: '3px',
                background: UI.blue,
              }"
            />
            <div style="display: flex; align-items: flex-start; gap: 12px">
              <div
                :style="{
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  background: UI.amberBg,
                  color: UI.amber,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }"
              >
                <UIIcon name="alert" :size="15" />
              </div>
              <div style="flex: 1; min-width: 0">
                <div
                  :style="{
                    fontSize: '13.5px',
                    fontWeight: 600,
                    color: UI.text,
                    marginBottom: '4px',
                    lineHeight: 1.3,
                  }"
                >
                  {{ n.title }}
                </div>
                <div :style="{ fontSize: '12.5px', color: UI.textSub, lineHeight: 1.5 }">
                  {{ n.message }}
                </div>
                <div
                  :style="{
                    fontSize: '11px',
                    color: UI.textMute,
                    marginTop: '6px',
                    fontVariantNumeric: 'tabular-nums',
                  }"
                >
                  {{ formatDate(n.createdAt) }}
                </div>
                <div
                  v-if="n.type === 'MISSING_INGREDIENT' && n.status !== 'RESOLVED'"
                  style="display: flex; gap: 6px; margin-top: 10px"
                >
                  <UIBtn
                    size="sm"
                    variant="primary"
                    :data-testid="`notification-${n.id}-action-button`"
                    @click="handleAction(n.id, n.referenceDisplay)"
                  >
                    Cadastrar ingrediente
                  </UIBtn>
                  <UIBtn
                    size="sm"
                    variant="ghost"
                    :data-testid="`notification-${n.id}-dismiss-button`"
                    @click="handleDismiss(n.id)"
                  >
                    Descartar
                  </UIBtn>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bell-button {
  transition: background 0.12s ease, border-color 0.12s ease;
}
.bell-button:hover {
  border-color: #94a3b8;
}
</style>
