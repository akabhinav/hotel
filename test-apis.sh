#!/bin/bash

# Hotel Reservation System API Test Script
# This script tests all major API endpoints

BASE_URL="http://localhost:8080"
CONTENT_TYPE="Content-Type: application/json"

echo "======================================"
echo "Hotel Reservation System API Tests"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test header
print_test() {
    echo -e "${YELLOW}TEST: $1${NC}"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
    echo ""
}

# Function to print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
    echo ""
}

# Wait for application to start
echo "Checking if application is running..."
if ! curl -s "$BASE_URL/v1/hotels" > /dev/null 2>&1; then
    echo -e "${RED}Application is not running. Please start it first with: mvn spring-boot:run${NC}"
    exit 1
fi
print_success "Application is running"

# Test 1: Get all hotels
print_test "1. Get all hotels"
response=$(curl -s "$BASE_URL/v1/hotels")
if echo "$response" | grep -q "Marriott"; then
    print_success "Retrieved hotels successfully"
else
    print_error "Failed to retrieve hotels"
fi

# Test 2: Get specific hotel
print_test "2. Get hotel by ID (ID=1)"
response=$(curl -s "$BASE_URL/v1/hotels/1")
if echo "$response" | grep -q "hotelId"; then
    print_success "Retrieved hotel details successfully"
else
    print_error "Failed to retrieve hotel details"
fi

# Test 3: Get room types for hotel
print_test "3. Get room types for hotel ID=1"
response=$(curl -s "$BASE_URL/v1/hotels/1/room-types")
if echo "$response" | grep -q "roomTypeId"; then
    print_success "Retrieved room types successfully"
else
    print_error "Failed to retrieve room types"
fi

# Test 4: Get all guests
print_test "4. Get all guests"
response=$(curl -s "$BASE_URL/v1/guests")
if echo "$response" | grep -q "guestId"; then
    print_success "Retrieved guests successfully"
else
    print_error "Failed to retrieve guests"
fi

# Test 5: Check availability
print_test "5. Check room availability"
START_DATE=$(date -d "+7 days" +%Y-%m-%d)
END_DATE=$(date -d "+10 days" +%Y-%m-%d)
response=$(curl -s -X POST "$BASE_URL/v1/reservations/check-availability?hotelId=1&roomTypeId=1&startDate=$START_DATE&endDate=$END_DATE&roomCount=2")
if echo "$response" | grep -q '"available":true'; then
    print_success "Room availability check successful"
    echo "Response: $response"
    echo ""
else
    print_error "Room availability check failed"
fi

# Test 6: Create a reservation
print_test "6. Create a new reservation"
RESERVATION_ID="RES-TEST-$(date +%s)"
response=$(curl -s -X POST "$BASE_URL/v1/reservations" \
    -H "$CONTENT_TYPE" \
    -d '{
        "reservationId": "'$RESERVATION_ID'",
        "hotelId": 1,
        "roomTypeId": 1,
        "guestId": 1,
        "startDate": "'$START_DATE'",
        "endDate": "'$END_DATE'",
        "roomCount": 2
    }')
if echo "$response" | grep -q "PENDING"; then
    print_success "Reservation created successfully"
    echo "Reservation ID: $RESERVATION_ID"
    echo ""
else
    print_error "Failed to create reservation"
    echo "Response: $response"
    echo ""
fi

# Test 7: Get reservation details
print_test "7. Get reservation details"
response=$(curl -s "$BASE_URL/v1/reservations/$RESERVATION_ID")
if echo "$response" | grep -q "$RESERVATION_ID"; then
    print_success "Retrieved reservation details successfully"
else
    print_error "Failed to retrieve reservation details"
fi

# Test 8: Mark reservation as paid
print_test "8. Mark reservation as PAID"
response=$(curl -s -X POST "$BASE_URL/v1/reservations/$RESERVATION_ID/pay")
if echo "$response" | grep -q "PAID"; then
    print_success "Reservation marked as PAID successfully"
else
    print_error "Failed to mark reservation as paid"
fi

