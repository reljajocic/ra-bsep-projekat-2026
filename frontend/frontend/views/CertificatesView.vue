<template>
  <div class="page">
    <div class="header">
      <h2>Sertifikati</h2>
      <button @click="$router.push('/certificates/new')">+ Novi sertifikat</button>
    </div>

    <table v-if="certificates.length">
      <thead>
        <tr>
          <th>Tip</th>
          <th>Subject</th>
          <th>Issuer</th>
          <th>Važi do</th>
          <th>Status</th>
          <th>Akcije</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="cert in certificates" :key="cert.id" :class="{ revoked: cert.revoked }">
          <td><span class="badge" :class="cert.type.toLowerCase()">{{ cert.type }}</span></td>
          <td>{{ cert.subjectDN }}</td>
          <td>{{ cert.issuerDN }}</td>
          <td>{{ formatDate(cert.validUntil) }}</td>
          <td>{{ cert.revoked ? 'Povučen' : 'Aktivan' }}</td>
          <td>
            <button @click="download(cert)" class="btn-small">Preuzmi</button>
            <button v-if="!cert.revoked" @click="openRevoke(cert)" class="btn-small danger">Povuci</button>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-else>Nema sertifikata.</p>

    <!-- Modal za revokaciju -->
    <div v-if="revokeModal" class="modal-overlay">
      <div class="modal">
        <h3>Povlačenje sertifikata</h3>
        <p>{{ selectedCert?.subjectDN }}</p>
        <select v-model="revokeReason">
          <option value="UNSPECIFIED">Nespecifikovano</option>
          <option value="KEY_COMPROMISE">Kompromitovani ključ</option>
          <option value="CA_COMPROMISE">Kompromitovani CA</option>
          <option value="AFFILIATION_CHANGED">Promenjena afilijacija</option>
          <option value="SUPERSEDED">Zamenjen</option>
          <option value="CESSATION_OF_OPERATION">Prestanak rada</option>
          <option value="CERTIFICATE_HOLD">Privremeno zadržan</option>
          <option value="PRIVILEGE_WITHDRAWN">Povučene privilegije</option>
        </select>
        <div class="modal-actions">
          <button @click="revokeModal = false">Otkaži</button>
          <button @click="confirmRevoke" class="danger">Potvrdi povlačenje</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { certificateService } from '../services/certificateService'

export default {
  data() {
    return {
      certificates: [],
      revokeModal: false,
      selectedCert: null,
      revokeReason: 'UNSPECIFIED'
    }
  },
  async mounted() {
    await this.load()
  },
  methods: {
    async load() {
      this.certificates = await certificateService.getAll()
    },
    formatDate(date) {
      return new Date(date).toLocaleDateString('sr-RS')
    },
    download(cert) {
      certificateService.downloadPem(cert)
    },
    openRevoke(cert) {
      this.selectedCert = cert
      this.revokeReason = 'UNSPECIFIED'
      this.revokeModal = true
    },
    async confirmRevoke() {
      await certificateService.revoke(this.selectedCert.id, this.revokeReason)
      this.revokeModal = false
      await this.load()
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
tr.revoked td { color: #999; text-decoration: line-through; }
.badge { padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.8rem; font-weight: 600; }
.badge.root { background: #e8f4fd; color: #2980b9; }
.badge.intermediate { background: #fef9e7; color: #d68910; }
.badge.end_entity { background: #eafaf1; color: #1e8449; }
button { padding: 0.5rem 1rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; }
.btn-small { padding: 0.3rem 0.6rem; font-size: 0.85rem; margin-right: 0.3rem; }
.danger { background: #e74c3c; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; }
.modal { background: white; padding: 2rem; border-radius: 8px; min-width: 400px; }
.modal select { width: 100%; padding: 0.5rem; margin: 1rem 0; }
.modal-actions { display: flex; gap: 0.5rem; justify-content: flex-end; }
</style>