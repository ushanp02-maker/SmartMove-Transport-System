import EntityPage from '../components/EntityPage'
import ApiAdminEntity from '../components/ApiAdminEntity'
import { isApiMode } from '../services/apiClient'
export default function Trips() { return isApiMode() ? <ApiAdminEntity entity="trips" /> : <EntityPage entity="trips" /> }
