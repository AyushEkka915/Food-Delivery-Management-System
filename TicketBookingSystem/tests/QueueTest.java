import java.util.NoSuchElementException;

/**
 * Independent test harness for the CustomQueue implementation (Phase 3).
 *
 * This file only uses the public API of CustomQueue:
 *   new CustomQueue(), enqueue, dequeue, peek, isEmpty, size, displayQueue
 *
 * It is deliberately a plain main() program with no test framework so it can
 * be compiled and run directly from the command line.
 */
public class QueueTest {

    private static int checks = 0;
    private static int failures = 0;

    /** Name used for every generated test customer. */
    private static final String TEST_CUSTOMER_NAME = "Test Customer";

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("        CUSTOM QUEUE TEST HARNESS (PHASE 3)      ");
        System.out.println("=================================================");

        testNewQueue();
        testEmptyQueueBehaviour();
        testEnqueueOne();
        testFifoOrder();
        testSingleRemovalAndReuse();
        testNullEnqueue();
        testStressFifoIntegrity();
        testDisplayQueue();

        System.out.println();
        System.out.println("=================================================");
        System.out.println("RESULT: " + checks + " checks, " + failures + " failed");
        System.out.println("=================================================");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    /** 1. A brand new queue must be empty and report size 0. */
    private static void testNewQueue() {
        section("1. New queue");

        CustomQueue queue = new CustomQueue();
        check("new queue isEmpty() is true", queue.isEmpty());
        check("new queue size() is 0", queue.size() == 0);
    }

    /** 2. Empty-queue operations must throw the documented exception or be safe. */
    private static void testEmptyQueueBehaviour() {
        section("2. Empty queue behaviour");

        CustomQueue queue = new CustomQueue();

        expectThrows("dequeue() on empty queue throws NoSuchElementException",
                NoSuchElementException.class, queue::dequeue);
        expectThrows("peek() on empty queue throws NoSuchElementException",
                NoSuchElementException.class, queue::peek);

        // The display method should print an "empty" message instead of throwing.
        doesNotThrow("displayQueue() on empty queue does not throw", queue::displayQueue);

        // Empty operations must not change the state.
        check("empty queue still reports size 0", queue.size() == 0);
        check("empty queue still reports isEmpty() true", queue.isEmpty());
    }

    /** 3. Enqueueing one booking updates state and peek returns that booking. */
    private static void testEnqueueOne() {
        section("3. Enqueue one booking");

        CustomQueue queue = new CustomQueue();
        Booking b001 = booking("B001", "C001", 2, BookingStatus.WAITING);

        queue.enqueue(b001);

        check("size() is 1 after one enqueue", queue.size() == 1);
        check("isEmpty() is false after one enqueue", !queue.isEmpty());

        Booking peeked = queue.peek();
        check("peek() returns the same booking ID (B001)",
                peeked != null && "B001".equals(peeked.getBookingId()));
        check("peek() does not remove the booking", queue.size() == 1);
    }

    /** 4. Three enqueues must come back out in FIFO order with correct sizes. */
    private static void testFifoOrder() {
        section("4. FIFO order with three bookings");

        CustomQueue queue = new CustomQueue();
        queue.enqueue(booking("B001", "C001", 1, BookingStatus.WAITING));
        queue.enqueue(booking("B002", "C002", 2, BookingStatus.WAITING));
        queue.enqueue(booking("B003", "C003", 3, BookingStatus.WAITING));

        check("size() is 3 after three enqueues", queue.size() == 3);

        Booking first = queue.dequeue();
        check("first dequeue returns B001", first != null && "B001".equals(first.getBookingId()));
        check("size() is 2 after first dequeue", queue.size() == 2);

        Booking second = queue.dequeue();
        check("second dequeue returns B002", second != null && "B002".equals(second.getBookingId()));
        check("size() is 1 after second dequeue", queue.size() == 1);

        Booking third = queue.dequeue();
        check("third dequeue returns B003", third != null && "B003".equals(third.getBookingId()));
        check("size() is 0 after third dequeue", queue.size() == 0);
        check("queue isEmpty() is true after draining", queue.isEmpty());
    }

    /** 5. A queue can be drained to empty and then reused. */
    private static void testSingleRemovalAndReuse() {
        section("5. Single-element removal and reuse");

        CustomQueue queue = new CustomQueue();
        queue.enqueue(booking("B010", "C010", 1, BookingStatus.CONFIRMED));

        Booking removed = queue.dequeue();
        check("dequeue returns B010", removed != null && "B010".equals(removed.getBookingId()));
        check("queue is empty after removing its only booking",
                queue.isEmpty() && queue.size() == 0);

        // Reuse the same queue object for two more bookings.
        queue.enqueue(booking("B011", "C011", 1, BookingStatus.WAITING));
        queue.enqueue(booking("B012", "C012", 1, BookingStatus.WAITING));
        check("reused queue size() is 2", queue.size() == 2);

        Booking first = queue.dequeue();
        check("reused queue dequeue returns B011 first",
                first != null && "B011".equals(first.getBookingId()));
        check("reused queue size() is 1 after first dequeue", queue.size() == 1);

        Booking second = queue.dequeue();
        check("reused queue dequeue returns B012 second",
                second != null && "B012".equals(second.getBookingId()));
        check("reused queue is empty after draining again",
                queue.isEmpty() && queue.size() == 0);
    }

