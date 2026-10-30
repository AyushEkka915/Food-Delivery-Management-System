# Ticket Booking Queue System Using Java and DSA

## Abstract

The **Ticket Booking Queue System** is a menu-driven console-based application developed using **Java** and core **Data Structures & Algorithms (DSA)**. It simulates a real-world ticket reservation system (like railway/event ticket counters) where customers can book tickets, join a waiting list when unavailable, and get promoted automatically on cancellations. 

This project demonstrates the practical implementation of a **Custom Linked List Queue (FIFO)**, **Stack (LIFO)**, **HashMap (via Registry)**, and **ArrayList** with proper validation, inventory management, and strict fairness rules.

## 1. Objectives

- To implement a real-world ticket booking system using Java.
- To apply core DSA concepts like Queue, Stack, HashMap and Linked List practically.
- To build a custom queue from scratch instead of using built-in `java.util.Queue`.
- To maintain ticket inventory with strict consistency checks.
- To implement FIFO-based waiting list with automatic promotion.
- To ensure data integrity through validation and exception handling.
- To design a modular, object-oriented and menu-driven application.
- To prepare industry/college-level documentation and testing.

## 2. Features

- **System Initialization** - Configure total ticket capacity at runtime.
- **Customer Registration** - Register customers with validated unique IDs and names.
- **Ticket Booking** - Book tickets with automatic confirmation or waitlisting.
- **Waiting List (FIFO)** - Strict First-In-First-Out queue for unconfirmed requests.
- **Automatic Promotion** - On cancellation, eligible waiting customers are promoted in order.
- **Booking Cancellation** - Cancel only confirmed bookings with ticket release.
- **Booking Search** - Search any booking by Booking ID across all statuses.
- **View All/Confirmed Bookings** - Filter and display bookings by status.
- **Waiting List Display** - View queue from front to rear.
- **Action History** - Track recent operations using a Stack (newest first).
- **System Summary** - Real-time view of inventory, counts and consistency.
- **Input Validation** - Robust handling of invalid inputs without crashes.

## 3. Data Structures Used

| Data Structure | Implementation | Purpose |
|---|---|---|
| **Singly Linked List Queue** | `QueueNode.java`, `CustomQueue.java` | Waiting list with strict FIFO. O(1) enqueue/dequeue/peek. |
| **Stack (Array-based)** | `ActionStack.java` | Maintain recent action history in LIFO order. |
| **HashMap (via Registry)** | `BookingRegistry.java` | Fast booking lookup by Booking ID in O(1) average. Stores all bookings including cancelled ones. |
| **ArrayList** | Used in `TicketBookingSystem.java` & `BookingRegistry.java` | Customer registry and report generation. |
| **Enum** | `BookingStatus.java` | Type-safe booking states (CONFIRMED, WAITING, CANCELLED). |

## 4. Tech Stack

- **Language:** Java (JDK 17+ Compatible, Tested on Java 26)
- **IDE/Tools:** VS Code / Any Java IDE
- **Version Control:** Git
- **Paradigm:** Object-Oriented Programming (OOP)
- **Interface:** Console-based Menu Driven

## 5. Project Structure

```text
TicketBookingSystem/
│
├── .gitignore
├── README.md
│
├── src/
│   ├── Main.java                # Menu-driven console interface
│   ├── TicketBookingSystem.java # Core reservation engine
│   ├── Customer.java            # Customer model with validation
│   ├── Booking.java             # Booking model with status transitions
│   ├── BookingStatus.java       # Enum for booking states
│   ├── QueueNode.java           # Node for custom linked queue
│   ├── CustomQueue.java         # Custom FIFO queue implementation
│   ├── ActionStack.java         # Array-based stack for history
│   └── BookingRegistry.java     # HashMap-backed booking index
│
├── tests/
│   ├── QueueTest.java           # Queue tests
│   ├── StackAndRegistryTest.java# Stack & Registry tests
│   ├── TicketBookingSystemTest.java # Engine tests
│   ├── CancellationTest.java    # Cancellation & promotion tests
│   └── EndToEndTest.java        # Full system integration tests
│
├── docs/                        # Project documentation
└── screenshots/                 # (For demo/screenshots)
```

