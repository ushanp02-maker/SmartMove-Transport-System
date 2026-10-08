

import { useMemo, useState } from 'react'

import {

  BusFront,

  CalendarDays,

  Check,

  Clock3,

  Plus,

  Route,

  Search,

  ShieldCheck,

  Users,

  X,

} from 'lucide-react'

import { useAppData } from '../../services/useAppData'

import { formatDate } from '../../services/formatters'

import { supportedPlaces } from '../../services/placeCatalogue'

import {

  STAFF_WEEKDAYS,

  prepareRecurringStaffTrips,

} from '../../services/staffScheduleService'



const today = () => {

  const date = new Date()

  const local = new Date(

    date.getTime() - date.getTimezoneOffset() * 60000

  )

  return local.toISOString().slice(0, 10)

}



const defaultStops =

  'Piliyandala, Maharagama, Nugegoda, Orion City, Colombo'



export default function StaffTransportAdmin() {

  const {

    data,

    addRecord,

    addRecords,

    updateRecord,

  } = useAppData()



  const [showForm, setShowForm] = useState(false)

  const [notice, setNotice] = useState('')

  const [error, setError] = useState('')

  const [query, setQuery] = useState('')



  const [companyId, setCompanyId] = useState(

    data.companies[0]?.id || ''

  )



  const [placeNames, setPlaceNames] = useState(defaultStops)



  const [operatingDays, setOperatingDays] = useState([

    'Mon',

    'Tue',

    'Wed',

    'Thu',

    'Fri',

  ])



  const [vehicleId, setVehicleId] = useState('')

  const [driverId, setDriverId] = useState('')



  const [addCompany, setAddCompany] = useState(false)

  const [newCompanyName, setNewCompanyName] = useState('')

  const [newWorkplace, setNewWorkplace] = useState('')



  const [travelDate, setTravelDate] = useState('')

  const [departure, setDeparture] = useState('07:00')

  const [arrival, setArrival] = useState('08:15')



  const [expandedRouteId, setExpandedRouteId] = useState(null)



  const routes = useMemo(

    () => data.staffRoutes || [],

    [data.staffRoutes]

  )



  const filtered = useMemo(

    () =>

      routes.filter(route =>

        [

          route.name,

          route.companyName,

          route.origin,

          route.destination,

        ].some(value =>

          String(value || '')

            .toLowerCase()

            .includes(query.toLowerCase())

        )

      ),

    [routes, query]

  )



  const routeStops = useMemo(

    () =>

      placeNames

        .split(',')

        .map(name =>

          supportedPlaces.find(

            place =>

              place.name.toLowerCase() ===

              name.trim().toLowerCase()

          )

        )

        .filter(Boolean),

    [placeNames]

  )



  const toggleDay = day => {

    setOperatingDays(days =>

      days.includes(day)

        ? days.filter(item => item !== day)

        : [...days, day]

    )

  }



  const saveCompany = () => {

    setError('')



    const name = newCompanyName.trim()

    const workplace = newWorkplace.trim()



    if (name.length < 2 || workplace.length < 2) {

      setError('Enter a company name and workplace destination.')

      return

    }



    if (

      data.companies.some(

        company =>

          company.name.toLowerCase() === name.toLowerCase()

      )

    ) {

      setError('That demo company already exists.')

      return

    }



    const id =

      `CO-${crypto.randomUUID().slice(0, 8).toUpperCase()}`



    addRecord('companies', {

      id,

      name,

      workplace,

      employeeIds: [],

    })



    setCompanyId(id)

    setNewCompanyName('')

    setNewWorkplace('')

    setAddCompany(false)



    setNotice('Demo company and workplace added successfully.')

  }



  const createRoute = event => {

    event.preventDefault()



    setError('')

    setNotice('')



    const company = data.companies.find(

      item => item.id === companyId

    )



    const vehicle = data.vehicles.find(

      item => item.id === vehicleId

    )



    const driver = data.drivers.find(

      item => item.id === driverId

    )



    const driverAccount = data.users.find(

      user =>

        user.linkedProfileId === driverId &&

        user.role === 'DRIVER'

    )



    if (!company || !vehicle || !driver) {

      setError('Choose a company, vehicle and driver.')

      return

    }



    const requestedNames = placeNames

      .split(',')

      .map(name => name.trim())

      .filter(Boolean)



    if (

      requestedNames.length < 2 ||

      routeStops.length !== requestedNames.length

    ) {

      setError(

        'Enter at least two valid supported stop names, separated by commas.'

      )

      return

    }



    if (

      new Set(routeStops.map(stop => stop.name)).size !==

      routeStops.length

    ) {

      setError('Do not repeat the same stop in a route.')

      return

    }



    if (operatingDays.length === 0) {

      setError('Select at least one operating day.')

      return

    }



    if (!travelDate || travelDate < today()) {

      setError('Select today or a future first service date.')

      return

    }



    if (vehicle.status !== 'Active') {

      setError('Only active vehicles can serve staff routes.')

      return

    }



    if (driver.status !== 'On duty') {

      setError('Only on-duty drivers can serve staff routes.')

      return

    }



    if (

      driverAccount &&

      driverAccount.accountStatus !== 'ACTIVE'

    ) {

      setError('The selected driver account is disabled.')

      return

    }



    if (typeof addRecords !== 'function') {

      setError(

        'Bulk trip saving is unavailable. Check AppContext.jsx.'

      )

      return

    }



    const id =

      `ST-${crypto.randomUUID().slice(0, 8).toUpperCase()}`



    const routeId = `RT-${id}`



    const stops = routeStops.map((place, index) => ({

      stopOrder: index + 1,

      name: place.name,

      latitude: place.latitude,

      longitude: place.longitude,

      scheduledTime:

        index === 0

          ? departure

          : index === routeStops.length - 1

            ? arrival

            : '',

    }))



    const route = {

      id,

      routeId,

      serviceType: 'STAFF',

      companyId,

      companyName: company.name,

      name: `${stops[0].name} → ${stops.at(-1).name}`,

      origin: stops[0].name,

      destination: stops.at(-1).name,

      stops,

      operatingDays: [...operatingDays],

      assignedVehicleId: vehicleId,

      assignedDriverId: driverId,

      departure,

      arrival,

      firstServiceDate: travelDate,

      scheduleWeeks: 4,

      distance: 0,

      duration: 'Recurring demo schedule',

      fare: 0,

      status: 'Active',

    }



    let result



    try {

      result = prepareRecurringStaffTrips({

        trips: data.trips,

        routeId,

        companyId,

        companyName: company.name,

        vehicleId,

        driverId,

        startDate: travelDate,

        operatingDays,

        departure,

        arrival,

        seats: Number(vehicle.capacity),

        weeks: 4,

      })

    } catch (scheduleError) {

      setError(scheduleError.message)

      return

    }



    if (result.error || result.conflicts.length > 0) {

      const details = result.conflicts

        .slice(0, 3)

        .map(

          conflict =>

            `${conflict.date}: ${conflict.reason} (${conflict.tripId})`

        )

        .join('; ')



      setError(

        `${result.error || 'Scheduling conflict.'} ${details}`

      )

      return

    }



    if (result.trips.length === 0) {

      setError('No trips were generated for these dates.')

      return

    }



    // Both updates use the existing shared demo state.

    addRecord('staffRoutes', route)

    addRecords('trips', result.trips)



    setNotice(

      `${route.name} created for ${company.name}. ` +

      `${result.trips.length} recurring trips scheduled ` +

      `across four weeks, starting ${formatDate(result.dates[0])}.`

    )



    setExpandedRouteId(routeId)

    setPlaceNames(defaultStops)

    setShowForm(false)

  }



  const toggleEligibility = employee => {

    updateRecord('employees', employee.id, {

      eligible: !employee.eligible,

    })



    setNotice(

      `Employee ${employee.employeeCode} ${

        employee.eligible ? 'removed from' : 'added to'

      } the eligible roster.`

    )

  }



  const reviewReservation = (booking, action) => {
    if (booking.status !== 'Requested') return

    const trip = data.trips.find(item => item.id === booking.tripId)
    const employee = data.employees.find(item => item.id === booking.employeeId)
    const passenger = data.passengers.find(item => item.id === employee?.passengerId)
    const name = passenger?.name || employee?.employeeCode || booking.employeeId

    if (action === 'Approved') {
      if (!trip || trip.status === 'Cancelled' || trip.status === 'Canceled') {
        setNotice('Cannot approve: the associated trip is missing or cancelled.')
        return
      }
      if (!employee?.eligible || employee.companyId !== booking.companyId || trip.companyId !== booking.companyId) {
        setNotice('Cannot approve: employee eligibility or company assignment is invalid.')
        return
      }
      const occupied = (data.staffBookings || [])
        .filter(item => item.tripId === trip.id && item.id !== booking.id && !['Cancelled', 'Rejected'].includes(item.status))
        .reduce((total, item) => total + Number(item.seats || 0), 0)
      if (occupied + Number(booking.seats || 0) > Number(trip.seats || 0)) {
        setNotice('Cannot approve: the trip does not have enough available seats.')
        return
      }
    }

    // Existing passenger search treats Cancelled requests as released seats.
    // Preserve that behaviour until passenger-side Rejected handling is updated.
    const status = action === 'Approved' ? 'Approved' : 'Cancelled'
    updateRecord('staffBookings', booking.id, {
      status,
      reviewDecision: action,
      reviewedAt: new Date().toISOString(),
    })
    setNotice(`${name}'s staff reservation was ${action.toLowerCase()}.`)
  }

  const routeTrips = routeId =>

    data.trips

      .filter(trip => trip.routeId === routeId)

      .sort(

        (a, b) =>

          `${a.date}T${a.departure}`.localeCompare(

            `${b.date}T${b.departure}`

          )

      )



  return (

    <div className="page-content">

      <div className="page-heading">

        <div>

          <div className="eyebrow">

            TRANSPORT SERVICES · STAFF

          </div>

          <h1>Staff transportation</h1>

          <p>

            Manage company shuttles, recurring schedules,

            employee eligibility and vehicle assignments.

          </p>

        </div>



        <button

          type="button"

          className="button button-primary"

          onClick={() => {

            setShowForm(value => !value)

            setError('')

          }}

        >

          <Plus size={16} />

          {showForm ? 'Close' : 'Create staff route'}

        </button>

      </div>



      <div className="custom-admin-banner">

        <ShieldCheck size={16} />

        <span>

          Staff routes are separate from commuter services.

          Recurring schedules are stored in local demo data.

          A backend must eventually enforce availability and

          employee permissions.

        </span>

      </div>



      {notice && (

        <div className="notice-banner" role="status">

          <span>{notice}</span>

          <button

            type="button"

            onClick={() => setNotice('')}

            aria-label="Dismiss notification"

          >

            <X size={16} />

          </button>

        </div>

      )}



      {showForm && (

        <form

          className="staff-route-form portal-panel"

          onSubmit={createRoute}

        >

          <div className="portal-panel-head">

            <div>

              <h2>New recurring company shuttle</h2>

              <p>

                Select operating days and automatically generate

                four weeks of scheduled trips.

              </p>

            </div>

          </div>



          <div className="staff-form-grid">

            <label>

              Company

              <select

                value={companyId}

                onChange={event =>

                  setCompanyId(event.target.value)

                }

              >

                {data.companies.map(company => (

                  <option

                    key={company.id}

                    value={company.id}

                  >

                    {company.name} · {company.workplace}

                  </option>

                ))}

              </select>

            </label>



            <button

              type="button"

              className="button button-quiet add-company-toggle"

              onClick={() =>

                setAddCompany(value => !value)

              }

            >

              + Define company/workplace

            </button>



            {addCompany && (

              <div className="new-company-fields">

                <label>

                  Company name

                  <input

                    value={newCompanyName}

                    onChange={event =>

                      setNewCompanyName(event.target.value)

                    }

                  />

                </label>



                <label>

                  Workplace destination

                  <input

                    value={newWorkplace}

                    onChange={event =>

                      setNewWorkplace(event.target.value)

                    }

                  />

                </label>



                <button

                  type="button"

                  className="button button-outline"

                  onClick={saveCompany}

                >

                  Add company

                </button>

              </div>

            )}



            <label className="staff-stops-field">

              Stops in travel order

              <input

                value={placeNames}

                onChange={event =>

                  setPlaceNames(event.target.value)

                }

                placeholder="Supported stops, comma separated"

                required

              />

              <small>

                Supported names:{' '}

                {supportedPlaces

                  .slice(0, 10)

                  .map(place => place.name)

                  .join(', ')}

                …

              </small>

            </label>



            <label>

              Operating days

              <div className="staff-days">

                {STAFF_WEEKDAYS.map(day => (

                  <button

                    key={day}

                    type="button"

                    className={

                      operatingDays.includes(day)

                        ? 'selected'

                        : ''

                    }

                    onClick={() => toggleDay(day)}

                    aria-pressed={operatingDays.includes(day)}

                  >

                    {day}

                  </button>

                ))}

              </div>

            </label>



            <label>

              First service date

              <input

                type="date"

                min={today()}

                value={travelDate}

                onChange={event =>

                  setTravelDate(event.target.value)

                }

                required

              />

            </label>



            <label>

              Departure

              <input

                type="time"

                value={departure}

                onChange={event =>

                  setDeparture(event.target.value)

                }

                required

              />

            </label>



            <label>

              Destination arrival

              <input

                type="time"

                value={arrival}

                onChange={event =>

                  setArrival(event.target.value)

                }

                required

              />

            </label>



            <label>

              Vehicle

              <select

                value={vehicleId}

                onChange={event =>

                  setVehicleId(event.target.value)

                }

                required

              >

                <option value="">

                  Select active vehicle

                </option>



                {data.vehicles.map(vehicle => (

                  <option

                    key={vehicle.id}

                    value={vehicle.id}

                    disabled={vehicle.status !== 'Active'}

                  >

                    {vehicle.plate} · {vehicle.name} (

                    {vehicle.capacity} seats · {vehicle.status})

                  </option>

                ))}

              </select>

            </label>



            <label>

              Driver

              <select

                value={driverId}

                onChange={event =>

                  setDriverId(event.target.value)

                }

                required

              >

                <option value="">

                  Select on-duty driver

                </option>



                {data.drivers.map(driver => (

                  <option

                    key={driver.id}

                    value={driver.id}

                    disabled={driver.status !== 'On duty'}

                  >

                    {driver.name} · {driver.status}

                  </option>

                ))}

              </select>

            </label>

          </div>



          <p className="portal-demo-caption">

            The selected date starts a 28-day scheduling

            window. Trips are created only on the selected

            weekdays. Any detected assignment conflict

            prevents the entire schedule from being saved.

          </p>



          {error && (

            <p className="form-error-text" role="alert">

              {error}

            </p>

          )}



          <button

            type="submit"

            className="button button-primary"

          >

            <Check size={15} />

            Generate four-week staff schedule

          </button>

        </form>

      )}



      <div className="staff-admin-grid">

        <section className="portal-panel">

          <div className="portal-panel-head">

            <div>

              <h2>Company staff routes</h2>

              <p>{routes.length} dedicated route(s)</p>

            </div>



            <label className="staff-search">

              <Search size={14} />

              <input

                value={query}

                onChange={event =>

                  setQuery(event.target.value)

                }

                placeholder="Search company or stop"

              />

            </label>

          </div>



          {filtered.length ? (

            <div className="staff-route-list">

              {filtered.map(route => {

                const trips = routeTrips(route.routeId)



                const vehicle = data.vehicles.find(

                  item =>

                    item.id === route.assignedVehicleId

                )



                const driver = data.drivers.find(

                  item =>

                    item.id === route.assignedDriverId

                )



                const expanded =

                  expandedRouteId === route.routeId



                return (

                  <article

                    className="staff-route-admin-card"

                    key={route.id}

                  >

                    <div className="staff-route-admin-title">

                      <span className="staff-route-icon">

                        <BusFront size={17} />

                      </span>



                      <div>

                        <strong>{route.name}</strong>

                        <small>

                          {route.companyName} ·{' '}

                          {route.operatingDays?.join(' / ')}

                        </small>

                      </div>



                      <span className="staff-service-badge">

                        STAFF

                      </span>

                    </div>



                    <div className="staff-route-stops">

                      {route.stops?.map(stop => (

                        <span key={stop.stopOrder}>

                          <i />

                          {stop.name}

                          {stop.scheduledTime && (

                            <small>

                              {stop.scheduledTime}

                            </small>

                          )}

                        </span>

                      ))}

                    </div>



                    <div className="staff-route-meta">

                      <span>

                        <BusFront size={13} />

                        {vehicle?.plate ||

                          'Vehicle not assigned'}

                      </span>



                      <span>

                        <Users size={13} />

                        {driver?.name ||

                          'Driver not assigned'}

                      </span>



                      <span>

                        <CalendarDays size={13} />

                        {trips.length} scheduled trip(s)

                      </span>



                      <span>

                        <Clock3 size={13} />

                        {trips[0]

                          ? `${trips[0].departure}–${trips[0].arrival}`

                          : '—'}

                      </span>

                    </div>



                    <button

                      type="button"

                      className="button button-outline"

                      onClick={() =>

                        setExpandedRouteId(

                          expanded ? null : route.routeId

                        )

                      }

                    >

                      {expanded

                        ? 'Hide scheduled trips'

                        : `View ${trips.length} scheduled trips`}

                    </button>



                    {expanded && (

                      <div style={{ marginTop: 16 }}>

                        <h3>Scheduled departures</h3>



                        {trips.length === 0 ? (

                          <p>No trips scheduled yet.</p>

                        ) : (

                          <div

                            style={{

                              maxHeight: 320,

                              overflowY: 'auto',

                            }}

                          >

                            <table

                              style={{

                                width: '100%',

                                textAlign: 'left',

                              }}

                            >

                              <thead>

                                <tr>

                                  <th>Date</th>

                                  <th>Time</th>

                                  <th>Status</th>

                                </tr>

                              </thead>



                              <tbody>

                                {trips.map(trip => (

                                  <tr key={trip.id}>

                                    <td>

                                      {formatDate(trip.date)}

                                    </td>

                                    <td>

                                      {trip.departure}–

                                      {trip.arrival}

                                    </td>

                                    <td>{trip.status}</td>

                                  </tr>

                                ))}

                              </tbody>

                            </table>

                          </div>

                        )}

                      </div>

                    )}

                  </article>

                )

              })}

            </div>

          ) : (

            <div className="portal-empty compact">

              <span>

                <Route size={20} />

              </span>

              <strong>No matching staff routes</strong>

              <p>Create a route or adjust the search.</p>

            </div>

          )}

        </section>



        <section className="portal-panel">

          <div className="portal-panel-head">

            <div>

              <h2>Employee eligibility</h2>

              <p>Demo rosters, separated by company.</p>

            </div>

          </div>



          {data.employees.length ? (

            <div className="employee-eligibility-list">

              {data.employees.map(employee => {

                const company = data.companies.find(

                  item => item.id === employee.companyId

                )



                const passenger = data.passengers.find(

                  item => item.id === employee.passengerId

                )



                return (

                  <article key={employee.id}>

                    <span className="employee-avatar">

                      {passenger?.name

                        ?.split(' ')

                        .map(part => part[0])

                        .slice(0, 2)

                        .join('') || 'EM'}

                    </span>



                    <div>

                      <strong>

                        {passenger?.name ||

                          employee.employeeCode}

                      </strong>



                      <small>

                        {company?.name} ·{' '}

                        {employee.employeeCode}

                      </small>

                    </div>



                    <button

                      type="button"

                      className={`eligibility-toggle ${

                        employee.eligible

                          ? 'eligible'

                          : 'ineligible'

                      }`}

                      onClick={() =>

                        toggleEligibility(employee)

                      }

                    >

                      {employee.eligible

                        ? 'Eligible'

                        : 'Not eligible'}

                    </button>

                  </article>

                )

              })}

            </div>

          ) : (

            <div className="portal-empty compact">

              <span>

                <Users size={20} />

              </span>

              <strong>No demo employee roster</strong>

              <p>

                Employee profiles will be managed from

                the backend.

              </p>

            </div>

          )}

        </section>

      </div>



      <section className="portal-panel staff-bookings-admin">

        <div className="portal-panel-head">

          <div>

            <h2>Staff reservations & active trips</h2>

            <p>

              Employee reservations and current staff

              service assignments.

            </p>

          </div>

        </div>



        <div className="staff-admin-grid">

          <div>

            <strong className="staff-subheading">

              RESERVATIONS

            </strong>



            {(data.staffBookings || []).length ? (

              data.staffBookings.map(booking => {
                const employee = data.employees.find(item => item.id === booking.employeeId)
                const passenger = data.passengers.find(item => item.id === employee?.passengerId)
                const trip = data.trips.find(item => item.id === booking.tripId)
                const route = data.staffRoutes.find(item => item.routeId === trip?.routeId)
                const journey = booking.boardingStopName && booking.destinationStopName
                  ? `${booking.boardingStopName} → ${booking.destinationStopName}`
                  : booking.routeName || route?.name || booking.tripId
                const displayStatus = booking.reviewDecision === 'Rejected'
                  ? 'Rejected'
                  : booking.status

                return (
                  <div className="staff-mini-record" key={booking.id}>
                    <strong>{passenger?.name || employee?.employeeCode || booking.employeeId}</strong>
                    <div>{journey}</div>
                    <small>
                      {trip?.date ? formatDate(trip.date) : 'Trip date unavailable'} ·{' '}
                      {booking.boardingTime || trip?.departure || '—'} ·{' '}
                      {booking.seats} seat(s) · {displayStatus}
                    </small>
                    <small>{booking.id} · {booking.tripId}</small>
                    {booking.status === 'Requested' && (
                      <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
                        <button
                          type="button"
                          className="button button-primary"
                          onClick={() => reviewReservation(booking, 'Approved')}
                        >
                          <Check size={14} /> Approve
                        </button>
                        <button
                          type="button"
                          className="button button-outline"
                          onClick={() => reviewReservation(booking, 'Rejected')}
                        >
                          <X size={14} /> Reject
                        </button>
                      </div>
                    )}
                  </div>
                )
              })

            ) : (

              <div className="empty-small">

                No staff reservations yet.

              </div>

            )}

          </div>



          <div>

            <strong className="staff-subheading">

              ACTIVE STAFF TRIPS

            </strong>



            {data.trips.filter(

              trip =>

                trip.serviceType === 'STAFF' &&

                trip.status === 'In progress'

            ).length ? (

              data.trips

                .filter(

                  trip =>

                    trip.serviceType === 'STAFF' &&

                    trip.status === 'In progress'

                )

                .map(trip => (

                  <div

                    className="staff-mini-record"

                    key={trip.id}

                  >

                    <span>

                      {trip.companyName} · {trip.id}

                    </span>

                    <small>

                      {trip.date} · {trip.departure} ·{' '}

                      {trip.vehicleId}

                    </small>

                  </div>

                ))

            ) : (

              <div className="empty-small">

                No staff service is currently marked

                in progress.

              </div>

            )}

          </div>

        </div>

      </section>



      <p className="portal-demo-caption">

        Recurring schedules and employee records use

        shared local demo state. Vehicle availability,

        employee access and seat reservations require

        backend validation before production use.

      </p>

    </div>

  )

}