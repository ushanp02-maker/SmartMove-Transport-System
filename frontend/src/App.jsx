import { isApiMode } from './services/apiClient'
import { lazy, Suspense, useState } from 'react'
import { BrowserRouter, Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Navbar from './components/Navbar'
import RouteGuard from './components/portal/RouteGuard'
import { PortalShell } from './components/portal/PortalShell'
import { BusFront, ShieldCheck } from 'lucide-react'
import { DataProvider } from './services/AppContext'
import { useAppData } from './services/useAppData'
import './App.css'
import './components/TransportServices.css'

const ApiDashboard = lazy(() => import('./pages/ApiDashboard'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const Vehicles = lazy(() => import('./pages/Vehicles'))
const Drivers = lazy(() => import('./pages/Drivers'))
const FleetDriverList = lazy(() => import('./pages/FleetDrivers').then(m=>({default:m.DriverList})))
const FleetDriverForm = lazy(() => import('./pages/FleetDrivers').then(m=>({default:m.DriverForm})))
const FleetDriverDetail = lazy(() => import('./pages/FleetDrivers').then(m=>({default:m.DriverDetail})))
const FleetDriverAccounts = lazy(() => import('./pages/FleetDrivers').then(m=>({default:m.DriverAccounts})))
const RoutesPage = lazy(() => import('./pages/Routes'))
const Trips = lazy(() => import('./pages/Trips'))
const Passengers = lazy(() => import('./pages/Passengers'))
const Bookings = lazy(() => import('./pages/Bookings'))
const Payments = lazy(() => import('./pages/Payments'))
const Maintenance = lazy(() => import('./pages/Maintenance'))
const Reviews = lazy(() => import('./pages/Reviews'))
const Announcements = lazy(() => import('./pages/Announcements'))
const Reports = lazy(() => import('./pages/Reports'))
const AdminManagement = lazy(() => import('./components/AccountManagement'))
const ApiAccounts = lazy(() => import('./components/ApiAccounts'))
const ApiRequestAdmin = lazy(() => import('./components/ApiRequestAdmin'))
const CustomTripRequestsAdmin = lazy(() => import('./pages/admin/CustomTripRequests'))
const StaffTransportAdmin = lazy(() => import('./pages/admin/StaffTransport'))
const LiveFleet = lazy(() => import('./pages/admin/LiveFleet'))
const ApiSearchTrips = lazy(() => import('./pages/portal/ApiPassengerJourneys').then(m => ({default:m.ApiSearchTrips})))
const ApiPassengerTripDetails = lazy(() => import('./pages/portal/ApiPassengerJourneys').then(m => ({default:m.ApiPassengerTripDetails})))
const ApiBookTicket = lazy(() => import('./pages/portal/ApiPassengerJourneys').then(m => ({default:m.ApiBookTicket})))
const ApiMyBookings = lazy(() => import('./pages/portal/ApiPassengerJourneys').then(m => ({default:m.ApiMyBookings})))
const ApiMyTickets = lazy(() => import('./pages/portal/ApiPassengerJourneys').then(m => ({default:m.ApiMyTickets})))
const ApiPassengerDashboard = lazy(() => import('./pages/portal/ApiPassengerDashboard'))
const PassengerDashboard = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.PassengerDashboard })))
const SearchTrips = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.SearchTrips })))
const BookTicket = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.BookTicket })))
const MyBookings = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.MyBookings })))
const MyTickets = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.MyTickets })))
const ApiPassengerPayments = lazy(() => import('./pages/portal/ApiPassengerAccount').then(m => ({default:m.ApiPassengerPayments})))
const ApiPassengerProfile = lazy(() => import('./pages/portal/ApiPassengerAccount').then(m => ({default:m.ApiPassengerProfile})))
const PassengerPayments = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.PassengerPayments })))
const ApiPassengerReviews = lazy(() => import('./pages/portal/ApiPassengerExtras').then(m => ({default:m.ApiPassengerReviews})))
const ApiPassengerAnnouncements = lazy(() => import('./pages/portal/ApiPassengerExtras').then(m => ({default:m.ApiPassengerAnnouncements})))
const PassengerReviews = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.PassengerReviews })))
const PassengerAnnouncements = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.PassengerAnnouncements })))
const PassengerProfile = lazy(() => import('./pages/portal/PassengerPortal').then(module => ({ default: module.PassengerProfile })))
const ApiDriverTrips = lazy(() => import('./pages/portal/ApiDriverPortal').then(module => ({ default: module.ApiDriverTrips })))
const ApiDriverTripDetails = lazy(() => import('./pages/portal/ApiDriverPortal').then(module => ({ default: module.ApiDriverTripDetails })))
const ApiLiveFleet = lazy(() => import('./pages/admin/ApiLiveFleet'))
const DriverDashboard = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverDashboard })))
const DriverTrips = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverTrips })))
const DriverTripDetails = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverTripDetails })))
const ApiDriverSchedule = lazy(() => import('./pages/portal/ApiDriverExtras').then(m => ({default:m.ApiDriverSchedule})))
const ApiDriverProfile = lazy(() => import('./pages/portal/ApiDriverExtras').then(m => ({default:m.ApiDriverProfile})))
const ApiDriverAnnouncements = lazy(() => import('./pages/portal/ApiDriverExtras').then(m => ({default:m.ApiDriverAnnouncements})))
const DriverSchedule = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverSchedule })))
const ApiDriverVehicle = lazy(() => import('./pages/portal/ApiDriverVehicle'))
const DriverVehicle = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverVehicle })))
const DriverStatusPage = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverStatusPage })))
const ApiDriverIssues = lazy(() => import('./pages/portal/ApiDriverIssues'))
const DriverIssues = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverIssues })))
const DriverAnnouncements = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverAnnouncements })))
const DriverProfile = lazy(() => import('./pages/portal/DriverPortal').then(module => ({ default: module.DriverProfile })))
const ApiStaffTransport = lazy(() => import('./pages/portal/ApiStaffTransport'))
const ApiCustomTripRequests = lazy(() => import('./pages/portal/ApiCustomTripRequests'))
const CustomTripRequests = lazy(() => import('./pages/portal/TransportServices').then(module => ({ default: module.CustomTripRequests })))
const ApiPassengerTracking = lazy(() => import('./pages/portal/ApiPassengerTracking'))
const PassengerTripTracking = lazy(() => import('./pages/portal/TransportServices').then(module => ({ default: module.PassengerTripTracking })))
const TripDetailsPage = lazy(() => import('./pages/portal/TripDetailsPage'))
const Landing = lazy(() => import('./pages/Landing'))
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))

