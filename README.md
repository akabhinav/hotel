# Hotel Reservation System

A comprehensive hotel reservation system built with **Java 21** and **Spring Boot 3.2**, designed to handle hotel room bookings with support for dynamic pricing, inventory management, concurrency control, and overbooking.

## Features

### Core Functionality
- ✅ Hotel and room management (CRUD operations)
- ✅ Room type categorization with dynamic pricing
- ✅ Guest management
- ✅ Reservation system with inventory tracking
- ✅ Support for 10% overbooking
- ✅ Dynamic pricing based on dates
- ✅ Reservation cancellation and refunds

### Technical Features
- ✅ **Concurrency Control**: Optimistic locking with automatic retry
- ✅ **Idempotency**: Prevention of double booking using reservation IDs
- ✅ **ACID Compliance**: Database transactions for data consistency
- ✅ **RESTful APIs**: Well-designed REST endpoints
- ✅ **Comprehensive Testing**: Unit tests and integration tests
- ✅ **Sample Data Loader**: Auto-populates database for testing

## Technology Stack

- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.0
- **Spring Data JPA**: Database abstraction
- **H2 Database**: In-memory database for local testing
- **PostgreSQL**: Optional for production-like testing
- **Lombok**: Reduces boilerplate code
- **Spring Retry**: Automatic retry for optimistic locking failures
- **JUnit 5**: Unit and integration testing
- **Maven**: Dependency management and build tool

## Project Structure

```
hotel-reservation-system/
├── src/
│   ├── main/
│   │   ├── java/com/hotel/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── DataLoader.java  # Sample data loader
│   │   │   │   └── RetryConfig.java # Retry configuration
│   │   │   ├── controller/          # REST controllers
│   │   │   │   ├── HotelController.java
│   │   │   │   ├── RoomController.java
│   │   │   │   ├── GuestController.java
│   │   │   │   └── ReservationController.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── HotelRequest.java
│   │   │   │   ├── RoomRequest.java
│   │   │   │   ├── RoomTypeRequest.java
│   │   │   │   ├── GuestRequest.java
│   │   │   │   └── ReservationRequest.java
│   │   │   ├── exception/           # Custom exceptions
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── InsufficientInventoryException.java
│   │   │   │   ├── DuplicateReservationException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/               # Domain entities
│   │   │   │   ├── Hotel.java
│   │   │   │   ├── Room.java
│   │   │   │   ├── RoomType.java
│   │   │   │   ├── Guest.java
│   │   │   │   ├── Reservation.java
│   │   │   │   ├── ReservationStatus.java
│   │   │   │   ├── RoomTypeRate.java
│   │   │   │   └── RoomTypeInventory.java
│   │   │   ├── repository/          # JPA repositories
│   │   │   │   ├── HotelRepository.java
│   │   │   │   ├── RoomRepository.java
│   │   │   │   ├── RoomTypeRepository.java
│   │   │   │   ├── GuestRepository.java
│   │   │   │   ├── ReservationRepository.java
│   │   │   │   ├── RoomTypeRateRepository.java
│   │   │   │   └── RoomTypeInventoryRepository.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── HotelService.java
│   │   │   │   ├── RoomService.java
│   │   │   │   ├── RoomTypeService.java
│   │   │   │   ├── GuestService.java
│   │   │   │   └── ReservationService.java
│   │   │   └── HotelReservationApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/hotel/
│           ├── controller/
│           │   └── HotelControllerTest.java
│           ├── service/
│           │   └── ReservationServiceTest.java
│           └── integration/
│               └── ReservationIntegrationTest.java
├── pom.xml
└── README.md
```

## Database Schema

### Core Tables

#### hotels
| Column      | Type    | Description          |
|-------------|---------|----------------------|
| hotel_id    | BIGINT  | Primary key          |
| name        | VARCHAR | Hotel name           |
| address     | VARCHAR | Hotel address        |
| location    | VARCHAR | City/region          |
| description | VARCHAR | Hotel description    |

#### room_types
| Column        | Type    | Description              |
|---------------|---------|--------------------------|
| room_type_id  | BIGINT  | Primary key              |
| hotel_id      | BIGINT  | Foreign key to hotels    |
| name          | VARCHAR | Room type name           |
| description   | VARCHAR | Room type description    |
| capacity      | INT     | Maximum occupancy        |

