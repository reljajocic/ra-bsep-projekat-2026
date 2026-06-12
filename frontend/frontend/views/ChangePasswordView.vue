<template>
  <div class="cp-container">
    <div class="cp-box">
      <h2>Promena lozinke</h2>
      <p v-if="forced" class="forced">Pri prvoj prijavi morate da promenite podrazumevanu lozinku.</p>

      <form @submit.prevent="submit">
        <div class="form-group">
          <label>Trenutna lozinka</label>
          <input v-model="oldPassword" type="password" required />
        </div>
        <div class="form-group">
          <label>Nova lozinka</label>
          <input v-model="newPassword" type="password" required />
          <ul v-if="newPassword" class="reqs">
            <li :class="{ ok: req.len }">Najmanje 8 karaktera</li>
            <li :class="{ ok: req.upper }">Veliko slovo</li>
            <li :class="{ ok: req.lower }">Malo slovo</li>
            <li :class="{ ok: req.digit }">Cifra</li>
            <li :class="{ ok: req.special }">Specijalni karakter</li>
          </ul>
        </div>
        <div class="form-group">
          <label>Ponovi novu lozinku</label>
          <input v-model="newPassword2" type="password" required />
          <p v-if="newPassword2 && newPassword !== newPassword2" class="error">Lozinke se ne poklapaju</p>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>

        <button type="submit" :disabled="loading || !canSubmit">
          {{ loading ? 'Čuvanje...' : 'Promeni lozinku' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  data() {
    return { oldPassword: '', newPassword: '', newPassword2: '', error: '', success: '', loading: false }
  },
  computed: {
    forced() { return authService.mustChangePassword() },
    req() {
      const p = this.newPassword
      return {
        len: p.length >= 8, upper: /[A-Z]/.test(p), lower: /[a-z]/.test(p),
        digit: /[0-9]/.test(p), special: /[^A-Za-z0-9]/.test(p)
      }
    },
    canSubmit() {
      const r = this.req
      return r.len && r.upper && r.lower && r.digit && r.special
        && this.newPassword === this.newPassword2 && this.oldPassword
    }
  },
  methods: {
    async submit() {
      this.error = ''; this.success = ''; this.loading = true
      try {
        await authService.changePassword(this.oldPassword, this.newPassword)
        this.success = 'Lozinka uspešno promenjena.'
        setTimeout(() => this.$router.push('/certificates'), 1200)
      } catch (e) {
        this.error = e.response?.data?.error || 'Greška pri promeni lozinke'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.cp-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f5f5; }
.cp-box { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); width: 100%; max-width: 420px; }
h2 { margin-bottom: 1rem; text-align: center; }
.forced { background: #fff3cd; border: 1px solid #ffc107; padding: 0.7rem; border-radius: 4px; font-size: 0.85rem; margin-bottom: 1rem; }
.form-group { margin-bottom: 1rem; }
label { display: block; margin-bottom: 0.3rem; font-weight: 500; }
input { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
button { width: 100%; padding: 0.7rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; }
button:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #e74c3c; font-size: 0.85rem; }
.success { color: #27ae60; font-size: 0.9rem; }
.reqs { list-style: none; padding: 0; margin: 0.4rem 0 0; font-size: 0.78rem; color: #999; }
.reqs li::before { content: '✗ '; color: #e74c3c; }
.reqs li.ok { color: #27ae60; }
.reqs li.ok::before { content: '✓ '; color: #27ae60; }
</style>
