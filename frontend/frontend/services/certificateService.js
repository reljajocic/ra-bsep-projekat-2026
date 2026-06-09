import api from './api'

export const certificateService = {
  async getAll() {
    const response = await api.get('/certificates')
    return response.data
  },

  async getById(id) {
    const response = await api.get(`/certificates/${id}`)
    return response.data
  },

  async issue(data) {
    const response = await api.post('/certificates', data)
    return response.data
  },

  async revoke(id, reason) {
    await api.post(`/certificates/${id}/revoke`, { reason })
  },

  downloadPem(cert) {
    const blob = new Blob([cert.certificatePem], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `certificate_${cert.serialNumber}.pem`
    a.click()
    URL.revokeObjectURL(url)
  }
}