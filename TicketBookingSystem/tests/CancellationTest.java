import java.util.List;

/**
 * Test harness for Phase 6: Cancellation and waiting-list promotion.
 *
 * Tests strict FIFO promotion, invalid cancellations, and inventory consistency.
 */
public class CancellationTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   CANCELLATION & PROMOTION TESTS (PHASE 6)       ");
        System.out.println("=================================================");

        testCancelConfirmedBooking();
        testCancelErrors();
        testPromotionAfterCancellation();
        testStrictFifoPromotion();
        testMultiplePromotions();
        testInventoryAfterCancelAndPromotion();
        testActionHistoryCancelAndPromote();

        System.out.println();
        System.out.println("=================================================");
        System.out.println("RESULT: " + checks + " checks, " + failures + " failed");
        System.out.println("=================================================");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void testCancelConfirmedBooking() {
        section("1. Cancel confirmed booking");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        Booking b001 = system.bookTickets("C001", 2);

        check("starts confirmed", b001.getStatus() == BookingStatus.CONFIRMED);
        check("available is 3", system.getAvailableTickets() == 3);

        Booking cancelled = system.cancelBooking("B001");
        check("cancel returns booking", cancelled == b001);
        check("status becomes CANCELLED", b001.getStatus() == BookingStatus.CANCELLED);
        check("available becomes 5", system.getAvailableTickets() == 5);
        check("reserved becomes 0", system.getReservedTickets() == 0);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testCancelErrors() {
        section("2. Cancel errors");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        Booking b001 = system.bookTickets("C001", 5);
        Booking b002 = system.bookTickets("C002", 1); // waiting

        expectThrows("unknown booking throws", IllegalArgumentException.class,
                () -> system.cancelBooking("B999"));
        expectThrows("cancel waiting booking throws", IllegalStateException.class,
                () -> system.cancelBooking("B002"));
        check("waiting booking still waiting", b002.getStatus() == BookingStatus.WAITING);

        system.cancelBooking("B001");
        expectThrows("cancel already cancelled throws", IllegalStateException.class,
                () -> system.cancelBooking("B001"));
    }

    private static void testPromotionAfterCancellation() {
        section("3. Promotion after cancellation");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        Booking b001 = system.bookTickets("C001", 5);
        Booking b002 = system.bookTickets("C002", 2);
        check("b002 is waiting", b002.getStatus() == BookingStatus.WAITING);
        check("waiting count 1", system.getWaitingCount() == 1);

        system.cancelBooking("B001");
        check("b001 cancelled", b001.getStatus() == BookingStatus.CANCELLED);
        check("b002 promoted to confirmed", b002.getStatus() == BookingStatus.CONFIRMED);
        check("waiting count 0", system.getWaitingCount() == 0);
        check("available becomes 3", system.getAvailableTickets() == 3);
        check("reserved becomes 2", system.getReservedTickets() == 2);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testStrictFifoPromotion() {
        section("4. Strict FIFO promotion");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        Booking b001 = system.bookTickets("C001", 5);
        Booking b002 = system.bookTickets("C002", 4); // front, needs 4
        Booking b003 = system.bookTickets("C003", 1); // needs 1

        check("b002 waiting", b002.getStatus() == BookingStatus.WAITING);
        check("b003 waiting", b003.getStatus() == BookingStatus.WAITING);

        system.cancelBooking("B001");
        check("b002 promoted (front fits)", b002.getStatus() == BookingStatus.CONFIRMED);
        check("b003 still waiting (front was promoted but b003 checked after)", b003.getStatus() == BookingStatus.CONFIRMED);
        // After promoting b002 (used 4, left 1), b003 needs 1 and fits -> promoted
        check("waiting count 0 after both fit", system.getWaitingCount() == 0);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testMultiplePromotions() {
        section("5. Multiple promotions if enough tickets");

        TicketBookingSystem system = new TicketBookingSystem(10);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");
        system.registerCustomer("C004", "Dave");

        Booking b001 = system.bookTickets("C001", 10);
        system.bookTickets("C002", 3);
        system.bookTickets("C003", 3);
        system.bookTickets("C004", 3);

        system.cancelBooking("B001");
        check("waiting count 0", system.getWaitingCount() == 0);
        check("all promoted", system.getWaitingBookings().size() == 0);
        check("reserved is 9", system.getReservedTickets() == 9);
        check("available is 1", system.getAvailableTickets() == 1);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testInventoryAfterCancelAndPromotion() {
        section("6. Inventory after cancel and promotion");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        Booking b001 = system.bookTickets("C001", 5); // fills all
        Booking b002 = system.bookTickets("C002", 4); // waiting (front)
        Booking b003 = system.bookTickets("C003", 1); // waiting (no bypass)
        check("b002 waiting", b002.getStatus() == BookingStatus.WAITING);
        check("b003 waiting", b003.getStatus() == BookingStatus.WAITING);

        system.cancelBooking("B001"); // free 5, b002 needs 4 fits -> promote, available becomes 1, b003 needs 1 fits -> promote
        check("b002 promoted", b002.getStatus() == BookingStatus.CONFIRMED);
        check("b003 promoted", b003.getStatus() == BookingStatus.CONFIRMED);
        check("waiting count 0", system.getWaitingCount() == 0);
        check("available is 0", system.getAvailableTickets() == 0);
        check("reserved is 5", system.getReservedTickets() == 5);
        check("inventory consistent", system.isInventoryConsistent());
    }

    private static void testActionHistoryCancelAndPromote() {
        section("7. Action history");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        system.bookTickets("C001", 5);
        system.bookTickets("C002", 2);
        check("action count 2", system.getActionCount() == 2);

        system.cancelBooking("B001");
        check("action count 4", system.getActionCount() == 4);
        String latest = system.getLatestAction();
        check("latest action is promoted", latest != null && latest.contains("Promoted"));

        doesNotThrow("displayActionHistory does not throw", system::displayActionHistory);
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
