# Test Cases: Ticket Booking Queue System Using Java and DSA

**Project:** Ticket Booking Queue System  
**Testing Type:** Unit Testing, Integration Testing & End-to-End Testing  
**Total Checks:** 258  
**Status:** ALL PASSED (0 Failures)

---

## 1. Queue Tests (`QueueTest.java`) - 34 Checks

| Test Case | Description | Initial State | Operation | Expected Result | Status |
|---|---|---|---|---|---|
| TC-Q01 | New Queue | Empty system | Create `CustomQueue()` | `isEmpty() = true`, `size() = 0` | PASS |
| TC-Q02 | Dequeue on Empty | Empty Queue | `dequeue()` | `NoSuchElementException` thrown | PASS |
| TC-Q03 | Peek on Empty | Empty Queue | `peek()` | `NoSuchElementException` thrown | PASS |
| TC-Q04 | Display Empty Queue | Empty Queue | `displayQueue()` | Prints "Waiting list is empty." without crash | PASS |
| TC-Q05 | Enqueue Single Element | Empty Queue | `enqueue(B001)` | `size()=1`, `isEmpty()=false`, `peek()` returns B001 | PASS |
| TC-Q06 | FIFO Order (3 Elements) | Empty Queue | Enqueue B001,B002,B003 then dequeue | Dequeue returns B001 -> B002 -> B003 in order | PASS |
| TC-Q07 | Single Element Removal & Reuse | Queue has B010 | Dequeue B010, then enqueue B011,B012 | Becomes empty after removal, reused correctly with FIFO order | PASS |
| TC-Q08 | Null Enqueue Rejection | Queue has B020 | `enqueue(null)` | `IllegalArgumentException` thrown, size unchanged, front intact | PASS |
| TC-Q09 | Stress Test (1000 Elements) | Empty Queue | Enqueue B0000-B0999, dequeue all | All in correct order, size becomes 0 at end | PASS |
| TC-Q10 | Display Queue | Queue has 2 WAITING bookings | `displayQueue()` | Prints front->rear with numbers 1., 2. without changing size | PASS |

---

## 2. Stack & Registry Tests (`StackAndRegistryTest.java`) - 54 Checks

### A. ActionStack Tests

| Test Case | Description | Operation | Expected Result | Status |
|---|---|---|---|---|
| TC-S01 | New Stack | `new ActionStack(3)` | Empty, size 0, not full, capacity 3 | PASS |
| TC-S02 | LIFO Order | Push A,B,C then pop/peek | peek returns C, pop returns C,B,A in order | PASS |
| TC-S03 | Push Null/Blank | Push null or "   " | `IllegalArgumentException` thrown, size unchanged | PASS |
| TC-S04 | Pop/Peek Empty | Empty stack | `NoSuchElementException` thrown | PASS |
| TC-S05 | Fixed Capacity | Fill to capacity, push again | Becomes full, next push throws `IllegalStateException` | PASS |
| TC-S06 | Display History | Empty & with elements | Empty shows "No actions recorded.", with elements shows newest first | PASS |

### B. BookingRegistry Tests

| Test Case | Description | Operation | Expected Result | Status |
|---|---|---|---|---|
| TC-R01 | Empty Registry | New registry | size 0, empty, lookups return null/false | PASS |
| TC-R02 | Add & Find | Add B001 | Found by ID, returns same object, appears in getAll() | PASS |
| TC-R03 | Duplicate Booking ID | Add B001 again with different booking | `IllegalArgumentException`, original preserved | PASS |
| TC-R04 | Add Null Booking | Add null | `IllegalArgumentException`, size unchanged | PASS |
| TC-R05 | Copy Safety | Clear getAll()/getByStatus() lists | Registry unchanged (returns copies) | PASS |
| TC-R06 | Status Visibility | Change status via object reference | Registry reflects changes, cancelled remains searchable | PASS |
| TC-R07 | Cancelled ID Not Reusable | Cancel B001 then try to add B001 again | Rejected, ID cannot be reused | PASS |

---

## 3. Ticket Booking System Tests (`TicketBookingSystemTest.java`) - 71 Checks

| Test Case | Description | Operation | Expected Result | Status |
|---|---|---|---|---|
| TC-T01 | Constructor Validation | capacity 0, -5, valid | Invalid throw exception, valid initializes correctly | PASS |
| TC-T02 | Customer Registration | Register valid, duplicate, book unknown | Valid registered, duplicate throws, unknown customer throws | PASS |
| TC-T03 | Confirmed Booking | Book within capacity | Status CONFIRMED, available/reserved updated correctly | PASS |
| TC-T04 | Waiting Booking | Book when no tickets left | Status WAITING, available unchanged, added to queue | PASS |
| TC-T05 | Strict No-Bypass | Queue not empty, tickets available | New request goes to WAITING (no line-jumping) | PASS |
| TC-T06 | Input Validation | 0, negative, > capacity tickets | All rejected, state unchanged, invariant holds | PASS |
| TC-T07 | ID Generation | Multiple bookings | Sequential B001,B002,B003... all unique | PASS |
| TC-T08 | Multiple Bookings Per Customer | Same customer books twice | Allowed, both get separate booking IDs | PASS |
| TC-T09 | Inventory Invariant | Mixed operations | `available + reserved == totalCapacity` always true | PASS |
| TC-T10 | Action History | Book confirmed/waiting | Actions logged with correct messages, newest first | PASS |
| TC-T11 | Queue Snapshot | Get waiting bookings | Returns list in front-to-rear (FIFO) order | PASS |

