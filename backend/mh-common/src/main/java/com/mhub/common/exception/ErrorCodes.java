package com.mhub.common.exception;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    // Auth
    public static final String AUTH_INVALID_TOKEN = "AUTH_001";
    public static final String AUTH_EXPIRED_TOKEN = "AUTH_002";
    public static final String AUTH_INSUFFICIENT_PERMISSION = "AUTH_003";
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_004";
    public static final String AUTH_REFRESH_FAILED = "AUTH_005";
    public static final String AUTH_TENANT_MISSING = "AUTH_006";

    // Tenant
    public static final String TENANT_NOT_FOUND = "TENANT_001";
    public static final String TENANT_INACTIVE = "TENANT_002";

    // Order
    public static final String ORDER_NOT_FOUND = "ORDER_001";
    public static final String ORDER_INVALID_STATUS_TRANSITION = "ORDER_002";
    public static final String ORDER_DUPLICATE = "ORDER_003";

    // Marketplace
    public static final String MARKETPLACE_API_ERROR = "MKT_001";
    public static final String MARKETPLACE_AUTH_FAILED = "MKT_002";
    public static final String MARKETPLACE_RATE_LIMITED = "MKT_003";
    public static final String MARKETPLACE_CREDENTIAL_NOT_FOUND = "MKT_004";
    public static final String MARKETPLACE_CREDENTIAL_DUPLICATE = "MKT_005";
    public static final String MARKETPLACE_CONNECTION_FAILED = "MKT_006";

    // Shipping
    public static final String SHIPPING_NO_TRACKING_NUMBER = "SHIP_001";
    public static final String SHIPPING_RESERVATION_FAILED = "SHIP_002";
    public static final String SHIPPING_COURIER_API_ERROR = "SHIP_003";

    // ERP
    public static final String ERP_SYNC_FAILED = "ERP_001";
    public static final String ERP_API_ERROR = "ERP_002";
    public static final String ERP_DOCUMENT_CREATE_FAILED = "ERP_003";
    public static final String ERP_CONFIG_NOT_FOUND = "ERP_004";
    public static final String ERP_CONFIG_DUPLICATE = "ERP_005";
    public static final String ERP_CONNECTION_FAILED = "ERP_006";
    public static final String ERP_ITEM_SYNC_FAILED = "ERP_007";
    public static final String ERP_ITEM_NOT_FOUND = "ERP_008";

    // Product Mapping
    public static final String PRODUCT_MAPPING_NOT_FOUND = "PM_001";
    public static final String PRODUCT_MAPPING_INVALID_REQUEST = "PM_002";

    // Settlement
    public static final String SETTLEMENT_SYNC_FAILED = "SETTLE_001";
    public static final String SETTLEMENT_NO_DATA = "SETTLE_002";

    // General
    public static final String INTERNAL_ERROR = "SYS_001";
    public static final String VALIDATION_ERROR = "SYS_002";
    public static final String RATE_LIMIT_EXCEEDED = "SYS_003";
}
