import java.util.List;
import java.util.NoSuchElementException;

/**
 * Independent test harness for Phase 4: ActionStack and BookingRegistry.
 *
 * Plain main() program with no external test framework so it can be compiled
 * and run directly from the command line.
 */
public class StackAndRegistryTest {

    private static int checks = 0;
    private static int failures = 0;

    private static final String TEST_CUSTOMER_NAME = "Test Customer";

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   ACTION STACK + BOOKING REGISTRY TESTS (PHASE 4)");
        System.out.println("=================================================");

        testNewStack();
        testStackLifo();
        testStackValidation();
        testStackCapacity();
        testStackDisplay();

        testEmptyRegistry();
        testRegistryAddAndFind();
        testRegistryDuplicate();
        testRegistryCopySafety();
        testRegistryStatusVisibility();

        System.out.println();
        System.out.println("=================================================");
        System.out.println("RESULT: " + checks + " checks, " + failures + " failed");
        System.out.println("=================================================");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // ActionStack tests
    // ------------------------------------------------------------------

    private static void testNewStack() {
        section("1. New stack");

        ActionStack stack = new ActionStack(3);
        check("new stack isEmpty() is true", stack.isEmpty());
        check("new stack size() is 0", stack.size() == 0);
        check("new stack isFull() is false", !stack.isFull());
        check("new stack capacity() is 3", stack.capacity() == 3);
    }

    private static void testStackLifo() {
        section("2. LIFO order");

        ActionStack stack = new ActionStack(5);
        stack.push("First");
        stack.push("Second");
        stack.push("Third");

        check("peek() returns newest action (Third)", "Third".equals(stack.peek()));
        check("size() is 3 after three pushes", stack.size() == 3);

        check("pop() returns Third", "Third".equals(stack.pop()));
        check("pop() returns Second", "Second".equals(stack.pop()));
        check("pop() returns First", "First".equals(stack.pop()));
        check("stack is empty after three pops", stack.isEmpty() && stack.size() == 0);
    }

    private static void testStackValidation() {
        section("3. Invalid stack operations");

        ActionStack stack = new ActionStack(3);
        expectThrows("push(null) throws IllegalArgumentException",
                IllegalArgumentException.class, () -> stack.push(null));
        expectThrows("push(blank) throws IllegalArgumentException",
                IllegalArgumentException.class, () -> stack.push("   "));
        check("rejected pushes leave size 0", stack.size() == 0);

        expectThrows("pop() on empty throws NoSuchElementException",
                NoSuchElementException.class, stack::pop);
        expectThrows("peek() on empty throws NoSuchElementException",
                NoSuchElementException.class, stack::peek);
    }

    private static void testStackCapacity() {
        section("4. Fixed capacity");

        ActionStack stack = new ActionStack(2);
        stack.push("A");
        stack.push("B");

        check("stack with 2/2 slots is full", stack.isFull());
        expectThrows("push beyond capacity throws IllegalStateException",
                IllegalStateException.class, () -> stack.push("C"));
        check("size stays 2 after rejected push", stack.size() == 2);
        check("top is still B after rejected push", "B".equals(stack.peek()));
    }

    private static void testStackDisplay() {
        section("5. Display history");

        ActionStack stack = new ActionStack(5);
        System.out.println("-- displayHistory() on empty --");
        doesNotThrow("displayHistory() on empty does not throw", stack::displayHistory);

        stack.push("Confirmed booking B001");
        stack.push("Cancelled booking B001");

        System.out.println("-- displayHistory() with two actions --");
        doesNotThrow("displayHistory() with actions does not throw", stack::displayHistory);
        check("displayHistory() does not change size", stack.size() == 2);
    }

    // ------------------------------------------------------------------
    // BookingRegistry tests
    // ------------------------------------------------------------------

    private static void testEmptyRegistry() {
        section("6. Empty registry");

        BookingRegistry registry = new BookingRegistry();
        check("new registry size() is 0", registry.size() == 0);
        check("new registry isEmpty() is true", registry.isEmpty());
        check("unknown ID returns null", registry.findById("B999") == null);
        check("unknown ID contains() is false", !registry.contains("B999"));
        check("null ID lookup returns null", registry.findById(null) == null);
        check("blank ID lookup returns null", registry.findById("   ") == null);
        check("getAll() is empty", registry.getAll().isEmpty());
        check("getByStatus(CONFIRMED) is empty",
                registry.getByStatus(BookingStatus.CONFIRMED).isEmpty());
    }

