<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIngredientStore } from '@/stores/ingredientStore'
import PageControls from '@/components/PageControls.vue'
import type { IngredientRequest, IngredientResponse, IngredientProductUsageResponse } from '@/types/Ingredient'
import type { ProductResponse } from '@/types/Product'
import { ingredientService } from '@/services/ingredientService'
import { productService } from '@/services/productService'
import { includeService } from '@/services/includeService'

const store = useIngredientStore()
const route = useRoute()
const router = useRouter()

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

// --- Includes específicos por produto (dentro do modal de criar/editar) ---
// Cada include é um item da ficha técnica do produto com nome/custo/quantidade.
// Quando o ingrediente é editado nesta tela, a gramatura específica de um produto
// vira a `quantity` de um include nesse produto (name = ingrediente.name, cost = ingrediente.costPerUnit).

interface SpecificQuantity {
  includeId?: string
  productId: string
  productName: string
  quantity: number
}

const specificGrammages = ref<SpecificQuantity[]>([])
const toDelete = ref<{ includeId: string; productId: string }[]>([])

const allProducts = ref<ProductResponse[]>([])
const newSpecificProductId = ref('')
const newSpecificGrammage = ref<number | null>(null)

const addSpecificError = ref<string | null>(null)

const availableProducts = computed(() =>
  allProducts.value.filter(
    (p) => !specificGrammages.value.some((sg) => sg.productId === p.id),
  ),
)

async function loadProducts() {
  const page = await productService.findAll({ search: '', page: 0, size: 500 })
  allProducts.value = page.content
}

function addSpecificGrammage() {
  addSpecificError.value = null
  if (!newSpecificProductId.value) {
    addSpecificError.value = 'Selecione um produto.'
    return
  }
  if (!newSpecificGrammage.value || newSpecificGrammage.value <= 0) {
    addSpecificError.value = 'Informe uma quantidade maior que zero.'
    return
  }
  const product = allProducts.value.find((p) => p.id === newSpecificProductId.value)
  if (!product) return

  specificGrammages.value.push({
    productId: product.id,
    productName: product.name,
    quantity: newSpecificGrammage.value,
  })

  newSpecificProductId.value = ''
  newSpecificGrammage.value = null
}

function removeSpecificGrammage(index: number, sg: SpecificQuantity) {
  if (sg.includeId) {
    toDelete.value.push({ includeId: sg.includeId, productId: sg.productId })
  }
  specificGrammages.value.splice(index, 1)
}

async function openCreateModal(prefilledName = '') {
  editing.value = null
  form.value = { name: prefilledName, unit: '', costPerUnit: 0, defaultQuantity: 0 }
  specificGrammages.value = []
  toDelete.value = []
  newSpecificProductId.value = ''
  newSpecificGrammage.value = null
  addSpecificError.value = null
  submitError.value = null
  resetAutoCalcFields()
  await loadProducts()
  showModal.value = true
}

