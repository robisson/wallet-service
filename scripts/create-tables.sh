#!/bin/bash

# Create DynamoDB tables for local development

ENDPOINT="http://localhost:8000"

echo "Creating wallet service tables..."

# Create wallets table
aws dynamodb create-table \
    --table-name wallets \
    --attribute-definitions \
        AttributeName=walletId,AttributeType=S \
        AttributeName=userId,AttributeType=S \
    --key-schema \
        AttributeName=walletId,KeyType=HASH \
    --global-secondary-indexes \
        'IndexName=UserIdIndex,KeySchema=[{AttributeName=userId,KeyType=HASH}],Projection={ProjectionType=ALL},ProvisionedThroughput={ReadCapacityUnits=5,WriteCapacityUnits=5}' \
    --provisioned-throughput ReadCapacityUnits=5000,WriteCapacityUnits=5000 \
    --endpoint-url $ENDPOINT

# Create transactions table
aws dynamodb create-table \
    --table-name transactions \
    --attribute-definitions \
        AttributeName=walletId,AttributeType=S \
        AttributeName=transactionId,AttributeType=S \
    --key-schema \
        AttributeName=walletId,KeyType=HASH \
        AttributeName=transactionId,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5000,WriteCapacityUnits=5000 \
    --endpoint-url $ENDPOINT

# Create audit_logs table
aws dynamodb create-table \
    --table-name audit_logs \
    --attribute-definitions \
        AttributeName=walletId,AttributeType=S \
        AttributeName=transactionId,AttributeType=S \
    --key-schema \
        AttributeName=walletId,KeyType=HASH \
        AttributeName=transactionId,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5000,WriteCapacityUnits=5000 \
    --endpoint-url $ENDPOINT

# Create wallet_snapshots table
aws dynamodb create-table \
    --table-name wallet_snapshots \
    --attribute-definitions \
        AttributeName=walletId,AttributeType=S \
        AttributeName=snapshotId,AttributeType=S \
    --key-schema \
        AttributeName=walletId,KeyType=HASH \
        AttributeName=snapshotId,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5000,WriteCapacityUnits=5000 \
    --endpoint-url $ENDPOINT

echo "Tables created successfully"