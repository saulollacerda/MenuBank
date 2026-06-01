<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const NAV = [
  { id: 'dashboard', to: '/dashboard', ic: 'home', label: 'Dashboard' },
  { id: 'orders', to: '/orders', ic: 'cart', label: 'Pedidos' },
  { id: 'products', to: '/products', ic: 'burger', label: 'Produtos' },
  { id: 'categories', to: '/categories', ic: 'folder', label: 'Categorias' },
  { id: 'ingredients', to: '/ingredients', ic: 'leaf', label: 'Ingredientes' },
  { id: 'customers', to: '/customers', ic: 'user', label: 'Clientes' },
  { id: 'fees', to: '/fees', ic: 'card', label: 'Taxas' },
  { id: 'settings', to: '/settings', ic: 'gear', label: 'Configurações' },
]

const activeId = computed(() => {
  const path = route.path
  if (path === '/dashboard') return 'dashboard'
  const match = NAV.find((n) => path.startsWith(n.to))
  return match?.id ?? 'dashboard'
})

const initials = computed(() => {
  const name = auth.restaurantName || 'MB'
  const parts = name.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || 'MB'
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <aside
    :style="{
      width: '232px',
      background: UI.navBg,
      color: UI.navText,
      display: 'flex',
      flexDirection: 'column',
      flexShrink: 0,
      borderRight: `1px solid ${UI.navBg2}`,
      position: 'sticky',
      top: 0,
      height: '100vh',
      overflow: 'hidden',
    }"
  >
    <div
      :style="{
        padding: '24px 22px 22px',
        borderBottom: '1px solid #1a2638',
        cursor: 'pointer',
      }"
      @click="router.push('/')"
    >
      <div
        :style="{
          fontSize: '21px',
          fontWeight: 700,
          letterSpacing: '-0.4px',
          color: '#fff',
        }"
      >
        menu<span :style="{ color: UI.emerald }">bank</span>
      </div>
      <div
        :style="{
          fontSize: '11px',
          color: '#64748b',
          marginTop: '3px',
          letterSpacing: '0.2px',
        }"
      >
        gestão · custo · lucro
      </div>
    </div>

    <nav
      style="
        padding: 14px 12px;
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 2px;
      "
    >
      <div
        v-for="it in NAV"
        :key="it.id"
        class="ui-nav"
        :class="{ 'is-active': it.id === activeId }"
        :style="{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '9px 12px',
          borderRadius: '8px',
          fontSize: '13.5px',
          fontWeight: 500,
          background: it.id === activeId ? UI.navActive : 'transparent',
          color: it.id === activeId ? '#fff' : UI.navText,
          cursor: 'pointer',
        }"
        @click="router.push(it.to)"
      >
        <UIIcon :name="it.ic" :size="16" />
        <span style="flex: 1">{{ it.label }}</span>
      </div>
    </nav>

    <div :style="{ padding: '12px', borderTop: '1px solid #1a2638', flexShrink: 0 }">
      <div
        :style="{
          background: '#0a1322',
          padding: '11px 12px',
          borderRadius: '8px',
          display: 'flex',
          alignItems: 'center',
          gap: '11px',
        }"
      >
        <div
          :style="{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            background: 'linear-gradient(135deg,#10b981,#059669)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '14px',
            fontWeight: 700,
            color: '#fff',
            flexShrink: 0,
          }"
        >
          {{ initials }}
        </div>
        <div :style="{ flex: 1, minWidth: 0 }">
          <div :style="{ fontSize: '12.5px', fontWeight: 600, color: '#fff' }">
            {{ auth.restaurantName || 'MenuBank' }}
          </div>
          <div
            :style="{
              fontSize: '11px',
              color: '#64748b',
              display: 'flex',
              alignItems: 'center',
              gap: '5px',
            }"
          >
            <span
              :style="{
                width: '6px',
                height: '6px',
                borderRadius: '3px',
                background: UI.emerald,
              }"
            />
            Loja aberta
          </div>
        </div>
        <button
          class="ui-logout-btn"
          title="Sair"
          @click="logout"
        >
          <UIIcon name="logout" :size="15" />
        </button>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.ui-nav {
  transition: background 0.12s ease, color 0.12s ease;
}
.ui-nav:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.ui-nav.is-active:hover {
  background: #2563eb !important;
}
.ui-logout-btn {
  background: transparent;
  border: none;
  color: #475569;
  cursor: pointer;
  padding: 5px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.12s, color 0.12s;
}
.ui-logout-btn:hover {
  background: rgba(225, 29, 72, 0.15);
  color: #f87171;
}
</style>
