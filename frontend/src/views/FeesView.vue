<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useFeeStore } from '@/stores/feeStore'
import {
  UI,
  UITopbar,
  UIBtn,
  UISearch,
  UIField,
  UIInput,
  UIModal,
  UIIcon,
  UIRowAction,
  UIEmpty,
} from '@/design'
import type { FeeRequest, FeeResponse } from '@/types/Fee'

const store = useFeeStore()

const showModal = ref(false)
const editing = ref<FeeResponse | null>(null)
const form = ref<FeeRequest>({ name: '', feeRate: 0 })
const confirmDeleteId = ref<string | null>(null)

function openCreate() {
  editing.value = null
  form.value = { name: '', feeRate: 0 }
  showModal.value = true
}
function openEdit(f: FeeResponse) {
  editing.value = f
  form.value = { name: f.name, feeRate: Number(f.feeRate) }
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
    /* error in store */
  }
}
async function handleDelete() {
  if (!confirmDeleteId.value) return
  try {
    await store.remove(confirmDeleteId.value)
  } catch {
    /* error in store */
  }
  confirmDeleteId.value = null
}

let searchDebounce: ReturnType<typeof setTimeout> | null = null
function onSearchInput(v: string) {
  store.search = v
  if (searchDebounce) clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => store.fetchPage({ search: v, page: 0 }), 300)
}
function onPageChange(p: number) {
  if (p < 0 || p >= store.totalPages) return
  store.fetchPage({ page: p })
}

const cols = '1.4fr 1.2fr 130px 120px'

onMounted(() => {
  store.fetchPage({ page: 0, search: '' })
})
</script>

