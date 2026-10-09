import EntityPage from '../components/EntityPage'
import ApiAdminEntity from '../components/ApiAdminEntity'
import { isApiMode } from '../services/apiClient'
export default function Routes() { return isApiMode() ? <ApiAdminEntity entity="routes" /> : <EntityPage entity="routes" /> }
