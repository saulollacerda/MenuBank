<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import NotificationBell from '@/components/NotificationBell.vue'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="sidebar-logo">
        Menu<span>Bank</span>
      </div>
      <NotificationBell />
    </div>
    <nav class="sidebar-nav">
      <RouterLink to="/">
        <span class="nav-icon">📊</span>
        <span>Dashboard</span>
      </RouterLink>
      <RouterLink to="/orders">
        <span class="nav-icon">🛒</span>
        <span>Pedidos</span>
      </RouterLink>
      <RouterLink to="/products">
        <span class="nav-icon">🍔</span>
        <span>Produtos</span>
      </RouterLink>
      <RouterLink to="/categories">
        <span class="nav-icon">📁</span>
        <span>Categorias</span>
      </RouterLink>
      <RouterLink to="/ingredients">
        <span class="nav-icon">🥬</span>
        <span>Ingredientes</span>
      </RouterLink>
      <RouterLink to="/customers">
        <span class="nav-icon">👤</span>
        <span>Clientes</span>
      </RouterLink>
      <RouterLink to="/payment-methods">
        <span class="nav-icon">💳</span>
        <span>Formas de Pagamento</span>
      </RouterLink>
      <RouterLink to="/settings">
        <span class="nav-icon">⚙️</span>
        <span>Configurações</span>
      </RouterLink>
    </nav>
    <div class="sidebar-footer">
      <div class="sidebar-user">
        <span class="nav-icon">🏪</span>
        <span class="sidebar-restaurant-name">{{ authStore.restaurantName }}</span>
      </div>
      <button class="sidebar-logout" @click="handleLogout">
        <span class="nav-icon">🚪</span>
        <span>Sair</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1rem;
}
</style>
