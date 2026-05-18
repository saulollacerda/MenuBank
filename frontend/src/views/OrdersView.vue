<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useOrderStore } from '@/stores/orderStore'
import { useCustomerStore } from '@/stores/customerStore'
import { useProductStore } from '@/stores/productStore'
import { useIngredientStore } from '@/stores/ingredientStore'
import { usePaymentMethodStore } from '@/stores/paymentMethodStore'
import { useAnotaAIStore } from '@/stores/anotaAIStore'
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
const paymentMethodStore = usePaymentMethodStore()
const anotaAIStore = useAnotaAIStore()

async function handleSyncAnotaAI() {
  anotaAIStore.clearResult()
  try {
    await anotaAIStore.syncOrders()
  } catch {
    // erro fica em anotaAIStore.error
  }
}

const showModal = ref(false)
const showDetailModal = ref(false)
const selectedOrder = ref<OrderResponse | null>(null)
const confirmDeleteId = ref<string | null>(null)
const editingOrderId = ref<string | null>(null)

const form = ref<OrderRequest>({
  customerId: '',
  items: [{ productId: '', quantity: 1, extraIngredients: [] }],
})

function ensureExtrasArray(item: OrderItemRequest): OrderItemExtraIngredientRequest[] {
  if (!item.extraIngredients) item.extraIngredients = []
  return item.extraIngredients
}

function formatCurrency(value: number | null | undefined): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(value ?? 0)
}

function formatPercent(value: number | null | undefined): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'percent',
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(value ?? 0)
}

function orderTotalCost(order: OrderResponse): number {
  return order.items.reduce((sum, item) => sum + (item.totalCost ?? 0), 0)
}

function orderMargin(order: OrderResponse): number {
  if (!order.totalValue) return 0
  return order.estimatedProfit / order.totalValue
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
  editingOrderId.value = null
  form.value = {
    customerId: '',
    items: [{ productId: '', quantity: 1, extraIngredients: [] }],
  }
  showModal.value = true
}

function openEditModal(order: OrderResponse) {
  editingOrderId.value = order.id
  form.value = {
    customerId: order.customerId,
    status: order.status,
    paymentMethodId: order.paymentMethodId,
    items: order.items.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
      extraIngredients: (item.extraIngredients ?? []).map((extra) => ({
        ingredientId: extra.ingredientId,
        quantity: extra.quantity,
      })),
    })),
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingOrderId.value = null
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
  if (!item) return
  ensureExtrasArray(item).push({ ingredientId: '', quantity: 1 })
}

function onExtraIngredientChange(extra: OrderItemExtraIngredientRequest, ingredientId: string) {
  extra.ingredientId = ingredientId
  const ingredient = ingredientStore.items.find((i) => i.id === ingredientId)
  extra.quantity = ingredient?.defaultQuantity ?? 1
}

function removeExtraIngredient(itemIndex: number, extraIndex: number) {
  const item = form.value.items[itemIndex]
  if (!item) return
  const extras = ensureExtrasArray(item)
  extras.splice(extraIndex, 1)
}

