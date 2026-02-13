import { useEffect, useState, useCallback } from "react";
import { toast } from "sonner";
import { Loader2, CheckCircle, XCircle, Printer } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import api from "@/lib/api";
import ShippingLabelPrintDialog from "@/components/shipping/ShippingLabelPrintDialog";
import type {
  Order,
  OrderItemRow,
  MarketplaceType,
  OrderStatus,
  PageResponse,
  MarketplaceCredentialResponse,
  CourierConfigResponse,
  BulkBookingResult,
  Shipment,
  ApiResponse,
} from "@/types";

// 내부 통합 상태 기반 뱃지 스타일
const getStatusBadgeClass = (status?: OrderStatus): string => {
  if (!status) return "bg-gray-100 text-gray-700 border-gray-200";

  switch (status) {
    case "COLLECTED":
      return "bg-red-100 text-red-700 border-red-200";  // 신규주문 (결제완료)
    case "CONFIRMED":
    case "READY_TO_SHIP":
      return "bg-green-100 text-green-700 border-green-200";  // 상품준비중/발송대기
    case "SHIPPING":
      return "bg-orange-100 text-orange-700 border-orange-200";  // 배송중
    case "DELIVERED":
      return "bg-blue-100 text-blue-700 border-blue-200";  // 배송완료
    case "PURCHASE_CONFIRMED":
      return "bg-emerald-100 text-emerald-700 border-emerald-200";  // 구매확정
    case "CANCELLED":
    case "RETURNED":
    case "EXCHANGED":
      return "bg-gray-100 text-gray-500 border-gray-200";  // 취소/반품/교환
    default:
      return "bg-gray-100 text-gray-700 border-gray-200";
  }
};

// 내부 통합 상태 한글 레이블
const statusLabels: Record<OrderStatus, string> = {
  COLLECTED: "결제완료",
  CONFIRMED: "상품준비중",
  READY_TO_SHIP: "발송대기",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  CANCELLED: "취소",
  RETURNED: "반품",
  EXCHANGED: "교환",
  PURCHASE_CONFIRMED: "구매확정",
};

