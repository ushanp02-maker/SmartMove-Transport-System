import { lazy, Suspense, useState } from 'react'
import { BrowserRouter, Link, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Navbar from './components/Navbar'
import RouteGuard from './components/portal/RouteGuard'
import { PortalShell } from './components/portal/PortalShell'
import { BusFront } from 'lucide-react'
import { DataProvider } from './services/AppContext'
import { useAppData } from './services/useAppData'
import { PassengerDashboard, SearchTrips, PassengerTripDetails, BookTicket, MyBookings, MyTickets, PassengerPayments, PassengerReviews, PassengerAnnouncements, PassengerProfile } from './pages/portal/PassengerPortal'
import { DriverDashboard, DriverTrips, DriverTripDetails, DriverSchedule, DriverVehicle, DriverStatusPage, DriverIssues, DriverAnnouncements, DriverProfile } from './pages/portal/DriverPortal'
import './App.css'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const Vehicles = lazy(() => import('./pages/Vehicles'))
const Drivers = lazy(() => import('./pages/Drivers'))
const RoutesPage = lazy(() => import('./pages/Routes'))
const Trips = lazy(() => import('./pages/Trips'))
const Passengers = lazy(() => import('./pages/Passengers'))
const Bookings = lazy(() => import('./pages/Bookings'))
const Payments = lazy(() => import('./pages/Payments'))
const Maintenance = lazy(() => import('./pages/Maintenance'))
const Reviews = lazy(() => import('./pages/Reviews'))
const Announcements = lazy(() => import('./pages/Announcements'))
const Reports = lazy(() => import('./pages/Reports'))
const Landing = lazy(() => import('./pages/Landing'))
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))

function AdminShell() {
  const [menuOpen, setMenuOpen] = useState(false)
  return <div className="app-shell"><Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} /><div className="app-main"><Navbar onMenu={() => setMenuOpen(true)} /><main className="main-content"><Suspense fallback={<div className="page-loading">Loading workspace…</div>}><Outlet/></Suspense></main><footer className="app-footer"><span>© 2026 SmartMove Transport Solutions</span><span>Operations console <i /> Demo environment</span></footer></div></div>
}

function GuardedPortal({ role, children }) {
  return <RouteGuard role={role}>{children}</RouteGuard>
}

function PublicTravelPage({ children }) {
  return <div className="travel-public"><header className="travel-public-header"><Link className="public-logo" to="/"><span><BusFront size={21}/></span><b>smart<span>move</span></b></Link><nav><Link to="/#destinations">Destinations</Link><Link to="/#routes">Routes</Link><Link to="/login">Log in</Link><Link className="public-signup" to="/register">Sign up</Link></nav></header><main className="travel-public-main">{children}</main><footer className="travel-public-footer">SmartMove Transport Solutions · Demo trip information only</footer></div>
}

function PassengerSearchEntry() {
  const { session } = useAppData()
  if (session?.role === 'passenger') return <PortalShell role="passenger"><SearchTrips/></PortalShell>
  return <PublicTravelPage><SearchTrips/></PublicTravelPage>
}

function AppRoutes() {
  return <Suspense fallback={<div className="page-loading">Preparing your journey…</div>}><Routes>
    <Route path="/" element={<Landing/>}/>
    <Route path="/login" element={<Login/>}/>
    <Route path="/register" element={<Register/>}/>
    <Route path="/passenger/search" element={<PassengerSearchEntry/>}/>
    <Route path="/passenger/trip/:tripId" element={<PublicTravelPage><PassengerTripDetails/></PublicTravelPage>}/>
    <Route path="/passenger" element={<GuardedPortal role="passenger"><PortalShell role="passenger"/></GuardedPortal>}>
      <Route index element={<PassengerDashboard/>}/>
      <Route path="book/:tripId" element={<BookTicket/>}/>
      <Route path="bookings" element={<MyBookings/>}/>
      <Route path="tickets" element={<MyTickets/>}/>
      <Route path="payments" element={<PassengerPayments/>}/>
      <Route path="reviews" element={<PassengerReviews/>}/>
      <Route path="announcements" element={<PassengerAnnouncements/>}/>
      <Route path="profile" element={<PassengerProfile/>}/>
    </Route>
    <Route path="/driver" element={<GuardedPortal role="driver"><PortalShell role="driver"/></GuardedPortal>}>
      <Route index element={<DriverDashboard/>}/>
      <Route path="trips" element={<DriverTrips/>}/>
      <Route path="trip/:tripId" element={<DriverTripDetails/>}/>
      <Route path="schedule" element={<DriverSchedule/>}/>
      <Route path="vehicle" element={<DriverVehicle/>}/>
      <Route path="trip-status" element={<DriverStatusPage/>}/>
      <Route path="issues" element={<DriverIssues/>}/>
      <Route path="announcements" element={<DriverAnnouncements/>}/>
      <Route path="profile" element={<DriverProfile/>}/>
    </Route>
    <Route path="/admin" element={<GuardedPortal role="admin"><AdminShell/></GuardedPortal>}>
      <Route index element={<Dashboard/>}/><Route path="vehicles" element={<Vehicles/>}/><Route path="drivers" element={<Drivers/>}/><Route path="routes" element={<RoutesPage/>}/><Route path="trips" element={<Trips/>}/><Route path="passengers" element={<Passengers/>}/><Route path="bookings" element={<Bookings/>}/><Route path="payments" element={<Payments/>}/><Route path="maintenance" element={<Maintenance/>}/><Route path="reviews" element={<Reviews/>}/><Route path="announcements" element={<Announcements/>}/><Route path="reports" element={<Reports/>}/>
    </Route>
    {['vehicles','drivers','routes','trips','passengers','bookings','payments','maintenance','reviews','announcements','reports'].map(path=><Route key={path} path={`/${path}`} element={<Navigate to={`/admin/${path}`} replace/>}/>)}
    <Route path="*" element={<Navigate to="/" replace/>}/>
  </Routes></Suspense>
}

export default function App() {
  return <BrowserRouter><DataProvider><AppRoutes/></DataProvider></BrowserRouter>
}
