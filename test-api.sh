#!/bin/bash

echo "=== Event-Driven Order Management API Testing ==="
echo

# Base URL
BASE_URL="http://localhost:8080/api/orders"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to make API calls and display results
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    echo -e "${YELLOW}$description${NC}"
    echo "Request: $method $url"
    
    if [ -n "$data" ]; then
        echo "Data: $data"
        response=$(curl -s -X $method "$url" -H "Content-Type: application/json" -d "$data")
    else
        response=$(curl -s -X $method "$url")
    fi
    
    echo -e "${GREEN}Response:${NC}"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
    echo "---"
    echo
    
    # Extract order number from response for later use
    if [[ "$response" == *"orderNumber"* ]]; then
        ORDER_NUMBER=$(echo "$response" | jq -r '.orderNumber' 2>/dev/null)
    fi
}

# Test 1: Create a new order
echo -e "${YELLOW}Step 1: Creating a new order${NC}"
make_request "POST" "$BASE_URL" '{
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "productName": "Laptop",
    "quantity": 2,
    "unitPrice": 999.99
}' "Creating order for John Doe"

# Store the order number for subsequent tests
if [ -n "$ORDER_NUMBER" ]; then
    echo "Order created with number: $ORDER_NUMBER"
    echo
    
    # Test 2: Get the created order
    echo -e "${YELLOW}Step 2: Retrieving the created order${NC}"
    make_request "GET" "$BASE_URL/$ORDER_NUMBER" "" "Getting order $ORDER_NUMBER"
    
    # Test 3: Confirm the order
    echo -e "${YELLOW}Step 3: Confirming the order${NC}"
    make_request "PUT" "$BASE_URL/$ORDER_NUMBER/confirm" "" "Confirming order $ORDER_NUMBER"
    
    # Test 4: Ship the order
    echo -e "${YELLOW}Step 4: Shipping the order${NC}"
    make_request "PUT" "$BASE_URL/$ORDER_NUMBER/ship" "" "Shipping order $ORDER_NUMBER"
    
    # Test 5: Deliver the order
    echo -e "${YELLOW}Step 5: Delivering the order${NC}"
    make_request "PUT" "$BASE_URL/$ORDER_NUMBER/deliver" "" "Delivering order $ORDER_NUMBER"
    
    # Test 6: Get order by customer email
    echo -e "${YELLOW}Step 6: Getting orders by customer email${NC}"
    make_request "GET" "$BASE_URL/customer/john.doe@example.com" "" "Getting orders for john.doe@example.com"
    
else
    echo -e "${RED}Failed to create order or extract order number${NC}"
fi

# Test 7: Create another order for cancellation test
echo -e "${YELLOW}Step 7: Creating another order to test cancellation${NC}"
make_request "POST" "$BASE_URL" '{
    "customerName": "Jane Smith",
    "customerEmail": "jane.smith@example.com",
    "productName": "Smartphone",
    "quantity": 1,
    "unitPrice": 599.99
}' "Creating order for Jane Smith"

# Store the second order number
if [ -n "$ORDER_NUMBER" ]; then
    SECOND_ORDER_NUMBER=$ORDER_NUMBER
    echo "Second order created with number: $SECOND_ORDER_NUMBER"
    echo
    
    # Test 8: Cancel the second order
    echo -e "${YELLOW}Step 8: Cancelling the second order${NC}"
    make_request "PUT" "$BASE_URL/$SECOND_ORDER_NUMBER/cancel" "" "Cancelling order $SECOND_ORDER_NUMBER"
fi

# Test 9: Get all orders
echo -e "${YELLOW}Step 9: Getting all orders${NC}"
make_request "GET" "$BASE_URL" "" "Getting all orders"

echo -e "${GREEN}=== API Testing Complete ===${NC}"
echo
echo "You can now check:"
echo "- Kafka UI at http://localhost:8080 to see the events"
echo "- pgAdmin at http://localhost:8081 to see the database records"
echo "  (Email: admin@example.com, Password: admin)"
echo "- Application logs to see the event processing"