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

  mustChangePassword() {
    return this.getUser()?.mustChangePassword === true
  },

  async changePassword(oldPassword, newPassword) {
    const res = await api.post('/auth/change-password', { oldPassword, newPassword })
    // posle promene vise ne mora da menja
    const user = this.getUser()
    if (user) {
      user.mustChangePassword = false
      localStorage.setItem('user', JSON.stringify(user))
    }
    return res.data
  },

  async createCaUser(data) {
    const res = await api.post('/admin/ca-users', data)
    return res.data
  },

  async getCaUsers() {
    const res = await api.get('/admin/ca-users')
    return res.data
  },

  async register(data) {
    const response = await api.post('/auth/register', data)
    return response.data
  },

  async activate(token) {
    const response = await api.get('/auth/activate', { params: { token } })
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

  getRole() {
    return this.getUser()?.role || null
  },

  isAdmin() {
    return this.getRole() === 'ADMIN'
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