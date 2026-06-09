import api from './api'

export const passwordService = {
  async getAll() {
    const response = await api.get('/passwords')
    return response.data
  },

  async create(siteName, username, encryptedPassword) {
    const response = await api.post('/passwords', { siteName, username, encryptedPassword })
    return response.data
  },

  async delete(id) {
    await api.delete(`/passwords/${id}`)
  },

  // Generiše RSA par ključeva u browseru
  async generateKeyPair() {
    const keyPair = await window.crypto.subtle.generateKey(
      {
        name: 'RSA-OAEP',
        modulusLength: 2048,
        publicExponent: new Uint8Array([1, 0, 1]),
        hash: 'SHA-256'
      },
      true,
      ['encrypt', 'decrypt']
    )

    const publicKeyExported = await window.crypto.subtle.exportKey('spki', keyPair.publicKey)
    const privateKeyExported = await window.crypto.subtle.exportKey('pkcs8', keyPair.privateKey)

    return {
      publicKeyPem: exportToPem(publicKeyExported, 'PUBLIC KEY'),
      privateKeyPem: exportToPem(privateKeyExported, 'PRIVATE KEY'),
      publicKey: keyPair.publicKey,
      privateKey: keyPair.privateKey
    }
  },

  // Enkriptuje lozinku javnim ključem
  async encrypt(plaintext, publicKeyPem) {
    const publicKey = await importPublicKey(publicKeyPem)
    const encoded = new TextEncoder().encode(plaintext)
    const encrypted = await window.crypto.subtle.encrypt(
      { name: 'RSA-OAEP' },
      publicKey,
      encoded
    )
    return btoa(String.fromCharCode(...new Uint8Array(encrypted)))
  },

  // Dekriptuje lozinku privatnim ključem (radi se lokalno, privatni ključ nikad ne ide na server)
  async decrypt(encryptedBase64, privateKeyPem) {
    const privateKey = await importPrivateKey(privateKeyPem)
    const encryptedBytes = Uint8Array.from(atob(encryptedBase64), c => c.charCodeAt(0))
    const decrypted = await window.crypto.subtle.decrypt(
      { name: 'RSA-OAEP' },
      privateKey,
      encryptedBytes
    )
    return new TextDecoder().decode(decrypted)
  }
}

function exportToPem(buffer, type) {
  const base64 = btoa(String.fromCharCode(...new Uint8Array(buffer)))
  const lines = base64.match(/.{1,64}/g).join('\n')
  return `-----BEGIN ${type}-----\n${lines}\n-----END ${type}-----`
}

async function importPublicKey(pem) {
  const base64 = pem.replace(/-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----|\n/g, '')
  const buffer = Uint8Array.from(atob(base64), c => c.charCodeAt(0))
  return window.crypto.subtle.importKey(
    'spki',
    buffer,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt']
  )
}

async function importPrivateKey(pem) {
  const base64 = pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\n/g, '')
  const buffer = Uint8Array.from(atob(base64), c => c.charCodeAt(0))
  return window.crypto.subtle.importKey(
    'pkcs8',
    buffer,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['decrypt']
  )
}