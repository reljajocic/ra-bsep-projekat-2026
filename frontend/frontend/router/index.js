import { createRouter, createWebHistory } from 'vue-router'
import { authService } from '../services/authService'

import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import ActivateView from '../views/ActivateView.vue'
import ChangePasswordView from '../views/ChangePasswordView.vue'
import CaUsersView from '../views/CaUsersView.vue'
import TemplatesView from '../views/TemplatesView.vue'
import CertificatesView from '../views/CertificatesView.vue'
import CreateCertificateView from '../views/CreateCertificateView.vue'
import PasswordManagerView from '../views/PasswordManagerView.vue'
import SessionsView from '../views/SessionsView.vue'
import SecurityView from '../views/SecurityView.vue'

const routes = [
  { path: '/', redirect: '/certificates' },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/register', component: RegisterView, meta: { public: true } },
  { path: '/activate', component: ActivateView, meta: { public: true } },
  { path: '/certificates', component: CertificatesView },
  { path: '/certificates/new', component: CreateCertificateView },
  { path: '/passwords', component: PasswordManagerView, meta: { adminOnly: true } },
  { path: '/ca-users', component: CaUsersView, meta: { adminOnly: true } },
  { path: '/templates', component: TemplatesView, meta: { caOrAdmin: true } },
  { path: '/change-password', component: ChangePasswordView },
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
  } else if (to.meta.adminOnly && !authService.isAdmin()) {
    return '/certificates'
  } else if (to.meta.caOrAdmin && !(authService.isAdmin() || authService.getRole() === 'CA_USER')) {
    return '/certificates'
  } else if (authService.isLoggedIn() && authService.mustChangePassword()
             && to.path !== '/change-password') {
    // Forsiraj promenu podrazumevane lozinke pre bilo cega drugog
    return '/change-password'
  }
})

export default router