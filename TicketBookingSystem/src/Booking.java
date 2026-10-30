/**
 * One ticket request made by a customer.
 *
 * A booking is created with status CONFIRMED (tickets were available) or
 * WAITING (the request joined the queue). It is never created as CANCELLED.
 * The status can later change only through updateStatus(), which enforces
 * the legal transitions.
 */
public class Booking {

    private final String bookingId;
    private final Customer customer;
    private final int ticketsRequested;
    private BookingStatus status;

    /**
     * @param bookingId        unique ID such as "B001"
     * @param customer         the customer making the request
     * @param ticketsRequested number of tickets; must be greater than zero
     * @param status           initial status: CONFIRMED or WAITING
     * @throws IllegalArgumentException for invalid IDs, customers, quantities,
     *                                  or an attempt to start as CANCELLED
     */
    public Booking(String bookingId, Customer customer, int ticketsRequested, BookingStatus status) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("Booking ID cannot be empty.");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Booking must belong to a customer.");
        }
        if (ticketsRequested <= 0) {
            throw new IllegalArgumentException(
                    "Tickets requested must be greater than zero, but was " + ticketsRequested + ".");
        }
        if (status == null) {
            throw new IllegalArgumentException("Booking status cannot be null.");
        }
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("A new booking cannot start as CANCELLED.");
        }

        this.bookingId = bookingId.trim();
        this.customer = customer;
        this.ticketsRequested = ticketsRequested;
        this.status = status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public Customer getCustomer() {
        return customer;
    }

    /** Convenience shortcut so callers do not need customer.getCustomerId(). */
    public String getCustomerId() {
        return customer.getCustomerId();
    }

    public int getTicketsRequested() {
        return ticketsRequested;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean isWaiting() {
        return status == BookingStatus.WAITING;
    }

    /**
     * Moves the booking to a new state, rejecting illegal transitions.
     *
     * Allowed:
     *   WAITING   -> CONFIRMED
     *   CONFIRMED -> CANCELLED
     */
    public void updateStatus(BookingStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null.");
        }
        if (!isLegalTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                    "Illegal status change for " + bookingId + ": "
                            + this.status + " -> " + newStatus);
        }
        this.status = newStatus;
    }

    private static boolean isLegalTransition(BookingStatus from, BookingStatus to) {
        return (from == BookingStatus.WAITING && to == BookingStatus.CONFIRMED)
                || (from == BookingStatus.CONFIRMED && to == BookingStatus.CANCELLED);
    }

    /** Two bookings are equal when their booking IDs are equal. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Booking)) {
            return false;
        }
        Booking that = (Booking) other;
        return bookingId.equals(that.bookingId);
    }

    @Override
    public int hashCode() {
        return bookingId.hashCode();
    }

    @Override
    public String toString() {
        return bookingId + " | " + customer.getCustomerId()
                + " (" + customer.getName() + ")"
                + " | " + ticketsRequested + " ticket(s)"
                + " | " + status;
    }
}
