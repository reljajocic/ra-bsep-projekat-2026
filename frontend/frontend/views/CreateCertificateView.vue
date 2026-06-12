<template>
  <div class="page">
    <h2>Novi sertifikat</h2>

    <form @submit.prevent="submit">
      <div class="section">
        <h3>Tip sertifikata</h3>
        <select v-model="form.certificateType" @change="onTypeChange" :disabled="isUser">
          <option v-if="isAdmin" value="ROOT">ROOT</option>
          <option v-if="isAdmin || isCaUser" value="INTERMEDIATE">INTERMEDIATE</option>
          <option value="END_ENTITY">END_ENTITY</option>
        </select>
        <p v-if="isUser" class="hint">Kao običan korisnik možeš da zatražiš samo End-Entity sertifikat uploadom CSR-a.</p>
        <p v-else-if="isCaUser" class="hint">Kao CA korisnik izdaješ sertifikate isključivo u okviru svoje organizacije (svog lanca).</p>
      </div>

      <div class="section" v-if="form.certificateType !== 'ROOT'">
        <h3>Issuer sertifikat</h3>
        <select v-model="form.issuerCertificateId" @change="onIssuerChange">
          <option value="">Izaberi issuer...</option>
          <option v-for="cert in availableIssuers" :key="cert.id" :value="cert.id">
            {{ cert.subjectDN }} ({{ cert.type }})
          </option>
        </select>
      </div>

      <!-- Sablon - popunjava ekstenzije i namece politiku (CN/SAN regex, TTL) -->
      <div class="section" v-if="form.certificateType !== 'ROOT' && matchingTemplates.length">
        <h3>Šablon (opciono)</h3>
        <select v-model="form.templateId" @change="applyTemplate">
          <option value="">Bez šablona</option>
          <option v-for="t in matchingTemplates" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
        <p v-if="selectedTemplate" class="hint">
          <span v-if="selectedTemplate.cnRegex">CN mora da odgovara: <code>{{ selectedTemplate.cnRegex }}</code>. </span>
          <span v-if="selectedTemplate.sanRegex">SAN mora da odgovara: <code>{{ selectedTemplate.sanRegex }}</code>. </span>
          <span v-if="selectedTemplate.ttlDays">Max trajanje: {{ selectedTemplate.ttlDays }} dana.</span>
        </p>
      </div>

      <!-- CSR upload za END_ENTITY -->
      <div class="section" v-if="form.certificateType === 'END_ENTITY'">
        <h3>CSR {{ isAdmin ? '(opciono)' : '(obavezno)' }}</h3>
        <p class="hint">Ako uploaduješ CSR, subject podaci se preuzimaju iz njega.</p>
        <input type="file" accept=".pem,.csr,.txt" @change="loadCsrFile" />
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
        <div class="form-group">
          <label>Subject Alternative Names (SAN)</label>
          <input v-model="form.subjectAltNames" placeholder="npr. www.ftn.com,api.ftn.com,10.0.0.1" />
          <p class="hint">Razdvoji zarezom. Email → rfc822, brojevi/tačke → IP, ostalo → DNS.</p>
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
import { templateService } from '../services/templateService'
import { authService } from '../services/authService'

export default {
  data() {
    return {
      form: {
        certificateType: 'ROOT',
        issuerCertificateId: '',
        templateId: '',
        subjectCN: '',
        subjectO: '',
        subjectOU: '',
        subjectC: '',
        validityDays: 365,
        keyUsage: 'keyCertSign,cRLSign',
        extendedKeyUsage: '',
        subjectAltNames: '',
        csrPem: ''
      },
      allCertificates: [],
      templates: [],
      loading: false,
      error: '',
      success: false
    }
  },
  computed: {
    isAdmin() {
      return authService.isAdmin()
    },
    isCaUser() {
      return authService.getRole() === 'CA_USER'
    },
    isUser() {
      return authService.getRole() === 'USER'
    },
    availableIssuers() {
      return this.allCertificates
    },
    matchingTemplates() {
      if (!this.form.issuerCertificateId) return []
      return this.templates.filter(t => String(t.issuerCertificateId) === String(this.form.issuerCertificateId))
    },
    selectedTemplate() {
      return this.templates.find(t => String(t.id) === String(this.form.templateId)) || null
    }
  },
  async mounted() {
    // Dostupni CA sertifikati za odabir issuera (i USER mora da izabere CA za potpis)
    this.allCertificates = await certificateService.getIssuers()
    // Sabloni su dostupni samo CA korisniku i adminu
    if (this.isAdmin || this.isCaUser) {
      try { this.templates = await templateService.getAll() } catch (e) { this.templates = [] }
    }
    if (this.isUser) {
      this.form.certificateType = 'END_ENTITY'
      this.onTypeChange()
    } else if (this.isCaUser) {
      this.form.certificateType = 'INTERMEDIATE'
      this.onTypeChange()
    }
  },
  methods: {
    onIssuerChange() {
      // Promenom issuera se resetuje izbor sablona (sabloni su vezani za issuer-a)
      this.form.templateId = ''
    },
    applyTemplate() {
      const t = this.selectedTemplate
      if (!t) return
      if (t.keyUsage) this.form.keyUsage = t.keyUsage
      if (t.extendedKeyUsage) this.form.extendedKeyUsage = t.extendedKeyUsage
      if (t.ttlDays && this.form.validityDays > t.ttlDays) this.form.validityDays = t.ttlDays
    },
    loadCsrFile(event) {
      const file = event.target.files[0]
      if (!file) return
      const reader = new FileReader()
      reader.onload = (e) => { this.form.csrPem = e.target.result }
      reader.readAsText(file)
    },
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
          issuerCertificateId: this.form.issuerCertificateId || null,
          templateId: this.form.templateId || null
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