function AdminShell() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { dataMode } = useAppData()
  return <div className="app-shell"><Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} /><div className="app-main"><Navbar onMenu={() => setMenuOpen(true)} /><main className="main-content"><Suspense fallback={<div className="page-loading">Loading workspace…</div>}><Outlet/></Suspense></main><footer className="app-footer"><span>© 2026 SmartMove Transport Solutions</span><span>Operations console <i />{dataMode==='API'?'API mode · Integration in progress':'Local demo environment'}</span></footer></div></div>
}

function GuardedPortal({ role, children, superAdminOnly = false }) {
  return <RouteGuard role={role} superAdminOnly={superAdminOnly}>{children}</RouteGuard>
}

function PublicTravelPage({ children }) {
  return <div className="travel-public"><header className="travel-public-header"><Link className="public-logo" to="/"><span><BusFront size={21}/></span><b>smart<span>move</span></b></Link><nav><Link to="/#destinations">Destinations</Link><Link to="/#routes">Routes</Link><Link to="/login">Log in</Link><Link className="public-signup" to="/register">Sign up</Link></nav></header><main className="travel-public-main">{children}</main><footer className="travel-public-footer">SmartMove Transport Solutions · Trip availability subject to confirmation</footer></div>
}

function PassengerSearchEntry() {
  const { currentUser } = useAppData()
  if (currentUser?.role === 'PASSENGER' && currentUser.accountStatus === 'ACTIVE') return <PortalShell role="passenger">{isApiMode() ? <ApiSearchTrips/> : <SearchTrips/>}</PortalShell>
  return <PublicTravelPage>{isApiMode() ? <ApiSearchTrips/> : <SearchTrips/>}</PublicTravelPage>
}

function PassengerBookEntry() {
  const { session, currentUser } = useAppData()
  const { pathname, search } = useLocation()
  if (!session || currentUser?.accountStatus !== 'ACTIVE') return <Navigate to={`/login?next=${encodeURIComponent(pathname + search)}`} replace/>
  if (currentUser?.role !== 'PASSENGER') return <PublicTravelPage><div className="booking-role-notice"><ShieldCheck size={25}/><h1>Passenger account required</h1><p>Only passenger accounts can make bookings. You are signed in with a {currentUser?.role?.replace('_',' ') || 'non-passenger'} account.</p><Link className="button button-outline" to="/">Return home</Link><Link className="button button-primary" to="/login?switch=1">Switch account</Link></div></PublicTravelPage>
  return <PortalShell role="passenger">{isApiMode() ? <ApiBookTicket/> : <BookTicket/>}</PortalShell>
}

