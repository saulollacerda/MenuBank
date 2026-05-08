<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useOrderStore } from '@/stores/orderStore'
import { useCustomerStore } from '@/stores/customerStore'
import { useProductStore } from '@/stores/productStore'
import { useIngredientStore } from '@/stores/ingredientStore'
import type {
  OrderRequest,
  OrderResponse,
  OrderItemRequest,
  OrderItemExtraIngredientRequest,
} from '@/types/Order'

const orderStore = useOrderStore()
const customerStore = useCustomerStore()
const productStore = useProductStore()
const ingredientStore = useIngredientStore()

const showModal = ref(false)
const showDetailModal = ref(false)
const selectedOrder = ref<OrderResponse | null>(null)
const confirmDeleteId = ref<string | null>(null)

const form = ref<OrderRequest>({
  customerId: '',
  items: [{ productId: '', quantity: 1, extraIngredients: [] }],
})

function ensureExtrasArray(item: OrderItemRequest): OrderItemExtraIngredientRequest[] {
  if (!item.extraIngredients) item.extraIngredients = []
  return item.extraIngredients
}

function formatCurrency(value: number | null | undefined): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    value ?? 0,
  )
}

function formatDateTime(dateTime: string): string {
  return new Date(dateTime).toLocaleString('pt-BR')
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'Pendente',
    PAID: 'Pago',
    CANCELLED: 'Cancelado',
  }
  return map[status] ?? status
}

function statusClass(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'badge badge-pending',
    PAID: 'badge badge-paid',
    CANCELLED: 'badge badge-cancelled',
  }
  return map[status] ?? 'badge'
}

