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

// The current Spring Boot backend uses stateless HTTP Basic.
// There is no server-side logout endpoint to invoke.
export const logout = () => Promise.resolve(null)

export const getCurrentAccount = () =>
  isApiMode()
    ? request('/users/me')
    : Promise.resolve({ mode: 'DEMO', account: null })
