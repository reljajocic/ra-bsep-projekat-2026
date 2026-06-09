import api from './api'

export const authService = {
  async login(email, password) {
    const response = await api.post('/auth/login', { email, password })
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
  }
}