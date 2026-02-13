import { forwardRef, useEffect, useRef } from "react";
import JsBarcode from "jsbarcode";
import type { Order, Shipment } from "@/types";

interface CjStandardLabelProps {
  order: Order;
  shipment: Shipment;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
  printCount?: string;
  isReprint?: boolean;
  reprintCode?: string;
  boxQty?: number;
  boxType?: string;
  freight?: number;
  freightType?: string;
}

const BOX_TYPE_LABELS: Record<string, string> = {
  "01": "극소",
  "02": "소",
  "03": "중",
  "04": "대1",
  "05": "이형",
  "06": "취급제한",
  "07": "대2",
};

// 마스킹: 두 번째 글자 마스킹 (홍길동 → 홍*동)
function maskName(name: string): string {
  if (!name) return "";
  const trimmed = name.replace(/\s/g, "");
  if (trimmed.length < 2) return name;
  if (trimmed.length === 2) return trimmed[0] + "*";
  return trimmed[0] + "*" + trimmed.slice(2);
}

// 전화번호 마스킹: 마지막 4자리
function maskPhone(phone: string): string {
  if (!phone) return "";
  const cleaned = phone.replace(/[^0-9]/g, "");
  if (cleaned.length < 4) return phone;
  if (cleaned.length === 11) {
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 7)}-****`;
  } else if (cleaned.length === 10) {
    if (cleaned.startsWith("02")) {
      return `${cleaned.slice(0, 2)}-${cleaned.slice(2, 6)}-****`;
    }
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 6)}-****`;
  }
  return cleaned.slice(0, -4) + "-****";
}

