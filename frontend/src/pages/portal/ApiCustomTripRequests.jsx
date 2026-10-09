import { useEffect, useState } from 'react'
import MapPicker from '../../components/MapPicker'
import { request } from '../../services/apiClient'
export default function ApiCustomTripRequests(){
 const [form,setForm]=useState({pickupAddress:'',pickupLatitude:'',pickupLongitude:'',destinationAddress:'',destinationLatitude:'',destinationLongitude:'',requestedPickupTime:'',passengerCount:1,serviceType:'STANDARD',specialRequirements:''})
 const [pickup,setPickup]=useState(null),[destination,setDestination]=useState(null),[pickTarget,setPickTarget]=useState('pickup')
 const [rows,setRows]=useState([]),[error,setError]=useState(''),[refresh,setRefresh]=useState(0)
 useEffect(()=>{request('/on-demand/my-requests').then(setRows).catch(e=>setError(e.message))},[refresh])
 const submit=async e=>{e.preventDefault();if(!pickup||!destination){setError('Select both map locations.');return}try{await request('/on-demand/requests',{method:'POST',body:JSON.stringify({...form,pickupAddress:pickup.label||'Pickup',pickupLatitude:pickup.latitude,pickupLongitude:pickup.longitude,destinationAddress:destination.label||'Destination',destinationLatitude:destination.latitude,destinationLongitude:destination.longitude,passengerCount:Number(form.passengerCount)})});setRefresh(n=>n+1)}catch(err){setError(err.message)}}
 return <section className="portal-panel"><h1>Custom trip request</h1><form onSubmit={submit}><MapPicker pickup={pickup} destination={destination} onPickupChange={setPickup} onDestinationChange={setDestination} pickTarget={pickTarget} onTargetChange={setPickTarget} height={300}/>{Object.keys(form).filter(key=>!key.startsWith('pickup')&&!key.startsWith('destination')).map(key=><label key={key}>{key}<input required={key!=='specialRequirements'} type={key==='requestedPickupTime'?'datetime-local':'text'} value={form[key]} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))}/></label>)}<button className="button button-primary">Submit</button></form>{error&&<p className="auth-error">{error}</p>}{rows.map(row=><p key={row.id}>#{row.id}: {row.status}</p>)}</section>
}
