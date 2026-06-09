<template>
  <div class="page">
    <div class="header">
      <h2>Password Manager</h2>
      <button @click="showAddModal = true">+ Dodaj lozinku</button>
    </div>

    <!-- Upozorenje ako nema ključeva -->
    <div v-if="!publicKeyPem" class="warning">
      <p>Nemaš generisan RSA par ključeva. Generiši ga da bi mogao da koristiš password manager.</p>
      <button @click="generateKeys">Generiši ključeve</button>
    </div>

    <!-- Dugme za učitavanje privatnog ključa -->
    <div v-if="publicKeyPem && !privateKeyPem" class="info">
      <p>Učitaj privatni ključ da bi mogao da dekriptuješ lozinke.</p>
      <input type="file" @change="loadPrivateKey" accept=".pem,.txt" />
    </div>

    <table v-if="entries.length">
      <thead>
        <tr>
          <th>Sajt</th>
          <th>Korisničko ime</th>
          <th>Lozinka</th>
          <th>Kreirano</th>
          <th>Akcije</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="entry in entries" :key="entry.id">
          <td>{{ entry.siteName }}</td>
          <td>{{ entry.username }}</td>
          <td>
            <span v-if="decrypted[entry.id]">{{ decrypted[entry.id] }}</span>
            <button v-else-if="privateKeyPem" @click="decrypt(entry)" class="btn-small">Prikaži</button>
            <span v-else>••••••••</span>
          </td>
          <td>{{ formatDate(entry.createdAt) }}</td>
          <td>
            <button @click="deleteEntry(entry.id)" class="btn-small danger">Obriši</button>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-else-if="publicKeyPem">Nema sačuvanih lozinki.</p>

    <!-- Modal za dodavanje -->
    <div v-if="showAddModal" class="modal-overlay">
      <div class="modal">
        <h3>Nova lozinka</h3>
        <div class="form-group">
          <label>Naziv sajta</label>
          <input v-model="newEntry.siteName" placeholder="npr. Gmail" />
        </div>
        <div class="form-group">
          <label>Korisničko ime</label>
          <input v-model="newEntry.username" placeholder="email@example.com" />
        </div>
        <div class="form-group">
          <label>Lozinka</label>
          <input v-model="newEntry.password" type="password" placeholder="Lozinka" />
        </div>
        <p v-if="addError" class="error">{{ addError }}</p>
        <div class="modal-actions">
          <button @click="showAddModal = false">Otkaži</button>
          <button @click="addEntry" :disabled="addLoading">
            {{ addLoading ? 'Čuvanje...' : 'Sačuvaj' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { passwordService } from '../services/passwordService'

export default {
  data() {
    return {
      entries: [],
      decrypted: {},
      publicKeyPem: localStorage.getItem('publicKeyPem') || '',
      privateKeyPem: '',
      showAddModal: false,
      newEntry: { siteName: '', username: '', password: '' },
      addError: '',
      addLoading: false
    }
  },
  async mounted() {
    if (this.publicKeyPem) {
      await this.load()
    }
  },
  methods: {
    async load() {
      this.entries = await passwordService.getAll()
    },
    formatDate(date) {
      return new Date(date).toLocaleDateString('sr-RS')
    },
    async generateKeys() {
      const keys = await passwordService.generateKeyPair()
      this.publicKeyPem = keys.publicKeyPem
      localStorage.setItem('publicKeyPem', keys.publicKeyPem)

      // Preuzmi privatni ključ kao fajl
      const blob = new Blob([keys.privateKeyPem], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'private_key.pem'
      a.click()
      URL.revokeObjectURL(url)

      alert('Privatni ključ je preuzet kao private_key.pem. Čuvaj ga na sigurnom mestu!')
      await this.load()
    },
    loadPrivateKey(event) {
      const file = event.target.files[0]
      if (!file) return
      const reader = new FileReader()
      reader.onload = (e) => {
        this.privateKeyPem = e.target.result
      }
      reader.readAsText(file)
    },
    async decrypt(entry) {
      try {
        const plaintext = await passwordService.decrypt(entry.encryptedPassword, this.privateKeyPem)
        this.decrypted = { ...this.decrypted, [entry.id]: plaintext }
      } catch (e) {
        alert('Greška pri dekripciji. Provjeri da li si učitao ispravan privatni ključ.')
      }
    },
    async addEntry() {
      this.addError = ''
      this.addLoading = true
      try {
        const encrypted = await passwordService.encrypt(this.newEntry.password, this.publicKeyPem)
        await passwordService.create(this.newEntry.siteName, this.newEntry.username, encrypted)
        this.showAddModal = false
        this.newEntry = { siteName: '', username: '', password: '' }
        await this.load()
      } catch (e) {
        this.addError = 'Greška pri čuvanju lozinke'
      } finally {
        this.addLoading = false
      }
    },
    async deleteEntry(id) {
      if (confirm('Obrisati ovu lozinku?')) {
        await passwordService.delete(id)
        await this.load()
      }
    }
  }
}
</script>

<style scoped>
.page { padding: 2rem; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.warning { background: #fff3cd; border: 1px solid #ffc107; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; }
.info { background: #d1ecf1; border: 1px solid #bee5eb; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #eee; }
th { background: #f8f8f8; font-weight: 600; }
button { padding: 0.5rem 1rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; }
.btn-small { padding: 0.3rem 0.6rem; font-size: 0.85rem; margin-right: 0.3rem; }
.danger { background: #e74c3c; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; }
.modal { background: white; padding: 2rem; border-radius: 8px; min-width: 400px; }
.form-group { margin-bottom: 1rem; }
label { display: block; margin-bottom: 0.3rem; font-weight: 500; }
input { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem; }
.error { color: red; font-size: 0.9rem; }
</style>