function formatPhone(phone: string): string {
  if (!phone) return "";
  const cleaned = phone.replace(/[^0-9]/g, "");
  if (cleaned.length === 11) {
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 7)}-${cleaned.slice(7)}`;
  } else if (cleaned.length === 10) {
    if (cleaned.startsWith("02")) {
      return `${cleaned.slice(0, 2)}-${cleaned.slice(2, 6)}-${cleaned.slice(6)}`;
    }
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 6)}-${cleaned.slice(6)}`;
  }
  return phone;
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) {
    const now = new Date();
    return `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, "0")}.${String(now.getDate()).padStart(2, "0")}`;
  }
  try {
    const date = new Date(dateStr);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, "0")}.${String(date.getDate()).padStart(2, "0")}`;
  } catch {
    return dateStr;
  }
}

function formatTrackingNumber(trackingNumber: string): string {
  if (!trackingNumber) return "";
  const cleaned = trackingNumber.replace(/[^0-9]/g, "");
  if (cleaned.length === 12) {
    return `${cleaned.slice(0, 4)}-${cleaned.slice(4, 8)}-${cleaned.slice(8)}`;
  }
  return trackingNumber;
}

function summarizeProducts(order: Order): { line: string; qty: number }[] {
  if (!order.items || order.items.length === 0) return [];
  const lines: { line: string; qty: number }[] = [];
  for (let i = 0; i < Math.min(order.items.length, 2); i++) {
    const item = order.items[i];
    let line = item.productName;
    if (item.optionName) line += ` ${item.optionName}`;
    lines.push({ line, qty: item.quantity });
  }
  return lines;
}

/**
 * CJ대한통운 표준운송장 라벨 (123mm x 100mm)
 *
 * 레이아웃 기준 (디자인가이드 참조):
 * - 전체: 123mm(W) x 100mm(H)
 * - 라벨지에 이미 인쇄된 항목: 운송장번호, 받는분, 보내는분, 수량, 운임, 정산 라벨
 *
 * 바코드:
 * - 5번: 분류코드 바코드 (좌측 상단)
 * - 8번: 운송장번호 바코드 (우측 상단 + 하단 우측)
 *
 * 데이터 맵핑:
 * 1: 운송장번호, 2: 접수일자, 3: 출력매수, 4: 재출력코드
 * 5: 분류코드바코드, 6: 분류코드텍스트, 7: 받는분이름+전화
 * 8: 운송장번호바코드(2곳), 9: 받는분주소, 10: 주소약칭
 * 11: 보내는분이름, 12: 보내는분전화, 13: 수량값, 14: 운임값/정산값
 * 15: 보내는분주소, 16: 상품명, 17: 배송메시지, 18: 배달점소-구역
 * 19: 특수문자1(권내배송코드 P0~P50)
 */
const CjStandardLabel = forwardRef<HTMLDivElement, CjStandardLabelProps>(
  (
    {
      order,
      shipment,
      senderName,
      senderPhone,
      senderAddress,
      printCount = "1/1",
      isReprint = false,
      reprintCode = "",
      boxQty = 1,
      boxType = "02",
      freight = 0,
      freightType = "신용",
    },
    ref
  ) => {
    const classificationBarcodeRef = useRef<SVGSVGElement>(null);
    const topRightBarcodeRef = useRef<SVGSVGElement>(null);
    const bottomBarcodeRef = useRef<SVGSVGElement>(null);

    useEffect(() => {
      // 5번: 분류코드 바코드 (좌측 상단) - CODE128
      // 위치: X 8mm, Y 10mm, 폭 30mm, 높이 15mm
      // 주의: CODE128A는 소문자 미지원, CODE128 자동 선택 사용
      if (classificationBarcodeRef.current) {
        try {
          // 분류코드만 바코드에 포함 (subClassificationCode는 텍스트로만 표시)
          const barcodeValue = (shipment as any).classification_code || shipment.classificationCode || "0000";
          JsBarcode(classificationBarcodeRef.current, barcodeValue.toUpperCase(), {
            format: "CODE128A",
            width: 2,
            height: 57, // 15mm ≈ 57px
            displayValue: false,
            margin: 0,
            background: "transparent",
          });
        } catch (e) {
          console.error("Classification barcode error:", e);
        }
      }

      // 8번: 운송장번호 바코드 - 우측 상단 - CODE128C
      // 위치: 우측에서 10mm, Y 24mm, 폭 35mm, 높이 5mm
      if (topRightBarcodeRef.current) {
        try {
          const numericTracking = shipment.trackingNumber?.replace(/[^0-9]/g, "") || "000000000000";
          JsBarcode(topRightBarcodeRef.current, numericTracking, {
            format: "CODE128C",
            width: 1,
            height: 19, // 5mm ≈ 19px
            displayValue: false,
            margin: 0,
            background: "transparent",
          });
          // SVG가 컨테이너에 맞게 늘어나도록 설정
          topRightBarcodeRef.current.setAttribute("preserveAspectRatio", "none");
        } catch (e) {
          console.error("Top right barcode error:", e);
        }
      }

      // 8번: 운송장번호 바코드 - 하단 우측 - CODE128C
      // 위치: X 88mm, Y 85mm, 폭 35mm, 높이 10mm
      if (bottomBarcodeRef.current) {
        try {
          const numericTracking = shipment.trackingNumber?.replace(/[^0-9]/g, "") || "000000000000";
          JsBarcode(bottomBarcodeRef.current, numericTracking, {
            format: "CODE128C",
            width: 1.5,
            height: 38,
            displayValue: false,
            margin: 0,
            background: "transparent",
          });
        } catch (e) {
          console.error("Bottom barcode error:", e);
        }
      }
    }, [shipment]);

    // 6번: 분류코드 텍스트 파싱 - "4W44-4g" 형식
    // snake_case와 camelCase 모두 처리
    const classCode = (shipment as any).classification_code || shipment.classificationCode || "";
    // 19번: 권내배송코드 (P1 등)
    const subCode = (shipment as any).sub_classification_code || shipment.subClassificationCode || "";

    const productLines = summarizeProducts(order);

    return (
      <div
        ref={ref}
        className="cj-standard-label"
        style={{
          width: "123mm",
          height: "100mm",
          fontFamily: "'Malgun Gothic', 'Noto Sans KR', sans-serif",
          boxSizing: "border-box",
          backgroundColor: "#fff",
          border: "1px solid #ccc",
          pageBreakAfter: "always",
          position: "relative",
          overflow: "hidden",
        }}
      >
        {/* ========== 헤더 영역 (Y: 0~10mm) ========== */}

        {/* 1번: 운송장번호 값 (라벨지에 "운송장번호" 텍스트 있음) */}
        <div
          style={{
            position: "absolute",
            top: "1mm",
            left: "18mm",
            fontSize: "12pt",
            fontWeight: "bold",
          }}
        >
          {formatTrackingNumber(shipment.trackingNumber)}
        </div>

        {/* 2번: 접수일자 */}
        <div
          style={{
            position: "absolute",
            top: "1mm",
            left: "55mm",
            fontSize: "8pt",
            fontWeight: "bold",
          }}
        >
          {formatDate(shipment.receiptDate || shipment.reservedAt)}
        </div>

        {/* 3번: 출력매수 */}
        <div
          style={{
            position: "absolute",
            top: "1mm",
            left: "78mm",
            fontSize: "8pt",
          }}
        >
          {printCount}
        </div>

        {/* 4번: 재출력코드 */}
        {isReprint && (
          <div
            style={{
              position: "absolute",
              top: "2mm",
              left: "88mm",
              fontSize: "7pt",
            }}
          >
            재출력:{reprintCode}
          </div>
        )}

        {/* ========== 분류코드 영역 (Y: 10~26mm) ========== */}

        {/* 5번: 분류코드 바코드 (X: 8mm, Y: 10mm, W: 30mm, H: 15mm) */}
        <div
          style={{
            position: "absolute",
            top: "10mm",
            left: "6mm",
            width: "30mm",
            height: "15mm",
            overflow: "hidden",
          }}
        >
          {/* <svg ref={classificationBarcodeRef} style={{ width: "100%", height: "100%" }} /> */}
          <svg ref={classificationBarcodeRef} />
        </div>

        {/* 6번: 분류코드 텍스트 - CLSFCD-SUBCLSFCD 형식 */}
        <div
          style={{
            position: "absolute",
            top: "12mm",
            left: "40mm",
            display: "flex",
            alignItems: "baseline",
            fontWeight: "bold",
            fontFamily: "Arial, sans-serif",
          }}
        >
          <span style={{ fontSize: "36pt", lineHeight: 1 }}>{classCode || "----"}</span>
          {subCode && (
            <span style={{ fontSize: "24pt", lineHeight: 1 }}>-{subCode}</span>
          )}
        </div>

        {/* 8번: 운송장번호 바코드 - 우측 상단 (X: 88mm, Y: 10mm, W: 35mm, H: 16mm) */}
        <div
          style={{
            position: "absolute",
            top: "24mm",
            right: "10mm",
            width: "35mm",
            height: "5mm",
            overflow: "hidden",
          }}
        >
          <svg ref={topRightBarcodeRef} style={{ width: "100%", height: "100%" }} />
        </div>

        {/* ========== 받는분 영역 (Y: 26~51mm) - 라벨지에 "받는분" 세로라벨 있음 ========== */}

        {/* 7번: 받는분 이름 + 전화번호 */}
        <div
          style={{
            position: "absolute",
            top: "26mm",
            left: "8mm",
            fontSize: "10pt",
            fontWeight: "bold",
          }}
        >
          {maskName(order.receiverName)} {maskPhone(order.receiverPhone)}
          {order.buyerPhone && order.buyerPhone !== order.receiverPhone && (
            <> / {maskPhone(order.buyerPhone)}</>
          )}
        </div>

        {/* 9번: 받는분 주소 */}
        <div
          style={{
            position: "absolute",
            top: "30.5mm",
            left: "8mm",
            right: "38mm",
            fontSize: "9pt",
            lineHeight: 1.0,
            overflow: "hidden",
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
          }}
        >
          {order.receiverAddress}
        </div>

        {/* 10번: 주소약칭 (큰 글씨, 크기 24pt) */}
        <div
          style={{
            position: "absolute",
            top: "32mm",
            left: "7mm",
            right: "10mm",
            fontSize: "24pt",
            fontWeight: "bold",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {shipment.addressAlias || ""}
        </div>

        {/* ========== 보내는분 영역 (Y: 51~65mm) - 라벨지에 "보내는분" 세로라벨 있음 ========== */}

        {/* 11번: 보내는분 이름 */}
        <div
          style={{
            position: "absolute",
            top: "43mm",
            left: "8mm",
            fontSize: "7pt",
            fontWeight: "bold",
          }}
        >
          {senderName}
        </div>

        {/* 12번: 보내는분 전화번호 */}
        <div
          style={{
            position: "absolute",
            top: "43mm",
            left: "28mm",
            fontSize: "7pt",
          }}
        >
          {formatPhone(senderPhone)}
        </div>

        {/* 13번: 박스크기 + 수량 (라벨지에 "수량" 박스 있음) */}
        <div
          style={{
            position: "absolute",
            top: "44mm",
            left: "71mm",
            fontSize: "10pt",
            fontWeight: "bold",
          }}
        >
          {BOX_TYPE_LABELS[boxType] || boxType} {boxQty}
        </div>

        {/* 14번: 운임 값 (라벨지에 "운임" 박스 있음) */}
        <div
          style={{
            position: "absolute",
            top: "44mm",
            right: "27mm",
            fontSize: "10pt",
            fontWeight: "bold",
          }}
        >
          {freight}
        </div>

        {/* 14번: 정산 값 (라벨지에 "정산" 박스 있음) */}
        <div
          style={{
            position: "absolute",
            top: "44mm",
            right: "2mm",
            fontSize: "10pt",
            fontWeight: "bold",
          }}
        >
          {freightType}
        </div>

        {/* 15번: 보내는분 주소 */}
        <div
          style={{
            position: "absolute",
            top: "47mm",
            left: "8mm",
            right: "10mm",
            fontSize: "8pt",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {senderAddress}
        </div>

        {/* ========== 상품명 영역 (Y: 63~78mm) ========== */}

        {/* 16번: 상품명 (크기 9pt) */}
        <div
          style={{
            position: "absolute",
            top: "54mm",
            left: "3mm",
            right: "10mm",
            fontSize: "9pt",
            overflow: "hidden",
            maxHeight: "15mm",
          }}
        >
          {productLines.map((item, idx) => (
            <div
              key={idx}
              style={{
                display: "flex",
                justifyContent: "space-between",
                overflow: "hidden",
                lineHeight: 1.5,
              }}
            >
              <span 
                style={{ 
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                  flex: 1,
                }}
              >
                {item.line}
              </span>
              <span style={{ marginLeft: "3mm", flexShrink: 0, fontWeight: "bold" }}>
                {item.qty}
              </span>
            </div>
          ))}
        </div>

        {/* ========== 배송메시지 영역 (Y: 78~85mm) ========== */}

        {/* 17번: 배송메시지 (크기 8pt) */}
        <div
          style={{
            position: "absolute",
            top: "83mm",
            left: "3mm",
            right: "10mm",
            fontSize: "8pt",
            overflow: "hidden",
            maxHeight: "7mm",
            lineHeight: 1.4,
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
          }}
        >
          {shipment.deliveryMessage || ""}
        </div>

        {/* ========== 하단 영역 (Y: 85~100mm) ========== */}

        {/* 18번: 배달점소-구역 (크기 18pt) */}
        <div
          style={{
            position: "absolute",
            bottom: "0mm",
            left: "3mm",
            fontSize: "18pt",
            fontWeight: "bold",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
            maxWidth: "80mm",
          }}
        >
          {shipment.deliveryBranchName && (
            <>
              {shipment.deliveryBranchName}
              {shipment.deliveryEmployeeNickname &&
                shipment.deliveryEmployeeNickname !== "##" && (
                  <> - {shipment.deliveryEmployeeNickname}</>
                )}
            </>
          )}
        </div>

        {/* 8번: 운송장번호 바코드 - 하단 우측 (X: 88mm, Y: 85mm, W: 35mm) */}
        <div
          style={{
            position: "absolute",
            bottom: "0mm",
            right: "6mm",
            width: "35mm",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
          }}
        >
          <svg ref={bottomBarcodeRef} style={{ width: "100%", height: "38px" }} />
          <div
            style={{
              fontSize: "9pt",
              fontWeight: "bold",
              marginTop: "1mm",
              letterSpacing: "0.5px",
            }}
          >
            {shipment.trackingNumber?.replace(/[^0-9]/g, "") || "000000000000"}
          </div>
        </div>
      </div>
    );
  }
);

CjStandardLabel.displayName = "CjStandardLabel";

export default CjStandardLabel;