<template>
  <div style="display: flex; flex-direction: column; flex: 1">
    <UITopbar
      title="Taxas"
      :subtitle="`${store.totalElements} taxas configuradas`"
    >
      <template #actions>
        <UIBtn icon="plus" variant="dark" @click="openCreate">Nova Taxa</UIBtn>
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
      <div
        v-if="store.error"
        :style="{
          padding: '10px 14px',
          background: UI.roseBg,
          color: UI.rose2,
          borderRadius: '10px',
          fontSize: '13px',
          marginBottom: '12px',
        }"
      >
        {{ store.error }}
      </div>

      <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 18px; flex-wrap: wrap">
        <UISearch
          :model-value="store.search"
          placeholder="Buscar taxa por nome…"
          :width="340"
          @update:model-value="onSearchInput"
        />
        <div style="flex: 1" />
      </div>

      <UIEmpty
        v-if="!store.loading && !store.items.length && !store.search"
        icon="card"
        accent="blue"
        title="Configure taxas de pagamento e entrega"
        body="Crie taxas para cada forma de pagamento ou tipo de pedido. Elas aparecem como opção ao registrar um pedido novo."
        primary="Criar taxa"
        @primary="openCreate"
      />

      <div
        v-else
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
          <span>Nome</span>
          <span>Detalhe</span>
          <span style="text-align: right">Taxa</span>
          <span style="text-align: right">Ações</span>
        </div>

        <div style="flex: 1; overflow: auto">
          <div
            v-if="store.loading"
            :style="{ padding: '32px', textAlign: 'center', color: UI.textMute }"
          >
            Carregando…
          </div>
          <div
            v-else-if="!store.items.length"
            :style="{ padding: '60px 32px', textAlign: 'center', color: UI.textMute, fontSize: '13px' }"
          >
            Nenhuma taxa encontrada.
          </div>
          <div
            v-for="(t, i) in store.items"
            v-else
            :key="t.id"
            class="ui-row"
            :style="{
              display: 'grid',
              gridTemplateColumns: cols,
              gap: '12px',
              padding: '12px 18px',
              borderBottom: i === store.items.length - 1 ? 'none' : `1px solid ${UI.borderSub}`,
              fontSize: '13px',
              color: UI.text,
              alignItems: 'center',
            }"
          >
            <span style="display: flex; align-items: center; gap: 10px; min-width: 0">
              <span
                :style="{
                  width: '30px',
                  height: '30px',
                  borderRadius: '8px',
                  background: UI.blueBg,
                  color: UI.blue,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }"
              >
                <UIIcon name="card" :size="15" />
              </span>
              <span
                :style="{
                  fontWeight: 600,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }"
              >
                {{ t.name }}
              </span>
            </span>
            <span :style="{ color: UI.textSub, fontSize: '12px' }">
              Aplicada sobre o subtotal do pedido
            </span>
            <span
              :style="{
                textAlign: 'right',
                fontWeight: 600,
                fontVariantNumeric: 'tabular-nums',
              }"
            >
              {{ Number(t.feeRate).toFixed(2).replace('.', ',') }}%
            </span>
            <span style="display: flex; gap: 5px; justify-content: flex-end">
              <UIRowAction icon="edit" color="blue" label="Editar" @click="openEdit(t)" />
              <UIRowAction icon="trash" color="rose" label="Excluir" @click="confirmDeleteId = t.id" />
            </span>
          </div>
        </div>

        <div
          :style="{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 18px',
            borderTop: `1px solid ${UI.border}`,
            background: UI.bgSoft,
            fontSize: '12px',
            color: UI.textSub,
            flexShrink: 0,
          }"
        >
          <span>
            Página {{ store.page + 1 }} de {{ Math.max(store.totalPages, 1) }}
            · {{ store.totalElements }} taxas
          </span>
          <div style="display: flex; gap: 6px; align-items: center">
            <UIBtn
              size="sm"
              icon="chevLeft"
              variant="secondary"
              :disabled="store.page === 0 || store.loading"
              @click="onPageChange(store.page - 1)"
            >
              Anterior
            </UIBtn>
            <UIBtn
              size="sm"
              icon="chevRight"
              variant="secondary"
              :disabled="store.page >= store.totalPages - 1 || store.loading"
              @click="onPageChange(store.page + 1)"
            >
              Próximo
            </UIBtn>
          </div>
        </div>
      </div>
    </div>

    <UIModal
      v-if="showModal"
      :title="editing ? 'Editar Taxa' : 'Nova Taxa'"
      subtitle="Configure uma taxa de pagamento ou serviço"
      :width="520"
      @close="closeModal"
    >
      <form id="fee-form" @submit.prevent="handleSubmit">
        <div style="display: flex; flex-direction: column; gap: 14px">
          <UIField label="Nome">
            <UIInput v-model="form.name" placeholder="Ex: Crédito, Débito, Pix, Dinheiro" />
          </UIField>
          <UIField label="Taxa (%)" hint="Aplicada como percentual sobre o valor do pedido">
            <UIInput v-model.number="form.feeRate" type="number" placeholder="Ex: 2.5" />
          </UIField>
        </div>
      </form>

      <template #footer>
        <UIBtn
          v-if="editing"
          variant="softDanger"
          icon="trash"
          @click="
            () => {
              if (editing) confirmDeleteId = editing.id
              closeModal()
            }
          "
        >
          Excluir
        </UIBtn>
        <div style="flex: 1" />
        <UIBtn variant="secondary" @click="closeModal">Cancelar</UIBtn>
        <UIBtn variant="primary" icon="check" :disabled="store.loading" @click="handleSubmit">
          {{ editing ? 'Salvar' : 'Criar taxa' }}
        </UIBtn>
      </template>
    </UIModal>

    <UIModal
      v-if="confirmDeleteId"
      title="Excluir taxa"
      subtitle="Esta ação não pode ser desfeita"
      :width="420"
      @close="confirmDeleteId = null"
    >
      <p :style="{ color: UI.textSub, fontSize: '13.5px', lineHeight: 1.6 }">
        Tem certeza que deseja excluir esta taxa?
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
