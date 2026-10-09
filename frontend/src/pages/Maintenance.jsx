import EntityPage from '../components/EntityPage'
import ApiAdminEntity from '../components/ApiAdminEntity'
import { isApiMode } from '../services/apiClient'
export default function Maintenance() { return isApiMode() ? <ApiAdminEntity entity="maintenance" /> : <EntityPage entity="maintenance" /> }
