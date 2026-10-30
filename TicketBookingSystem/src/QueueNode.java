/**
 * A single node in a singly linked queue.
 *
 * Each node holds one {@link Booking} and a pointer to the node behind it in
 * the waiting line. The booking itself is not checked here: {@link CustomQueue}
 * is responsible for rejecting null bookings, so this class stays a plain
 * data holder with no validation rules of its own.
 */
public class QueueNode {

    private Booking booking;
    private QueueNode next;

    /**
     * @param booking the booking stored in this node; may be null at this level
     */
    public QueueNode(Booking booking) {
        this.booking = booking;
        this.next = null;
    }

    public Booking getBooking() {
        return booking;
    }

    /**
     * @return the next node towards the rear of the queue, or null at the rear
     */
    public QueueNode getNext() {
        return next;
    }

    public void setNext(QueueNode next) {
        this.next = next;
    }
}
