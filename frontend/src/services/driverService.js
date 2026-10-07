import { request, isApiMode } from './apiClient'

export const listAssignedTrips = () => isApiMode() ? request('/drivers/me/trips') : Promise.resolve({mode:'DEMO',items:[]})
export const getAssignedVehicle = () => isApiMode() ? request('/drivers/me/vehicle') : Promise.resolve({mode:'DEMO',item:null})
export const updateAssignedTripStatus = (id,status) => isApiMode() ? request(`/driver/trips/${encodeURIComponent(id)}/status`,{method:'PATCH',body:JSON.stringify({status})}) : Promise.resolve({mode:'DEMO',status})
