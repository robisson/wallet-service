#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

BASE_URL="http://localhost:8080/api/v1/wallets"

# Configuration
WALLETS=${1:-5}
DEPOSITS=${2:-10}
TRANSFERS=${3:-5}
WITHDRAWALS=${4:-5}
DELAY=${5:-0.1}

declare -a WALLET_IDS

echo -e "${CYAN}🚀 Load Test: $WALLETS wallets, $DEPOSITS deposits, $TRANSFERS transfers, $WITHDRAWALS withdrawals${NC}"

# Phase 1: Create Wallets
echo -e "\n${YELLOW}Creating $WALLETS wallets...${NC}"
success=0
for i in $(seq 1 $WALLETS); do
    echo -n "."
    user_id="test_$(date +%s)_${i}_${RANDOM}"
    response=$(curl -s -X POST "$BASE_URL" -H "Content-Type: application/json" -d "{\"userId\": \"$user_id\"}")
    
    if [[ $response == *"walletId"* ]]; then
        wallet_id=$(echo "$response" | jq -r '.walletId' 2>/dev/null)
        if [ -n "$wallet_id" ] && [ "$wallet_id" != "null" ]; then
            WALLET_IDS+=("$wallet_id")
            ((success++))
        fi
    fi
    sleep $DELAY
done
echo -e "\n${GREEN}✅ Created $success wallets${NC}"

# Phase 2: Deposits
echo -e "\n${YELLOW}Making $DEPOSITS deposits...${NC}"
success=0
for i in $(seq 1 $DEPOSITS); do
    echo -n "."
    wallet_id=${WALLET_IDS[$((RANDOM % ${#WALLET_IDS[@]}))]}
    amount=$((RANDOM % 500 + 50))
    response=$(curl -s -X POST "$BASE_URL/$wallet_id/deposit" -H "Content-Type: application/json" -d "{\"amount\": $amount}")
    
    if [[ $response == *"walletId"* ]]; then
        ((success++))
    fi
    sleep $DELAY
done
echo -e "\n${GREEN}✅ Made $success deposits${NC}"

# Phase 3: Transfers
echo -e "\n${YELLOW}Making $TRANSFERS transfers...${NC}"
success=0
for i in $(seq 1 $TRANSFERS); do
    echo -n "."
    
    # Ensure we have at least 2 wallets for transfers
    if [ ${#WALLET_IDS[@]} -lt 2 ]; then
        echo -e "\n${YELLOW}⚠️  Need at least 2 wallets for transfers, skipping...${NC}"
        break
    fi
    
    # Select two different wallets
    from_index=$((RANDOM % ${#WALLET_IDS[@]}))
    to_index=$((RANDOM % ${#WALLET_IDS[@]}))
    
    # Ensure different wallets
    while [ $from_index -eq $to_index ]; do
        to_index=$((RANDOM % ${#WALLET_IDS[@]}))
    done
    
    from_wallet=${WALLET_IDS[$from_index]}
    to_wallet=${WALLET_IDS[$to_index]}
    amount=$((RANDOM % 50 + 10))
    
    response=$(curl -s -X POST "$BASE_URL/transfer" -H "Content-Type: application/json" -d "{\"fromWalletId\": \"$from_wallet\", \"toWalletId\": \"$to_wallet\", \"amount\": $amount}")
    
    # Check if transfer was successful (empty response or no error)
    if [ -z "$response" ] || [[ $response != *"error"* ]]; then
        ((success++))
    fi
    
    sleep $DELAY
done
echo -e "\n${GREEN}✅ Made $success transfers${NC}"

# Phase 4: Withdrawals
echo -e "\n${YELLOW}Making $WITHDRAWALS withdrawals...${NC}"
success=0
for i in $(seq 1 $WITHDRAWALS); do
    echo -n "."
    wallet_id=${WALLET_IDS[$((RANDOM % ${#WALLET_IDS[@]}))]}
    amount=$((RANDOM % 30 + 5))
    response=$(curl -s -X POST "$BASE_URL/$wallet_id/withdraw" -H "Content-Type: application/json" -d "{\"amount\": $amount}")
    
    if [[ $response == *"walletId"* ]]; then
        ((success++))
    fi
    sleep $DELAY
done
echo -e "\n${GREEN}✅ Made $success withdrawals${NC}"

echo -e "\n${GREEN}🎉 Load test complete! Check Grafana for metrics.${NC}"