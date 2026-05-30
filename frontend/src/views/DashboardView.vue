<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { usePolling } from '@/composables/usePolling'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboardStore'
import { useOrderStore } from '@/stores/orderStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { useAuthStore } from '@/stores/authStore'
import { UI, UITopbar, UIBtn, UIIcon, brl, num } from '@/design'
import type { OrderOrigin } from '@/types/Order'

const dash = useDashboardStore()
const orderStore = useOrderStore()
const notif = useNotificationStore()
const auth = useAuthStore()
const router = useRouter()

const MONTHS_PT = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
]

const periodLabel = computed(
  () => `${MONTHS_PT[dash.selectedMonthNumber - 1]} ${dash.selectedYear}`,
)

const kpis = computed(() => {
  const d = dash.data
  return [
    { icon: 'money', iconBg: UI.emeraldBg, iconColor: UI.emerald2, label: 'Faturamento',  value: brl(Number(d?.totalSales ?? 0)) },
    { icon: 'receipt', iconBg: UI.blueBg, iconColor: UI.blue,      label: 'Pedidos',       value: num(Number(d?.orderCount ?? 0)) },
    { icon: 'chart', iconBg: '#f3e8ff',    iconColor: '#7c3aed',    label: 'Ticket médio',  value: brl(Number(d?.averageTicket ?? 0)) },
    { icon: 'trend', iconBg: UI.emeraldBg, iconColor: UI.emerald2,  label: 'Lucro estimado', value: brl(Number(d?.estimatedProfit ?? 0)) },
  ]
})

const sales = computed(() => dash.data?.salesByDay ?? [])
const maxSale = computed(() => Math.max(1, ...sales.value.map((s) => Number(s.total))))

const topProducts = computed(() => dash.data?.topProducts ?? [])

const recentOrders = computed(() => orderStore.items.slice(0, 5))
const missingIngredientAlerts = computed(() =>
  notif.items.filter((n) => n.type === 'MISSING_INGREDIENT' && n.status !== 'RESOLVED').slice(0, 4),
)

const STATUS_STYLE: Record<string, { bg: string; fg: string; label: string }> = {
  PENDING: { bg: '#fef3c7', fg: '#92400e', label: 'pendente' },
  PAID: { bg: '#dcfce7', fg: '#166534', label: 'pago' },
  CANCELLED: { bg: '#fee2e2', fg: '#991b1b', label: 'cancelado' },
}

function originLabel(o: OrderOrigin | undefined): string {
  if (o === 'ANOTA_AI') return 'Anota.AI'
  if (o === 'IFOOD') return 'iFood'
  return 'MenuBank'
}

