<template>
  <div class="login-container">
    <div class="login-box">
      <h2>BSEP Admin</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>Email</label>
          <input v-model="email" type="email" placeholder="admin@bsep.com" required />
        </div>
        <div class="form-group">
          <label>Lozinka</label>
          <input v-model="password" type="password" placeholder="Lozinka" required />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? 'Prijava...' : 'Prijavi se' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  data() {
    return {
      email: '',
      password: '',
      error: '',
      loading: false
    }
  },
  methods: {
    async handleLogin() {
      this.error = ''
      this.loading = true
      try {
        await authService.login(this.email, this.password)
        this.$router.push('/certificates')
      } catch (e) {
        this.error = 'Pogrešan email ili lozinka'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f5f5;
}
.login-box {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
}
h2 {
  margin-bottom: 1.5rem;
  text-align: center;
}
.form-group {
  margin-bottom: 1rem;
}
label {
  display: block;
  margin-bottom: 0.3rem;
  font-weight: 500;
}
input {
  width: 100%;
  padding: 0.6rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
}
button {
  width: 100%;
  padding: 0.7rem;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 1rem;
}
button:disabled {
  opacity: 0.6;
}
.error {
  color: red;
  font-size: 0.9rem;
}
</style>