/** Shared API entry point. Endpoints are implemented by Spring Boot controllers. */
export { request as apiRequest, isApiMode, dataMode } from './apiClient'

export const apiIntegrationStatus =
  'Authentication connected in API mode; operational modules are still being integrated.'

export const implementedAuthEndpoints = {
  login: '/auth/login',
  register: '/auth/register',
  currentAccount: '/users/me',
  currentRole: '/users/me/role',
  sessionCheck: '/users/me/session',
}
