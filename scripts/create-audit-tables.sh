#!/bin/bash

# Script to create audit tables in DynamoDB Local
# This script assumes DynamoDB Local is running at http://localhost:8000

echo "Creating audit tables in DynamoDB Local..."

# Create audit_logs table
aws dynamodb create-table \
    --endpoint-url http://localhost:8000 \
    --table-name audit_logs \
    --attribute-definitions \
        AttributeName=walletId,AttributeType=S \
        AttributeName=transactionId,AttributeType=S \
    --key-schema \
        AttributeName=walletId,KeyType=HASH \
        AttributeName=transactionId,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-1

# Create wallet_snapshots table
aws dynamodb create-table \
    --endpoint-url http://localhost:8000 \
    --table-name wallet_snapshots \
    --attribute-definitions \
        AttributeName=walletId,AttributeType=S \
        AttributeName=snapshotId,AttributeType=S \
    --key-schema \
        AttributeName=walletId,KeyType=HASH \
        AttributeName=snapshotId,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-1

echo "Audit tables created successfully!"