---

## 4. Cancellation & Promotion Tests (`CancellationTest.java`) - 40 Checks

| Test Case | Description | Operation | Expected Result | Status |
|---|---|---|---|---|
| TC-C01 | Cancel Confirmed Booking | Cancel valid CONFIRMED booking | Status becomes CANCELLED, tickets released, available increased | PASS |
| TC-C02 | Cancel Unknown Booking | Cancel non-existent ID | `IllegalArgumentException` thrown | PASS |
| TC-C03 | Cancel Already Cancelled | Cancel CANCELLED booking again | `IllegalStateException` thrown | PASS |
| TC-C04 | Cancel Waiting Booking | Cancel WAITING booking | `IllegalStateException` thrown (not allowed) | PASS |
| TC-C05 | Promotion After Cancellation | Cancel confirmed, front waiting fits | Front promoted to CONFIRMED, removed from queue | PASS |
| TC-C06 | Strict FIFO Promotion | Front blocked, later exists | Front stays in queue, later NOT promoted (no skipping) | PASS |
| TC-C07 | Multiple Promotions | Free enough tickets for multiple | All eligible promoted in FIFO order | PASS |
| TC-C08 | Inventory After Cancel+Promote | Mixed cancel/promotion | Reserved updated correctly, invariant holds | PASS |
| TC-C09 | History Logs | Cancel and promote | Both actions recorded with details, newest first | PASS |

---

## 5. End-to-End Tests (`EndToEndTest.java`) - 59 Checks

| Test Case | Description | Operation | Expected Result | Status |
|---|---|---|---|---|
| TC-E01 | Full End-to-End Flow | Register -> Book -> Waitlist -> Cancel -> Promote | Complete flow works correctly with all rules enforced | PASS |
| TC-E02 | Exact Capacity Fill | Fill exactly to capacity | Last ticket fills completely, next goes to waitlist | PASS |
| TC-E03 | Strict No-Bypass | Queue has elements, tickets available | New request waits despite availability | PASS |
| TC-E04 | Strict FIFO Blocking | Front doesn't fit freed tickets | Front remains blocked, promotion stops | PASS |
| TC-E05 | Multiple Promotions | Cancel large booking | Multiple waiting requests promoted in arrival order | PASS |
| TC-E06 | All Cancellation Rules | Test all invalid cancels | All properly rejected with correct exceptions | PASS |
| TC-E07 | Complete Validation Suite | All invalid inputs | Rejected without state corruption | PASS |
| TC-E08 | Search Across Statuses | Search CONFIRMED/WAITING/CANCELLED | All findable, cancelled still searchable | PASS |
| TC-E09 | Inventory Invariant Stress | 6+ mixed operations | Invariant holds after every single operation | PASS |
| TC-E10 | History Integrity | Complex flow | All actions logged in correct order (newest first) | PASS |
| TC-E11 | Queue Order Preservation | 4 bookings with waitlist | Exact arrival order maintained (B002,B003,B004) | PASS |
| TC-E12 | State Unchanged On Failure | Failed operations | Inventory and counts unchanged after exceptions | PASS |

---

## 6. Manual Testing via Console Menu

| Test Case | Steps | Expected Result | Status |
|---|---|---|---|
| MT-01 | Initialize system with capacity 10 | System initialized, available = 10 | PASS |
| MT-02 | Register customers C001,C002,C003 | All registered successfully | PASS |
| MT-03 | Book tickets for all 10 | All confirmed, available = 0 | PASS |
| MT-04 | Book when full | Added to waiting list, shown as WAITING | PASS |
| MT-05 | View Waiting List (Option 8) | Shows front->rear in FIFO order | PASS |
| MT-06 | Cancel confirmed booking (Option 4) | Cancelled, tickets released, promotion triggered | PASS |
| MT-07 | Search booking (Option 5) | Shows correct details for any status | PASS |
| MT-08 | View Action History (Option 9) | Shows newest first with all actions | PASS |
| MT-09 | View System Summary (Option 10) | All counts correct, invariant shows YES | PASS |
| MT-10 | Invalid menu choice | Shows error, menu redisplays | PASS |

---

## 7. Test Summary

| Category | Total Checks | Passed | Failed |
|---|---|---|---|
| Queue Tests | 34 | 34 | 0 |
| Stack & Registry Tests | 54 | 54 | 0 |
| Ticket Booking System Tests | 71 | 71 | 0 |
| Cancellation & Promotion Tests | 40 | 40 | 0 |
| End-to-End Tests | 59 | 59 | 0 |
| **GRAND TOTAL** | **258** | **258** | **0** |

**Conclusion:** All test cases pass successfully. The system meets all functional requirements and business rules with zero failures.
