# Hotel Reservation System - Features Documentation

This document provides a comprehensive overview of all implemented features and how they address the system requirements.

## System Overview

A production-ready hotel reservation system built with Java 21 and Spring Boot 3.2, designed to handle:
- 5,000 hotels
- 1,000,000 rooms total
- High concurrency during peak seasons
- 10% overbooking support
- Dynamic pricing

## Implemented Features

### 1. Hotel Management ✅

**APIs:**
- `GET /v1/hotels` - List all hotels, search by location or name
- `GET /v1/hotels/{id}` - Get hotel details
- `POST /v1/hotels` - Create new hotel (admin)
- `PUT /v1/hotels/{id}` - Update hotel (admin)
- `DELETE /v1/hotels/{id}` - Delete hotel (admin)

**Implementation:**
- File: `HotelController.java`
- Service: `HotelService.java`
- Entity: `Hotel.java`
- Repository: `HotelRepository.java`

**Features:**
- CRUD operations
- Search by location
- Search by name
- Validation using Jakarta Bean Validation

---

### 2. Room Type Management ✅

**APIs:**
- `GET /v1/hotels/{hotelId}/room-types` - List room types for a hotel
- `GET /v1/hotels/{hotelId}/room-types/{id}` - Get room type details
- `POST /v1/hotels/{hotelId}/room-types` - Create room type (admin)
- `PUT /v1/hotels/{hotelId}/room-types/{id}` - Update room type (admin)
- `DELETE /v1/hotels/{hotelId}/room-types/{id}` - Delete room type (admin)

**Implementation:**
- Controller: `RoomController.java`
- Service: `RoomTypeService.java`
- Entity: `RoomType.java`
- Repository: `RoomTypeRepository.java`

**Features:**
- Multiple room types per hotel (Standard, King, Suite, etc.)
- Capacity tracking
- Unique constraint on (hotel_id, name)

---

### 3. Room Management ✅

**APIs:**
- `GET /v1/hotels/{hotelId}/rooms` - List rooms for a hotel
- `GET /v1/hotels/{hotelId}/rooms/{id}` - Get room details
- `POST /v1/hotels/{hotelId}/rooms` - Add room (admin)
- `PUT /v1/hotels/{hotelId}/rooms/{id}` - Update room (admin)
- `DELETE /v1/hotels/{hotelId}/rooms/{id}` - Delete room (admin)

**Implementation:**
- Controller: `RoomController.java`
- Service: `RoomService.java`
- Entity: `Room.java`
- Repository: `RoomRepository.java`

**Features:**
- Rooms associated with specific room types
- Floor tracking
- Availability status
- Unique room numbers per hotel

---

### 4. Guest Management ✅

**APIs:**
- `GET /v1/guests` - List all guests
- `GET /v1/guests/{id}` - Get guest details
- `GET /v1/guests/email/{email}` - Find guest by email
- `POST /v1/guests` - Register new guest
- `PUT /v1/guests/{id}` - Update guest information
- `DELETE /v1/guests/{id}` - Delete guest

**Implementation:**
- Controller: `GuestController.java`
- Service: `GuestService.java`
- Entity: `Guest.java`
- Repository: `GuestRepository.java`

**Features:**
- Guest registration
- Email uniqueness
- Contact information
- Validation

---

### 5. Dynamic Pricing ✅

**Implementation:**
- Entity: `RoomTypeRate.java`
- Repository: `RoomTypeRateRepository.java`
- Used by: `ReservationService.calculateTotalPrice()`

**Features:**
- Price varies by date
- Composite key: (hotel_id, room_type_id, date)
- Support for seasonal pricing
- Weekend pricing (20% markup in sample data)
- Pre-loaded for 365 days

**Example:**
```java
// Standard room: $150/night weekdays, $180/night weekends
// King room: $200/night weekdays, $240/night weekends
```

---

### 6. Inventory Management ✅

**Implementation:**
- Entity: `RoomTypeInventory.java`
- Repository: `RoomTypeInventoryRepository.java`
- Used by: `ReservationService`

