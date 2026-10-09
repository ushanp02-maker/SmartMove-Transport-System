import EntityPage from '../components/EntityPage'
import ApiDirectory from '../components/ApiDirectory'
import { isApiMode } from '../services/apiClient'
export default function Bookings() { return isApiMode() ? <ApiDirectory entity="bookings"/> : <EntityPage entity="bookings"/> }
