import { useRef, useMemo } from "react";
import { useReactToPrint } from "react-to-print";
import { Printer } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import ShippingLabel from "./ShippingLabel";
import CjStandardLabel from "./CjStandardLabel";
import type { Order, CourierType, Shipment } from "@/types";

interface ShippingLabelPrintDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  orders: Order[];
  shipments: Shipment[];
  courierType: CourierType;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
}

export default function ShippingLabelPrintDialog({
  open,
  onOpenChange,
  orders,
  shipments,
  courierType,
  senderName,
  senderPhone,
  senderAddress,
}: ShippingLabelPrintDialogProps) {
  const printRef = useRef<HTMLDivElement>(null);

  // 송장번호가 있는 주문만 필터링하고 shipment 정보와 매핑
  const printableOrders = useMemo(() => {
    const shipmentMap = new Map(shipments.map((s) => [s.orderId, s]));
    return orders
      .filter((order) => shipmentMap.has(order.id) && shipmentMap.get(order.id)?.trackingNumber)
      .map((order) => ({
        order,
        shipment: shipmentMap.get(order.id)!,
      }));
  }, [orders, shipments]);

  // CJ 표준운송장 사용 여부
  const useCjStandardLabel = courierType === "CJ";

  const handlePrint = useReactToPrint({
    contentRef: printRef,
    documentTitle: `송장_${new Date().toISOString().split("T")[0]}`,
    pageStyle: useCjStandardLabel
      ? `
      @page {
        size: 123mm 100mm;
        margin: 0;
      }
      @media print {
        body {
          margin: 0;
          padding: 0;
        }
        .cj-standard-label {
          page-break-after: always;
          break-after: page;
        }
        .cj-standard-label:last-child {
          page-break-after: auto;
          break-after: auto;
        }
      }
    `
      : `
      @page {
        size: 100mm 70mm;
        margin: 0;
      }
      @media print {
        body {
          margin: 0;
          padding: 0;
        }
        .shipping-label {
          page-break-after: always;
          break-after: page;
        }
        .shipping-label:last-child {
          page-break-after: auto;
          break-after: auto;
        }
      }
    `,
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Printer className="h-5 w-5" />
            송장 출력
          </DialogTitle>
          <DialogDescription>
            {printableOrders.length}건의 송장을 출력합니다.{" "}
            {useCjStandardLabel
              ? "CJ대한통운 표준 라벨 용지(123x100mm)를 프린터에 넣어주세요."
              : "라벨 용지(100x70mm)를 프린터에 넣어주세요."}
          </DialogDescription>
        </DialogHeader>

        {printableOrders.length === 0 ? (
          <div className="py-8 text-center text-muted-foreground">
            출력할 수 있는 송장이 없습니다.
            <br />
            택배접수가 완료된 주문을 선택해주세요.
          </div>
        ) : (
          <>
            {/* 미리보기 영역 */}
            <div className="flex-1 overflow-auto border rounded-lg bg-gray-100 p-4">
              <div className="flex flex-wrap gap-4 justify-center">
                {printableOrders.slice(0, useCjStandardLabel ? 2 : 4).map(({ order, shipment }) => (
                  <div
                    key={order.id}
                    className="transform origin-top-left"
                    style={
                      useCjStandardLabel
                        ? { transform: "scale(0.45)", width: "55mm", height: "45mm" }
                        : { transform: "scale(0.6)", width: "60mm", height: "42mm" }
                    }
                  >
                    {useCjStandardLabel ? (
                      <CjStandardLabel
                        order={order}
                        shipment={shipment}
                        senderName={senderName}
                        senderPhone={senderPhone}
                        senderAddress={senderAddress}
                      />
                    ) : (
                      <ShippingLabel
                        order={order}
                        trackingNumber={shipment.trackingNumber}
                        courierType={courierType}
                        senderName={senderName}
                        senderPhone={senderPhone}
                        senderAddress={senderAddress}
                      />
                    )}
                  </div>
                ))}
                {printableOrders.length > (useCjStandardLabel ? 2 : 4) && (
                  <div className="flex items-center justify-center w-[60mm] h-[42mm] text-muted-foreground text-sm">
                    +{printableOrders.length - (useCjStandardLabel ? 2 : 4)}건 더
                  </div>
                )}
              </div>
            </div>

            {/* 출력용 숨겨진 영역 */}
            <div className="hidden">
              <div ref={printRef}>
                {printableOrders.map(({ order, shipment }) =>
                  useCjStandardLabel ? (
                    <CjStandardLabel
                      key={order.id}
                      order={order}
                      shipment={shipment}
                      senderName={senderName}
                      senderPhone={senderPhone}
                      senderAddress={senderAddress}
                    />
                  ) : (
                    <ShippingLabel
                      key={order.id}
                      order={order}
                      trackingNumber={shipment.trackingNumber}
                      courierType={courierType}
                      senderName={senderName}
                      senderPhone={senderPhone}
                      senderAddress={senderAddress}
                    />
                  )
                )}
              </div>
            </div>

            {/* 버튼 영역 */}
            <div className="flex justify-between items-center pt-4 border-t">
              <div className="text-sm text-muted-foreground">
                총 {printableOrders.length}건
              </div>
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => onOpenChange(false)}>
                  취소
                </Button>
                <Button onClick={() => handlePrint()}>
                  <Printer className="mr-2 h-4 w-4" />
                  출력하기
                </Button>
              </div>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