function AppRoutes() {
  return <Suspense fallback={<div className="page-loading">Preparing your journey…</div>}><Routes>
    <Route path="/" element={<Landing/>}/>
    <Route path="/login" element={<Login/>}/>
    <Route path="/register" element={<Register/>}/>
    <Route path="/passenger/search" element={<PassengerSearchEntry/>}/>
    <Route path="/passenger/trip/:tripId" element={<PublicTravelPage>{isApiMode() ? <ApiPassengerTripDetails/> : <TripDetailsPage/>}</PublicTravelPage>}/>
    <Route path="/passenger/book/:tripId" element={<PassengerBookEntry/>}/>
    <Route path="/passenger" element={<GuardedPortal role="passenger"><PortalShell role="passenger"/></GuardedPortal>}>
      <Route index element={isApiMode() ? <ApiPassengerDashboard/> : <PassengerDashboard/>}/>
      <Route path="bookings" element={isApiMode() ? <ApiMyBookings/> : <MyBookings/>}/>
      <Route path="custom-trips" element={isApiMode() ? <ApiCustomTripRequests/> : <CustomTripRequests/>}/>
      <Route path="staff-transport" element={isApiMode() ? <ApiStaffTransport/> : <CustomTripRequests/>}/>
      <Route path="tracking" element={isApiMode() ? <ApiPassengerTracking/> : <PassengerTripTracking/>}/>
      <Route path="tracking/:tripId" element={isApiMode() ? <ApiPassengerTracking/> : <PassengerTripTracking/>}/>
      <Route path="tickets" element={isApiMode() ? <ApiMyTickets/> : <MyTickets/>}/>
      <Route path="payments" element={isApiMode() ? <ApiPassengerPayments/> : <PassengerPayments/>}/>
      <Route path="reviews" element={isApiMode() ? <ApiPassengerReviews/> : <PassengerReviews/>}/>
      <Route path="announcements" element={isApiMode() ? <ApiPassengerAnnouncements/> : <PassengerAnnouncements/>}/>
      <Route path="profile" element={isApiMode() ? <ApiPassengerProfile/> : <PassengerProfile/>}/>
    </Route>
    <Route path="/driver" element={<GuardedPortal role="driver"><PortalShell role="driver"/></GuardedPortal>}>
      <Route index element={isApiMode() ? <ApiDriverTrips dashboard/> : <DriverDashboard/>}/>
      <Route path="trips" element={isApiMode() ? <ApiDriverTrips/> : <DriverTrips/>}/>
      <Route path="trip/:tripId" element={isApiMode() ? <ApiDriverTripDetails/> : <DriverTripDetails/>}/>
      <Route path="schedule" element={isApiMode() ? <ApiDriverSchedule/> : <DriverSchedule/>}/>
      <Route path="vehicle" element={isApiMode() ? <ApiDriverVehicle/> : <DriverVehicle/>}/>
      <Route path="trip-status" element={isApiMode() ? <ApiDriverTrips/> : <DriverStatusPage/>}/>
      <Route path="issues" element={isApiMode() ? <ApiDriverIssues/> : <DriverIssues/>}/>
      <Route path="announcements" element={isApiMode() ? <ApiDriverAnnouncements/> : <DriverAnnouncements/>}/>
      <Route path="profile" element={isApiMode() ? <ApiDriverProfile/> : <DriverProfile/>}/>
    </Route>
    <Route path="/admin" element={<GuardedPortal role="admin"><AdminShell/></GuardedPortal>}>
      <Route index element={isApiMode() ? <ApiDashboard/> : <Dashboard/>}/><Route path="vehicles" element={<Vehicles/>}/><Route path="drivers" element={isApiMode()?<FleetDriverList/>:<Drivers/>}/><Route path="drivers/new" element={isApiMode()?<FleetDriverForm/>:<Drivers/>}/><Route path="drivers/accounts" element={isApiMode()?<FleetDriverAccounts/>:<Drivers/>}/><Route path="drivers/:id/edit" element={isApiMode()?<FleetDriverForm/>:<Drivers/>}/><Route path="drivers/:id" element={isApiMode()?<FleetDriverDetail/>:<Drivers/>}/><Route path="routes" element={<RoutesPage/>}/><Route path="trips" element={<Trips/>}/><Route path="passengers" element={<Passengers/>}/><Route path="bookings" element={<Bookings/>}/><Route path="payments" element={<Payments/>}/><Route path="maintenance" element={<Maintenance/>}/><Route path="reviews" element={<Reviews/>}/><Route path="announcements" element={<Announcements/>}/><Route path="reports" element={<Reports/>}/><Route path="custom-trip-requests" element={isApiMode() ? <ApiRequestAdmin kind="custom"/> : <CustomTripRequestsAdmin/>}/><Route path="staff-transport" element={isApiMode() ? <ApiRequestAdmin kind="staff"/> : <StaffTransportAdmin/>}/><Route path="live-fleet" element={isApiMode() ? <ApiLiveFleet/> : <LiveFleet/>}/><Route path="admin-management" element={<GuardedPortal role="admin" superAdminOnly>{isApiMode() ? <ApiAccounts kind="ADMIN"/> : <AdminManagement kind="ADMIN"/>}</GuardedPortal>}/>
    </Route>
    {['vehicles','drivers','routes','trips','passengers','bookings','payments','maintenance','reviews','announcements','reports'].map(path=><Route key={path} path={`/${path}`} element={<Navigate to={`/admin/${path}`} replace/>}/>)}
    <Route path="*" element={<Navigate to="/" replace/>}/>
  </Routes></Suspense>
}

export default function App() {
  return <BrowserRouter><DataProvider><AppRoutes/></DataProvider></BrowserRouter>
}
