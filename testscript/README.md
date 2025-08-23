# Order PDF Generator Service - Demo & Test Scripts

This is a demonstration of PDF generation based on DynamoDB events through a Step Functions workflow. The service automatically generates PDF invoices when orders are inserted into DynamoDB.

## Architecture Overview

This is a **100% serverless** demonstration using these AWS services:
- **AWS Lambda** - Serverless compute for PDF generation
- **AWS Step Functions** - Workflow orchestration with batch processing loop
- **AWS DynamoDB** - NoSQL database with streams
- **AWS SQS** - Message queuing service  
- **AWS EventBridge Pipes** - Event routing from streams to SQS
- **AWS S3** - Object storage for generated PDFs

### Technology Stack
- **Main Application**: Java 24 with Maven (Lambda functions, PDF generation)
- **Test Scripts**: TypeScript with Yarn (data generation and testing)
- **Infrastructure**: Pulumi with Java

### Workflow Sequence

The sequence diagram above shows how the serverless pipeline processes orders:

1. **Test Script** inserts random order data into DynamoDB
2. **DynamoDB Streams** automatically capture the change events
3. **EventBridge Pipes** route stream records to SQS queue
4. **SQS** triggers the Step Functions state machine
5. **Step Functions** orchestrates batch processing in a loop:
   - For each order item in the batch:
     - **Preprocess Lambda** parses and validates the order data
     - **Generate PDF Lambda** creates the invoice PDF using HTML/CSS templates
     - PDF is stored in **S3 bucket**
6. Step Functions completes the batch and acknowledges SQS

The Step Functions workflow processes multiple orders efficiently through its loop mechanism.

## Development Setup

### Prerequisites
- **Node.js** and **Yarn** package manager
- **AWS CLI** configured with appropriate permissions
- **Java 24** and **Maven** for Lambda development

### Install Dependencies
```bash
# Install test script dependencies
yarn install
```

## Deployment Commands

### Deploy the Infrastructure
```bash
# Navigate to infrastructure directory
cd ../pulumi

# Deploy all AWS resources
pulumi up  

# Confirm deployment when prompted
```

### Destroy the Infrastructure
```bash
# Navigate to infrastructure directory  
cd ../pulumi

# Destroy all AWS resources
pulumi destroy

# Confirm destruction when prompted
```

## Test Script Setup

1. **Install dependencies:**
   ```bash
   yarn install
   ```

2. **Configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your AWS configuration
   ```

3. **Set up AWS credentials** (choose one):
   - Use AWS CLI: `aws configure`
   - Use environment variables in `.env`

## Scripts

### `insert-test-orders.ts`
Generates and inserts completely random orders into DynamoDB to trigger the PDF generation workflow.

**Features:**
- Generates completely random order data using custom data arrays and generation functions
- Random order IDs with various patterns (ORD-123456, ORDER-2025-1234, WEB-ABC12345, etc.)
- Random customer names and email addresses from predefined arrays
- Random product names combining categories, adjectives, and product types
- Random quantities, prices, and item counts with realistic variations
- Custom data generation without external dependencies
- Uses AWS SDK v3 with proper TypeScript types
- Configurable number of orders to generate
- Clean, comment-free code for production use

**Usage:**
```bash
# Run test script directly with ts-node
yarn ts-node insert-test-orders.ts

# Or use the dev script
yarn dev

# Or build and run
yarn build
yarn start

# Generate specific number of orders
ORDER_COUNT=10 yarn ts-node insert-test-orders.ts
```

**Sample output:**
```
🚀 Starting Random DynamoDB Test Data Generation
============================================================
📋 Target table: orders
🌍 AWS Region: af-south-1
🔢 Generating 5 random orders

📝 Generating order 1/5...
   🎲 Generated: ORD-847362 for Emma Johnson
   💰 Total: $156.47 (3 items)
✅ Inserted order ORD-847362 - Emma Johnson ($156.47)
   📦 Added item ITEM-K7M2N9: Premium Electronics Headphones (Qty: 2)
   📦 Added item ITEM-P4R8T1: Smart Tech Mouse Pad (Qty: 1)
   📦 Added item ITEM-B9X5L3: Portable Office Charger (Qty: 1)
```

## Testing Workflow

1. **Deploy infrastructure** (from main project):
   ```bash
   cd ../pulumi
   pulumi up
   ```

2. **Insert random test data:**
   ```bash
   yarn ts-node insert-test-orders.ts
   # Or generate specific number of orders:
   ORDER_COUNT=20 yarn ts-node insert-test-orders.ts
   ```

3. **Monitor AWS services:**
   - **DynamoDB**: Check table for inserted records and stream activity
   - **EventBridge Pipes**: Monitor pipe execution logs
   - **SQS**: Verify queue messages are processed
   - **Step Functions**: View state machine executions
   - **Lambda**: Check function logs for PDF generation
   - **S3**: Verify generated PDFs appear in bucket

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AWS_REGION` | AWS region for services | `us-east-1` |
| `DYNAMODB_TABLE_NAME` | DynamoDB table name | `orders` |
| `AWS_ACCESS_KEY_ID` | AWS access key (optional) | - |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key (optional) | - |

## Test Data Structure

### Main Order Record
```json
{
  "pk": "ORDER#ORD-001",
  "sk": "STATE#v1",
  "orderId": "ORD-001",
  "customerName": "John Doe",
  "customerEmail": "john.doe@email.com",
  "totalAmount": 65.48,
  "status": "PENDING",
  "pdfStatus": "PENDING",
  "createdAt": "2025-08-13T21:30:00Z",
  "updatedAt": "2025-08-13T21:30:00Z"
}
```

### Item Detail Record
```json
{
  "pk": "ORDER#ORD-001", 
  "sk": "ITEM#001",
  "orderId": "ORD-001",
  "itemId": "ITEM-001",
  "productName": "Premium Coffee Mug",
  "quantity": 2,
  "price": 24.99
}
```

## Troubleshooting

### Common Issues

1. **AWS Credentials Error:**
   - Verify AWS credentials are configured
   - Check IAM permissions for DynamoDB access

2. **Table Not Found:**
   - Ensure table name matches Pulumi configuration
   - Verify table exists in the correct region

3. **Permission Denied:**
   - Check IAM permissions for DynamoDB operations
   - Verify region matches your deployed infrastructure

### Required Permissions

Your AWS credentials need these permissions:
- `dynamodb:PutItem`
- `dynamodb:GetItem` 
- `dynamodb:Query`
- `dynamodb:Scan`
- `dynamodb:DeleteItem`

## Integration with Pipeline

These test scripts create data that flows through the complete serverless pipeline:

```
DynamoDB Insert → DynamoDB Streams → EventBridge Pipes → SQS Queue → Step Functions → Preprocess Lambda → Generate PDF Lambda → S3 PDF Storage
```

### Monitoring the Pipeline

Monitor each stage to verify the end-to-end workflow is functioning correctly:

1. **DynamoDB**: Check the orders table for inserted records and verify stream activity
2. **EventBridge Pipes**: Monitor pipe execution logs in CloudWatch
3. **SQS**: Verify queue messages are being processed (should be empty after processing)
4. **Step Functions**: View state machine executions and success/failure rates
5. **Lambda Functions**: Check CloudWatch logs for both preprocess and PDF generation functions
6. **S3**: Verify generated PDF files appear in the output bucket

The entire workflow should complete within seconds for each order insertion.