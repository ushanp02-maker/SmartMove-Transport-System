import { request, isApiMode } from './apiClient'

export const listVehicles = params => isApiMode() ? request(`/vehicles?${new URLSearchParams(params||{})}`) : Promise.resolve({mode:'DEMO',items:[]})
export const getVehicle = id => isApiMode() ? request(`/vehicles/${encodeURIComponent(id)}`) : Promise.resolve({mode:'DEMO',item:null})
export const listFleetLocations = params => isApiMode() ? request(`/admin/fleet/locations?${new URLSearchParams(params||{})}`) : Promise.resolve({mode:'DEMO',items:[]})
