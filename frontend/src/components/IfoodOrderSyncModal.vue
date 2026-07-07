<script setup lang="ts">
import { ref } from 'vue'
import { UI, UIBtn, UIIcon, UIModal } from '@/design'
import { ifoodAuthService, type IfoodStatusResponse } from '@/services/ifoodAuthService'

const props = defineProps<{
  enabled: boolean
  catalogImported: boolean
}>()

const emit = defineEmits<{
  (e: 'updated', status: IfoodStatusResponse): void
  (e: 'close'): void
}>()

const saving = ref(false)
const error = ref<string | null>(null)

async function toggle() {
  saving.value = true
  error.value = null
  try {
    const status = await ifoodAuthService.setOrderSync(!props.enabled)
    emit('updated', status)
  } catch {
    error.value = 'Não foi possível atualizar a sincronia de pedidos. Tente novamente em instantes.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <UIModal
    title="Sincronia de pedidos do iFood"
    subtitle="Com a sincronia ativa, seus pedidos do iFood são importados automaticamente a cada 30 segundos."
    :width="520"
    title-test-id="ifood-sync-modal-title"
    @close="emit('close')"
  >
    <div :style="{ fontSize: '13px', color: UI.textSub, lineHeight: 1.6 }">
      {{
        enabled
          ? 'A sincronia está ativa. Ao desativar, novos pedidos do iFood deixam de ser importados até você reativar.'
          : 'Ative para começar a importar os pedidos concluídos da sua loja no iFood, com cálculo de custo e lucro por pedido.'
      }}
    </div>

    <div
      v-if="!catalogImported"
      data-testid="ifood-sync-warning"
      :style="{
        display: 'flex',
        gap: '10px',
        alignItems: 'flex-start',
        marginTop: '14px',
        padding: '10px 12px',
        borderRadius: '9px',
        background: UI.bg,
      }"
    >
      <UIIcon name="alert" :size="16" :style="{ color: UI.rose, flexShrink: 0, marginTop: '2px' }" />
      <div :style="{ fontSize: '12.5px', color: UI.textSub, lineHeight: 1.5 }">
        Você ainda não importou o cardápio do iFood. Itens de produtos não cadastrados serão
        <strong>ignorados</strong> nos pedidos, distorcendo custo e lucro. Recomendamos importar
        o cardápio antes de ativar.
      </div>
    </div>

    <div
      v-if="error"
      data-testid="ifood-sync-error"
      :style="{ fontSize: '12px', color: '#ef4444', marginTop: '12px' }"
    >
      {{ error }}
    </div>

    <template #footer>
      <UIBtn variant="ghost" data-testid="ifood-sync-cancel" @click="emit('close')">
        Cancelar
      </UIBtn>
      <UIBtn
        :variant="enabled ? 'danger' : 'primary'"
        data-testid="ifood-sync-toggle"
        :disabled="saving"
        @click="toggle"
      >
        {{ saving ? 'Salvando…' : enabled ? 'Desativar sincronia' : 'Ativar sincronia' }}
      </UIBtn>
    </template>
  </UIModal>
</template>
