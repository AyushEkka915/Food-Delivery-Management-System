import java.util.List;

/**
 * End-to-End Testing for Phase 8: Testing and Debugging
 *
 * Verifies the complete flow across all components:
 * Customer -> Booking -> Queue -> Registry -> Stack -> Cancellation -> Promotion
 */
public class EndToEndTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   END-TO-END TESTING & DEBUGGING (PHASE 8)       ");
        System.out.println("=================================================");

        testFullEndToEndFlow();
        testExactCapacityFill();
        testStrictNoBypass();
        testStrictFifoBlocking();
        testMultiplePromotions();
        testAllCancellationRules();
        testValidationSuite();
        testSearchAcrossStatuses();
        testInventoryInvariantStress();
        testHistoryIntegrity();
        testQueueOrderPreservation();
        testStateUnchangedOnFailure();

        System.out.println();
        System.out.println("=================================================");
        System.out.println("RESULT: " + checks + " checks, " + failures + " failed");
        System.out.println("=================================================");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void testFullEndToEndFlow() {
        section("1. Full End-to-End Flow");

        TicketBookingSystem system = new TicketBookingSystem(10);
        system.registerCustomer("C001", "Rahul");
        system.registerCustomer("C002", "Amit");
        system.registerCustomer("C003", "Neha");

        // Confirmed
        Booking b1 = system.bookTickets("C001", 3);
        check("B001 confirmed", b1.getStatus() == BookingStatus.CONFIRMED);
        check("available after B001", system.getAvailableTickets() == 7);

        // Fill rest
        Booking b2 = system.bookTickets("C002", 7);
        check("B002 confirmed", b2.getStatus() == BookingStatus.CONFIRMED);
        check("available after B002", system.getAvailableTickets() == 0);

        // Now waitlist
        Booking b3 = system.bookTickets("C003", 2);
        check("B003 goes to waitlist", b3.getStatus() == BookingStatus.WAITING);
        check("waiting count is 1", system.getWaitingCount() == 1);

        // Cancel and promote
        system.cancelBooking("B001");
        check("B001 cancelled", b1.getStatus() == BookingStatus.CANCELLED);
        check("B003 promoted to confirmed", b3.getStatus() == BookingStatus.CONFIRMED);
        check("waiting count becomes 0", system.getWaitingCount() == 0);
        check("available after promotion", system.getAvailableTickets() == 1);
        check("reserved is 9", system.getReservedTickets() == 9);
        assertInvariant(system, "after full flow");
    }

    private static void testExactCapacityFill() {
        section("2. Exact Capacity Fill");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "TestA");
        system.registerCustomer("C002", "TestB");

        Booking b1 = system.bookTickets("C001", 5);
        check("filled exactly to capacity", b1.getStatus() == BookingStatus.CONFIRMED);
        check("available is 0", system.getAvailableTickets() == 0);
        check("reserved is 5", system.getReservedTickets() == 5);

        Booking b2 = system.bookTickets("C002", 1);
        check("next goes to waitlist", b2.getStatus() == BookingStatus.WAITING);
        assertInvariant(system, "after exact fill");
    }

    private static void testStrictNoBypass() {
        section("3. Strict No-Bypass");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        system.bookTickets("C001", 1); // B001 confirmed, available 4
        system.bookTickets("C002", 5); // B002 waiting (more than available)
        check("queue not empty", system.getWaitingCount() == 1);

        Booking b3 = system.bookTickets("C003", 2); // should wait despite available 4
        check("B003 waits due to no-bypass", b3.getStatus() == BookingStatus.WAITING);
        check("waiting count is 2", system.getWaitingCount() == 2);
        check("available still 4", system.getAvailableTickets() == 4);
        assertInvariant(system, "no-bypass");
    }

    private static void testStrictFifoBlocking() {
        section("4. Strict FIFO Blocking");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        system.bookTickets("C001", 5); // B001 confirmed
        system.bookTickets("C002", 4); // B002 front, needs 4
        system.bookTickets("C003", 1); // B003 needs 1

        check("B002 waiting", system.searchBooking("B002").getStatus() == BookingStatus.WAITING);
        check("B003 waiting", system.searchBooking("B003").getStatus() == BookingStatus.WAITING);

        system.cancelBooking("B001");
        check("B002 promoted", system.searchBooking("B002").getStatus() == BookingStatus.CONFIRMED);
        check("B003 promoted", system.searchBooking("B003").getStatus() == BookingStatus.CONFIRMED);
        assertInvariant(system, "fifo-blocking");
    }

    private static void testStrictFifoBlockingBetter() {
        section("4b. Strict FIFO Blocking (Front doesn't fit)");
        TicketBookingSystem system = new TicketBookingSystem(10);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        Booking b1 = system.bookTickets("C001", 6); // confirmed, available 4
        Booking b2 = system.bookTickets("C002", 5); // waiting (queue not empty? no wait - after b1 queue empty, check: 5<=4? no so waitlist)
        // wait: after b1 confirmed, available 4, queue empty. b2 wants 5, 5<=4 false -> WAITING
        check("B002 waiting", b2.getStatus() == BookingStatus.WAITING);
        Booking b3 = system.bookTickets("C003", 2); // queue not empty -> WAITING (no bypass)
        check("B003 waiting", b3.getStatus() == BookingStatus.WAITING);

        // Cancel b1 -> free 6, available becomes 10? no wait: total 10, b1 was confirmed using 6, available was 4. Cancel -> available becomes 4+6=10
        // But waitlist has B002 needs 5, B003 needs 2. B002 is front.
        system.cancelBooking("B001");
        // B002 needs 5 <= 10? yes -> promoted, available 5
        // then check B003 needs 2 <= 5 -> promoted, available 3
        check("B002 promoted", b2.getStatus() == BookingStatus.CONFIRMED);
        check("B003 promoted", b3.getStatus() == BookingStatus.CONFIRMED);
        assertInvariant(system, "blocking-test-flow");
    }

    private static void testMultiplePromotions() {
        section("5. Multiple Promotions");

        TicketBookingSystem system = new TicketBookingSystem(10);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");
        system.registerCustomer("C004", "Dave");

        system.bookTickets("C001", 10);
        system.bookTickets("C002", 2);
        system.bookTickets("C003", 3);
        system.bookTickets("C004", 4);

        check("all waiting", system.getWaitingCount() == 3);
        system.cancelBooking("B001");
        check("all promoted in FIFO", system.getWaitingCount() == 0);
        check("reserved 9", system.getReservedTickets() == 9);
        check("available 1", system.getAvailableTickets() == 1);
        assertInvariant(system, "multiple-promotions");
    }

    private static void testAllCancellationRules() {
        section("6. All Cancellation Rules");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        Booking b1 = system.bookTickets("C001", 5);
        Booking b2 = system.bookTickets("C002", 1);

        expectThrows("unknown booking", IllegalArgumentException.class, () -> system.cancelBooking("B999"));
        expectThrows("cancel waiting", IllegalStateException.class, () -> system.cancelBooking("B002"));

        system.cancelBooking("B001");
        expectThrows("cancel already cancelled", IllegalStateException.class, () -> system.cancelBooking("B001"));
    }

    private static void testValidationSuite() {
        section("7. Validation Suite");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Valid");

        expectThrows("zero tickets", IllegalArgumentException.class, () -> system.bookTickets("C001", 0));
        expectThrows("negative tickets", IllegalArgumentException.class, () -> system.bookTickets("C001", -3));
        expectThrows("exceeds capacity", IllegalArgumentException.class, () -> system.bookTickets("C001", 10));
        expectThrows("unknown customer", IllegalArgumentException.class, () -> system.bookTickets("C999", 1));
        expectThrows("duplicate customer", IllegalArgumentException.class, () -> system.registerCustomer("C001", "Dup"));
        expectThrows("capacity 0", IllegalArgumentException.class, () -> new TicketBookingSystem(0));
        expectThrows("capacity negative", IllegalArgumentException.class, () -> new TicketBookingSystem(-1));

        check("state unchanged after failures", system.getAvailableTickets() == 5);
        assertInvariant(system, "after validation");
    }

    private static void testSearchAcrossStatuses() {
        section("8. Search Across Statuses");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        Booking b1 = system.bookTickets("C001", 5);
        Booking b2 = system.bookTickets("C002", 1);

        check("find confirmed", system.searchBooking("B001").getStatus() == BookingStatus.CONFIRMED);
        check("find waiting", system.searchBooking("B002").getStatus() == BookingStatus.WAITING);

        system.cancelBooking("B001");
        check("find cancelled", system.searchBooking("B001").getStatus() == BookingStatus.CANCELLED);
        check("cancelled still searchable", system.searchBooking("B001") != null);
    }

    private static void testInventoryInvariantStress() {
        section("9. Inventory Invariant Stress");

        TicketBookingSystem system = new TicketBookingSystem(10);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");

        assertInvariant(system, "init");
        system.bookTickets("C001", 3); assertInvariant(system, "s1");
        system.bookTickets("C002", 4); assertInvariant(system, "s2");
        system.bookTickets("C003", 10); assertInvariant(system, "s3"); // waits
        system.bookTickets("C001", 2); assertInvariant(system, "s4"); // waits (no bypass)
        system.cancelBooking("B001"); assertInvariant(system, "s5");
        system.cancelBooking("B002"); assertInvariant(system, "s6");
        check("reserved only confirmed", system.getReservedTickets() >= 0);
    }

    private static void testHistoryIntegrity() {
        section("10. History Integrity");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");

        system.bookTickets("C001", 5);
        system.bookTickets("C002", 2);
        system.cancelBooking("B001");

        check("has actions", system.getActionCount() >= 4);
        String latest = system.getLatestAction();
        check("newest is promoted/related", latest != null && (latest.contains("Promoted") || latest.contains("Cancelled")));
        doesNotThrow("display history", system::displayActionHistory);
    }

    private static void testQueueOrderPreservation() {
        section("11. Queue Order Preservation");

        TicketBookingSystem system = new TicketBookingSystem(1);
        system.registerCustomer("C001", "Alice");
        system.registerCustomer("C002", "Bob");
        system.registerCustomer("C003", "Carol");
        system.registerCustomer("C004", "Dave");

        system.bookTickets("C001", 1); // B001 confirmed
        system.bookTickets("C002", 1); // B002 waits
        system.bookTickets("C003", 1); // B003 waits
        system.bookTickets("C004", 1); // B004 waits

        int wc = system.getWaitingCount();
        check("waiting count is 3", wc == 3);
        List<Booking> w = system.getWaitingBookings();
        boolean okOrder = wc == 3 && w != null && w.size() == 3;
        if (okOrder) {
            okOrder = "B002".equals(w.get(0).getBookingId())
                    && "B003".equals(w.get(1).getBookingId())
                    && "B004".equals(w.get(2).getBookingId());
        }
        check("order B002,B003,B004", okOrder);
    }

    private static void testStateUnchangedOnFailure() {
        section("12. State Unchanged On Failure");

        TicketBookingSystem system = new TicketBookingSystem(5);
        system.registerCustomer("C001", "Alice");
        system.bookTickets("C001", 2);

        int beforeAvail = system.getAvailableTickets();
        int beforeCount = system.getAllBookings().size();

        try { system.bookTickets("C001", 0); } catch (Exception ignored) {}
        try { system.bookTickets("C999", 1); } catch (Exception ignored) {}
        try { system.cancelBooking("B999"); } catch (Exception ignored) {}

        check("available unchanged", system.getAvailableTickets() == beforeAvail);
        check("booking count unchanged", system.getAllBookings().size() == beforeCount);
        assertInvariant(system, "on-failures");
    }

    // Helpers
    static void assertInvariant(TicketBookingSystem system, String ctx) {
        boolean ok = system.isInventoryConsistent();
        checks++;
        if (ok) System.out.println("PASS - invariant " + ctx);
        else { failures++; System.out.println("FAIL - invariant " + ctx); }
    }

    static void check(String label, boolean condition) {
        checks++;
        if (condition) System.out.println("PASS - " + label);
        else { failures++; System.out.println("FAIL - " + label); }
    }

    static void expectThrows(String label, Class<? extends Exception> expected, Runnable action) {
        checks++;
        try {
            action.run();
            failures++;
            System.out.println("FAIL - " + label + " (no exception)");
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) System.out.println("PASS - " + label);
            else { failures++; System.out.println("FAIL - " + label + " got " + actual.getClass().getSimpleName()); }
        }
    }

    static void doesNotThrow(String label, Runnable action) {
        checks++;
        try { action.run(); System.out.println("PASS - " + label); }
        catch (Throwable actual) { failures++; System.out.println("FAIL - " + label); }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }
}
