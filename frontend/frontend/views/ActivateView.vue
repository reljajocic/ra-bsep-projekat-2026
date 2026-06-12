<template>
  <div class="activate-container">
    <div class="activate-box">
      <h2>Aktivacija naloga</h2>
      <p v-if="loading">Aktivacija u toku...</p>
      <p v-else-if="success" class="success">{{ message }}</p>
      <p v-else class="error">{{ message }}</p>
      <router-link v-if="!loading" to="/login" class="btn">Idi na prijavu</router-link>
    </div>
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  data() {
    return { loading: true, success: false, message: '' }
  },
  async mounted() {
    const token = this.$route.query.token
    if (!token) {
      this.loading = false
      this.message = 'Nedostaje aktivacioni token.'
      return
    }
    try {
      const res = await authService.activate(token)
      this.success = true
      this.message = res.message || 'Nalog je uspešno aktiviran.'
    } catch (e) {
      this.message = e.response?.data?.error || 'Aktivacija nije uspela.'
    } finally {
      this.loading = false
    }
  }
}
</script>

<style scoped>
.activate-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f5f5; }
.activate-box { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 400px; text-align: center; }
h2 { margin-bottom: 1rem; }
.success { color: #27ae60; }
.error { color: #e74c3c; }
.btn { display: inline-block; margin-top: 1rem; padding: 0.6rem 1.2rem; background: #2c3e50; color: white; border-radius: 4px; text-decoration: none; }
</style>
