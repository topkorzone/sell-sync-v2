import { useEffect, useState, useCallback } from "react";
import { toast } from "sonner";
import { RefreshCw, Search, Loader2, Upload, Calendar } from "lucide-react";
import MappingDialog from "@/components/orders/MappingDialog";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Calendar as CalendarComponent } from "@/components/ui/calendar";
import { format, subDays, subMonths, startOfDay, endOfDay } from "date-fns";
import { ko } from "date-fns/locale";
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
import api from "@/lib/api";
import type { Order, OrderItem, OrderItemRow, MarketplaceType, OrderStatus, PageResponse, MarketplaceCredentialResponse, ApiResponse, ErpSyncResult, ErpBatchSyncResult } from "@/types";

// 상태별 뱃지 색상 - CSS 클래스로 직접 지정
const getStatusBadgeClass = (marketplaceStatus?: string): string => {
  if (!marketplaceStatus) return "bg-gray-100 text-gray-700 border-gray-200";

  // 결제완료/확인 상태 - 빨간색
  if (["ACCEPT", "PAYED", "PAYMENT_WAITING"].includes(marketplaceStatus)) {
    return "bg-red-100 text-red-700 border-red-200";
  }
  // 배송지시/준비중 상태 - 파란색
  if (["INSTRUCT", "DEPARTURE"].includes(marketplaceStatus)) {
    return "bg-blue-100 text-blue-700 border-blue-200";
  }
  // 배송중 상태 - 주황색
  if (["DELIVERING"].includes(marketplaceStatus)) {
    return "bg-orange-100 text-orange-700 border-orange-200";
  }
  // 배송완료 상태 - 초록색
  if (["FINAL_DELIVERY", "DELIVERED"].includes(marketplaceStatus)) {
    return "bg-green-100 text-green-700 border-green-200";
  }
  // 구매확정 상태 - 진한 초록색
  if (["PURCHASE_DECIDED"].includes(marketplaceStatus)) {
    return "bg-emerald-100 text-emerald-700 border-emerald-200";
  }
  // 취소/반품/교환 상태 - 회색
  if (["CANCEL", "CANCELLED", "RETURN", "RETURNED", "EXCHANGED"].includes(marketplaceStatus)) {
    return "bg-gray-100 text-gray-500 border-gray-200";
  }
  // 상품준비중 상태 - 노란색
  if (["INSTRUCT"].includes(marketplaceStatus)) {
    return "bg-yellow-100 text-yellow-700 border-yellow-200";
  }

  return "bg-gray-100 text-gray-700 border-gray-200";
};


