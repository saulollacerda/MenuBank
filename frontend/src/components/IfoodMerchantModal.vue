<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { UI, UIBtn, UIModal, UIPill } from '@/design'
import {
  ifoodMerchantService,
  merchantErrorMessage,
  toIsoWithOffset,
  type DayOfWeek,
  type MerchantDetails,
  type MerchantInterruption,
  type MerchantState,
  type MerchantStatus,
  type ShiftInput,
} from '@/services/ifoodMerchantService'

const emit = defineEmits<{ (e: 'close'): void }>()

type Tab = 'status' | 'pauses' | 'hours'
type PillColor = 'gray' | 'emerald' | 'blue' | 'amber' | 'rose' | 'violet' | 'dark'

const activeTab = ref<Tab>('status')

// State/day translations — code stays English, UI copy is pt-BR.
const STATE_LABELS: Record<MerchantState, string> = {
  OK: 'Aberta',
  WARNING: 'Atenção',
  CLOSED: 'Fechada',
  ERROR: 'Erro',
}
const STATE_COLORS: Record<MerchantState, PillColor> = {
  OK: 'emerald',
  WARNING: 'amber',
  CLOSED: 'gray',
  ERROR: 'rose',
}
const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Segunda-feira',
  TUESDAY: 'Terça-feira',
  WEDNESDAY: 'Quarta-feira',
  THURSDAY: 'Quinta-feira',
  FRIDAY: 'Sexta-feira',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
}
const DAY_ORDER: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]

const tabs: { key: Tab; label: string }[] = [
  { key: 'status', label: 'Status' },
  { key: 'pauses', label: 'Pausas' },
  { key: 'hours', label: 'Horários' },
]

// ---- Status section ----
const details = ref<MerchantDetails | null>(null)
const statusList = ref<MerchantStatus[]>([])
const statusLoading = ref(false)
const statusError = ref<string | null>(null)

async function loadStatus() {
  statusLoading.value = true
  statusError.value = null
  try {
    const [d, s] = await Promise.all([
      ifoodMerchantService.getDetails(),
      ifoodMerchantService.getStatus(),
    ])
    details.value = d
    statusList.value = s
  } catch (err) {
    statusError.value = merchantErrorMessage(
      err,
      'Não foi possível carregar o status da loja. Tente novamente em instantes.',
    )
  } finally {
    statusLoading.value = false
  }
}

// ---- Pauses section ----
const interruptions = ref<MerchantInterruption[]>([])
const pausesLoading = ref(false)
const pausesLoaded = ref(false)
const pausesError = ref<string | null>(null)
const form = ref<{ description: string; start: string; end: string }>({
  description: '',
  start: '',
  end: '',
})
const creating = ref(false)
const createError = ref<string | null>(null)
const confirmingDeleteId = ref<string | null>(null)
const deletingId = ref<string | null>(null)

const canSubmitPause = computed(
  () =>
    form.value.description.trim().length > 0 &&
    form.value.start.length > 0 &&
    form.value.end.length > 0,
)

async function loadPauses() {
  pausesLoading.value = true
  pausesError.value = null
  try {
    interruptions.value = await ifoodMerchantService.getInterruptions()
    pausesLoaded.value = true
  } catch (err) {
    pausesError.value = merchantErrorMessage(
      err,
      'Não foi possível carregar as pausas. Tente novamente em instantes.',
    )
  } finally {
    pausesLoading.value = false
  }
}

async function submitPause() {
  if (!canSubmitPause.value || creating.value) return
  creating.value = true
  createError.value = null
  try {
    await ifoodMerchantService.createInterruption({
      description: form.value.description.trim(),
      // datetime-local values are naive local times; iFood needs offset-aware ISO-8601.
      start: toIsoWithOffset(form.value.start),
      end: toIsoWithOffset(form.value.end),
    })
    form.value = { description: '', start: '', end: '' }
    await loadPauses()
  } catch (err) {
    createError.value = merchantErrorMessage(
      err,
      'Não foi possível criar a pausa. Tente novamente em instantes.',
    )
  } finally {
    creating.value = false
  }
}

function askDelete(id: string) {
  confirmingDeleteId.value = id
}

function cancelDelete() {
  confirmingDeleteId.value = null
}

