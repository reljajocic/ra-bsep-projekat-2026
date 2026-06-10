<template>
  <div class="page">
    <h2>Bezbednost naloga</h2>

    <div class="card">
      <h3>Dvofaktorska autentifikacija (2FA)</h3>
      <p class="desc">
        Dodatni nivo zaštite. Pri prijavi ćeš pored lozinke morati da uneseš i
        6-cifreni kod iz authenticator aplikacije (Google Authenticator, Authy i slično).
      </p>

      <!-- Vec je ukljucen -->
      <div v-if="alreadyEnabled" class="status-on">
        2FA je uključen na ovom nalogu.
      </div>

      <!-- Nije ukljucen, dugme za pokretanje -->
      <button v-if="!alreadyEnabled && !qrCode && !confirmed" @click="startSetup" :disabled="loading">
        {{ loading ? 'Učitavanje...' : 'Uključi 2FA' }}
      </button>

      <!-- Prikaz QR koda i unos prvog koda -->
      <div v-if="qrCode && !confirmed" class="setup">
        <p>1. Skeniraj ovaj QR kod u svojoj authenticator aplikaciji:</p>
        <img :src="qrCode" alt="QR kod" class="qr" />
        <p>2. Unesi kod koji se prikazuje u aplikaciji:</p>
        <input v-model="code" type="text" placeholder="6-cifreni kod" maxlength="6" />
        <p v-if="error" class="error">{{ error }}</p>
        <button @click="confirm" :disabled="loading">Potvrdi i aktiviraj</button>
      </div>

      <!-- Uspeh -->
      <div v-if="confirmed" class="status-on">
        2FA je uspešno aktiviran. Pri sledećoj prijavi tražiće se kod.
      </div>
    </div>
  </div>
</template>

<script>
import { authService } from '../services/authService'

export default {
  data() {
    return {
      alreadyEnabled: false,
      qrCode: '',
      code: '',
      confirmed: false,
      loading: false,
      error: ''
    }
  },
  async mounted() {
    try {
      this.alreadyEnabled = await authService.getTotpStatus()
    } catch (e) {
      this.alreadyEnabled = false
    }
  },
  methods: {
    async startSetup() {
      this.loading = true
      this.error = ''
      try {
        this.qrCode = await authService.setupTotp()
      } catch (e) {
        this.error = 'Greška pri pokretanju 2FA'
      } finally {
        this.loading = false
      }
    },
    async confirm() {
      this.loading = true
      this.error = ''
      try {
        await authService.confirmTotp(this.code)
        this.confirmed = true
        this.qrCode = ''
      } catch (e) {
        this.error = 'Pogrešan kod, pokušaj ponovo'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.page { padding: 2rem; max-width: 600px; }
.card { background: white; padding: 1.5rem; border: 1px solid #eee; border-radius: 8px; margin-top: 1rem; }
h3 { margin-bottom: 0.5rem; }
.desc { color: #666; margin-bottom: 1rem; font-size: 0.9rem; }
.setup { margin-top: 1rem; }
.setup p { margin: 0.8rem 0 0.4rem; }
.qr { display: block; width: 200px; height: 200px; margin: 0.5rem 0; }
input { padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; width: 200px; }
button { padding: 0.6rem 1.2rem; background: #2c3e50; color: white; border: none; border-radius: 4px; cursor: pointer; display: block; margin-top: 1rem; }
button:disabled { opacity: 0.6; }
.error { color: red; font-size: 0.9rem; }
.status-on { background: #eafaf1; color: #1e8449; padding: 1rem; border-radius: 4px; margin-top: 1rem; font-weight: 500; }
</style>