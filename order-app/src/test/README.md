# Test Suite Documentation

## Overview
This test suite has been updated to work with the new order processing system that removes image download functionality and implements rich customer/order data from DynamoDB.

## Test Files

### 1. GeneratePdfHandlerTest.java
**Updated to remove image download dependencies**
- Removed `ImageDownloadServiceInterface` mock
- Updated constructor calls to match new handler signature
- Tests now focus on PDF generation without images
- Added test for rich customer data with full address and order metadata
- All tests verify proper S3 upload and error handling

**Key Test Cases:**
- `shouldGeneratePdfSuccessfully()` - Basic PDF generation
- `shouldHandleOrderDetailsServiceFailure()` - DynamoDB failure handling
- `shouldHandlePdfGenerationFailure()` - PDF generation error handling
- `shouldHandleS3UploadFailure()` - S3 upload error handling
- `shouldGeneratePdfWithRichCustomerData()` - Full order data processing

### 2. OrderDetailsServiceTest.java *(NEW)*
**Comprehensive tests for DynamoDB integration**
- Tests real DynamoDB data mapping to Java DTOs
- Verifies all new fields are properly extracted (phone, address, status, notes, etc.)
- Tests error handling and fallback to sample data
- Tests partial data scenarios

**Key Test Cases:**
- `shouldFetchOrderDetailsFromDynamoDB()` - Full data mapping
- `shouldReturnSampleOrderWhenDynamoDBItemNotFound()` - Missing record handling
- `shouldReturnSampleOrderWhenDynamoDBThrowsException()` - Error handling
- `shouldHandlePartialDataFromDynamoDB()` - Incomplete data scenarios

### 3. PdfDocumentGenerationServiceTest.java *(NEW)*
**Tests PDF generation with various order scenarios**
- Tests PDF generation with different order data configurations
- Verifies PDF output is valid (starts with PDF magic number)
- Tests edge cases like empty order lines, long addresses, etc.

**Key Test Cases:**
- `shouldGeneratePdfWithBasicOrderData()` - Standard order
- `shouldGeneratePdfWithRichOrderData()` - Full metadata order
- `shouldGeneratePdfWithMinimalOrderData()` - Minimal required fields
- `shouldGeneratePdfWithMultipleItems()` - Large orders
- `shouldGeneratePdfWithLongAddress()` - Layout stress test
- `shouldHandleEmptyOrderLines()` - Edge case handling

### 4. PreprocessHandlerTest.java
**No changes required** - This test continues to work as-is since it doesn't interact with the Customer/Order DTOs directly.

## Test Fixtures

### JSON Test Data
- `sample-order.json` - Basic order for testing
- `rich-order.json` - Complex order with all metadata
- `dynamodb-order-record.json` - Raw DynamoDB record format

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=GeneratePdfHandlerTest

# Run with coverage
mvn test jacoco:report
```

## Test Data Summary

The tests now work with the complete order data structure:

**Customer Data:**
- Name, email, phone, address

**Order Metadata:**
- Status, priority, source, region, notes
- Processing time, order date, total amount

**Order Lines:**
- SKU, quantity, price (in minor units)

## Key Changes Made

1. **Removed Image Dependencies** - All image download mocks and tests removed
2. **Enhanced DTOs** - Tests work with new Customer and Order field structures  
3. **DynamoDB Integration** - New tests verify real data mapping from DynamoDB
4. **PDF Generation** - Tests verify PDF creation with rich data
5. **Error Handling** - Comprehensive error scenario testing
6. **Fixture Data** - JSON test fixtures for consistent test data

## Integration with Test Script

The test suite is designed to work seamlessly with the `TestScripts/insert-test-orders.ts` script:
- Tests expect the same field structure as the script inserts
- DynamoDB mapping tests use the exact field names from the script
- Sample data in tests mirrors the random data generation patterns
