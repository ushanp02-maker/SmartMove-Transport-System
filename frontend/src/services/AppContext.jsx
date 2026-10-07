import { useEffect, useMemo, useState } from 'react'
import { initialData } from '../data/mockData'
import { DataContext } from './DataContext'

const STORAGE_KEY = 'smartmove-admin-demo-v1'

export function DataProvider({ children }) {
  const [data, setData] = useState(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      return saved ? { ...initialData, ...JSON.parse(saved) } : initialData
    } catch {
      return initialData
    }
  })

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  }, [data])

  const api = useMemo(() => ({
    data,
    addRecord: (key, record) => setData(current => ({ ...current, [key]: [{ ...record, id: record.id || `${key.slice(0, 2).toUpperCase()}-${Date.now().toString().slice(-5)}` }, ...current[key]] })),
    updateRecord: (key, id, changes) => setData(current => ({ ...current, [key]: current[key].map(record => record.id === id ? { ...record, ...changes } : record) })),
    deleteRecord: (key, id) => setData(current => ({ ...current, [key]: current[key].filter(record => record.id !== id) })),
  }), [data])
  return <DataContext.Provider value={api}>{children}</DataContext.Provider>
}
