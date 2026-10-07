import { useEffect, useMemo, useState } from 'react'
import { initialData } from '../data/mockData'
import { DataContext } from './DataContext'
import { dataMode } from './apiClient'

const STORAGE_KEY = 'smartmove-admin-demo-v1'
const SESSION_KEY = 'smartmove-demo-session-v1'
const DATA_VERSION_KEY = 'smartmove-demo-data-version'

function normalizeAccounts(data) {
  const users = [...(data.users || [])]
  const appendLegacy = (profiles, role, prefix, makeUsername) => {
    for (const profile of profiles || []) {
      const linked = users.some(user => user.linkedProfileId === profile.id && user.role === role)
      if (linked) continue
      let username = String(profile.username || makeUsername(profile)).toLowerCase()
      const email = String(profile.email || `${username}@${role.toLowerCase()}.smartmove.demo`).toLowerCase()
      if (users.some(user => user.email?.toLowerCase() === email)) continue
      if (!profile.username) {
        const base = username
        let suffix = 2
        while (users.some(user => user.username?.toLowerCase() === username)) username = `${base}${suffix++}`
      } else if (users.some(user => user.username?.toLowerCase() === username)) continue
      users.push({ userId: `${prefix}-${profile.id}`, username, email, role, accountStatus: profile.accountStatus === 'DISABLED' ? 'DISABLED' : 'ACTIVE', linkedProfileId: profile.id })
    }
  }
  appendLegacy(data.passengers, 'PASSENGER', 'USR-PASS', profile => profile.username || profile.email?.split('@')[0] || profile.id.toLowerCase())
  appendLegacy(data.drivers, 'DRIVER', 'USR-DRIVER', profile => profile.username || profile.id.toLowerCase())
  appendLegacy(data.admins, 'ADMIN', 'USR-ADMIN', profile => profile.username || profile.email?.split('@')[0] || profile.id.toLowerCase())
  const superAdmin = users.find(user => user.role === 'SUPER_ADMIN')
  if (!superAdmin) users.unshift({ userId: 'USR-ADMIN-001', username: 'superadmin', email: 'admin@smartmove.lk', role: 'SUPER_ADMIN', accountStatus: 'ACTIVE', linkedProfileId: 'ADM-001' })
  const merged = { ...data, users }
  const appendMissing = (collection, seeds) => {
    const rows = [...(merged[collection] || [])]
    for (const seed of seeds || []) if (!rows.some(item => item.id === seed.id)) rows.push(seed)
    merged[collection] = rows
  }
  appendMissing('companies', initialData.companies)
  appendMissing('employees', initialData.employees)
  appendMissing('staffRoutes', initialData.staffRoutes)
  appendMissing('trips', initialData.trips.filter(trip => ['TR-4030', 'TR-4031', 'TR-ST-001', 'TR-ST-002'].includes(trip.id)))
  appendMissing('routes', initialData.routes.filter(route => ['RT-092', 'RT-093'].includes(route.id)))
  merged.trips = merged.trips.map(trip => ({ serviceType: 'COMMUTER', ...trip }))
  merged.routes = merged.routes.map(route => {
    const seed = initialData.routes.find(item => item.id === route.id)
    return { serviceType: 'COMMUTER', ...route, stops: route.stops?.length ? route.stops : seed?.stops || [] }
  })
  merged.staffRoutes = merged.staffRoutes.map(route => {
    const seed = initialData.staffRoutes.find(item => item.id === route.id)
    return { serviceType: 'STAFF', ...seed, ...route, stops: route.stops?.length ? route.stops : seed?.stops || [] }
  })
  merged.staffBookings ||= []
  merged.customTripRequests ||= []
  merged.issueReports ||= []
  return merged
}

