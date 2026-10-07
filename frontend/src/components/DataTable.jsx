import { useMemo, useState } from 'react'
import { Search, Pencil, Trash2, SlidersHorizontal } from 'lucide-react'
import { resolveValue } from '../services/formatters'

export default function DataTable({ data = [], columns = [], collections = {}, onEdit, onDelete, searchPlaceholder = 'Search records...' }) {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('All status')
  const statuses = [...new Set(data.map(item => item.status).filter(Boolean))]
  const filtered = useMemo(() => data.filter(record => {
    const searchMatches = Object.values(record).some(value => String(value).toLowerCase().includes(query.toLowerCase()))
    return searchMatches && (status === 'All status' || record.status === status)
  }), [data, query, status])

  return <section className="table-card">
    <div className="table-toolbar">
      <label className="search-box"><Search size={17} /><input value={query} onChange={event => setQuery(event.target.value)} placeholder={searchPlaceholder} aria-label={searchPlaceholder} /></label>
      <label className="filter-box"><SlidersHorizontal size={15} /><select value={status} onChange={event => setStatus(event.target.value)} aria-label="Filter by status"><option>All status</option>{statuses.map(item => <option key={item}>{item}</option>)}</select></label>
      <span className="record-count">{filtered.length} record{filtered.length === 1 ? '' : 's'}</span>
    </div>
    <div className="table-scroll"><table><thead><tr>{columns.map(([key, label]) => <th key={key}>{label}</th>)}{(onEdit || onDelete) && <th className="actions-heading">Actions</th>}</tr></thead>
      <tbody>{filtered.map(record => <tr key={record.id}>{columns.map(([key]) => <td key={key}>{key === 'status' ? <span className={`status-badge status-${String(record[key]).toLowerCase().replace(/[^a-z]+/g, '-')}`}>{record[key]}</span> : <span className={key === 'id' ? 'table-id' : ''}>{resolveValue(record, key, collections)}</span>}</td>)}{(onEdit || onDelete) && <td><div className="row-actions">{onEdit && <button type="button" className="icon-button" onClick={() => onEdit(record)} aria-label={`Edit ${record.id}`}><Pencil size={16} /></button>}{onDelete && <button type="button" className="icon-button danger-icon" onClick={() => onDelete(record)} aria-label={`Delete ${record.id}`}><Trash2 size={16} /></button>}</div></td>}</tr>)}
      </tbody></table>
      {filtered.length === 0 && <div className="empty-state"><div className="empty-icon"><Search size={20} /></div><strong>{query || status !== 'All status' ? 'No matching records' : 'Nothing here yet'}</strong><p>{query || status !== 'All status' ? 'Try another search or change the status filter.' : 'New records will appear here once added.'}</p></div>}
    </div>
    <div className="table-footer">Showing <strong>{filtered.length}</strong> of <strong>{data.length}</strong> records <span>Demo data · saved in this browser</span></div>
  </section>
}
