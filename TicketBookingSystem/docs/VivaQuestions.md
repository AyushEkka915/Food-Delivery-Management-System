# Viva Questions & Answers: Ticket Booking Queue System Using Java and DSA

## A. Basic Concepts (1-5)

**Q1. What is the title and objective of your project?**  
**A:** *"Ticket Booking Queue System Using Java and DSA"*. The main objective is to simulate a real-world ticket booking system using core Data Structures like Queue, Stack, HashMap and Linked List with proper validation, inventory management and FIFO-based waiting list.

**Q2. Why did you choose Java for this project?**  
**A:** Java is object-oriented, platform-independent, easy for implementing DSA, has strong exception handling and is widely taught in DSA labs. It also helped us maintain modularity using classes and packages.

**Q3. What type of application is this?**  
**A:** It is a **menu-driven console-based application** built using Java and core Data Structures & Algorithms.

**Q4. How many tickets were taken as default?**  
**A:** We initialized the system with a **configurable ticket capacity**. For testing we used **10 tickets** as default.

**Q5. What real-life scenario does this project mimic?**  
**A:** It mimics a ticket counter (Railway/Event booking counter) where customers book tickets on a first-come-first-served basis. If tickets are unavailable, customers go to a waiting list and are served on cancellation in arrival order.

## B. Data Structures (6-10)

**Q6. Which Data Structures have you used in this project?**  
**A:** We have used **Singly Linked List (Custom Queue)**, **Array-based Stack**, **HashMap (via BookingRegistry)**, **ArrayList**, and **Enum (BookingStatus)**.

**Q7. Why did you implement a custom Queue instead of using `java.util.Queue`?**  
**A:** To demonstrate our understanding of how a Queue works internally. By implementing `QueueNode`, `CustomQueue` with `front`, `rear` and `size`, we learned pointer manipulation, FIFO logic, empty/single-node cases and time/space complexity practically.

**Q8. What is FIFO? How is it implemented here?**  
**A:** **FIFO** stands for **First In, First Out**. The customer who requests first is served first. We implemented it using a **custom singly linked list queue**. New requests are added to `rear` using `enqueue()` and served from `front` using `dequeue()`.

**Q9. What is the use of Stack in your project?**  
**A:** We used an **array-based Stack (ActionStack)** to maintain **recent booking actions** in **LIFO (Last In, First Out)** order. It records actions like "Confirmed booking B001", "Waitlisted booking B002", "Cancelled booking B001", "Promoted booking B002 to CONFIRMED". The history is displayed as **newest first**.

**Q10. Why did you use HashMap/BookingRegistry?**  
**A:** To achieve **O(1) average lookup** for searching bookings by Booking ID. `BookingRegistry` uses `HashMap<String, Booking>` to store all bookings (CONFIRMED, WAITING, CANCELLED). This allows fast search and also prevents duplicate Booking IDs.

## C. Logic & Business Rules (11-16)

**Q11. What happens when tickets are available?**  
**A:** If the **waiting list is empty** and requested tickets <= available tickets, the booking is marked as **CONFIRMED**, tickets are deducted from `availableTickets` and added to `reservedTickets`. A confirmation action is logged.

**Q12. What happens when tickets are not available?**  
**A:** The booking is marked as **WAITING**, added to the **CustomQueue (waiting list)** in FIFO order, registered in `BookingRegistry`, and a "Waitlisted" action is recorded. `availableTickets` remains unchanged.

**Q13. What is the "No-Bypass" rule in your project?**  
**A:** If there is **already someone in the waiting list**, any new booking request **goes directly to the waiting list** even if enough tickets are currently available. This ensures strict fairness and preserves the queue order.

**Q14. Who can be cancelled and who cannot?**  
**A:** Only **CONFIRMED** bookings can be cancelled. **WAITING** bookings **cannot** be cancelled. **Already CANCELLED** bookings cannot be cancelled again. All these are enforced using the `Booking` status transition logic.

**Q15. Explain the promotion logic.**  
**A:** On cancellation of a CONFIRMED booking, `availableTickets` are increased and `promoteFromWaitingList()` is called. It follows **strict FIFO** – only the **front** of the queue is checked. If `front.ticketsRequested <= availableTickets`, it is **dequeued**, changed to **CONFIRMED**, tickets deducted and logged as "Promoted". Then it loops again to check the **new front**. If the front doesn't fit, it **stops** (no skipping).

**Q16. Can a waiting list request be cancelled in your system?**  
**A:** **No**. As per our locked business rule, only CONFIRMED bookings can be cancelled. Attempting to cancel a WAITING booking throws an `IllegalStateException`.

## D. OOP, Design & Implementation (17-21)

**Q17. How did you ensure ticket overselling is not possible?**  
**A:** We never confirm a booking unless `tickets <= availableTickets`. For WAITING bookings, `availableTickets` is not touched. Only `CONFIRMED` bookings are counted in `reservedTickets`. The inventory invariant `available + reserved == totalCapacity` is verified after every operation.

**Q18. How are Booking IDs and Customer IDs made unique?**  
**A:** Booking IDs are auto-generated sequentially as `B001`, `B002`, `B003,...` using a counter. Customer IDs are checked against the `customers` list in `TicketBookingSystem` and `BookingRegistry` prevents duplicate Booking IDs using `containsKey()`.

**Q19. What is the use of `BookingStatus` enum?**  
**A:** `BookingStatus` is an enum with `CONFIRMED`, `WAITING`, `CANCELLED`. Using an enum prevents invalid string values, provides compile-time safety, makes code readable and allows us to enforce legal status transitions in `Booking.updateStatus()`.

**Q20. Explain Encapsulation in your project.**  
**A:** All fields are declared `private`. Access is provided only through `public` getters/setters where necessary. For example, `Customer` is immutable (`final` fields), `Booking` controls status changes only via `updateStatus()`, and internal data structures are not exposed directly.

**Q21. Why do you keep CANCELLED bookings in the registry?**  
**A:** We don't delete them on cancellation. Keeping all bookings (including CANCELLED) allows us to **search by Booking ID** even after cancellation, maintains a complete history, prevents **ID reuse**, and helps in generating accurate reports.

## E. Testing, Complexity & Viva Tricky (22-25)

**Q22. What is the time complexity of your queue operations?**  
**A:** `enqueue()`, `dequeue()`, `peek()` are all **O(1)** as we use `front` and `rear` pointers. `displayQueue()` and `toList()` are **O(n)** since we traverse the entire queue.

**Q23. What is the time complexity of booking search?**  
**A:** Using `HashMap` in `BookingRegistry`, `findById()` is **O(1) on average** and **O(n) in worst case** due to possible hash collisions. Customer lookup using `ArrayList` is **O(n)**.

**Q24. How did you test your project?**  
**A:** We created 5 automated test harnesses - `QueueTest`, `StackAndRegistryTest`, `TicketBookingSystemTest`, `CancellationTest` and `EndToEndTest`. Together they cover **258 checks** with **0 failures**. We also tested manually through the `Main` menu for real user scenarios.

**Q25. What are the key learnings from this project?**  
**A:** We learned to implement custom data structures from scratch, apply FIFO/LIFO practically, enforce business rules, maintain data consistency using invariants, write modular OOP code, handle exceptions properly, and follow an **incremental development approach** with proper Git commits phase by phase.