function timeOf(iso: string): string {
  const d = new Date(iso)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatDay(dateStr: string): string {
  const d = new Date(dateStr + 'T00:00:00')
  return String(d.getDate()).padStart(2, '0')
}

const yearOptions = computed(() => {
  const now = new Date().getFullYear()
  const start = 2026
  const end = Math.max(now, start)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

const monthOptions = computed(() => {
  const maxMonth = dash.selectedYear === currentYear ? currentMonth : 12
  return MONTHS_PT.slice(0, maxMonth).map((label, i) => ({ value: i + 1, label }))
})

watch(
  () => dash.selectedYear,
  () => {
    const max = dash.selectedYear === currentYear ? currentMonth : 12
    if (dash.selectedMonthNumber > max) dash.selectedMonthNumber = max
  },
)

watch(
  () => [dash.selectedYear, dash.selectedMonthNumber],
  () => {
    dash.fetchDashboard(true)
  },
)

onMounted(() => {
  dash.fetchDashboard()
  orderStore.fetchPage({ page: 0 }).catch(() => {})
  notif.fetchAll().catch(() => {})
})

usePolling(() => { orderStore.fetchPage({ page: 0 }, true).catch(() => {}) }, 30_000)
usePolling(() => { dash.fetchDashboard(true, true).catch(() => {}) }, 60_000)

function navOrders() {
  router.push('/orders')
}

function navIngredients() {
  router.push('/ingredients')
}

function navIngredientsWithName(name: string | null) {
  router.push({ name: 'ingredients', query: name ? { createName: name } : {} })
}
</script>

<template>
  <div style="display: flex; flex-direction: column; flex: 1">
    <UITopbar
      :title="`Olá, ${auth.restaurantName || 'MenuBank'} 👋`"
      :subtitle="`Resumo da sua operação em ${periodLabel}`"
      :period-label="periodLabel"
    >
      <template #actions>
        <select
          v-model.number="dash.selectedMonthNumber"
          :style="{
            padding: '8px 10px',
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '9px',
            fontSize: '12.5px',
            color: UI.text,
            cursor: 'pointer',
          }"
        >
          <option v-for="m in monthOptions" :key="m.value" :value="m.value">{{ m.label }}</option>
        </select>
        <select
          v-model.number="dash.selectedYear"
          :style="{
            padding: '8px 10px',
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '9px',
            fontSize: '12.5px',
            color: UI.text,
            cursor: 'pointer',
          }"
        >
          <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
        </select>
        <UIBtn
          icon="download"
          variant="secondary"
          :disabled="dash.exporting"
          @click="dash.exportDashboard()"
        >
          Exportar
        </UIBtn>
        <UIBtn
          icon="check"
          variant="dark"
          :disabled="dash.exporting"
          @click="dash.exportDayClosing()"
        >
          Fechamento
        </UIBtn>
      </template>
    </UITopbar>

    <div
      style="
        flex: 1;
        padding: 28px;
        display: flex;
        flex-direction: column;
        gap: 18px;
        overflow: auto;
      "
    >
      <div
        v-if="dash.error"
        :style="{
          padding: '12px 16px',
          background: UI.roseBg,
          color: UI.rose2,
          borderRadius: '10px',
          fontSize: '13px',
        }"
      >
        {{ dash.error }}
      </div>

      <!-- KPIs -->
      <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px">
        <div
          v-for="k in kpis"
          :key="k.label"
          :style="{
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '14px',
            padding: '18px',
            display: 'flex',
            flexDirection: 'column',
            gap: '14px',
          }"
        >
          <div style="display: flex; align-items: center; gap: 10px">
            <div
              :style="{
                width: '34px',
                height: '34px',
                borderRadius: '9px',
                background: k.iconBg,
                color: k.iconColor,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }"
            >
              <UIIcon :name="k.icon" :size="17" />
            </div>
            <div :style="{ fontSize: '12.5px', color: UI.textSub, fontWeight: 500 }">
              {{ k.label }}
            </div>
          </div>
          <div>
            <div
              :style="{
                fontSize: '26px',
                fontWeight: 700,
                color: UI.text,
                letterSpacing: '-0.6px',
                lineHeight: 1.1,
                fontVariantNumeric: 'tabular-nums',
              }"
            >
              {{ dash.loading ? '—' : k.value }}
            </div>
          </div>
        </div>
      </div>

      <!-- Mid: sales chart + peak hours placeholder -->
      <div style="display: flex; gap: 16px; min-height: 348px">
        <div
          :style="{
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '14px',
            padding: '20px 22px',
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
          }"
        >
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px">
            <div>
              <div :style="{ fontSize: '14px', fontWeight: 600, color: UI.text }">Vendas por dia</div>
              <div :style="{ fontSize: '12px', color: UI.textSub, marginTop: '2px' }">
                {{ sales.length }} dias · R$
              </div>
            </div>
          </div>
          <div
            v-if="!sales.length"
            :style="{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: UI.textMute,
              fontSize: '13px',
            }"
          >
            Nenhuma venda no período.
          </div>
          <div
            v-else
            style="
              display: flex;
              align-items: flex-end;
              gap: 7px;
              flex: 1;
              padding-top: 14px;
              position: relative;
            "
          >
            <div
              v-for="(d, i) in sales"
              :key="i"
              style="
                flex: 1;
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 6px;
              "
            >
              <div style="width: 100%; height: 220px; display: flex; align-items: flex-end">
                <div
                  :style="{
                    width: '100%',
                    height: ((Number(d.total) / maxSale) * 100) + '%',
                    background: Number(d.total) === maxSale ? UI.emerald : '#a7f3d0',
                    borderRadius: '6px 6px 0 0',
                  }"
                />
              </div>
              <div
                :style="{
                  fontSize: '10.5px',
                  color: UI.textMute,
                  fontVariantNumeric: 'tabular-nums',
                }"
              >
                {{ formatDay(d.date) }}
              </div>
            </div>
          </div>
        </div>

        <!-- Peak hours / placeholder until backend supports -->
        <div
          :style="{
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '14px',
            padding: '20px 22px',
            width: '320px',
            display: 'flex',
            flexDirection: 'column',
          }"
        >
          <div :style="{ fontSize: '14px', fontWeight: 600, color: UI.text }">Horário de pico</div>
          <div :style="{ fontSize: '12px', color: UI.textSub, marginTop: '2px' }">
            Pedidos por hora
          </div>
          <div
            :style="{
              marginTop: '18px',
              padding: '12px 14px',
              background: UI.amberBg,
              borderRadius: '10px',
              display: 'flex',
              alignItems: 'center',
              gap: '11px',
              color: UI.amber2,
            }"
          >
            <UIIcon name="info" :size="18" />
            <div style="flex: 1">
              <div :style="{ fontSize: '11px', fontWeight: 500 }">Em breve</div>
              <div :style="{ fontSize: '13px', fontWeight: 600, marginTop: '2px' }">
                Backend ainda não expõe esse dado
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom: top products + alerts + recent orders -->
      <div style="display: flex; gap: 16px; flex: 1; min-height: 0">
        <!-- Top products -->
        <div
          :style="{
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '14px',
            padding: '20px 22px',
            flex: 1,
          }"
        >
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px">
            <div>
              <div :style="{ fontSize: '14px', fontWeight: 600, color: UI.text }">Top produtos</div>
              <div :style="{ fontSize: '12px', color: UI.textSub, marginTop: '2px' }">{{ periodLabel }}</div>
            </div>
            <div
              :style="{ fontSize: '11.5px', color: UI.blue, fontWeight: 500, cursor: 'pointer' }"
              @click="router.push('/products')"
            >
              Ver tudo →
            </div>
          </div>
          <div v-if="!topProducts.length" :style="{ color: UI.textMute, fontSize: '13px' }">
            Sem vendas no período.
          </div>
          <div v-else style="display: flex; flex-direction: column; gap: 10px">
            <div
              v-for="(p, i) in topProducts"
              :key="p.productName"
              style="display: flex; align-items: center; gap: 11px"
            >
              <div
                :style="{
                  width: '22px',
                  height: '22px',
                  borderRadius: '6px',
                  background: i === 0 ? UI.emerald : UI.bg,
                  color: i === 0 ? '#fff' : UI.textSub,
                  fontSize: '11px',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }"
              >
                {{ i + 1 }}
              </div>
              <div style="flex: 1; min-width: 0">
                <div
                  :style="{
                    fontSize: '12.5px',
                    fontWeight: 600,
                    color: UI.text,
                    marginBottom: '2px',
                  }"
                >
                  {{ p.productName }}
                </div>
                <div :style="{ fontSize: '10.5px', color: UI.textMute }">
                  {{ p.quantitySold }} vendas
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Missing ingredient alerts -->
        <div
          :style="{
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '14px',
            padding: '20px 22px',
            flex: 1,
          }"
        >
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px">
            <div style="display: flex; align-items: center; gap: 8px">
              <div
                :style="{
                  width: '22px',
                  height: '22px',
                  borderRadius: '6px',
                  background: UI.amberBg,
                  color: UI.amber,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }"
              >
                <UIIcon name="alert" :size="13" />
              </div>
              <div>
                <div :style="{ fontSize: '14px', fontWeight: 600, color: UI.text }">
                  Ingredientes ausentes
                </div>
                <div :style="{ fontSize: '11.5px', color: UI.textSub, marginTop: '1px' }">
                  {{ missingIngredientAlerts.length }} alertas pendentes
                </div>
              </div>
            </div>
          </div>
          <div
            v-if="!missingIngredientAlerts.length"
            :style="{ color: UI.textMute, fontSize: '13px' }"
          >
            Tudo certo · nenhum ingrediente ausente.
          </div>
          <div v-else style="display: flex; flex-direction: column; gap: 8px">
            <div
              v-for="a in missingIngredientAlerts"
              :key="a.id"
              :style="{
                padding: '10px 12px',
                background: UI.amberBg + '70',
                borderLeft: `3px solid ${UI.amber}`,
                borderRadius: '0 8px 8px 0',
              }"
            >
              <div
                style="
                  display: flex;
                  justify-content: space-between;
                  align-items: baseline;
                  margin-bottom: 4px;
                "
              >
                <div :style="{ fontSize: '12.5px', fontWeight: 600, color: UI.text }">
                  {{ a.referenceDisplay || a.title }}
                </div>
              </div>
              <div
                :style="{
                  fontSize: '10.5px',
                  color: UI.textMute,
                  display: 'flex',
                  justifyContent: 'space-between',
                }"
              >
                <span>{{ a.message }}</span>
                <span
                  :style="{ color: UI.blue, fontWeight: 600, cursor: 'pointer' }"
                  @click="navIngredientsWithName(a.referenceDisplay)"
                >
                  Cadastrar
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent orders -->
        <div
          :style="{
            background: UI.panel,
            border: `1px solid ${UI.border}`,
            borderRadius: '14px',
            padding: '20px 22px',
            flex: 1,
          }"
        >
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px">
            <div>
              <div :style="{ fontSize: '14px', fontWeight: 600, color: UI.text }">
                Pedidos recentes
              </div>
              <div :style="{ fontSize: '12px', color: UI.textSub, marginTop: '2px' }">
                Últimos · todos os canais
              </div>
            </div>
            <div
              :style="{ fontSize: '11.5px', color: UI.blue, fontWeight: 500, cursor: 'pointer' }"
              @click="navOrders"
            >
              Abrir →
            </div>
          </div>
          <div v-if="!recentOrders.length" :style="{ color: UI.textMute, fontSize: '13px' }">
            Sem pedidos recentes.
          </div>
          <div v-else style="display: flex; flex-direction: column">
            <div
              v-for="(o, i) in recentOrders"
              :key="o.id"
              :style="{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '9px 0',
                borderBottom: i === recentOrders.length - 1 ? 'none' : `1px solid ${UI.borderSub}`,
              }"
            >
              <div
                :style="{
                  fontSize: '11px',
                  color: UI.textMute,
                  width: '38px',
                  fontVariantNumeric: 'tabular-nums',
                }"
              >
                {{ timeOf(o.dateTime) }}
              </div>
              <div style="flex: 1; min-width: 0">
                <div
                  :style="{
                    fontSize: '12.5px',
                    fontWeight: 600,
                    color: UI.text,
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }"
                >
                  {{ o.customerName }}
                </div>
                <div :style="{ fontSize: '10.5px', color: UI.textMute }">
                  {{ originLabel(o.origin) }}
                </div>
              </div>
              <div
                :style="{
                  fontSize: '10px',
                  fontWeight: 600,
                  background: STATUS_STYLE[o.status]?.bg ?? '#f1f5f9',
                  color: STATUS_STYLE[o.status]?.fg ?? '#475569',
                  padding: '2px 7px',
                  borderRadius: '4px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.4px',
                }"
              >
                {{ STATUS_STYLE[o.status]?.label ?? o.status }}
              </div>
              <div
                :style="{
                  fontSize: '12px',
                  fontWeight: 600,
                  color: UI.text,
                  fontVariantNumeric: 'tabular-nums',
                  width: '70px',
                  textAlign: 'right',
                }"
              >
                {{ brl(Number(o.totalValue)) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped></style>
