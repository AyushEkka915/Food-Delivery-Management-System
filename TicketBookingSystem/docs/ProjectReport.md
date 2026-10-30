# Project Report: Ticket Booking Queue System Using Java and DSA

**Project Title:** Ticket Booking Queue System Using Java and DSA  
**Project Type:** Minor Project (DSA Training)  
**Submitted To:** [Faculty Name]  
**Submitted By:** [Student Name]  
**Date:** October 2026  
**Technology Used:** Java, Data Structures & Algorithms

---

## Table of Contents

1. [Abstract](#1-abstract)
2. [Introduction](#2-introduction)
3. [Problem Statement](#3-problem-statement)
4. [Objectives](#4-objectives)
5. [Scope and Limitations](#5-scope-and-limitations)
6. [Literature Review](#6-literature-review)
7. [System Analysis](#7-system-analysis)
8. [System Design](#8-system-design)
9. [Data Structures & Algorithms Used](#9-data-structures--algorithms-used)
10. [Implementation Details](#10-implementation-details)
11. [Testing](#11-testing)
12. [Results and Discussion](#12-results-and-discussion)
13. [Conclusion](#13-conclusion)
14. [Future Enhancements](#14-future-enhancements)
15. [References](#15-references)
16. [Appendix](#16-appendix)

---

## 1. Abstract

The Ticket Booking Queue System is a menu-driven console application developed using Java and core DSA concepts. It simulates the ticket reservation process where customers can request tickets, get confirmed bookings when available, or join a waiting list on unavailability. On cancellation, waiting customers are automatically promoted in strict FIFO order. The project demonstrates practical use of Custom Linked List Queue, Array-Based Stack, HashMap-based Registry, and Object-Oriented Programming principles.

---

## 2. Introduction

Ticket booking systems are widely used in railways, cinemas, buses and events. Managing seat availability and fair allocation during high demand is a real challenge. This project aims to solve this by implementing a simplified version using data structures like Queue for fair waiting list management and Stack for activity tracking. By building custom implementations, the project emphasizes strong DSA fundamentals.

---

## 3. Problem Statement

To design and implement a Ticket Booking Queue System using Java and DSA that:
- Manages ticket inventory efficiently
- Handles booking requests fairly using FIFO
- Maintains a waiting list with automatic promotion
- Allows booking search, cancellation and history tracking
- Prevents overselling and maintains data consistency

---

## 4. Objectives

- Implement custom Queue using Linked List (without Java Collections Queue)
- Implement Stack for action history
- Use HashMap-based lookup for fast booking search
- Apply OOP concepts (Classes, Objects, Encapsulation, Enum)
- Enforce business rules and validation
- Maintain inventory invariant
- Create a user-friendly menu-driven interface
- Test thoroughly for edge cases

---

## 5. Scope and Limitations

### Scope
- Console-based application
- Fixed ticket capacity per session
- Customer registration and ticket booking
- FIFO waiting list with promotion
- Booking cancellation and search
- Action history tracking

### Limitations
- No database persistence (data lost on exit)
- No GUI
- Single session only
- Waiting list requests cannot be cancelled
- No payment integration
- No seat numbering

---

## 6. Literature Review

Queues are ideal for fair, first-come-first-served systems. Stacks help in tracking recent operations. HashMaps provide efficient key-value lookups. This project combines these to simulate a realistic booking counter.

---

## 7. System Analysis

### Existing System
Manual or API-based systems exist but are complex. This project provides a simplified educational model.

### Proposed System
A Java-based console system with custom DSA implementations, strict rules, validation and automatic promotion.

---

## 8. System Design

### 8.1 System Architecture
The system follows a layered design:
- **Model Layer:** Customer, Booking, BookingStatus
- **Data Structure Layer:** QueueNode, CustomQueue, ActionStack
- **Data Access Layer:** BookingRegistry
- **Business Layer:** TicketBookingSystem
- **Presentation Layer:** Main (Console Menu)

### 8.2 Entity Relationship
- Customer has multiple Bookings (1:M)
- Booking belongs to one Customer
- Bookings can be CONFIRMED, WAITING or CANCELLED

### 8.3 Flowchart (Summary)
1. Initialize System
2. Register Customer
3. Book Tickets -> Check Availability -> Confirm or Waitlist
4. Cancel Booking -> Release Tickets -> Promote from Waitlist (FIFO)
5. View/Search/History/Summary
6. Exit

### 8.4 Class Diagram (Summary)
Key classes: Main, TicketBookingSystem, Customer, Booking, BookingStatus, QueueNode, CustomQueue, ActionStack, BookingRegistry

---

## 9. Data Structures & Algorithms Used

| Structure/Algo | Type | Use Case | Complexity |
|---|---|---|---|
| **Singly Linked List** | Data Structure | Custom Queue nodes | O(1) enqueue/dequeue |
| **Queue (FIFO)** | Data Structure | Waiting list management | O(1) operations |
| **Stack (Array-based)** | Data Structure | Action history (LIFO) | O(1) push/pop |
| **HashMap** | Data Structure | Booking lookup by ID | O(1) average |
| **ArrayList** | Data Structure | Customers & reports | O(n) scan for search |
| **Linear Search** | Algorithm | Find customer by ID | O(n) |
| **Linear Traversal** | Algorithm | Display queue/history/reports | O(n) |

---

## 10. Implementation Details

### 10.1 Core Classes

- **BookingStatus.java:** Enum with CONFIRMED, WAITING, CANCELLED
- **Customer.java:** Immutable, validated customer with equals/hashCode by ID
- **Booking.java:** Booking with legal status transitions (WAITING->CONFIRMED, CONFIRMED->CANCELLED)
- **QueueNode.java:** Node holding Booking and next reference
- **CustomQueue.java:** Custom FIFO with front/rear/size, enqueue/dequeue/peek/displayQueue/toList
- **ActionStack.java:** Array-backed stack with push/pop/peek/displayHistory
- **BookingRegistry.java:** HashMap<String,Booking> for lookup, stores all statuses
- **TicketBookingSystem.java:** Engine with inventory, booking, cancellation, promotion
- **Main.java:** Menu-driven interface with input validation

### 10.2 Key Logic

**Booking:** If waiting list empty AND tickets available -> CONFIRMED else WAITING (strict no-bypass)

**Cancellation:** Only CONFIRMED allowed -> release tickets -> call promoteFromWaitingList()

**Promotion:** While queue not empty and front.tickets <= available -> dequeue front, set CONFIRMED, deduct tickets, log promotion, continue. Else break (strict FIFO)

**Inventory Invariant:** `available + reserved == totalCapacity` where reserved = sum of CONFIRMED tickets

---

## 11. Testing

All tests automated with custom harnesses:

| Test File | Purpose | Result |
|---|---|---|
| QueueTest.java | Queue operations, FIFO, stress test (1000 elements) | **PASSED (34/34)** |
| StackAndRegistryTest.java | Stack LIFO, Registry CRUD, copies, status visibility | **PASSED (54/54)** |
| TicketBookingSystemTest.java | Engine validation, booking, inventory | **PASSED (71/71)** |
| CancellationTest.java | Cancellation rules, promotion | **PASSED (40/40)** |
| EndToEndTest.java | Full integration, edge cases, invariant stress | **PASSED (59/59)** |

**Total: 258 checks, 0 failures**

---

## 12. Results and Discussion

- Successfully implemented custom DSA structures from scratch
- Strict FIFO and no-bypass rules enforced correctly
- Inventory consistency maintained throughout all operations
- Automatic promotion works as expected
- System handles invalid inputs gracefully without crashes
- All edge cases tested and passed
- Clean modular design with separation of concerns

---

## 13. Conclusion

The Ticket Booking Queue System successfully demonstrates the application of Data Structures and Algorithms in solving real-world problems. By implementing custom Queue, Stack and using HashMap effectively, the project achieves fair ticket allocation, proper inventory management and complete traceability through action history. The project meets all objectives and is suitable for college submission.

---

## 14. Future Enhancements

- Database connectivity (MySQL/SQLite) for persistence
- Graphical User Interface (JavaFX/Swing)
- Seat number allocation
- Multiple events/show timings
- User authentication
- Email/SMS notifications
- Billing and receipt generation
- Report export (PDF/CSV)

---

## 15. References

- Herbert Schildt, "Java: The Complete Reference", McGraw Hill
- Data Structures by Seymour Lipschutz
- GeeksforGeeks - https://www.geeksforgeeks.org/
- Oracle Java Documentation - https://docs.oracle.com/javase/
- TutorialsPoint - https://www.tutorialspoint.com/java/

---

## 16. Appendix

### Appendix A: Sample Menu
(See Main.java for complete menu)

### Appendix B: Key Code Snippets
- CustomQueue.enqueue/dequeue
- TicketBookingSystem.promoteFromWaitingList()
- TicketBookingSystem.cancelBooking()

### Appendix C: Git Commits
All 8 phases committed with meaningful messages showing incremental development.
