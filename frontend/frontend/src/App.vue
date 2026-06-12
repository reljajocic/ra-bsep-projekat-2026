<template>
  <div>
    <nav v-if="isLoggedIn">
      <div class="nav-brand">BSEP PKI <span class="role-tag">{{ role }}</span></div>
      <div class="nav-links">
        <router-link to="/certificates">Sertifikati</router-link>
        <router-link v-if="isAdmin" to="/ca-users">CA korisnici</router-link>
        <router-link v-if="isAdmin || isCaUser" to="/templates">Šabloni</router-link>
        <router-link v-if="isAdmin" to="/passwords">Lozinke</router-link>
        <router-link to="/sessions">Sesije</router-link>
        <router-link to="/security">Security</router-link>
      </div>
      <button @click="logout" class="nav-logout">Odjavi se</button>
    </nav>
    <router-view />
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  computed: {
    isLoggedIn() {
      return authService.isLoggedIn()
    },
    isAdmin() {
      return authService.isAdmin()
    },
    isCaUser() {
      return authService.getRole() === 'CA_USER'
    },
    role() {
      return authService.getRole()
    }
  },
  methods: {
    logout() {
      authService.logout()
    }
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; }
nav {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 0.8rem 2rem;
  background: #2c3e50;
  color: white;
}
.nav-brand { font-weight: 700; font-size: 1.1rem; margin-right: auto; }
.role-tag { font-size: 0.7rem; background: #1abc9c; padding: 0.1rem 0.4rem; border-radius: 3px; margin-left: 0.4rem; vertical-align: middle; }
.nav-links { display: flex; gap: 1rem; }
.nav-links a { color: #bdc3c7; text-decoration: none; }
.nav-links a.router-link-active { color: white; font-weight: 600; }
.nav-logout { padding: 0.4rem 0.8rem; background: #e74c3c; color: white; border: none; border-radius: 4px; cursor: pointer; }
</style>