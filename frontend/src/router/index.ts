import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/email-verificado',
      name: 'email-verified',
      component: () => import('@/views/EmailVerifiedView.vue'),
      // Confirming the email logs the user in (detectSessionInUrl), so this page must
      // stay visible even when authenticated instead of bouncing to the dashboard.
      meta: { public: true, allowAuthenticated: true },
    },
    {
      path: '/',
      name: 'landing',
      component: () => import('@/views/LandingView.vue'),
      meta: { public: true, allowAuthenticated: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/OrdersView.vue'),
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('@/views/ProductsView.vue'),
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('@/views/CategoriesView.vue'),
    },
    {
      path: '/ingredients',
      name: 'ingredients',
      component: () => import('@/views/IngredientsView.vue'),
    },
    {
      path: '/customers',
      name: 'customers',
      component: () => import('@/views/CustomersView.vue'),
    },
    {
      path: '/fees',
      name: 'fees',
      component: () => import('@/views/FeesView.vue'),
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/SettingsView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const isPublic = to.meta.public === true

  if (!auth.isAuthenticated && !isPublic) {
    return { name: 'login' }
  }

  if (auth.isAuthenticated && isPublic && to.meta.allowAuthenticated !== true) {
    return { name: 'dashboard' }
  }
})

export default router
