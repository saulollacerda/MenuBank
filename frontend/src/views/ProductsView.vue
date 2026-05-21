<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useProductStore } from '@/stores/productStore'
import { useIngredientStore } from '@/stores/ingredientStore'
import { useCategoryStore } from '@/stores/categoryStore'
import { useAnotaAIStore } from '@/stores/anotaAIStore'
import PageControls from '@/components/PageControls.vue'
import type {
  ProductRequest,
  ProductResponse,
  ProductComplementGroupResponse,
  ProductIngredientRequest,
} from '@/types/Product'

const productStore = useProductStore()
const ingredientStore = useIngredientStore()
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
const recipeForm = ref<ProductIngredientRequest>({ ingredientId: '', grammage: 0, isOptional: false })
const confirmDeleteId = ref<string | null>(null)
const confirmClearRecipe = ref(false)
const addingFromGroup = ref<ProductComplementGroupResponse | null>(null)
const groupIngredientForm = ref<ProductIngredientRequest>({ ingredientId: '', grammage: 0, isOptional: true })

// Wizard de grupos obrigatórios
const showWizard = ref(false)
interface WizardRow {
  group: ProductComplementGroupResponse
  ingredientId: string
  grammage: number
  skip: boolean
}
const wizardRows = ref<WizardRow[]>([])

const uncoveredRequiredGroups = computed<ProductComplementGroupResponse[]>(() => {
  if (!selectedProduct.value?.complementGroups) return []
  const covered = new Set(
    productStore.productIngredients
      .map((item) => item.ingredientCategoryId)
      .filter((id): id is string => !!id),
  )
  return selectedProduct.value.complementGroups.filter(
    (g) => g.minRequired > 0 && !covered.has(g.ingredientCategoryId),
  )
})

function openWizard() {
  wizardRows.value = uncoveredRequiredGroups.value.map((group) => ({
    group,
    ingredientId: '',
    grammage: 0,
    skip: false,
  }))
  showWizard.value = true
}

function closeWizard() {
  showWizard.value = false
  wizardRows.value = []
}

function ingredientsForGroup(group: ProductComplementGroupResponse) {
  return ingredientStore.items.filter(
    (i) => i.ingredientCategoryId === group.ingredientCategoryId,
  )
}

function onWizardIngredientChange(row: WizardRow) {
  const selected = ingredientStore.items.find((i) => i.id === row.ingredientId)
  row.grammage = selected?.defaultQuantity ?? 0
}

async function handleWizardSave() {
  if (!selectedProduct.value) return
  // Ingredientes vindos do wizard são por padrão opcionais (vêm de grupos de complementos)
  const payload: ProductIngredientRequest[] = wizardRows.value
    .filter((r) => !r.skip && r.ingredientId && r.grammage > 0)
    .map((r) => ({ ingredientId: r.ingredientId, grammage: r.grammage, isOptional: true }))
  if (payload.length === 0) {
    closeWizard()
    return
  }
  try {
    await productStore.batchAddProductIngredients(selectedProduct.value.id, payload)
    closeWizard()
  } catch {
    // Error is handled by the store
  }
}

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
  recipeForm.value = { ingredientId: '', grammage: 0, isOptional: false }
  showRecipeModal.value = true
  await productStore.fetchProductIngredients(product.id)
}

function handleRecipeIngredientChange() {
  const selected = ingredientStore.items.find(
    (ingredient) => ingredient.id === recipeForm.value.ingredientId,
  )
  recipeForm.value.grammage = selected?.defaultQuantity ?? 0
}

function closeRecipeModal() {
  showRecipeModal.value = false
  selectedProduct.value = null
}

