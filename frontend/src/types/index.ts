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
  settlementCollected: boolean;
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
  commissionRate?: number | null;
  expectedSettlementAmount?: number | null;
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
  // CJ 분류코드 관련 필드 (표준운송장 출력용)
  classificationCode: string | null;
  subClassificationCode: string | null;
  addressAlias: string | null;
  deliveryBranchName: string | null;
  deliveryEmployeeNickname: string | null;
  receiptDate: string | null;
  deliveryMessage: string | null;
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

export interface OrderSettlement {
  id: string;
  tenantId: string;
  marketplaceType: MarketplaceType;
  marketplaceOrderId: string;
  marketplaceProductOrderId: string | null;
  orderId: string | null;
  settleType: string | null;
  settleBasisDate: string | null;
  settleExpectDate: string | null;
  settleCompleteDate: string | null;
  payDate: string | null;
  productId: string | null;
  productName: string | null;
  vendorItemId: string | null;
  saleAmount: number | null;
  commissionAmount: number | null;
  deliveryFeeAmount: number | null;
  deliveryFeeCommission: number | null;
  settlementAmount: number | null;
  discountAmount: number | null;
  sellerDiscountAmount: number | null;
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

export interface CourierConfigResponse {
  id: string;
  courierType: CourierType;
  contractCode: string;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
  senderZipcode: string;
  active: boolean;
  hasApiKey: boolean;
  extraConfig: Record<string, string> | null;
  createdAt: string;
}

export interface CourierConfigRequest {
  courierType: CourierType;
  apiKey: string;
  contractCode: string;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
  senderZipcode: string;
  extraConfig: Record<string, string>;
}

export interface BulkBookingResult {
  total: number;
  successCount: number;
  failCount: number;
  results: BookingItemResult[];
}

export interface BookingItemResult {
  orderId: string;
  success: boolean;
  trackingNumber: string | null;
  error: string | null;
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
  // 재고 정보 (품목 조회 시 함께 반환)
  inventoryBalances?: InventoryBalance[];
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

export interface CoupangSellerProduct {
  id: string;
  tenantId: string;
  sellerProductId: number;
  sellerProductName: string | null;
  displayCategoryCode: number | null;
  categoryId: number | null;
  productId: number | null;
  vendorId: string | null;
  saleStartedAt: string | null;
  saleEndedAt: string | null;
  brand: string | null;
  statusName: string | null;
  syncedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface CoupangSellerProductSyncResponse {
  success: boolean;
  totalCount: number;
  insertedCount: number;
  updatedCount: number;
  syncStartedAt: string;
  syncCompletedAt: string;
  durationMs: number;
  errorMessage: string | null;
}

export interface SalesLineTemplate {
  prodCd: string;
  prodDes: string;
  qtySource: string;
  priceSource: string;
  vatCalculation: string;
  negateAmount: boolean;
  skipIfZero: boolean;
  remarks: string;
  extraFields: Record<string, string>;
  // 마켓별 품목코드 (판매수수료, 배송수수료용)
  marketplaceProdCds?: Record<string, { prodCd: string; prodDes: string }>;
}

export interface ErpSalesTemplateRequest {
  marketplaceHeaders: Record<string, Record<string, string>>;
  defaultHeader: Record<string, string>;
  lineProductSale: SalesLineTemplate;
  lineDeliveryFee: SalesLineTemplate;
  lineSalesCommission: SalesLineTemplate;
  lineDeliveryCommission: SalesLineTemplate;
  active: boolean;
}

export interface ErpSalesTemplateResponse {
  id: string;
  erpConfigId: string;
  marketplaceHeaders: Record<string, Record<string, string>>;
  defaultHeader: Record<string, string>;
  lineProductSale: SalesLineTemplate;
  lineDeliveryFee: SalesLineTemplate;
  lineSalesCommission: SalesLineTemplate;
  lineDeliveryCommission: SalesLineTemplate;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ErpSyncResult {
  success: boolean;
  documentId: string;
  message: string;
}

export interface ErpBatchSyncResult {
  totalCount: number;
  successCount: number;
  failCount: number;
}

export interface InventoryBalance {
  whCd: string;
  whDes: string;
  prodCd: string;
  balQty: number;
}

export interface InventoryBalanceResponse {
  success: boolean;
  items: InventoryBalance[];
  itemsByProdCd: Record<string, InventoryBalance[]>;
  errorMessage: string | null;
}

// ERP 판매전표 관련 타입
export type ErpDocumentStatus = 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED';

export const ErpDocumentStatusLabels: Record<ErpDocumentStatus, string> = {
  PENDING: '미전송',
  SENT: '전송완료',
  FAILED: '전송실패',
  CANCELLED: '취소',
};

export interface ErpSalesDocument {
  id: string;
  orderId: string;
  marketplaceOrderId: string | null;
  status: ErpDocumentStatus;
  documentDate: string;
  marketplaceType: MarketplaceType;
  customerCode: string | null;
  customerName: string | null;
  totalAmount: number;
  documentLines: SalesDocumentLine[];
  erpDocumentId: string | null;
  sentAt: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SalesDocumentLine {
  LINE_NO: string;
  IO_DATE: string;
  UPLOAD_SER_NO: string;
  PROD_CD: string;
  PROD_DES: string;
  QTY: string;
  SUPPLY_AMT: string;
  VAT_AMT: string;
  PRICE: string;
  CUST?: string;
  CUST_DES?: string;
  REMARKS?: string;
  [key: string]: string | undefined;
}

export interface ErpDocumentCounts {
  PENDING: number;
  SENT: number;
  FAILED: number;
}

export interface ErpDocumentSendResult {
  documentId: string;
  orderId: string;
  success: boolean;
  erpDocumentId: string | null;
  errorMessage: string | null;
}

export interface ErpBatchSendResponse {
  totalCount: number;
  successCount: number;
  failCount: number;
  results: ErpDocumentSendResult[];
}

export interface ErpPendingOrder {
  id: string;
  marketplaceType: MarketplaceType;
  marketplaceOrderId: string;
  status: OrderStatus;
  receiverName: string;
  totalAmount: number;
  orderedAt: string;
}
