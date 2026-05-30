<script setup lang="ts">
import { useAuthStore } from '@/stores/authStore'
import UISidebar from '@/design/UISidebar.vue'

const authStore = useAuthStore()
</script>

<template>
  <div v-if="authStore.isAuthenticated" class="app-layout">
    <UISidebar />
    <main class="main-content">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
</template>

<style scoped></style>
