import { useEffect, useState } from 'react'
import { FileText, Image, UploadCloud, Trash2, Eye } from 'lucide-react'
import { request, downloadVehicleFile } from '../services/apiClient'
import './VehicleAttachments.css'

const types=['REGISTRATION','INSURANCE','REVENUE_LICENSE','FITNESS_CERTIFICATE','INSPECTION','EMISSION_TEST','OTHER']
export default function VehicleAttachments({vehicleId}) {
 const [items,setItems]=useState([]),[rev,setRev]=useState(0),[error,setError]=useState(''),[busy,setBusy]=useState(false)
 const [file,setFile]=useState(null),[type,setType]=useState('REGISTRATION'),[title,setTitle]=useState(''),[expiry,setExpiry]=useState('')
 useEffect(()=>{let active=true;request('/vehicle-documents/vehicle/'+vehicleId).then(x=>{if(active)setItems(x||[])}).catch(e=>{if(active)setError(e.message)});return()=>{active=false}},[vehicleId,rev])
 const upload=async e=>{e.preventDefault();if(!file)return;setBusy(true);setError('');try{
  const data=new FormData();data.append('vehicleId',vehicleId);data.append('documentType',type);data.append('title',title);if(expiry)data.append('expiryDate',expiry);data.append('file',file)
  await request('/vehicle-documents/files',{method:'POST',body:data})
  setFile(null);setTitle('');setExpiry('');e.target.reset();setRev(n=>n+1)
 }catch(err){setError(err.message)}finally{setBusy(false)}}
 const view=async x=>{try{setError('');const blob=await downloadVehicleFile(x.storedFileName);const url=URL.createObjectURL(blob);window.open(url,'_blank','noopener,noreferrer');setTimeout(()=>URL.revokeObjectURL(url),60000)}catch(err){setError(err.message)}}
 const remove=async x=>{if(!window.confirm('Archive this document and permanently delete its uploaded file?'))return;setBusy(true);setError('');try{await request('/vehicle-documents/files/'+encodeURIComponent(x.id),{method:'DELETE'});setRev(n=>n+1)}catch(err){setError(err.message)}finally{setBusy(false)}}
 return <section className="va-panel"><h3><FileText size={20}/> Documents &amp; images</h3><p>Upload photos, registration, insurance or service documents. Files are stored in MongoDB GridFS.</p>
 <form onSubmit={upload} className="va-form"><label>Document type<select value={type} onChange={e=>setType(e.target.value)}>{types.map(t=><option key={t} value={t}>{t.replaceAll('_',' ')}</option>)}</select></label><label>Document title<input required maxLength={200} value={title} onChange={e=>setTitle(e.target.value)} placeholder="e.g. Vehicle photo or insurance 2026"/></label><label>Expiry date (optional)<input type="date" value={expiry} onChange={e=>setExpiry(e.target.value)}/></label><label className="va-file">Choose PDF or image<input type="file" required accept=".pdf,.jpg,.jpeg,.png,.webp" onChange={e=>setFile(e.target.files?.[0]||null)}/></label><button disabled={busy||!file} type="submit"><UploadCloud size={16}/>{busy?'Uploading...':'Upload file'}</button></form>
 {error&&<p className="va-error" role="alert">{error}</p>}
 <div className="va-list">{items.filter(x=>x.status!=='ARCHIVED').map(x=><article key={x.id}><span className="va-icon">{x.contentType?.startsWith('image/')?<Image size={20}/>:<FileText size={20}/>}</span><div><strong>{x.title}</strong><small>{x.documentType?.replaceAll('_',' ')} · {x.verificationStatus||'PENDING'}{x.expiryDate?' · Expires '+x.expiryDate:''}</small></div><div className="va-actions">{x.storedFileName&&<button type="button" onClick={()=>view(x)}><Eye size={15}/> View</button>}<button type="button" disabled={busy} onClick={()=>remove(x)}><Trash2 size={15}/> Remove</button></div></article>)}{!items.filter(x=>x.status!=='ARCHIVED').length&&<p className="va-empty">No documents or images uploaded for this vehicle yet.</p>}</div>
 </section>
}
