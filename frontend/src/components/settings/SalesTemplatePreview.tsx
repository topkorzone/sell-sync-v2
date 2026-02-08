import { useMemo } from "react";
import type { SalesLineTemplate, MarketplaceType } from "@/types";

interface SalesTemplatePreviewProps {
  defaultHeader: Record<string, string>;
  marketplaceHeaders: Record<string, Record<string, string>>;
  lineProductSale: SalesLineTemplate;
  lineDeliveryFee: SalesLineTemplate;
  lineSalesCommission: SalesLineTemplate;
  lineDeliveryCommission: SalesLineTemplate;
  selectedMarketplace?: MarketplaceType;
}

// 샘플 주문 데이터 (현실적인 단일 주문)
const SAMPLE_ORDER = {
  orderDate: new Date().toISOString().split("T")[0].replace(/-/g, "/"),
  productName: "경옥고 울금",
  quantity: 2,
  unitPrice: 24700,
  deliveryFee: 3500,
  commission: 2470,
  deliveryCommission: 350,
};

// 헤더 필드 한글 레이블
const HEADER_LABELS: Record<string, string> = {
  WH_CD: "창고",
  EMP_CD: "담당자",
  IO_TYPE: "거래유형",
  DEPT_CD: "부서",
  PROJ_CD: "프로젝트",
  CUST: "거래처코드",
  CUST_DES: "거래처",
};

// IO_TYPE 값 표시
const IO_TYPE_LABELS: Record<string, string> = {
  "": "부가세율 적용",
  "01": "과세",
  "02": "영세",
  "03": "면세",
};

