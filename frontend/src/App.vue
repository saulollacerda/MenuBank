<script setup lang="ts">
import SidebarNav from '@/components/layout/SidebarNav.vue'
</script>

<template>
  <div class="app-layout">
    <SidebarNav />
    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped></style>
