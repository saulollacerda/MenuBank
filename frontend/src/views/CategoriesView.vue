<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCategoryStore } from '@/stores/categoryStore'
import PageControls from '@/components/PageControls.vue'
import type { CategoryRequest, CategoryResponse } from '@/types/Category'

const store = useCategoryStore()

const showModal = ref(false)
const editing = ref<CategoryResponse | null>(null)
const form = ref<CategoryRequest>({ name: '' })
const confirmDeleteId = ref<string | null>(null)

function openCreateModal() {
  editing.value = null
  form.value = { name: '' }
  showModal.value = true
}

function openEditModal(category: CategoryResponse) {
  editing.value = category
  form.value = { name: category.name }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editing.value = null
}

async function handleSubmit() {
  try {
    if (editing.value) {
      await store.update(editing.value.id, form.value)
    } else {
      await store.create(form.value)
    }
    closeModal()
  } catch {
    // Error is handled by the store
  }
}

function confirmDelete(id: string) {
  confirmDeleteId.value = id
}

async function handleDelete() {
  if (!confirmDeleteId.value) return
  try {
    await store.remove(confirmDeleteId.value)
  } catch {
    // Error is handled by the store
  }
  confirmDeleteId.value = null
}

function onSearch(term: string) {
  store.fetchPage({ search: term, page: 0 })
}

function onPageChange(p: number) {
  store.fetchPage({ page: p })
}

onMounted(() => {
  store.fetchPage({ page: 0, search: '' })
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Categorias</h1>
      <button class="btn btn-primary" @click="openCreateModal">+ Nova Categoria</button>
    </div>

    <div v-if="store.error" class="alert alert-error">{{ store.error }}</div>

    <PageControls
      v-model="store.search"
      :page="store.page"
      :total-pages="store.totalPages"
      :total-elements="store.totalElements"
      :loading="store.loading"
      placeholder="Buscar categoria por nome..."
      @search="onSearch"
      @page-change="onPageChange"
    />

    <div v-if="store.loading" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="store.items.length === 0" class="empty-state">
      <p v-if="store.search">Nenhuma categoria encontrada para "{{ store.search }}".</p>
      <template v-else>
        <p>Nenhuma categoria cadastrada.</p>
        <button class="btn btn-primary" @click="openCreateModal">Criar primeira categoria</button>
      </template>
    </div>

    <div v-else class="table-container">
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th style="width: 150px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="category in store.items" :key="category.id">
            <td>{{ category.name }}</td>
            <td>
              <div class="table-actions">
                <button class="btn btn-secondary btn-sm" @click="openEditModal(category)">
                  Editar
                </button>
                <button class="btn btn-danger btn-sm" @click="confirmDelete(category.id)">
                  Excluir
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create/Edit Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ editing ? 'Editar Categoria' : 'Nova Categoria' }}</h2>
          <button class="modal-close" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>Nome</label>
              <input
                v-model="form.name"
                type="text"
                class="form-control"
                placeholder="Nome da categoria"
                required
              />
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModal">
                Cancelar
              </button>
              <button type="submit" class="btn btn-primary" :disabled="store.loading">
                {{ editing ? 'Salvar' : 'Criar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="confirmDeleteId" class="modal-overlay" @click.self="confirmDeleteId = null">
      <div class="modal">
        <div class="modal-header">
          <h2>Confirmar Exclusão</h2>
          <button class="modal-close" @click="confirmDeleteId = null">✕</button>
        </div>
        <div class="modal-body">
          <p>Tem certeza que deseja excluir esta categoria?</p>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="confirmDeleteId = null">Cancelar</button>
            <button class="btn btn-danger" @click="handleDelete">Excluir</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped></style>