# Test 9: Test idempotency (duplicate reservation)
print_test "9. Test idempotency - Try to create duplicate reservation"
DUPLICATE_ID="RES-DUPLICATE-TEST"
# Create first reservation
curl -s -X POST "$BASE_URL/v1/reservations" \
    -H "$CONTENT_TYPE" \
    -d '{
        "reservationId": "'$DUPLICATE_ID'",
        "hotelId": 1,
        "roomTypeId": 2,
        "guestId": 2,
        "startDate": "'$START_DATE'",
        "endDate": "'$END_DATE'",
        "roomCount": 1
    }' > /dev/null

# Try to create duplicate
response=$(curl -s -X POST "$BASE_URL/v1/reservations" \
    -H "$CONTENT_TYPE" \
    -d '{
        "reservationId": "'$DUPLICATE_ID'",
        "hotelId": 1,
        "roomTypeId": 2,
        "guestId": 2,
        "startDate": "'$START_DATE'",
        "endDate": "'$END_DATE'",
        "roomCount": 1
    }')
if echo "$response" | grep -q "already exists"; then
    print_success "Idempotency working correctly - duplicate prevented"
else
    print_error "Idempotency check failed"
fi

# Test 10: Cancel a reservation
print_test "10. Cancel a reservation"
CANCEL_ID="RES-CANCEL-$(date +%s)"
# Create reservation to cancel
curl -s -X POST "$BASE_URL/v1/reservations" \
    -H "$CONTENT_TYPE" \
    -d '{
        "reservationId": "'$CANCEL_ID'",
        "hotelId": 1,
        "roomTypeId": 1,
        "guestId": 3,
        "startDate": "'$START_DATE'",
        "endDate": "'$END_DATE'",
        "roomCount": 1
    }' > /dev/null

# Cancel it
response=$(curl -s -X DELETE "$BASE_URL/v1/reservations/$CANCEL_ID")
if echo "$response" | grep -q "CANCELED"; then
    print_success "Reservation canceled successfully"
else
    print_error "Failed to cancel reservation"
fi

# Test 11: Get reservations by guest
print_test "11. Get all reservations for guest ID=1"
response=$(curl -s "$BASE_URL/v1/reservations?guestId=1")
if echo "$response" | grep -q "reservationId"; then
    print_success "Retrieved guest reservations successfully"
else
    print_error "Failed to retrieve guest reservations"
fi

# Test 12: Test insufficient inventory
print_test "12. Test insufficient inventory (book more than available)"
FAIL_START=$(date -d "+30 days" +%Y-%m-%d)
FAIL_END=$(date -d "+33 days" +%Y-%m-%d)
response=$(curl -s -X POST "$BASE_URL/v1/reservations" \
    -H "$CONTENT_TYPE" \
    -d '{
        "reservationId": "RES-FAIL-'$(date +%s)'",
        "hotelId": 1,
        "roomTypeId": 1,
        "guestId": 1,
        "startDate": "'$FAIL_START'",
        "endDate": "'$FAIL_END'",
        "roomCount": 120
    }')
if echo "$response" | grep -q "Insufficient"; then
    print_success "Inventory check working correctly - booking prevented"
else
    print_error "Inventory check failed"
fi

# Test 13: Create a new guest
print_test "13. Create a new guest"
response=$(curl -s -X POST "$BASE_URL/v1/guests" \
    -H "$CONTENT_TYPE" \
    -d '{
        "firstName": "Test",
        "lastName": "User",
        "email": "test.user.'$(date +%s)'@example.com",
        "phone": "+1-555-9999"
    }')
if echo "$response" | grep -q "guestId"; then
    print_success "Guest created successfully"
else
    print_error "Failed to create guest"
fi

# Summary
echo "======================================"
echo "Test Suite Completed"
echo "======================================"
echo ""
echo "To view the H2 database console, visit:"
echo "http://localhost:8080/h2-console"
echo ""
echo "JDBC URL: jdbc:h2:mem:hoteldb"
echo "Username: sa"
echo "Password: (leave empty)"
echo ""
