<template>
  <div class="page">
    <h2>Aktivne sesije</h2>
    <table v-if="sessions.length">
      <thead>
        <tr>
          <th>IP adresa</th>
          <th>Browser / Uređaj</th>
          <th>Kreirano</th>
          <th>Poslednja aktivnost</th>
          <th>Akcije</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="session in sessions" :key="session.jti">
          <td>{{ session.ipAddress }}</td>
          <td>{{ session.userAgent }}</td>
          <td>{{ formatDate(session.createdAt) }}</td>
          <td>{{ formatDate(session.lastSeen) }}</td>
          <td>
            <button @click="revoke(session.jti)" class="btn-small danger">Opozovi</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else>Nema aktivnih sesija.</p>
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  data() {
    return { sessions: [] }
  },
  async mounted() {
    await this.load()
  },
  methods: {
    async load() {
      this.sessions = await authService.getSessions()
    },
    formatDate(date) {
      return new Date(date).toLocaleString('sr-RS')
    },
    async revoke(jti) {
      if (confirm('Opozvati ovu sesiju?')) {
        await authService.revokeSession(jti)
        await this.load()
      }
    }
  }
}
</script>

<style scoped>
.page { padding: 2rem; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #eee; }
th { background: #f8f8f8; font-weight: 600; }
button { padding: 0.5rem 1rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; }
.btn-small { padding: 0.3rem 0.6rem; font-size: 0.85rem; }
.danger { background: #e74c3c; }
</style>