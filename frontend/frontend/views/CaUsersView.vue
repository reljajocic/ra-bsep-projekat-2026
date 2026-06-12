<template>
  <div class="page">
    <div class="header">
      <h2>CA korisnici</h2>
      <button @click="showModal = true">+ Novi CA korisnik</button>
    </div>

    <table v-if="users.length">
      <thead>
        <tr>
          <th>Email</th>
          <th>Ime i prezime</th>
          <th>Organizacija</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.email }}</td>
          <td>{{ u.firstName }} {{ u.lastName }}</td>
          <td>{{ u.organization }}</td>
          <td>
            <span :class="u.mustChangePassword ? 'pending' : 'active'">
              {{ u.mustChangePassword ? 'Čeka prvu promenu lozinke' : 'Aktivan' }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else>Nema CA korisnika.</p>

    <div v-if="showModal" class="modal-overlay">
      <div class="modal">
        <h3>Novi CA korisnik</h3>
        <div class="form-group"><label>Email</label><input v-model.trim="form.email" type="email" /></div>
        <div class="row">
          <div class="form-group"><label>Ime</label><input v-model.trim="form.firstName" /></div>
          <div class="form-group"><label>Prezime</label><input v-model.trim="form.lastName" /></div>
        </div>
        <div class="form-group"><label>Organizacija</label><input v-model.trim="form.organization" /></div>
        <p class="hint">Sistem generiše nasumičnu lozinku i šalje je korisniku na email.</p>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>
        <div class="modal-actions">
          <button @click="close">Zatvori</button>
          <button @click="create" :disabled="loading">{{ loading ? 'Kreiranje...' : 'Kreiraj' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  data() {
    return {
      users: [],
      showModal: false,
      form: { email: '', firstName: '', lastName: '', organization: '' },
      error: '', success: '', loading: false
    }
  },
  async mounted() { await this.load() },
  methods: {
    async load() { this.users = await authService.getCaUsers() },
    close() {
      this.showModal = false; this.error = ''; this.success = ''
      this.form = { email: '', firstName: '', lastName: '', organization: '' }
    },
    async create() {
      this.error = ''; this.success = ''; this.loading = true
      try {
        const res = await authService.createCaUser(this.form)
        this.success = res.message || 'CA korisnik kreiran.'
        await this.load()
        this.form = { email: '', firstName: '', lastName: '', organization: '' }
      } catch (e) {
        this.error = e.response?.data?.error || 'Greška pri kreiranju'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.page { padding: 2rem; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #eee; }
th { background: #f8f8f8; font-weight: 600; }
.active { color: #27ae60; }
.pending { color: #e67e22; }
button { padding: 0.5rem 1rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; }
.modal { background: white; padding: 2rem; border-radius: 8px; min-width: 420px; }
.row { display: flex; gap: 0.75rem; }
.row .form-group { flex: 1; }
.form-group { margin-bottom: 0.8rem; }
label { display: block; margin-bottom: 0.3rem; font-weight: 500; }
input { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
.hint { font-size: 0.8rem; color: #888; }
.error { color: #e74c3c; font-size: 0.85rem; }
.success { color: #27ae60; font-size: 0.85rem; }
.modal-actions { display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem; }
</style>
