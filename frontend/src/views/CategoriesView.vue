<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCategoryStore } from '@/stores/categoryStore'
import {
  UI,
  UITopbar,
  UIBtn,
  UISearch,
  UIField,
  UIInput,
  UIModal,
  UIIcon,
  UIPill,
  UIRowAction,
  UIEmpty,
} from '@/design'
import type { CategoryRequest, CategoryResponse } from '@/types/Category'
import { catalogOriginLabel, catalogOriginPillColor } from '@/types/Category'

const store = useCategoryStore()

const showModal = ref(false)
const editing = ref<CategoryResponse | null>(null)
const form = ref<CategoryRequest>({ name: '' })
const confirmDeleteId = ref<string | null>(null)
const view = ref<'cards' | 'table'>('cards')

const PALETTE = ['#fbbf24', '#a78bfa', '#60a5fa', '#34d399', '#f87171', '#e879f9', '#22d3ee', '#94a3b8']
function colorFor(name: string): string {
  let h = 0
  for (const c of name) h = (h * 31 + c.charCodeAt(0)) | 0
  return PALETTE[Math.abs(h) % PALETTE.length]!
}

function openCreate() {
  editing.value = null
  form.value = { name: '' }
  showModal.value = true
}
function openEdit(c: CategoryResponse) {
  editing.value = c
  form.value = { name: c.name }
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

onMounted(() => {
  store.fetchPage({ page: 0, search: '' })
})
</script>

<template>
  <div style="display: flex; flex-direction: column; flex: 1">
    <UITopbar
      title="Categorias"
      :subtitle="`${store.totalElements} categorias cadastradas`"
    >
      <template #actions>
        <UIBtn icon="plus" variant="dark" @click="openCreate">Nova Categoria</UIBtn>
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
          placeholder="Buscar categoria por nome…"
          :width="340"
          @update:model-value="onSearchInput"
        />
        <div style="flex: 1" />
        <div
          :style="{
            display: 'flex',
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '9px',
            padding: '3px',
          }"
        >
          <div
            :style="{
              padding: '5px 10px',
              borderRadius: '6px',
              background: view === 'cards' ? UI.bg : 'transparent',
              fontSize: '12px',
              fontWeight: view === 'cards' ? 600 : 500,
              color: view === 'cards' ? UI.text : UI.textSub,
              cursor: 'pointer',
            }"
            @click="view = 'cards'"
          >
            Cards
          </div>
          <div
            :style="{
              padding: '5px 10px',
              borderRadius: '6px',
              background: view === 'table' ? UI.bg : 'transparent',
              fontSize: '12px',
              fontWeight: view === 'table' ? 600 : 500,
              color: view === 'table' ? UI.text : UI.textSub,
              cursor: 'pointer',
            }"
            @click="view = 'table'"
          >
            Tabela
          </div>
        </div>
      </div>

      <UIEmpty
        v-if="!store.loading && !store.items.length && !store.search"
        icon="folder"
        accent="amber"
        title="Organize seu cardápio em categorias"
        body="Categorias agrupam produtos por tipo (Combos, Bebidas, Sobremesas…) e aparecem como filtro nos pedidos."
        primary="Criar categoria"
        @primary="openCreate"
      />

      <div
        v-else-if="view === 'cards'"
        style="flex: 1; overflow: auto; display: flex; flex-direction: column; gap: 16px"
      >
        <div
          v-if="store.loading"
          :style="{ padding: '32px', textAlign: 'center', color: UI.textMute }"
        >
          Carregando…
        </div>
        <div
          v-else-if="!store.items.length"
          :style="{ padding: '32px', textAlign: 'center', color: UI.textMute, fontSize: '13px' }"
        >
          Nenhuma categoria encontrada.
        </div>
        <div
          v-else
          class="grid-cols-4"
          style="gap: 14px"
        >
          <div
            v-for="c in store.items"
            :key="c.id"
            :style="{
              background: UI.panel,
              border: `1px solid ${UI.border}`,
              borderRadius: '14px',
              display: 'flex',
              flexDirection: 'column',
              position: 'relative',
              overflow: 'hidden',
            }"
            class="ui-card-hover"
          >
            <div :style="{ height: '6px', background: colorFor(c.name) }" />
            <div style="padding: 18px; display: flex; flex-direction: column; gap: 14px">
              <div>
                <div
                  :style="{
                    fontSize: '10px',
                    color: UI.textMute,
                    fontWeight: 600,
                    letterSpacing: '1.2px',
                    textTransform: 'uppercase',
                    marginBottom: '6px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }"
                >
                  Categoria
                  <UIPill :color="catalogOriginPillColor(c.origin)" size="sm" data-testid="category-origin-pill">
                    {{ catalogOriginLabel(c.origin) }}
                  </UIPill>
                </div>
                <div
                  :style="{
                    fontSize: '18px',
                    fontWeight: 700,
                    color: UI.text,
                    letterSpacing: '-0.3px',
                  }"
                >
                  {{ c.name }}
                </div>
              </div>
              <div style="display: flex; gap: 6px">
                <UIBtn size="sm" icon="edit" variant="secondary" @click="openEdit(c)">
                  Editar
                </UIBtn>
                <UIBtn
                  size="sm"
                  icon="trash"
                  variant="softDanger"
                  @click="confirmDeleteId = c.id"
                >
                  Excluir
                </UIBtn>
              </div>
            </div>
          </div>
        </div>
      </div>

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
            padding: '12px 18px',
            borderBottom: `1px solid ${UI.border}`,
            fontSize: '13px',
            fontWeight: 600,
            color: UI.text,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexShrink: 0,
          }"
        >
          Todas as categorias
          <span :style="{ fontSize: '11.5px', color: UI.textSub, fontWeight: 500 }">
            {{ store.totalElements }} resultados
          </span>
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
            Nenhuma categoria encontrada.
          </div>
          <div
            v-for="(c, i) in store.items"
            v-else
            :key="c.id"
            class="ui-row"
            :style="{
              display: 'grid',
              gridTemplateColumns: '1.5fr 130px',
              gap: '12px',
              padding: '12px 18px',
              borderBottom: i === store.items.length - 1 ? 'none' : `1px solid ${UI.borderSub}`,
              fontSize: '13px',
              color: UI.text,
              alignItems: 'center',
            }"
          >
            <span style="display: flex; align-items: center; gap: 11px">
              <span
                :style="{
                  width: '4px',
                  height: '26px',
                  borderRadius: '2px',
                  background: colorFor(c.name),
                  flexShrink: 0,
                }"
              />
              <span :style="{ fontWeight: 600 }">{{ c.name }}</span>
              <UIPill :color="catalogOriginPillColor(c.origin)" size="sm">
                {{ catalogOriginLabel(c.origin) }}
              </UIPill>
            </span>
            <span style="display: flex; gap: 5px; justify-content: flex-end">
              <UIRowAction icon="edit" color="blue" label="Editar" @click="openEdit(c)" />
              <UIRowAction icon="trash" color="rose" label="Excluir" @click="confirmDeleteId = c.id" />
            </span>
          </div>
        </div>
        <div
          v-if="store.totalPages > 1"
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
          <span>Página {{ store.page + 1 }} de {{ store.totalPages }}</span>
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
      :title="editing ? 'Editar Categoria' : 'Nova Categoria'"
      :width="460"
      @close="closeModal"
    >
      <form id="cat-form" @submit.prevent="handleSubmit">
        <div style="display: flex; flex-direction: column; gap: 14px">
          <UIField label="Nome">
            <UIInput v-model="form.name" placeholder="Ex.: Combos, Açaí, Bebidas…" />
          </UIField>
          <div
            v-if="editing"
            :style="{
              padding: '12px',
              background: UI.amberBg,
              borderRadius: '9px',
              fontSize: '12px',
              color: UI.amber2,
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
            }"
          >
            <UIIcon name="info" :size="16" />
            Produtos vinculados continuarão associados após a renomeação.
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
          {{ editing ? 'Salvar' : 'Criar' }}
        </UIBtn>
      </template>
    </UIModal>

    <UIModal
      v-if="confirmDeleteId"
      title="Excluir categoria"
      subtitle="Esta ação não pode ser desfeita"
      :width="420"
      @close="confirmDeleteId = null"
    >
      <p :style="{ color: UI.textSub, fontSize: '13.5px', lineHeight: 1.6 }">
        Tem certeza que deseja excluir esta categoria?
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
.ui-card-hover {
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
.ui-card-hover:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  border-color: #d4d8e0;
}
</style>
