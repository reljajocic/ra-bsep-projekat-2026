<template>
  <div class="register-container">
    <div class="register-box">
      <h2>Registracija</h2>
      <form @submit.prevent="handleRegister">
        <div class="row">
          <div class="form-group">
            <label>Ime</label>
            <input v-model.trim="firstName" type="text" required />
          </div>
          <div class="form-group">
            <label>Prezime</label>
            <input v-model.trim="lastName" type="text" required />
          </div>
        </div>

        <div class="form-group">
          <label>Email</label>
          <input v-model.trim="email" type="email" required />
        </div>

        <div class="form-group">
          <label>Organizacija</label>
          <input v-model.trim="organization" type="text" required />
        </div>

        <div class="form-group">
          <label>Lozinka</label>
          <input v-model="password" type="password" required />
          <!-- Estimator jacine lozinke -->
          <div v-if="password" class="strength">
            <div class="bar">
              <div class="fill" :style="{ width: strength.percent + '%', background: strength.color }"></div>
            </div>
            <span :style="{ color: strength.color }">{{ strength.label }}</span>
          </div>
          <ul v-if="password" class="reqs">
            <li :class="{ ok: req.len }">Najmanje 8 karaktera</li>
            <li :class="{ ok: req.upper }">Veliko slovo</li>
            <li :class="{ ok: req.lower }">Malo slovo</li>
            <li :class="{ ok: req.digit }">Cifra</li>
            <li :class="{ ok: req.special }">Specijalni karakter</li>
          </ul>
        </div>

        <div class="form-group">
          <label>Ponovi lozinku</label>
          <input v-model="password2" type="password" required />
          <p v-if="password2 && password !== password2" class="error">Lozinke se ne poklapaju</p>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>

        <button type="submit" :disabled="loading || !canSubmit">
          {{ loading ? 'Registracija...' : 'Registruj se' }}
        </button>
      </form>
      <p class="link"><router-link to="/login">Već imaš nalog? Prijavi se</router-link></p>
    </div>
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  data() {
    return {
      firstName: '', lastName: '', email: '', organization: '',
      password: '', password2: '',
      error: '', success: '', loading: false
    }
  },
  computed: {
    req() {
      const p = this.password
      return {
        len: p.length >= 8,
        upper: /[A-Z]/.test(p),
        lower: /[a-z]/.test(p),
        digit: /[0-9]/.test(p),
        special: /[^A-Za-z0-9]/.test(p)
      }
    },
    strength() {
      const r = this.req
      let score = Object.values(r).filter(Boolean).length
      if (this.password.length >= 12) score++
      const levels = [
        { label: 'Veoma slaba', color: '#e74c3c' },
        { label: 'Slaba', color: '#e67e22' },
        { label: 'Osrednja', color: '#f1c40f' },
        { label: 'Dobra', color: '#2ecc71' },
        { label: 'Jaka', color: '#27ae60' },
        { label: 'Veoma jaka', color: '#16a085' }
      ]
      const idx = Math.min(score, levels.length - 1)
      return { ...levels[idx], percent: ((idx + 1) / levels.length) * 100 }
    },
    canSubmit() {
      const r = this.req
      return r.len && r.upper && r.lower && r.digit && r.special
        && this.password === this.password2
        && this.firstName && this.lastName && this.email && this.organization
    }
  },
  methods: {
    async handleRegister() {
      this.error = ''
      this.success = ''
      this.loading = true
      try {
        const res = await authService.register({
          email: this.email,
          password: this.password,
          firstName: this.firstName,
          lastName: this.lastName,
          organization: this.organization
        })
        this.success = res.message || 'Registracija uspešna. Proverite email za aktivacioni link.'
      } catch (e) {
        this.error = e.response?.data?.error || 'Greška pri registraciji'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f5f5; }
.register-box { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); width: 100%; max-width: 440px; }
h2 { margin-bottom: 1.5rem; text-align: center; }
.row { display: flex; gap: 0.75rem; }
.row .form-group { flex: 1; }
.form-group { margin-bottom: 1rem; }
label { display: block; margin-bottom: 0.3rem; font-weight: 500; }
input { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
button { width: 100%; padding: 0.7rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; margin-top: 0.5rem; }
button:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #e74c3c; font-size: 0.85rem; margin-top: 0.3rem; }
.success { color: #27ae60; font-size: 0.9rem; }
.strength { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.5rem; font-size: 0.8rem; }
.bar { flex: 1; height: 6px; background: #eee; border-radius: 3px; overflow: hidden; }
.fill { height: 100%; transition: width 0.2s, background 0.2s; }
.reqs { list-style: none; padding: 0; margin: 0.5rem 0 0; font-size: 0.78rem; color: #999; }
.reqs li::before { content: '✗ '; color: #e74c3c; }
.reqs li.ok { color: #27ae60; }
.reqs li.ok::before { content: '✓ '; color: #27ae60; }
.link { text-align: center; margin-top: 1rem; font-size: 0.9rem; }
</style>
