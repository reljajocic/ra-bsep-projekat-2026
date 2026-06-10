import { createRouter, createWebHistory } from 'vue-router'
import { authService } from '../services/authService'

import LoginView from '../views/LoginView.vue'
import CertificatesView from '../views/CertificatesView.vue'
import CreateCertificateView from '../views/CreateCertificateView.vue'
import PasswordManagerView from '../views/PasswordManagerView.vue'
import SessionsView from '../views/SessionsView.vue'
import SecurityView from '../views/SecurityView.vue'

const routes = [
  { path: '/', redirect: '/certificates' },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/certificates', component: CertificatesView },
  { path: '/certificates/new', component: CreateCertificateView },
  { path: '/passwords', component: PasswordManagerView },
  { path: '/sessions', component: SessionsView },
  { path: '/security', component: SecurityView}
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guard - ako nije ulogovan, redirektuj na login
router.beforeEach((to, from) => {
  if (!to.meta.public && !authService.isLoggedIn()) {
    return '/login'
  } else if (to.path === '/login' && authService.isLoggedIn()) {
    return '/certificates'
  }
})

export default router