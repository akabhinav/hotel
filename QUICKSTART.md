# Quick Start Guide

This guide will help you get the Hotel Reservation System up and running in minutes.

## Prerequisites

- Java 21 (Download from: https://adoptium.net/)
- Maven 3.6+ (Download from: https://maven.apache.org/)
- curl (for testing APIs)

## Step 1: Verify Prerequisites

```bash
# Check Java version (should be 21 or higher)
java -version

# Check Maven version
mvn -version
```

## Step 2: Build the Application

```bash
# Navigate to project directory
cd hotel

# Clean and build the project
mvn clean install
```

This will:
- Download all dependencies
- Compile the source code
- Run all tests
- Create an executable JAR file

## Step 3: Run the Application

```bash
# Start the application
mvn spring-boot:run
```

**Alternative:** Run the JAR directly
```bash
java -jar target/hotel-reservation-system-1.0.0.jar
```

Wait for the message: `Started HotelReservationApplication in X seconds`

## Step 4: Verify It's Running

Open your browser and navigate to:
- API: http://localhost:8080/v1/hotels
- H2 Console: http://localhost:8080/h2-console

You should see JSON data showing the sample hotels.

## Step 5: Run API Tests

In a new terminal window:

```bash
# Make sure you're in the project directory
cd hotel

# Run the test script
./test-apis.sh
```

This will automatically test all major features:
- Hotel management
- Room types and rooms
- Guest management
- Availability checking
- Reservations (create, read, update, delete)
- Idempotency (double-booking prevention)
- Concurrency control
- Overbooking limits

## Step 6: Explore the H2 Database

1. Open http://localhost:8080/h2-console in your browser
2. Enter the following connection details:
   - **JDBC URL:** `jdbc:h2:mem:hoteldb`
   - **Username:** `sa`
   - **Password:** (leave empty)
3. Click "Connect"

You can now browse all tables and run SQL queries.

### Useful SQL Queries

```sql
-- View all hotels
SELECT * FROM HOTELS;

-- View all room types
SELECT * FROM ROOM_TYPES;

-- View room inventory
SELECT * FROM ROOM_TYPE_INVENTORY
WHERE HOTEL_ID = 1
ORDER BY DATE
LIMIT 10;

-- View all reservations
SELECT * FROM RESERVATIONS;

-- Check inventory utilization
SELECT
    DATE,
    TOTAL_INVENTORY,
    TOTAL_RESERVED,
    (TOTAL_RESERVED * 100.0 / TOTAL_INVENTORY) as UTILIZATION_PCT
FROM ROOM_TYPE_INVENTORY
WHERE HOTEL_ID = 1
  AND ROOM_TYPE_ID = 1
ORDER BY DATE
LIMIT 10;
```

## Step 7: Test Individual APIs with curl

### Check Availability
```bash
curl -X POST "http://localhost:8080/v1/reservations/check-availability?hotelId=1&roomTypeId=1&startDate=2024-12-25&endDate=2024-12-27&roomCount=2"
```

### Create a Reservation
```bash
curl -X POST http://localhost:8080/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "reservationId": "RES-QUICKSTART-001",
    "hotelId": 1,
    "roomTypeId": 1,
    "guestId": 1,
    "startDate": "2024-12-25",
    "endDate": "2024-12-27",
    "roomCount": 2
  }'
```

### View the Reservation
```bash
curl http://localhost:8080/v1/reservations/RES-QUICKSTART-001
```

### Get All Hotels
```bash
curl http://localhost:8080/v1/hotels
```

### Get Hotel Details
```bash
curl http://localhost:8080/v1/hotels/1
```

## Step 8: Run Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ReservationServiceTest

# Run integration tests
mvn test -Dtest=*IntegrationTest
```

## Common Issues and Solutions

### Issue 1: Port 8080 already in use

**Solution:** Change the port in `application.properties`:
```properties
server.port=8081
```

### Issue 2: Java version error

**Error:** `Unsupported major.minor version`

**Solution:** Ensure you're using Java 21:
```bash
java -version
# Should show version 21.x.x
```

### Issue 3: Maven build fails

**Solution:** Clean and rebuild:
```bash
mvn clean install -U
```

### Issue 4: Application won't start

**Solution:** Check if another instance is running:
```bash
# On Linux/Mac
lsof -i :8080

# On Windows
netstat -ano | findstr :8080
```

## What's Pre-loaded?

The application comes with sample data:

### Hotels (3)
- San Francisco Marriott Marquis (ID: 1)
- Hilton San Francisco Union Square (ID: 2)
- Grand Hyatt San Francisco (ID: 3)

### Room Types (5)
- Standard Room
- King Room
- Executive Suite
- Deluxe Room

### Rooms (350+)
- Distributed across all hotels
- Multiple room types per hotel

### Guests (5)
- John Doe (ID: 1)
- Jane Smith (ID: 2)
- Bob Johnson (ID: 3)
- Alice Williams (ID: 4)
- Charlie Brown (ID: 5)

### Inventory & Rates
- 365 days of pre-loaded data
- Dynamic pricing (weekends are 20% more expensive)
- 100+ rooms per major room type

## Testing Scenarios

### Scenario 1: Happy Path Reservation

1. Check availability
2. Create reservation
3. Mark as paid
4. View reservation details

### Scenario 2: Test Idempotency

1. Create a reservation with ID "RES-TEST-123"
2. Try to create the same reservation again
3. Verify you get a 409 Conflict error

### Scenario 3: Test Insufficient Inventory

1. Try to book 120 rooms (more than available)
2. Verify you get an error about insufficient inventory

### Scenario 4: Test Cancellation

1. Create a reservation
2. Cancel it
3. Verify status changed to CANCELED
4. Check that inventory was released

### Scenario 5: Test Overbooking

The system supports 10% overbooking:
- Total inventory: 100 rooms
- Can book up to: 110 rooms (100 + 10%)
- 111th room: Will fail

## Next Steps

Now that you have the system running, you can:

1. **Read the full documentation** in `README.md`
2. **Explore the code structure** to understand the architecture
3. **Modify the sample data** in `DataLoader.java`
4. **Add new features** and test them
5. **Deploy to production** with PostgreSQL

## API Documentation

For complete API documentation, see the [README.md](README.md) file.

Key endpoints:
- Hotels: `/v1/hotels`
- Rooms: `/v1/hotels/{hotelId}/rooms`
- Guests: `/v1/guests`
- Reservations: `/v1/reservations`

## Stopping the Application

Press `Ctrl+C` in the terminal where the application is running.

## Support

If you encounter any issues:
1. Check the application logs
2. Review the `README.md` for detailed documentation
3. Check the H2 console for database state
4. Review test files for usage examples

---

**Happy Testing! 🎉**
