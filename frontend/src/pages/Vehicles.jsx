import EntityPage from '../components/EntityPage'
import ApiAdminEntity from '../components/ApiAdminEntity'
import { isApiMode } from '../services/apiClient'
export default function Vehicles() { return isApiMode() ? <ApiAdminEntity entity="vehicles" /> : <EntityPage entity="vehicles" /> }