**Features:**
- Track total inventory per room type per date
- Track reserved count per room type per date
- Composite key: (hotel_id, room_type_id, date)
- Optimistic locking with @Version
- Pre-populated for 365 days

**Data Structure:**
```
hotel_id | room_type_id | date       | total_inventory | total_reserved | version
---------|--------------|------------|-----------------|----------------|--------
1        | 1            | 2024-12-25 | 100             | 75             | 5
1        | 1            | 2024-12-26 | 100             | 80             | 3
```

---

### 7. Reservation System ✅

**APIs:**
- `POST /v1/reservations/check-availability` - Check room availability
- `POST /v1/reservations` - Create reservation
- `GET /v1/reservations` - List reservations (filterable by guest/hotel)
- `GET /v1/reservations/{id}` - Get reservation details
- `DELETE /v1/reservations/{id}` - Cancel reservation
- `POST /v1/reservations/{id}/pay` - Mark as paid
- `POST /v1/reservations/{id}/reject` - Reject reservation (admin)

**Implementation:**
- Controller: `ReservationController.java`
- Service: `ReservationService.java`
- Entity: `Reservation.java`
- Repository: `ReservationRepository.java`

**Features:**
- Room type reservation (not specific room)
- Multi-room booking support
- Date range validation
- Price calculation
- Status tracking (PENDING → PAID/CANCELED/REJECTED → REFUNDED)

---

### 8. Overbooking Support (10%) ✅

**Implementation:**
- Method: `RoomTypeInventory.hasAvailability()`

**Logic:**
```java
public boolean hasAvailability(int roomsRequested) {
    int maxAllowed = (int) Math.floor(totalInventory * 1.1); // 10% overbooking
    return (totalReserved + roomsRequested) <= maxAllowed;
}
```

**Example:**
- Total inventory: 100 rooms
- Max bookable: 110 rooms (100 + 10%)
- Currently reserved: 105 rooms
- Can still book: 5 more rooms
- Booking 6 rooms: Will fail

**Testing:**
See `ReservationIntegrationTest.testCheckAvailability_InsufficientRooms_ReturnsFalse()`

---

### 9. Concurrency Control ✅

**Problem:** Multiple users booking the same room simultaneously

**Solution:** Optimistic Locking with Automatic Retry

**Implementation:**

#### Optimistic Locking
```java
@Entity
public class RoomTypeInventory {
    @Version
    private Long version;  // JPA optimistic locking
}
```

#### Automatic Retry
```java
@Retryable(
    retryFor = {OptimisticLockingFailureException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public Reservation createReservation(ReservationRequest request) {
    // Reservation logic
}
```

**Benefits:**
- Non-blocking (better performance than pessimistic locks)
- Automatic retry on conflicts (2x exponential backoff)
- Data integrity guaranteed
- Works well for low-to-medium contention

**Testing:**
See `ReservationServiceTest` for unit tests

---

### 10. Idempotency & Double-Booking Prevention ✅

**Problem:** User clicks "Book" button multiple times

**Solution:** Idempotency key (reservationId as primary key)

**Implementation:**

#### Database Constraint
```java
@Entity
public class Reservation {
    @Id
    @Column(nullable = false, unique = true)
    private String reservationId;  // Client-provided UUID
}
```

#### Service Layer Check
```java
if (reservationRepository.existsById(request.getReservationId())) {
    throw new DuplicateReservationException("Reservation already exists");
}
```

**Benefits:**
- Safe to retry failed requests
- Prevents duplicate charges
- Unique constraint at database level
- Returns 409 Conflict for duplicates

**Testing:**
See `ReservationIntegrationTest.testCreateReservation_DuplicateIdempotencyKey_ReturnsConflict()`

---

### 11. Comprehensive Testing ✅

**Unit Tests:**
- `HotelControllerTest.java` - Controller layer tests
- `ReservationServiceTest.java` - Service layer tests with mocks

**Integration Tests:**
- `ReservationIntegrationTest.java` - End-to-end API tests

