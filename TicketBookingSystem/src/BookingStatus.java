/**
 * The three legal states of a booking in this system.
 *
 * Lifecycle:
 *   WAITING   -> CONFIRMED   (promoted after a cancellation frees enough tickets)
 *   CONFIRMED -> CANCELLED   (customer cancels; tickets are released)
 *
 * A booking is never created directly as CANCELLED, and a CANCELLED
 * booking is final (it cannot return to CONFIRMED or WAITING).
 */
public enum BookingStatus {
    CONFIRMED,
    WAITING,
    CANCELLED;

    /** Human-friendly text for menus and reports. */
    public String displayName() {
        return switch (this) {
            case CONFIRMED -> "Confirmed";
            case WAITING -> "Waiting";
            case CANCELLED -> "Cancelled";
        };
    }
}