function openCreateModal() {
  form.value = {
    customerId: '',
    items: [{ productId: '', quantity: 1, extraIngredients: [] }],
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

function addItem() {
  form.value.items.push({ productId: '', quantity: 1, extraIngredients: [] })
}

function removeItem(index: number) {
  if (form.value.items.length > 1) {
    form.value.items.splice(index, 1)
  }
}

function addExtraIngredient(itemIndex: number) {
  const item = form.value.items[itemIndex]
  ensureExtrasArray(item).push({ ingredientId: '', quantity: 1 })
}

function removeExtraIngredient(itemIndex: number, extraIndex: number) {
  const item = form.value.items[itemIndex]
  const extras = ensureExtrasArray(item)
  extras.splice(extraIndex, 1)
}

async function handleSubmit() {
  try {
    await orderStore.create(form.value)
    closeModal()
  } catch {
    // Error is handled by the store
  }
}

function viewDetail(order: OrderResponse) {
  selectedOrder.value = order
  showDetailModal.value = true
}

function closeDetailModal() {
  showDetailModal.value = false
  selectedOrder.value = null
}

function confirmDelete(id: string) {
  confirmDeleteId.value = id
}

async function handleDelete() {
  if (!confirmDeleteId.value) return
  try {
    await orderStore.remove(confirmDeleteId.value)
  } catch {
    // Error is handled by the store
  }
  confirmDeleteId.value = null
}

onMounted(() => {
  orderStore.fetchAll()
  customerStore.fetchAll()
  productStore.fetchAll()
  ingredientStore.fetchAll()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Pedidos</h1>
      <button class="btn btn-primary" data-testid="new-order-button" @click="openCreateModal">
        + Novo Pedido
      </button>
    </div>

    <div v-if="orderStore.error" class="alert alert-error">{{ orderStore.error }}</div>

    <div v-if="orderStore.loading" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="orderStore.items.length === 0" class="empty-state">
      <p>Nenhum pedido cadastrado.</p>
      <button class="btn btn-primary" @click="openCreateModal">Criar primeiro pedido</button>
    </div>

    <div v-else class="table-container">
      <table>
        <thead>
          <tr>
            <th>Data/Hora</th>
            <th>Cliente</th>
            <th>Status</th>
            <th>Valor Total</th>
            <th>Lucro Estimado</th>
            <th style="width: 200px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orderStore.items" :key="order.id">
            <td>{{ formatDateTime(order.dateTime) }}</td>
            <td>{{ order.customerName }}</td>
            <td>
              <span :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
            </td>
            <td>{{ formatCurrency(order.totalValue) }}</td>
            <td>{{ formatCurrency(order.estimatedProfit) }}</td>
            <td>
              <div class="table-actions">
                <button class="btn btn-secondary btn-sm" @click="viewDetail(order)">
                  Detalhes
                </button>
                <button class="btn btn-danger btn-sm" @click="confirmDelete(order.id)">
                  Excluir
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal modal-wide">
        <div class="modal-header">
          <h2>Novo Pedido</h2>
          <button class="modal-close" @click="closeModal">✕</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>Cliente</label>
              <select
                v-model="form.customerId"
                class="form-control"
                data-testid="order-customer-select"
                required
              >
                <option value="" disabled>Selecione o cliente...</option>
                <option
                  v-for="customer in customerStore.items"
                  :key="customer.id"
                  :value="customer.id"
                >
                  {{ customer.name }}
                </option>
              </select>
            </div>

            <div style="margin-bottom: 16px">
              <label style="font-weight: 600; margin-bottom: 8px; display: block">
                Itens do Pedido
              </label>
              <div
                v-for="(item, index) in form.items"
                :key="index"
                class="order-items-row"
              >
                <div class="form-group">
                  <label>Produto</label>
                  <select
                    v-model="item.productId"
                    class="form-control"
                    :data-testid="`order-item-${index}-product-select`"
                    required
                  >
                    <option value="" disabled>Selecione...</option>
                    <option
                      v-for="product in productStore.items"
                      :key="product.id"
                      :value="product.id"
                    >
                      {{ product.name }} — {{ formatCurrency(product.price) }}
                    </option>
                  </select>
                </div>
                <div class="form-group" style="max-width: 120px">
                  <label>Qtd</label>
                  <input
                    v-model.number="item.quantity"
                    type="number"
                    min="1"
                    class="form-control"
                    :data-testid="`order-item-${index}-quantity-input`"
                    required
                  />
                </div>
                <button
                  v-if="form.items.length > 1"
                  type="button"
                  class="btn btn-danger btn-sm"
                  style="margin-bottom: 0"
                  @click="removeItem(index)"
                >
                  ✕
                </button>

                <div class="order-extras" style="grid-column: 1 / -1">
                  <label style="font-weight: 600; margin-bottom: 6px; display: block">
                    Ingredientes extras
                  </label>

                  <div
                    v-for="(extra, extraIndex) in ensureExtrasArray(item)"
                    :key="extraIndex"
                    class="order-extras-row"
                    style="display: flex; gap: 8px; align-items: flex-end; margin-bottom: 8px"
                  >
                    <div class="form-group" style="flex: 1">
                      <label>Ingrediente</label>
                      <select
                        v-model="extra.ingredientId"
                        class="form-control"
                        :data-testid="`order-item-${index}-extra-${extraIndex}-ingredient-select`"
                        required
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

                    <div class="form-group" style="max-width: 160px">
                      <label>Quantidade</label>
                      <input
                        v-model.number="extra.quantity"
                        type="number"
                        step="0.01"
                        min="0.01"
                        class="form-control"
                        :data-testid="`order-item-${index}-extra-${extraIndex}-quantity-input`"
                        required
                      />
                    </div>

                    <button
                      type="button"
                      class="btn btn-danger btn-sm"
                      @click="removeExtraIngredient(index, extraIndex)"
                    >
                      ✕
                    </button>
                  </div>

                  <button
                    type="button"
                    class="btn btn-secondary btn-sm"
                    :data-testid="`order-item-${index}-add-extra-button`"
                    @click="addExtraIngredient(index)"
                  >
                    + Adicionar ingrediente extra
                  </button>
                </div>
              </div>
              <button type="button" class="btn btn-secondary btn-sm" @click="addItem">
                + Adicionar Item
              </button>
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModal">
                Cancelar
              </button>
              <button
                type="submit"
                class="btn btn-primary"
                data-testid="order-submit-button"
                :disabled="orderStore.loading"
              >
                Criar Pedido
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Order Detail Modal -->
    <div v-if="showDetailModal && selectedOrder" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal modal-wide">
        <div class="modal-header">
          <h2>Detalhes do Pedido</h2>
          <button class="modal-close" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px">
            <div>
              <strong>Cliente:</strong> {{ selectedOrder.customerName }}
            </div>
            <div>
              <strong>Data:</strong> {{ formatDateTime(selectedOrder.dateTime) }}
            </div>
            <div>
              <strong>Status:</strong>
              <span :class="statusClass(selectedOrder.status)" style="margin-left: 4px">
                {{ statusLabel(selectedOrder.status) }}
              </span>
            </div>
            <div>
              <strong>Valor Total:</strong> {{ formatCurrency(selectedOrder.totalValue) }}
            </div>
            <div>
              <strong>Lucro Estimado:</strong>
              {{ formatCurrency(selectedOrder.estimatedProfit) }}
            </div>
          </div>

          <h3 style="font-size: 0.875rem; font-weight: 600; margin-bottom: 8px">Itens</h3>
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Quantidade</th>
                  <th>Preço Unitário</th>
                  <th>Subtotal</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedOrder.items" :key="item.id">
                  <td>{{ item.productName }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatCurrency(item.unitPrice) }}</td>
                  <td>{{ formatCurrency(item.unitPrice * item.quantity) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="form-actions">
            <button class="btn btn-secondary" @click="closeDetailModal">Fechar</button>
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
          <p>Tem certeza que deseja excluir este pedido?</p>
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