async function openEditModal(ingredient: IngredientResponse) {
  editing.value = ingredient
  form.value = {
    name: ingredient.name,
    unit: ingredient.unit,
    costPerUnit: ingredient.costPerUnit,
    defaultQuantity: ingredient.defaultQuantity ?? 0,
  }
  specificGrammages.value = []
  toDelete.value = []
  newSpecificProductId.value = ''
  newSpecificGrammage.value = null
  addSpecificError.value = null
  submitError.value = null
  resetAutoCalcFields()
  await loadProducts()
  try {
    const usages: IngredientProductUsageResponse[] = await ingredientService.fetchUsages(ingredient.id)
    specificGrammages.value = usages.map((u) => ({
      includeId: u.includeId,
      productId: u.productId,
      productName: u.productName,
      quantity: u.quantity,
    }))
  } catch {
    // usages não críticos — modal abre mesmo assim
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editing.value = null
}

const submitError = ref<string | null>(null)

function extractErrorMessage(err: unknown): string {
  const e = err as { response?: { data?: { detail?: string; message?: string } }; message?: string }
  return (
    e?.response?.data?.detail ||
    e?.response?.data?.message ||
    e?.message ||
    'Erro ao salvar'
  )
}

async function handleSubmit() {
  submitError.value = null
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
  } catch (err: unknown) {
    submitError.value = extractErrorMessage(err)
    return
  }

  // Snapshot do nome/custo no momento do save — usado na ficha técnica dos produtos
  const includeName = form.value.name
  const includeCost = Number(form.value.costPerUnit ?? 0)

  try {
    for (const d of toDelete.value) {
      await includeService.remove(d.productId, d.includeId)
    }

    for (const sg of specificGrammages.value) {
      if (sg.includeId) {
        await includeService.update(sg.productId, sg.includeId, {
          name: includeName,
          cost: includeCost,
          quantity: sg.quantity,
        })
      } else {
        await includeService.add(sg.productId, {
          name: includeName,
          cost: includeCost,
          quantity: sg.quantity,
        })
      }
    }
  } catch (err: unknown) {
    submitError.value =
      'Ingrediente salvo, mas falha em sincronizar com fichas técnicas: ' + extractErrorMessage(err)
    return
  }

  closeModal()
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

function maybeOpenFromQuery() {
  const createName = route.query.createName
  if (typeof createName === 'string' && createName.trim().length > 0) {
    openCreateModal(createName)
    router.replace({ query: {} })
  }
}

onMounted(() => {
  store.fetchPage({ page: 0, search: '' })
  maybeOpenFromQuery()
})

watch(() => route.query.createName, () => maybeOpenFromQuery())
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Ingredientes</h1>
      <button class="btn btn-primary" @click="openCreateModal()">+ Novo Ingrediente</button>
    </div>

    <div v-if="store.error" class="alert alert-error">{{ store.error }}</div>

    <PageControls
      v-model="store.search"
      :page="store.page"
      :total-pages="store.totalPages"
      :total-elements="store.totalElements"
      :loading="store.loading"
      placeholder="Buscar ingrediente por nome..."
      @search="onSearch"
      @page-change="onPageChange"
    />

    <div v-if="store.loading" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="store.items.length === 0" class="empty-state">
      <p v-if="store.search">Nenhum ingrediente encontrado para "{{ store.search }}".</p>
      <template v-else>
        <p>Nenhum ingrediente cadastrado.</p>
        <button class="btn btn-primary" @click="openCreateModal()">
          Cadastrar primeiro ingrediente
        </button>
      </template>
    </div>

    <div v-else class="table-container">
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Unidade</th>
            <th title="Custo: o que você paga pelo ingrediente">Custo/Unidade</th>
            <th title="Preço de venda no cardápio Anota.AI (informativo)">Preço Venda</th>
            <th>Qtd. Padrão</th>
            <th>Status</th>
            <th style="width: 150px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ingredient in store.items" :key="ingredient.id">
            <td>{{ ingredient.name }}</td>
            <td>{{ ingredient.unit }}</td>
            <td>
              <span
                v-if="Number(ingredient.costPerUnit) === 0"
                class="badge badge-warning"
                title="Custo não configurado — cadastre o valor que você paga"
              >
                ⚠ R$ 0,00
              </span>
              <template v-else>{{ formatCurrency(ingredient.costPerUnit) }}</template>
            </td>
            <td>
              <template v-if="ingredient.salePrice != null">
                <span title="Preço de venda vindo do cardápio Anota.AI">
                  🛒 {{ formatCurrency(ingredient.salePrice) }}
                </span>
              </template>
              <template v-else>—</template>
            </td>
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
      <div class="modal modal-wide">
        <div class="modal-header">
          <h2>{{ editing ? 'Editar Ingrediente' : 'Novo Ingrediente' }}</h2>
          <button class="modal-close" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="submitError" class="alert alert-error" style="margin-bottom: 12px">
            {{ submitError }}
          </div>
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>Nome</label>
              <input
                v-model="form.name"
                type="text"
                class="form-control"
                placeholder="Nome do ingrediente"
                data-testid="ingredient-name-input"
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

            <!-- Quantidade específica por produto (cria/atualiza include no produto) -->
            <div class="form-section">
              <h3 class="form-section-title">Quantidade específica por produto</h3>
              <p class="form-hint">
                Adiciona este ingrediente à ficha técnica do produto selecionado.
                O nome e o custo deste ingrediente são gravados no item da ficha técnica.
              </p>

              <div class="add-specific-row">
                <select v-model="newSpecificProductId" class="form-control form-control-flex">
                  <option value="">Selecionar produto...</option>
                  <option v-for="p in availableProducts" :key="p.id" :value="p.id">
                    {{ p.name }}
                  </option>
                </select>
                <input
                  v-model.number="newSpecificGrammage"
                  type="number"
                  step="0.001"
                  min="0.001"
                  class="form-control form-control-grammage"
                  placeholder="Qtd."
                />
                <button type="button" class="btn btn-secondary" @click="addSpecificGrammage">
                  + Adicionar
                </button>
              </div>

              <p v-if="addSpecificError" class="field-error">{{ addSpecificError }}</p>

              <div v-if="specificGrammages.length > 0" class="table-container" style="margin-top: 8px">
                <table>
                  <thead>
                    <tr>
                      <th>Produto</th>
                      <th>Quantidade</th>
                      <th style="width: 80px"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(sg, idx) in specificGrammages" :key="sg.productId">
                      <td>{{ sg.productName }}</td>
                      <td>
                        <input
                          v-model.number="sg.quantity"
                          type="number"
                          step="0.001"
                          min="0.001"
                          class="form-control form-control-inline"
                        />
                      </td>
                      <td>
                        <button
                          type="button"
                          class="btn btn-danger btn-sm"
                          @click="removeSpecificGrammage(idx, sg)"
                        >
                          Remover
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
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

<style scoped>
.modal-wide {
  width: min(680px, 95vw);
}

.form-section {
  border-top: 1px solid #e2e8f0;
  margin-top: 1.25rem;
  padding-top: 1rem;
}

.form-section-title {
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0 0 4px;
}

.form-hint {
  font-size: 0.8rem;
  color: #64748b;
  margin: 0 0 12px;
}

.add-specific-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.form-control-flex {
  flex: 1;
}

.form-control-grammage {
  width: 120px;
}

.form-control-inline {
  width: 100px;
  padding: 4px 8px;
}

.field-error {
  color: #dc2626;
  font-size: 0.8rem;
  margin: 4px 0 0;
}
</style>
