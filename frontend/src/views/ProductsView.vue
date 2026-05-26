<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useProductStore } from '@/stores/productStore'
import { useCategoryStore } from '@/stores/categoryStore'
import { useAnotaAIStore } from '@/stores/anotaAIStore'
import PageControls from '@/components/PageControls.vue'
import type { ProductRequest, ProductResponse, IncludeRequest } from '@/types/Product'

const productStore = useProductStore()
const categoryStore = useCategoryStore()
const anotaAIStore = useAnotaAIStore()

const syncClearRecipes = ref(false)

async function handleSyncCatalog() {
  anotaAIStore.clearResult()
  try {
    await anotaAIStore.syncCatalog({ clearRecipes: syncClearRecipes.value })
  } catch {
    // erro fica em anotaAIStore.error
  }
}

const showModal = ref(false)
const showRecipeModal = ref(false)
const editing = ref<ProductResponse | null>(null)
const selectedProduct = ref<ProductResponse | null>(null)
const form = ref<ProductRequest>({ name: '', price: 0, categoryId: '' })
const recipeForm = ref<IncludeRequest>({ name: '', cost: 0, quantity: 1 })
const confirmDeleteId = ref<string | null>(null)
const confirmClearRecipe = ref(false)

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

function openCreateModal() {
  editing.value = null
  form.value = { name: '', price: 0, categoryId: '' }
  showModal.value = true
}