async function confirmDelete(id: string) {
  deletingId.value = id
  pausesError.value = null
  try {
    await ifoodMerchantService.deleteInterruption(id)
    confirmingDeleteId.value = null
    await loadPauses()
  } catch (err) {
    pausesError.value = merchantErrorMessage(
      err,
      'Não foi possível remover a pausa. Tente novamente em instantes.',
    )
  } finally {
    deletingId.value = null
  }
}

function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ---- Opening hours section ----
interface EditableShift {
  dayOfWeek: DayOfWeek
  /** "HH:mm" for the time input. */
  start: string
  /** Duration in minutes. */
  duration: number
}

const editableShifts = ref<EditableShift[]>([])
const hoursLoading = ref(false)
const hoursLoaded = ref(false)
const hoursError = ref<string | null>(null)
const hoursSuccess = ref(false)
const savingHours = ref(false)

async function loadHours() {
  hoursLoading.value = true
  hoursError.value = null
  hoursSuccess.value = false
  try {
    const { shifts } = await ifoodMerchantService.getOpeningHours()
    editableShifts.value = shifts.map((s) => ({
      dayOfWeek: s.dayOfWeek,
      start: s.start.slice(0, 5),
      duration: s.duration,
    }))
    hoursLoaded.value = true
  } catch (err) {
    hoursError.value = merchantErrorMessage(
      err,
      'Não foi possível carregar os horários. Tente novamente em instantes.',
    )
  } finally {
    hoursLoading.value = false
  }
}

function shiftsForDay(day: DayOfWeek): EditableShift[] {
  return editableShifts.value.filter((s) => s.dayOfWeek === day)
}

function addShift(day: DayOfWeek) {
  editableShifts.value.push({ dayOfWeek: day, start: '08:00', duration: 600 })
  hoursSuccess.value = false
}

function removeShift(shift: EditableShift) {
  const index = editableShifts.value.indexOf(shift)
  if (index >= 0) editableShifts.value.splice(index, 1)
  hoursSuccess.value = false
}

function updateStart(shift: EditableShift, value: string) {
  shift.start = value
  hoursSuccess.value = false
}

function updateDuration(shift: EditableShift, value: string) {
  const parsed = Number.parseInt(value, 10)
  shift.duration = Number.isNaN(parsed) ? 0 : parsed
  hoursSuccess.value = false
}

/** Computes the shift end time (HH:mm) from a start "HH:mm" and a duration in minutes. */
function shiftEnd(shift: EditableShift): string {
  const parts = shift.start.split(':')
  const h = Number.parseInt(parts[0] ?? '', 10)
  const m = Number.parseInt(parts[1] ?? '', 10)
  if (Number.isNaN(h) || Number.isNaN(m)) return '--:--'
  const total = ((h * 60 + m + shift.duration) % (24 * 60) + 24 * 60) % (24 * 60)
  const eh = Math.floor(total / 60)
  const em = total % 60
  return `${String(eh).padStart(2, '0')}:${String(em).padStart(2, '0')}`
}

function normalizeStart(value: string): string {
  return value.length === 5 ? `${value}:00` : value
}

async function saveHours() {
  if (savingHours.value) return
  savingHours.value = true
  hoursError.value = null
  hoursSuccess.value = false
  try {
    const payload: ShiftInput[] = editableShifts.value.map((s) => ({
      dayOfWeek: s.dayOfWeek,
      start: normalizeStart(s.start),
      duration: s.duration,
    }))
    const { shifts } = await ifoodMerchantService.updateOpeningHours(payload)
    editableShifts.value = shifts.map((s) => ({
      dayOfWeek: s.dayOfWeek,
      start: s.start.slice(0, 5),
      duration: s.duration,
    }))
    hoursSuccess.value = true
  } catch (err) {
    hoursError.value = merchantErrorMessage(
      err,
      'Não foi possível salvar os horários. Tente novamente em instantes.',
    )
  } finally {
    savingHours.value = false
  }
}

function selectTab(tab: Tab) {
  activeTab.value = tab
  if (tab === 'pauses' && !pausesLoaded.value) loadPauses()
  if (tab === 'hours' && !hoursLoaded.value) loadHours()
}

onMounted(loadStatus)
</script>