    /** 6. Enqueueing null must be rejected and must not grow the queue. */
    private static void testNullEnqueue() {
        section("6. Null enqueue");

        CustomQueue queue = new CustomQueue();
        queue.enqueue(booking("B020", "C020", 1, BookingStatus.WAITING));
        int sizeBefore = queue.size();

        expectThrows("enqueue(null) throws IllegalArgumentException",
                IllegalArgumentException.class, () -> queue.enqueue(null));

        check("size() is unchanged after rejected null enqueue",
                queue.size() == sizeBefore);
        check("existing booking is still at the front after rejected null enqueue",
                queue.peek() != null && "B020".equals(queue.peek().getBookingId()));
    }

    /**
     * 7. Stress test: 1000 bookings in, 1000 bookings out in the same order.
     * All per-item results are accumulated into booleans so the output stays
     * short instead of printing 1000 PASS lines.
     */
    private static void testStressFifoIntegrity() {
        section("7. Stress test - 1000 bookings (FIFO integrity)");

        final int count = 1000;
        CustomQueue queue = new CustomQueue();

        for (int i = 0; i < count; i++) {
            String bookingId = String.format("B%04d", i);   // B0000 .. B0999
            String customerId = String.format("C%04d", i);  // C0000 .. C0999
            queue.enqueue(booking(bookingId, customerId, (i % 5) + 1, BookingStatus.WAITING));
        }

        boolean enqueueSizeOk = (queue.size() == count);
        boolean fifoOk = true;
        boolean perStepSizeOk = true;
        String firstMismatch = null;

        for (int i = 0; i < count; i++) {
            String expectedId = String.format("B%04d", i);
            Booking actual = queue.dequeue();
            String actualId = (actual == null) ? "<null>" : actual.getBookingId();

            if (!expectedId.equals(actualId)) {
                fifoOk = false;
                if (firstMismatch == null) {
                    firstMismatch = "index " + i + ": expected " + expectedId
                            + " but got " + actualId;
                }
            }

            int expectedSize = count - i - 1;
            if (queue.size() != expectedSize) {
                perStepSizeOk = false;
                if (firstMismatch == null) {
                    firstMismatch = "size after " + (i + 1) + " dequeue(s) was "
                            + queue.size() + ", expected " + expectedSize;
                }
            }
        }

        check("stress: size() is 1000 after 1000 enqueues", enqueueSizeOk);
        check("stress: all 1000 bookings dequeued in FIFO order with correct sizes"
                        + (firstMismatch == null ? "" : " [" + firstMismatch + "]"),
                fifoOk && perStepSizeOk);
        check("stress: queue is empty after draining all 1000 bookings",
                queue.isEmpty() && queue.size() == 0);
    }

    /** 8. displayQueue() prints both WAITING bookings without throwing. */
    private static void testDisplayQueue() {
        section("8. Display two WAITING bookings");

        CustomQueue queue = new CustomQueue();
        queue.enqueue(booking("B100", "C100", 2, BookingStatus.WAITING));
        queue.enqueue(booking("B101", "C101", 3, BookingStatus.WAITING));

        System.out.println("-- displayQueue() output begins --");
        doesNotThrow("displayQueue() with two WAITING bookings does not throw",
                queue::displayQueue);
        System.out.println("-- displayQueue() output ends --");

        // Displaying is read-only.
        check("displayQueue() does not change size()", queue.size() == 2);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Records one boolean assertion as PASS or FAIL. */
    static void check(String label, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("PASS - " + label);
        } else {
            failures++;
            System.out.println("FAIL - " + label);
        }
    }

    /**
     * Runs {@code action} and passes only if it throws an instance of
     * {@code expected}. Any other outcome (including no exception) is a FAIL.
     */
    static void expectThrows(String label, Class<? extends Exception> expected, Runnable action) {
        checks++;
        try {
            action.run();
            failures++;
            System.out.println("FAIL - " + label
                    + " (expected " + expected.getSimpleName()
                    + " but no exception was thrown)");
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                System.out.println("PASS - " + label);
            } else {
                failures++;
                System.out.println("FAIL - " + label
                        + " (expected " + expected.getSimpleName()
                        + " but got " + actual.getClass().getSimpleName()
                        + ": " + actual.getMessage() + ")");
            }
        }
    }

    /** Runs {@code action} and passes only if it completes without throwing. */
    static void doesNotThrow(String label, Runnable action) {
        checks++;
        try {
            action.run();
            System.out.println("PASS - " + label);
        } catch (Throwable actual) {
            failures++;
            System.out.println("FAIL - " + label
                    + " (unexpected " + actual.getClass().getSimpleName()
                    + ": " + actual.getMessage() + ")");
        }
    }

    /** Builds a booking with a freshly created customer for the given customer ID. */
    static Booking booking(String id, String customerId, int tickets, BookingStatus status) {
        return new Booking(id, new Customer(customerId, TEST_CUSTOMER_NAME), tickets, status);
    }

    /** Prints a section heading. */
    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }
}
