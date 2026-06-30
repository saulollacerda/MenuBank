<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { UI, UITopbar, UICard, UIBtn, UIField, UIInput, UIPill, UIIcon } from '@/design'
import type { DayOfWeek, OpeningHour } from '@/types/User'
import { ifoodAuthService } from '@/services/ifoodAuthService'

const authStore = useAuthStore()
const route = useRoute()

const apiKey = ref('')
const showKey = ref(false)
const successMessage = ref<string | null>(null)
const loadError = ref<string | null>(null)
const section = ref<'loja' | 'ints' | 'horario' | 'alerta' | 'time' | 'billing' | 'danger'>('loja')

const inputType = computed(() => (showKey.value ? 'text' : 'password'))

const user = computed(() => authStore.currentUser)
const initials = computed(() => {
  const name = authStore.restaurantName || 'MB'
  const parts = name.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || 'MB'
})

// ── Opening hours ────────────────────────────────────────────────────────────

const DAY_ORDER: DayOfWeek[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
]
const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Segunda', TUESDAY: 'Terça', WEDNESDAY: 'Quarta', THURSDAY: 'Quinta',
  FRIDAY: 'Sexta', SATURDAY: 'Sábado', SUNDAY: 'Domingo',
}

function defaultHours(): OpeningHour[] {
  return DAY_ORDER.map((d) => ({
    dayOfWeek: d,
    openTime: '11:00',
    closeTime: '22:00',
    closed: true,
  }))
}

const localHours = ref<OpeningHour[]>(defaultHours())
const hoursSaving = ref(false)
const hoursSuccess = ref<string | null>(null)

function initHours(saved: OpeningHour[] | null | undefined) {
  if (!saved || saved.length === 0) {
    localHours.value = defaultHours()
    return
  }
  // Fill missing days with defaults so the grid always shows all 7 days
  const byDay = Object.fromEntries(saved.map((h) => [h.dayOfWeek, h]))
  localHours.value = DAY_ORDER.map((d) => byDay[d] ?? {
    dayOfWeek: d, openTime: '11:00', closeTime: '22:00', closed: false,
  })
}

async function saveHours() {
  hoursSaving.value = true
  hoursSuccess.value = null
  try {
    await authStore.updateOpeningHours(localHours.value)
    hoursSuccess.value = 'Horários salvos com sucesso.'
    setTimeout(() => (hoursSuccess.value = null), 4000)
  } catch {
    /* error in store */
  } finally {
    hoursSaving.value = false
  }
}

// ── Profile load ─────────────────────────────────────────────────────────────

async function loadProfile() {
  loadError.value = null
  try {
    const u = await authStore.fetchCurrentUser()
    apiKey.value = u?.anotaAiApiKey ?? ''
    initHours(u?.openingHours)
  } catch {
    loadError.value = 'Não foi possível carregar suas configurações.'
  }
}

// ── iFood ─────────────────────────────────────────────────────────────────────

const ifoodConnected = ref(false)
const ifoodModal = ref(false)
const ifoodStep = ref<'code' | 'loading' | 'done'>('code')
const ifoodUserCode = ref('')
const ifoodVerificationUrl = ref('')
const ifoodExpiresIn = ref(0)
const ifoodCountdown = ref(0)
const ifoodAuthCode = ref('')
const ifoodError = ref<string | null>(null)
const ifoodConnecting = ref(false)

let ifoodTimer: ReturnType<typeof setInterval> | null = null

function clearIfoodTimer() {
  if (ifoodTimer) { clearInterval(ifoodTimer); ifoodTimer = null }
}

