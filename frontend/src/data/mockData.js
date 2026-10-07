export const initialData = {
	users: [
		{ userId: 'USR-ADMIN-001', username: 'superadmin', email: 'admin@smartmove.lk', role: 'SUPER_ADMIN', accountStatus: 'ACTIVE', linkedProfileId: 'ADM-001' },
		{ userId: 'USR-ADMIN-002', username: 'operations', email: 'operations@smartmove.lk', role: 'ADMIN', accountStatus: 'ACTIVE', linkedProfileId: 'ADM-002' },
	],
	admins: [
		{ id: 'ADM-001', name: 'Amara Silva', email: 'admin@smartmove.lk', username: 'superadmin', phone: '+94 77 100 0001' },
		{ id: 'ADM-002', name: 'Nimali Fernando', email: 'operations@smartmove.lk', username: 'operations', phone: '+94 71 100 0002' },
	],
	vehicles: [
		{ id: 'VH-1042', name: 'Toyota Coaster', plate: 'NB-7842', type: 'Mini Coach', capacity: 28, status: 'Active', mileage: 68420, nextService: '2025-11-14' },
		{ id: 'VH-1088', name: 'Isuzu Journey', plate: 'NC-2190', type: 'Coach', capacity: 42, status: 'Active', mileage: 92410, nextService: '2025-11-20' },
		{ id: 'VH-1126', name: 'Mitsubishi Rosa', plate: 'NA-5631', type: 'Mini Coach', capacity: 26, status: 'In service', mileage: 51800, nextService: '2025-10-28' },
		{ id: 'VH-1161', name: 'Toyota HiAce', plate: 'WP-CA 4821', type: 'Van', capacity: 12, status: 'Active', mileage: 32670, nextService: '2025-12-03' },
		{ id: 'VH-1194', name: 'Ashok Leyland Viking', plate: 'NC-7328', type: 'Coach', capacity: 48, status: 'Active', mileage: 108200, nextService: '2025-11-08' },
	],
	drivers: [
		{ id: 'DR-201', name: 'Nuwan Perera', phone: '+94 77 234 8190', license: 'B5120483', experience: 8, status: 'On duty', rating: 4.9 },
		{ id: 'DR-204', name: 'Kasun Jayawardena', phone: '+94 71 508 3762', license: 'B5087621', experience: 12, status: 'On duty', rating: 4.8 },
		{ id: 'DR-208', name: 'Chamara Fernando', phone: '+94 76 991 4220', license: 'B4991022', experience: 6, status: 'Off duty', rating: 4.7 },
		{ id: 'DR-211', name: 'Ruwan Silva', phone: '+94 72 117 6408', license: 'B5210349', experience: 10, status: 'On duty', rating: 4.9 },
	],
	routes: [
		{ id: 'RT-031', name: 'Colombo → Kandy', origin: 'Colombo', destination: 'Kandy', distance: 116, duration: '3h 15m', fare: 1850, status: 'Active' },
		{ id: 'RT-044', name: 'Colombo → Galle', origin: 'Colombo', destination: 'Galle', distance: 126, duration: '2h 30m', fare: 1600, status: 'Active' },
		{ id: 'RT-052', name: 'Kandy → Nuwara Eliya', origin: 'Kandy', destination: 'Nuwara Eliya', distance: 77, duration: '2h 45m', fare: 1400, status: 'Active' },
		{ id: 'RT-067', name: 'Colombo → Jaffna', origin: 'Colombo', destination: 'Jaffna', distance: 398, duration: '7h 30m', fare: 4200, status: 'Active' },
		{ id: 'RT-073', name: 'Colombo → Negombo', origin: 'Colombo', destination: 'Negombo', distance: 37, duration: '1h 10m', fare: 850, status: 'Active' },
		{ id: 'RT-081', name: 'Colombo → Ella', origin: 'Colombo', destination: 'Ella', distance: 205, duration: '5h 15m', fare: 2750, status: 'Active' },
	],
	trips: [
		{ id: 'TR-4021', routeId: 'RT-031', vehicleId: 'VH-1042', driverId: 'DR-201', date: '2026-10-07', departure: '07:30', arrival: '10:45', seats: 28, status: 'Scheduled' },
		{ id: 'TR-4022', routeId: 'RT-044', vehicleId: 'VH-1088', driverId: 'DR-204', date: '2026-10-08', departure: '08:00', arrival: '10:30', seats: 42, status: 'Scheduled' },
		{ id: 'TR-4023', routeId: 'RT-052', vehicleId: 'VH-1161', driverId: 'DR-211', date: '2026-10-09', departure: '09:15', arrival: '12:00', seats: 12, status: 'Scheduled' },
		{ id: 'TR-4018', routeId: 'RT-067', vehicleId: 'VH-1194', driverId: 'DR-208', date: '2026-10-06', departure: '21:00', arrival: '04:30', seats: 48, status: 'Completed' },
		{ id: 'TR-4024', routeId: 'RT-073', vehicleId: 'VH-1042', driverId: 'DR-201', date: '2026-10-10', departure: '06:45', arrival: '07:55', seats: 28, status: 'Scheduled' },
		{ id: 'TR-4025', routeId: 'RT-081', vehicleId: 'VH-1161', driverId: 'DR-211', date: '2026-10-11', departure: '06:30', arrival: '11:45', seats: 12, status: 'Scheduled' },
		{ id: 'TR-4026', routeId: 'RT-067', vehicleId: 'VH-1194', driverId: 'DR-204', date: '2026-10-12', departure: '20:30', arrival: '04:00', seats: 48, status: 'Scheduled' },
	],
	passengers: [
		{ id: 'PS-5831', name: 'Ishara de Silva', email: 'ishara.ds@email.com', phone: '+94 77 851 2093', city: 'Colombo', joined: '2025-07-12', trips: 12 },
		{ id: 'PS-5832', name: 'Malith Wijesinghe', email: 'malith.w@email.com', phone: '+94 71 450 1298', city: 'Kandy', joined: '2025-08-03', trips: 8 },
		{ id: 'PS-5833', name: 'Amaya Perera', email: 'amaya.p@email.com', phone: '+94 76 332 8810', city: 'Galle', joined: '2025-08-16', trips: 5 },
		{ id: 'PS-5834', name: 'Dinuka Ranasinghe', email: 'dinuka.r@email.com', phone: '+94 72 214 6701', city: 'Negombo', joined: '2025-09-02', trips: 3 },
		{ id: 'PS-5835', name: 'Tharushi Gunasekara', email: 'tharushi.g@email.com', phone: '+94 77 673 1490', city: 'Colombo', joined: '2025-09-19', trips: 7 },
	],
	bookings: [
		{ id: 'BK-9081', tripId: 'TR-4021', passengerId: 'PS-5831', bookedAt: '2026-10-01', seats: 2, amount: 3700, status: 'Confirmed' },
		{ id: 'BK-9082', tripId: 'TR-4022', passengerId: 'PS-5832', bookedAt: '2026-10-02', seats: 1, amount: 1600, status: 'Confirmed' },
		{ id: 'BK-9083', tripId: 'TR-4023', passengerId: 'PS-5833', bookedAt: '2026-10-03', seats: 2, amount: 2800, status: 'Pending' },
		{ id: 'BK-9084', tripId: 'TR-4021', passengerId: 'PS-5835', bookedAt: '2026-10-04', seats: 1, amount: 1850, status: 'Confirmed' },
		{ id: 'BK-9085', tripId: 'TR-4024', passengerId: 'PS-5834', bookedAt: '2026-10-05', seats: 3, amount: 2550, status: 'Cancelled' },
		{ id: 'BK-9086', tripId: 'TR-4018', passengerId: 'PS-5832', bookedAt: '2026-10-01', seats: 1, amount: 4200, status: 'Confirmed' },
	],
	payments: [
		{ id: 'PY-6112', bookingId: 'BK-9081', date: '2026-10-01', amount: 3700, method: 'Card (demo)', status: 'Paid', reference: 'SM-9A81K' },
		{ id: 'PY-6113', bookingId: 'BK-9082', date: '2026-10-02', amount: 1600, method: 'Bank transfer (demo)', status: 'Paid', reference: 'SM-9A82M' },
		{ id: 'PY-6114', bookingId: 'BK-9083', date: '2026-10-03', amount: 2800, method: 'Cash (demo)', status: 'Pending', reference: 'SM-9A83Q' },
		{ id: 'PY-6115', bookingId: 'BK-9084', date: '2026-10-04', amount: 1850, method: 'Card (demo)', status: 'Paid', reference: 'SM-9A84R' },
	],
	maintenance: [
		{ id: 'MT-301', vehicleId: 'VH-1126', issue: 'Scheduled 50,000 km service', type: 'Service', date: '2025-10-28', cost: 28500, status: 'Scheduled', notes: 'Oil, filters and brake inspection' },
		{ id: 'MT-302', vehicleId: 'VH-1042', issue: 'Rear tyre replacement', type: 'Repair', date: '2025-10-19', cost: 18600, status: 'In progress', notes: 'Replace and balance two tyres' },
		{ id: 'MT-303', vehicleId: 'VH-1194', issue: 'Air conditioning inspection', type: 'Inspection', date: '2025-10-15', cost: 9500, status: 'Completed', notes: 'Routine quarterly check' },
	],
	reviews: [
		{ id: 'RV-701', passengerId: 'PS-5831', tripId: 'TR-4018', rating: 5, date: '2025-10-23', comment: 'Comfortable ride and a very helpful driver.', status: 'Published' },
		{ id: 'RV-702', passengerId: 'PS-5832', tripId: 'TR-4022', rating: 4, date: '2025-10-22', comment: 'On time and clean vehicle. Good experience.', status: 'Published' },
		{ id: 'RV-703', passengerId: 'PS-5835', tripId: 'TR-4021', rating: 3, date: '2025-10-21', comment: 'Pleasant trip, though boarding was a little slow.', status: 'Needs review' },
	],
	announcements: [
		{ id: 'AN-101', title: 'Extended weekend service to Kandy', audience: 'All passengers', date: '2026-10-06', status: 'Published', message: 'Additional evening departures are available this weekend.' },
		{ id: 'AN-102', title: 'Service update: Colombo Fort', audience: 'Colombo routes', date: '2026-10-12', status: 'Scheduled', message: 'Please allow extra time for boarding due to station works.' },
	],
	issueReports: [],
}

