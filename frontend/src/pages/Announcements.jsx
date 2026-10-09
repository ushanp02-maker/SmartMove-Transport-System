import EntityPage from '../components/EntityPage'
import ApiAnnouncementsAdmin from '../components/ApiAnnouncementsAdmin'
import { isApiMode } from '../services/apiClient'
export default function Announcements() { return isApiMode() ? <ApiAnnouncementsAdmin/> : <EntityPage entity="announcements" /> }
