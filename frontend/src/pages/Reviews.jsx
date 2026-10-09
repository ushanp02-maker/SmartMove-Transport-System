import EntityPage from '../components/EntityPage'
import ApiFeedbackAdmin from '../components/ApiFeedbackAdmin'
import { isApiMode } from '../services/apiClient'
export default function Reviews() { return isApiMode() ? <ApiFeedbackAdmin/> : <EntityPage entity="reviews" /> }
