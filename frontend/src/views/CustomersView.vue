<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCustomerStore } from '@/stores/customerStore'
import type { CustomerRequest, CustomerResponse } from '@/types/Customer'

const store = useCustomerStore()

const showModal = ref(false)
const editing = ref<CustomerResponse | null>(null)
const form = ref<CustomerRequest>({ name: '', phone: '', email: '' })
const confirmDeleteId = ref<string | null>(null)

function openCreateModal() {
  editing.value = null
  form.value = { name: '', phone: '', email: '' }
  showModal.value = true
}

function openEditModal(customer: CustomerResponse) {
  editing.value = customer
  form.value = {
    name: customer.name,
    phone: customer.phone ?? '',
    email: customer.email ?? '',
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
      await store.update(editing.value.id, form.value)
    } else {
      await store.create(form.value)
    }
    closeModal()
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
    await store.remove(confirmDeleteId.value)
  } catch {
    // Error is handled by the store
  }
  confirmDeleteId.value = null
}

onMounted(() => {
  store.fetchAll()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Clientes</h1>
      <button class="btn btn-primary" @click="openCreateModal">+ Novo Cliente</button>
    </div>

    <div v-if="store.error" class="alert alert-error">{{ store.error }}</div>

    <div v-if="store.loading" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="store.items.length === 0" class="empty-state">
      <p>Nenhum cliente cadastrado.</p>
      <button class="btn btn-primary" @click="openCreateModal">Cadastrar primeiro cliente</button>
    </div>

    <div v-else class="table-container">
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Telefone</th>
            <th>Email</th>
            <th style="width: 150px">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="customer in store.items" :key="customer.id">
            <td>{{ customer.name }}</td>
            <td>{{ customer.phone || '—' }}</td>
            <td>{{ customer.email || '—' }}</td>
            <td>
              <div class="table-actions">
                <button class="btn btn-secondary btn-sm" @click="openEditModal(customer)">
                  Editar
                </button>
                <button class="btn btn-danger btn-sm" @click="confirmDelete(customer.id)">
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
          <h2>{{ editing ? 'Editar Cliente' : 'Novo Cliente' }}</h2>
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
                placeholder="Nome do cliente"
                required
              />
            </div>
            <div class="form-group">
              <label>Telefone</label>
              <input
                v-model="form.phone"
                type="tel"
                class="form-control"
                placeholder="(00) 00000-0000"
              />
            </div>
            <div class="form-group">
              <label>Email</label>
              <input
                v-model="form.email"
                type="email"
                class="form-control"
                placeholder="email@exemplo.com"
              />
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
          <p>Tem certeza que deseja excluir este cliente?</p>
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

