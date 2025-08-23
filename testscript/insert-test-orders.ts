#!/usr/bin/env node
import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, PutCommand } from '@aws-sdk/lib-dynamodb';
import * as dotenv from 'dotenv';

dotenv.config();

const FIRST_NAMES = [
  'John', 'Jane', 'Michael', 'Sarah', 'David', 'Emma', 'Robert', 'Lisa',
  'James', 'Jennifer', 'William', 'Jessica', 'Richard', 'Ashley', 'Charles',
  'Amanda', 'Thomas', 'Melissa', 'Christopher', 'Deborah', 'Daniel', 'Rachel',
  'Matthew', 'Carolyn', 'Anthony', 'Janet', 'Mark', 'Catherine', 'Donald', 'Maria'
];

const LAST_NAMES = [
  'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis',
  'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson',
  'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee', 'Perez', 'Thompson',
  'White', 'Harris', 'Sanchez', 'Clark', 'Ramirez', 'Lewis', 'Robinson'
];

const PRODUCT_CATEGORIES = [
  'Electronics', 'Home & Garden', 'Clothing', 'Books', 'Sports', 
  'Beauty', 'Automotive', 'Office', 'Kitchen', 'Tech'
];

const PRODUCT_ADJECTIVES = [
  'Premium', 'Deluxe', 'Professional', 'Eco-Friendly', 'Smart', 
  'Wireless', 'Portable', 'Durable', 'Luxury', 'Compact'
];

const PRODUCT_NAMES = [
  'Headphones', 'Notebook', 'Coffee Mug', 'Phone Case', 'Backpack',
  'Desk Lamp', 'Water Bottle', 'Keyboard', 'Mouse Pad', 'Charger',
  'Bluetooth Speaker', 'Tablet Stand', 'Pen Set', 'Calendar',
  'Power Bank', 'Cable Organizer', 'Monitor Stand', 'Webcam',
  'USB Hub', 'Desk Organizer', 'Plant Pot', 'Picture Frame',
  'Travel Mug', 'Notebook Holder', 'Smartphone Stand', 'Laptop Sleeve'
];

const EMAIL_DOMAINS = [
  'gmail.com', 'yahoo.com', 'hotmail.com', 'outlook.com', 'email.com',
  'company.com', 'business.net', 'mail.org', 'inbox.com', 'fastmail.com',
  'protonmail.com', 'icloud.com', 'live.com', 'msn.com', 'aol.com',
  'zoho.com', 'yandex.com', 'tutanota.com', 'mailfence.com', 'hey.com'
];

const ORDER_STATUSES = [
  'PENDING', 'PROCESSING', 'CONFIRMED', 'SHIPPED', 'DELIVERED'
];

const PDF_STATUSES = [
  'PENDING', 'GENERATING', 'COMPLETED', 'FAILED'
];

const STREET_NAMES = [
  'Main Street', 'Oak Avenue', 'Park Road', 'First Street', 'Second Avenue',
  'Elm Street', 'Washington Avenue', 'Maple Street', 'Cedar Road', 'Pine Street',
  'Lincoln Avenue', 'Broadway', 'Church Street', 'High Street', 'School Road'
];

const CITIES = [
  'Springfield', 'Franklin', 'Georgetown', 'Madison', 'Riverside',
  'Oakland', 'Fairview', 'Greenville', 'Salem', 'Bristol',
  'Clinton', 'Manchester', 'Ashland', 'Burlington', 'Dover'
];

interface OrderItem {
  itemId: string;
  productName: string;
  quantity: number;
  price: number;
}

interface Order {
  orderId: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  shippingAddress?: string;
  items: OrderItem[];
  totalAmount: number;
  orderDate?: string;
  notes?: string;
}

/**
 * Initialize DynamoDB Document Client with AWS SDK v3
 */
function getDynamoDBClient(): DynamoDBDocumentClient {
  const client = new DynamoDBClient({
    region: process.env.AWS_REGION || 'af-south-1',

  });
  
  return DynamoDBDocumentClient.from(client);
}

/**
 * Utility function to get random element from array
 */
function getRandomElement<T>(array: T[]): T {
  return array[Math.floor(Math.random() * array.length)];
}

/**
 * Generate random number between min and max (inclusive)
 */