## 6. How to Run

### Prerequisites
- Java JDK 17 or higher installed
- Git (optional)

### Steps

1. **Clone/Open the project**
   ```bash
   cd "D:\MAin project\Dsa Projects\TicketBookingSystem"
   ```

2. **Compile all source files**
   ```bash
   javac -d out src\*.java
   ```

3. **Run the application**
   ```bash
   java -cp out Main
   ```

4. **Run all tests (optional but recommended)**
   ```bash
   javac -d out src\*.java tests\*.java
   java -cp out QueueTest
   java -cp out StackAndRegistryTest
   java -cp out TicketBookingSystemTest
   java -cp out CancellationTest
   java -cp out EndToEndTest
   ```

## 7. Business Rules Implemented

- **Capacity Control:** Total tickets fixed at initialization. Never oversold.
- **Strict FIFO:** Waiting list follows First-In-First-Out with no bypass.
- **No-Bypass Rule:** If waiting list is not empty, new requests join waitlist even if tickets available.
- **Cancellation Restriction:** Only `CONFIRMED` bookings can be cancelled. `WAITING` and `CANCELLED` cannot be cancelled.
- **Strict Promotion:** On cancellation, only front of queue is checked. If doesn't fit, stop (no skipping).
- **Inventory Invariant:** `availableTickets + sum(CONFIRMED tickets) = totalCapacity` always maintained.
- **Unique IDs:** Auto-generated unique Booking IDs (`B001`, `B002`, ...) and Customer IDs.
- **All Bookings Tracked:** Registry keeps CONFIRMED, WAITING and CANCELLED bookings for searchability.
- **Validation First:** All inputs validated before state changes.

## 8. Testing Summary

| Test Suite | Checks | Status |
|---|---|---|
| `QueueTest.java` | 34 | **PASSED** |
| `StackAndRegistryTest.java` | 54 | **PASSED** |
| `TicketBookingSystemTest.java` | 71 | **PASSED** |
| `CancellationTest.java` | 40 | **PASSED** |
| `EndToEndTest.java` | 59 | **PASSED** |
| **TOTAL** | **258** | **ALL PASSED** |

## 9. Git History

```text
9e61d72 Phase 8: add end-to-end testing and debugging with full regression pass
7c42f91 Phase 7: add menu-driven console interface (Main.java)
3f9b8a2 Phase 6: add cancellation and strict FIFO waiting-list promotion
2a63ad5 Phase 5: add reservation engine with inventory, IDs, and waiting-list insertion
c7b6544 Phase 4: add action stack and HashMap booking registry with tests
5833e6e Phase 3: add custom linked-list FIFO queue with tests
5807c6d Phase 2: add core domain models (Customer, Booking, BookingStatus)
```

## 10. Learning Outcomes

- Mastered implementation of **custom data structures** from scratch.
- Gained practical knowledge of **Queue (FIFO), Stack (LIFO), HashMap and Linked List**.
- Understood **OOP principles** (Encapsulation, Abstraction, Modularity).
- Learned **exception handling**, input validation and defensive programming.
- Applied **algorithmic thinking** for real-world business logic.
- Practiced **incremental development**, testing and debugging.
- Improved **documentation** and project presentation skills.

## 11. Future Enhancements

- Add GUI using Java Swing/JavaFX
- Implement file handling to save/load booking data
- Add customer login system
- Support multiple showtimes/events
- Generate printable booking receipts
- Add sorting of bookings by name/date
- Implement priority-based booking for special cases

## 12. References

- GeeksforGeeks - Data Structures & Algorithms
- Oracle Java Documentation (https://docs.oracle.com/en/java/)
- JavaTpoint - Java Tutorials
- College DSA Lab Material

**Developed as a Minor Project for DSA Training | Java + Data Structures & Algorithms**
