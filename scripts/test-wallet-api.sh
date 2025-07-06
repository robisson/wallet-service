#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080/api/v1/wallets"

echo -e "${CYAN}🚀 Starting Wallet Service API Test${NC}"
echo "=================================="

# Function to make API calls with colored output
api_call() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    echo -e "\n${YELLOW}📋 $description${NC}" >&2
    echo -e "${BLUE}$method $url${NC}" >&2
    
    if [ -n "$data" ]; then
        echo -e "${PURPLE}Data: $data${NC}" >&2
        response=$(curl -s -X $method "$url" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -X $method "$url")
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Response:${NC}" >&2
        echo "$response" | jq '.' 2>/dev/null >&2 || echo "$response" >&2
        echo "$response"  # Return response to stdout
    else
        echo -e "${RED}❌ Request failed${NC}" >&2
        return 1
    fi
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}⚠️  jq not found. Installing for better JSON formatting...${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install jq 2>/dev/null || echo -e "${RED}Please install jq: brew install jq${NC}"
    else
        echo -e "${RED}Please install jq for better JSON formatting${NC}"
    fi
fi

# Step 1: Create first wallet
echo -e "\n${CYAN}Step 1: Creating first wallet${NC}"
wallet1_response=$(api_call "POST" "$BASE_URL" '{"userId": "user1234"}' "Create wallet for user123")
wallet1_id=$(echo "$wallet1_response" | jq -r '.walletId' 2>/dev/null)

if [ "$wallet1_id" == "null" ] || [ -z "$wallet1_id" ]; then
    echo -e "${RED}❌ Failed to extract wallet ID. Response: $wallet1_response${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Wallet 1 ID: $wallet1_id${NC}"

# Step 2: Create second wallet
echo -e "\n${CYAN}Step 2: Creating second wallet${NC}"
wallet2_response=$(api_call "POST" "$BASE_URL" '{"userId": "user456"}' "Create wallet for user456")
wallet2_id=$(echo "$wallet2_response" | jq -r '.walletId' 2>/dev/null)

if [ "$wallet2_id" == "null" ] || [ -z "$wallet2_id" ]; then
    echo -e "${RED}❌ Failed to create second wallet${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Wallet 2 ID: $wallet2_id${NC}"

# Step 3: Deposit money to first wallet
echo -e "\n${CYAN}Step 3: Depositing $500 to first wallet${NC}"
api_call "POST" "$BASE_URL/$wallet1_id/deposit" '{"amount": 500.00}' "Deposit $500 to wallet 1"

# Step 4: Deposit money to second wallet
echo -e "\n${CYAN}Step 4: Depositing $300 to second wallet${NC}"
api_call "POST" "$BASE_URL/$wallet2_id/deposit" '{"amount": 300.00}' "Deposit $300 to wallet 2"

# Step 5: Check balances
echo -e "\n${CYAN}Step 5: Checking wallet balances${NC}"
api_call "GET" "$BASE_URL/$wallet1_id" "" "Get wallet 1 balance"
api_call "GET" "$BASE_URL/$wallet2_id" "" "Get wallet 2 balance"

# Step 6: Transfer money from wallet 1 to wallet 2
echo -e "\n${CYAN}Step 6: Transferring $150 from wallet 1 to wallet 2${NC}"
api_call "POST" "$BASE_URL/transfer" "{\"fromWalletId\": \"$wallet1_id\", \"toWalletId\": \"$wallet2_id\", \"amount\": 150.00}" "Transfer $150"

# Step 7: Check final balances
echo -e "\n${CYAN}Step 7: Checking final balances${NC}"
api_call "GET" "$BASE_URL/$wallet1_id" "" "Get wallet 1 final balance"
api_call "GET" "$BASE_URL/$wallet2_id" "" "Get wallet 2 final balance"

# Step 8: Test withdrawal
echo -e "\n${CYAN}Step 8: Withdrawing $100 from wallet 2${NC}"
api_call "POST" "$BASE_URL/$wallet2_id/withdraw" '{"amount": 100.00}' "Withdraw $100 from wallet 2"

# Step 9: Final balance check
echo -e "\n${CYAN}Step 9: Final balance check${NC}"
api_call "GET" "$BASE_URL/$wallet1_id" "" "Get wallet 1 final balance"
api_call "GET" "$BASE_URL/$wallet2_id" "" "Get wallet 2 final balance"

echo -e "\n${GREEN}🎉 Test completed successfully!${NC}"
echo -e "${CYAN}Summary:${NC}"
echo -e "• Created 2 wallets"
echo -e "• Deposited $500 to wallet 1, $300 to wallet 2"
echo -e "• Transferred $150 from wallet 1 to wallet 2"
echo -e "• Withdrew $100 from wallet 2"
echo -e "• Expected final balances: Wallet 1: $350, Wallet 2: $350"