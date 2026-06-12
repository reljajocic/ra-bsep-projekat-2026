import api from './api'

export const templateService = {
  async getAll() {
    const response = await api.get('/templates')
    return response.data
  },

  async create(data) {
    const response = await api.post('/templates', data)
    return response.data
  },

  async delete(id) {
    await api.delete(`/templates/${id}`)
  }
}