export const entityConfig = {
	vehicles: { title: 'Vehicles', singular: 'Vehicle', description: 'Manage your fleet, capacity and service readiness.', icon: 'BusFront', fields: [{ key: 'name', label: 'Vehicle name', required: true }, { key: 'plate', label: 'Registration number', required: true }, { key: 'type', label: 'Vehicle type', options: ['Coach', 'Mini Coach', 'Van', 'Bus'] }, { key: 'capacity', label: 'Seat capacity', type: 'number', required: true }, { key: 'status', label: 'Status', options: ['Active', 'In service', 'Unavailable'] }, { key: 'mileage', label: 'Mileage (km)', type: 'number' }, { key: 'nextService', label: 'Next service', type: 'date' }], columns: [['name', 'Vehicle'], ['plate', 'Registration'], ['type', 'Type'], ['capacity', 'Seats'], ['mileage', 'Mileage'], ['nextService', 'Next service'], ['status', 'Status']] },
	drivers: { title: 'Drivers', singular: 'Driver', description: 'Keep driver profiles, availability and performance up to date.', icon: 'Users', fields: [{ key: 'name', label: 'Full name', required: true }, { key: 'phone', label: 'Phone number', required: true }, { key: 'license', label: 'Licence number', required: true }, { key: 'experience', label: 'Experience (years)', type: 'number' }, { key: 'rating', label: 'Rating', type: 'number' }, { key: 'status', label: 'Availability', options: ['On duty', 'Off duty', 'On leave'] }], columns: [['name', 'Driver'], ['phone', 'Phone'], ['license', 'Licence'], ['experience', 'Experience'], ['rating', 'Rating'], ['status', 'Availability']] },
	routes: { title: 'Routes', singular: 'Route', description: 'Plan the destinations and fares your network serves.', icon: 'Route', fields: [{ key: 'name', label: 'Route name', required: true }, { key: 'origin', label: 'Origin', required: true }, { key: 'destination', label: 'Destination', required: true }, { key: 'distance', label: 'Distance (km)', type: 'number' }, { key: 'duration', label: 'Est. duration' }, { key: 'fare', label: 'Base fare (LKR)', type: 'number', required: true }, { key: 'status', label: 'Status', options: ['Active', 'Paused'] }], columns: [['name', 'Route'], ['distance', 'Distance'], ['duration', 'Duration'], ['fare', 'Base fare'], ['status', 'Status']] },
	trips: { title: 'Trips', singular: 'Trip', description: 'Schedule and monitor your upcoming and completed journeys.', icon: 'Navigation', fields: [{ key: 'routeId', label: 'Route', relation: 'routes', required: true }, { key: 'vehicleId', label: 'Vehicle', relation: 'vehicles', required: true }, { key: 'driverId', label: 'Driver', relation: 'drivers', required: true }, { key: 'date', label: 'Travel date', type: 'date', required: true }, { key: 'departure', label: 'Departure', type: 'time', required: true }, { key: 'arrival', label: 'Arrival', type: 'time' }, { key: 'seats', label: 'Seat capacity', type: 'number' }, { key: 'status', label: 'Status', options: ['Scheduled', 'In progress', 'Completed', 'Cancelled'] }], columns: [['id', 'Trip ID'], ['routeId', 'Route'], ['date', 'Date'], ['departure', 'Departure'], ['vehicleId', 'Vehicle'], ['driverId', 'Driver'], ['status', 'Status']] },
	passengers: { title: 'Passengers', singular: 'Passenger', description: 'View and maintain your passenger directory.', icon: 'ContactRound', fields: [{ key: 'name', label: 'Full name', required: true }, { key: 'email', label: 'Email address', type: 'email', required: true }, { key: 'phone', label: 'Phone number', required: true }, { key: 'city', label: 'City', options: ['Colombo', 'Kandy', 'Galle', 'Negombo', 'Jaffna', 'Nuwara Eliya'] }, { key: 'joined', label: 'Joined date', type: 'date' }], columns: [['name', 'Passenger'], ['email', 'Email'], ['phone', 'Phone'], ['city', 'City'], ['joined', 'Joined'], ['trips', 'Trips']] },
	bookings: { title: 'Bookings', singular: 'Booking', description: 'Track reservations, passenger details and seat allocations.', icon: 'TicketCheck', fields: [{ key: 'tripId', label: 'Trip', relation: 'trips', required: true }, { key: 'passengerId', label: 'Passenger', relation: 'passengers', required: true }, { key: 'bookedAt', label: 'Booked on', type: 'date', required: true }, { key: 'seats', label: 'Seats', type: 'number', required: true }, { key: 'amount', label: 'Amount (LKR)', type: 'number', required: true }, { key: 'status', label: 'Status', options: ['Confirmed', 'Pending', 'Cancelled'] }], columns: [['id', 'Booking ID'], ['passengerId', 'Passenger'], ['tripId', 'Trip'], ['bookedAt', 'Booked on'], ['seats', 'Seats'], ['amount', 'Amount'], ['status', 'Status']] },
	payments: { title: 'Payments', singular: 'Payment', description: 'Administrative tracking of booking payment records only.', icon: 'Wallet', fields: [{ key: 'bookingId', label: 'Booking', relation: 'bookings', required: true }, { key: 'date', label: 'Date', type: 'date', required: true }, { key: 'amount', label: 'Amount (LKR)', type: 'number', required: true }, { key: 'method', label: 'Recorded method', options: ['Card', 'Cash', 'Bank transfer', 'Other'] }, { key: 'reference', label: 'Reference' }, { key: 'status', label: 'Status', options: ['Paid', 'Pending', 'Refunded'] }], columns: [['id', 'Payment ID'], ['bookingId', 'Booking'], ['date', 'Date'], ['method', 'Method'], ['reference', 'Reference'], ['amount', 'Amount'], ['status', 'Status']] },
	maintenance: { title: 'Maintenance', singular: 'Work order', description: 'Coordinate fleet servicing, inspections and repairs.', icon: 'Wrench', fields: [{ key: 'vehicleId', label: 'Vehicle', relation: 'vehicles', required: true }, { key: 'issue', label: 'Work description', required: true }, { key: 'type', label: 'Work type', options: ['Service', 'Repair', 'Inspection'] }, { key: 'date', label: 'Service date', type: 'date', required: true }, { key: 'cost', label: 'Estimated cost (LKR)', type: 'number' }, { key: 'status', label: 'Status', options: ['Scheduled', 'In progress', 'Completed'] }, { key: 'notes', label: 'Notes' }], columns: [['id', 'Work order'], ['vehicleId', 'Vehicle'], ['issue', 'Description'], ['type', 'Type'], ['date', 'Date'], ['cost', 'Est. cost'], ['status', 'Status']] },
	reviews: { title: 'Reviews', singular: 'Review', description: 'Monitor passenger feedback and follow up on concerns.', icon: 'Star', fields: [{ key: 'passengerId', label: 'Passenger', relation: 'passengers', required: true }, { key: 'tripId', label: 'Trip', relation: 'trips', required: true }, { key: 'rating', label: 'Rating (1–5)', type: 'number', required: true }, { key: 'date', label: 'Date', type: 'date' }, { key: 'comment', label: 'Comment', required: true }, { key: 'status', label: 'Status', options: ['Published', 'Needs review', 'Resolved'] }], columns: [['id', 'Review'], ['passengerId', 'Passenger'], ['tripId', 'Trip'], ['rating', 'Rating'], ['comment', 'Comment'], ['date', 'Date'], ['status', 'Status']] },
	announcements: { title: 'Announcements', singular: 'Announcement', description: 'Share timely service updates with your passengers.', icon: 'Megaphone', fields: [{ key: 'title', label: 'Title', required: true }, { key: 'audience', label: 'Audience', options: ['All passengers', 'Colombo routes', 'Drivers'] }, { key: 'date', label: 'Publish date', type: 'date', required: true }, { key: 'message', label: 'Message', required: true }, { key: 'status', label: 'Status', options: ['Draft', 'Scheduled', 'Published'] }], columns: [['title', 'Announcement'], ['audience', 'Audience'], ['date', 'Publish date'], ['message', 'Message'], ['status', 'Status']] },
}
