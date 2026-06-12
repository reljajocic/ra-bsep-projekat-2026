<template>
  <div class="page">
    <div class="header">
      <h2>Šabloni za sertifikate</h2>
      <button @click="showModal = true">+ Novi šablon</button>
    </div>

    <table v-if="templates.length">
      <thead>
        <tr>
          <th>Naziv</th>
          <th>Issuer (org)</th>
          <th>CN regex</th>
          <th>SAN regex</th>
          <th>TTL (dana)</th>
          <th>Key Usage</th>
          <th>EKU</th>
          <th>Akcije</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="t in templates" :key="t.id">
          <td>{{ t.name }}</td>
          <td>{{ t.organization }}</td>
          <td><code>{{ t.cnRegex || '—' }}</code></td>
          <td><code>{{ t.sanRegex || '—' }}</code></td>
          <td>{{ t.ttlDays || '—' }}</td>
          <td>{{ t.keyUsage || '—' }}</td>
          <td>{{ t.extendedKeyUsage || '—' }}</td>
          <td><button @click="remove(t.id)" class="btn-small danger">Obriši</button></td>
        </tr>
      </tbody>
    </table>
    <p v-else>Nema šablona. Kreiraj prvi da ubrzaš izdavanje sertifikata.</p>

    <div v-if="showModal" class="modal-overlay">
      <div class="modal">
        <h3>Novi šablon</h3>
        <div class="form-group"><label>Naziv šablona *</label><input v-model.trim="form.name" /></div>
        <div class="form-group">
          <label>CA issuer *</label>
          <select v-model="form.issuerCertificateId">
            <option value="">Izaberi CA...</option>
            <option v-for="c in issuers" :key="c.id" :value="c.id">
              {{ c.subjectDN }} ({{ c.type }}) — {{ c.organization }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label>CN regex</label>
          <input v-model.trim="form.cnRegex" placeholder="npr. .*\.ftn\.com" />
        </div>
        <div class="form-group">
          <label>SAN regex</label>
          <input v-model.trim="form.sanRegex" placeholder="npr. .*\.ftn\.com" />
        </div>
        <div class="form-group"><label>TTL (max dana)</label><input v-model.number="form.ttlDays" type="number" min="1" /></div>
        <div class="form-group"><label>Key Usage (default)</label><input v-model.trim="form.keyUsage" placeholder="digitalSignature,keyEncipherment" /></div>
        <div class="form-group"><label>Extended Key Usage (default)</label><input v-model.trim="form.extendedKeyUsage" placeholder="serverAuth,clientAuth" /></div>
        <p v-if="error" class="error">{{ error }}</p>
        <div class="modal-actions">
          <button @click="close">Otkaži</button>
          <button @click="create" :disabled="loading || !form.name || !form.issuerCertificateId">
            {{ loading ? 'Čuvanje...' : 'Sačuvaj' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { templateService } from '../services/templateService'
import { certificateService } from '../services/certificateService'

export default {
  data() {
    return {
      templates: [],
      issuers: [],
      showModal: false,
      form: { name: '', issuerCertificateId: '', cnRegex: '', sanRegex: '', ttlDays: null, keyUsage: '', extendedKeyUsage: '' },
      error: '', loading: false
    }
  },
  async mounted() {
    await this.load()
    this.issuers = await certificateService.getIssuers()
  },
  methods: {
    async load() { this.templates = await templateService.getAll() },
    close() {
      this.showModal = false; this.error = ''
      this.form = { name: '', issuerCertificateId: '', cnRegex: '', sanRegex: '', ttlDays: null, keyUsage: '', extendedKeyUsage: '' }
    },
    async create() {
      this.error = ''; this.loading = true
      try {
        await templateService.create({ ...this.form, issuerCertificateId: this.form.issuerCertificateId || null })
        await this.load()
        this.close()
      } catch (e) {
        this.error = e.response?.data?.message || 'Greška pri kreiranju šablona'
      } finally {
        this.loading = false
      }
    },
    async remove(id) {
      if (confirm('Obrisati šablon?')) {
        await templateService.delete(id)
        await this.load()
      }
    }
  }
}
</script>

<style scoped>
.page { padding: 2rem; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
th, td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; }
th { background: #f8f8f8; font-weight: 600; }
code { background: #f4f4f4; padding: 0.1rem 0.3rem; border-radius: 3px; font-size: 0.85em; }
button { padding: 0.5rem 1rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; }
.btn-small { padding: 0.3rem 0.6rem; font-size: 0.85rem; }
.danger { background: #e74c3c; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; }
.modal { background: white; padding: 2rem; border-radius: 8px; min-width: 460px; max-height: 90vh; overflow-y: auto; }
.form-group { margin-bottom: 0.8rem; }
label { display: block; margin-bottom: 0.3rem; font-weight: 500; }
input, select { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
.error { color: #e74c3c; font-size: 0.85rem; }
.modal-actions { display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem; }
</style>
