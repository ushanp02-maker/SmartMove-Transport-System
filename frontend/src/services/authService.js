import { request, isApiMode } from './apiClient'

export const login = credentials =>
  isApiMode()
    ? request('/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
      })
    : Promise.resolve({
        mode: 'DEMO',
        message: 'Use AppContext demo account lookup.',
      })

export const registerPassenger = profile =>
  isApiMode()
    ? request('/auth/register', {
        method: 'POST',
        body: JSON.stringify(profile),
      })
    : Promise.resolve({
        mode: 'DEMO',
        message: 'Use AppContext demo passenger registration.',
      })

export const logout = () => isApiMode() ? request('/auth/logout', { method: 'POST' }) : Promise.resolve(null)

export const restoreSession = () => request('/auth/session')

export const getCurrentAccount = () =>
  isApiMode()
    ? request('/users/me')
    : Promise.resolve({ mode: 'DEMO', account: null })
