import AccountManagement from '../components/AccountManagement'
import ApiDriverProfiles from '../components/ApiDriverProfiles'
import ApiAccounts from '../components/ApiAccounts'
import { isApiMode } from '../services/apiClient'
export default function Drivers() { return isApiMode() ? <><ApiDriverProfiles/><ApiAccounts kind="DRIVER" /></> : <AccountManagement kind="DRIVER" /> }
