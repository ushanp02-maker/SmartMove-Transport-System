import EntityPage from '../components/EntityPage'
import ApiAdminEntity from '../components/ApiAdminEntity'
import { isApiMode } from '../services/apiClient'
export default function Payments() { return isApiMode() ? <ApiAdminEntity entity="payments" /> : <EntityPage entity="payments" /> }