export function DataProvider({ children }) {
  const [data, setData] = useState(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (!saved) return normalizeAccounts(initialData)
      const stored = JSON.parse(saved)
      const merged = { ...initialData, ...stored }
      if (Number(localStorage.getItem(DATA_VERSION_KEY) || 0) < 2) {
        for (const collection of ['trips', 'bookings', 'payments', 'announcements']) {
          merged[collection] = (merged[collection] || []).map(record => {
            const seed = initialData[collection].find(item => item.id === record.id)
            const dateKey = collection === 'trips' ? 'date' : collection === 'bookings' ? 'bookedAt' : 'date'
            return seed && record[dateKey]?.startsWith('2025-') ? { ...record, [dateKey]: seed[dateKey] } : record
          })
        }
        for (const [collection, ids] of Object.entries({ routes: ['RT-081'], trips: ['TR-4025', 'TR-4026'], bookings: ['BK-9086'] })) {
          for (const id of ids) {
            const seed = initialData[collection].find(item => item.id === id)
            if (seed && !merged[collection].some(item => item.id === id)) merged[collection].push(seed)
          }
        }
      }
      return normalizeAccounts(merged)
    } catch {
      return normalizeAccounts(initialData)
    }
  })
  const [session, setSession] = useState(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(SESSION_KEY) || 'null')
      if (!saved) return null
      const storedData = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
      const accounts = normalizeAccounts({ ...initialData, ...storedData })
      if (saved.accountId && accounts.users.some(user => user.userId === saved.accountId)) return saved
      if (saved.userId && accounts.users.some(user => user.linkedProfileId === saved.userId)) {
        const account = accounts.users.find(user => user.linkedProfileId === saved.userId)
        return { userId: saved.userId, accountId: account.userId }
      }
      const legacyProfileId = saved.profileId || saved.userId
      const legacyRole = saved.role === 'admin' ? 'ADMIN' : saved.role?.toUpperCase()
      const user = accounts.users.find(item => item.linkedProfileId === legacyProfileId && (item.role === legacyRole || (legacyRole === 'ADMIN' && item.role === 'SUPER_ADMIN')))
      return user ? { userId: user.linkedProfileId, accountId: user.userId } : null
    } catch { return null }
  })

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    localStorage.setItem(DATA_VERSION_KEY, '2')
  }, [data])
  useEffect(() => {
    if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    else localStorage.removeItem(SESSION_KEY)
  }, [session])

  const api = useMemo(() => ({
    data,
    dataMode,
    integrationNotice: dataMode === 'API' ? 'API mode is configured, but domain endpoints are not connected; these modules still use demo records.' : 'Demo data is stored in this browser only.',
    session,
    currentUser: data.users.find(user => user.userId === session?.accountId) || data.users.find(user => user.linkedProfileId === session?.userId) || null,
    demoAuthenticate: (identity, demoAcknowledged) => {
      if (!demoAcknowledged) return { error: 'Confirm that you are using demo authentication before signing in.' }
      const account = data.users.find(user => user.username?.toLowerCase() === identity.trim().toLowerCase() || user.email?.toLowerCase() === identity.trim().toLowerCase())
      if (!account) return { error: 'No demo account matches that email address or username. Check it or create a passenger account.' }
      if (account.accountStatus !== 'ACTIVE') return { error: 'This account is disabled. Please contact a SmartMove administrator.' }
      setSession({ userId: account.linkedProfileId, accountId: account.userId })
      return { account }
    },
    signOut: () => {
      localStorage.removeItem(SESSION_KEY)
      setSession(null)
    },
    createPassengerAccount: profile => {
      const username = profile.username.trim().toLowerCase()
      const email = profile.email.trim().toLowerCase()
      if (data.users.some(user => user.username?.toLowerCase() === username || user.email?.toLowerCase() === email)) return { error: 'That username or email is already registered.' }
      const profileId = `PS-${crypto.randomUUID().slice(0, 8).toUpperCase()}`
      const userId = `USR-PASS-${crypto.randomUUID().slice(0, 8).toUpperCase()}`
      setData(current => ({ ...current,
        passengers: [{ id: profileId, name: profile.name.trim(), email, username, phone: profile.phone.trim(), city: profile.city || 'Colombo', joined: new Date().toISOString().slice(0, 10), trips: 0 }, ...current.passengers],
        users: [{ userId, username, email, role: 'PASSENGER', accountStatus: 'ACTIVE', linkedProfileId: profileId }, ...current.users],
      }))
      setSession({ userId: profileId, accountId: userId })
      return { userId }
    },
    createProvisionedAccount: ({ role, profile, accountStatus = 'ACTIVE' }) => {
      const username = profile.username.trim().toLowerCase()
      const email = profile.email.trim().toLowerCase()
      if (data.users.some(user => user.username?.toLowerCase() === username || user.email?.toLowerCase() === email)) return { error: 'That username or email is already assigned to an account.' }
      const profileId = `${role === 'DRIVER' ? 'DR' : 'ADM'}-${crypto.randomUUID().slice(0, 8).toUpperCase()}`
      const userId = `USR-${role}-${crypto.randomUUID().slice(0, 8).toUpperCase()}`
      const record = { ...profile, id: profileId, email, username, accountStatus }
      setData(current => ({ ...current,
        [role === 'DRIVER' ? 'drivers' : 'admins']: [record, ...current[role === 'DRIVER' ? 'drivers' : 'admins']],
        users: [{ userId, username, email, role, accountStatus, linkedProfileId: profileId }, ...current.users],
      }))
      return { userId, profileId }
    },
    updateAccountAndProfile: (userId, role, profileChanges, accountChanges = {}) => {
      const duplicate = data.users.find(user => user.userId !== userId && ((accountChanges.email && user.email?.toLowerCase() === accountChanges.email.toLowerCase()) || (accountChanges.username && user.username?.toLowerCase() === accountChanges.username.toLowerCase())))
      if (duplicate) return { error: 'That email or username is already assigned to another account.' }
      setData(current => {
      const account = current.users.find(user => user.userId === userId)
      if (!account || account.role !== role) return current
      const collection = role === 'DRIVER' ? 'drivers' : role === 'PASSENGER' ? 'passengers' : 'admins'
      const profiles = current[collection].map(profile => profile.id === account.linkedProfileId ? { ...profile, ...profileChanges, ...(accountChanges.email ? { email: accountChanges.email } : {}), ...(accountChanges.username ? { username: accountChanges.username } : {}), ...(accountChanges.accountStatus ? { accountStatus: accountChanges.accountStatus } : {}) } : profile)
      const users = current.users.map(user => user.userId === userId ? { ...user, ...accountChanges } : user)
      return { ...current, [collection]: profiles, users }
      })
      return null
    },
    deleteProvisionedDriver: userId => {
      const account = data.users.find(user => user.userId === userId && user.role === 'DRIVER')
      if (!account) return { error: 'Driver account was not found.' }
      const linkedTrips = data.trips.filter(trip => trip.driverId === account.linkedProfileId).length
      const linkedIssues = (data.issueReports || []).filter(issue => issue.driverId === account.linkedProfileId).length
      if (linkedTrips || linkedIssues) return { error: 'This driver has assigned trip or issue history. Disable the account instead of deleting it.' }
      setData(current => ({ ...current, users: current.users.filter(user => user.userId !== userId), drivers: current.drivers.filter(profile => profile.id !== account.linkedProfileId) }))
      return null
    },
    addRecord: (key, record) => {
      const id = record.id || `${key.slice(0, 2).toUpperCase()}-${Date.now().toString().slice(-5)}`
      setData(current => ({ ...current, [key]: [{ ...record, id }, ...current[key]] }))
      return id
    },
    updateRecord: (key, id, changes) => setData(current => ({ ...current, [key]: current[key].map(record => record.id === id ? { ...record, ...changes } : record) })),
    deleteRecord: (key, id) => setData(current => ({ ...current, [key]: current[key].filter(record => record.id !== id) })),
  }), [data, session])
  return <DataContext.Provider value={api}>{children}</DataContext.Provider>
}
