/**
 * A customer who can request tickets.
 *
 * This class is immutable: a Customer's ID and name never change after
 * construction. Immutability makes customers safe to share with bookings
 * and prevents accidental edits deep inside the system.
 */
public class Customer {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_ID_LENGTH = 20;

    private final String customerId;
    private final String name;

    /**
     * Creates a validated customer.
     *
     * @param customerId unique ID such as "C001"
     * @param name       customer's display name
     * @throws IllegalArgumentException if either value is missing or malformed
     */
    public Customer(String customerId, String name) {
        this.customerId = validateCustomerId(customerId);
        this.name = validateName(name);
    }

    private static String validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty.");
        }

        String trimmed = customerId.trim();
        if (trimmed.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "Customer ID must be at most " + MAX_ID_LENGTH + " characters.");
        }
        // No spaces or punctuation, so IDs are easy to type in the console.
        if (!trimmed.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Customer ID may contain only letters, digits, '_' and '-'.");
        }
        return trimmed;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }

        // Collapse repeated spaces so "Alice   Smith" becomes "Alice Smith".
        String trimmed = name.trim().replaceAll("\\s+", " ");

        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Customer name must be between " + MIN_NAME_LENGTH
                            + " and " + MAX_NAME_LENGTH + " characters.");
        }
        // Letters, spaces, dot, apostrophe and hyphen cover names like "O'Brien" or "Anne-Marie".
        if (!trimmed.matches("[A-Za-z][A-Za-z .'-]*")) {
            throw new IllegalArgumentException(
                    "Customer name may contain only letters, spaces, '.', ''' and '-'.");
        }
        return trimmed;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    /**
     * Two customers are the same if they have the same ID.
     * This is what the system uses to block duplicate registrations.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Customer)) {
            return false;
        }
        Customer that = (Customer) other;
        return customerId.equals(that.customerId);
    }

    @Override
    public int hashCode() {
        return customerId.hashCode();
    }

    @Override
    public String toString() {
        return customerId + " (" + name + ")";
    }
}