const statusLabels: Record<OrderStatus, string> = {
  PAYMENT_COMPLETE: "결제완료",
  PREPARING: "상품준비중",
  SHIPPING_READY: "배송지시",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  CANCELLED: "취소",
  RETURNED: "반품",
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

// 쿠팡 마켓플레이스 원본 상태 레이블
const coupangStatusLabels: Record<string, string> = {
  ACCEPT: "결제완료",
  INSTRUCT: "상품준비중",
  DEPARTURE: "배송지시",
  DELIVERING: "배송중",
  FINAL_DELIVERY: "배송완료",
  NONE_TRACKING: "직접배송",
  CANCEL: "취소",
  RETURN: "반품",
};

// 네이버 마켓플레이스 원본 상태 레이블
const naverStatusLabels: Record<string, string> = {
  PAYMENT_WAITING: "결제대기",
  PAYED: "결제완료",
  DELIVERING: "배송중",
  DELIVERED: "배송완료",
  PURCHASE_DECIDED: "구매확정",
  EXCHANGED: "교환",
  CANCELLED: "취소",
  RETURNED: "반품",
};

// 마켓플레이스별 원본 상태 레이블 가져오기
function getMarketplaceStatusLabel(marketplaceType: MarketplaceType, marketplaceStatus?: string): string {
  if (!marketplaceStatus) return "-";

  switch (marketplaceType) {
    case "COUPANG":
      return coupangStatusLabels[marketplaceStatus] || marketplaceStatus;
    case "NAVER":
      return naverStatusLabels[marketplaceStatus] || marketplaceStatus;
    default:
      return marketplaceStatus;
  }
}

// 날짜 포맷팅 (YYYY-MM-DD HH:mm)
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

// 주문 목록을 플랫 행 목록으로 변환 (각 OrderItem을 별도 행으로)
function flattenOrdersToRows(orders: Order[]): OrderItemRow[] {
  const rows: OrderItemRow[] = [];
  orders.forEach((order, orderIndex) => {
    const items = order.items || [];
    if (items.length === 0) {
      // 상품이 없는 주문은 빈 상품으로 하나의 행 생성
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

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [syncingSettlement, setSyncingSettlement] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [marketplaceFilter, setMarketplaceFilter] = useState<string>("");
  const [search, setSearch] = useState("");
  const [mappingDialogOpen, setMappingDialogOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [selectedMappingItem, setSelectedMappingItem] = useState<OrderItem | null>(null);
  const [registeredMarketplaces, setRegisteredMarketplaces] = useState<MarketplaceType[]>([]);
  const [syncingErp, setSyncingErp] = useState(false);
  const [syncingErpOrderId, setSyncingErpOrderId] = useState<string | null>(null);

  // 날짜 필터 상태
  const [dateRange, setDateRange] = useState<"today" | "1week" | "1month" | "3months" | "custom">("1week");
  const [startDate, setStartDate] = useState<Date>(subDays(new Date(), 7));
  const [endDate, setEndDate] = useState<Date>(new Date());
  const [startDateOpen, setStartDateOpen] = useState(false);
  const [endDateOpen, setEndDateOpen] = useState(false);

  // 날짜 범위 선택 핸들러
  const handleDateRangeChange = (range: "today" | "1week" | "1month" | "3months") => {
    const now = new Date();
    let start: Date;
    switch (range) {
      case "today":
        start = startOfDay(now);
        break;
      case "1week":
        start = subDays(now, 7);
        break;
      case "1month":
        start = subMonths(now, 1);
        break;
      case "3months":
        start = subMonths(now, 3);
        break;
    }
    setStartDate(start);
    setEndDate(endOfDay(now));
    setDateRange(range);
    setPage(0);
  };

  // 날짜를 API 형식으로 변환 (YYYY-MM-DD)
  const formatDateForApi = (date: Date): string => {
    return format(date, "yyyy-MM-dd");
  };

  // 등록된 마켓플레이스 목록 조회
  useEffect(() => {
    const fetchMarketplaces = async () => {
      try {
        const { data } = await api.get<ApiResponse<MarketplaceCredentialResponse[]>>(
          "/api/v1/settings/marketplaces"
        );
        const marketplaces = (data.data ?? [])
          .filter(cred => cred.active)
          .map(cred => cred.marketplaceType as MarketplaceType);
        setRegisteredMarketplaces(marketplaces);
      } catch {
        // 실패해도 전체 마켓 표시
      }
    };
    fetchMarketplaces();
  }, []);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page, size: 20 };
      if (statusFilter) params.status = statusFilter;
      if (marketplaceFilter) params.marketplace = marketplaceFilter;
      if (search) params.search = search;
      // 날짜 필터 추가
      params.startDate = formatDateForApi(startDate);
      params.endDate = formatDateForApi(endDate);
      const { data } = await api.get<{ data: PageResponse<Order> }>("/api/v1/orders", { params });
      setOrders(data.data?.content ?? []);
      setTotal(data.data?.totalElements ?? 0);
    } catch {
      toast.error("주문 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter, marketplaceFilter, search, startDate, endDate]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleSync = async () => {
    setSyncing(true);
    try {
      await api.post("/api/v1/orders/sync");
      toast.success("동기화 요청이 완료되었습니다.");
      fetchOrders();
    } catch {
      toast.error("동기화 요청에 실패했습니다.");
    } finally {
      setSyncing(false);
    }
  };

  const handleSettlementSync = async () => {
    setSyncingSettlement(true);
    try {
      const { data } = await api.post<ApiResponse<Record<string, unknown>>>("/api/v1/settlements/sync");
      const totalSynced = (data.data as Record<string, unknown>)?.totalSynced ?? 0;
      toast.success(`정산 수집 완료: ${totalSynced}건`);
      fetchOrders();
    } catch {
      toast.error("정산 수집에 실패했습니다.");
    } finally {
      setSyncingSettlement(false);
    }
  };

  const handleErpSyncAll = async () => {
    setSyncingErp(true);
    try {
      const { data } = await api.post<ApiResponse<ErpBatchSyncResult>>("/api/v1/erp/orders/sync-all");
      const result = data.data;
      if (result) {
        toast.success(`ERP 전표 등록: 성공 ${result.successCount}건 / 실패 ${result.failCount}건`);
      }
      fetchOrders();
    } catch {
      toast.error("ERP 전표 일괄 등록에 실패했습니다");
    } finally {
      setSyncingErp(false);
    }
  };

  const handleErpSyncOrder = async (orderId: string) => {
    setSyncingErpOrderId(orderId);
    try {
      const { data } = await api.post<ApiResponse<ErpSyncResult>>(`/api/v1/erp/orders/${orderId}/sync`);
      if (data.data?.success) {
        toast.success(data.data.message || "전표 등록 완료");
        fetchOrders();
      } else {
        toast.error(data.data?.message || "전표 등록 실패");
      }
    } catch {
      toast.error("ERP 전표 등록에 실패했습니다");
    } finally {
      setSyncingErpOrderId(null);
    }
  };

  const totalPages = Math.ceil(total / 20);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-semibold tracking-tight">주문 관리</h2>
        <div className="flex gap-2 hidden">
          <Button variant="outline" onClick={handleErpSyncAll} disabled={syncingErp}>
            {syncingErp ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <Upload className="mr-2 h-4 w-4" />
            )}
            ERP 전표 등록
          </Button>
          <Button variant="outline" onClick={handleSettlementSync} disabled={syncingSettlement}>
            <RefreshCw className={`mr-2 h-4 w-4 ${syncingSettlement ? "animate-spin" : ""}`} />
            정산 수집
          </Button>
          <Button onClick={handleSync} disabled={syncing}>
            <RefreshCw className={`mr-2 h-4 w-4 ${syncing ? "animate-spin" : ""}`} />
            주문 동기화
          </Button>
        </div>
      </div>
      <Card>
        <CardContent className="pt-6">
          {/* 날짜 필터 */}
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <span className="text-sm text-muted-foreground">조회기간</span>
            <div className="flex items-center gap-1 border rounded-md p-1">
              <Button
                variant={dateRange === "today" ? "default" : "ghost"}
                size="sm"
                className="h-7 px-3"
                onClick={() => handleDateRangeChange("today")}
              >
                오늘
              </Button>
              <Button
                variant={dateRange === "1week" ? "default" : "ghost"}
                size="sm"
                className="h-7 px-3"
                onClick={() => handleDateRangeChange("1week")}
              >
                1주일
              </Button>
              <Button
                variant={dateRange === "1month" ? "default" : "ghost"}
                size="sm"
                className="h-7 px-3"
                onClick={() => handleDateRangeChange("1month")}
              >
                1개월
              </Button>
              <Button
                variant={dateRange === "3months" ? "default" : "ghost"}
                size="sm"
                className="h-7 px-3"
                onClick={() => handleDateRangeChange("3months")}
              >
                3개월
              </Button>
            </div>
            <Popover open={startDateOpen} onOpenChange={setStartDateOpen}>
              <PopoverTrigger asChild>
                <Button variant="outline" size="sm" className="h-9 w-[130px] justify-start text-left font-normal">
                  <Calendar className="mr-2 h-4 w-4" />
                  {format(startDate, "yyyy.MM.dd")}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <CalendarComponent
                  mode="single"
                  selected={startDate}
                  onSelect={(date) => {
                    if (date) {
                      setStartDate(date);
                      setDateRange("custom");
                      setPage(0);
                    }
                    setStartDateOpen(false);
                  }}
                  locale={ko}
                  initialFocus
                />
              </PopoverContent>
            </Popover>
            <span className="text-muted-foreground">~</span>
            <Popover open={endDateOpen} onOpenChange={setEndDateOpen}>
              <PopoverTrigger asChild>
                <Button variant="outline" size="sm" className="h-9 w-[130px] justify-start text-left font-normal">
                  <Calendar className="mr-2 h-4 w-4" />
                  {format(endDate, "yyyy.MM.dd")}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <CalendarComponent
                  mode="single"
                  selected={endDate}
                  onSelect={(date) => {
                    if (date) {
                      setEndDate(endOfDay(date));
                      setDateRange("custom");
                      setPage(0);
                    }
                    setEndDateOpen(false);
                  }}
                  locale={ko}
                  initialFocus
                />
              </PopoverContent>
            </Popover>
          </div>
          {/* 마켓/상태/검색 필터 */}
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <Select
              value={marketplaceFilter}
              onValueChange={(v) => {
                setMarketplaceFilter(v === "all" ? "" : v);
                setSearch("");
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
            <Select
              value={statusFilter}
              onValueChange={(v) => {
                setStatusFilter(v === "all" ? "" : v);
                setSearch("");
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[130px]">
                <SelectValue placeholder="상태 선택" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 상태</SelectItem>
                {Object.entries(statusLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="주문번호 / 수취인 검색"
                className="w-[240px] pl-9"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    setPage(0);
                    fetchOrders();
                  }
                }}
              />
            </div>
          </div>
          {loading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : orders.length === 0 ? (
            <p className="py-12 text-center text-sm text-muted-foreground">
              주문 데이터가 없습니다.
            </p>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    {/* 주문 레벨 컬럼 */}
                    <TableHead className="w-[150px]">주문번호</TableHead>
                    <TableHead className="w-[90px]">마켓</TableHead>
                    {/* 상품 레벨 컬럼 */}
                    <TableHead>상품명</TableHead>
                    <TableHead className="w-[50px] text-right">수량</TableHead>
                    <TableHead className="w-[80px] text-right">단가</TableHead>
                    <TableHead className="w-[90px] text-right">금액</TableHead>
                    {/* 배송비, 정산예정 - 금액 다음으로 이동 */}
                    <TableHead className="w-[70px] text-right">배송비</TableHead>
                    <TableHead className="w-[90px] text-right">정산예정</TableHead>
                    {/* 주문 레벨 컬럼 (계속) */}
                    <TableHead className="w-[80px]">수취인</TableHead>
                    <TableHead className="w-[80px]">상태</TableHead>
                    {/* 상품 레벨 컬럼 */}
                    <TableHead className="w-[70px]">매핑</TableHead>
                    {/* 주문 레벨 컬럼 */}
                    <TableHead className="w-[60px]">정산</TableHead>
                    <TableHead className="w-[60px]">ERP</TableHead>
                    <TableHead className="w-[90px]">주문일</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {flattenOrdersToRows(orders).map((row) => {
                    const { order, item, isFirstItemOfOrder, itemsInOrder, orderIndex } = row;
                    const isMapped = item.erpProdCd || item.hasMasterMapping;
                    const rowClassName = `${
                      isFirstItemOfOrder ? "order-row-first" : "order-row-continued"
                    } ${orderIndex % 2 === 0 ? "order-group-even" : "order-group-odd"}`;

                    return (
                      <TableRow key={row.rowKey} className={rowClassName}>
                        {/* 주문번호 - 첫 행만 표시 */}
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
                        {/* 마켓 - 첫 행만 표시 */}
                        <TableCell className="text-sm">
                          {isFirstItemOfOrder
                            ? marketplaceLabels[order.marketplaceType] || order.marketplaceType
                            : ""}
                        </TableCell>
                        {/* 상품명 + 옵션 - 모든 행 표시 */}
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
                        {/* 수량 - 모든 행 표시 */}
                        <TableCell className="text-right text-sm">
                          {item.quantity || "-"}
                        </TableCell>
                        {/* 단가 - 모든 행 표시 */}
                        <TableCell className="text-right text-sm">
                          {item.unitPrice ? `${item.unitPrice.toLocaleString()}` : "-"}
                        </TableCell>
                        {/* 금액 - 모든 행 표시 */}
                        <TableCell className="text-right text-sm">
                          {item.totalPrice ? `${item.totalPrice.toLocaleString()}` : "-"}
                        </TableCell>
                        {/* 배송비 - 첫 행만 표시 (금액 다음으로 이동) */}
                        <TableCell className="text-right text-sm">
                          {isFirstItemOfOrder
                            ? `${order.deliveryFee?.toLocaleString() || 0}`
                            : ""}
                        </TableCell>
                        {/* 정산예정 - 각 상품별 표시 (배송비 다음으로 이동) */}
                        <TableCell className="text-right text-sm">
                          {item.expectedSettlementAmount != null
                            ? `${item.expectedSettlementAmount.toLocaleString()}`
                            : "-"}
                        </TableCell>
                        {/* 수취인 - 첫 행만 표시 */}
                        <TableCell className="text-sm">
                          {isFirstItemOfOrder ? order.receiverName : ""}
                        </TableCell>
                        {/* 상태 - 첫 행만 표시 (상태별 색상 구분) */}
                        <TableCell>
                          {isFirstItemOfOrder && (
                            <Badge
                              variant="outline"
                              className={`text-xs border ${getStatusBadgeClass(order.marketplaceStatus)}`}
                            >
                              {getMarketplaceStatusLabel(order.marketplaceType, order.marketplaceStatus)}
                            </Badge>
                          )}
                        </TableCell>
                        {/* 매핑 - 모든 행 표시 */}
                        <TableCell>
                          {item.id && (
                            <Badge
                              variant={isMapped ? "default" : "outline"}
                              className="cursor-pointer hover:opacity-80 text-xs"
                              onClick={() => {
                                setSelectedOrder(order);
                                setSelectedMappingItem(item);
                                setMappingDialogOpen(true);
                              }}
                            >
                              {isMapped ? (item.erpProdCd || "매핑") : "미매핑"}
                            </Badge>
                          )}
                        </TableCell>
                        {/* 정산 - 첫 행만 표시 */}
                        <TableCell>
                          {isFirstItemOfOrder && (
                            <Badge
                              variant="outline"
                              className={`text-xs border ${
                                order.settlementCollected
                                  ? "bg-green-100 text-green-700 border-green-200"
                                  : "bg-gray-100 text-gray-500 border-gray-200"
                              }`}
                            >
                              {order.settlementCollected ? "완료" : "미수집"}
                            </Badge>
                          )}
                        </TableCell>
                        {/* ERP - 첫 행만 표시 */}
                        <TableCell>
                          {isFirstItemOfOrder && (
                            <Badge
                              variant="outline"
                              className={`text-xs border cursor-pointer hover:opacity-80 ${
                                order.erpSynced
                                  ? "bg-blue-100 text-blue-700 border-blue-200"
                                  : "bg-gray-100 text-gray-500 border-gray-200"
                              }`}
                              onClick={() => {
                                if (!order.erpSynced) {
                                  handleErpSyncOrder(order.id);
                                }
                              }}
                            >
                              {syncingErpOrderId === order.id ? (
                                <Loader2 className="h-3 w-3 animate-spin" />
                              ) : order.erpSynced ? (
                                "완료"
                              ) : (
                                "미등록"
                              )}
                            </Badge>
                          )}
                        </TableCell>
                        {/* 주문일 - 첫 행만 표시 */}
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

      {/* ERP 품목 매핑 다이얼로그 */}
      {selectedOrder && (
        <MappingDialog
          order={selectedOrder}
          initialItemId={selectedMappingItem?.id}
          open={mappingDialogOpen}
          onOpenChange={(open) => {
            setMappingDialogOpen(open);
            if (!open) setSelectedMappingItem(null);
          }}
          onMappingComplete={fetchOrders}
        />
      )}
    </div>
  );
}