const marketplaceLabels: Record<MarketplaceType, string> = {
  NAVER: "스마트스토어",
  COUPANG: "쿠팡",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

const COURIER_LABELS: Record<string, string> = {
  CJ: "CJ대한통운",
  HANJIN: "한진택배",
  LOGEN: "로젠택배",
  LOTTE: "롯데택배",
  POST: "우체국택배",
};

const BOX_TYPE_OPTIONS = [
  { value: "01", label: "극소" },
  { value: "02", label: "소" },
  { value: "03", label: "중" },
  { value: "04", label: "대" },
  { value: "05", label: "특대" },
];

function formatDate(dateStr?: string): string {
  if (!dateStr) return "-";
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch {
    return dateStr;
  }
}

function flattenOrdersToRows(orders: Order[]): OrderItemRow[] {
  const rows: OrderItemRow[] = [];
  orders.forEach((order, orderIndex) => {
    const items = order.items || [];
    if (items.length === 0) {
      rows.push({
        rowKey: `${order.id}-empty`,
        orderId: order.id,
        isFirstItemOfOrder: true,
        isLastItemOfOrder: true,
        itemsInOrder: 0,
        itemIndex: 0,
        orderIndex,
        order,
        item: {
          id: "",
          orderId: order.id,
          productName: "-",
          optionName: "",
          quantity: 0,
          unitPrice: 0,
          totalPrice: 0,
          marketplaceProductId: "",
          marketplaceItemId: "",
        },
      });
    } else {
      items.forEach((item, itemIndex) => {
        rows.push({
          rowKey: `${order.id}-${item.id}`,
          orderId: order.id,
          isFirstItemOfOrder: itemIndex === 0,
          isLastItemOfOrder: itemIndex === items.length - 1,
          itemsInOrder: items.length,
          itemIndex,
          orderIndex,
          order,
          item,
        });
      });
    }
  });
  return rows;
}

export default function Shipments() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [marketplaceFilter, setMarketplaceFilter] = useState<string>("");
  const [registeredMarketplaces, setRegisteredMarketplaces] = useState<MarketplaceType[]>([]);

  // Selection & booking state
  const [selectedOrderIds, setSelectedOrderIds] = useState<Set<string>>(new Set());
  const [booking, setBooking] = useState(false);
  const [courierConfigs, setCourierConfigs] = useState<CourierConfigResponse[]>([]);
  const [boxTypeMap, setBoxTypeMap] = useState<Record<string, string>>({});
  const [trackingMap, setTrackingMap] = useState<Record<string, string>>({});
  const [bookingResultDialogOpen, setBookingResultDialogOpen] = useState(false);
  const [bookingResult, setBookingResult] = useState<BulkBookingResult | null>(null);

  // 송장 출력 관련 상태
  const [printDialogOpen, setPrintDialogOpen] = useState(false);
  const [shipments, setShipments] = useState<Shipment[]>([]);

  useEffect(() => {
    const fetchMarketplaces = async () => {
      try {
        const { data } = await api.get<ApiResponse<MarketplaceCredentialResponse[]>>(
          "/api/v1/settings/marketplaces"
        );
        const marketplaces = (data.data ?? [])
          .filter((cred) => cred.active)
          .map((cred) => cred.marketplaceType as MarketplaceType);
        setRegisteredMarketplaces(marketplaces);
      } catch {
        // 실패해도 전체 마켓 표시
      }
    };
    const fetchCourierConfigs = async () => {
      try {
        const { data } = await api.get<ApiResponse<CourierConfigResponse[]>>(
          "/api/v1/settings/couriers"
        );
        setCourierConfigs(data.data ?? []);
      } catch {
        // ignore
      }
    };
    fetchMarketplaces();
    fetchCourierConfigs();
  }, []);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number | string[]> = {
        page,
        size: 20,
        statuses: ["CONFIRMED", "READY_TO_SHIP"],
      };
      if (marketplaceFilter) params.marketplace = marketplaceFilter;
      const { data } = await api.get<{ data: PageResponse<Order> }>("/api/v1/orders", {
        params,
        paramsSerializer: (p) => {
          const searchParams = new URLSearchParams();
          Object.entries(p as Record<string, unknown>).forEach(([key, val]) => {
            if (Array.isArray(val)) {
              val.forEach((v) => searchParams.append(key, String(v)));
            } else if (val !== undefined && val !== null) {
              searchParams.append(key, String(val));
            }
          });
          return searchParams.toString();
        },
      });
      const fetchedOrders = data.data?.content ?? [];
      setOrders(fetchedOrders);
      setTotal(data.data?.totalElements ?? 0);

      // Fetch shipments for displayed orders
      const orderIds = fetchedOrders.map((o) => o.id);
      if (orderIds.length > 0) {
        try {
          const shipRes = await api.get<ApiResponse<Shipment[]>>("/api/v1/shipments", {
            params: { orderIds },
            paramsSerializer: (p) => {
              const sp = new URLSearchParams();
              Object.entries(p as Record<string, unknown>).forEach(([key, val]) => {
                if (Array.isArray(val)) {
                  val.forEach((v) => sp.append(key, String(v)));
                } else if (val !== undefined && val !== null) {
                  sp.append(key, String(val));
                }
              });
              return sp.toString();
            },
          });
          const fetchedShipments = shipRes.data.data ?? [];
          setShipments(fetchedShipments);
          const newTrackingMap: Record<string, string> = {};
          for (const s of fetchedShipments) {
            if (s.trackingNumber) {
              newTrackingMap[s.orderId] = s.trackingNumber;
            }
          }
          setTrackingMap(newTrackingMap);
        } catch {
          // shipment fetch failure is non-critical
        }
      } else {
        setTrackingMap({});
        setShipments([]);
      }
    } catch {
      toast.error("주문 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [page, marketplaceFilter]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Clear selection and box type map when page/filter changes
  useEffect(() => {
    setSelectedOrderIds(new Set());
    setBoxTypeMap({});
  }, [page, marketplaceFilter]);

  // Initialize boxTypeMap for new orders with default "01" (극소)
  useEffect(() => {
    setBoxTypeMap((prev) => {
      const next = { ...prev };
      let changed = false;
      for (const order of orders) {
        if (!(order.id in next)) {
          next[order.id] = "01";
          changed = true;
        }
      }
      return changed ? next : prev;
    });
  }, [orders]);

  const totalPages = Math.ceil(total / 20);

  const toggleOrderSelection = (orderId: string) => {
    setSelectedOrderIds((prev) => {
      const next = new Set(prev);
      if (next.has(orderId)) {
        next.delete(orderId);
      } else {
        next.add(orderId);
      }
      return next;
    });
  };

  // 선택된 주문 중 택배접수 가능한 주문 수 (송장번호 없는 주문)
  const selectedBookableCount = orders.filter((o) => selectedOrderIds.has(o.id) && !trackingMap[o.id] && o.status !== "READY_TO_SHIP").length;
  // 선택된 주문 중 송장출력 가능한 주문 수 (송장번호 있는 주문)
  const selectedPrintableCount = orders.filter((o) => selectedOrderIds.has(o.id) && !!trackingMap[o.id]).length;

  const toggleAllSelection = () => {
    if (orders.length > 0 && selectedOrderIds.size === orders.length) {
      setSelectedOrderIds(new Set());
    } else {
      setSelectedOrderIds(new Set(orders.map((o) => o.id)));
    }
  };

  const handleBulkBook = async () => {
    // 선택된 주문 중 송장번호가 없는 주문만 접수
    const bookableIds = orders
      .filter((o) => selectedOrderIds.has(o.id) && !trackingMap[o.id] && o.status !== "READY_TO_SHIP")
      .map((o) => o.id);

    if (bookableIds.length === 0) {
      toast.error("접수할 주문을 선택해주세요");
      return;
    }
    if (courierConfigs.length === 0) {
      toast.error("설정 > 택배사 탭에서 택배사를 먼저 등록하세요");
      return;
    }

    const courierType = courierConfigs[0].courierType;

    setBooking(true);
    setBookingResult(null);
    try {
      // Build per-order boxTypeCd map for bookable orders only
      const boxTypeCdMap: Record<string, string> = {};
      for (const id of bookableIds) {
        boxTypeCdMap[id] = boxTypeMap[id] || "01";
      }

      const { data } = await api.post<ApiResponse<BulkBookingResult>>(
        "/api/v1/shipments/bulk-book",
        {
          orderIds: bookableIds,
          courierType,
          boxTypeCdMap,
        },
        { timeout: 120000 }
      );

      const result = data.data;
      if (result) {
        setBookingResult(result);
        setBookingResultDialogOpen(true);
        setSelectedOrderIds(new Set());
        // Refresh orders + shipments to reflect updated status and tracking numbers
        fetchOrders();
      }
    } catch {
      toast.error("택배접수 중 오류가 발생했습니다");
    } finally {
      setBooking(false);
    }
  };

  return (
    <div>
      <h2 className="mb-4 text-xl font-semibold tracking-tight">배송 관리</h2>
      <Card>
        <CardContent className="pt-6">
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <Select
              value={marketplaceFilter}
              onValueChange={(v) => {
                setMarketplaceFilter(v === "all" ? "" : v);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="마켓 선택" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 마켓</SelectItem>
                {registeredMarketplaces.length > 0
                  ? registeredMarketplaces.map((marketplace) => (
                      <SelectItem key={marketplace} value={marketplace}>
                        {marketplaceLabels[marketplace] || marketplace}
                      </SelectItem>
                    ))
                  : Object.entries(marketplaceLabels).map(([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    ))}
              </SelectContent>
            </Select>

            <div className="ml-auto flex items-center gap-2">
              {selectedOrderIds.size > 0 && (
                <Badge variant="secondary" className="text-sm">
                  {selectedOrderIds.size}건 선택
                </Badge>
              )}
              <Button
                size="sm"
                variant="outline"
                onClick={() => setPrintDialogOpen(true)}
                disabled={selectedPrintableCount === 0}
              >
                <Printer className="mr-2 h-4 w-4" />
                송장출력
                {selectedPrintableCount > 0 && (
                  <span className="ml-1 text-xs opacity-75">
                    ({selectedPrintableCount}건)
                  </span>
                )}
              </Button>
              <Button
                size="sm"
                onClick={handleBulkBook}
                disabled={booking || selectedBookableCount === 0}
              >
                {booking ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : null}
                택배접수
                {selectedBookableCount > 0 && (
                  <span className="ml-1 text-xs opacity-75">
                    ({selectedBookableCount}건)
                  </span>
                )}
                {courierConfigs.length > 0 && selectedBookableCount === 0 && (
                  <span className="ml-1 text-xs opacity-75">
                    ({COURIER_LABELS[courierConfigs[0].courierType] || courierConfigs[0].courierType})
                  </span>
                )}
              </Button>
            </div>
          </div>

          {courierConfigs.length === 0 && (
            <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
              설정 &gt; 택배사 탭에서 택배사를 먼저 등록하세요.
            </div>
          )}

          {loading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : orders.length === 0 ? (
            <p className="py-12 text-center text-sm text-muted-foreground">
              상품준비중인 주문이 없습니다.
            </p>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[40px]">
                      <Checkbox
                        checked={orders.length > 0 && selectedOrderIds.size === orders.length}
                        onCheckedChange={toggleAllSelection}
                        disabled={orders.length === 0}
                      />
                    </TableHead>
                    <TableHead className="w-[150px]">주문번호</TableHead>
                    <TableHead className="w-[90px]">마켓</TableHead>
                    <TableHead>상품명</TableHead>
                    <TableHead className="w-[50px] text-right">수량</TableHead>
                    <TableHead className="w-[80px]">수취인</TableHead>
                    <TableHead className="w-[120px]">연락처</TableHead>
                    <TableHead>배송지</TableHead>
                    <TableHead className="w-[80px]">상태</TableHead>
                    <TableHead className="w-[80px]">박스</TableHead>
                    <TableHead className="w-[130px]">송장번호</TableHead>
                    <TableHead className="w-[90px]">주문일</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {flattenOrdersToRows(orders).map((row) => {
                    const { order, item, isFirstItemOfOrder, itemsInOrder, orderIndex } = row;
                    const isSelected = selectedOrderIds.has(order.id);
                    const isBooked = !!trackingMap[order.id] || order.status === "READY_TO_SHIP";
                    const rowClassName = `${
                      isFirstItemOfOrder ? "order-row-first" : "order-row-continued"
                    } ${orderIndex % 2 === 0 ? "order-group-even" : "order-group-odd"} ${
                      isBooked ? "bg-green-50/50" : isSelected ? "bg-blue-50/50" : ""
                    }`;

                    return (
                      <TableRow key={row.rowKey} className={rowClassName}>
                        <TableCell>
                          {isFirstItemOfOrder && (
                            <Checkbox
                              checked={isSelected}
                              onCheckedChange={() => toggleOrderSelection(order.id)}
                            />
                          )}
                        </TableCell>
                        <TableCell className="truncate font-mono text-xs">
                          {isFirstItemOfOrder ? (
                            <div className="flex items-center gap-1">
                              <span>{order.marketplaceOrderId}</span>
                              {itemsInOrder > 1 && (
                                <Badge variant="secondary" className="text-[10px] px-1 py-0 h-4">
                                  {itemsInOrder}건
                                </Badge>
                              )}
                            </div>
                          ) : (
                            <span className="text-muted-foreground pl-2">└</span>
                          )}
                        </TableCell>
                        <TableCell className="text-sm">
                          {isFirstItemOfOrder
                            ? marketplaceLabels[order.marketplaceType] || order.marketplaceType
                            : ""}
                        </TableCell>
                        <TableCell className="max-w-0">
                          <div className="min-w-0">
                            <span className="truncate block text-sm" title={item.productName}>
                              {item.productName}
                            </span>
                            {item.optionName && (
                              <span className="truncate block text-xs text-muted-foreground" title={item.optionName}>
                                옵션: {item.optionName}
                              </span>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-right text-sm">
                          {item.quantity || "-"}
                        </TableCell>
                        <TableCell className="text-sm">
                          {isFirstItemOfOrder ? order.receiverName : ""}
                        </TableCell>
                        <TableCell className="text-sm">
                          {isFirstItemOfOrder ? order.receiverPhone : ""}
                        </TableCell>
                        <TableCell className="max-w-0">
                          {isFirstItemOfOrder ? (
                            <span
                              className="truncate block text-sm"
                              title={order.receiverAddress}
                            >
                              {order.receiverAddress}
                            </span>
                          ) : null}
                        </TableCell>
                        <TableCell>
                          {isFirstItemOfOrder && (
                            <Badge
                              variant="outline"
                              className={`text-xs border ${getStatusBadgeClass(order.status)}`}
                            >
                              {statusLabels[order.status] || order.status}
                            </Badge>
                          )}
                        </TableCell>
                        <TableCell>
                          {isFirstItemOfOrder && (
                            <Select
                              value={boxTypeMap[order.id] || "01"}
                              onValueChange={(v) =>
                                setBoxTypeMap((prev) => ({ ...prev, [order.id]: v }))
                              }
                              disabled={isBooked}
                            >
                              <SelectTrigger className="h-7 w-[70px] text-xs">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                {BOX_TYPE_OPTIONS.map((opt) => (
                                  <SelectItem key={opt.value} value={opt.value}>
                                    {opt.label}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          )}
                        </TableCell>
                        <TableCell className="font-mono text-xs">
                          {isFirstItemOfOrder && trackingMap[order.id] ? (
                            <span className="text-green-700">{trackingMap[order.id]}</span>
                          ) : null}
                        </TableCell>
                        <TableCell className="text-sm whitespace-nowrap">
                          {isFirstItemOfOrder ? formatDate(order.orderedAt) : ""}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
              <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                <span>총 {total}건</span>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    이전
                  </Button>
                  <span className="flex items-center px-2">
                    {page + 1} / {totalPages || 1}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page + 1 >= totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* 송장 출력 다이얼로그 */}
      {courierConfigs.length > 0 && (
        <ShippingLabelPrintDialog
          open={printDialogOpen}
          onOpenChange={setPrintDialogOpen}
          orders={orders.filter((o) => selectedOrderIds.has(o.id))}
          shipments={shipments}
          courierType={courierConfigs[0].courierType}
          senderName={courierConfigs[0].senderName}
          senderPhone={courierConfigs[0].senderPhone}
          senderAddress={
            courierConfigs[0].extraConfig?.senderDetailAddress
              ? `${courierConfigs[0].senderAddress} ${courierConfigs[0].extraConfig.senderDetailAddress}`
              : courierConfigs[0].senderAddress
          }
          boxType={(courierConfigs[0].extraConfig?.boxTypeCd as string) || "02"}
        />
      )}

      {/* Booking result dialog */}
      <Dialog open={bookingResultDialogOpen} onOpenChange={setBookingResultDialogOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {bookingResult && bookingResult.failCount === 0 ? (
                <>
                  <CheckCircle className="h-5 w-5 text-green-500" />
                  택배접수 완료
                </>
              ) : (
                <>
                  {bookingResult && bookingResult.successCount > 0 ? (
                    <CheckCircle className="h-5 w-5 text-amber-500" />
                  ) : (
                    <XCircle className="h-5 w-5 text-red-500" />
                  )}
                  택배접수 결과
                </>
              )}
            </DialogTitle>
            <DialogDescription>
              {bookingResult
                ? `총 ${bookingResult.total}건 중 성공 ${bookingResult.successCount}건, 실패 ${bookingResult.failCount}건`
                : ""}
            </DialogDescription>
          </DialogHeader>

          {bookingResult && (
            <div className="mt-2 space-y-3">
              <div className="grid grid-cols-3 gap-4">
                <div className="rounded-lg border p-3 text-center">
                  <div className="text-2xl font-bold">{bookingResult.total}</div>
                  <div className="text-sm text-muted-foreground">전체</div>
                </div>
                <div className="rounded-lg border p-3 text-center">
                  <div className="text-2xl font-bold text-green-600">{bookingResult.successCount}</div>
                  <div className="text-sm text-muted-foreground">성공</div>
                </div>
                <div className="rounded-lg border p-3 text-center">
                  <div className="text-2xl font-bold text-red-600">{bookingResult.failCount}</div>
                  <div className="text-sm text-muted-foreground">실패</div>
                </div>
              </div>

              {bookingResult.results.length > 0 && (
                <div className="max-h-60 overflow-y-auto rounded-lg border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[60px]">결과</TableHead>
                        <TableHead>운송장번호</TableHead>
                        <TableHead>비고</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {bookingResult.results.map((r, idx) => (
                        <TableRow key={idx}>
                          <TableCell>
                            {r.success ? (
                              <Badge variant="default" className="text-xs">성공</Badge>
                            ) : (
                              <Badge variant="destructive" className="text-xs">실패</Badge>
                            )}
                          </TableCell>
                          <TableCell className="font-mono text-xs">
                            {r.trackingNumber || "-"}
                          </TableCell>
                          <TableCell className="text-xs text-muted-foreground max-w-[200px] truncate">
                            {r.error || "-"}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}

              <div className="flex justify-end">
                <Button onClick={() => setBookingResultDialogOpen(false)}>
                  확인
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