#### rooms
| Column       | Type    | Description                |
|--------------|---------|----------------------------|
| room_id      | BIGINT  | Primary key                |
| hotel_id     | BIGINT  | Foreign key to hotels      |
| room_type_id | BIGINT  | Foreign key to room_types  |
| room_number  | VARCHAR | Room number                |
| floor        | INT     | Floor number               |
| is_available | BOOLEAN | Availability status        |
| notes        | VARCHAR | Additional notes           |

#### guests
| Column     | Type    | Description     |
|------------|---------|-----------------|
| guest_id   | BIGINT  | Primary key     |
| first_name | VARCHAR | First name      |
| last_name  | VARCHAR | Last name       |
| email      | VARCHAR | Email (unique)  |
| phone      | VARCHAR | Phone number    |

#### reservations
| Column         | Type      | Description                |
|----------------|-----------|----------------------------|
| reservation_id | VARCHAR   | Primary key (UUID)         |
| hotel_id       | BIGINT    | Foreign key to hotels      |
| room_type_id   | BIGINT    | Foreign key to room_types  |
| guest_id       | BIGINT    | Foreign key to guests      |
| start_date     | DATE      | Check-in date              |
| end_date       | DATE      | Check-out date             |
| room_count     | INT       | Number of rooms            |
| status         | VARCHAR   | Reservation status         |
| total_price    | DECIMAL   | Total price                |
| created_at     | TIMESTAMP | Creation timestamp         |
| updated_at     | TIMESTAMP | Last update timestamp      |

#### room_type_rates
| Column       | Type    | Description (Composite PK) |
|--------------|---------|----------------------------|
| hotel_id     | BIGINT  | Part of composite key      |
| room_type_id | BIGINT  | Part of composite key      |
| date         | DATE    | Part of composite key      |
| rate         | DECIMAL | Price for the date         |

#### room_type_inventory
| Column          | Type   | Description (Composite PK) |
|-----------------|--------|----------------------------|
| hotel_id        | BIGINT | Part of composite key      |
| room_type_id    | BIGINT | Part of composite key      |
| date            | DATE   | Part of composite key      |
| total_inventory | INT    | Total rooms available      |
| total_reserved  | INT    | Total rooms reserved       |
| version         | BIGINT | Optimistic locking version |

## API Documentation

### Hotel APIs

#### Get All Hotels
```
GET /v1/hotels
Query Parameters:
  - location (optional): Filter by location
  - name (optional): Filter by name
Response: List of hotels
```

#### Get Hotel by ID
```
GET /v1/hotels/{id}
Response: Hotel details
```

#### Create Hotel
```
POST /v1/hotels
Body:
{
  "name": "Hotel Name",
  "address": "123 Main St",
  "location": "City, State",
  "description": "Hotel description"
}
Response: Created hotel (201)
```

#### Update Hotel
```
PUT /v1/hotels/{id}
Body: Same as create
Response: Updated hotel
```

#### Delete Hotel
```
DELETE /v1/hotels/{id}
Response: 204 No Content
```

### Room Type APIs

#### Get Room Types for Hotel
```
GET /v1/hotels/{hotelId}/room-types
Response: List of room types
```

#### Create Room Type
```
POST /v1/hotels/{hotelId}/room-types
Body:
{
  "name": "Standard Room",
  "description": "Comfortable standard room",
  "capacity": 2
}
Response: Created room type (201)
```

### Room APIs

#### Get Rooms for Hotel
```
GET /v1/hotels/{hotelId}/rooms
Response: List of rooms
```

#### Create Room
```
POST /v1/hotels/{hotelId}/rooms
Body:
{
  "roomTypeId": 1,
  "roomNumber": "101",
  "floor": 1,
  "isAvailable": true,
  "notes": "Ocean view"
}
Response: Created room (201)
```

### Guest APIs

#### Get All Guests
```
GET /v1/guests
Response: List of guests
```

#### Create Guest
```
POST /v1/guests
Body:
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@email.com",
  "phone": "+1-555-0101"
}
Response: Created guest (201)
```

### Reservation APIs

