<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-logo">
      Menu<span>Bank</span>
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
    </nav>
  </aside>
</template>

<style scoped></style>

