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
  marketplaceStatus?: string;
  buyerName: string;
  buyerPhone: string;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  receiverZipCode: string;
  totalAmount: number;
  deliveryFee: number;
  expectedSettlementAmount: number | null;
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
  erpItemId?: string | null;
  erpProdCd?: string | null;
  hasMasterMapping?: boolean;
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

export interface MarketplaceCredentialResponse {
  id: string;
  marketplaceType: MarketplaceType;
  sellerId: string;
  active: boolean;
  hasClientId: boolean;
  hasClientSecret: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MarketplaceCredentialRequest {
  marketplaceType: MarketplaceType;
  sellerId: string;
  clientId: string;
  clientSecret: string;
}

export interface ConnectionTestResponse {
  connected: boolean;
  message: string;
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

export type ErpType = 'ICOUNT' | 'ECOUNT';

export interface ErpConfig {
  id: string;
  erpType: ErpType;
  companyCode: string;
  active: boolean;
  fieldMapping: Record<string, string>;
  createdAt: string;
}

export interface ErpConfigResponse {
  id: string;
  erpType: ErpType;
  companyCode: string;
  userId: string;
  active: boolean;
  hasApiKey: boolean;
  fieldMapping: Record<string, unknown> | null;
  extraConfig: Record<string, unknown> | null;
  createdAt: string;
  updatedAt: string;
}

export interface ErpConfigRequest {
  erpType: ErpType;
  companyCode: string;
  userId: string;
  apiKey: string;
  fieldMapping?: Record<string, unknown>;
  extraConfig?: Record<string, unknown>;
}

export interface ErpConnectionTestRequest {
  erpType: ErpType;
  companyCode: string;
  userId: string;
  apiKey: string;
}

export interface ErpItem {
  id: string;
  prodCd: string;
  prodDes: string;
  sizeDes: string | null;
  unit: string | null;
  prodType: string | null;
  inPrice: number | null;
  outPrice: number | null;
  barCode: string | null;
  classCd: string | null;
  classCd2: string | null;
  classCd3: string | null;
  setFlag: boolean;
  balFlag: boolean;
  lastSyncedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface ErpItemSyncResponse {
  success: boolean;
  totalCount: number;
  syncedCount: number;
  failedCount: number;
  message: string;
  syncedAt: string;
}

export interface ErpItemSyncStatusResponse {
  totalItems: number;
  lastSyncedAt: string | null;
  hasSyncedBefore: boolean;
}

export interface ProductMapping {
  id: string;
  tenantId: string;
  marketplaceType: MarketplaceType;
  marketplaceProductId: string;
  marketplaceSku: string | null;
  marketplaceProductName: string | null;
  marketplaceOptionName: string | null;
  erpItemId: string | null;
  erpProdCd: string;
  autoCreated: boolean;
  useCount: number;
  lastUsedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductMappingRequest {
  marketplaceType: MarketplaceType;
  marketplaceProductId: string;
  marketplaceSku?: string;
  marketplaceProductName?: string;
  marketplaceOptionName?: string;
  erpItemId?: string;
  erpProdCd?: string;
}

export interface UnmappedProduct {
  marketplaceType: MarketplaceType;
  marketplaceProductId: string;
  marketplaceSku: string | null;
  productName: string | null;
  optionName: string | null;
  orderCount: number;
}

export interface OrderItemRow {
  rowKey: string;              // `${order.id}-${item.id}`
  orderId: string;
  isFirstItemOfOrder: boolean;
  isLastItemOfOrder: boolean;
  itemsInOrder: number;
  itemIndex: number;
  orderIndex: number;          // 주문 순서 (그룹 배경색용)
  order: Order;
  item: OrderItem;
}
