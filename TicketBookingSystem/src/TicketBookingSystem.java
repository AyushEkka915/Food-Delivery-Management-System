import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Core reservation engine for the Ticket Booking Queue System.
 *
 * Responsibilities:
 *   - Track ticket inventory (total, available, reserved)
 *   - Register and look up customers
 *   - Create bookings with unique IDs (B001, B002, ...)
 *   - Confirm requests when possible, otherwise enqueue into the waiting list
 *   - Record actions in history
 *   - Keep inventory consistent through every operation
 *
 * Policies (locked for this project):
 *   - Capacity must be > 0.
 *   - A request larger than total capacity is rejected (can never be fulfilled).
 *   - Ticket quantity must be greater than zero.
 *   - Multiple bookings per customer are allowed.
 *   - If the waiting list is not empty, new requests join the waiting list
 *     even if tickets are currently available (strict no-bypass / FIFO order).
 *   - Waiting bookings are registered in {@link BookingRegistry} so they can
 *     be searched, and they remain in {@link CustomQueue} for promotion.
 *   - Only CONFIRMED bookings count as reserved tickets.
 *   - Waiting tickets never reduce available tickets.
 *   - Action history records the outcome of each booking attempt.
 */
public class TicketBookingSystem {

    private final int totalCapacity;
    private int availableTickets;

    private final List<Customer> customers;
    private final BookingRegistry bookings;
    private final CustomQueue waitingList;
    private final ActionStack actionHistory;

    private int nextBookingNumber;

    /**
     * Creates a new booking system with the given ticket capacity.
     *
     * @param totalCapacity number of tickets in total
     * @throws IllegalArgumentException if totalCapacity is not greater than zero
     */
    public TicketBookingSystem(int totalCapacity) {
        if (totalCapacity <= 0) {
            throw new IllegalArgumentException("Total capacity must be greater than zero.");
        }

        this.totalCapacity = totalCapacity;
        this.availableTickets = totalCapacity;

        this.customers = new ArrayList<>();
        this.bookings = new BookingRegistry();
        this.waitingList = new CustomQueue();
        this.actionHistory = new ActionStack();
        this.nextBookingNumber = 1;
    }

    // ------------------------------------------------------------------
    // Customer management
    // ------------------------------------------------------------------

