import { lazy, Suspense, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Navbar from './components/Navbar'
import { DataProvider } from './services/AppContext'
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

function Shell() {
  const [menuOpen, setMenuOpen] = useState(false)
  return (
    <div className="app-shell">
      <Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="app-main"><Navbar onMenu={() => setMenuOpen(true)} />
        <main className="main-content"><Suspense fallback={<div className="page-loading">Loading workspace…</div>}><Routes>
          <Route path="/" element={<Dashboard />} /><Route path="/vehicles" element={<Vehicles />} /><Route path="/drivers" element={<Drivers />} /><Route path="/routes" element={<RoutesPage />} /><Route path="/trips" element={<Trips />} /><Route path="/passengers" element={<Passengers />} /><Route path="/bookings" element={<Bookings />} /><Route path="/payments" element={<Payments />} /><Route path="/maintenance" element={<Maintenance />} /><Route path="/reviews" element={<Reviews />} /><Route path="/announcements" element={<Announcements />} /><Route path="/reports" element={<Reports />} /><Route path="*" element={<Navigate to="/" replace />} />
        </Routes></Suspense></main>
        <footer className="app-footer"><span>© 2025 SmartMove Transport Solutions</span><span>Operations console <i /> Demo environment</span></footer>
      </div>
    </div>
  )
}

export default function App() {
  return <BrowserRouter><DataProvider><Shell /></DataProvider></BrowserRouter>
}
