<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useIngredientStore } from '@/stores/ingredientStore'
import type { IngredientRequest, IngredientResponse } from '@/types/Ingredient'

const store = useIngredientStore()

const showModal = ref(false)
const editing = ref<IngredientResponse | null>(null)
const form = ref<IngredientRequest>({ name: '', unit: '', costPerUnit: 0 })
const confirmDeleteId = ref<string | null>(null)

function formatCurrency(value: number | null | undefined): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    value ?? 0,
  )
}

function statusLabel(status: string): string {
  return status === 'ACTIVE' ? 'Ativo' : 'Inativo'
}

function statusClass(status: string): string {
  return status === 'ACTIVE' ? 'badge badge-active' : 'badge badge-inactive'
}

function openCreateModal() {
  editing.value = null
  form.value = { name: '', unit: '', costPerUnit: 0 }
  showModal.value = true
}

function openEditModal(ingredient: IngredientResponse) {
  editing.value = ingredient
  form.value = {
    name: ingredient.name,
    unit: ingredient.unit,
    costPerUnit: ingredient.costPerUnit,
  }
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

onMounted(() => {
  store.fetchAll()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Ingredientes</h1>
      <button class="btn btn-primary" @click="openCreateModal">+ Novo Ingrediente</button>
    </div>

    <div v-if="store.error" class="alert alert-error">{{ store.error }}</div>

    <div v-if="store.loading" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="store.items.length === 0" class="empty-state">
      <p>Nenhum ingrediente cadastrado.</p>
      <button class="btn btn-primary" @click="openCreateModal">
        Cadastrar primeiro ingrediente
      </button>
    </div>

    <div v-else class="table-container">
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Unidade</th>
            <th>Custo/Unidade</th>
            <th>Status</th>
            <th style="width: 150px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ingredient in store.items" :key="ingredient.id">
            <td>{{ ingredient.name }}</td>
            <td>{{ ingredient.unit }}</td>
            <td>{{ formatCurrency(ingredient.costPerUnit) }}</td>
            <td>
              <span :class="statusClass(ingredient.status)">
                {{ statusLabel(ingredient.status) }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button class="btn btn-secondary btn-sm" @click="openEditModal(ingredient)">
                  Editar
                </button>
                <button class="btn btn-danger btn-sm" @click="confirmDelete(ingredient.id)">
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
          <h2>{{ editing ? 'Editar Ingrediente' : 'Novo Ingrediente' }}</h2>
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
                placeholder="Nome do ingrediente"
                required
              />
            </div>
            <div class="form-group">
              <label>Unidade</label>
              <input
                v-model="form.unit"
                type="text"
                class="form-control"
                placeholder="Ex: kg, L, un"
                required
              />
            </div>
            <div class="form-group">
              <label>Custo por Unidade (R$)</label>
              <input
                v-model.number="form.costPerUnit"
                type="number"
                step="0.01"
                min="0.01"
                class="form-control"
                placeholder="0,00"
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
          <p>Tem certeza que deseja excluir este ingrediente?</p>
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