// 마켓플레이스 레이블
const MARKETPLACE_LABELS: Record<MarketplaceType, string> = {
  COUPANG: "쿠팡",
  NAVER: "스마트스토어",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

export default function SalesTemplatePreview({
  defaultHeader,
  marketplaceHeaders,
  lineProductSale,
  lineDeliveryFee,
  lineSalesCommission,
  lineDeliveryCommission,
  selectedMarketplace = "COUPANG",
}: SalesTemplatePreviewProps) {
  // 마켓별 헤더 병합
  const mergedHeader = useMemo(() => {
    const mktHeader = marketplaceHeaders[selectedMarketplace] ?? {};
    return { ...defaultHeader, ...mktHeader };
  }, [defaultHeader, marketplaceHeaders, selectedMarketplace]);

  // 전표 행 생성
  const lines = useMemo(() => {
    const result: Array<{
      prodCd: string;
      prodDes: string;
      qty: number;
      unitPrice: number;
      supplyPrice: number;
      vat: number;
      total: number;
    }> = [];

    // 1. 상품판매
    if (lineProductSale.prodCd || lineProductSale.prodDes) {
      const qty =
        lineProductSale.qtySource === "ORDER_QUANTITY"
          ? SAMPLE_ORDER.quantity
          : 1;
      const totalPrice = SAMPLE_ORDER.unitPrice * SAMPLE_ORDER.quantity;
      const supplyPrice =
        lineProductSale.vatCalculation === "SUPPLY_DIV_11"
          ? Math.round(totalPrice / 1.1)
          : totalPrice;
      const vat = totalPrice - supplyPrice;
      result.push({
        prodCd: lineProductSale.prodCd === "FROM_MAPPING" ? "01809" : lineProductSale.prodCd,
        prodDes: lineProductSale.prodDes || SAMPLE_ORDER.productName,
        qty,
        unitPrice: Math.round(totalPrice / qty),
        supplyPrice,
        vat,
        total: totalPrice,
      });
    }

    // 2. 배송비
    if (lineDeliveryFee.prodDes && SAMPLE_ORDER.deliveryFee > 0) {
      if (!lineDeliveryFee.skipIfZero || SAMPLE_ORDER.deliveryFee > 0) {
        const supplyPrice =
          lineDeliveryFee.vatCalculation === "SUPPLY_DIV_11"
            ? Math.round(SAMPLE_ORDER.deliveryFee / 1.1)
            : SAMPLE_ORDER.deliveryFee;
        const vat = SAMPLE_ORDER.deliveryFee - supplyPrice;
        result.push({
          prodCd: lineDeliveryFee.prodCd || "",
          prodDes: lineDeliveryFee.prodDes,
          qty: 1,
          unitPrice: SAMPLE_ORDER.deliveryFee,
          supplyPrice,
          vat,
          total: SAMPLE_ORDER.deliveryFee,
        });
      }
    }

    // 3. 판매수수료
    if (lineSalesCommission.prodDes && lineSalesCommission.priceSource === "COMMISSION_AMOUNT") {
      if (!lineSalesCommission.skipIfZero || SAMPLE_ORDER.commission > 0) {
        const amount = lineSalesCommission.negateAmount
          ? -SAMPLE_ORDER.commission
          : SAMPLE_ORDER.commission;
        const supplyPrice =
          lineSalesCommission.vatCalculation === "SUPPLY_DIV_11"
            ? Math.round(amount / 1.1)
            : amount;
        const vat = amount - supplyPrice;
        result.push({
          prodCd: lineSalesCommission.prodCd || "",
          prodDes: lineSalesCommission.prodDes,
          qty: 1,
          unitPrice: amount,
          supplyPrice,
          vat,
          total: amount,
        });
      }
    }

    // 4. 배송수수료
    if (lineDeliveryCommission.prodDes && lineDeliveryCommission.priceSource === "DELIVERY_COMMISSION") {
      if (!lineDeliveryCommission.skipIfZero || SAMPLE_ORDER.deliveryCommission > 0) {
        const amount = lineDeliveryCommission.negateAmount
          ? -SAMPLE_ORDER.deliveryCommission
          : SAMPLE_ORDER.deliveryCommission;
        const supplyPrice =
          lineDeliveryCommission.vatCalculation === "SUPPLY_DIV_11"
            ? Math.round(amount / 1.1)
            : amount;
        const vat = amount - supplyPrice;
        result.push({
          prodCd: lineDeliveryCommission.prodCd || "",
          prodDes: lineDeliveryCommission.prodDes,
          qty: 1,
          unitPrice: amount,
          supplyPrice,
          vat,
          total: amount,
        });
      }
    }

    return result;
  }, [lineProductSale, lineDeliveryFee, lineSalesCommission, lineDeliveryCommission]);

  // 합계 계산
  const totals = useMemo(() => {
    return lines.reduce(
      (acc, line) => ({
        supplyPrice: acc.supplyPrice + line.supplyPrice,
        vat: acc.vat + line.vat,
        total: acc.total + line.total,
      }),
      { supplyPrice: 0, vat: 0, total: 0 }
    );
  }, [lines]);

  const formatNumber = (num: number) => {
    return num.toLocaleString("ko-KR");
  };

  return (
    <div className="rounded-lg border bg-muted/30 h-full flex flex-col overflow-hidden">
      {/* 헤더 */}
      <div className="border-b bg-muted/50 px-3 py-2 shrink-0">
        <div className="text-xs font-medium text-muted-foreground">
          미리보기 (샘플 데이터)
        </div>
      </div>

      {/* 스크롤 가능한 컨텐츠 영역 */}
      <div className="flex-1 overflow-y-auto min-h-0">
        {/* 전표 헤더 정보 */}
        <div className="border-b px-3 py-2 space-y-1">
          <div className="flex items-center gap-2 text-xs">
            <span className="text-muted-foreground min-w-12">일자</span>
            <span className="font-medium">{SAMPLE_ORDER.orderDate}</span>
          </div>
          <div className="flex items-center gap-2 text-xs">
            <span className="text-muted-foreground min-w-12">거래처</span>
            <span className="font-medium">
              {mergedHeader.CUST_DES || MARKETPLACE_LABELS[selectedMarketplace] || "-"}
            </span>
          </div>
          {mergedHeader.EMP_CD && (
            <div className="flex items-center gap-2 text-xs">
              <span className="text-muted-foreground min-w-12">{HEADER_LABELS.EMP_CD}</span>
              <span>{mergedHeader.EMP_CD}</span>
            </div>
          )}
          {mergedHeader.IO_TYPE && (
            <div className="flex items-center gap-2 text-xs">
              <span className="text-muted-foreground min-w-12">{HEADER_LABELS.IO_TYPE}</span>
              <span>{IO_TYPE_LABELS[mergedHeader.IO_TYPE] || mergedHeader.IO_TYPE}</span>
            </div>
          )}
        </div>

        {/* 전표 행 테이블 */}
        <div className="px-3 py-2">
        <table className="w-full text-xs">
          <thead>
            <tr className="border-b text-muted-foreground">
              <th className="pb-2 text-left font-medium">품목코드</th>
              <th className="pb-2 text-left font-medium">품목명</th>
              <th className="pb-2 text-right font-medium">수량</th>
              <th className="pb-2 text-right font-medium">공급가</th>
              <th className="pb-2 text-right font-medium">부가세</th>
              <th className="pb-2 text-right font-medium">합계</th>
            </tr>
          </thead>
          <tbody>
            {lines.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-4 text-center text-muted-foreground">
                  설정된 전표 행이 없습니다
                </td>
              </tr>
            ) : (
              lines.map((line, idx) => (
                <tr key={idx} className="border-b last:border-0">
                  <td className="py-2 text-muted-foreground">
                    {line.prodCd || "-"}
                  </td>
                  <td className="py-2 max-w-24 truncate" title={line.prodDes}>
                    {line.prodDes}
                  </td>
                  <td className="py-2 text-right tabular-nums">
                    {formatNumber(line.qty)}
                  </td>
                  <td className="py-2 text-right tabular-nums">
                    {formatNumber(line.supplyPrice)}
                  </td>
                  <td className="py-2 text-right tabular-nums">
                    {formatNumber(line.vat)}
                  </td>
                  <td className="py-2 text-right tabular-nums font-medium">
                    {formatNumber(line.total)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
          {lines.length > 0 && (
            <tfoot>
              <tr className="border-t-2 font-medium">
                <td colSpan={3} className="py-2">합계</td>
                <td className="py-2 text-right tabular-nums">
                  {formatNumber(totals.supplyPrice)}
                </td>
                <td className="py-2 text-right tabular-nums">
                  {formatNumber(totals.vat)}
                </td>
                <td className="py-2 text-right tabular-nums">
                  {formatNumber(totals.total)}
                </td>
              </tr>
            </tfoot>
          )}
        </table>
        </div>
      </div>
    </div>
  );
}
