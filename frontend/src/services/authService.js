import { request, isApiMode } from './apiClient'

export const login = credentials => isApiMode() ? request('/auth/login',{method:'POST',body:JSON.stringify(credentials)}) : Promise.resolve({mode:'DEMO',message:'Use AppContext demo account lookup.'})
export const registerPassenger = profile => isApiMode() ? request('/auth/register',{method:'POST',body:JSON.stringify(profile)}) : Promise.resolve({mode:'DEMO',message:'Passenger demo registration uses shared AppContext.'})
export const logout = () => isApiMode() ? request('/auth/logout',{method:'POST'}) : Promise.resolve({mode:'DEMO'})
export const getCurrentAccount = () => isApiMode() ? request('/auth/me') : Promise.resolve({mode:'DEMO',account:null})
