# Smart Food Delivery Management System

A full-stack Food Delivery Management System developed as a B.Tech Computer Science major project using Java, Spring Boot, MySQL, and React.

**Project Status:** 🚧 Under Active Development

## 1. Project Overview

The Smart Food Delivery Management System is designed to manage the complete food ordering and delivery process through a centralized platform.

The system is planned around four primary user roles:

- **Customer:** Browse restaurants, explore menus, place orders, and track deliveries.
- **Restaurant Manager:** Manage restaurant information, food items, availability, and incoming orders.
- **Delivery Partner:** Handle delivery assignments and update delivery progress.
- **Administrator:** Manage platform operations, approve restaurants, and oversee system activity.

The project aims to demonstrate practical software engineering concepts, backend development, database management, data structures and algorithms, and frontend integration.

## 2. Technology Stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.5 |
| Backend Security | Spring Security, JWT Authentication |
| Database | MySQL 8 |
| ORM | Spring Data JPA, Hibernate |
| Database Migrations | Flyway |
| Build Tool | Maven |
| Testing | JUnit 5, Mockito |
| Frontend (planned) | React, Vite, Tailwind CSS |
| API Testing | Postman |
| Version Control | Git, GitHub |

## 3. Implemented Features

The following backend functionality has been implemented:

### Authentication and Authorization
- JWT-based authentication.
- Access and refresh token handling.
- Refresh token rotation.
- Role-based access control.

### Restaurant Management
- Restaurant registration.
- Restaurant ownership and management.
- Restaurant approval workflow.
- Restaurant suspension functionality.
- Admin restaurant management.

### Menu Management
- Food item management.
- Menu availability and stock handling.
- Restaurant ownership validation.
- Public menu retrieval.

### Public Restaurant Catalog
- Approved restaurant listing.
- Restaurant detail retrieval.
- Restaurant filtering and sorting.
- Price and vegetarian filters.
- Pagination support.
- Public visibility restrictions for unapproved restaurants and unavailable food items.

### Testing and Database Safety
- Unit and repository tests.
- MySQL integration tests.
- Dedicated test database configuration.
- Flyway migration validation.
- Test database isolation safeguards.

## 4. Planned Features

The following modules are part of the intended project scope and are still under development:

- Customer registration and profile management.
- Shopping cart and checkout.
- Dynamic pricing, taxes, delivery fees, and coupons.
- Order placement and lifecycle management.
- FIFO-based order processing.
- Delivery partner assignment using priority queues/heaps.
- Graph-based delivery route optimization using Dijkstra's algorithm.
- Delivery tracking and WebSocket integration.
- Payment integration.
- Admin analytics and audit logs.
- React frontend integration.
- Final system testing and deployment.

## 5. Data Structures and Algorithms

An important objective of this project is to demonstrate the practical application of DSA concepts in a real-world software system.

| Concept | Intended Application |
|---|---|
| Queue | Order processing and FIFO order management |
| Priority Queue / Heap | Delivery partner assignment |
| Graph | Representation of delivery routes |
| Dijkstra's Algorithm | Shortest-path calculation |
| Sorting | Restaurant and menu sorting |
| Searching | Restaurant and food item discovery |

These integrations will be implemented as the corresponding project modules are developed.

## 6. Repository Structure

```text
Food-Delivery-Management-System/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fooddelivery/
│   │   │   └── resources/
│   │   └── test/
│   │
│   └── pom.xml
│
├── frontend/
├── design/
├── docs/
├── postman/
├── scripts/
│
├── .env.example
├── .gitignore
├── .editorconfig
├── .gitattributes
└── README.md
```

Some modules and directories are still being developed.

## 7. Prerequisites

| Requirement | Version |
|---|---|
| JDK | 21 |
| Maven | 3.9.x |
| MySQL | 8.x |
| Node.js | 20+ (for frontend development) |
| npm | Bundled with Node.js |

## 8. Getting Started

### Backend Setup

1. Clone the repository:

```bash
git clone https://github.com/AyushEkka915/Food-Delivery-Management-System.git
```

2. Navigate to the project directory:

```bash
cd Food-Delivery-Management-System
```

3. Configure your environment variables using `.env.example` as a reference.

4. Configure your MySQL database and credentials.

5. Activate the project environment in PowerShell:

```powershell
. .\scripts\env.ps1
```

6. Run the backend using Maven:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

Database migrations are managed by Flyway. Ensure that your database credentials and required environment variables are configured before starting the application.

## 9. Development Roadmap

| Phase | Description | Status |
|---|---|---|
| 0 | Environment and repository setup | Completed |
| 1 | Requirements and architecture | Completed |
| 2 | UI/UX and wireframes | Completed |
| 3 | Database design and ER model | Completed |
| 4 | Spring Boot initialization | Completed |
| 5 | Domain model and backend foundation | Completed |
| 6 | Authentication and role-based access | Completed |
| 7 | Restaurant and menu management | In Progress |
| 8 | DSA core implementations | Planned |
| 9 | Cart, pricing and coupons | Planned |
| 10 | Order lifecycle and FIFO queue | Planned |
| 11 | Delivery assignment using heaps | Planned |
| 12 | Graph algorithms and route optimization | Planned |
| 13 | React frontend | Planned |
| 14 | WebSocket live tracking | Planned |
| 15 | Payment integration | Planned |
| 16 | Admin analytics and audit | Planned |
| 17 | Testing and optimization | In Progress |
| 18 | Deployment and documentation | Planned |

*Roadmap statuses reflect the current development stage and may change as implementation progresses.*

## 10. Project Goals

- Build a functional full-stack food delivery application.
- Apply Java and Spring Boot backend development concepts.
- Implement meaningful DSA concepts in practical workflows.
- Design and manage a relational database using MySQL.
- Develop secure REST APIs.
- Follow modular architecture and software engineering practices.
- Gain experience with testing, version control, and deployment.

## 11. Author

**Ayush Ekka**

B.Tech – Computer Science and Engineering

GitHub: [AyushEkka915](https://github.com/AyushEkka915)

---

**Note:** This is an academic project under active development. Not all planned features are currently implemented. The repository will be updated as development progresses.
