const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'
let authorization = ''

export function setApiCredentials(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  authorization = `Basic ${btoa(String.fromCharCode(...bytes))}`
}

export function clearApiCredentials() {
  authorization = ''
}

export function registerAccount(username, password) {
  return request('/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  }, false)
}

async function request(path, options, includeAuthorization = true) {
  const headers = new Headers(options?.headers || {})
  if (includeAuthorization && authorization) headers.set('Authorization', authorization)
  const response = await fetch(`${API_BASE}${path}`, { credentials: 'same-origin', ...options, headers })
  const payload = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(payload?.message || `Request failed (${response.status})`)
  }
  return payload
}

export function uploadStatement(file) {
  const body = new FormData()
  body.append('file', file)
  return request('/statements', { method: 'POST', body })
}

export function getSummary(range) {
  return request(`/analytics/summary?${range}`)
}

export function getTrends(range) {
  return request(`/analytics/trends?${range}`)
}

export function getCategories(range) {
  return request(`/analytics/categories?${range}`)
}

export function getTransactions({ range, category, direction, q, page, size }) {
  const params = new URLSearchParams(range)
  if (category) params.set('category', category)
  if (direction) params.set('direction', direction)
  if (q) params.set('q', q)
  params.set('page', String(page))
  params.set('size', String(size))
  return request(`/transactions?${params}`)
}

export function updateCategory(id, category) {
  return request(`/transactions/${id}/category`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ category }),
  })
}