#### Check Availability
```
POST /v1/reservations/check-availability
Query Parameters:
  - hotelId: Hotel ID
  - roomTypeId: Room type ID
  - startDate: Check-in date (YYYY-MM-DD)
  - endDate: Check-out date (YYYY-MM-DD)
  - roomCount: Number of rooms
Response:
{
  "available": true,
  "hotelId": 1,
  "roomTypeId": 1,
  "startDate": "2024-01-01",
  "endDate": "2024-01-03",
  "roomCount": 2,
  "totalPrice": 400.00
}
```

#### Create Reservation
```
POST /v1/reservations
Body:
{
  "reservationId": "RES-12345",  // Idempotency key (UUID recommended)
  "hotelId": 1,
  "roomTypeId": 1,
  "guestId": 1,
  "startDate": "2024-01-01",
  "endDate": "2024-01-03",
  "roomCount": 2
}
Response: Created reservation (201)
```

#### Get Reservation by ID
```
GET /v1/reservations/{reservationId}
Response: Reservation details
```

#### Get Reservations
```
GET /v1/reservations
Query Parameters:
  - guestId (optional): Filter by guest
  - hotelId (optional): Filter by hotel
Response: List of reservations
```

#### Cancel Reservation
```
DELETE /v1/reservations/{reservationId}
Response: Canceled reservation
```

#### Mark as Paid
```
POST /v1/reservations/{reservationId}/pay
Response: Updated reservation with PAID status
```

#### Reject Reservation
```
POST /v1/reservations/{reservationId}/reject
Response: Updated reservation with REJECTED status
```

## Key Design Decisions

### 1. Concurrency Control - Optimistic Locking

The system uses **optimistic locking** with automatic retry to handle concurrent reservations:

```java
@Version
private Long version;  // In RoomTypeInventory entity
```

**Why Optimistic Locking?**
- Lower overhead than pessimistic locking
- Better performance for low-to-medium contention
- Automatic retry mechanism for failed transactions
- No database locks held during user think time

**Retry Configuration:**
```java
@Retryable(
    retryFor = {OptimisticLockingFailureException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
```

### 2. Idempotency - Preventing Double Booking

Uses `reservationId` as the primary key to ensure idempotency:

```java
@Id
@Column(nullable = false, unique = true)
private String reservationId;  // Client-provided UUID
```

**Benefits:**
- Prevents duplicate reservations from double-clicks
- Allows safe retry of failed requests
- Unique constraint at database level

### 3. Overbooking Support (10%)

Inventory check allows 10% overbooking:

```java
public boolean hasAvailability(int roomsRequested) {
    int maxAllowed = (int) Math.floor(totalInventory * 1.1); // 10% overbooking
    return (totalReserved + roomsRequested) <= maxAllowed;
}
```

### 4. Room Type vs. Specific Room

Reservations are made for **room types**, not specific rooms:
- Room numbers are assigned at check-in
- More flexible inventory management
- Follows real-world hotel practices

### 5. Dynamic Pricing

Prices vary by date and are stored in `room_type_rates` table:
- Supports weekend pricing (20% markup in sample data)
- Seasonal pricing support
- Event-based pricing capability

## Running the Application

### Prerequisites

- Java 21 or later
- Maven 3.6+
- (Optional) PostgreSQL for production-like testing

### Build and Run

1. **Clone the repository**
   ```bash
   cd hotel
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Base URL: `http://localhost:8080`
   - H2 Console: `http://localhost:8080/h2-console`
     - JDBC URL: `jdbc:h2:mem:hoteldb`
     - Username: `sa`
     - Password: (empty)

### Running Tests

**Run all tests:**
```bash
mvn test
```

**Run specific test class:**
```bash
mvn test -Dtest=ReservationServiceTest
```

**Run integration tests only:**
```bash
mvn test -Dtest=*IntegrationTest
```

## Sample Data

The application automatically loads sample data on startup:

- **3 Hotels**: Marriott, Hilton, Hyatt
- **5 Room Types**: Standard, King, Suite, Deluxe
- **350+ Rooms**: Distributed across hotels
- **5 Guests**: Sample guests for testing
- **365 Days of Rates**: Dynamic pricing for next year
- **365 Days of Inventory**: Pre-populated inventory

### Sample Hotel IDs

After starting the application, hotels will have IDs 1, 2, 3:
- ID 1: San Francisco Marriott Marquis
- ID 2: Hilton San Francisco Union Square
- ID 3: Grand Hyatt San Francisco

## Testing the APIs

### Example 1: Check Availability