function getRandomNumber(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * Generate random alphanumeric string of specified length
 */
function generateRandomAlphaNumeric(length: number): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

/**
 * Generate random phone number
 */
function generateRandomPhoneNumber(): string {
  const areaCodes = ['555', '444', '333', '222', '111', '666', '777', '888', '999'];
  const areaCode = getRandomElement(areaCodes);
  const exchange = getRandomNumber(100, 999);
  const number = getRandomNumber(1000, 9999);
  return `+1-${areaCode}-${exchange}-${number}`;
}

/**
 * Generate random shipping address
 */
function generateRandomAddress(): string {
  const streetNumber = getRandomNumber(100, 9999);
  const streetName = getRandomElement(STREET_NAMES);
  const city = getRandomElement(CITIES);
  const states = ['CA', 'NY', 'TX', 'FL', 'IL', 'PA', 'OH', 'GA', 'NC', 'MI'];
  const state = getRandomElement(states);
  const zipCode = getRandomNumber(10000, 99999);
  
  return `${streetNumber} ${streetName}, ${city}, ${state} ${zipCode}`;
}

/**
 * Generate random order notes
 */
function generateRandomOrderNotes(): string {
  const notes = [
    'Please handle with care',
    'Gift wrapping requested',
    'Delivery instructions: Ring doorbell',
    'Leave at front door if not home',
    'Contact customer before delivery',
    'Fragile items - handle carefully',
    'Rush delivery requested',
    'Customer prefers morning delivery',
    'Special packaging requested',
    'Contact via phone for delivery'
  ];
  
    return Math.random() < 0.7 ? getRandomElement(notes) : '';
}

/**
 * Generate random past date within last 30 days
 */
function generateRandomOrderDate(): string {
  const now = new Date();
  const daysAgo = getRandomNumber(0, 30);
  const hoursAgo = getRandomNumber(0, 23);
  const minutesAgo = getRandomNumber(0, 59);
  
  const orderDate = new Date(now.getTime() - (daysAgo * 24 * 60 * 60 * 1000) - (hoursAgo * 60 * 60 * 1000) - (minutesAgo * 60 * 1000));
  return orderDate.toISOString();
}

/**
 * Generate random product names
 */
function generateRandomProductName(): string {
  const category = getRandomElement(PRODUCT_CATEGORIES);
  const adjective = getRandomElement(PRODUCT_ADJECTIVES);
  const product = getRandomElement(PRODUCT_NAMES);
  
  return `${adjective} ${category} ${product}`;
}


/**
 * Generate a unique order ID to ensure INSERT events (not MODIFY)
 */
function generateRandomOrderId(): string {
  const now = new Date();
  const timestamp = now.getTime(); // Full timestamp for uniqueness
  const milliseconds = now.getMilliseconds().toString().padStart(3, '0');
  const randomSuffix = generateRandomAlphaNumeric(4);
  
    const patterns = [
    () => `ORD-${timestamp}-${randomSuffix}`,
    () => `ORDER-${now.getFullYear()}${(now.getMonth() + 1).toString().padStart(2, '0')}${now.getDate().toString().padStart(2, '0')}-${now.getHours().toString().padStart(2, '0')}${now.getMinutes().toString().padStart(2, '0')}${now.getSeconds().toString().padStart(2, '0')}${milliseconds}-${randomSuffix}`,
    () => `${getRandomElement(['WEB', 'APP', 'STORE'])}-${timestamp}-${generateRandomAlphaNumeric(3)}`,
    () => `TEST-${timestamp.toString().slice(-8)}-${randomSuffix}`,
    () => `${generateRandomAlphaNumeric(2)}-${timestamp}-${generateRandomAlphaNumeric(2)}`,
  ];
  
  const pattern = getRandomElement(patterns);
  return pattern();
}

/**
 * Generate random order items
 */
function generateRandomOrderItems(): OrderItem[] {
  const itemCount = getRandomNumber(1, 8);
  const items: OrderItem[] = [];
  const usedProductNames = new Set<string>();
  
  for (let i = 0; i < itemCount; i++) {
    let productName = generateRandomProductName();
    
    let attempts = 0;
    while (usedProductNames.has(productName) && attempts < 10) {
      productName = generateRandomProductName();
      attempts++;
    }
    usedProductNames.add(productName);
    
    const basePrice = getRandomNumber(299, 49999);
    const priceVariations = [
      basePrice,
      Math.round(basePrice * 0.85),
      Math.round(basePrice * 1.15),
      Math.round(basePrice * 0.95),
      basePrice + getRandomNumber(-50, 200)
    ];
    
    const item: OrderItem = {
      itemId: `${getRandomElement(['ITEM', 'SKU', 'PROD'])}-${generateRandomAlphaNumeric(getRandomNumber(6, 10))}`,
      productName: productName,
      quantity: getRandomNumber(1, 5),
      price: parseFloat((getRandomElement(priceVariations) / 100).toFixed(2)),
    };
    items.push(item);
  }
  
  return items;
}

/**
 * Generate a complete random order
 */
function generateRandomOrder(): Order {
  const items = generateRandomOrderItems();
  const totalAmount = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  
  const firstName = getRandomElement(FIRST_NAMES);
  const lastName = getRandomElement(LAST_NAMES);
  const domain = getRandomElement(EMAIL_DOMAINS);
  
    const emailVariations = [
    `${firstName.toLowerCase()}.${lastName.toLowerCase()}@${domain}`,
    `${firstName.toLowerCase()}${lastName.toLowerCase()}@${domain}`,
    `${firstName.toLowerCase()}_${lastName.toLowerCase()}@${domain}`,
    `${firstName.toLowerCase()}${getRandomNumber(1, 999)}@${domain}`,
    `${firstName.toLowerCase().slice(0, 3)}${lastName.toLowerCase().slice(0, 3)}${getRandomNumber(10, 99)}@${domain}`
  ];
  
  const order: Order = {
    orderId: generateRandomOrderId(),
    customerName: `${firstName} ${lastName}`,
    customerEmail: getRandomElement(emailVariations),
    customerPhone: Math.random() < 0.8 ? generateRandomPhoneNumber() : undefined,
    shippingAddress: Math.random() < 0.9 ? generateRandomAddress() : undefined,
    items,
    totalAmount: parseFloat(totalAmount.toFixed(2)),
    orderDate: generateRandomOrderDate(),
    notes: Math.random() < 0.4 ? generateRandomOrderNotes() : undefined,
  };
  
  return order;
}

/**
 * Insert a single order into DynamoDB
 */
async function insertOrderToDynamoDB(
  client: DynamoDBDocumentClient,
  tableName: string,
  order: Order
): Promise<boolean> {
  try {
    const timestamp = new Date().toISOString();
    const orderId = order.orderId;
    
    const mainItem = {
      pk: `ORDER#${orderId}`,
      sk: 'STATE#v1',
      orderId: orderId,
      customerName: order.customerName,
      customerEmail: order.customerEmail,
      ...(order.customerPhone && { customerPhone: order.customerPhone }),
      ...(order.shippingAddress && { shippingAddress: order.shippingAddress }),
      totalAmount: order.totalAmount,
      status: getRandomElement(ORDER_STATUSES),
      pdfStatus: getRandomElement(PDF_STATUSES),
      ...(order.orderDate && { orderDate: order.orderDate }),
      ...(order.notes && { notes: order.notes }),
      createdAt: timestamp,
      updatedAt: timestamp,
      items: order.items,
      processingTime: getRandomNumber(100, 5000),
      source: getRandomElement(['website', 'mobile_app', 'phone', 'store', 'api']),
      priority: getRandomElement(['low', 'normal', 'high', 'urgent']),
      region: getRandomElement(['us-east', 'us-west', 'eu-central', 'ap-southeast']),
    };
    
    await client.send(new PutCommand({
      TableName: tableName,
      Item: mainItem,
    }));
    
    console.log(`✅ Inserted order ${orderId} - ${order.customerName} ($${order.totalAmount})`);
    
    const additionalStates = [
      {
        sk: 'STATE#v2',
        version: 'v2',
        description: 'Order state version 2 - updated schema'
      },
      {
        sk: 'PAYMENT#v1', 
        paymentStatus: 'COMPLETED',
        paymentMethod: getRandomElement(['credit_card', 'debit_card', 'paypal', 'bank_transfer']),
        transactionId: `TXN-${generateRandomAlphaNumeric(10)}`
      },
      {
        sk: 'SHIPPING#v1',
        shippingStatus: getRandomElement(['PENDING', 'DISPATCHED', 'IN_TRANSIT', 'DELIVERED']),
        trackingNumber: `TRK-${generateRandomAlphaNumeric(12)}`,
        carrier: getRandomElement(['FedEx', 'UPS', 'DHL', 'USPS'])
      },
      {
        sk: 'AUDIT#v1',
        action: 'ORDER_CREATED',
        userId: `USER-${generateRandomAlphaNumeric(8)}`,
        ipAddress: `${getRandomNumber(1, 255)}.${getRandomNumber(1, 255)}.${getRandomNumber(1, 255)}.${getRandomNumber(1, 255)}`
      }
    ];

    const statesToInsert = additionalStates.slice(0, getRandomNumber(2, 4));
    
    for (const stateData of statesToInsert) {
      const stateRecord = {
        pk: `ORDER#${orderId}`,
        orderId: orderId,
        createdAt: timestamp,
        ...stateData
      };
      
      await client.send(new PutCommand({
        TableName: tableName,
        Item: stateRecord,
      }));
      
      console.log(`   📋 Added state record: ${stateData.sk}`);
    }
    
    for (let idx = 0; idx < order.items.length; idx++) {
      const item = order.items[idx];
      const itemRecord = {
        pk: `ORDER#${orderId}`,
        sk: `ITEM#${(idx + 1).toString().padStart(3, '0')}`,
        orderId: orderId,
        itemId: item.itemId,
        productName: item.productName,
        quantity: item.quantity,
        price: item.price,
        createdAt: timestamp,
      };
      
      await client.send(new PutCommand({
        TableName: tableName,
        Item: itemRecord,
      }));
      
      console.log(`   📦 Added item ${item.itemId}: ${item.productName} (Qty: ${item.quantity})`);
    }
    
    return true;
  } catch (error) {
    console.error(`❌ Failed to insert order ${order.orderId}:`, error);
    return false;
  }
}

/**
 * Main function to generate and insert random test orders
 */
async function main(): Promise<void> {
  console.log('🚀 Starting Random DynamoDB Test Data Generation');
  console.log('=' .repeat(60));
  
    const tableName = process.env.DYNAMODB_TABLE_NAME || 'orders-table';
  const orderCount = parseInt(process.env.ORDER_COUNT || '100');
  
  console.log(`📋 Target table: ${tableName}`);
  console.log(`🌍 AWS Region: ${process.env.AWS_REGION || 'af-south-1'}`);
  console.log(`🔢 Generating ${orderCount} random orders`);
  
  try {
 
    const client = getDynamoDBClient();
    console.log('✅ Connected to DynamoDB');
    
    let successfulInserts = 0;
    
    for (let i = 1; i <= orderCount; i++) {
      console.log(`\n📝 Generating order ${i}/${orderCount}...`);
      
      const randomOrder = generateRandomOrder();
      console.log(`   🎲 Generated: ${randomOrder.orderId} for ${randomOrder.customerName}`);
      console.log(`   💰 Total: $${randomOrder.totalAmount} (${randomOrder.items.length} items)`);
      
      if (await insertOrderToDynamoDB(client, tableName, randomOrder)) {
        successfulInserts++;
      }
      
      if (i < orderCount) {
        const delay = getRandomNumber(50, 300);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
    
    console.log('\n' + '='.repeat(60));
    console.log(`✅ Successfully inserted ${successfulInserts}/${orderCount} random orders`);
    console.log('🎉 Random test data generation completed!');
    
    if (successfulInserts > 0) {
      console.log('\n💡 Next steps:');
      console.log('   1. Check DynamoDB table for inserted records');
      console.log('   2. Verify DynamoDB streams are enabled and flowing');
      console.log('   3. Monitor EventBridge Pipes for stream processing');
      console.log('   4. Check Step Functions executions for PDF generation');
      console.log('   5. Verify generated PDFs appear in S3 bucket');
      console.log('\n📊 Pipeline flow:');
      console.log('   DynamoDB → Stream → EventBridge Pipes → SQS → Step Functions → Lambda → S3');
    }
    
  } catch (error) {
    console.error('❌ Script failed:', error);
    console.log('💡 Check your AWS credentials and region configuration');
    process.exit(1);
  }
}

if (require.main === module) {
  main().catch(console.error);
}