function openEditModal(product: ProductResponse) {
  editing.value = product
  form.value = {
    name: product.name,
    price: product.price,
    categoryId: product.categoryId,
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
  recipeForm.value = { name: '', cost: 0, quantity: 1 }
  showRecipeModal.value = true
  await productStore.fetchIncludes(product.id)
}

function closeRecipeModal() {
  showRecipeModal.value = false
  selectedProduct.value = null
}

async function handleAddRecipeItem() {
  if (!selectedProduct.value) return
  try {
    await productStore.addInclude(selectedProduct.value.id, recipeForm.value)
    recipeForm.value = { name: '', cost: 0, quantity: 1 }
  } catch {
    // Error is handled by the store
  }
}

async function handleClearRecipe() {
  if (!selectedProduct.value) return
  try {
    await productStore.clearRecipe(selectedProduct.value.id)
  } catch {
    // Error is handled by the store
  } finally {
    confirmClearRecipe.value = false
  }
}

async function handleRemoveRecipeItem(includeId: string) {
  if (!selectedProduct.value) return
  try {
    await productStore.removeInclude(selectedProduct.value.id, includeId)
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

function onSearch(term: string) {
  productStore.fetchPage({ search: term, page: 0 })
}

function onPageChange(p: number) {
  productStore.fetchPage({ page: p })
}

onMounted(() => {
  productStore.fetchPage({ page: 0, search: '' })
  categoryStore.fetchAll()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Produtos</h1>
      <div class="page-header-actions">
        <label
          style="display: flex; align-items: center; gap: 6px; font-size: 0.8rem; color: #475569"
          title="Apaga todos os itens das fichas técnicas antes de re-importar"
        >
          <input
            v-model="syncClearRecipes"
            type="checkbox"
            data-testid="sync-clear-recipes-checkbox"
          />
          Limpar fichas técnicas
        </label>
        <button
          class="btn btn-secondary"
          data-testid="sync-anotaai-catalog-button"
          :disabled="anotaAIStore.syncingCatalog"
          @click="handleSyncCatalog"
        >
          <span v-if="anotaAIStore.syncingCatalog" class="spinner spinner-sm"></span>
          <span v-else>🔄 Sincronizar Cardápio</span>
        </button>
        <button
          class="btn btn-primary"
          data-testid="new-product-button"
          @click="openCreateModal"
        >
          + Novo Produto
        </button>
      </div>
    </div>

    <div v-if="anotaAIStore.error" class="alert alert-error">{{ anotaAIStore.error }}</div>
    <div
      v-if="anotaAIStore.lastResult && !anotaAIStore.error"
      class="alert alert-success"
    >
      Categorias: {{ anotaAIStore.lastResult.categoriesCreated }} criada(s),
      {{ anotaAIStore.lastResult.categoriesUpdated }} atualizada(s).
      Produtos: {{ anotaAIStore.lastResult.productsCreated }} criado(s),
      {{ anotaAIStore.lastResult.productsUpdated }} atualizado(s).
      <div style="margin-top: 4px; font-size: 0.8rem; color: #475569">
        Ingredientes do cardápio são cadastrados manualmente em "Ingredientes".
      </div>
    </div>

    <div v-if="productStore.error" class="alert alert-error">{{ productStore.error }}</div>

    <PageControls
      v-if="!showRecipeModal"
      v-model="productStore.search"
      :page="productStore.page"
      :total-pages="productStore.totalPages"
      :total-elements="productStore.totalElements"
      :loading="productStore.loading"
      placeholder="Buscar produto por nome..."
      @search="onSearch"
      @page-change="onPageChange"
    />

    <div v-if="productStore.loading && !showRecipeModal" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="productStore.items.length === 0 && !showRecipeModal" class="empty-state">
      <p v-if="productStore.search">Nenhum produto encontrado para "{{ productStore.search }}".</p>
      <template v-else>
        <p>Nenhum produto cadastrado.</p>
        <button class="btn btn-primary" @click="openCreateModal">Criar primeiro produto</button>
      </template>
    </div>

    <div v-else-if="!showRecipeModal" class="table-container">
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Categoria</th>
            <th>Preço</th>
            <th>Status</th>
            <th style="width: 220px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in productStore.items" :key="product.id">
            <td>{{ product.name }}</td>
            <td>{{ product.categoryName }}</td>
            <td>{{ formatCurrency(product.price) }}</td>
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
                <button
                  class="btn btn-secondary btn-sm"
                  :data-testid="`product-${product.id}-edit-button`"
                  @click="openEditModal(product)"
                >
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
          <form data-testid="product-form" @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>Nome</label>
              <input
                v-model="form.name"
                type="text"
                class="form-control"
                placeholder="Nome do produto"
                data-testid="product-name-input"
                required
              />
            </div>
            <div class="form-group">
              <label>Categoria</label>
              <select
                v-model="form.categoryId"
                class="form-control"
                data-testid="product-category-select"
                required
              >
                <option value="" disabled>Selecione...</option>
                <option
                  v-for="category in categoryStore.items"
                  :key="category.id"
                  :value="category.id"
                >
                  {{ category.name }}
                </option>
              </select>
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
                data-testid="product-price-input"
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
          <div
            v-if="productStore.includes.length > 0"
            style="display: flex; justify-content: flex-end; margin-bottom: 12px"
          >
            <button
              class="btn btn-danger btn-sm"
              data-testid="clear-recipe-button"
              style="margin-bottom: 0"
              @click="confirmClearRecipe = true"
            >
              🗑 Limpar Ficha Técnica
            </button>
          </div>
          <!-- Add include form -->
          <form @submit.prevent="handleAddRecipeItem" class="order-items-row" style="margin-bottom: 16px">
            <div class="form-group">
              <label>Nome</label>
              <input
                v-model="recipeForm.name"
                type="text"
                class="form-control"
                placeholder="Ex.: Copo, Colher, Açaí Base"
                data-testid="recipe-name-input"
                required
              />
            </div>
            <div class="form-group">
              <label>Custo (R$)</label>
              <input
                v-model.number="recipeForm.cost"
                type="number"
                step="0.0001"
                min="0"
                class="form-control"
                placeholder="0,00"
                data-testid="recipe-cost-input"
                required
              />
            </div>
            <div class="form-group">
              <label>Quantidade</label>
              <input
                v-model.number="recipeForm.quantity"
                type="number"
                step="0.0001"
                min="0.0001"
                class="form-control"
                placeholder="1"
                data-testid="recipe-quantity-input"
                required
              />
            </div>
            <button type="submit" class="btn btn-primary btn-sm" style="margin-bottom: 0">
              Adicionar
            </button>
          </form>

          <!-- Recipe items table -->
          <div v-if="productStore.includes.length === 0" class="empty-state">
            <p>Nenhum item na ficha técnica.</p>
          </div>
          <div v-else class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Custo</th>
                  <th>Quantidade</th>
                  <th>Custo Total</th>
                  <th style="width: 80px">Ações</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in productStore.includes" :key="item.id">
                  <td>{{ item.name }}</td>
                  <td>{{ formatCurrency(item.cost) }}</td>
                  <td>{{ item.quantity }}</td>
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

    <!-- Clear Recipe Confirmation Modal -->
    <div v-if="confirmClearRecipe" class="modal-overlay" @click.self="confirmClearRecipe = false">
      <div class="modal">
        <div class="modal-header">
          <h2>Limpar Ficha Técnica</h2>
          <button class="modal-close" @click="confirmClearRecipe = false">✕</button>
        </div>
        <div class="modal-body">
          <p>
            Tem certeza que deseja remover <strong>todos</strong> os itens da ficha técnica
            de <strong>{{ selectedProduct?.name }}</strong>?
          </p>
          <p style="font-size: 0.85rem; color: #64748b">Essa ação não pode ser desfeita.</p>
          <div class="form-actions">
            <button class="btn btn-secondary" @click="confirmClearRecipe = false">Cancelar</button>
            <button
              class="btn btn-danger"
              data-testid="confirm-clear-recipe-button"
              @click="handleClearRecipe"
            >
              Limpar tudo
            </button>
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

<style scoped>
.page-header-actions {
  display: flex;
  gap: 0.5rem;
}
</style>
