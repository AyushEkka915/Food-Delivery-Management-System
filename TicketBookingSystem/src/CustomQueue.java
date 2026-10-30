import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A hand-written singly linked FIFO (first in, first out) queue of bookings.
 *
 * This class exists to show the data structure itself, so it does not use any
 * class from the Java collections framework. Bookings leave the queue in the
 * same order they joined it: the customer who has waited longest is served
 * first.
 *
 * The queue keeps two pointers. {@code front} is the node that will be removed
 * next and {@code rear} is the last node, whose {@code next} is always null.
 * Keeping both means enqueue and dequeue are both O(1). The {@code size} field
 * is kept in step with the pointers so callers do not have to walk the list.
 */
public class CustomQueue {

    private QueueNode front;
    private QueueNode rear;
    private int size;

    /** Creates an empty queue with no front, no rear and a size of zero. */
    public CustomQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    /**
     * Adds a booking to the rear of the queue.
     *
     * @param booking the booking to add; must not be null
     * @throws IllegalArgumentException if {@code booking} is null
     */
    public void enqueue(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Cannot enqueue a null booking.");
        }

        QueueNode node = new QueueNode(booking);

        if (isEmpty()) {
            // Empty queue: the new node is both the front and the rear.
            front = node;
        } else {
            // Non-empty queue: link the old rear to the new node.
            rear.setNext(node);
        }
        // In both cases the new node becomes the new rear.
        rear = node;
        size++;
    }

    /**
     * Removes and returns the booking at the front of the queue.
     *
     * The front pointer simply moves one node forward; the removed node is then
     * unreachable and can be garbage collected. When the last node is removed,
     * {@code front} becomes null and {@code rear} must be reset to null too,
     * otherwise it would keep pointing at a node that is no longer in the queue.
     *
     * @return the booking that had waited longest
     * @throws NoSuchElementException if the queue is empty
     */
    public Booking dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot dequeue from an empty queue.");
        }

        Booking removed = front.getBooking();
        front = front.getNext();

        // Single-element case: the queue is now empty, so clear the rear.
        if (front == null) {
            rear = null;
        }
        size--;
        return removed;
    }

    /**
     * Returns, without removing, the booking at the front of the queue.
     *
     * @return the booking that will be dequeued next
     * @throws NoSuchElementException if the queue is empty
     */
    public Booking peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peek an empty queue.");
        }
        return front.getBooking();
    }

    /**
     * An empty queue has no front node. This stays consistent with
     * {@code size == 0} because both are updated together.
     *
     * @return true when the queue holds no bookings
     */
    public boolean isEmpty() {
        return front == null;
    }

    /**
     * @return the number of bookings currently waiting
     */
    public int size() {
        return size;
    }

    /**
     * Prints the waiting line from front to rear, numbered from 1.
     *
     * Front is printed first because that booking is served first. An empty
     * queue prints a single explanatory line instead of a header.
     */
    public void displayQueue() {
        if (isEmpty()) {
            System.out.println("Waiting list is empty.");
            return;
        }

        System.out.println("Waiting list (front -> rear):");

        // Walk the links rather than the array-style index, numbering as we go.
        int position = 1;
        QueueNode current = front;
        while (current != null) {
            System.out.println("  " + position + ". " + current.getBooking());
            current = current.getNext();
            position++;
        }
    }

    /**
     * Returns a snapshot of the queued bookings in front-to-rear order.
     *
     * A new list is created, so changing it cannot modify the queue. The
     * bookings themselves are shared references, which is intentional: a
     * status change on a booking must be visible everywhere.
     *
     * @return list of waiting bookings, oldest first
     */
    public List<Booking> toList() {
        List<Booking> snapshot = new ArrayList<>(size);
        QueueNode current = front;
        while (current != null) {
            snapshot.add(current.getBooking());
            current = current.getNext();
        }
        return snapshot;
    }
}
