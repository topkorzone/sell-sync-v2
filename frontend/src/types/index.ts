export type MarketplaceType =
  | 'NAVER'
  | 'COUPANG'
  | 'ELEVEN_ST'
  | 'GMARKET'
  | 'AUCTION'
  | 'WEMAKEPRICE'
  | 'TMON';

export type OrderStatus =
  | 'COLLECTED'
  | 'CONFIRMED'
  | 'READY_TO_SHIP'
  | 'SHIPPING'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURNED'
  | 'EXCHANGED'
  | 'PURCHASE_CONFIRMED';

export type CourierType = 'CJ' | 'HANJIN' | 'LOGEN' | 'LOTTE' | 'POST';

export type ShipmentStatus =
  | 'PENDING'
  | 'RESERVED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED';

export type SyncStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';

export type UserRole = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'TENANT_USER';

export interface Tenant {
  id: string;
  companyName: string;
  businessNumber: string;
  ownerName: string;
  email: string;
  phone: string;
  active: boolean;
  settings: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface Order {
  id: string;
  tenantId: string;
  marketplaceType: MarketplaceType;
  marketplaceOrderId: string;
  marketplaceProductOrderId: string;
  status: OrderStatus;
  buyerName: string;
  buyerPhone: string;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  receiverZipCode: string;
  totalAmount: number;
  deliveryFee: number;
  orderedAt: string;
  erpSynced: boolean;
  erpDocumentId: string | null;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  id: string;
  orderId: string;
  productName: string;
  optionName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  marketplaceProductId: string;
  marketplaceItemId: string;
}

export interface Shipment {
  id: string;
  orderId: string;
  tenantId: string;
  courierType: CourierType;
  trackingNumber: string;
  status: ShipmentStatus;
  reservedAt: string | null;
  pickedUpAt: string | null;
  deliveredAt: string | null;
  waybillUrl: string | null;
  marketplaceNotified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Settlement {
  id: string;
  tenantId: string;
  marketplaceType: MarketplaceType;
  settlementDate: string;
  orderCount: number;
  totalSales: number;
  totalCommission: number;
  totalDeliveryFee: number;
  netAmount: number;
  erpSynced: boolean;
  erpDocumentId: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  errorCode: string | null;
  message: string | null;
}

export interface DashboardOverview {
  todayOrders: number;
  todayShipments: number;
  pendingOrders: number;
  monthlyRevenue: number;
  recentOrders: Order[];
}

export interface MarketplaceCredential {
  id: string;
  marketplaceType: MarketplaceType;
  sellerId: string;
  active: boolean;
  tokenExpiresAt: string | null;
  createdAt: string;
}

export interface CourierConfig {
  id: string;
  courierType: CourierType;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
  senderZipCode: string;
  active: boolean;
  createdAt: string;
}

export interface ErpConfig {
  id: string;
  erpType: string;
  companyCode: string;
  active: boolean;
  fieldMapping: Record<string, string>;
  createdAt: string;
}