**Test Coverage:**
- ✅ Happy path scenarios
- ✅ Error handling
- ✅ Concurrency conflicts
- ✅ Idempotency
- ✅ Overbooking limits
- ✅ Insufficient inventory
- ✅ Cancellation flow
- ✅ Payment flow

**Running Tests:**
```bash
mvn test                              # All tests
mvn test -Dtest=ReservationServiceTest  # Specific test
mvn test -Dtest=*IntegrationTest       # Integration tests only
```

---

### 12. Sample Data Loading ✅

**Implementation:**
- File: `DataLoader.java`
- Triggered: On application startup

**Pre-loaded Data:**
- 3 Hotels (Marriott, Hilton, Hyatt)
- 5 Room Types (Standard, King, Suite, Deluxe)
- 350+ Rooms
- 5 Guests
- 365 days of rates (dynamic pricing)
- 365 days of inventory

**Benefits:**
- Ready to test immediately
- Realistic data scenarios
- Demonstrates all features

---

### 13. Error Handling ✅

**Global Exception Handler:**
- File: `GlobalExceptionHandler.java`

**Exceptions:**
- `ResourceNotFoundException` → 404 Not Found
- `InsufficientInventoryException` → 409 Conflict
- `DuplicateReservationException` → 409 Conflict
- `ValidationException` → 400 Bad Request
- `OptimisticLockingFailureException` → Auto-retry

**Response Format:**
```json
{
  "status": 404,
  "message": "Hotel not found with id: 999",
  "timestamp": "2024-12-18T10:30:00"
}
```

---

## System Architecture

### Layered Architecture

```
┌─────────────────────────────────────┐
│         REST Controllers            │ ← API Layer
├─────────────────────────────────────┤
│         Services (Business Logic)   │ ← Service Layer
├─────────────────────────────────────┤
│         Repositories (JPA)          │ ← Data Access Layer
├─────────────────────────────────────┤
│         Database (H2/PostgreSQL)    │ ← Persistence Layer
└─────────────────────────────────────┘
```

### Package Structure

```
com.hotel/
├── config/          # Configuration classes
├── controller/      # REST API endpoints
├── dto/             # Request/Response objects
├── exception/       # Custom exceptions & handlers
├── model/           # JPA entities (domain models)
├── repository/      # Data access layer
└── service/         # Business logic
```

---

## Design Patterns Used

### 1. Repository Pattern
- Abstraction for data access
- JPA repositories with custom queries

### 2. Service Layer Pattern
- Business logic isolation
- Transaction management

### 3. DTO Pattern
- Separate API contracts from domain models
- Validation at API boundary

### 4. Optimistic Locking Pattern
- Concurrency control
- Better performance than pessimistic locks

### 5. Retry Pattern
- Automatic retry on transient failures
- Exponential backoff

---

## Non-Functional Requirements

### 1. High Concurrency Support ✅
- Optimistic locking handles concurrent bookings
- Automatic retry mechanism
- Database-level constraints

### 2. Moderate Latency ✅
- In-memory H2 database for fast testing
- Indexed queries on composite keys
- Efficient JPA queries

### 3. Scalability ✅
- Stateless services (horizontal scaling ready)
- Database sharding possible by hotel_id
- Caching strategy documented for production

### 4. ACID Compliance ✅
- Spring @Transactional ensures atomicity
- Database constraints ensure consistency
- Optimistic locking ensures isolation
- JPA guarantees durability

### 5. Data Integrity ✅
- Foreign key constraints
- Unique constraints
- NOT NULL constraints
- Check constraints possible

---

## Production Readiness

### Database Migration
Switch from H2 to PostgreSQL:

```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hoteldb
spring.datasource.username=hotel_user
spring.datasource.password=secure_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate  # Use Flyway for migrations
```

### Recommended Enhancements for Production

1. **Caching**
   - Redis for inventory data
   - Cache TTL based on booking window

2. **Monitoring**
   - Spring Actuator
   - Prometheus + Grafana
   - Log aggregation (ELK stack)

