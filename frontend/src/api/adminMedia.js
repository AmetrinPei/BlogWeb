import http from './http'

/**
 * Upload an image file; returns { url, originalFilename, size, contentType }.
 * Do not set Content-Type — browser must attach multipart boundary.
 */
export function uploadMedia(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/admin/media', form)
}
