import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static TicketBookingSystem system = null;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   TICKET BOOKING QUEUE SYSTEM USING JAVA & DSA   ");
        System.out.println("=================================================");
        System.out.println("Welcome! Start by initializing the system (Option 1).");
        System.out.println();

        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1 -> initializeSystem();
                case 2 -> registerCustomer();
                case 3 -> bookTickets();
                case 4 -> cancelBooking();
                case 5 -> searchBooking();
                case 6 -> viewAllBookings();
                case 7 -> viewConfirmedBookings();
                case 8 -> viewWaitingList();
                case 9 -> viewActionHistory();
                case 10 -> viewSystemSummary();
                case 11 -> {
                    System.out.println("\nThank you for using Ticket Booking Queue System. Exiting...");
                    running = false;
                }
                default -> System.out.println("\n[ERROR] Invalid choice! Please enter a number between 1-11.");
            }

            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }

        scanner.close();
    }

    private static void displayMenu() {
        System.out.println("\n=================================================");
        System.out.println("                    MAIN MENU                    ");
        System.out.println("=================================================");
        System.out.println("1.  Initialize / Set Ticket Capacity");
        System.out.println("2.  Register Customer");
        System.out.println("3.  Book Tickets");
        System.out.println("4.  Cancel Booking");
        System.out.println("5.  Search Booking by Booking ID");
        System.out.println("6.  View All Bookings");
        System.out.println("7.  View Confirmed Bookings");
        System.out.println("8.  View Waiting List");
        System.out.println("9.  View Action History");
        System.out.println("10. View System Summary");
        System.out.println("11. Exit");
        System.out.println("=================================================");
    }

    private static void initializeSystem() {
        System.out.println("\n--- INITIALIZE SYSTEM ---");
        int capacity = getIntInput("Enter total ticket capacity (e.g., 10): ");

        try {
            system = new TicketBookingSystem(capacity);
            System.out.println("\n[SUCCESS] System initialized with " + capacity + " tickets.");
            System.out.println("Available Tickets: " + system.getAvailableTickets());
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private static void registerCustomer() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- REGISTER CUSTOMER ---");
        System.out.print("Enter Customer ID (e.g., C001): ");
        String customerId = scanner.nextLine().trim();

        System.out.print("Enter Customer Name: ");
        String name = scanner.nextLine().trim();

        try {
            Customer customer = system.registerCustomer(customerId, name);
            System.out.println("\n[SUCCESS] Customer registered successfully!");
            System.out.println("Customer: " + customer);
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private static void bookTickets() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- BOOK TICKETS ---");
        System.out.print("Enter Customer ID: ");
        String customerId = scanner.nextLine().trim();

        int tickets = getIntInput("Enter number of tickets: ");

        try {
            Booking booking = system.bookTickets(customerId, tickets);
            System.out.println("\n[SUCCESS] Booking request processed!");
            System.out.println("Booking ID: " + booking.getBookingId());
            System.out.println("Customer: " + booking.getCustomerId() + " (" + booking.getCustomer().getName() + ")");
            System.out.println("Tickets: " + booking.getTicketsRequested());
            System.out.println("Status: " + booking.getStatus());

            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                System.out.println("Message: Booking CONFIRMED!");
            } else if (booking.getStatus() == BookingStatus.WAITING) {
                System.out.println("Message: Tickets not available. Added to WAITING LIST (FIFO).");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private static void cancelBooking() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- CANCEL BOOKING ---");
        System.out.print("Enter Booking ID to cancel (e.g., B001): ");
        String bookingId = scanner.nextLine().trim().toUpperCase();

        try {
            Booking cancelled = system.cancelBooking(bookingId);
            System.out.println("\n[SUCCESS] Booking cancelled successfully!");
            System.out.println("Booking ID: " + cancelled.getBookingId());
            System.out.println("Customer: " + cancelled.getCustomerId());
            System.out.println("Tickets Released: " + cancelled.getTicketsRequested());
            System.out.println("Status: " + cancelled.getStatus());
            System.out.println("Note: Waiting list promotion attempted (if eligible).");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("\n[ERROR] " + e.getMessage());
        }
    }

    private static void searchBooking() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- SEARCH BOOKING ---");
        System.out.print("Enter Booking ID (e.g., B001): ");
        String bookingId = scanner.nextLine().trim().toUpperCase();

        Booking booking = system.searchBooking(bookingId);
        if (booking == null) {
            System.out.println("\n[NOT FOUND] No booking found with ID: " + bookingId);
        } else {
            System.out.println("\n--- BOOKING DETAILS ---");
            System.out.println("Booking ID: " + booking.getBookingId());
            System.out.println("Customer ID: " + booking.getCustomerId());
            System.out.println("Customer Name: " + booking.getCustomer().getName());
            System.out.println("Tickets Requested: " + booking.getTicketsRequested());
            System.out.println("Status: " + booking.getStatus());
        }
    }

    private static void viewAllBookings() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- ALL BOOKINGS ---");
        List<Booking> allBookings = system.getAllBookings();

        if (allBookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }

        System.out.println(String.format("%-8s %-10s %-20s %-10s %-12s", 
                "BookingID", "CustomerID", "CustomerName", "Tickets", "Status"));
        System.out.println("--------------------------------------------------------------------------");

        for (Booking booking : allBookings) {
            System.out.println(String.format("%-8s %-10s %-20s %-10s %-12s",
                    booking.getBookingId(),
                    booking.getCustomerId(),
                    booking.getCustomer().getName(),
                    booking.getTicketsRequested(),
                    booking.getStatus()));
        }
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Total Bookings: " + allBookings.size());
    }

    private static void viewConfirmedBookings() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- CONFIRMED BOOKINGS ---");
        List<Booking> confirmed = system.getConfirmedBookings();

        if (confirmed.isEmpty()) {
            System.out.println("No confirmed bookings found.");
            return;
        }

        System.out.println(String.format("%-8s %-10s %-20s %-10s %-12s", 
                "BookingID", "CustomerID", "CustomerName", "Tickets", "Status"));
        System.out.println("--------------------------------------------------------------------------");

        for (Booking booking : confirmed) {
            System.out.println(String.format("%-8s %-10s %-20s %-10s %-12s",
                    booking.getBookingId(),
                    booking.getCustomerId(),
                    booking.getCustomer().getName(),
                    booking.getTicketsRequested(),
                    booking.getStatus()));
        }
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Total Confirmed: " + confirmed.size());
    }

    private static void viewWaitingList() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- WAITING LIST ---");
        system.displayWaitingList();
        System.out.println("Waiting Count: " + system.getWaitingCount());
        System.out.println("Total Tickets in Waiting: " + system.getWaitingTickets());
    }

    private static void viewActionHistory() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n--- ACTION HISTORY ---");
        system.displayActionHistory();
        System.out.println("Total Actions Recorded: " + system.getActionCount());
    }

    private static void viewSystemSummary() {
        if (!isSystemInitialized()) {
            return;
        }

        System.out.println("\n=================================================");
        System.out.println("                SYSTEM SUMMARY                   ");
        System.out.println("=================================================");
        System.out.println("Total Capacity:      " + system.getTotalCapacity());
        System.out.println("Available Tickets:   " + system.getAvailableTickets());
        System.out.println("Reserved Tickets:    " + system.getReservedTickets());
        System.out.println("Waiting Tickets:     " + system.getWaitingTickets());
        System.out.println("-------------------------------------------------");
        System.out.println("Registered Customers: " + system.getCustomerCount());
        System.out.println("Total Bookings:       " + system.getAllBookings().size());
        System.out.println("Confirmed Bookings:   " + system.getConfirmedBookings().size());
        System.out.println("Waiting Bookings:     " + system.getWaitingBookings().size());
        System.out.println("Cancelled Bookings:   " + system.getCancelledBookings().size());
        System.out.println("-------------------------------------------------");
        System.out.println("Waiting List Count:   " + system.getWaitingCount());
        System.out.println("Actions in History:   " + system.getActionCount());
        System.out.println("Inventory Consistent: " + (system.isInventoryConsistent() ? "YES" : "NO"));
        System.out.println("=================================================");
    }

    private static boolean isSystemInitialized() {
        if (system == null) {
            System.out.println("\n[ERROR] System not initialized! Please choose Option 1 first.");
            return false;
        }
        return true;
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("[ERROR] Input cannot be empty. Try again.");
                    continue;
                }
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid number.");
            }
        }
    }
}
