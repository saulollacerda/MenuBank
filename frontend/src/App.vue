<script setup lang="ts">
import { useAuthStore } from '@/stores/authStore'
import SidebarNav from '@/components/layout/SidebarNav.vue'

const authStore = useAuthStore()
</script>

<template>
  <div v-if="authStore.isAuthenticated" class="app-layout">
    <SidebarNav />
    <main class="main-content">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
</template>

<style scoped></style>
