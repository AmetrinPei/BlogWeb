import http from './http'

/** GET /api/me */
export function getMe() {
  return http.get('/me')
}

/** PUT /api/me — body: { displayName, bio, avatarUrl } */
export function updateMe(payload) {
  return http.put('/me', payload)
}