async function handleSubmit() {
  try {
    if (editingOrderId.value) {
      await orderStore.update(editingOrderId.value, form.value)
    } else {
      await orderStore.create(form.value)
    }
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
  paymentMethodStore.fetchAll()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Pedidos</h1>
      <div class="page-header-actions">
        <button
          class="btn btn-secondary"
          data-testid="sync-anotaai-orders-button"
          :disabled="anotaAIStore.syncingOrders"
          @click="handleSyncAnotaAI"
        >
          <span v-if="anotaAIStore.syncingOrders" class="spinner spinner-sm"></span>
          <span v-else>📥 Importar do Anota.AI</span>
        </button>
        <button class="btn btn-primary" data-testid="new-order-button" @click="openCreateModal">
          + Novo Pedido
        </button>
      </div>
    </div>

    <div v-if="anotaAIStore.error" class="alert alert-error">{{ anotaAIStore.error }}</div>
    <div
      v-if="anotaAIStore.lastResult && !anotaAIStore.error"
      class="alert alert-success"
    >
      {{ anotaAIStore.lastResult.ordersImported }} pedido(s) importado(s).
      {{ anotaAIStore.lastResult.ordersSkipped }} já existente(s).
      <span v-if="anotaAIStore.lastResult.errors.length > 0">
        ({{ anotaAIStore.lastResult.errors.length }} erro(s) ignorado(s))
      </span>
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
            <td>
              {{ order.customerName }}
              <span v-if="order.origin === 'ANOTA_AI'" class="badge badge-anotaai">Anota.AI</span>
            </td>
            <td>
              <span :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
            </td>
            <td>{{ formatCurrency(order.totalValue) }}</td>
            <td>{{ formatCurrency(order.estimatedProfit) }}</td>
            <td>
              <div class="table-actions">
                <button
                  class="btn btn-secondary btn-sm"
                  :data-testid="`order-${order.id}-detail-button`"
                  @click="viewDetail(order)"
                >
                  Detalhes
                </button>
                <button
                  class="btn btn-primary btn-sm"
                  :data-testid="`order-${order.id}-edit-button`"
                  @click="openEditModal(order)"
                >
                  Editar
                </button>
                <button
                  class="btn btn-danger btn-sm"
                  :data-testid="`order-${order.id}-delete-button`"
                  @click="confirmDelete(order.id)"
                >
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
          <h2 data-testid="order-form-title">{{ editingOrderId ? 'Editar Pedido' : 'Novo Pedido' }}</h2>
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

            <div v-if="editingOrderId" class="form-group">
              <label>Status</label>
              <select
                v-model="form.status"
                class="form-control"
                data-testid="order-status-select"
              >
                <option value="PENDING">Pendente</option>
                <option value="PAID">Pago</option>
                <option value="CANCELLED">Cancelado</option>
              </select>
            </div>

            <div class="form-group">
              <label>Forma de Pagamento</label>
              <select
                v-model="form.paymentMethodId"
                class="form-control"
                data-testid="order-payment-method-select"
              >
                <option :value="undefined">Nenhuma</option>
                <option
                  v-for="pm in paymentMethodStore.items"
                  :key="pm.id"
                  :value="pm.id"
                >
                  {{ pm.name }} ({{ pm.feeRate }}%)
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
                        :value="extra.ingredientId"
                        class="form-control"
                        :data-testid="`order-item-${index}-extra-${extraIndex}-ingredient-select`"
                        required
                        @change="
                          onExtraIngredientChange(extra, ($event.target as HTMLSelectElement).value)
                        "
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
                        step="0.0001"
                        min="0.0001"
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
                {{ editingOrderId ? 'Salvar Alterações' : 'Criar Pedido' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Order Detail Modal -->
    <div
      v-if="showDetailModal && selectedOrder"
      class="modal-overlay"
      data-testid="order-detail-modal"
      @click.self="closeDetailModal"
    >
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
              <strong>Total de custos:</strong>
              <span data-testid="order-detail-total-cost">
                {{ formatCurrency(orderTotalCost(selectedOrder)) }}
              </span>
            </div>
            <div>
              <strong>Lucro Estimado:</strong>
              <span data-testid="order-detail-estimated-profit">
                {{ formatCurrency(selectedOrder.estimatedProfit) }}
              </span>
            </div>
            <div>
              <strong>Margem:</strong>
              <span data-testid="order-detail-margin">
                {{ formatPercent(orderMargin(selectedOrder)) }}
              </span>
            </div>
            <div v-if="selectedOrder.paymentMethodName">
              <strong>Forma de Pagamento:</strong>
              {{ selectedOrder.paymentMethodName }} ({{ selectedOrder.feeRate }}%)
            </div>
          </div>

          <h3 style="font-size: 0.875rem; font-weight: 600; margin-bottom: 8px">Itens</h3>
          <div class="order-detail-items">
            <div
              v-for="item in selectedOrder.items"
              :key="item.id"
              class="order-detail-item"
              :data-testid="`order-detail-item-${item.id}`"
              style="border: 1px solid #e2e8f0; border-radius: 6px; padding: 12px; margin-bottom: 12px"
            >
              <div
                style="display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 8px"
              >
                <div><strong>Produto:</strong> {{ item.productName }}</div>
                <div><strong>Qtd:</strong> {{ item.quantity }}</div>
                <div><strong>Preço Unit.:</strong> {{ formatCurrency(item.unitPrice) }}</div>
                <div><strong>Subtotal:</strong> {{ formatCurrency(item.unitPrice * item.quantity) }}</div>
              </div>
              <div
                style="display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; color: #475569"
              >
                <div><strong>Custo Unit.:</strong> {{ formatCurrency(item.unitCost) }}</div>
                <div><strong>Custo Total:</strong> {{ formatCurrency(item.totalCost) }}</div>
                <div>
                  <strong>Lucro do item:</strong>
                  {{ formatCurrency(item.unitPrice * item.quantity - item.totalCost) }}
                </div>
              </div>

              <div
                v-if="item.extraIngredients && item.extraIngredients.length"
                style="margin-top: 10px"
              >
                <div style="font-weight: 600; margin-bottom: 4px">Ingredientes extras</div>
                <table style="width: 100%; font-size: 0.875rem">
                  <thead>
                    <tr>
                      <th style="text-align: left">Ingrediente</th>
                      <th style="text-align: right">Quantidade</th>
                      <th style="text-align: right">Custo Unit.</th>
                      <th style="text-align: right">Custo Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="extra in item.extraIngredients" :key="extra.id">
                      <td>{{ extra.ingredientName }}</td>
                      <td style="text-align: right">
                        {{ extra.quantity }} {{ extra.ingredientUnit }}
                      </td>
                      <td style="text-align: right">{{ formatCurrency(extra.costPerUnit) }}</td>
                      <td style="text-align: right">{{ formatCurrency(extra.totalCost) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
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

<style scoped>
.page-header-actions {
  display: flex;
  gap: 0.5rem;
}

.badge-anotaai {
  display: inline-block;
  margin-left: 0.5rem;
  padding: 0.125rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #fff;
  background: #ef4444;
  border-radius: 999px;
}
</style>