<template>
  <UIModal
    title="Minha loja no iFood"
    subtitle="Acompanhe o status, gerencie pausas e ajuste os horários de funcionamento."
    :width="640"
    title-test-id="ifood-merchant-modal-title"
    @close="emit('close')"
  >
    <!-- Tabs -->
    <div
      :style="{
        display: 'flex',
        gap: '6px',
        marginBottom: '16px',
        borderBottom: `1px solid ${UI.border}`,
      }"
    >
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :data-testid="`tab-${tab.key}`"
        type="button"
        :style="{
          padding: '8px 12px',
          fontSize: '12.5px',
          fontWeight: 600,
          background: 'transparent',
          border: 'none',
          borderBottom: `2px solid ${activeTab === tab.key ? UI.blue : 'transparent'}`,
          color: activeTab === tab.key ? UI.blue : UI.textSub,
          cursor: 'pointer',
          fontFamily: 'inherit',
        }"
        @click="selectTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- ===== Status ===== -->
    <section v-show="activeTab === 'status'">
      <div
        :style="{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: '12px',
          marginBottom: '14px',
        }"
      >
        <div>
          <div data-testid="merchant-store-name" :style="{ fontSize: '14px', fontWeight: 700, color: UI.text }">
            {{ details?.name ?? 'Loja iFood' }}
          </div>
          <div v-if="details?.corporateName" :style="{ fontSize: '12px', color: UI.textSub, marginTop: '2px' }">
            {{ details.corporateName }}
          </div>
        </div>
        <UIBtn
          variant="secondary"
          size="sm"
          icon="sync"
          data-testid="merchant-status-refresh"
          :disabled="statusLoading"
          @click="loadStatus"
        >
          Atualizar
        </UIBtn>
      </div>

      <div v-if="statusLoading" data-testid="merchant-status-loading" :style="{ fontSize: '12.5px', color: UI.textSub }">
        Carregando status…
      </div>
      <div
        v-else-if="statusError"
        data-testid="merchant-status-error"
        :style="{ fontSize: '12.5px', color: UI.rose, padding: '10px 12px', background: UI.roseBg, borderRadius: '9px' }"
      >
        {{ statusError }}
      </div>
      <div v-else data-testid="merchant-status-list" :style="{ display: 'flex', flexDirection: 'column', gap: '10px' }">
        <div v-if="statusList.length === 0" :style="{ fontSize: '12.5px', color: UI.textSub }">
          Nenhuma operação retornada para esta loja.
        </div>
        <div
          v-for="(op, index) in statusList"
          :key="`${op.operation}-${op.salesChannel}-${index}`"
          data-testid="merchant-status-row"
          :style="{ padding: '12px', background: UI.bg, borderRadius: '10px' }"
        >
          <div :style="{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }">
            <span :style="{ fontSize: '12.5px', fontWeight: 700, color: UI.text }">{{ op.operation }}</span>
            <UIPill :color="STATE_COLORS[op.state]" size="sm" dot>{{ STATE_LABELS[op.state] }}</UIPill>
            <UIPill :color="op.available ? 'emerald' : 'gray'" size="sm">
              {{ op.available ? 'Disponível' : 'Indisponível' }}
            </UIPill>
          </div>
          <div v-if="op.message" :style="{ marginTop: '8px' }">
            <div :style="{ fontSize: '12.5px', fontWeight: 600, color: UI.text }">{{ op.message.title }}</div>
            <div :style="{ fontSize: '12px', color: UI.textSub }">{{ op.message.subtitle }}</div>
          </div>
          <div
            v-for="validation in (op.validations ?? []).filter((v) => v.message)"
            :key="validation.id"
            :style="{ marginTop: '8px', paddingLeft: '10px', borderLeft: `2px solid ${UI.border}` }"
          >
            <div :style="{ fontSize: '12px', fontWeight: 600, color: UI.text }">{{ validation.message?.title }}</div>
            <div :style="{ fontSize: '11.5px', color: UI.textSub }">{{ validation.message?.subtitle }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== Pausas ===== -->
    <section v-show="activeTab === 'pauses'">
      <div v-if="pausesLoading" data-testid="pauses-loading" :style="{ fontSize: '12.5px', color: UI.textSub }">
        Carregando pausas…
      </div>
      <template v-else>
        <div
          v-if="pausesError"
          :style="{ fontSize: '12.5px', color: UI.rose, padding: '10px 12px', background: UI.roseBg, borderRadius: '9px', marginBottom: '12px' }"
        >
          {{ pausesError }}
        </div>

        <div :style="{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }">
          <div v-if="interruptions.length === 0" :style="{ fontSize: '12.5px', color: UI.textSub }">
            Nenhuma pausa ativa no momento.
          </div>
          <div
            v-for="pause in interruptions"
            :key="pause.id"
            data-testid="interruption-row"
            :style="{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '10px 12px',
              background: UI.bg,
              borderRadius: '9px',
            }"
          >
            <div style="flex: 1">
              <div :style="{ fontSize: '12.5px', fontWeight: 600, color: UI.text }">{{ pause.description }}</div>
              <div :style="{ fontSize: '11.5px', color: UI.textSub }">
                {{ formatDateTime(pause.start) }} — {{ formatDateTime(pause.end) }}
              </div>
            </div>
            <template v-if="confirmingDeleteId === pause.id">
              <span :style="{ fontSize: '11.5px', color: UI.textSub }">Remover?</span>
              <UIBtn
                variant="danger"
                size="sm"
                data-testid="interruption-confirm-delete"
                :disabled="deletingId === pause.id"
                @click="confirmDelete(pause.id)"
              >
                Confirmar
              </UIBtn>
              <UIBtn variant="ghost" size="sm" @click="cancelDelete">Cancelar</UIBtn>
            </template>
            <UIBtn
              v-else
              variant="softDanger"
              size="sm"
              icon="trash"
              data-testid="interruption-delete"
              @click="askDelete(pause.id)"
            >
              Remover
            </UIBtn>
          </div>
        </div>

        <div :style="{ padding: '14px', background: UI.bgSoft, border: `1px solid ${UI.border}`, borderRadius: '11px' }">
          <div :style="{ fontSize: '12.5px', fontWeight: 700, color: UI.text, marginBottom: '10px' }">
            Nova pausa
          </div>
          <div :style="{ display: 'flex', flexDirection: 'column', gap: '10px' }">
            <label :style="{ display: 'block' }">
              <span :style="{ fontSize: '12px', color: UI.text, fontWeight: 600 }">Descrição</span>
              <input
                v-model="form.description"
                data-testid="interruption-description"
                type="text"
                placeholder="Ex.: Manutenção da cozinha"
                :style="{
                  display: 'block',
                  width: '100%',
                  marginTop: '6px',
                  padding: '9px 12px',
                  border: `1px solid ${UI.border}`,
                  borderRadius: '9px',
                  fontSize: '13px',
                  fontFamily: 'inherit',
                  color: UI.text,
                }"
              />
            </label>
            <div :style="{ display: 'flex', gap: '10px' }">
              <label :style="{ display: 'block', flex: 1 }">
                <span :style="{ fontSize: '12px', color: UI.text, fontWeight: 600 }">Início</span>
                <input
                  v-model="form.start"
                  data-testid="interruption-start"
                  type="datetime-local"
                  :style="{
                    display: 'block',
                    width: '100%',
                    marginTop: '6px',
                    padding: '9px 12px',
                    border: `1px solid ${UI.border}`,
                    borderRadius: '9px',
                    fontSize: '13px',
                    fontFamily: 'inherit',
                    color: UI.text,
                  }"
                />
              </label>
              <label :style="{ display: 'block', flex: 1 }">
                <span :style="{ fontSize: '12px', color: UI.text, fontWeight: 600 }">Fim</span>
                <input
                  v-model="form.end"
                  data-testid="interruption-end"
                  type="datetime-local"
                  :style="{
                    display: 'block',
                    width: '100%',
                    marginTop: '6px',
                    padding: '9px 12px',
                    border: `1px solid ${UI.border}`,
                    borderRadius: '9px',
                    fontSize: '13px',
                    fontFamily: 'inherit',
                    color: UI.text,
                  }"
                />
              </label>
            </div>
            <div
              v-if="createError"
              data-testid="interruption-error"
              :style="{ fontSize: '12px', color: UI.rose }"
            >
              {{ createError }}
            </div>
            <div :style="{ display: 'flex', justifyContent: 'flex-end' }">
              <UIBtn
                variant="primary"
                size="sm"
                data-testid="interruption-submit"
                :disabled="!canSubmitPause || creating"
                @click="submitPause"
              >
                {{ creating ? 'Criando…' : 'Criar pausa' }}
              </UIBtn>
            </div>
          </div>
        </div>
      </template>
    </section>

    <!-- ===== Horários ===== -->
    <section v-show="activeTab === 'hours'">
      <div v-if="hoursLoading" data-testid="hours-loading" :style="{ fontSize: '12.5px', color: UI.textSub }">
        Carregando horários…
      </div>
      <template v-else>
        <div
          v-if="hoursError"
          data-testid="hours-error"
          :style="{ fontSize: '12.5px', color: UI.rose, padding: '10px 12px', background: UI.roseBg, borderRadius: '9px', marginBottom: '12px' }"
        >
          {{ hoursError }}
        </div>
        <div
          v-if="hoursSuccess"
          data-testid="hours-success"
          :style="{ fontSize: '12.5px', color: UI.emerald2, padding: '10px 12px', background: UI.emeraldBg, borderRadius: '9px', marginBottom: '12px' }"
        >
          Horários salvos com sucesso.
        </div>

        <div data-testid="hours-list" :style="{ display: 'flex', flexDirection: 'column', gap: '10px' }">
          <div
            v-for="day in DAY_ORDER"
            :key="day"
            :data-testid="`hours-day-${day}`"
            :style="{ padding: '12px', background: UI.bg, borderRadius: '10px' }"
          >
            <div :style="{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }">
              <span :style="{ fontSize: '12.5px', fontWeight: 700, color: UI.text }">{{ DAY_LABELS[day] }}</span>
              <UIBtn
                variant="ghost"
                size="sm"
                icon="plus"
                :data-testid="`shift-add-${day}`"
                @click="addShift(day)"
              >
                Adicionar turno
              </UIBtn>
            </div>
            <div v-if="shiftsForDay(day).length === 0" :style="{ fontSize: '11.5px', color: UI.textMute }">
              Fechado
            </div>
            <div
              v-for="shift in shiftsForDay(day)"
              :key="`${shift.dayOfWeek}-${editableShifts.indexOf(shift)}`"
              data-testid="shift-row"
              :style="{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }"
            >
              <input
                data-testid="shift-start"
                type="time"
                :value="shift.start"
                :style="{
                  padding: '7px 10px',
                  border: `1px solid ${UI.border}`,
                  borderRadius: '9px',
                  fontSize: '13px',
                  fontFamily: 'inherit',
                  color: UI.text,
                }"
                @input="updateStart(shift, ($event.target as HTMLInputElement).value)"
              />
              <span :style="{ fontSize: '12px', color: UI.textSub }">por</span>
              <input
                data-testid="shift-duration"
                type="number"
                min="0"
                step="15"
                :value="shift.duration"
                :style="{
                  width: '84px',
                  padding: '7px 10px',
                  border: `1px solid ${UI.border}`,
                  borderRadius: '9px',
                  fontSize: '13px',
                  fontFamily: 'inherit',
                  color: UI.text,
                }"
                @input="updateDuration(shift, ($event.target as HTMLInputElement).value)"
              />
              <span :style="{ fontSize: '12px', color: UI.textSub }">min</span>
              <span data-testid="shift-end" :style="{ fontSize: '12px', color: UI.textSub }">
                até {{ shiftEnd(shift) }}
              </span>
              <UIBtn
                variant="softDanger"
                size="sm"
                icon="trash"
                data-testid="shift-remove"
                @click="removeShift(shift)"
              >
                Remover
              </UIBtn>
            </div>
          </div>
        </div>

        <div :style="{ display: 'flex', justifyContent: 'flex-end', marginTop: '14px' }">
          <UIBtn
            variant="primary"
            size="sm"
            data-testid="hours-save"
            :disabled="savingHours"
            @click="saveHours"
          >
            {{ savingHours ? 'Salvando…' : 'Salvar horários' }}
          </UIBtn>
        </div>
      </template>
    </section>

    <template #footer>
      <UIBtn variant="ghost" data-testid="ifood-merchant-close" @click="emit('close')">
        Fechar
      </UIBtn>
    </template>
  </UIModal>
</template>