async function handleAddRecipeItem() {
  if (!selectedProduct.value) return
  try {
    await productStore.addProductIngredient(selectedProduct.value.id, recipeForm.value)
    recipeForm.value = { ingredientId: '', grammage: 0, isOptional: false }
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

async function handleRemoveRecipeItem(productIngredientId: string) {
  if (!selectedProduct.value) return
  try {
    await productStore.removeProductIngredient(selectedProduct.value.id, productIngredientId)
  } catch {
    // Error is handled by the store
  }
}

function openGroupSelector(group: ProductComplementGroupResponse) {
  addingFromGroup.value = group
  groupIngredientForm.value = { ingredientId: '', grammage: 0, isOptional: true }
}

function closeGroupSelector() {
  addingFromGroup.value = null
}

function handleGroupIngredientChange() {
  const selected = ingredientStore.items.find((i) => i.id === groupIngredientForm.value.ingredientId)
  groupIngredientForm.value.grammage = selected?.defaultQuantity ?? 0
}

async function handleAddFromGroup() {
  if (!selectedProduct.value || !groupIngredientForm.value.ingredientId) return
  try {
    await productStore.addProductIngredient(selectedProduct.value.id, groupIngredientForm.value)
    closeGroupSelector()
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
  ingredientStore.fetchAll()
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
            v-if="productStore.recipeItems.length > 0"
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
              <label>Gramatura</label>
              <input
                v-model.number="recipeForm.grammage"
                type="number"
                step="0.001"
                min="0.001"
                class="form-control"
                placeholder="0"
                data-testid="recipe-quantity-input"
                required
              />
            </div>
            <div class="form-group" style="min-width: 130px">
              <label style="display: flex; align-items: center; gap: 6px; cursor: pointer">
                <input
                  v-model="recipeForm.isOptional"
                  type="checkbox"
                  data-testid="recipe-isoptional-checkbox"
                />
                Opcional
              </label>
              <small style="font-size: 0.7rem; color: #64748b">
                Só conta no custo quando aparece nos extras do pedido
              </small>
            </div>
            <button type="submit" class="btn btn-primary btn-sm" style="margin-bottom: 0">
              Adicionar
            </button>
          </form>

          <!-- Recipe items table -->
          <div v-if="productStore.productIngredients.length === 0" class="empty-state">
            <p>Nenhum ingrediente na ficha técnica.</p>
          </div>
          <div v-else class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Ingrediente</th>
                  <th>Unidade</th>
                  <th>Gramatura</th>
                  <th>Tipo</th>
                  <th>Custo/Unidade</th>
                  <th>Custo Total</th>
                  <th style="width: 80px">Ações</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in productStore.productIngredients" :key="item.id">
                  <td>{{ item.ingredientName }}</td>
                  <td>{{ item.ingredientUnit }}</td>
                  <td>{{ item.grammage }}</td>
                  <td>
                    <span v-if="item.isOptional" class="badge badge-warning" title="Só entra no custo quando aparece nos extras do pedido">
                      Opcional
                    </span>
                    <span v-else class="badge badge-active">Base</span>
                  </td>
                  <td>
                    <span v-if="Number(item.costPerUnit) === 0" class="badge badge-warning" title="Custo não configurado — configure no cadastro do ingrediente">
                      ⚠ R$ 0,00
                    </span>
                    <template v-else>{{ formatCurrency(item.costPerUnit) }}</template>
                  </td>
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

          <!-- Complement groups section -->
          <template v-if="selectedProduct?.complementGroups?.length">
            <div style="display: flex; align-items: center; justify-content: space-between; margin: 16px 0 8px">
              <h3 style="font-size: 0.875rem; font-weight: 600; margin: 0">
                Grupos de Complementos
              </h3>
              <button
                v-if="uncoveredRequiredGroups.length > 0"
                class="btn btn-primary btn-sm"
                data-testid="open-wizard-button"
                style="margin-bottom: 0"
                @click="openWizard"
              >
                ⚙ Configurar grupos obrigatórios ({{ uncoveredRequiredGroups.length }})
              </button>
            </div>
            <p style="font-size: 0.8rem; color: #64748b; margin-bottom: 8px">
              Selecione um ingrediente de cada grupo para incluir no custo padrão do produto.
            </p>
            <div class="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Grupo</th>
                    <th>Mín.</th>
                    <th>Máx.</th>
                    <th style="width: 160px">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="group in selectedProduct.complementGroups"
                    :key="group.id"
                    :data-testid="`complement-group-${group.id}`"
                  >
                    <td>{{ group.ingredientCategoryName }}</td>
                    <td>{{ group.minRequired }}</td>
                    <td>{{ group.maxAllowed }}</td>
                    <td>
                      <button
                        class="btn btn-secondary btn-sm"
                        :data-testid="`add-from-group-${group.id}`"
                        @click="openGroupSelector(group)"
                      >
                        Adicionar ao custo
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Inline ingredient selector for a group -->
            <div
              v-if="addingFromGroup"
              style="border: 1px solid #e2e8f0; border-radius: 6px; padding: 12px; margin-top: 12px"
            >
              <p style="font-weight: 600; margin-bottom: 8px">
                Adicionar ingrediente do grupo: {{ addingFromGroup.ingredientCategoryName }}
              </p>
              <form
                class="order-items-row"
                style="margin-bottom: 0"
                @submit.prevent="handleAddFromGroup"
              >
                <div class="form-group">
                  <label>Ingrediente</label>
                  <select
                    v-model="groupIngredientForm.ingredientId"
                    class="form-control"
                    :data-testid="`group-ingredient-select-${addingFromGroup.id}`"
                    required
                    @change="handleGroupIngredientChange"
                  >
                    <option value="" disabled>Selecione...</option>
                    <option
                      v-for="ingredient in ingredientStore.items.filter(
                        (i) => i.ingredientCategoryId === addingFromGroup!.ingredientCategoryId,
                      )"
                      :key="ingredient.id"
                      :value="ingredient.id"
                    >
                      {{ ingredient.name }} ({{ ingredient.unit }})
                    </option>
                  </select>
                </div>
                <div class="form-group">
                  <label>Gramatura</label>
                  <input
                    v-model.number="groupIngredientForm.grammage"
                    type="number"
                    step="0.001"
                    min="0.001"
                    class="form-control"
                    :data-testid="`group-ingredient-quantity-${addingFromGroup.id}`"
                    required
                  />
                </div>
                <button type="submit" class="btn btn-primary btn-sm" style="margin-bottom: 0">
                  Confirmar
                </button>
                <button
                  type="button"
                  class="btn btn-secondary btn-sm"
                  style="margin-bottom: 0"
                  @click="closeGroupSelector"
                >
                  Cancelar
                </button>
              </form>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- Wizard de Grupos Obrigatórios -->
    <div v-if="showWizard" class="modal-overlay" @click.self="closeWizard">
      <div class="modal modal-wide">
        <div class="modal-header">
          <h2>Configurar grupos obrigatórios — {{ selectedProduct?.name }}</h2>
          <button class="modal-close" @click="closeWizard">✕</button>
        </div>
        <div class="modal-body">
          <p style="font-size: 0.85rem; color: #64748b; margin-bottom: 12px">
            Selecione o ingrediente padrão de cada grupo obrigatório (ou marque "Pular").
            Todos serão adicionados à Ficha Técnica em uma única operação.
          </p>

          <div v-if="wizardRows.length === 0" class="empty-state">
            <p>Não há grupos obrigatórios pendentes para configurar.</p>
          </div>

          <div v-else class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Grupo</th>
                  <th>Ingrediente</th>
                  <th style="width: 120px">Gramatura</th>
                  <th style="width: 80px">Pular</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, idx) in wizardRows"
                  :key="row.group.id"
                  :data-testid="`wizard-row-${row.group.id}`"
                  :style="row.skip ? 'opacity: 0.5' : ''"
                >
                  <td>{{ row.group.ingredientCategoryName }}</td>
                  <td>
                    <select
                      v-model="row.ingredientId"
                      class="form-control"
                      :disabled="row.skip"
                      :data-testid="`wizard-ingredient-${idx}`"
                      @change="onWizardIngredientChange(row)"
                    >
                      <option value="" disabled>Selecione...</option>
                      <option
                        v-for="ingredient in ingredientsForGroup(row.group)"
                        :key="ingredient.id"
                        :value="ingredient.id"
                      >
                        {{ ingredient.name }} ({{ ingredient.unit }})
                      </option>
                    </select>
                  </td>
                  <td>
                    <input
                      v-model.number="row.grammage"
                      type="number"
                      step="0.001"
                      min="0"
                      class="form-control"
                      :disabled="row.skip"
                      :data-testid="`wizard-quantity-${idx}`"
                    />
                  </td>
                  <td style="text-align: center">
                    <input
                      v-model="row.skip"
                      type="checkbox"
                      :data-testid="`wizard-skip-${idx}`"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeWizard">Cancelar</button>
            <button
              class="btn btn-primary"
              data-testid="wizard-save-button"
              :disabled="productStore.loading || wizardRows.length === 0"
              @click="handleWizardSave"
            >
              Salvar todos
            </button>
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
            Tem certeza que deseja remover <strong>todos</strong> os ingredientes da ficha técnica
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
