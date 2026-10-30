# Demo Script & Screenshot Checklist

## 1. Project Demonstration Script (3-5 Minutes)

### Introduction (30-40 seconds)
*"Good [morning/afternoon], I am presenting my DSA minor project - **Ticket Booking Queue System Using Java and DSA**. This project simulates a real-world ticket counter where customers can book tickets, join a waiting list on unavailability and get promoted on cancellation using proper data structures."*

### Objectives & Data Structures (30-40 seconds)
*"I have implemented this using **Custom Linked List Queue (FIFO)** for the waiting list, **Array-based Stack (LIFO)** for action history, **HashMap-based Registry** for fast booking lookup, and **ArrayList** for customer management. I also built the Queue and Stack from scratch to demonstrate DSA concepts."*

### Live Demo (2-2.5 minutes)

**Step 1: Initialize System**
- Run: `java -cp out Main`
- Select **Option 1** - Set capacity to **10**
- Show system initialized

**Step 2: Register Customers**
- Select **Option 2** - Register **C001 (Rahul)**, **C002 (Amit)**, **C003 (Neha)**

**Step 3: Book Tickets**
- Select **Option 3** - Book **5 tickets** for C001 → **CONFIRMED** (B001)
- Book **5 tickets** for C002 → **CONFIRMED** (B002)
- Book **2 tickets** for C003 → **WAITING** (B003) since no tickets left
- Explain: *"As tickets exhausted, B003 goes to waiting list"*

**Step 4: View Waiting List & Summary**
- Select **Option 8** - Show waiting list in **front -> rear** order
- Select **Option 10** - Show summary (Available: 0, Reserved: 10, Waiting: 1)

**Step 5: Cancel & Promotion**
- Select **Option 4** - Cancel **B001**
- Explain: *"On cancellation, tickets are released and since waiting list has B003, strict FIFO promotion happens. B003 is promoted to CONFIRMED automatically."*
- Select **Option 10** again - Show updated summary
- Select **Option 9** - Show **Action History** (newest first) showing Confirmed/Waitlisted/Cancelled/Promoted

**Step 6: Search & Reports**
- Select **Option 5** - Search **B003** to show it's now **CONFIRMED**
- Select **Option 6** - View all bookings
- Select **Option 7** - View confirmed bookings only

### Conclusion (20-30 seconds)
*"This project demonstrates practical implementation of Queue, Stack and HashMap. It follows strict FIFO, maintains inventory consistency with invariant checks, prevents overselling and handles all edge cases. All 258 test cases pass with zero failures. Thank you."*

---

## 2. Key Points to Explain to Examiner

| Topic | What to Explain |
|---|---|
| **Why Custom Queue?** | Implemented from scratch using QueueNode with front/rear to show linked list + FIFO logic |
| **No-Bypass Rule** | If queue not empty, new requests wait even if tickets available - ensures fairness |
| **Strict Promotion** | Only front checked. If doesn't fit, stop. No line-jumping. |
| **Waiting not cancellable** | Breaking queue order would be unfair and complex |
| **Inventory Invariant** | `available + reserved = totalCapacity` - always maintained |
| **Registry keeps cancelled** | For searchability and prevents booking ID reuse |
| **Stack shows LIFO** | Most recent action shown first in history |
| **Testing** | 5 test suites, 258 checks, all passed |

---

## 3. Screenshot Checklist

Capture the following screenshots during demo for documentation/report:

### Setup & Menu
- [ ] **SS-01:** Project folder structure in VS Code/Explorer
- [ ] **SS-02:** Main menu on application start
- [ ] **SS-03:** System initialization with capacity (Option 1)

### Booking Flow
- [ ] **SS-04:** Customer registration (Option 2)
- [ ] **SS-05:** Ticket booking - CONFIRMED status (Option 3)
- [ ] **SS-06:** Ticket booking - WAITING status (Option 3)
- [ ] **SS-07:** Waiting list display with front->rear (Option 8)

### Cancellation & Promotion
- [ ] **SS-08:** Cancelling a confirmed booking (Option 4)
- [ ] **SS-09:** Promotion happening after cancellation (observe status change)
- [ ] **SS-10:** Action history showing Cancelled & Promoted (Option 9)

### Reports & Search
- [ ] **SS-11:** Booking search result (Option 5)
- [ ] **SS-12:** View all bookings (Option 6)
- [ ] **SS-13:** View confirmed bookings (Option 7)
- [ ] **SS-14:** System summary with all counts (Option 10)
- [ ] **SS-15:** Test execution showing all tests passed (terminal)

### Code Snippets (Optional but Good)
- [ ] **SS-16:** `CustomQueue.java` showing enqueue/dequeue
- [ ] **SS-17:** `TicketBookingSystem.java` showing promotion logic
- [ ] **SS-18:** Git log showing all 8 phases committed

---

## 4. Quick Commands Reference

```bash
# Compile
javac -d out src\*.java

# Run Application
java -cp out Main

# Run All Tests
javac -d out src\*.java tests\*.java
java -cp out QueueTest
java -cp out StackAndRegistryTest
java -cp out TicketBookingSystemTest
java -cp out CancellationTest
java -cp out EndToEndTest

# Git Status
git log --oneline -9
git status
```

---

## 5. Common Questions & Quick Answers

| Question | Answer |
|---|---|
| **Difference between Queue and Stack?** | Queue = FIFO (used for waiting list), Stack = LIFO (used for action history) |
| **Why Linked List for Queue?** | Dynamic size, O(1) front/rear operations, no shifting like arrays |
| **What if front doesn't fit during promotion?** | We break immediately (strict FIFO) - don't skip to next |
| **Can we have multiple bookings?** | Yes, same customer can book multiple times with different booking IDs |
| **Is data saved after exit?** | No, it's console-based with in-memory storage (no file/DB) |
| **How many DSA concepts used?** | Queue, Stack, Linked List, HashMap, ArrayList, Enum + Searching/Traversal |
```