function formatCountdown(s: number): string {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

async function startIfoodAuth() {
  ifoodError.value = null
  ifoodAuthCode.value = ''
  ifoodStep.value = 'loading'
  ifoodModal.value = true
  clearIfoodTimer()
  try {
    const res = await ifoodAuthService.start()
    ifoodUserCode.value = res.userCode
    ifoodVerificationUrl.value = res.verificationUrl
    ifoodExpiresIn.value = res.expiresIn
    ifoodCountdown.value = res.expiresIn
    ifoodStep.value = 'code'
    ifoodTimer = setInterval(async () => {
      ifoodCountdown.value -= 1
      if (ifoodCountdown.value <= 0) {
        clearIfoodTimer()
        // Auto-renew: request new userCode
        try {
          const renewed = await ifoodAuthService.start()
          ifoodUserCode.value = renewed.userCode
          ifoodVerificationUrl.value = renewed.verificationUrl
          ifoodCountdown.value = renewed.expiresIn
          ifoodTimer = setInterval(() => {
            ifoodCountdown.value -= 1
            if (ifoodCountdown.value <= 0) clearIfoodTimer()
          }, 1000)
        } catch {
          ifoodError.value = 'Não foi possível renovar o código. Tente novamente.'
        }
      }
    }, 1000)
  } catch {
    ifoodError.value = 'Não foi possível obter o código de vínculo. Tente novamente.'
    ifoodStep.value = 'code'
  }
}

async function confirmIfoodConnect() {
  if (!ifoodAuthCode.value.trim()) return
  ifoodConnecting.value = true
  ifoodError.value = null
  try {
    await ifoodAuthService.connect(ifoodAuthCode.value.trim())
    ifoodConnected.value = true
    ifoodModal.value = false
    clearIfoodTimer()
  } catch {
    ifoodError.value = 'Código inválido ou expirado. Verifique e tente novamente.'
  } finally {
    ifoodConnecting.value = false
  }
}

function closeIfoodModal() {
  ifoodModal.value = false
  clearIfoodTimer()
}

async function revokeIfood() {
  try {
    await ifoodAuthService.revoke()
    ifoodConnected.value = false
  } catch {
    /* ignore */
  }
}

onUnmounted(clearIfoodTimer)

async function handleSaveKey() {
  successMessage.value = null
  try {
    const trimmed = apiKey.value.trim()
    await authStore.updateAnotaAIKey(trimmed.length > 0 ? trimmed : null)
    successMessage.value = 'Chave do Anota.AI salva com sucesso.'
    setTimeout(() => (successMessage.value = null), 4000)
  } catch {
    /* error in store */
  }
}

const SUBNAV: Array<{
  id: typeof section.value
  ic: string
  l: string
  danger?: boolean
}> = [
  { id: 'loja', ic: 'box', l: 'Perfil da loja' },
  { id: 'ints', ic: 'sync', l: 'Integrações' },
  { id: 'horario', ic: 'clock', l: 'Horários' },
  { id: 'alerta', ic: 'bell', l: 'Alertas' },
  { id: 'time', ic: 'user', l: 'Time' },
  { id: 'billing', ic: 'card', l: 'Plano e pagamento' },
  { id: 'danger', ic: 'alert', l: 'Zona perigosa', danger: true },
]

onMounted(async () => {
  await loadProfile()
  const q = route.query.section
  if (q && typeof q === 'string' && SUBNAV.some((s) => s.id === q)) {
    section.value = q as typeof section.value
  }
})
</script>

<template>
  <div style="display: flex; flex-direction: column; flex: 1">
    <UITopbar
      title="Configurações"
      subtitle="Perfil da loja, integrações e preferências"
    />

    <div style="flex: 1; padding: 28px; display: flex; gap: 20px; overflow: hidden; min-height: 0">
      <!-- Sub-nav -->
      <div style="width: 220px; display: flex; flex-direction: column; gap: 4px; flex-shrink: 0">
        <div
          v-for="it in SUBNAV"
          :key="it.id"
          class="settings-subnav-item"
          :style="{
            padding: '10px 14px',
            borderRadius: '9px',
            background: section === it.id ? UI.panel : 'transparent',
            border: section === it.id ? `1px solid ${UI.border}` : '1px solid transparent',
            color: it.danger ? UI.rose : section === it.id ? UI.text : UI.textSub,
            fontSize: '13px',
            fontWeight: section === it.id ? 600 : 500,
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            cursor: 'pointer',
          }"
          @click="section = it.id"
        >
          <UIIcon :name="it.ic" :size="15" />
          {{ it.l }}
        </div>
      </div>

      <!-- Right content -->
      <div
        style="
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 16px;
          overflow: auto;
          min-height: 0;
        "
      >
        <div
          v-if="loadError"
          :style="{
            padding: '10px 14px',
            background: UI.roseBg,
            color: UI.rose2,
            borderRadius: '10px',
            fontSize: '13px',
          }"
        >
          {{ loadError }}
        </div>
        <div
          v-if="authStore.error"
          :style="{
            padding: '10px 14px',
            background: UI.roseBg,
            color: UI.rose2,
            borderRadius: '10px',
            fontSize: '13px',
          }"
        >
          {{ authStore.error }}
        </div>
        <div
          v-if="successMessage"
          :style="{
            padding: '10px 14px',
            background: UI.emeraldBg,
            color: UI.emerald2,
            borderRadius: '10px',
            fontSize: '13px',
          }"
        >
          {{ successMessage }}
        </div>

        <!-- Perfil da loja -->
        <UICard v-if="section === 'loja'" :padding="22">
          <div
            style="
              display: flex;
              align-items: flex-start;
              justify-content: space-between;
              margin-bottom: 4px;
            "
          >
            <div>
              <div
                :style="{
                  fontSize: '16px',
                  fontWeight: 700,
                  color: UI.text,
                  letterSpacing: '-0.3px',
                }"
              >
                Perfil da loja
              </div>
              <div :style="{ fontSize: '12px', color: UI.textSub, marginTop: '4px' }">
                Informações principais do seu cadastro.
              </div>
            </div>
          </div>

          <div style="display: flex; gap: 22px; margin-top: 22px; align-items: flex-start">
            <div style="display: flex; flex-direction: column; align-items: center; gap: 10px">
              <div
                :style="{
                  width: '100px',
                  height: '100px',
                  borderRadius: '50px',
                  background: 'linear-gradient(135deg,#10b981,#059669)',
                  color: '#fff',
                  fontSize: '36px',
                  fontWeight: 800,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }"
              >
                {{ initials }}
              </div>
              <UIBtn variant="secondary" size="sm" icon="upload">Trocar logo</UIBtn>
            </div>

            <div style="flex: 1; display: grid; grid-template-columns: 1fr 1fr; gap: 14px">
              <UIField label="Nome da loja">
                <UIInput :model-value="user?.merchantName ?? ''" disabled />
              </UIField>
              <UIField label="CNPJ">
                <UIInput :model-value="user?.cnpj ?? ''" disabled />
              </UIField>
              <UIField label="Email">
                <UIInput :model-value="user?.email ?? ''" icon="mail" disabled />
              </UIField>
              <UIField label="Telefone">
                <UIInput :model-value="user?.phone ?? ''" disabled />
              </UIField>
            </div>
          </div>

          <div
            :style="{
              marginTop: '22px',
              padding: '12px 14px',
              background: UI.amberBg,
              color: UI.amber2,
              borderRadius: '9px',
              fontSize: '12.5px',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
            }"
          >
            <UIIcon name="info" :size="16" />
            A edição dos dados da loja ainda não está disponível no backend
            (apenas a chave Anota.AI é editável hoje).
          </div>
        </UICard>

        <!-- Integrações -->
        <UICard v-if="section === 'ints'" :padding="22">
          <div
            style="
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 14px;
            "
          >
            <div>
              <div
                :style="{
                  fontSize: '16px',
                  fontWeight: 700,
                  color: UI.text,
                  letterSpacing: '-0.3px',
                }"
              >
                Integrações
              </div>
              <div :style="{ fontSize: '12px', color: UI.textSub, marginTop: '4px' }">
                Conecte canais de venda externos.
              </div>
            </div>
          </div>

          <div style="display: flex; flex-direction: column; gap: 8px">
            <div
              :style="{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '14px',
                padding: '14px',
                background: UI.bgSoft,
                border: `1px solid ${UI.border}`,
                borderRadius: '11px',
                flexDirection: 'column',
              }"
            >
              <div style="display: flex; align-items: center; gap: 14px; width: 100%">
                <div
                  :style="{
                    width: '38px',
                    height: '38px',
                    borderRadius: '9px',
                    background: apiKey ? UI.emeraldBg : UI.bg,
                    color: apiKey ? UI.emerald2 : UI.textMute,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                  }"
                >
                  <UIIcon name="sync" :size="16" />
                </div>
                <div style="flex: 1">
                  <div :style="{ fontSize: '13.5px', fontWeight: 600 }">Anota.AI</div>
                  <div :style="{ fontSize: '11.5px', color: UI.textSub, marginTop: '2px' }">
                    Importação automática de pedidos · sincronização de cardápio.
                  </div>
                </div>
                <UIPill :color="apiKey ? 'emerald' : 'gray'" dot>
                  {{ apiKey ? 'Conectado' : 'Desconectado' }}
                </UIPill>
              </div>

              <form
                style="width: 100%; display: flex; flex-direction: column; gap: 10px; margin-top: 4px"
                @submit.prevent="handleSaveKey"
              >
                <UIField
                  label="Token de integração"
                  hint="Cole aqui o token de integração fornecido pelo Anota.AI."
                >
                  <UIInput v-model="apiKey" :type="inputType" placeholder="Cole o token do Anota.AI">
                    <template #rightAddon>
                      <span
                        :style="{
                          fontSize: '11.5px',
                          color: UI.blue,
                          fontWeight: 600,
                          cursor: 'pointer',
                        }"
                        @click="showKey = !showKey"
                      >
                        {{ showKey ? 'ocultar' : 'mostrar' }}
                      </span>
                    </template>
                  </UIInput>
                </UIField>
                <div style="display: flex; justify-content: flex-end">
                  <UIBtn
                    variant="primary"
                    icon="check"
                    type="submit"
                    :disabled="authStore.loading"
                  >
                    {{ authStore.loading ? 'Salvando…' : 'Salvar token' }}
                  </UIBtn>
                </div>
              </form>
            </div>

            <!-- iFood -->
            <div
              :style="{
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
                padding: '12px 14px',
                background: UI.bgSoft,
                border: `1px solid ${UI.border}`,
                borderRadius: '11px',
              }"
            >
              <div style="display: flex; align-items: center; gap: 14px">
                <div
                  :style="{
                    width: '38px',
                    height: '38px',
                    borderRadius: '9px',
                    background: ifoodConnected ? UI.emeraldBg : UI.bg,
                    color: ifoodConnected ? UI.emerald2 : UI.textMute,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                  }"
                >
                  <UIIcon name="sync" :size="16" />
                </div>
                <div style="flex: 1">
                  <div :style="{ fontSize: '13.5px', fontWeight: 600 }">iFood</div>
                  <div :style="{ fontSize: '11.5px', color: UI.textSub, marginTop: '2px' }">
                    Importação direta de pedidos via API oficial do iFood.
                  </div>
                </div>
                <UIPill :color="ifoodConnected ? 'emerald' : 'gray'" dot>
                  {{ ifoodConnected ? 'Conectado' : 'Desconectado' }}
                </UIPill>
              </div>
              <div style="display: flex; justify-content: flex-end; gap: 8px">
                <UIBtn v-if="ifoodConnected" variant="ghost" size="sm" @click="revokeIfood">
                  Desconectar
                </UIBtn>
                <UIBtn v-else variant="primary" size="sm" icon="link" @click="startIfoodAuth">
                  Conectar iFood
                </UIBtn>
              </div>
            </div>

            <!-- Em breve -->
            <div
              v-for="g in [
                { name: 'WhatsApp Business', desc: 'Em breve · notificações de status para o cliente.' },
                { name: 'Mercado Pago', desc: 'Em breve · conciliação de recebíveis.' },
              ]"
              :key="g.name"
              :style="{
                display: 'flex',
                alignItems: 'center',
                gap: '14px',
                padding: '12px 14px',
                background: UI.bgSoft,
                border: `1px solid ${UI.border}`,
                borderRadius: '11px',
              }"
            >
              <div
                :style="{
                  width: '38px',
                  height: '38px',
                  borderRadius: '9px',
                  background: UI.bg,
                  color: UI.textMute,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }"
              >
                <UIIcon name="sync" :size="16" />
              </div>
              <div style="flex: 1">
                <div :style="{ fontSize: '13.5px', fontWeight: 600 }">{{ g.name }}</div>
                <div :style="{ fontSize: '11.5px', color: UI.textSub, marginTop: '2px' }">
                  {{ g.desc }}
                </div>
              </div>
              <UIPill color="gray" dot>Em breve</UIPill>
            </div>

            <!-- iFood Modal -->
            <Teleport to="body">
              <div
                v-if="ifoodModal"
                style="
                  position: fixed; inset: 0; z-index: 1000;
                  background: rgba(0,0,0,0.45);
                  display: flex; align-items: center; justify-content: center;
                  padding: 16px;
                "
                @click.self="closeIfoodModal"
              >
                <div
                  :style="{
                    background: UI.bg,
                    border: `1px solid ${UI.border}`,
                    borderRadius: '16px',
                    padding: '24px',
                    width: '100%',
                    maxWidth: '460px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '20px',
                  }"
                >
                  <div style="display: flex; align-items: center; justify-content: space-between">
                    <div :style="{ fontSize: '16px', fontWeight: 700, color: UI.text }">
                      Conectar iFood
                    </div>
                    <span
                      :style="{ color: UI.textMute, cursor: 'pointer', fontSize: '20px', lineHeight: 1 }"
                      @click="closeIfoodModal"
                    >×</span>
                  </div>

                  <div v-if="ifoodStep === 'loading'" :style="{ color: UI.textSub, fontSize: '13.5px', textAlign: 'center' }">
                    Gerando código de vínculo…
                  </div>

                  <template v-else>
                    <!-- userCode display -->
                    <div style="display: flex; flex-direction: column; align-items: center; gap: 8px">
                      <div :style="{ fontSize: '12px', color: UI.textMute, textTransform: 'uppercase', letterSpacing: '0.05em' }">
                        Seu código de vínculo
                      </div>
                      <div
                        :style="{
                          fontSize: '28px',
                          fontWeight: 700,
                          letterSpacing: '0.15em',
                          color: UI.text,
                          fontFamily: 'monospace',
                          background: UI.bgSoft,
                          border: `1px solid ${UI.border}`,
                          borderRadius: '10px',
                          padding: '10px 24px',
                        }"
                      >
                        {{ ifoodUserCode }}
                      </div>
                      <div :style="{ fontSize: '12px', color: ifoodCountdown <= 60 ? '#ef4444' : UI.textSub }">
                        ⏱ Expira em: {{ formatCountdown(ifoodCountdown) }}
                      </div>
                    </div>

                    <!-- Steps -->
                    <div style="display: flex; flex-direction: column; gap: 6px">
                      <div :style="{ fontSize: '12.5px', fontWeight: 600, color: UI.textSub, marginBottom: '2px' }">
                        Siga os passos no Portal do Parceiro:
                      </div>
                      <ol :style="{ margin: 0, paddingLeft: '18px', display: 'flex', flexDirection: 'column', gap: '4px' }">
                        <li :style="{ fontSize: '13px', color: UI.text }">
                          Acesse o
                          <a
                            :href="ifoodVerificationUrl || 'https://portal.ifood.com.br/setup/onboarding'"
                            target="_blank"
                            :style="{ color: UI.blue, fontWeight: 600 }"
                          >Portal do Parceiro iFood</a>
                        </li>
                        <li :style="{ fontSize: '13px', color: UI.text }">Faça login na sua conta iFood</li>
                        <li :style="{ fontSize: '13px', color: UI.text }">Clique em <strong>Integrações</strong> na barra lateral</li>
                        <li :style="{ fontSize: '13px', color: UI.text }">Clique em <strong>Ativar aplicativo por código</strong></li>
                        <li :style="{ fontSize: '13px', color: UI.text }">Insira o código acima e confirme</li>
                        <li :style="{ fontSize: '13px', color: UI.text }">O portal fornecerá um <strong>código de autorização</strong></li>
                      </ol>
                    </div>

                    <!-- authorizationCode input -->
                    <UIField label="Código de autorização" hint="Cole aqui o código que o portal do iFood forneceu.">
                      <UIInput
                        v-model="ifoodAuthCode"
                        placeholder="Cole o código de autorização"
                        @keyup.enter="confirmIfoodConnect"
                      />
                    </UIField>

                    <div v-if="ifoodError" :style="{ fontSize: '12.5px', color: '#ef4444' }">
                      {{ ifoodError }}
                    </div>

                    <div style="display: flex; justify-content: flex-end; gap: 8px">
                      <UIBtn variant="ghost" @click="closeIfoodModal">Cancelar</UIBtn>
                      <UIBtn
                        variant="primary"
                        :disabled="!ifoodAuthCode.trim() || ifoodConnecting"
                        @click="confirmIfoodConnect"
                      >
                        {{ ifoodConnecting ? 'Conectando…' : 'Confirmar' }}
                      </UIBtn>
                    </div>
                  </template>
                </div>
              </div>
            </Teleport>
          </div>
        </UICard>

        <!-- Horários de funcionamento -->
        <UICard v-if="section === 'horario'" :padding="22">
          <div
            style="
              display: flex;
              align-items: flex-start;
              justify-content: space-between;
              margin-bottom: 20px;
            "
          >
            <div>
              <div
                :style="{ fontSize: '16px', fontWeight: 700, color: UI.text, letterSpacing: '-0.3px' }"
              >
                Horários de funcionamento
              </div>
              <div :style="{ fontSize: '12px', color: UI.textSub, marginTop: '4px' }">
                Durante esses horários os pedidos do Anota.AI são importados automaticamente.
              </div>
            </div>
          </div>

          <div style="display: flex; flex-direction: column; gap: 8px">
            <div
              v-for="hour in localHours"
              :key="hour.dayOfWeek"
              :style="{
                display: 'grid',
                gridTemplateColumns: '96px 1fr 1fr auto',
                alignItems: 'center',
                gap: '12px',
                padding: '10px 14px',
                background: UI.bgSoft,
                border: `1px solid ${UI.border}`,
                borderRadius: '10px',
                opacity: hour.closed ? '0.55' : '1',
              }"
            >
              <div :style="{ fontSize: '13px', fontWeight: 600, color: UI.text }">
                {{ DAY_LABELS[hour.dayOfWeek] }}
              </div>

              <UIField label="Abertura" style="margin: 0">
                <UIInput
                  type="time"
                  :model-value="hour.openTime ?? ''"
                  :disabled="hour.closed"
                  @update:model-value="hour.openTime = $event || null"
                />
              </UIField>

              <UIField label="Fechamento" style="margin: 0">
                <UIInput
                  type="time"
                  :model-value="hour.closeTime ?? ''"
                  :disabled="hour.closed"
                  @update:model-value="hour.closeTime = $event || null"
                />
              </UIField>

              <label
                :style="{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontSize: '12.5px',
                  color: UI.textSub,
                  cursor: 'pointer',
                  userSelect: 'none',
                  whiteSpace: 'nowrap',
                }"
              >
                <input
                  type="checkbox"
                  :checked="hour.closed"
                  style="accent-color: var(--color-rose, #ef4444); width: 14px; height: 14px"
                  @change="hour.closed = ($event.target as HTMLInputElement).checked"
                />
                Fechado
              </label>
            </div>
          </div>

          <div
            v-if="hoursSuccess"
            :style="{
              marginTop: '14px',
              padding: '10px 14px',
              background: UI.emeraldBg,
              color: UI.emerald2,
              borderRadius: '9px',
              fontSize: '13px',
            }"
          >
            {{ hoursSuccess }}
          </div>
          <div v-if="authStore.error" :style="{
            marginTop: '14px',
            padding: '10px 14px',
            background: UI.roseBg,
            color: UI.rose2,
            borderRadius: '9px',
            fontSize: '13px',
          }">
            {{ authStore.error }}
          </div>

          <div style="margin-top: 18px; display: flex; justify-content: flex-end">
            <UIBtn :loading="hoursSaving" @click="saveHours">Salvar horários</UIBtn>
          </div>
        </UICard>

        <!-- Placeholder sections -->
        <UICard
          v-if="['alerta', 'time', 'billing'].includes(section)"
          :padding="22"
        >
          <div
            :style="{
              fontSize: '16px',
              fontWeight: 700,
              color: UI.text,
              letterSpacing: '-0.3px',
              marginBottom: '6px',
            }"
          >
            {{ SUBNAV.find((s) => s.id === section)?.l }}
          </div>
          <div :style="{ fontSize: '12.5px', color: UI.textSub, marginBottom: '18px' }">
            Esta seção ainda não está disponível.
          </div>
          <div
            :style="{
              padding: '14px',
              background: UI.amberBg,
              color: UI.amber2,
              borderRadius: '10px',
              fontSize: '13px',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
            }"
          >
            <UIIcon name="info" :size="16" />
            O backend ainda não expõe os campos necessários para esta tela. Consulte
            <code style="font-family: ui-monospace, monospace">docs/BACKEND_GAPS.md</code>.
          </div>
        </UICard>

        <UICard v-if="section === 'danger'" :padding="22">
          <div
            :style="{
              fontSize: '16px',
              fontWeight: 700,
              color: UI.rose,
              letterSpacing: '-0.3px',
              marginBottom: '6px',
            }"
          >
            Zona perigosa
          </div>
          <div :style="{ fontSize: '12.5px', color: UI.textSub, marginBottom: '18px' }">
            Ações destrutivas. Só faça aqui o que você tem certeza.
          </div>
          <div
            :style="{
              padding: '14px',
              background: UI.roseBg,
              color: UI.rose2,
              borderRadius: '10px',
              fontSize: '13px',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
            }"
          >
            <UIIcon name="alert" :size="16" />
            Funcionalidades como excluir conta / cancelar plano ainda não existem no backend.
          </div>
        </UICard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-subnav-item {
  transition: opacity 0.12s ease, background 0.12s ease;
}
.settings-subnav-item:hover {
  opacity: 0.85;
}
</style>
