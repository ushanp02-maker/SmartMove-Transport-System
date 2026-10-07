export const formatLkr = value => `LKR ${Number(value || 0).toLocaleString('en-LK')}`
export const formatDate = value => value ? new Date(`${value}T00:00:00`).toLocaleDateString('en-LK', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'

export function resolveValue(record, key, data) {
  const relations = { routeId: ['routes', 'name'], vehicleId: ['vehicles', 'name'], driverId: ['drivers', 'name'], passengerId: ['passengers', 'name'], tripId: ['trips', 'id'], bookingId: ['bookings', 'id'] }
  if (key === 'fare' || key === 'amount' || key === 'cost') return formatLkr(record[key])
  if (key === 'mileage') return `${Number(record[key] || 0).toLocaleString()} km`
  if (key === 'rating') return `${record[key]} ★`
  if (relations[key]) {
    const [collection, label] = relations[key]
    const referenced = data[collection]?.find(item => item.id === record[key])
    return referenced ? referenced[label] : record[key] || '—'
  }
  return record[key] ?? '—'
}
