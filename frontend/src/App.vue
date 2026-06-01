<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import UISidebar from '@/design/UISidebar.vue'

const authStore = useAuthStore()
const route = useRoute()

const showSidebar = () => authStore.isAuthenticated && !route.meta.public
</script>

<template>
  <div v-if="showSidebar()" class="app-layout">
    <UISidebar />
    <main class="main-content">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
</template>

<style scoped></style>