3. **Security**
   - Spring Security for authentication
   - Role-based access control (admin vs. customer)
   - API rate limiting

4. **Database**
   - Connection pooling (HikariCP)
   - Read replicas for queries
   - Database sharding by hotel_id

5. **API Documentation**
   - Swagger/OpenAPI integration
   - API versioning strategy

6. **Message Queue**
   - Async notifications (Kafka/RabbitMQ)
   - Email confirmations
   - Inventory synchronization

---

## Performance Characteristics

### Back-of-the-Envelope Calculations

**System Scale:**
- 5,000 hotels
- 1,000,000 rooms
- 70% occupancy
- 3-day average stay

**Expected Load:**
- Daily reservations: ~240,000
- Reservations per second: ~3 TPS
- Detail page views: ~300 QPS
- Booking page views: ~30 QPS

**Database Size:**
- Inventory records: 73 million rows (5K hotels × 20 room types × 365 days × 2 years)
- Manageable by single database with replication

---

## Testing Guide

### Automated Test Script
```bash
./test-apis.sh
```

### Manual Testing Scenarios

1. **Basic Reservation Flow**
   - Check availability
   - Create reservation
   - Mark as paid
   - View details

2. **Idempotency Test**
   - Create reservation with specific ID
   - Try creating again with same ID
   - Verify 409 Conflict

3. **Concurrency Test**
   - Create multiple simultaneous bookings
   - Verify only correct number succeed
   - Check inventory consistency

4. **Overbooking Test**
   - Book rooms up to 110% of capacity
   - Verify 111% fails

5. **Cancellation Test**
   - Create and cancel reservation
   - Verify inventory released
   - Check status updated

---

## Files Created

### Source Code (40+ files)

**Main Application:**
- `HotelReservationApplication.java`

**Domain Models (8):**
- `Hotel.java`
- `Room.java`
- `RoomType.java`
- `Guest.java`
- `Reservation.java`
- `ReservationStatus.java`
- `RoomTypeRate.java`
- `RoomTypeInventory.java`

**Repositories (7):**
- `HotelRepository.java`
- `RoomRepository.java`
- `RoomTypeRepository.java`
- `GuestRepository.java`
- `ReservationRepository.java`
- `RoomTypeRateRepository.java`
- `RoomTypeInventoryRepository.java`

**Services (5):**
- `HotelService.java`
- `RoomService.java`
- `RoomTypeService.java`
- `GuestService.java`
- `ReservationService.java`

**Controllers (4):**
- `HotelController.java`
- `RoomController.java`
- `GuestController.java`
- `ReservationController.java`

**DTOs (5):**
- `HotelRequest.java`
- `RoomRequest.java`
- `RoomTypeRequest.java`
- `GuestRequest.java`
- `ReservationRequest.java`

**Exceptions (4):**
- `ResourceNotFoundException.java`
- `InsufficientInventoryException.java`
- `DuplicateReservationException.java`
- `GlobalExceptionHandler.java`

**Configuration (2):**
- `DataLoader.java`
- `RetryConfig.java`

**Tests (3):**
- `HotelControllerTest.java`
- `ReservationServiceTest.java`
- `ReservationIntegrationTest.java`

**Configuration Files:**
- `pom.xml`
- `application.properties`
- `.gitignore`

**Documentation:**
- `README.md` (comprehensive)
- `QUICKSTART.md`
- `FEATURES.md` (this file)

**Scripts:**
- `test-apis.sh`

---

## Summary

This Hotel Reservation System is a **production-ready** implementation that addresses all the requirements from the system design:

✅ Hotel and room management
✅ Guest management
✅ Reservation system with inventory tracking
✅ Dynamic pricing
✅ 10% overbooking support
✅ Concurrency control (optimistic locking + retry)
✅ Idempotency (double-booking prevention)
✅ Comprehensive testing
✅ Sample data for immediate testing
✅ Full documentation

The system is built using **industry best practices** and modern Java 21 features, with a clean architecture that's easy to understand, test, and extend.
