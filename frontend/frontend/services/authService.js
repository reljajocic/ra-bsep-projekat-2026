import api from './api'

export const authService = {
  async login(email, password, totpCode = null) {
    const body = { email, password }
    if (totpCode) body.totpCode = totpCode
    const response = await api.post('/auth/login', body)
    localStorage.setItem('token', response.data.token)
    localStorage.setItem('user', JSON.stringify(response.data))
    return response.data
  },

  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
  },

  getUser() {
    const user = localStorage.getItem('user')
    return user ? JSON.parse(user) : null
  },

  isLoggedIn() {
    return !!localStorage.getItem('token')
  },

  async getSessions() {
    const response = await api.get('/auth/sessions')
    return response.data
  },

  async revokeSession(jti) {
    await api.delete(`/auth/sessions/${jti}`)
  },

  async setupTotp() {
    const response = await api.post('/auth/2fa/setup')
    return response.data.qrCode
  },

  async confirmTotp(code) {
    await api.post('/auth/2fa/confirm', { code })
  },

  async getTotpStatus() {
  const response = await api.get('/auth/2fa/status')
  return response.data.enabled
}
}