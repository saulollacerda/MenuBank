<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCustomerStore } from '@/stores/customerStore'
import {
  UI,
  UITopbar,
  UIBtn,
  UISearch,
  UIField,
  UIInput,
  UIModal,
  UIRowAction,
  UIEmpty,
} from '@/design'
import type { CustomerRequest, CustomerResponse } from '@/types/Customer'

const store = useCustomerStore()

const showModal = ref(false)
const editing = ref<CustomerResponse | null>(null)
const form = ref<CustomerRequest>({ name: '', phone: '', email: '' })
const confirmDeleteId = ref<string | null>(null)

function openCreate() {
  editing.value = null
  form.value = { name: '', phone: '', email: '' }
  showModal.value = true
}
function openEdit(c: CustomerResponse) {
  editing.value = c
  form.value = { name: c.name, phone: c.phone ?? '', email: c.email ?? '' }
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

const cols = '1.4fr 160px 1.2fr 120px'
const tableMinWidth = '640px'

onMounted(() => {
  store.fetchPage({ page: 0, search: '' })
})
</script>

<template>
  <div style="display: flex; flex-direction: column; flex: 1">
    <UITopbar
      title="Clientes"
      :subtitle="`${store.totalElements} clientes cadastrados`"
    >
      <template #actions>
        <UIBtn icon="plus" variant="dark" @click="openCreate">Novo Cliente</UIBtn>
      </template>
    </UITopbar>

    <div class="view-content">
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
          placeholder="Buscar cliente por nome, telefone…"
          :width="340"
          @update:model-value="onSearchInput"
        />
        <div style="flex: 1" />
      </div>

      <UIEmpty
        v-if="!store.loading && !store.items.length && !store.search"
        icon="user"
        accent="violet"
        title="Seus clientes aparecem aqui"
        body="A cada pedido importado ou criado, o cliente é cadastrado automaticamente. Você também pode adicioná-los manualmente."
        primary="Adicionar manualmente"
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
        <div class="table-scroll">
        <div :style="{ minWidth: tableMinWidth }">
        <div
          class="table-sticky-header"
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
          <span>Telefone</span>
          <span>Email</span>
          <span style="text-align: right">Ações</span>
        </div>

        <div>
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
            Nenhum cliente encontrado.
          </div>
          <div
            v-for="(c, i) in store.items"
            v-else
            :key="c.id"
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
                  width: '4px',
                  height: '26px',
                  borderRadius: '2px',
                  background: UI.violet,
                  flexShrink: 0,
                }"
              />
              <span
                :style="{
                  fontWeight: 600,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }"
              >
                {{ c.name }}
              </span>
            </span>
            <span :style="{ color: UI.textSub, fontVariantNumeric: 'tabular-nums' }">
              {{ c.phone || '—' }}
            </span>
            <span :style="{ color: UI.textSub }">{{ c.email || '—' }}</span>
            <span style="display: flex; gap: 5px; justify-content: flex-end">
              <UIRowAction icon="edit" color="blue" label="Editar" @click="openEdit(c)" />
              <UIRowAction icon="trash" color="rose" label="Excluir" @click="confirmDeleteId = c.id" />
            </span>
          </div>
        </div>
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
            · {{ store.totalElements }} clientes
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
      :title="editing ? 'Editar Cliente' : 'Novo Cliente'"
      :subtitle="editing ? 'Atualize os dados do cliente' : 'Cadastre manualmente um cliente'"
      :width="520"
      @close="closeModal"
    >
      <form id="cust-form" @submit.prevent="handleSubmit">
        <div style="display: flex; flex-direction: column; gap: 14px">
          <UIField label="Nome">
            <UIInput v-model="form.name" placeholder="Nome completo" />
          </UIField>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
            <UIField label="Telefone">
              <UIInput v-model="form.phone" type="tel" placeholder="(11) 99999-9999" />
            </UIField>
            <UIField label="Email" hint="Opcional">
              <UIInput v-model="form.email" type="email" placeholder="cliente@email.com" />
            </UIField>
          </div>
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
          {{ editing ? 'Salvar' : 'Criar cliente' }}
        </UIBtn>
      </template>
    </UIModal>

    <UIModal
      v-if="confirmDeleteId"
      title="Excluir cliente"
      subtitle="Esta ação não pode ser desfeita"
      :width="420"
      @close="confirmDeleteId = null"
    >
      <p :style="{ color: UI.textSub, fontSize: '13.5px', lineHeight: 1.6 }">
        Tem certeza que deseja excluir este cliente?
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
