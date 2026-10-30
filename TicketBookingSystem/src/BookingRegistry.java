import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A HashMap-backed index of every booking created in the system.
 *
 * Key   = booking ID (for example "B001")
 * Value = the Booking object
 *
 * The map is the system's record of bookings; the waiting-list queue is a
 * separate structure that preserves arrival order. Both point at the SAME
 * Booking objects, so a status change made through either reference is seen
 * everywhere.
 *
 * All statuses are kept here, including CANCELLED, so that:
 *   - search by booking ID can explain what happened to a booking,
 *   - a booking ID can never be reused, even after cancellation,
 *   - reports can filter by status.
 */
public class BookingRegistry {

    // Program to the Map interface, not the concrete HashMap class.
    private final Map<String, Booking> bookingsById;

    public BookingRegistry() {
        this.bookingsById = new HashMap<>();
    }

    /**
     * Adds a booking to the index.
     *
     * @throws IllegalArgumentException if the booking is null or its ID is
     *                                  already registered
     */
    public void add(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Cannot register a null booking.");
        }

        String bookingId = booking.getBookingId();
        if (bookingsById.containsKey(bookingId)) {
            throw new IllegalArgumentException("Booking ID already exists: " + bookingId);
        }

        bookingsById.put(bookingId, booking);
    }

    /**
     * Looks up a booking by ID.
     *
     * @return the booking, or null when the ID is unknown, null, or blank
     */
    public Booking findById(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) {
            return null;
        }
        return bookingsById.get(bookingId.trim());
    }

    /** @return true when a booking with this ID exists */
    public boolean contains(String bookingId) {
        return findById(bookingId) != null;
    }

    /** @return number of bookings stored, including cancelled ones */
    public int size() {
        return bookingsById.size();
    }

    /** @return true when no bookings have been registered */
    public boolean isEmpty() {
        return bookingsById.isEmpty();
    }

    /**
     * Returns a snapshot of every booking.
     *
     * A new list is created on each call, so changing it cannot modify the
     * registry. The list itself is in no guaranteed order.
     */
    public List<Booking> getAll() {
        return new ArrayList<>(bookingsById.values());
    }

    /**
     * Returns a snapshot of bookings whose current status matches.
     *
     * This is a linear filter over the map values: O(n) time, O(n) space for
     * the returned list. It is used by reports and by inventory checks.
     */
    public List<Booking> getByStatus(BookingStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null.");
        }

        List<Booking> matches = new ArrayList<>();
        for (Booking booking : bookingsById.values()) {
            if (booking.getStatus() == status) {
                matches.add(booking);
            }
        }
        return matches;
    }
}
