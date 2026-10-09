import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
const columns = ['id','title','rating','comment','category','status','createdAt']
export default function ApiFeedbackAdmin() {
  const [items,setItems] = useState([])
  const [error,setError] = useState('')
  const [reload,setReload] = useState(0)
  const [busy,setBusy] = useState(false)
  useEffect(() => { let active = true; request('/feedback?page=0&size=100').then(data => {if(active) {setItems(Array.isArray(data) ? data : (data.content || []));setError('')}}).catch(err => {if(active)setError(err.message)});return () => {active=false} },[reload])
  const moderate = async (item,status) => {
    const moderatorUserId = window.prompt('Your Oracle admin user ID')
    if (!moderatorUserId) return
    if (!Number.isSafeInteger(Number(moderatorUserId)) || Number(moderatorUserId) < 1) return setError('Invalid moderator user ID')
    const moderationNotes = window.prompt('Moderation notes (optional)', '')
    if (moderationNotes === null) return
    setBusy(true);setError('')
    try { await request(`/feedback/${encodeURIComponent(item.id)}/moderate`,{method:'PATCH',body:JSON.stringify({status,moderatorUserId:Number(moderatorUserId),moderationNotes})});setReload(n=>n+1) } catch(err){setError(err.message)} finally{setBusy(false)}
  }
  return <section className="portal-panel"><div className="portal-panel-head"><div><h1>Reviews and moderation</h1><p>Reviews loaded from the MongoDB-backed feedback API.</p></div><button className="button button-outline" onClick={() => setReload(n=>n+1)}>Refresh</button></div>{error && <p className="auth-error" role="alert">{error}</p>}<div style={{overflowX:'auto'}}><table className="data-table"><thead><tr>{columns.map(x=><th key={x}>{x}</th>)}<th>Moderate</th></tr></thead><tbody>{items.map(item=><tr key={item.id}>{columns.map(key=><td key={key}>{String(item[key]??'—')}</td>)}<td>{['APPROVED','REJECTED'].map(status=><button key={status} disabled={busy} className="button button-outline" onClick={()=>moderate(item,status)}>{status}</button>)}</td></tr>)}</tbody></table></div>{!items.length&&<p>No reviews found.</p>}</section>
}
