import { useEffect, useState } from 'react'
import MapPicker from '../../components/MapPicker'
import { supportedPlaces } from '../../services/placeCatalogue'
import './smartmove-journey.css'
import { request } from '../../services/apiClient'
export default function ApiCustomTripRequests(){
 const [form,setForm]=useState({pickupAddress:'',pickupLatitude:'',pickupLongitude:'',destinationAddress:'',destinationLatitude:'',destinationLongitude:'',requestedPickupTime:'',passengerCount:1,serviceType:'CUSTOM',specialRequirements:''})
 const [pickup,setPickup]=useState(null),[destination,setDestination]=useState(null),[pickTarget,setPickTarget]=useState('pickup')
 const [pickupText,setPickupText]=useState(''),[destinationText,setDestinationText]=useState('')
 const [activeField,setActiveField]=useState(null)
 const findPlaces = text => text.trim().length >= 2 ? supportedPlaces.filter(place => place.name.toLowerCase().includes(text.trim().toLowerCase())).slice(0,8) : []
 const chooseTypedPlace = (target,place) => {
   const value={latitude:place.latitude,longitude:place.longitude,label:place.name}
   if(target==='pickup'){setPickup(value);setPickupText(place.name)}
   else{setDestination(value);setDestinationText(place.name)}
   setActiveField(null)
 }
 const resolveTypedPlace = text => {
   const normalized=text.trim().toLowerCase()
   if(!normalized)return null
   return supportedPlaces.find(place=>place.name.toLowerCase()===normalized) || (findPlaces(text).length===1 ? findPlaces(text)[0] : null)
 }
 const settleTypedLocation = target => {
   const text=target==='pickup'?pickupText:destinationText
   const match=resolveTypedPlace(text)
   if(match)chooseTypedPlace(target,match)
   else setActiveField(null)
 }
 const changeTypedLocation=(target,text)=>{
   if(target==='pickup'){setPickupText(text);setPickup(null)}
   else{setDestinationText(text);setDestination(null)}
   const exact=supportedPlaces.find(place=>place.name.toLowerCase()===text.trim().toLowerCase())
   if(exact)chooseTypedPlace(target,exact)
   else setActiveField(target)
 }
 useEffect(()=>{if(pickup)setPickupText(pickup.label||'')},[pickup])
 useEffect(()=>{if(destination)setDestinationText(destination.label||'')},[destination])
 const [rows,setRows]=useState([]),[error,setError]=useState(''),[refresh,setRefresh]=useState(0),[filter,setFilter]=useState('ALL'),[selected,setSelected]=useState(null)
 useEffect(()=>{request('/on-demand/my-requests').then(setRows).catch(e=>setError(e.message))},[refresh])
 const submit=async e=>{e.preventDefault();const resolvedPickup=pickup||resolveTypedPlace(pickupText);const resolvedDestination=destination||resolveTypedPlace(destinationText);if(!resolvedPickup||!resolvedDestination){setError('Enter a recognized pickup and destination, or select them from the suggestions.');return}if(resolvedPickup.latitude===resolvedDestination.latitude&&resolvedPickup.longitude===resolvedDestination.longitude){setError('Choose different pickup and destination locations.');return}try{await request('/on-demand/requests',{method:'POST',body:JSON.stringify({...form,pickupAddress:resolvedPickup.label||resolvedPickup.name||'Pickup',pickupLatitude:resolvedPickup.latitude,pickupLongitude:resolvedPickup.longitude,destinationAddress:resolvedDestination.label||resolvedDestination.name||'Destination',destinationLatitude:resolvedDestination.latitude,destinationLongitude:resolvedDestination.longitude,passengerCount:Number(form.passengerCount)})});setRefresh(n=>n+1)}catch(err){setError(err.message)}}
 return <div className="sm-custom-page">
 <header className="sm-custom-header"><div><span className="sm-eyebrow">SMARTMOVE PASSENGER</span><h1>Custom trip request</h1><p>Plan a personalised journey anywhere in Sri Lanka. Choose your pickup, destination, preferred time and seats. Our team will arrange transport after reviewing your request.</p></div><span className="sm-help">How custom trips work: Request → Review → Assignment</span></header>
 <div className="sm-custom-grid"><form className="sm-surface sm-custom-form" onSubmit={submit}>
 <h2>Trip details</h2><p>Select your pickup and destination on the map or search below.</p>
 <div className="sm-custom-inner"><div className="sm-custom-inputs"><div className="sm-fields"><label>Pickup location<input value={pickupText} placeholder="Type pickup location" autoComplete="off" onFocus={()=>{setPickTarget('pickup');setActiveField('pickup')}} onChange={e=>changeTypedLocation('pickup',e.target.value)} onBlur={()=>{if(resolveTypedPlace(pickupText))settleTypedLocation('pickup')}}/>{activeField==='pickup'&&pickupText.trim().length>=2&&<div className="map-place-results" style={{position:'relative',zIndex:100}}>{findPlaces(pickupText).map(place=><button key={place.name} type="button" onClick={()=>chooseTypedPlace('pickup',place)}>{place.name}</button>)}{findPlaces(pickupText).length===0&&<small>No matching place. Choose a point on the map instead.</small>}</div>}</label><label>Destination<input value={destinationText} placeholder="Type destination" autoComplete="off" onFocus={()=>{setPickTarget('destination');setActiveField('destination')}} onChange={e=>changeTypedLocation('destination',e.target.value)} onBlur={()=>{if(resolveTypedPlace(destinationText))settleTypedLocation('destination')}}/>{activeField==='destination'&&destinationText.trim().length>=2&&<div className="map-place-results" style={{position:'relative',zIndex:100}}>{findPlaces(destinationText).map(place=><button key={place.name} type="button" onClick={()=>chooseTypedPlace('destination',place)}>{place.name}</button>)}{findPlaces(destinationText).length===0&&<small>No matching place. Choose a point on the map instead.</small>}</div>}</label><label>Preferred date and time<input type="datetime-local" required value={form.requestedPickupTime} onChange={e=>setForm(v=>({...v,requestedPickupTime:e.target.value}))}/></label><label>Number of seats<input type="number" required min="1" max="1000" value={form.passengerCount} onChange={e=>setForm(v=>({...v,passengerCount:e.target.value}))}/></label></div></div><div className="sm-map-container"><MapPicker pickup={pickup} destination={destination} onPickupChange={setPickup} onDestinationChange={setDestination} pickTarget={pickTarget} onTargetChange={setPickTarget} height={340}/></div></div>
 <label className="sm-special">Special requirements (optional)<textarea rows="3" maxLength="300" placeholder="e.g. extra luggage, child seats, multiple stops..." value={form.specialRequirements} onChange={e=>setForm(v=>({...v,specialRequirements:e.target.value}))}/></label>
 {error&&<p className="auth-error" role="alert">{error}</p>}<button className="button button-primary sm-submit">Submit custom trip request</button></form>
 <aside className="sm-surface sm-summary"><h2>Request summary</h2><div className="sm-summary-route"><small>Pickup</small><strong>{pickup?.label||'Not selected'}</strong><small>Destination</small><strong>{destination?.label||'Not selected'}</strong></div><div className="sm-summary-row"><small>Preferred time</small><strong>{form.requestedPickupTime?.replace('T',' ')||'Not selected'}</strong></div><div className="sm-summary-row"><small>Seats</small><strong>{form.passengerCount}</strong></div><div className="sm-summary-row"><small>Special requirements</small><strong>{form.specialRequirements||'None'}</strong></div></aside></div>
 <section className="sm-surface sm-requests"><h2>My custom trip requests</h2><p>View the status of your custom trip requests.</p><div className="sm-filter-tabs">{['ALL','PENDING','APPROVED','REJECTED','CANCELLED'].map(status=><button key={status} type="button" className={filter===status?'selected':''} onClick={()=>setFilter(status)}>{status==='ALL'?'All':status.toLowerCase()} ({rows.filter(r=>status==='ALL'||String(r.status).toUpperCase()===status).length})</button>)}</div><div className="sm-table-scroll"><table className="sm-table"><thead><tr><th>Request ID</th><th>Route</th><th>Date & time</th><th>Seats</th><th>Special requirements</th><th>Status</th><th>Actions</th></tr></thead><tbody>{rows.filter(r=>filter==='ALL'||String(r.status).toUpperCase()===filter).map(row=><tr key={row.id}><td>CR-{row.id}</td><td>{row.pickupAddress} → {row.destinationAddress}</td><td>{String(row.requestedPickupTime||'').replace('T',' ').slice(0,16)}</td><td>{row.passengerCount}</td><td>{row.specialRequirements||'—'}</td><td><span className={'sm-status sm-'+String(row.status).toLowerCase()}>{row.status}</span></td><td><button type="button" className="sm-view" onClick={()=>setSelected(row)}>View</button></td></tr>)}</tbody></table></div>{selected&&<div className="sm-request-detail"><button className="sm-view" type="button" onClick={()=>setSelected(null)}>Close</button><h3>Request #{selected.id}</h3><p>{selected.pickupAddress} → {selected.destinationAddress} · {selected.passengerCount} seats · {selected.status}</p>{selected.assignedTripId&&<p>Assigned trip #{selected.assignedTripId}</p>}</div>}</section></div>
}
