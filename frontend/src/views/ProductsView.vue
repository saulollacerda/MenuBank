<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useProductStore } from '@/stores/productStore'
import { useIngredientStore } from '@/stores/ingredientStore'
import type { ProductRequest, ProductResponse, RecipeItemRequest } from '@/types/Product'

const productStore = useProductStore()
const ingredientStore = useIngredientStore()

const showModal = ref(false)
const showRecipeModal = ref(false)
const editing = ref<ProductResponse | null>(null)
const selectedProduct = ref<ProductResponse | null>(null)
const form = ref<ProductRequest>({ name: '', price: 0 })
const recipeForm = ref<RecipeItemRequest>({ ingredientId: '', quantity: 0 })
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
  form.value = { name: '', price: 0 }
  showModal.value = true
}

function openEditModal(product: ProductResponse) {
  editing.value = product
  form.value = { name: product.name, price: product.price }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editing.value = null
}

async function handleSubmit() {
  try {
    if (editing.value) {
      await productStore.update(editing.value.id, form.value)
    } else {
      await productStore.create(form.value)
    }
    closeModal()
  } catch {
    // Error is handled by the store
  }
}

async function openRecipeModal(product: ProductResponse) {
  selectedProduct.value = product
  recipeForm.value = { ingredientId: '', quantity: 0 }
  showRecipeModal.value = true
  await productStore.fetchRecipeItems(product.id)
}

function handleRecipeIngredientChange() {
  const selected = ingredientStore.items.find(
    (ingredient) => ingredient.id === recipeForm.value.ingredientId,
  )
  recipeForm.value.quantity = selected?.defaultQuantity ?? 0
}

function closeRecipeModal() {
  showRecipeModal.value = false
  selectedProduct.value = null
}

async function handleAddRecipeItem() {
  if (!selectedProduct.value) return
  try {
    await productStore.addRecipeItem(selectedProduct.value.id, recipeForm.value)
    recipeForm.value = { ingredientId: '', quantity: 0 }
  } catch {
    // Error is handled by the store
  }
}

async function handleRemoveRecipeItem(recipeItemId: string) {
  if (!selectedProduct.value) return
  try {
    await productStore.removeRecipeItem(selectedProduct.value.id, recipeItemId)
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
    await productStore.remove(confirmDeleteId.value)
  } catch {
    // Error is handled by the store
  }
  confirmDeleteId.value = null
}

onMounted(() => {
  productStore.fetchAll()
  ingredientStore.fetchAll()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Produtos</h1>
      <button class="btn btn-primary" @click="openCreateModal">+ Novo Produto</button>
    </div>

    <div v-if="productStore.error" class="alert alert-error">{{ productStore.error }}</div>

    <div v-if="productStore.loading && !showRecipeModal" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="productStore.items.length === 0 && !showRecipeModal" class="empty-state">
      <p>Nenhum produto cadastrado.</p>
      <button class="btn btn-primary" @click="openCreateModal">Criar primeiro produto</button>
    </div>

    <div v-else-if="!showRecipeModal" class="table-container">
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Preço</th>
            <th>Custo Estimado</th>
            <th>Margem</th>
            <th>CMV</th>
            <th>Status</th>
            <th style="width: 220px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in productStore.items" :key="product.id">
            <td>{{ product.name }}</td>
            <td>{{ formatCurrency(product.price) }}</td>
            <td>{{ formatCurrency(product.estimatedCost) }}</td>
            <td>{{ formatCurrency(product.margin) }}</td>
            <td>{{ formatCurrency(product.cmv) }}</td>
            <td>
              <span :class="statusClass(product.status)">
                {{ statusLabel(product.status) }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button class="btn btn-primary btn-sm" @click="openRecipeModal(product)">
                  Ficha Técnica
                </button>
                <button class="btn btn-secondary btn-sm" @click="openEditModal(product)">
                  Editar
                </button>
                <button class="btn btn-danger btn-sm" @click="confirmDelete(product.id)">
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
          <h2>{{ editing ? 'Editar Produto' : 'Novo Produto' }}</h2>
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
                placeholder="Nome do produto"
                required
              />
            </div>
            <div class="form-group">
              <label>Preço (R$)</label>
              <input
                v-model.number="form.price"
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
              <button type="submit" class="btn btn-primary" :disabled="productStore.loading">
                {{ editing ? 'Salvar' : 'Criar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Recipe Items Modal -->
    <div v-if="showRecipeModal" class="modal-overlay" @click.self="closeRecipeModal">
      <div class="modal modal-wide">
        <div class="modal-header">
          <h2>Ficha Técnica — {{ selectedProduct?.name }}</h2>
          <button class="modal-close" @click="closeRecipeModal">✕</button>
        </div>
        <div class="modal-body">
          <!-- Add ingredient form -->
          <form @submit.prevent="handleAddRecipeItem" class="order-items-row" style="margin-bottom: 16px">
            <div class="form-group">
              <label>Ingrediente</label>
              <select
                v-model="recipeForm.ingredientId"
                class="form-control"
                data-testid="recipe-ingredient-select"
                required
                @change="handleRecipeIngredientChange"
              >
                <option value="" disabled>Selecione...</option>
                <option
                  v-for="ingredient in ingredientStore.items"
                  :key="ingredient.id"
                  :value="ingredient.id"
                >
                  {{ ingredient.name }} ({{ ingredient.unit }})
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>Quantidade</label>
              <input
                v-model.number="recipeForm.quantity"
                type="number"
                step="0.001"
                min="0.001"
                class="form-control"
                placeholder="0"
                data-testid="recipe-quantity-input"
                required
              />
            </div>
            <button type="submit" class="btn btn-primary btn-sm" style="margin-bottom: 0">
              Adicionar
            </button>
          </form>

          <!-- Recipe items table -->
          <div v-if="productStore.recipeItems.length === 0" class="empty-state">
            <p>Nenhum ingrediente na ficha técnica.</p>
          </div>
          <div v-else class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Ingrediente</th>
                  <th>Unidade</th>
                  <th>Quantidade</th>
                  <th>Custo/Unidade</th>
                  <th>Custo Total</th>
                  <th style="width: 80px">Ações</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in productStore.recipeItems" :key="item.id">
                  <td>{{ item.ingredientName }}</td>
                  <td>{{ item.ingredientUnit }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatCurrency(item.costPerUnit) }}</td>
                  <td>{{ formatCurrency(item.totalCost) }}</td>
                  <td>
                    <button
                      class="btn btn-danger btn-sm"
                      @click="handleRemoveRecipeItem(item.id)"
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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
          <p>Tem certeza que deseja excluir este produto?</p>
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
