import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'

export default function ApiDriverIssuesAdmin() {
  const [items,setItems] = useState([])
  const [error,setError] = useState('')
  const [notice,setNotice] = useState('')
  const [refresh,setRefresh] = useState(0)
  const [busyId,setBusyId] = useState(null)

  useEffect(() => {
    let active = true
    request('/driver-issues/admin')
      .then(data => { if(active) setItems(Array.isArray(data) ? data : []) })
      .catch(err => { if(active) setError(err.message) })
    return () => { active = false }
  },[refresh])

  const change = async (issue,status) => {
    setBusyId(issue.id); setError(''); setNotice('')
    try {
      await request('/driver-issues/admin/'+encodeURIComponent(issue.id)+'/status',{
        method:'PATCH',body:JSON.stringify({status})
      })
      setRefresh(n=>n+1)
    } catch(err) { setError(err.message) }
    finally { setBusyId(null) }
  }

  const remove = async issue => {
    if(!window.confirm('Permanently delete this driver issue? This cannot be undone.')) return
    setBusyId(issue.id); setError(''); setNotice('')
    try {
      await request('/driver-issues/admin/'+encodeURIComponent(issue.id),{method:'DELETE'})
      setItems(old=>old.filter(item=>item.id!==issue.id))
      setNotice('Driver issue deleted permanently.')
      setRefresh(n=>n+1)
    } catch(err) { setError(err.message) }
    finally { setBusyId(null) }
  }

  return <section className="portal-panel">
    <div className="portal-panel-head">
      <div><h2>Driver issue reports</h2><p>Review and manage driver-reported vehicle problems stored in MongoDB.</p></div>
      <button type="button" className="button button-outline" onClick={()=>setRefresh(n=>n+1)}>Refresh</button>
    </div>
    {error&&<p className="auth-error" role="alert">{error}</p>}
    {notice&&<p role="status">{notice}</p>}
    {items.length ? items.map(issue=><article key={issue.id} className="portal-panel" style={{marginTop:14}}>
      <div style={{display:'flex',gap:10,alignItems:'center',flexWrap:'wrap',marginBottom:8}}>
        <strong>{issue.priority} priority</strong>
        <span style={{padding:'5px 11px',borderRadius:20,background:issue.status==='RESOLVED'?'#e4f5eb':'#fff1e4',color:issue.status==='RESOLVED'?'#14613f':'#87501e',fontSize:12,fontWeight:700}}>{String(issue.status||'OPEN').replaceAll('_',' ')}</span>
      </div>
      <p>Trip #{issue.tripId} · Vehicle #{issue.vehicleId} · Driver #{issue.driverId}</p>
      <p style={{whiteSpace:'pre-wrap'}}>{issue.description}</p>
      <div style={{display:'flex',gap:8,flexWrap:'wrap',alignItems:'center'}}>
        {['OPEN','IN_PROGRESS','RESOLVED'].filter(status=>status!==issue.status).map(status=>
          <button type="button" key={status} disabled={busyId!==null} className="button button-outline" onClick={()=>change(issue,status)}>
            {status==='IN_PROGRESS'?'Start progress':status==='RESOLVED'?'Resolve':'Reopen'}
          </button>
        )}
        <button type="button" disabled={busyId!==null} onClick={()=>remove(issue)}
          className="button button-outline" style={{borderColor:'#e9c6c5',color:'#b53636',marginLeft:'auto'}}>
          {busyId===issue.id?'Working...':'Delete issue'}
        </button>
      </div>
    </article>) : <p>No issues reported.</p>}
  </section>
}
