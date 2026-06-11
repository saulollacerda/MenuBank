<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePolling } from '@/composables/usePolling'
import { useAuthStore } from '@/stores/authStore'
import { useOrderStore } from '@/stores/orderStore'
import { useCustomerStore } from '@/stores/customerStore'
import { useProductStore } from '@/stores/productStore'
import { useIngredientStore } from '@/stores/ingredientStore'
import { useFeeStore } from '@/stores/feeStore'
import { useAnotaAIStore } from '@/stores/anotaAIStore'
import { useNotificationStore } from '@/stores/notificationStore'
import {
  UI,
  UITopbar,
  UIBtn,
  UIPill,
  UISearch,
  UIField,
  UIInput,
  UISelect,
  UIModal,
  UIIcon,
  UIRowAction,
  brl,
} from '@/design'
import type {
  OrderRequest,
  OrderResponse,
  OrderItemRequest,
  OrderItemExtraIngredientRequest,
  OrderOrigin,
} from '@/types/Order'

const router = useRouter()
const authStore = useAuthStore()
const orderStore = useOrderStore()
const customerStore = useCustomerStore()
const productStore = useProductStore()
const ingredientStore = useIngredientStore()
const feeStore = useFeeStore()
const anotaAIStore = useAnotaAIStore()
const notificationStore = useNotificationStore()

const showHoursBanner = computed(() => {
  const u = authStore.currentUser
  return !!u?.anotaAiApiKey && (!u.openingHours || u.openingHours.length === 0)
})

function goToHoursSettings() {
  router.push({ path: '/settings', query: { section: 'horario' } })
}

const showModal = ref(false)
const showDetailModal = ref(false)
const loadingDetail = ref(false)
const selectedOrder = ref<OrderResponse | null>(null)
const confirmDeleteId = ref<string | null>(null)
const editingOrderId = ref<string | null>(null)

const form = ref<OrderRequest>({
  customerId: '',
  origin: 'MENUBANK',
  items: [{ productId: '', quantity: 1, extraIngredients: [] }],
})

function ensureExtras(item: OrderItemRequest): OrderItemExtraIngredientRequest[] {
  if (!item.extraIngredients) item.extraIngredients = []
  return item.extraIngredients
}

function formatDateTime(s: string): string {
  return new Date(s).toLocaleString('pt-BR')
}
function timeOf(iso: string): string {
  const d = new Date(iso)
  const date = d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
  const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  return `${date} ${time}`
}
function pctFmt(v: number): string {
  return (v * 100).toFixed(1).replace('.', ',') + '%'
}

const STATUS_PILL: Record<string, { color: 'amber' | 'emerald' | 'rose' | 'blue'; label: string }> = {
  PENDING: { color: 'amber', label: 'Pendente' },
  PAID: { color: 'emerald', label: 'Pago' },
  CANCELLED: { color: 'rose', label: 'Cancelado' },
}

function originLabel(o: OrderOrigin | undefined): string {
  if (o === 'ANOTA_AI') return 'Anota.AI'
  if (o === 'IFOOD') return 'iFood'
  return 'MenuBank'
}
function originColor(o: OrderOrigin | undefined): 'blue' | 'rose' | 'violet' {
  if (o === 'ANOTA_AI') return 'blue'
  if (o === 'IFOOD') return 'rose'
  return 'violet'
}

function orderTotalCost(o: OrderResponse): number {
  return o.items.reduce((s, it) => s + (Number(it.totalCost) || 0), 0)
}
function orderMargin(o: OrderResponse): number {
  if (!o.totalValue) return 0
  return Number(o.estimatedProfit) / Number(o.totalValue)
}

const counts = computed(() => {
  const items = orderStore.items
  return {
    total: orderStore.totalElements,
    pending: items.filter((o) => o.status === 'PENDING').length,
    paid: items.filter((o) => o.status === 'PAID').length,
    cancelled: items.filter((o) => o.status === 'CANCELLED').length,
  }
})

const filterStatus = ref<'todos' | 'PENDING' | 'PAID' | 'CANCELLED'>('todos')
const filteredItems = computed(() => {
  if (filterStatus.value === 'todos') return orderStore.items
  return orderStore.items.filter((o) => o.status === filterStatus.value)
})

