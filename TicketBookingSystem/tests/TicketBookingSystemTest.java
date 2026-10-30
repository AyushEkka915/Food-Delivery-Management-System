import java.util.List;

/**
 * Test harness for Phase 5: TicketBookingSystem (reservation engine).
 *
 * Covers inventory, ID generation, confirmation/waitlist with strict no-bypass,
 * validation, search, and history. Also verifies the inventory invariant.
 */
public class TicketBookingSystemTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   TICKET BOOKING SYSTEM TESTS (PHASE 5)          ");
        System.out.println("=================================================");

        testConstructor();
        testCustomers();
        testConfirmedBooking();
        testWaitingBooking();
        testNoBypassPolicy();
        testValidation();
        testIdGeneration();
        testSearchAndMultipleBookings();
        testInventoryInvariant();
        testActionHistory();
        testQueueSnapshot();

        System.out.println();
        System.out.println("=================================================");
        System.out.println("RESULT: " + checks + " checks, " + failures + " failed");
        System.out.println("=================================================");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void testConstructor() {
        section("1. Constructor");

        expectThrows("capacity 0 throws", IllegalArgumentException.class,
                () -> new TicketBookingSystem(0));
        expectThrows("capacity -5 throws", IllegalArgumentException.class,
                () -> new TicketBookingSystem(-5));

        TicketBookingSystem system = new TicketBookingSystem(10);
        check("initial available is 10", system.getAvailableTickets() == 10);
        check("initial reserved is 0", system.getReservedTickets() == 0);
        check("initial waiting count is 0", system.getWaitingCount() == 0);
        check("initial waiting tickets is 0", system.getWaitingTickets() == 0);
        check("inventory consistent initially", system.isInventoryConsistent());
    }

    private static void testCustomers() {
        section("2. Customers");

        TicketBookingSystem system = new TicketBookingSystem(10);
        Customer alice = system.registerCustomer("C001", "Alice");
        check("register returns customer", alice != null);
        check("customer count is 1", system.getCustomerCount() == 1);
        check("findCustomer returns alice", system.findCustomer("C001") == alice);

        expectThrows("duplicate customer throws", IllegalArgumentException.class,
                () -> system.registerCustomer("C001", "Alice2"));
        check("customer count still 1", system.getCustomerCount() == 1);

        expectThrows("unknown customer books", IllegalArgumentException.class,
                () -> system.bookTickets("C999", 1));
        check("unknown lookup returns null", system.findCustomer("C999") == null);
    }

    private static void testConfirmedBooking() {
        section("3. Confirmed booking");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");

        Booking b001 = system.bookTickets("C001", 2);
        check("first booking returns booking", b001 != null);
        check("status is CONFIRMED", b001.getStatus() == BookingStatus.CONFIRMED);
        check("booking ID is B001", "B001".equals(b001.getBookingId()));
        check("available becomes 3", system.getAvailableTickets() == 3);
        check("reserved becomes 2", system.getReservedTickets() == 2);
        check("waiting count is 0", system.getWaitingCount() == 0);
        check("inventory consistent", system.isInventoryConsistent());

        Booking b002 = system.bookTickets("C001", 3);
        check("second booking status CONFIRMED", b002.getStatus() == BookingStatus.CONFIRMED);
        check("available becomes 0", system.getAvailableTickets() == 0);
        check("reserved becomes 5", system.getReservedTickets() == 5);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testWaitingBooking() {
        section("4. Waiting booking");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        system.bookTickets("C001", 5);
        check("available is 0 after filling", system.getAvailableTickets() == 0);

        Booking b002 = system.bookTickets("C002", 2);
        check("status is WAITING when not enough tickets", b002.getStatus() == BookingStatus.WAITING);
        check("available still 0", system.getAvailableTickets() == 0);
        check("reserved still 5", system.getReservedTickets() == 5);
        check("waiting count is 1", system.getWaitingCount() == 1);
        check("waiting tickets are 2", system.getWaitingTickets() == 2);
        check("waiting booking searchable", system.searchBooking("B002") == b002);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testNoBypassPolicy() {
        section("5. Strict no-bypass");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        system.bookTickets("C001", 2);
        system.bookTickets("C002", 4);
        check("queue has 1 after first wait", system.getWaitingCount() == 1);

        Booking b003 = system.bookTickets("C003", 2);
        check("C003 waits even if tickets fit (no bypass)", b003.getStatus() == BookingStatus.WAITING);
        check("waiting count is 2", system.getWaitingCount() == 2);
        check("available still 3", system.getAvailableTickets() == 3);
        check("reserved still 2", system.getReservedTickets() == 2);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testValidation() {
        section("6. Validation");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");

        expectThrows("tickets 0 throws", IllegalArgumentException.class,
                () -> system.bookTickets("C001", 0));
        expectThrows("tickets -1 throws", IllegalArgumentException.class,
                () -> system.bookTickets("C001", -1));
        expectThrows("tickets > capacity throws", IllegalArgumentException.class,
                () -> system.bookTickets("C001", 6));
        check("system state unchanged after invalid requests", system.getAvailableTickets() == 5);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testIdGeneration() {
        section("7. ID generation");

        TicketBookingSystem system = new TicketBookingSystem(10);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        Booking b001 = system.bookTickets("C001", 1);
        Booking b002 = system.bookTickets("C001", 1);
        system.bookTickets("C002", 5); // fills remaining
        Booking b004 = system.bookTickets("C002", 2); // now waits

        check("first ID B001", "B001".equals(b001.getBookingId()));
        check("second B002", "B002".equals(b002.getBookingId()));
        check("fourth ID B004", "B004".equals(b004.getBookingId()));
        check("all IDs unique", !b001.getBookingId().equals(b002.getBookingId())
                && !b002.getBookingId().equals(b004.getBookingId()));
    }

    private static void testSearchAndMultipleBookings() {
        section("8. Search and multiple bookings per customer");

        TicketBookingSystem system = new TicketBookingSystem(10);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        Booking b001 = system.bookTickets("C001", 2);
        Booking b002 = system.bookTickets("C002", 3);

        check("search finds confirmed B001", system.searchBooking("B001") == b001);
        check("search finds confirmed B002", system.searchBooking("B002") == b002);
        check("unknown returns null", system.searchBooking("B999") == null);
        check("Alice has two confirmed bookings", system.getConfirmedBookings().size() == 2);

        // Same customer can book again
        Booking b003 = system.bookTickets("C001", 1);
        check("same customer gets third booking", "B003".equals(b003.getBookingId()));
        check("customer count still 2", system.getCustomerCount() == 2);
    }

    private static void testInventoryInvariant() {
        section("9. Inventory invariant");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        system.bookTickets("C001", 2);
        check("invariant after B001", system.isInventoryConsistent());
        system.bookTickets("C002", 4);
        check("invariant after B002 wait", system.isInventoryConsistent());
        system.bookTickets("C003", 2);
        check("invariant after B003 wait", system.isInventoryConsistent());
        check("reserved is only confirmed", system.getReservedTickets() == 2);
        check("waiting tickets not in reserved", system.getWaitingTickets() == 6);
    }

    private static void testActionHistory() {
        section("10. Action history");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");

        system.bookTickets("C001", 1);
        check("action count is 1", system.getActionCount() == 1);
        String latest = system.getLatestAction();
        check("latest action mentions B001", latest != null && latest.contains("B001"));
        check("latest action is confirmed", latest != null && latest.contains("Confirmed"));

        system.bookTickets("C001", 4); // fills rest, available becomes 0
        check("action count is 2", system.getActionCount() == 2);
        String latest2 = system.getLatestAction();
        check("latest action is confirmed", latest2 != null && latest2.contains("Confirmed"));

        // Now all filled, next request waits
        system.bookTickets("C001", 1);
        check("action count is 3", system.getActionCount() == 3);
        String latest3 = system.getLatestAction();
        check("latest action is waitlisted", latest3 != null && latest3.contains("Waitlisted"));

        doesNotThrow("displayActionHistory does not throw", system::displayActionHistory);
    }

    private static void testQueueSnapshot() {
        section("11. Queue snapshot");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        system.bookTickets("C001", 5);
        system.bookTickets("C002", 1);
        system.bookTickets("C003", 1);

        List<Booking> waiting = system.getWaitingBookings();
        check("getWaitingBookings has 2", waiting.size() == 2);
        check("first in snapshot is B002", waiting.get(0).getBookingId().equals("B002"));
        check("second in snapshot is B003", waiting.get(1).getBookingId().equals("B003"));
        doesNotThrow("waiting list display does not throw", system::displayWaitingList);
    }

    // Helpers
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

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }
}
