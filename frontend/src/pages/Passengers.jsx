import EntityPage from '../components/EntityPage'
import ApiDirectory from '../components/ApiDirectory'
import { isApiMode } from '../services/apiClient'
export default function Passengers() { return isApiMode() ? <ApiDirectory entity="passengers"/> : <EntityPage entity="passengers"/> }
