import { useEffect, useState } from 'react'
import { Camera, ImagePlus, Trash2, Expand } from 'lucide-react'
import { request, downloadVehicleFile } from '../services/apiClient'
import './VehiclePhotoGallery.css'

export default function VehiclePhotoGallery({vehicleId}) {
 const [photos,setPhotos]=useState([]),[urls,setUrls]=useState({}),[file,setFile]=useState(null),[title,setTitle]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[revision,setRevision]=useState(0)
 useEffect(()=>{let live=true;const created=[];async function load(){try{
  const records=await request('/vehicle-documents/vehicle/'+vehicleId)
  const images=(Array.isArray(records)?records:[]).filter(x=>x.contentType?.startsWith('image/')&&x.status!=='ARCHIVED'&&x.storedFileName)
  if(!live)return
  setPhotos(images)
  const pairs=await Promise.all(images.map(async x=>{try{const blob=await downloadVehicleFile(x.storedFileName);const url=URL.createObjectURL(blob);created.push(url);return [x.id,url]}catch{return [x.id,null]}}))
  if(live)setUrls(Object.fromEntries(pairs))
 }catch(e){if(live)setError(e.message)}}load();return()=>{live=false;created.forEach(url=>URL.revokeObjectURL(url))}},[vehicleId,revision])
 async function upload(e){e.preventDefault();if(!file)return;setBusy(true);setError('');try{
  if(!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>10*1024*1024)throw new Error('Choose a JPG, PNG or WebP photo under 10 MB.')
  const data=new FormData();data.append('vehicleId',vehicleId);data.append('documentType','OTHER');data.append('title',title.trim()||file.name);data.append('file',file)
  await request('/vehicle-documents/files',{method:'POST',body:data});setFile(null);setTitle('');e.target.reset();setRevision(n=>n+1)
 }catch(err){setError(err.message)}finally{setBusy(false)}}
 async function remove(photo){if(!window.confirm('Remove this vehicle photo permanently?'))return;setBusy(true);setError('');try{await request('/vehicle-documents/files/'+encodeURIComponent(photo.id),{method:'DELETE'});setRevision(n=>n+1)}catch(err){setError(err.message)}finally{setBusy(false)}}
 return <section className="vpg"><div className="vpg-heading"><div className="vpg-symbol"><Camera size={21}/></div><div><h3>Vehicle photo gallery</h3><p>Add exterior, interior and condition photos for this vehicle.</p></div><span className="vpg-total">{photos.length} photos</span></div>
 <form className="vpg-upload" onSubmit={upload}><label className="vpg-drop"><ImagePlus size={26}/><strong>{file?file.name:'Choose a vehicle photo'}</strong><span>JPG, PNG or WebP · Maximum 10 MB</span><input type="file" accept="image/jpeg,image/png,image/webp" required onChange={e=>setFile(e.target.files?.[0]||null)}/></label><div className="vpg-fields"><input value={title} onChange={e=>setTitle(e.target.value)} placeholder="Photo title (optional)" maxLength={200} aria-label="Photo title"/><button type="submit" disabled={!file||busy}>{busy?'Saving...':'Upload photo'}</button></div></form>
 {error&&<p className="vpg-error" role="alert">{error}</p>}
 <div className="vpg-grid">{photos.map(photo=><article key={photo.id} className="vpg-card">{urls[photo.id]?<img src={urls[photo.id]} alt={photo.title||'Vehicle photo'} loading="lazy"/>:<div className="vpg-placeholder">Preview unavailable</div>}<div className="vpg-card-bottom"><strong title={photo.title}>{photo.title}</strong><div>{urls[photo.id]&&<button title="Open full size" type="button" onClick={()=>window.open(urls[photo.id],'_blank','noopener,noreferrer')}><Expand size={16}/></button>}<button title="Delete photo" type="button" disabled={busy} onClick={()=>remove(photo)}><Trash2 size={16}/></button></div></div></article>)}</div>
 {!photos.length&&<div className="vpg-empty">No vehicle photos uploaded yet. Add the first photo above.</div>}</section>
}