    /**
     * Registers a new customer.
     *
     * @param customerId unique ID such as "C001"
     * @param name        customer's name
     * @return the created Customer object
     * @throws IllegalArgumentException if the ID is already registered or
     *                                  validation fails
     */
    public Customer registerCustomer(String customerId, String name) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty.");
        }
        if (findCustomer(customerId) != null) {
            throw new IllegalArgumentException("Customer ID already registered: " + customerId.trim());
        }

        Customer customer = new Customer(customerId, name);
        customers.add(customer);
        return customer;
    }

    /**
     * Finds a customer by ID.
     *
     * @return the customer, or null if not found
     */
    public Customer findCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }

        String trimmed = customerId.trim();
        for (Customer customer : customers) {
            if (customer.getCustomerId().equals(trimmed)) {
                return customer;
            }
        }
        return null;
    }

    /** @return an unmodifiable snapshot of registered customers */
    public List<Customer> getCustomers() {
        return new ArrayList<>(customers);
    }

    public int getCustomerCount() {
        return customers.size();
    }

    // ------------------------------------------------------------------
    // Booking
    // ------------------------------------------------------------------

    /**
     * Requests tickets for a registered customer.
     *
     * Outcome:
     *   - CONFIRMED if the waiting list is empty AND enough tickets are available.
     *   - WAITING   otherwise (no bypass while others are waiting).
     *
     * Invalid requests throw exceptions and do not change system state.
     *
     * @param customerId the customer making the request
     * @param tickets    number of tickets requested (> 0)
     * @return the created Booking (status indicates CONFIRMED or WAITING)
     * @throws IllegalArgumentException for unknown customer, invalid quantity,
     *                                  or quantity greater than total capacity
     */
    public Booking bookTickets(String customerId, int tickets) {
        Customer customer = findCustomer(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Unknown customer ID: " + customerId);
        }
        if (tickets <= 0) {
            throw new IllegalArgumentException("Tickets requested must be greater than zero.");
        }
        if (tickets > totalCapacity) {
            throw new IllegalArgumentException(
                    "Requested tickets (" + tickets + ") exceed total capacity (" + totalCapacity + ").");
        }

        String bookingId = generateBookingId();
        Booking booking;

        // Strict no-bypass: if anyone is waiting, new requests join the queue.
        boolean canConfirm = waitingList.isEmpty() && tickets <= availableTickets;

        if (canConfirm) {
            // CONFIRMED
            booking = new Booking(bookingId, customer, tickets, BookingStatus.CONFIRMED);
            bookings.add(booking);
            availableTickets -= tickets;
            recordAction("Confirmed booking " + bookingId + " for " + customer.getCustomerId()
                    + " (" + tickets + " ticket(s))");
        } else {
            // WAITING
            booking = new Booking(bookingId, customer, tickets, BookingStatus.WAITING);
            bookings.add(booking);
            waitingList.enqueue(booking);
            recordAction("Waitlisted booking " + bookingId + " for " + customer.getCustomerId()
                    + " (" + tickets + " ticket(s))");
        }

        return booking;
    }

    private String generateBookingId() {
        String id = String.format("B%03d", nextBookingNumber);
        nextBookingNumber++;
        return id;
    }

    /**
     * Searches for a booking by ID across all statuses.
     *
     * @return the booking, or null if not found
     */
    public Booking searchBooking(String bookingId) {
        return bookings.findById(bookingId);
    }

    // ------------------------------------------------------------------
    // Reports
    // ------------------------------------------------------------------

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getAvailableTickets() {
        return availableTickets;
    }

    /**
     * Sum of tickets reserved by all CONFIRMED bookings.
     */
    public int getReservedTickets() {
        int total = 0;
        for (Booking booking : bookings.getByStatus(BookingStatus.CONFIRMED)) {
            total += booking.getTicketsRequested();
        }
        return total;
    }

    /**
     * Total tickets requested by all bookings currently in the waiting list.
     */
    public int getWaitingTickets() {
        int total = 0;
        for (Booking booking : waitingList.toList()) {
            total += booking.getTicketsRequested();
        }
        return total;
    }

    public int getWaitingCount() {
        return waitingList.size();
    }

    /**
     * @return true if available + reserved == totalCapacity
     */
    public boolean isInventoryConsistent() {
        return availableTickets + getReservedTickets() == totalCapacity;
    }

    public List<Booking> getAllBookings() {
        return bookings.getAll();
    }

    public List<Booking> getConfirmedBookings() {
        return bookings.getByStatus(BookingStatus.CONFIRMED);
    }

    public List<Booking> getWaitingBookings() {
        return waitingList.toList();
    }

    public List<Booking> getCancelledBookings() {
        return bookings.getByStatus(BookingStatus.CANCELLED);
    }

    public void displayWaitingList() {
        waitingList.displayQueue();
    }

    // ------------------------------------------------------------------
    // Action history
    // ------------------------------------------------------------------

    public int getActionCount() {
        return actionHistory.size();
    }

    public String getLatestAction() {
        if (actionHistory.isEmpty()) {
            return null;
        }
        return actionHistory.peek();
    }

    public void displayActionHistory() {
        actionHistory.displayHistory();
    }

    // ------------------------------------------------------------------
    // Cancellation and promotion
    // ------------------------------------------------------------------

    /**
     * Cancels a confirmed booking.
     *
     * Only CONFIRMED bookings can be cancelled. WAITING and CANCELLED bookings
     * cannot be cancelled. Tickets are released and waiting-list promotion is
     * attempted after cancellation.
     *
     * @param bookingId the booking to cancel
     * @return the cancelled booking
     * @throws IllegalArgumentException if booking not found
     * @throws IllegalStateException    if booking is not CONFIRMED
     */
    public Booking cancelBooking(String bookingId) {
        Booking booking = bookings.findById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking already cancelled: " + bookingId);
        }
        if (booking.getStatus() == BookingStatus.WAITING) {
            throw new IllegalStateException("Cannot cancel a waiting booking: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel booking with status: " + booking.getStatus());
        }

        // Cancel the booking and release tickets
        booking.updateStatus(BookingStatus.CANCELLED);
        availableTickets += booking.getTicketsRequested();
        recordAction("Cancelled booking " + booking.getBookingId() + " for "
                + booking.getCustomerId() + " (" + booking.getTicketsRequested() + " ticket(s))");

        // Attempt to promote eligible waiting requests (strict FIFO)
        promoteFromWaitingList();

        return booking;
    }

    /**
     * Promotes eligible waiting requests in strict FIFO order.
     *
     * Only the front of the queue is checked. If it fits, it is promoted to
     * CONFIRMED, removed from the queue, and tickets are deducted. The loop
     * continues to check the new front in case more tickets are available.
     * If the front does not fit, promotion stops (no skipping).
     */
    private void promoteFromWaitingList() {
        while (!waitingList.isEmpty()) {
            Booking front = waitingList.peek();
            if (front == null) {
                break;
            }
            if (front.getTicketsRequested() <= availableTickets) {
                // Promote front
                Booking promoted = waitingList.dequeue();
                promoted.updateStatus(BookingStatus.CONFIRMED);
                availableTickets -= promoted.getTicketsRequested();
                recordAction("Promoted booking " + promoted.getBookingId() + " to CONFIRMED ("
                        + promoted.getTicketsRequested() + " ticket(s))");
                // Continue loop to check new front
            } else {
                break; // front does not fit, stop (strict FIFO)
            }
        }
    }

    private void recordAction(String message) {
        try {
            actionHistory.push(message);
        } catch (IllegalStateException ignored) {
            // If the history is full, do not let logging block a successful operation.
        }
    }
}
