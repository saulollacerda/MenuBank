<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useIngredientStore } from '@/stores/ingredientStore'
import type { IngredientRequest, IngredientResponse } from '@/types/Ingredient'

const store = useIngredientStore()

const showModal = ref(false)
const editing = ref<IngredientResponse | null>(null)
const form = ref<IngredientRequest>({ name: '', unit: '', costPerUnit: 0, defaultQuantity: 0 })
const confirmDeleteId = ref<string | null>(null)

const autoCalcCost = ref(false)
const purchasePrice = ref<number | null>(null)
const purchaseQuantity = ref<number | null>(null)

const computedCostPerUnit = computed(() => {
  const price = purchasePrice.value ?? 0
  const qty = purchaseQuantity.value ?? 0
  if (qty <= 0 || price <= 0) return null
  return price / qty
})

function formatCurrency(value: number | null | undefined): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(value ?? 0)
}

function statusLabel(status: string): string {
  return status === 'ACTIVE' ? 'Ativo' : 'Inativo'
}

function statusClass(status: string): string {
  return status === 'ACTIVE' ? 'badge badge-active' : 'badge badge-inactive'
}

function resetAutoCalcFields() {
  autoCalcCost.value = false
  purchasePrice.value = null
  purchaseQuantity.value = null
}

function openCreateModal() {
  editing.value = null
  form.value = { name: '', unit: '', costPerUnit: 0, defaultQuantity: 0 }
  resetAutoCalcFields()
  showModal.value = true
}

function openEditModal(ingredient: IngredientResponse) {
  editing.value = ingredient
  form.value = {
    name: ingredient.name,
    unit: ingredient.unit,
    costPerUnit: ingredient.costPerUnit,
    defaultQuantity: ingredient.defaultQuantity ?? 0,
  }
  resetAutoCalcFields()
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editing.value = null
}

async function handleSubmit() {
  if (autoCalcCost.value) {
    if (computedCostPerUnit.value == null) return
    form.value.costPerUnit = Number(computedCostPerUnit.value.toFixed(4))
  }
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
            <th>Qtd. Padrão</th>
            <th>Status</th>
            <th style="width: 150px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ingredient in store.items" :key="ingredient.id">
            <td>{{ ingredient.name }}</td>
            <td>{{ ingredient.unit }}</td>
            <td>{{ formatCurrency(ingredient.costPerUnit) }}</td>
            <td>{{ ingredient.defaultQuantity ?? '-' }}</td>
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
              <label style="display: flex; align-items: center; gap: 8px; cursor: pointer">
                <input
                  v-model="autoCalcCost"
                  type="checkbox"
                  data-testid="ingredient-cost-auto-checkbox"
                />
                Calcular custo por unidade automaticamente
              </label>
            </div>

            <div v-if="!autoCalcCost" class="form-group">
              <label>Custo por Unidade (R$)</label>
              <input
                v-model.number="form.costPerUnit"
                type="number"
                step="0.0001"
                min="0.0001"
                class="form-control"
                placeholder="0,0000"
                data-testid="ingredient-cost-per-unit-input"
                required
              />
            </div>

            <div v-else>
              <div class="form-group">
                <label>Valor da compra (R$)</label>
                <input
                  v-model.number="purchasePrice"
                  type="number"
                  step="0.01"
                  min="0"
                  class="form-control"
                  placeholder="195,00"
                  data-testid="ingredient-purchase-price-input"
                  required
                />
              </div>
              <div class="form-group">
                <label>Quantidade comprada (na unidade do ingrediente)</label>
                <input
                  v-model.number="purchaseQuantity"
                  type="number"
                  step="0.001"
                  min="0.001"
                  class="form-control"
                  placeholder="9000"
                  data-testid="ingredient-purchase-quantity-input"
                  required
                />
              </div>
              <div class="form-group">
                <label>Custo por unidade calculado</label>
                <div
                  class="form-control"
                  data-testid="ingredient-cost-per-unit-computed"
                  style="background: #f1f5f9; color: #0f172a"
                >
                  {{ computedCostPerUnit != null ? formatCurrency(computedCostPerUnit) : '—' }}
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>Quantidade padrão (unidade do ingrediente)</label>
              <input
                v-model.number="form.defaultQuantity"
                type="number"
                step="0.001"
                min="0"
                class="form-control"
                placeholder="0"
                data-testid="ingredient-default-quantity-input"
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
