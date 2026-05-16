<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

const store = useDashboardStore()

function formatCurrency(value: number | null | undefined): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    value ?? 0,
  )
}

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: (value: string | number) =>
          `R$ ${Number(value).toLocaleString('pt-BR')}`,
      },
    },
  },
}

function getChartData() {
  const salesByDay = store.data?.salesByDay ?? []
  return {
    labels: salesByDay.map((s) => {
      const d = new Date(s.date + 'T00:00:00')
      return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
    }),
    datasets: [
      {
        label: 'Vendas',
        data: salesByDay.map((s) => s.total),
        backgroundColor: '#2563eb',
        borderRadius: 4,
      },
    ],
  }
}

function handleFilter() {
  store.fetchDashboard(true)
}

const MONTHS_PT = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
]

const START_YEAR = 2026

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = now.getMonth() + 1

const yearOptions = computed<number[]>(() => {
  const years: number[] = []
  const end = Math.max(currentYear, START_YEAR)
  for (let y = START_YEAR; y <= end; y++) {
    years.push(y)
  }
  return years
})

const monthOptions = computed<{ value: number; label: string }[]>(() => {
  const maxMonth = store.selectedYear === currentYear ? currentMonth : 12
  return MONTHS_PT.slice(0, maxMonth).map((label, idx) => ({ value: idx + 1, label }))
})

watch(
  () => store.selectedYear,
  () => {
    const maxMonth = store.selectedYear === currentYear ? currentMonth : 12
    if (store.selectedMonthNumber > maxMonth) {
      store.selectedMonthNumber = maxMonth
    }
  },
)

onMounted(() => {
  store.fetchDashboard()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Dashboard</h1>
      <div class="date-filter">
        <template v-if="store.filterMode === 'month'">
          <label>Mês:</label>
          <select v-model.number="store.selectedMonthNumber">
            <option v-for="m in monthOptions" :key="m.value" :value="m.value">
              {{ m.label }}
            </option>
          </select>
          <select v-model.number="store.selectedYear">
            <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}</option>
          </select>
          <button class="btn btn-link btn-sm" @click="store.filterMode = 'custom'">
            Usar período personalizado
          </button>
        </template>
        <template v-else>
          <label>De:</label>
          <input v-model="store.startDate" type="date" />
          <label>Até:</label>
          <input v-model="store.endDate" type="date" />
          <button class="btn btn-link btn-sm" @click="store.filterMode = 'month'">
            Usar seleção por mês
          </button>
        </template>
        <button class="btn btn-primary btn-sm" @click="handleFilter">Filtrar</button>
        <button
          class="btn btn-secondary btn-sm"
          :disabled="store.exporting"
          @click="store.exportDashboard()"
        >
          <span v-if="store.exporting">Exportando...</span>
          <span v-else>Exportar para Excel</span>
        </button>
        <button
          class="btn btn-secondary btn-sm"
          :disabled="store.exporting"
          @click="store.exportDayClosing()"
        >
          <span v-if="store.exporting">Exportando...</span>
          <span v-else>Fechamento do Dia</span>
        </button>
      </div>
    </div>

    <div v-if="store.loading" class="loading-container">
      <div class="spinner" />
    </div>

    <div v-else-if="store.error" class="alert alert-error">
      {{ store.error }}
    </div>

    <template v-else-if="store.data">
      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="kpi-label">Faturamento</div>
          <div class="kpi-value">{{ formatCurrency(store.data.totalSales) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Quantidade de Pedidos</div>
          <div class="kpi-value">{{ store.data.orderCount ?? 0 }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Ticket Médio</div>
          <div class="kpi-value">{{ formatCurrency(store.data.averageTicket) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Lucro Estimado</div>
          <div class="kpi-value">{{ formatCurrency(store.data.estimatedProfit) }}</div>
        </div>
      </div>

      <div class="dashboard-grid">
        <div class="chart-container">
          <h3>Vendas por Dia</h3>
          <div style="height: 300px">
            <Bar
              v-if="store.data.salesByDay && store.data.salesByDay.length > 0"
              :data="getChartData()"
              :options="chartOptions"
            />
            <div v-else class="empty-state">
              <p>Nenhum dado de vendas para o período selecionado.</p>
            </div>
          </div>
        </div>
        <div class="chart-container">
          <h3>Top 5 Produtos</h3>
          <ul
            v-if="store.data.topProducts && store.data.topProducts.length > 0"
            class="top-products-list"
          >
            <li v-for="(product, index) in store.data.topProducts" :key="product.productName">
              <span>
                <span class="rank">{{ index + 1 }}º</span>
                {{ product.productName }}
              </span>
              <span class="qty">{{ product.quantitySold }} vendas</span>
            </li>
          </ul>
          <div v-else class="empty-state">
            <p>Nenhum produto vendido no período.</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped></style>
