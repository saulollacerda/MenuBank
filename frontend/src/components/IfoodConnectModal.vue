<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { UI, UIBtn, UIIcon, UIModal, UIPill } from '@/design'
import { useIfoodConnectFlow } from '@/composables/useIfoodConnectFlow'

const props = withDefaults(defineProps<{ resume?: boolean }>(), { resume: false })
const emit = defineEmits<{ (e: 'connected'): void; (e: 'close'): void }>()

const flow = useIfoodConnectFlow()

const confirmingCancel = ref(false)
const copied = ref(false)
const copyHint = ref<string | null>(null)
const guidedStep = ref<1 | 2 | 3>(1)
const authInput = ref<HTMLInputElement | null>(null)

const GUIDED_STEPS = [
  { n: 1 as const, label: 'Copie o código' },
  { n: 2 as const, label: 'Autorize no portal do iFood' },
  { n: 3 as const, label: 'Cole o código de autorização' },
]

const portalHref = computed(
  () =>
    flow.verificationUrlComplete.value ||
    flow.verificationUrl.value ||
    'https://portal.ifood.com.br/apps/code',
)

const countdownLabel = computed(() => {
  const s = flow.remainingSeconds.value
  const m = Math.floor(s / 60)
  return `${String(m).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})

const urgent = computed(() => flow.remainingSeconds.value <= 60)

function chipState(n: 1 | 2 | 3): 'done' | 'active' | 'todo' {
  if (n < guidedStep.value) return 'done'
  if (n === guidedStep.value) return 'active'
  return 'todo'
}

function advance(to: 2 | 3) {
  if (to > guidedStep.value) guidedStep.value = to
}

async function copyCode() {
  copyHint.value = null
  try {
    await navigator.clipboard.writeText(flow.userCode.value)
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  } catch {
    copyHint.value = 'Não foi possível copiar automaticamente — selecione o código e copie manualmente.'
  }
  advance(2)
}

async function handleConfirm() {
  await flow.connect()
  if (flow.step.value === 'done') emit('connected')
}

/** Every close path (X, backdrop, Cancelar, route-leave) funnels through here. */
function requestClose() {
  if (flow.flowInProgress.value) {
    confirmingCancel.value = true
    return
  }
  emit('close')
}

function confirmLeave() {
  flow.finish()
  confirmingCancel.value = false
  emit('close')
}

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (flow.flowInProgress.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

function handleTabReturn() {
  if (document.visibilityState === 'hidden') return
  flow.syncRemaining()
  if (flow.step.value === 'code') {
    if (guidedStep.value === 2) advance(3)
    authInput.value?.focus()
  }
}

onBeforeRouteLeave(() => {
  if (flow.flowInProgress.value) {
    confirmingCancel.value = true
    return false
  }
  return true
})

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  document.addEventListener('visibilitychange', handleTabReturn)
  window.addEventListener('focus', handleTabReturn)
  if (!(props.resume && flow.resume())) {
    await flow.start()
  }
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  document.removeEventListener('visibilitychange', handleTabReturn)
  window.removeEventListener('focus', handleTabReturn)
})
</script>

<template>
  <UIModal
      title="Conectar iFood"
      subtitle="Leva menos de 2 minutos — seus pedidos concluídos passam a ser importados automaticamente."
      :width="560"
      title-test-id="ifood-modal-title"
      @close="requestClose"
    >
      <!-- Cancel confirmation -->
      <div v-if="confirmingCancel" data-testid="ifood-cancel-confirm">
        <div :style="{ display: 'flex', gap: '10px', alignItems: 'flex-start' }">
          <UIIcon name="alert" :size="18" :style="{ color: UI.rose, flexShrink: 0, marginTop: '2px' }" />
          <div>
            <div :style="{ fontSize: '14px', fontWeight: 600, color: UI.text }">
              Sair agora pode deixar uma autorização pendente no iFood
            </div>
            <div :style="{ fontSize: '13px', color: UI.textSub, marginTop: '6px', lineHeight: 1.5 }">
              Se você já autorizou no portal do iFood, cole o código de autorização para concluir —
              falta só esse passo. Saindo agora, você precisará gerar um novo código depois.
            </div>
          </div>
        </div>
      </div>

      <!-- Done -->
      <div
        v-else-if="flow.step.value === 'done'"
        data-testid="ifood-done-state"
        :style="{ textAlign: 'center', padding: '12px 0' }"
      >
        <div
          :style="{
            width: '52px',
            height: '52px',
            borderRadius: '50%',
            background: UI.emeraldBg,
            color: UI.emerald2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 14px',
          }"
        >
          <UIIcon name="check" :size="26" />
        </div>
        <div :style="{ fontSize: '17px', fontWeight: 700, color: UI.text }">iFood conectado!</div>
        <div :style="{ fontSize: '13px', color: UI.textSub, marginTop: '6px' }">
          Seus pedidos concluídos serão importados automaticamente.
        </div>
      </div>

      <!-- Loading -->
      <div
        v-else-if="flow.step.value === 'loading'"
        :style="{ textAlign: 'center', padding: '24px 0', color: UI.textSub, fontSize: '13px' }"
      >
        Gerando código de vínculo…
      </div>

      <!-- Code / Expired -->
      <div v-else>
        <!-- Step progress -->
        <div :style="{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '18px' }">
          <template v-for="(s, i) in GUIDED_STEPS" :key="s.n">
            <div
              :data-testid="`ifood-step-chip-${s.n}`"
              :data-state="chipState(s.n)"
              :style="{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '12px',
                fontWeight: 600,
                color: chipState(s.n) === 'todo' ? UI.textMute : chipState(s.n) === 'done' ? UI.emerald2 : UI.blue,
              }"
            >
              <span
                :style="{
                  width: '20px',
                  height: '20px',
                  borderRadius: '50%',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '11px',
                  background:
                    chipState(s.n) === 'done' ? UI.emeraldBg : chipState(s.n) === 'active' ? UI.blueBg : UI.bg,
                  border: `1px solid ${chipState(s.n) === 'todo' ? UI.border : 'transparent'}`,
                }"
              >
                <UIIcon v-if="chipState(s.n) === 'done'" name="check" :size="11" />
                <template v-else>{{ s.n }}</template>
              </span>
              {{ s.label }}
            </div>
            <div
              v-if="i < GUIDED_STEPS.length - 1"
              :style="{ flex: 1, height: '1px', background: UI.border, minWidth: '8px' }"
            />
          </template>
        </div>

        <!-- User code block -->
        <div
          :style="{
            background: UI.bg,
            border: `1px solid ${UI.border}`,
            borderRadius: '10px',
            padding: '14px 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
          }"
        >
          <div>
            <div :style="{ fontSize: '11px', color: UI.textMute, letterSpacing: '0.08em', fontWeight: 600 }">
              SEU CÓDIGO DE VÍNCULO
            </div>
            <div
              data-testid="ifood-user-code"
              :style="{
                fontSize: '26px',
                fontWeight: 700,
                letterSpacing: '0.15em',
                fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
                color: UI.text,
                marginTop: '2px',
              }"
            >
              {{ flow.userCode.value }}
            </div>
          </div>
          <UIBtn size="sm" variant="ghost" data-testid="ifood-copy-button" @click="copyCode">
            {{ copied ? 'Copiado!' : 'Copiar' }}
          </UIBtn>
        </div>
        <div v-if="copyHint" :style="{ fontSize: '12px', color: UI.textSub, marginTop: '6px' }">
          {{ copyHint }}
        </div>

        <!-- Countdown / expired -->
        <div
          v-if="flow.step.value === 'code'"
          data-testid="ifood-countdown"
          :data-urgent="urgent ? 'true' : 'false'"
          :style="{ fontSize: '12px', marginTop: '8px', color: urgent ? '#ef4444' : UI.textMute }"
        >
          ⏱ Expira em {{ countdownLabel }}
        </div>
        <div
          v-else
          data-testid="ifood-expired-state"
          :style="{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '10px' }"
        >
          <UIPill color="rose" size="sm">Código expirado</UIPill>
          <UIBtn size="sm" variant="primary" data-testid="ifood-regenerate-button" @click="flow.regenerate">
            Gerar novo código
          </UIBtn>
        </div>

        <!-- Portal link -->
        <a
          data-testid="ifood-portal-link"
          :href="portalHref"
          target="_blank"
          rel="noopener"
          :style="{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            marginTop: '16px',
            padding: '10px 14px',
            borderRadius: '9px',
            background: UI.blue,
            color: '#fff',
            fontSize: '13px',
            fontWeight: 600,
            textDecoration: 'none',
          }"
          @click="advance(2)"
        >
          <UIIcon name="link" :size="14" />
          Abrir portal do iFood (código já preenchido)
        </a>
        <div :style="{ fontSize: '12px', color: UI.textMute, marginTop: '6px', textAlign: 'center' }">
          No portal: Integrações → Ativar aplicativo por código → Confirmar
        </div>

        <!-- Authorization code input -->
        <div :style="{ marginTop: '18px' }">
          <div :style="{ fontSize: '12px', fontWeight: 600, color: UI.textSub, marginBottom: '5px' }">
            Código de autorização
          </div>
          <input
            ref="authInput"
            data-testid="ifood-auth-input"
            :value="flow.authCode.value"
            placeholder="Cole o código de autorização"
            :style="{
              width: '100%',
              boxSizing: 'border-box',
              padding: '10px 12px',
              borderRadius: '9px',
              border: `1px solid ${UI.border}`,
              background: UI.bg,
              color: UI.text,
              fontSize: '13px',
              outline: 'none',
            }"
            @input="
              flow.authCode.value = ($event.target as HTMLInputElement).value;
              advance(3)
            "
            @focus="advance(3)"
            @keyup.enter="handleConfirm"
          />
          <div :style="{ fontSize: '12px', color: UI.textMute, marginTop: '5px' }">
            {{
              flow.step.value === 'expired'
                ? 'Já autorizou no portal? Cole o código de autorização — ele ainda pode ser válido.'
                : 'Cole aqui o código que o portal do iFood forneceu.'
            }}
          </div>
        </div>

        <div
          v-if="flow.error.value"
          data-testid="ifood-error"
          :style="{ fontSize: '12px', color: '#ef4444', marginTop: '10px' }"
        >
          {{ flow.error.value }}
        </div>
      </div>

      <template #footer>
        <template v-if="confirmingCancel">
          <UIBtn variant="ghost" data-testid="ifood-cancel-continue" @click="confirmingCancel = false">
            Continuar conectando
          </UIBtn>
          <UIBtn variant="danger" data-testid="ifood-cancel-leave" @click="confirmLeave">
            Sair mesmo assim
          </UIBtn>
        </template>
        <template v-else-if="flow.step.value === 'done'">
          <UIBtn variant="primary" data-testid="ifood-done-close-button" @click="emit('close')">
            Fechar
          </UIBtn>
        </template>
        <template v-else>
          <UIBtn variant="ghost" data-testid="ifood-cancel-button" @click="requestClose">Cancelar</UIBtn>
          <UIBtn
            variant="primary"
            data-testid="ifood-confirm-button"
            :disabled="!flow.authCode.value.trim() || flow.connecting.value"
            @click="handleConfirm"
          >
            {{ flow.connecting.value ? 'Conectando…' : 'Confirmar' }}
          </UIBtn>
        </template>
      </template>
  </UIModal>
</template>
