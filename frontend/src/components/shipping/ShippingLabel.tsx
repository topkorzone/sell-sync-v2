import { forwardRef } from "react";
import type { Order, CourierType } from "@/types";

interface ShippingLabelProps {
  order: Order;
  trackingNumber: string;
  courierType: CourierType;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
}

const COURIER_LABELS: Record<string, string> = {
  CJ: "CJ대한통운",
  HANJIN: "한진택배",
  LOGEN: "로젠택배",
  LOTTE: "롯데택배",
  POST: "우체국택배",
};

const ShippingLabel = forwardRef<HTMLDivElement, ShippingLabelProps>(
  ({ order, trackingNumber, courierType, senderName, senderPhone, senderAddress }, ref) => {
    return (
      <div
        ref={ref}
        className="shipping-label"
        style={{
          width: "100mm",
          height: "70mm",
          padding: "4mm",
          border: "1px solid #000",
          fontFamily: "Arial, sans-serif",
          fontSize: "11px",
          boxSizing: "border-box",
          backgroundColor: "#fff",
          pageBreakAfter: "always",
          display: "flex",
          flexDirection: "column",
        }}
      >
        {/* 헤더: 택배사 정보 */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            borderBottom: "2px solid #000",
            paddingBottom: "2mm",
            marginBottom: "2mm",
          }}
        >
          <div style={{ fontWeight: "bold", fontSize: "14px" }}>
            {COURIER_LABELS[courierType] || courierType}
          </div>
          <div style={{ fontSize: "10px", color: "#666" }}>
            {order.marketplaceOrderId}
          </div>
        </div>

        {/* 송장번호 */}
        <div
          style={{
            textAlign: "center",
            padding: "2mm 0",
            borderBottom: "1px solid #ccc",
            marginBottom: "2mm",
          }}
        >
          <div style={{ fontSize: "10px", color: "#666" }}>송장번호</div>
          <div style={{ fontWeight: "bold", fontSize: "16px", letterSpacing: "1px" }}>
            {trackingNumber}
          </div>
        </div>

        {/* 받는 분 정보 */}
        <div
          style={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            gap: "1mm",
          }}
        >
          <div style={{ display: "flex", gap: "2mm" }}>
            <span
              style={{
                backgroundColor: "#000",
                color: "#fff",
                padding: "0.5mm 2mm",
                fontSize: "9px",
                fontWeight: "bold",
                whiteSpace: "nowrap",
              }}
            >
              받는분
            </span>
            <span style={{ fontWeight: "bold", fontSize: "13px" }}>
              {order.receiverName}
            </span>
            <span style={{ fontSize: "11px", color: "#333" }}>
              {order.receiverPhone}
            </span>
          </div>
          <div style={{ fontSize: "11px", lineHeight: "1.4", paddingLeft: "2mm" }}>
            [{order.receiverZipCode}] {order.receiverAddress}
          </div>
        </div>

        {/* 상품정보 */}
        <div
          style={{
            borderTop: "1px dashed #ccc",
            borderBottom: "1px dashed #ccc",
            padding: "2mm 0",
            margin: "1mm 0",
            fontSize: "10px",
            color: "#333",
            overflow: "hidden",
            maxHeight: "12mm",
          }}
        >
          <div style={{ fontWeight: "bold", marginBottom: "1mm" }}>상품:</div>
          {order.items.slice(0, 2).map((item, idx) => (
            <div
              key={idx}
              style={{
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {item.productName}
              {item.optionName && ` (${item.optionName})`}
              {item.quantity > 1 && ` x${item.quantity}`}
            </div>
          ))}
          {order.items.length > 2 && (
            <div style={{ color: "#666" }}>외 {order.items.length - 2}건</div>
          )}
        </div>

        {/* 보내는 분 정보 */}
        <div style={{ fontSize: "9px", color: "#666" }}>
          <div style={{ display: "flex", gap: "2mm" }}>
            <span
              style={{
                backgroundColor: "#666",
                color: "#fff",
                padding: "0.5mm 2mm",
                fontSize: "8px",
                whiteSpace: "nowrap",
              }}
            >
              보내는분
            </span>
            <span>{senderName}</span>
            <span>{senderPhone}</span>
          </div>
          <div
            style={{
              paddingLeft: "2mm",
              marginTop: "1mm",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {senderAddress}
          </div>
        </div>
      </div>
    );
  }
);

ShippingLabel.displayName = "ShippingLabel";

export default ShippingLabel;
