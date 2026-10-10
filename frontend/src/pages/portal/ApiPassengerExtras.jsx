import { useEffect, useState } from 'react'
import { Bell, BusFront, CalendarDays, CheckCircle2, ChevronDown, Megaphone, MessageCircle, Send, Star, Tag, Wrench } from 'lucide-react'
import { request } from '../../services/apiClient'
import { listPassengerBookings } from '../../services/bookingService'
import './PassengerPolish.css'

const list = data => Array.isArray(data) ? data : data?.content || []
const categoryIcon = category => /MAINTENANCE/i.test(category) ? Wrench : /ROUTE|SCHEDULE/i.test(category) ? BusFront : /PROMO|OFFER/i.test(category) ? Tag : Megaphone
const categoryName = value => String(value || 'GENERAL').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())

export function ApiPassengerAnnouncements() {
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('ALL')
  const [expanded, setExpanded] = useState(null)
  useEffect(() => {
    request('/announcements/public').then(data => setItems(list(data))).catch(e => setError(e.message)).finally(() => setLoading(false))
  }, [])
  const categories = [...new Set(items.map(a => String(a.category || 'GENERAL').toUpperCase()))]
  const visible = items.filter(a => filter === 'ALL' || String(a.category || 'GENERAL').toUpperCase() === filter)
  return <div className="sp-page">
    <header className="sp-heading sp-announcement-hero"><div><span className="sp-eyebrow">SMARTMOVE UPDATES</span><h1>Service announcements</h1><p>Stay updated with the latest news, alerts and important information.</p></div><span className="sp-hero-icon"><Megaphone size={43}/></span></header>
    <section className="sp-surface">
      <div className="sp-tabs"><button type="button" className={filter === 'ALL' ? 'active' : ''} onClick={() => setFilter('ALL')}>All announcements</button>{categories.map(category => <button type="button" key={category} className={filter === category ? 'active' : ''} onClick={() => setFilter(category)}>{categoryName(category)}</button>)}</div>
      {error && <p className="sp-error" role="alert">{error}</p>}
      {loading ? <p className="sp-empty">Loading announcements...</p> : visible.length === 0 ? <div className="sp-empty"><Bell size={28}/><h3>No published announcements</h3><p>Service updates will appear here when administrators publish them.</p></div> : <div className="sp-announcements">{visible.map((a, i) => {
        const Icon = categoryIcon(a.category)
        const isOpen = expanded === (a.id ?? i)
        return <article className="sp-announcement" key={a.id ?? i}>
          <div className="sp-announcement-icon"><Icon size={23}/></div>
          <div className="sp-announcement-copy"><div className="sp-announcement-title"><h3>{a.title}</h3><span className="sp-tag">{categoryName(a.category)}</span></div><small><CalendarDays size={13}/> {a.publishedAt || a.createdAt ? new Date(a.publishedAt || a.createdAt).toLocaleDateString() : 'Service update'} {a.priority ? ' · ' + categoryName(a.priority) + ' priority' : ''}</small><p className={isOpen ? '' : 'sp-clamp'}>{a.message}</p></div>
          <button className="sp-read-more" type="button" onClick={() => setExpanded(isOpen ? null : (a.id ?? i))}>{isOpen ? 'Show less' : 'Read more'} <ChevronDown size={15}/></button>
        </article>
      })}</div>}
    </section>
  </div>
}

export function ApiPassengerReviews() {
  const [bookings, setBookings] = useState([])
  const [bookingId, setBookingId] = useState('')
  const [rating, setRating] = useState(5)
  const [title, setTitle] = useState('')
  const [comment, setComment] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  useEffect(() => { listPassengerBookings().then(setBookings).catch(e => setMessage(e.message)) }, [])
  const submit = async e => {
    e.preventDefault()
    setSaving(true)
    setMessage('')
    try {
      const passenger = await request('/users/me/passenger')
      await request('/feedback', { method: 'POST', body: JSON.stringify({ passengerId: passenger.passengerId, bookingId: Number(bookingId), rating: Number(rating), title, comment, category: 'GENERAL' }) })
      setMessage('Your feedback has been submitted for review.')
      setTitle('')
      setComment('')
    } catch (err) { setMessage(err.message) }
    finally { setSaving(false) }
  }
  return <div className="sp-page">
    <header className="sp-heading sp-review-hero"><div><span className="sp-eyebrow">YOUR EXPERIENCE MATTERS</span><h1>Trip feedback</h1><p>Help us improve our service by sharing your experience.</p></div><div className="sp-review-art"><BusFront size={67}/><span>★★★★★</span></div></header>
    <form className="sp-surface sp-review-form" onSubmit={submit}>
      <div className="sp-booking-picker"><div className="sp-booking-icon"><BusFront size={22}/></div><div className="sp-booking-field"><label htmlFor="sp-booking">Select your booking</label><p>Choose a trip to leave a review</p><select id="sp-booking" required value={bookingId} onChange={e => setBookingId(e.target.value)}><option value="">Choose a booking</option>{bookings.map(b => <option key={b.id} value={b.id}>{b.bookingReference} · Trip #{b.tripId}</option>)}</select></div></div>
      <div className="sp-form-grid"><div className="sp-form-field sp-rating-card"><label>Your rating</label><p>How would you rate your trip?</p><div className="sp-stars" role="group" aria-label="Trip rating">{[1,2,3,4,5].map(n => <button type="button" aria-label={n + ' stars'} aria-pressed={rating === n} key={n} onClick={() => setRating(n)}><Star size={30} fill={n <= rating ? 'currentColor' : 'none'}/></button>)}</div></div><div className="sp-form-field"><label htmlFor="sp-review-title">Review title</label><p>Summarize your experience</p><input id="sp-review-title" required maxLength={150} placeholder="e.g. Great driver and smooth ride" value={title} onChange={e => setTitle(e.target.value)}/></div></div>
      <div className="sp-form-field"><label htmlFor="sp-review-comment"><MessageCircle size={16}/> Your comment</label><textarea id="sp-review-comment" required maxLength={3000} placeholder="Share your experience, suggestions or feedback..." value={comment} onChange={e => setComment(e.target.value)} rows={5}/></div>
      <div className="sp-form-footer">{message && <p className="sp-form-message" role="status"><CheckCircle2 size={17}/>{message}</p>}<button className="sp-primary" disabled={saving} type="submit"><Send size={16}/>{saving ? 'Submitting...' : 'Submit feedback'}</button></div>
    </form>
  </div>
}
