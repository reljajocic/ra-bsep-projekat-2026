<template>
  <div class="page">
    <h2>Novi sertifikat</h2>

    <form @submit.prevent="submit">
      <div class="section">
        <h3>Tip sertifikata</h3>
        <select v-model="form.certificateType" @change="onTypeChange">
          <option value="ROOT">ROOT</option>
          <option value="INTERMEDIATE">INTERMEDIATE</option>
          <option value="END_ENTITY">END_ENTITY</option>
        </select>
      </div>

      <div class="section" v-if="form.certificateType !== 'ROOT'">
        <h3>Issuer sertifikat</h3>
        <select v-model="form.issuerCertificateId">
          <option value="">Izaberi issuer...</option>
          <option v-for="cert in availableIssuers" :key="cert.id" :value="cert.id">
            {{ cert.subjectDN }} ({{ cert.type }})
          </option>
        </select>
      </div>

      <!-- CSR upload za END_ENTITY -->
      <div class="section" v-if="form.certificateType === 'END_ENTITY'">
        <h3>CSR (opciono)</h3>
        <p class="hint">Ako uploaduješ CSR, subject podaci se preuzimaju iz njega.</p>
        <textarea v-model="form.csrPem" placeholder="-----BEGIN CERTIFICATE REQUEST-----&#10;...&#10;-----END CERTIFICATE REQUEST-----" rows="6"></textarea>
      </div>

      <div class="section" v-if="!form.csrPem">
        <h3>Subject podaci</h3>
        <div class="form-row">
          <div class="form-group">
            <label>Common Name (CN) *</label>
            <input v-model="form.subjectCN" required placeholder="npr. My Root CA" />
          </div>
          <div class="form-group">
            <label>Organization (O)</label>
            <input v-model="form.subjectO" placeholder="npr. FTN" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Organizational Unit (OU)</label>
            <input v-model="form.subjectOU" placeholder="npr. IT" />
          </div>
          <div class="form-group">
            <label>Country (C)</label>
            <input v-model="form.subjectC" placeholder="npr. RS" maxlength="2" />
          </div>
        </div>
      </div>

      <div class="section">
        <h3>Period važenja</h3>
        <div class="form-group">
          <label>Broj dana *</label>
          <input v-model.number="form.validityDays" type="number" min="1" required />
        </div>
      </div>

      <div class="section">
        <h3>Ekstenzije</h3>
        <div class="form-group">
          <label>Key Usage</label>
          <input v-model="form.keyUsage" placeholder="npr. digitalSignature,keyEncipherment" />
        </div>
        <div class="form-group">
          <label>Extended Key Usage</label>
          <input v-model="form.extendedKeyUsage" placeholder="npr. serverAuth,clientAuth" />
        </div>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">Sertifikat uspešno kreiran!</p>

      <div class="actions">
        <button type="button" @click="$router.push('/certificates')">Otkaži</button>
        <button type="submit" :disabled="loading">
          {{ loading ? 'Kreiranje...' : 'Kreiraj sertifikat' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script>
import { certificateService } from '../services/certificateService'

export default {
  data() {
    return {
      form: {
        certificateType: 'ROOT',
        issuerCertificateId: '',
        subjectCN: '',
        subjectO: '',
        subjectOU: '',
        subjectC: '',
        validityDays: 365,
        keyUsage: 'keyCertSign,cRLSign',
        extendedKeyUsage: '',
        csrPem: ''
      },
      allCertificates: [],
      loading: false,
      error: '',
      success: false
    }
  },
  computed: {
    availableIssuers() {
      return this.allCertificates.filter(c =>
        !c.revoked && (c.type === 'ROOT' || c.type === 'INTERMEDIATE')
      )
    }
  },
  async mounted() {
    this.allCertificates = await certificateService.getAll()
  },
  methods: {
    onTypeChange() {
      if (this.form.certificateType === 'ROOT') {
        this.form.keyUsage = 'keyCertSign,cRLSign'
        this.form.validityDays = 3650
      } else if (this.form.certificateType === 'INTERMEDIATE') {
        this.form.keyUsage = 'keyCertSign,cRLSign'
        this.form.validityDays = 1825
      } else {
        this.form.keyUsage = 'digitalSignature,keyEncipherment'
        this.form.extendedKeyUsage = 'serverAuth,clientAuth'
        this.form.validityDays = 365
      }
    },
    async submit() {
      this.error = ''
      this.success = false
      this.loading = true
      try {
        const payload = {
          ...this.form,
          issuerCertificateId: this.form.issuerCertificateId || null
        }
        await certificateService.issue(payload)
        this.success = true
        setTimeout(() => this.$router.push('/certificates'), 1500)
      } catch (e) {
        this.error = e.response?.data?.message || 'Greška pri kreiranju sertifikata'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.page { padding: 2rem; max-width: 800px; }
.section { margin-bottom: 1.5rem; padding: 1rem; border: 1px solid #eee; border-radius: 8px; }
h3 { margin-bottom: 1rem; font-size: 1rem; color: #555; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group { margin-bottom: 0.8rem; }
label { display: block; margin-bottom: 0.3rem; font-weight: 500; font-size: 0.9rem; }
input, select, textarea { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; font-family: inherit; }
.hint { font-size: 0.85rem; color: #888; margin-bottom: 0.5rem; }
.actions { display: flex; gap: 1rem; margin-top: 1rem; }
button { padding: 0.6rem 1.2rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; }
button:disabled { opacity: 0.6; }
button[type="button"] { background: #95a5a6; }
.error { color: red; }
.success { color: green; }
</style>