import { useEffect, useMemo, useState } from 'react'
import { initialData } from '../data/mockData'
import { DataContext } from './DataContext'

const STORAGE_KEY = 'smartmove-admin-demo-v1'
const SESSION_KEY = 'smartmove-demo-session-v1'
const DATA_VERSION_KEY = 'smartmove-demo-data-version'

export function DataProvider({ children }) {
  const [data, setData] = useState(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (!saved) return initialData
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
      return merged
    } catch {
      return initialData
    }
  })
  const [session, setSession] = useState(() => {
    try { return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null') } catch { return null }
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
    session,
    signIn: (role, userId) => setSession({ role, userId }),
    signOut: () => setSession(null),
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
