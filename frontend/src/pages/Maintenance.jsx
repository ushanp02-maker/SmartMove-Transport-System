import EntityPage from '../components/EntityPage'
import ApiAdminEntity from '../components/ApiAdminEntity'
import ApiDriverIssuesAdmin from '../components/ApiDriverIssuesAdmin'
import { isApiMode } from '../services/apiClient'
export default function Maintenance() { return isApiMode() ? <><ApiDriverIssuesAdmin/><ApiAdminEntity entity="maintenance" /></> : <EntityPage entity="maintenance" /> }
