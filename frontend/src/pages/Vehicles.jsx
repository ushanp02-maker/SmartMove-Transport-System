import EntityPage from '../components/EntityPage'
import FleetVehicles from './FleetVehicles'
import { isApiMode } from '../services/apiClient'
export default function Vehicles() { return isApiMode() ? <FleetVehicles /> : <EntityPage entity="vehicles" /> }