```bash
curl -X POST "http://localhost:8080/v1/reservations/check-availability?hotelId=1&roomTypeId=1&startDate=2024-12-25&endDate=2024-12-27&roomCount=2"
```

### Example 2: Create a Reservation

```bash
curl -X POST http://localhost:8080/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "reservationId": "RES-'$(uuidgen)'",
    "hotelId": 1,
    "roomTypeId": 1,
    "guestId": 1,
    "startDate": "2024-12-25",
    "endDate": "2024-12-27",
    "roomCount": 2
  }'
```

### Example 3: Test Idempotency (Double Booking Prevention)

```bash
# First request - should succeed
curl -X POST http://localhost:8080/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "reservationId": "RES-TEST-123",
    "hotelId": 1,
    "roomTypeId": 1,
    "guestId": 1,
    "startDate": "2024-12-25",
    "endDate": "2024-12-27",
    "roomCount": 1
  }'

# Second request with same ID - should fail with 409 Conflict
curl -X POST http://localhost:8080/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "reservationId": "RES-TEST-123",
    "hotelId": 1,
    "roomTypeId": 1,
    "guestId": 1,
    "startDate": "2024-12-25",
    "endDate": "2024-12-27",
    "roomCount": 1
  }'
```

### Example 4: Test Overbooking (10% Limit)

The system allows 10% overbooking. For example, if total inventory is 100:
- Can book up to 110 rooms (100 + 10%)
- 111th booking will fail

### Example 5: Cancel Reservation

```bash
curl -X DELETE http://localhost:8080/v1/reservations/RES-TEST-123
```

### Example 6: Mark as Paid

```bash
curl -X POST http://localhost:8080/v1/reservations/RES-TEST-123/pay
```

## Concurrency Testing

### Test Scenario: Multiple Concurrent Reservations

To test concurrent booking attempts:

```bash
# Terminal 1
curl -X POST http://localhost:8080/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "reservationId": "RES-CONCURRENT-1",
    "hotelId": 1,
    "roomTypeId": 1,
    "guestId": 1,
    "startDate": "2024-12-30",
    "endDate": "2025-01-02",
    "roomCount": 50
  }' &

# Terminal 2 (run simultaneously)
curl -X POST http://localhost:8080/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "reservationId": "RES-CONCURRENT-2",
    "hotelId": 1,
    "roomTypeId": 1,
    "guestId": 2,
    "startDate": "2024-12-30",
    "endDate": "2025-01-02",
    "roomCount": 50
  }' &
```

**Expected Behavior:**
- Both requests attempt to book 50 rooms each
- Optimistic locking ensures only one succeeds initially
- Failed request automatically retries (up to 3 attempts)
- System maintains data integrity

## Error Handling

The system handles various error scenarios:

| Error | HTTP Status | Description |
|-------|-------------|-------------|
| ResourceNotFoundException | 404 | Entity not found |
| InsufficientInventoryException | 409 | Not enough rooms available |
| DuplicateReservationException | 409 | Reservation ID already exists |
| ValidationException | 400 | Invalid request data |
| OptimisticLockingFailureException | 500 (retried) | Concurrent modification |

## Performance Considerations

### Database Queries

- Uses indexed composite primary keys for fast lookups
- Optimized queries for date range searches
- Bulk operations for inventory updates

### Caching Strategy

For production deployment, consider:
- Redis cache for inventory data
- Cache TTL based on booking window
- Async cache updates

### Scaling

The application can be scaled:
- **Horizontally**: Multiple instances behind load balancer
- **Database**: Sharding by hotel_id
- **Read replicas**: For query-heavy operations

## Monitoring and Logging

The application logs:
- All reservation attempts
- Concurrency conflicts and retries
- Inventory updates
- API requests and responses

Logging configuration in `application.properties`:
```properties
logging.level.com.hotel=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

## Future Enhancements

Potential improvements:
- [ ] Payment gateway integration
- [ ] Email notifications
- [ ] Search functionality with filters
- [ ] Admin dashboard
- [ ] Booking analytics
- [ ] Multi-currency support
- [ ] Loyalty programs
- [ ] Room assignment algorithm
- [ ] Housekeeping integration
- [ ] Revenue management system

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is created for educational purposes.

## Contact

For questions or support, please open an issue in the repository.

---

**Built with ❤️ using Java 21 and Spring Boot**
