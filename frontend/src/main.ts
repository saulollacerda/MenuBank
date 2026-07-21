import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
// Tailwind first so the hand-written app styles in main.css keep precedence.
import './assets/tailwind.css'
import './assets/main.css'
import { useAuthStore } from '@/stores/authStore'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
app.use(router)

// Restore the persisted session before mounting so the first paint already has
// auth state (no login-layout flash). The router guard also awaits this same
// single-flight init(), which is what actually protects the initial navigation —
// app.use(router) starts navigating before this await settles.
await useAuthStore(pinia).init()

app.mount('#app')
