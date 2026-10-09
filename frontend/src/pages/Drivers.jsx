import AccountManagement from '../components/AccountManagement'
import ApiAccounts from '../components/ApiAccounts'
import { isApiMode } from '../services/apiClient'
export default function Drivers() { return isApiMode() ? <ApiAccounts kind="DRIVER" /> : <AccountManagement kind="DRIVER" /> }