    private static void testRegistryAddAndFind() {
        section("7. Add and find");

        BookingRegistry registry = new BookingRegistry();
        Booking b001 = booking("B001", "C001", 2, BookingStatus.CONFIRMED);
        registry.add(b001);

        check("size() is 1 after add", registry.size() == 1);
        check("isEmpty() is false after add", !registry.isEmpty());
        check("contains(B001) is true", registry.contains("B001"));
        check("findById() returns the same object", registry.findById("B001") == b001);
        check("getAll() has one booking", registry.getAll().size() == 1);
        check("getByStatus(CONFIRMED) has one booking",
                registry.getByStatus(BookingStatus.CONFIRMED).size() == 1);
        check("getByStatus(WAITING) is empty",
                registry.getByStatus(BookingStatus.WAITING).isEmpty());
    }

    private static void testRegistryDuplicate() {
        section("8. Duplicate and null bookings");

        BookingRegistry registry = new BookingRegistry();
        Booking original = booking("B001", "C001", 2, BookingStatus.CONFIRMED);
        registry.add(original);

        Booking duplicate = booking("B001", "C002", 5, BookingStatus.WAITING);
        expectThrows("duplicate booking ID throws IllegalArgumentException",
                IllegalArgumentException.class, () -> registry.add(duplicate));
        check("size stays 1 after duplicate add", registry.size() == 1);
        check("original booking is still stored", registry.findById("B001") == original);

        expectThrows("null booking throws IllegalArgumentException",
                IllegalArgumentException.class, () -> registry.add(null));
        check("size stays 1 after null add", registry.size() == 1);
    }

    private static void testRegistryCopySafety() {
        section("9. Returned lists are copies");

        BookingRegistry registry = new BookingRegistry();
        registry.add(booking("B001", "C001", 1, BookingStatus.CONFIRMED));
        registry.add(booking("B002", "C002", 2, BookingStatus.CONFIRMED));

        List<Booking> all = registry.getAll();
        all.clear();
        check("clearing getAll() copy does not change registry", registry.size() == 2);

        List<Booking> confirmed = registry.getByStatus(BookingStatus.CONFIRMED);
        confirmed.clear();
        check("clearing getByStatus() copy does not change registry", registry.size() == 2);
    }

    private static void testRegistryStatusVisibility() {
        section("10. Status changes are visible through the registry");

        BookingRegistry registry = new BookingRegistry();
        Customer alice = new Customer("C001", "Alice");
        Booking confirmed = new Booking("B001", alice, 2, BookingStatus.CONFIRMED);
        Booking waiting = new Booking("B002", alice, 1, BookingStatus.WAITING);
        registry.add(confirmed);
        registry.add(waiting);

        check("one CONFIRMED initially",
                registry.getByStatus(BookingStatus.CONFIRMED).size() == 1);
        check("one WAITING initially",
                registry.getByStatus(BookingStatus.WAITING).size() == 1);

        waiting.updateStatus(BookingStatus.CONFIRMED);
        check("promotion is visible through findById",
                registry.findById("B002").getStatus() == BookingStatus.CONFIRMED);
        check("two CONFIRMED after promotion",
                registry.getByStatus(BookingStatus.CONFIRMED).size() == 2);
        check("no WAITING after promotion",
                registry.getByStatus(BookingStatus.WAITING).isEmpty());

        confirmed.updateStatus(BookingStatus.CANCELLED);
        check("cancellation is visible through findById",
                registry.findById("B001").getStatus() == BookingStatus.CANCELLED);
        check("one CONFIRMED after cancellation",
                registry.getByStatus(BookingStatus.CONFIRMED).size() == 1);
        check("cancelled booking remains searchable", registry.contains("B001"));
        check("registry size still counts cancelled record", registry.size() == 2);

        expectThrows("cancelled booking ID cannot be reused",
                IllegalArgumentException.class,
                () -> registry.add(booking("B001", "C003", 1, BookingStatus.CONFIRMED)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    static void check(String label, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("PASS - " + label);
        } else {
            failures++;
            System.out.println("FAIL - " + label);
        }
    }

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

    static Booking booking(String id, String customerId, int tickets, BookingStatus status) {
        return new Booking(id, new Customer(customerId, TEST_CUSTOMER_NAME), tickets, status);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }
}
