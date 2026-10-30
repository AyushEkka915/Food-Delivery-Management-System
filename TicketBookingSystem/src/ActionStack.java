import java.util.NoSuchElementException;

/**
 * A fixed-capacity stack of human-readable action messages.
 *
 * A stack is LIFO (last in, first out): the most recent action is on top and
 * is the first one seen or removed. This is used for the "recent activity"
 * history in the booking system.
 *
 * The stack is array-backed. {@code top} is the index of the newest element;
 * -1 means the stack is empty. Because both push and pop work at the end of
 * the array, neither operation shifts existing elements.
 */
public class ActionStack {

    /** Capacity used when the caller does not specify one. */
    public static final int DEFAULT_CAPACITY = 100;

    private final String[] actions;
    private int top;

    /** Creates a stack with {@link #DEFAULT_CAPACITY} slots. */
    public ActionStack() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * @param capacity maximum number of actions that can be stored
     * @throws IllegalArgumentException if capacity is not positive
     */
    public ActionStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Stack capacity must be greater than zero.");
        }
        this.actions = new String[capacity];
        this.top = -1;
    }

    /**
     * Pushes a new action onto the top of the stack.
     *
     * @param action description such as "Confirmed booking B001"
     * @throws IllegalArgumentException if the action is null or blank
     * @throws IllegalStateException    if the stack has no free slot
     */
    public void push(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action description cannot be empty.");
        }
        if (isFull()) {
            throw new IllegalStateException(
                    "Action history is full (capacity " + actions.length + ").");
        }

        top++;
        actions[top] = action.trim();
    }

    /**
     * Removes and returns the most recent action.
     *
     * @throws NoSuchElementException if the history is empty
     */
    public String pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot pop from an empty action history.");
        }

        String removed = actions[top];
        actions[top] = null; // release the reference so the string can be collected
        top--;
        return removed;
    }

    /**
     * Returns the most recent action without removing it.
     *
     * @throws NoSuchElementException if the history is empty
     */
    public String peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peek an empty action history.");
        }
        return actions[top];
    }

    /** @return true when no actions are stored */
    public boolean isEmpty() {
        return top == -1;
    }

    /** @return true when every slot is occupied */
    public boolean isFull() {
        return top == actions.length - 1;
    }

    /** @return number of stored actions */
    public int size() {
        return top + 1;
    }

    /** @return maximum number of actions this stack can hold */
    public int capacity() {
        return actions.length;
    }

    /**
     * Prints the history from newest to oldest, which is the order a stack
     * exposes its elements. The stack itself is not modified.
     */
    public void displayHistory() {
        if (isEmpty()) {
            System.out.println("No actions recorded.");
            return;
        }

        System.out.println("Action history (newest first):");

        int position = 1;
        for (int i = top; i >= 0; i--) {
            System.out.println("  " + position + ". " + actions[i]);
            position++;
        }
    }
}