let searchDebounce: ReturnType<typeof setTimeout> | null = null
function onSearchInput(value: string) {
  orderStore.search = value
  if (searchDebounce) clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => {
    orderStore.fetchPage({ search: value, page: 0 })
  }, 300)
}
function onSortChange(value: string) {
  orderStore.fetchPage({ sort: value, page: 0 })
}
function onPageChange(p: number) {
  if (p < 0 || p >= orderStore.totalPages) return
  orderStore.fetchPage({ page: p })
}

async function handleSyncAnotaAI() {
  anotaAIStore.clearResult()
  try {
    await anotaAIStore.syncOrders()
  } catch {
    /* error in store */
  } finally {
    notificationStore.refreshCount()
  }
}

function openCreate() {
  editingOrderId.value = null
  form.value = {
    customerId: '',
    origin: 'MENUBANK',
    feeId: '',
    items: [{ productId: '', quantity: 1, extraIngredients: [] }],
  }
  showModal.value = true
}
function openEdit(o: OrderResponse) {
  editingOrderId.value = o.id
  form.value = {
    customerId: o.customerId,
    status: o.status,
    feeId: o.feeId ?? '',
    origin: o.origin,
    items: o.items.map((item) => ({
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
function removeItem(i: number) {
  if (form.value.items.length > 1) form.value.items.splice(i, 1)
}
function addExtra(i: number) {
  ensureExtras(form.value.items[i]!).push({ ingredientId: '', quantity: 1 })
}
function removeExtra(i: number, j: number) {
  ensureExtras(form.value.items[i]!).splice(j, 1)
}
function onExtraChange(extra: OrderItemExtraIngredientRequest, id: string) {
  extra.ingredientId = id
  const ing = ingredientStore.items.find((x) => x.id === id)
  extra.quantity = ing?.defaultQuantity ?? 1
}
async function handleSubmit() {
  try {
    const payload = { ...form.value, feeId: form.value.feeId || undefined }
    if (editingOrderId.value) {
      await orderStore.update(editingOrderId.value, payload)
    } else {
      await orderStore.create(payload)
    }
    closeModal()
  } catch {
    /* store has error */
  }
}
async function viewDetail(o: OrderResponse) {
  showDetailModal.value = true
  loadingDetail.value = true
  selectedOrder.value = null
  try {
    selectedOrder.value = await orderStore.findById(o.id)
  } catch {
    showDetailModal.value = false
  } finally {
    loadingDetail.value = false
  }
}
function closeDetail() {
  showDetailModal.value = false
  selectedOrder.value = null
}
async function handleDelete() {
  if (!confirmDeleteId.value) return
  try {
    await orderStore.remove(confirmDeleteId.value)
  } catch {
    /* store has error */
  }
  confirmDeleteId.value = null
}

const cols = '70px 1.5fr 100px 110px 110px 110px 90px 130px'

const statusPills = computed(() => [
  { id: 'todos', label: 'Todos', count: counts.value.total, dot: undefined },
  { id: 'PENDING', label: 'Pendentes', count: counts.value.pending, dot: UI.amber },
  { id: 'PAID', label: 'Pagos', count: counts.value.paid, dot: UI.emerald2 },
  { id: 'CANCELLED', label: 'Cancelados', count: counts.value.cancelled, dot: UI.rose },
])

onMounted(() => {
  orderStore.fetchPage({ page: 0, search: '' })
  customerStore.fetchAll()
  productStore.fetchAll()
  ingredientStore.fetchAll()
  feeStore.fetchAll()
})

usePolling(() => { orderStore.fetchPage({}, true).catch(() => {}) }, 30_000)
</script>

<template>
  <div style="display: flex; flex-direction: column; flex: 1">
    <UITopbar
      title="Pedidos"
      :subtitle="`${counts.total} pedidos no total`"
    >
      <template #actions>
        <UIBtn
          icon="download"
          variant="secondary"
          :disabled="anotaAIStore.syncingOrders"
          @click="handleSyncAnotaAI"
        >
          {{ anotaAIStore.syncingOrders ? 'Importando…' : 'Importar do Anota.AI' }}
        </UIBtn>
        <UIBtn icon="plus" variant="dark" data-testid="new-order-button" @click="openCreate">
          Novo Pedido
        </UIBtn>
      </template>
    </UITopbar>

    <div
      style="
        flex: 1;
        padding: 28px;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      "
    >
      <!-- Opening hours onboarding banner -->
      <div
        v-if="showHoursBanner"
        :style="{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '12px 16px',
          background: UI.amberBg,
          color: UI.amber2,
          borderRadius: '10px',
          fontSize: '13px',
          marginBottom: '12px',
        }"
      >
        <UIIcon name="clock" :size="16" style="flex-shrink: 0" />
        <span style="flex: 1">
          Configure os horários de funcionamento para ativar a importação automática de pedidos.
        </span>
        <button
          :style="{
            background: UI.amber2,
            color: UI.amberBg,
            border: 'none',
            borderRadius: '7px',
            padding: '5px 12px',
            fontSize: '12.5px',
            fontWeight: 600,
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }"
          @click="goToHoursSettings"
        >
          Configurar horários
        </button>
      </div>

      <!-- Anota.AI sync alerts -->
      <div
        v-if="anotaAIStore.error"
        :style="{
          padding: '10px 14px',
          background: UI.roseBg,
          color: UI.rose2,
          borderRadius: '10px',
          fontSize: '13px',
          marginBottom: '12px',
        }"
      >
        {{ anotaAIStore.error }}
      </div>
      <div
        v-if="anotaAIStore.lastResult && !anotaAIStore.error"
        :style="{
          padding: '10px 14px',
          background: UI.emeraldBg,
          color: UI.emerald2,
          borderRadius: '10px',
          fontSize: '13px',
          marginBottom: '12px',
        }"
      >
        {{ anotaAIStore.lastResult.ordersImported }} pedido(s) importado(s).
        {{ anotaAIStore.lastResult.ordersSkipped }} já existente(s).
      </div>
      <div
        v-if="anotaAIStore.lastResult && (anotaAIStore.lastResult.missingIngredientNames?.length ?? 0) > 0"
        :style="{
          padding: '10px 14px',
          background: UI.amberBg,
          color: UI.amber2,
          borderRadius: '10px',
          fontSize: '13px',
          marginBottom: '12px',
        }"
      >
        ⚠️ {{ anotaAIStore.lastResult.missingIngredientNames!.length }} ingrediente(s) não encontrado(s):
        <strong>{{ anotaAIStore.lastResult.missingIngredientNames!.join(', ') }}</strong>.
        Abra o sino para cadastrá-los.
      </div>

      <div
        v-if="orderStore.error"
        :style="{
          padding: '10px 14px',
          background: UI.roseBg,
          color: UI.rose2,
          borderRadius: '10px',
          fontSize: '13px',
          marginBottom: '12px',
        }"
      >
        {{ orderStore.error }}
      </div>

      <!-- Filter row -->
      <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 18px; flex-wrap: wrap">
        <div
          :style="{
            display: 'flex',
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '10px',
            padding: '4px',
            gap: '2px',
          }"
        >
          <div
            v-for="s in statusPills"
            :key="s.id"
            :style="{
              display: 'flex',
              alignItems: 'center',
              gap: '7px',
              padding: '6px 12px',
              borderRadius: '7px',
              fontSize: '12.5px',
              fontWeight: 500,
              background: filterStatus === s.id ? UI.bg : 'transparent',
              color: filterStatus === s.id ? UI.text : UI.textSub,
              cursor: 'pointer',
            }"
            @click="filterStatus = (s.id as typeof filterStatus)"
          >
            <span
              v-if="s.dot"
              :style="{ width: '6px', height: '6px', borderRadius: '3px', background: s.dot }"
            />
            {{ s.label }}
            <span
              :style="{
                fontSize: '10.5px',
                padding: '1px 6px',
                borderRadius: '4px',
                background: filterStatus === s.id ? UI.panel : UI.bg,
                color: UI.textMute,
                fontWeight: 600,
              }"
            >
              {{ s.count }}
            </span>
          </div>
        </div>
        <div style="flex: 1" />
        <UISearch
          :model-value="orderStore.search"
          placeholder="Buscar por cliente…"
          :width="260"
          @update:model-value="onSearchInput"
        />
        <UISelect
          :model-value="orderStore.sort"
          :width="220"
          @update:model-value="onSortChange"
        >
          <option value="dateTime,desc">Data: mais recentes</option>
          <option value="dateTime,asc">Data: mais antigos</option>
          <option value="totalValue,desc">Valor: maior → menor</option>
          <option value="totalValue,asc">Valor: menor → maior</option>
        </UISelect>
      </div>

      <!-- Table -->
      <div
        :style="{
          background: UI.panel,
          border: `1px solid ${UI.border}`,
          borderRadius: '14px',
          overflow: 'hidden',
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
        }"
      >
        <div
          :style="{
            display: 'grid',
            gridTemplateColumns: cols,
            gap: '12px',
            padding: '12px 18px',
            background: UI.bgSoft,
            borderBottom: `1px solid ${UI.border}`,
            fontSize: '10.5px',
            color: UI.textSub,
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
            flexShrink: 0,
          }"
        >
          <span>Hora</span>
          <span>Cliente</span>
          <span>Canal</span>
          <span>Status</span>
          <span style="text-align: right">Valor</span>
          <span style="text-align: right">Lucro</span>
          <span style="text-align: right">Margem</span>
          <span style="text-align: right">Ações</span>
        </div>

        <div style="flex: 1; overflow: auto">
          <div
            v-if="orderStore.loading"
            :style="{
              padding: '32px',
              textAlign: 'center',
              color: UI.textMute,
            }"
          >
            Carregando…
          </div>
          <div
            v-else-if="!filteredItems.length"
            :style="{
              padding: '60px 32px',
              textAlign: 'center',
              color: UI.textMute,
              fontSize: '13px',
            }"
          >
            Nenhum pedido encontrado.
          </div>
          <div
            v-for="(o, i) in filteredItems"
            v-else
            :key="o.id"
            class="ui-row"
            :style="{
              display: 'grid',
              gridTemplateColumns: cols,
              gap: '12px',
              padding: '12px 18px',
              borderBottom: i === filteredItems.length - 1 ? 'none' : `1px solid ${UI.borderSub}`,
              fontSize: '13px',
              color: UI.text,
              alignItems: 'center',
            }"
          >
            <span :style="{ color: UI.textSub, fontVariantNumeric: 'tabular-nums' }">{{ timeOf(o.dateTime) }}</span>
            <span style="min-width: 0">
              <span
                :style="{
                  display: 'block',
                  fontWeight: 600,
                  color: UI.text,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }"
              >
                {{ o.customerName }}
              </span>
              <span :style="{ display: 'block', fontSize: '11px', color: UI.textMute }">
                {{ o.items.length }} {{ o.items.length === 1 ? 'item' : 'itens' }}
              </span>
            </span>
            <span>
              <UIPill :color="originColor(o.origin)" size="sm">{{ originLabel(o.origin) }}</UIPill>
            </span>
            <span>
              <UIPill :color="STATUS_PILL[o.status]?.color ?? 'gray'" dot>
                {{ STATUS_PILL[o.status]?.label ?? o.status }}
              </UIPill>
            </span>
            <span
              :style="{
                textAlign: 'right',
                fontWeight: 600,
                color: UI.text,
                fontVariantNumeric: 'tabular-nums',
              }"
            >
              {{ brl(Number(o.totalValue)) }}
            </span>
            <span
              :style="{
                textAlign: 'right',
                color: UI.emerald2,
                fontWeight: 600,
                fontVariantNumeric: 'tabular-nums',
              }"
            >
              {{ brl(Number(o.estimatedProfit)) }}
            </span>
            <span
              :style="{
                textAlign: 'right',
                color: UI.textSub,
                fontVariantNumeric: 'tabular-nums',
              }"
            >
              {{ pctFmt(orderMargin(o)) }}
            </span>
            <span style="display: flex; gap: 5px; justify-content: flex-end">
              <UIRowAction
                icon="eye"
                color="gray"
                label="Detalhes"
                :data-testid="`order-${o.id}-detail-button`"
                @click="viewDetail(o)"
              />
              <UIRowAction
                icon="edit"
                color="blue"
                label="Editar"
                :data-testid="`order-${o.id}-edit-button`"
                @click="openEdit(o)"
              />
              <UIRowAction
                icon="trash"
                color="rose"
                label="Excluir"
                :data-testid="`order-${o.id}-delete-button`"
                @click="confirmDeleteId = o.id"
              />
            </span>
          </div>
        </div>

        <div
          :style="{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 18px',
            borderTop: `1px solid ${UI.border}`,
            background: UI.bgSoft,
            fontSize: '12px',
            color: UI.textSub,
            flexShrink: 0,
          }"
        >
          <span>
            Página {{ orderStore.page + 1 }} de {{ Math.max(orderStore.totalPages, 1) }}
            · {{ orderStore.totalElements }} pedidos
          </span>
          <div style="display: flex; gap: 6px; align-items: center">
            <UIBtn
              size="sm"
              icon="chevLeft"
              variant="secondary"
              :disabled="orderStore.page === 0 || orderStore.loading"
              @click="onPageChange(orderStore.page - 1)"
            >
              Anterior
            </UIBtn>
            <span
              :style="{
                padding: '5px 10px',
                background: UI.text,
                color: '#fff',
                borderRadius: '6px',
                fontSize: '11.5px',
                fontWeight: 600,
              }"
            >
              {{ orderStore.page + 1 }}
            </span>
            <UIBtn
              size="sm"
              icon="chevRight"
              variant="secondary"
              :disabled="orderStore.page >= orderStore.totalPages - 1 || orderStore.loading"
              @click="onPageChange(orderStore.page + 1)"
            >
              Próximo
            </UIBtn>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit modal -->
    <UIModal
      v-if="showModal"
      :title="editingOrderId ? 'Editar Pedido' : 'Novo Pedido'"
      :subtitle="editingOrderId ? 'Atualize os detalhes do pedido' : 'Preencha os detalhes para registrar'"
      :width="680"
      title-test-id="order-form-title"
      @close="closeModal"
    >
      <form id="order-form" @submit.prevent="handleSubmit">
        <div style="display: flex; flex-direction: column; gap: 16px">
          <div
            :style="{
              display: 'grid',
              gridTemplateColumns: editingOrderId ? '1fr 1fr 1fr' : '1fr 1fr',
              gap: '12px',
            }"
          >
            <UIField label="Cliente">
              <UISelect
                v-model="form.customerId"
                placeholder="Selecione o cliente…"
                data-testid="order-customer-select"
              >
                <option v-for="c in customerStore.items" :key="c.id" :value="c.id">
                  {{ c.name }}
                </option>
              </UISelect>
            </UIField>
            <UIField label="Canal">
              <UISelect v-model="form.origin" data-testid="order-origin-select">
                <option value="MENUBANK">MenuBank</option>
                <option value="ANOTA_AI">Anota.AI</option>
                <option value="IFOOD">iFood</option>
              </UISelect>
            </UIField>
            <UIField v-if="editingOrderId" label="Status">
              <UISelect v-model="form.status" data-testid="order-status-select">
                <option value="PENDING">Pendente</option>
                <option value="PAID">Pago</option>
                <option value="CANCELLED">Cancelado</option>
              </UISelect>
            </UIField>
            <UIField label="Taxa">
              <UISelect v-model="form.feeId" data-testid="order-fee-select">
                <option value="">Nenhuma</option>
                <option v-for="f in feeStore.items" :key="f.id" :value="f.id">
                  {{ f.name }} ({{ f.feeRate }}%)
                </option>
              </UISelect>
            </UIField>
          </div>

          <div>
            <div
              :style="{
                fontSize: '13px',
                color: UI.text,
                fontWeight: 600,
                marginBottom: '10px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }"
            >
              <span>Itens do pedido</span>
              <UIBtn size="sm" icon="plus" variant="secondary" @click="addItem">
                Adicionar item
              </UIBtn>
            </div>
            <div style="display: flex; flex-direction: column; gap: 10px">
              <div
                v-for="(item, i) in form.items"
                :key="i"
                :style="{
                  background: UI.bgSoft,
                  border: `1px solid ${UI.border}`,
                  borderRadius: '11px',
                  padding: '14px',
                  position: 'relative',
                }"
              >
                <div style="display: flex; gap: 10px; align-items: flex-end">
                  <UIField label="Produto" width="100%">
                    <UISelect
                      v-model="item.productId"
                      placeholder="Selecionar…"
                      :data-testid="`order-item-${i}-product-select`"
                    >
                      <option v-for="p in productStore.items" :key="p.id" :value="p.id">
                        {{ p.name }} — {{ brl(Number(p.price)) }}
                      </option>
                    </UISelect>
                  </UIField>
                  <UIField label="Qtd" :width="80">
                    <UIInput
                      v-model.number="item.quantity"
                      type="number"
                      :data-testid="`order-item-${i}-quantity-input`"
                    />
                  </UIField>
                  <div
                    v-if="form.items.length > 1"
                    :style="{
                      width: '38px',
                      height: '38px',
                      borderRadius: '9px',
                      background: UI.roseBg,
                      color: UI.rose,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      flexShrink: 0,
                    }"
                    @click="removeItem(i)"
                  >
                    <UIIcon name="trash" :size="14" />
                  </div>
                </div>
                <div
                  :style="{
                    marginTop: '14px',
                    paddingTop: '12px',
                    borderTop: `1px dashed ${UI.border}`,
                  }"
                >
                  <div
                    :style="{
                      fontSize: '11.5px',
                      color: UI.textSub,
                      fontWeight: 600,
                      marginBottom: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                    }"
                  >
                    <span>Ingredientes extras</span>
                    <span
                      :style="{ color: UI.blue, fontSize: '11px', cursor: 'pointer' }"
                      :data-testid="`order-item-${i}-add-extra-button`"
                      @click="addExtra(i)"
                    >
                      + Adicionar extra
                    </span>
                  </div>
                  <div
                    v-if="!ensureExtras(item).length"
                    :style="{ fontSize: '11.5px', color: UI.textMute, fontStyle: 'italic' }"
                  >
                    Nenhum extra
                  </div>
                  <div v-else style="display: flex; flex-direction: column; gap: 6px">
                    <div
                      v-for="(extra, j) in ensureExtras(item)"
                      :key="j"
                      style="display: flex; gap: 8px; align-items: center"
                    >
                      <UISelect
                        :model-value="extra.ingredientId"
                        width="100%"
                        :data-testid="`order-item-${i}-extra-${j}-ingredient-select`"
                        @update:model-value="(v) => onExtraChange(extra, v as string)"
                      >
                        <option v-for="ing in ingredientStore.items" :key="ing.id" :value="ing.id">
                          {{ ing.name }} ({{ ing.unit }})
                        </option>
                      </UISelect>
                      <UIInput
                        v-model.number="extra.quantity"
                        type="number"
                        :width="80"
                        :data-testid="`order-item-${i}-extra-${j}-quantity-input`"
                      />
                      <div
                        :style="{
                          width: '30px',
                          height: '30px',
                          borderRadius: '7px',
                          background: UI.bg,
                          color: UI.rose,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          cursor: 'pointer',
                          flexShrink: 0,
                        }"
                        @click="removeExtra(i, j)"
                      >
                        <UIIcon name="x" :size="12" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            :style="{
              padding: '14px',
              background: UI.bg,
              borderRadius: '10px',
              fontSize: '12.5px',
              color: UI.textSub,
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
            }"
          >
            <UIIcon name="info" :size="16" />
            O lucro estimado será calculado automaticamente a partir das fichas técnicas.
          </div>
        </div>
      </form>

      <template #footer>
        <UIBtn variant="secondary" @click="closeModal">Cancelar</UIBtn>
        <UIBtn
          variant="primary"
          icon="check"
          :disabled="orderStore.loading"
          @click="handleSubmit"
        >
          {{ editingOrderId ? 'Salvar alterações' : 'Criar pedido' }}
        </UIBtn>
      </template>
    </UIModal>

    <!-- Detail modal -->
    <UIModal
      v-if="showDetailModal"
      title="Detalhes do Pedido"
      :subtitle="
        selectedOrder
          ? `${selectedOrder.customerName} · ${originLabel(selectedOrder.origin)} · ${formatDateTime(selectedOrder.dateTime)}`
          : ''
      "
      :width="720"
      data-testid="order-detail-modal"
      @close="closeDetail"
    >
      <div v-if="loadingDetail" style="padding: 40px; text-align: center; color: #94a3b8">
        Carregando…
      </div>
      <div
        v-else-if="selectedOrder"
        style="display: flex; flex-direction: column; gap: 18px"
      >
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px">
          <div
            :style="{
              padding: '12px',
              background: UI.bg,
              border: `1px solid ${UI.border}`,
              borderRadius: '10px',
            }"
          >
            <div
              :style="{
                fontSize: '10.5px',
                color: UI.textMute,
                fontWeight: 600,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                marginBottom: '6px',
              }"
            >
              Status
            </div>
            <UIPill :color="STATUS_PILL[selectedOrder.status]?.color ?? 'gray'" dot>
              {{ STATUS_PILL[selectedOrder.status]?.label ?? selectedOrder.status }}
            </UIPill>
          </div>
          <div
            :style="{
              padding: '12px',
              background: UI.bg,
              border: `1px solid ${UI.border}`,
              borderRadius: '10px',
            }"
          >
            <div
              :style="{
                fontSize: '10.5px',
                color: UI.textMute,
                fontWeight: 600,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                marginBottom: '6px',
              }"
            >
              Valor total
            </div>
            <span :style="{ fontSize: '17px', fontWeight: 700 }">
              {{ brl(Number(selectedOrder.totalValue)) }}
            </span>
          </div>
          <div
            :style="{
              padding: '12px',
              background: UI.bg,
              border: `1px solid ${UI.border}`,
              borderRadius: '10px',
            }"
          >
            <div
              :style="{
                fontSize: '10.5px',
                color: UI.textMute,
                fontWeight: 600,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                marginBottom: '6px',
              }"
            >
              Custo
            </div>
            <span
              data-testid="order-detail-total-cost"
              :style="{ fontSize: '17px', fontWeight: 700, color: UI.textSub }"
            >
              {{ brl(Number(selectedOrder.totalCost ?? orderTotalCost(selectedOrder))) }}
            </span>
          </div>
          <div
            :style="{
              padding: '12px',
              background: UI.bg,
              border: `1px solid ${UI.border}`,
              borderRadius: '10px',
            }"
          >
            <div
              :style="{
                fontSize: '10.5px',
                color: UI.textMute,
                fontWeight: 600,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                marginBottom: '6px',
              }"
            >
              Lucro · margem
            </div>
            <span
              data-testid="order-detail-estimated-profit"
              :style="{ fontSize: '17px', fontWeight: 700, color: UI.emerald2 }"
            >
              {{ brl(Number(selectedOrder.estimatedProfit)) }}
            </span>
            <span
              data-testid="order-detail-margin"
              :style="{ fontSize: '11px', color: UI.textSub, marginLeft: '6px' }"
            >
              {{ pctFmt(orderMargin(selectedOrder)) }}
            </span>
          </div>
        </div>

        <div v-if="selectedOrder.deliveryFee && Number(selectedOrder.deliveryFee) > 0">
          <strong>Taxa de entrega:</strong>
          {{ brl(Number(selectedOrder.deliveryFee)) }}
          <span v-if="selectedOrder.feeName" :style="{ color: UI.textSub }">
            · {{ selectedOrder.feeName }} ({{ selectedOrder.feeRate }}%)
          </span>
        </div>

        <div>
          <div :style="{ fontSize: '13px', fontWeight: 700, color: UI.text, marginBottom: '10px' }">
            Itens ({{ selectedOrder.items.length }})
          </div>
          <div style="display: flex; flex-direction: column; gap: 10px">
            <div
              v-for="item in selectedOrder.items"
              :key="item.id"
              :style="{
                background: UI.bgSoft,
                border: `1px solid ${UI.border}`,
                borderRadius: '11px',
                overflow: 'hidden',
              }"
            >
              <div
                :style="{
                  padding: '12px 16px',
                  borderBottom: `1px solid ${UI.border}`,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                }"
              >
                <div
                  :style="{
                    width: '4px',
                    height: '40px',
                    borderRadius: '2px',
                    background: UI.violet,
                    flexShrink: 0,
                  }"
                />
                <div style="flex: 1">
                  <div :style="{ fontSize: '13.5px', fontWeight: 600 }">{{ item.productName }}</div>
                  <div :style="{ fontSize: '11px', color: UI.textMute }">
                    Qtd. {{ item.quantity }} · Unit. {{ brl(Number(item.unitPrice)) }} · Custo
                    {{ brl(Number(item.unitCost)) }}
                  </div>
                </div>
                <div style="text-align: right">
                  <div :style="{ fontSize: '14px', fontWeight: 700 }">
                    {{ brl(Number(item.unitPrice) * Number(item.quantity)) }}
                  </div>
                  <div :style="{ fontSize: '11px', color: UI.emerald2, fontWeight: 600 }">
                    + {{ brl(Number(item.unitPrice) * Number(item.quantity) - Number(item.totalCost)) }}
                  </div>
                </div>
              </div>

              <div
                v-if="(item.insumos && item.insumos.length) || (item.extraIngredients && item.extraIngredients.length)"
                style="padding: 12px 16px"
              >
                <div
                  :style="{
                    fontSize: '11px',
                    color: UI.textSub,
                    fontWeight: 600,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px',
                    marginBottom: '8px',
                  }"
                >
                  Ficha técnica + extras
                </div>
                <div style="display: flex; flex-direction: column; gap: 4px">
                  <div
                    v-for="ins in item.insumos ?? []"
                    :key="'i' + ins.id"
                    :style="{
                      display: 'grid',
                      gridTemplateColumns: '1fr 80px 100px',
                      gap: '10px',
                      padding: '6px 0',
                      fontSize: '12px',
                      alignItems: 'center',
                    }"
                  >
                    <span style="display: flex; align-items: center; gap: 8px">
                      <span
                        :style="{
                          width: '6px',
                          height: '6px',
                          borderRadius: '3px',
                          background: UI.emerald,
                        }"
                      />
                      {{ ins.name }}
                    </span>
                    <span :style="{ color: UI.textSub }">{{ ins.quantity }}</span>
                    <span
                      :style="{
                        textAlign: 'right',
                        fontVariantNumeric: 'tabular-nums',
                        color: UI.textSub,
                      }"
                    >
                      {{ brl(Number(ins.totalCost)) }}
                    </span>
                  </div>
                  <div
                    v-for="ex in item.extraIngredients ?? []"
                    :key="'e' + ex.id"
                    :style="{
                      display: 'grid',
                      gridTemplateColumns: '1fr 80px 100px',
                      gap: '10px',
                      padding: '6px 0',
                      fontSize: '12px',
                      alignItems: 'center',
                    }"
                  >
                    <span style="display: flex; align-items: center; gap: 8px">
                      <span
                        :style="{
                          width: '6px',
                          height: '6px',
                          borderRadius: '3px',
                          background: UI.textMute,
                        }"
                      />
                      {{ ex.ingredientName }} (extra)
                    </span>
                    <span :style="{ color: UI.textSub }">
                      {{ ex.quantity }} {{ ex.ingredientUnit }}
                    </span>
                    <span
                      :style="{
                        textAlign: 'right',
                        fontVariantNumeric: 'tabular-nums',
                        color: UI.textSub,
                      }"
                    >
                      {{ brl(Number(ex.totalCost)) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <UIBtn variant="secondary" @click="closeDetail">Fechar</UIBtn>
        <UIBtn
          v-if="selectedOrder"
          variant="primary"
          icon="edit"
          @click="
            () => {
              const o = selectedOrder!
              closeDetail()
              openEdit(o)
            }
          "
        >
          Editar pedido
        </UIBtn>
      </template>
    </UIModal>

    <!-- Delete confirm modal -->
    <UIModal
      v-if="confirmDeleteId"
      title="Excluir pedido"
      subtitle="Esta ação não pode ser desfeita"
      :width="420"
      @close="confirmDeleteId = null"
    >
      <p :style="{ color: UI.textSub, fontSize: '13.5px', lineHeight: 1.6 }">
        Tem certeza que deseja excluir este pedido?
      </p>
      <template #footer>
        <UIBtn variant="secondary" @click="confirmDeleteId = null">Cancelar</UIBtn>
        <UIBtn variant="danger" icon="trash" @click="handleDelete">Excluir</UIBtn>
      </template>
    </UIModal>
  </div>
</template>

<style scoped>
.ui-row {
  transition: background 0.12s ease;
}
.ui-row:hover {
  background: rgba(15, 23, 42, 0.025);
}
</style>
