#!/bin/bash
# Create SQS queues for local development

echo "Creating SQS queues..."

# Dead Letter Queue
awslocal sqs create-queue --queue-name order-sync-dlq --region ap-northeast-2

# Order Sync Queue with DLQ
awslocal sqs create-queue \
  --queue-name order-sync-queue \
  --region ap-northeast-2 \
  --attributes '{
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:ap-northeast-2:000000000000:order-sync-dlq\",\"maxReceiveCount\":\"3\"}",
    "VisibilityTimeout": "300"
  }'

echo "SQS queues created successfully!"
awslocal sqs list-queues --region ap-northeast-2
